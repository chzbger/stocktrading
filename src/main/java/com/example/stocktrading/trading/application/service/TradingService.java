package com.example.stocktrading.trading.application.service;

import com.example.stocktrading.user.application.port.out.UserPort;
import com.example.stocktrading.user.domain.User;
import com.example.stocktrading.trading.application.port.in.AssetUseCase;
import com.example.stocktrading.trading.application.port.in.TradingUseCase;
import com.example.stocktrading.trading.application.port.out.AiModelPort;
import com.example.stocktrading.trading.application.port.out.AiTrainingHistoryPort;
import com.example.stocktrading.trading.application.port.out.BrokerApiPort;
import com.example.stocktrading.trading.application.port.out.TradeLogPort;
import com.example.stocktrading.trading.application.port.out.TradingTargetPort;
import com.example.stocktrading.trading.domain.TradingTarget;
import com.example.stocktrading.trading.domain.TrainingHistory;
import com.example.stocktrading.trading.domain.TrainingHistory.TrainingStatus;
import com.example.stocktrading.trading.domain.Asset;
import com.example.stocktrading.trading.domain.StockCandle;
import com.example.stocktrading.trading.domain.StockOrder;
import com.example.stocktrading.trading.domain.TradeLog;
import com.example.stocktrading.user.application.port.out.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradingService implements TradingUseCase {

    private final UserPort userPort;
    private final TradeLogPort tradeLogPort;
    private final TradingTargetPort tradingTargetPort;
    private final AiTrainingHistoryPort aiTrainingHistoryPort;
    private final BrokerApiPort brokerApiPort;
    private final AssetUseCase assetUseCase;
    private final AiModelPort aiModelPort;
    private final NotificationPort notificationPort;

    private static final int CANCEL_TIMEOUT_MINUTES = 2;
    private static final int STALE_PENDING_MINUTES = 10;
    private static final int MAX_HOLDING_MINUTES = 25;

    private record CandleData(List<StockCandle> minute, List<StockCandle> fiveMin) {}

    @Override
    @Transactional
    public void initialize() {
        // 학습 진행중 상태인것들 취소 처리
        cleanupTraining();

        // PENDING 정리 (10분 이상)
        cleanupPendingOrders();
    }

    @Override
    @Transactional
    public void executeTradingCycle() {
        List<TradingTarget> activeItems = new ArrayList<>(tradingTargetPort.findActiveItems());
        if (activeItems.isEmpty()) return;

        // 유저맵 빌드 + 거래시간 필터
        Map<Long, User> userMap = activeItems.stream()
                .map(item -> userPort.findById(item.getUserId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getUserId, Function.identity(), (a, b) -> a));
        activeItems = filterByTradingHours(activeItems, userMap);
        if (activeItems.isEmpty()) return;

        log.info("[Cycle] ========== Trading Cycle Start (Active: {}) ==========", activeItems.size());

        // 0-1. PENDING SELL 가드: PENDING SELL이 있는 ticker는 제외
        activeItems.removeIf(item ->
                tradeLogPort.hasPendingSell(item.getUserId(), item.getTicker()));

        // 0-2. 보유 타임아웃 체크: 첫 매수 후 MAX_HOLDING_MINUTES 초과 시 강제 매도
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        for (TradingTarget item : new ArrayList<>(activeItems)) {
            User user = userMap.get(item.getUserId());
            if (user == null) continue;

            int holding = tradeLogPort.getHoldingCount(item.getUserId(), item.getTicker());
            if (holding <= 0) continue;

            ZonedDateTime openedAt = tradeLogPort.getPositionOpenedAt(item.getUserId(), item.getTicker());
            if (openedAt == null) continue;

            long minutes = Duration.between(openedAt, now).toMinutes();
            if (minutes >= MAX_HOLDING_MINUTES) {
                try {
                    BigDecimal currentPrice = brokerApiPort.getCurrentPrice(user, item.getTicker());
                    if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                        executeOrder(user, item, StockOrder.OrderType.SELL, currentPrice);
                        notificationPort.sendMessage(user.getUserId(),
                                String.format("[Timeout] %s force sold after %dmin", item.getTicker(), minutes));
                    }
                } catch (Exception e) {
                    log.error("[Timeout] {} force sell failed: {}", item.getTicker(), e.getMessage());
                }
            }
        }

        if (activeItems.isEmpty()) {
            log.info("[Cycle] ========== Trading Cycle End (all filtered) ==========");
            return;
        }

        // 1. 손절 체크
        List<TradingTarget> stopLossItems = executeStopLoss(activeItems, userMap);
        activeItems.removeAll(stopLossItems);
        if (activeItems.isEmpty()) {
            log.info("[Cycle] ========== Trading Cycle End (all stop-loss) ==========");
            return;
        }

        // 2. 캔들 fetch (1min + 5min — 한번만)
        Map<String, CandleData> candleCache = fetchAllCandles(activeItems, userMap);

        // 3. 트레일링스톱 (1min 캔들 사용)
        executeTrailingStop(activeItems, userMap, candleCache);

        // 4. AI 예측
        Map<String, AiModelPort.PredictionResult> predictions = fetchPredictions(activeItems, candleCache);

        // 5. 주문 실행 + 알림
        executeOrderByPrediction(activeItems, userMap, predictions, candleCache);

        log.info("[Cycle] ========== Trading Cycle End ==========");
    }

    public BrokerApiPort.OrderResult executeOrder(User user, TradingTarget item,
                                                    StockOrder.OrderType orderType, BigDecimal price) {
        int quantity;
        if (orderType == StockOrder.OrderType.BUY) {
            quantity = 1;
        } else {
            quantity = getSellQuantity(user, item);
            if (quantity <= 0) {
                return new BrokerApiPort.OrderResult(false, "No holdings to sell");
            }
        }

        StockOrder order = StockOrder.builder()
                .ticker(item.getTicker())
                .type(orderType)
                .quantity(quantity)
                .price(price)
                .build();

        log.info("[Order] {} {} {} {}x @ {}",
                user.getUsername(), item.getTicker(), orderType, quantity, price);

        BrokerApiPort.OrderResult result = brokerApiPort.sendOrder(user, order);

        if (result.success()) {
            // PENDING으로 저장 (orderId 포함)
            TradeLog tradeLog = TradeLog.createPending(
                    item.getUserId(), item.getTicker(), orderType, price, result.orderId());
            tradeLogPort.save(tradeLog);
        } else {
            // FAILED로 즉시 저장
            TradeLog tradeLog = TradeLog.createFailed(
                    item.getUserId(), item.getTicker(), orderType, price);
            tradeLogPort.save(tradeLog);
        }

        return result;
    }

    /**
     * PENDING 주문 확인/취소 처리
     */
    @Override
    @Transactional
    public void handlePendingOrders() {
        ZonedDateTime threshold = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(CANCEL_TIMEOUT_MINUTES);
        List<TradeLog> pendings = tradeLogPort.findPendingBefore(threshold);

        for (TradeLog pending : pendings) {
            User user = userPort.findById(pending.getUserId()).orElse(null);
            if (user == null) continue;

            if (pending.getOrderId() == null) {
                // orderId 없는 PENDING은 FAILED 처리
                tradeLogPort.updateStatus(pending.getId(), TradeLog.OrderStatus.FAILED);
                continue;
            }

            log.info("[PendingCheck] action: {} ticker: {} orderId: {}",
                    pending.getAction(), pending.getTicker(), pending.getOrderId());

            BrokerApiPort.CancelResult result = brokerApiPort.cancelOrder(user, pending.getOrderId());
            if (result.success()) {
                // 취소 성공
                tradeLogPort.updateStatus(pending.getId(), TradeLog.OrderStatus.CANCELLED);
                log.info("[PendingCheck] Cancelled: {}", pending.getOrderId());
            } else {
                // 취소 실패 = 이미 체결됨
                tradeLogPort.updateStatus(pending.getId(), TradeLog.OrderStatus.FILLED);
                log.info("[PendingCheck] Already filled: {}", pending.getOrderId());

                // SELL 체결 시 → 해당 ticker의 BUY FILLED 전부 → CLOSED
                if (pending.getAction() == StockOrder.OrderType.SELL) {
                    closeMatchingBuys(pending.getUserId(), pending.getTicker());
                }
            }
        }
    }

    // --- 내부 헬퍼 ---

    private void closeMatchingBuys(Long userId, String ticker) {
        List<TradeLog> filledBuys = tradeLogPort.findFilledBuys(userId, ticker);
        for (TradeLog buy : filledBuys) {
            tradeLogPort.updateStatus(buy.getId(), TradeLog.OrderStatus.CLOSED);
        }
        if (!filledBuys.isEmpty()) {
            log.info("[PendingCheck] Closed {} BUY records for {} {}", filledBuys.size(), userId, ticker);
        }
    }

    private void cleanupPendingOrders() {
        ZonedDateTime threshold = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(STALE_PENDING_MINUTES);
        List<TradeLog> stalePendings = tradeLogPort.findPendingBefore(threshold);

        for (TradeLog pending : stalePendings) {
            User user = userPort.findById(pending.getUserId()).orElse(null);
            if (user == null) {
                tradeLogPort.updateStatus(pending.getId(), TradeLog.OrderStatus.FAILED);
                continue;
            }
            if (pending.getOrderId() == null) {
                tradeLogPort.updateStatus(pending.getId(), TradeLog.OrderStatus.FAILED);
                continue;
            }

            BrokerApiPort.CancelResult result = brokerApiPort.cancelOrder(user, pending.getOrderId());
            if (result.success()) {
                tradeLogPort.updateStatus(pending.getId(), TradeLog.OrderStatus.CANCELLED);
                log.info("[Init] Stale PENDING cancelled: {}", pending.getOrderId());
            } else {
                tradeLogPort.updateStatus(pending.getId(), TradeLog.OrderStatus.FILLED);
                log.info("[Init] Stale PENDING already filled: {}", pending.getOrderId());

                if (pending.getAction() == StockOrder.OrderType.SELL) {
                    closeMatchingBuys(pending.getUserId(), pending.getTicker());
                }
            }
        }
    }

    private List<TradingTarget> filterByTradingHours(List<TradingTarget> items, Map<Long, User> userMap) {
        LocalTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalTime();

        return items.stream().filter(item -> {
            User user = userMap.get(item.getUserId());
            if (user == null) return false;

            LocalTime start = user.getTradingStartTime();
            LocalTime end = user.getTradingEndTime();
            if (start == null || end == null) return true;

            if (start.isAfter(end)) {
                return !now.isBefore(start) || !now.isAfter(end);
            }
            return !now.isBefore(start) && !now.isAfter(end);
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private Map<String, CandleData> fetchAllCandles(List<TradingTarget> items, Map<Long, User> userMap) {
        Map<String, User> tickerToUser = new LinkedHashMap<>();
        Set<String> predictionTickers = new HashSet<>();
        for (TradingTarget item : items) {
            User user = userMap.get(item.getUserId());
            if (user != null) {
                String predTicker = item.getPredictionTicker();
                tickerToUser.putIfAbsent(predTicker, user);
                predictionTickers.add(predTicker);
                tickerToUser.putIfAbsent(item.getTicker(), user);
            }
        }

        Map<String, CandleData> cache = new HashMap<>();

        for (Map.Entry<String, User> entry : tickerToUser.entrySet()) {
            String ticker = entry.getKey();
            User user = entry.getValue();
            boolean needsFiveMin = predictionTickers.contains(ticker);

            try {
                List<StockCandle> minute = brokerApiPort.getRecentCandles(user, ticker, 60);
                sleep(100);

                List<StockCandle> fiveMin = List.of();
                if (needsFiveMin) {
                    fiveMin = brokerApiPort.getRecentCandles5Min(user, ticker, 120);
                    sleep(100);
                    fiveMin = fiveMin != null ? fiveMin : List.of();
                }

                cache.put(ticker, new CandleData(
                        minute != null ? minute : List.of(), fiveMin));
            } catch (Exception e) {
                log.error("[Fetch] {} failed: {}", ticker, e.getMessage());
                cache.put(ticker, new CandleData(List.of(), List.of()));
            }
        }

        return cache;
    }

    private Map<String, AiModelPort.PredictionResult> fetchPredictions(List<TradingTarget> items,
                                                                        Map<String, CandleData> candleCache) {
        Map<String, List<TradingTarget>> grouped = new LinkedHashMap<>();
        for (TradingTarget item : items) {
            grouped.computeIfAbsent(item.getPredictionTicker(), k -> new ArrayList<>()).add(item);
        }

        Map<String, AiModelPort.PredictionResult> predictions = new HashMap<>();
        for (Map.Entry<String, List<TradingTarget>> entry : grouped.entrySet()) {
            String predTicker = entry.getKey();
            CandleData candles = candleCache.get(predTicker);

            if (candles == null || candles.minute().isEmpty() || candles.fiveMin().isEmpty()) {
                log.warn("[Predict] No candle data for {}", predTicker);
                predictions.put(predTicker, new AiModelPort.PredictionResult(0, 0.0, List.of(1.0, 0.0, 0.0)));
                continue;
            }

            TradingTarget firstItem = entry.getValue().get(0);
            try {
                AiModelPort.PredictionResult result = aiModelPort.predict(
                        predTicker, "scalping", firstItem.getUserId(),
                        candles.minute(), candles.fiveMin(),
                        firstItem.getBuyThreshold(), firstItem.getSellThreshold());
                predictions.put(predTicker, result);
            } catch (Exception e) {
                log.error("[Predict] Failed for {}: {}", predTicker, e.getMessage());
                predictions.put(predTicker, new AiModelPort.PredictionResult(0, 0.0, List.of(1.0, 0.0, 0.0)));
            }
        }

        log.info("[Predict] items={} uniqueTickers={}", items.size(), grouped.size());
        return predictions;
    }

    private void executeOrderByPrediction(List<TradingTarget> items,
                                          Map<Long, User> userMap,
                                          Map<String, AiModelPort.PredictionResult> predictions,
                                          Map<String, CandleData> candleCache) {
        for (TradingTarget item : items) {
            User user = userMap.get(item.getUserId());
            if (user == null) continue;

            String predTicker = item.getPredictionTicker();
            AiModelPort.PredictionResult rawResult = predictions.get(predTicker);
            if (rawResult == null || rawResult.prediction() == 0) continue;

            int prediction = item.applyInverse(rawResult.prediction());
            if (prediction == 0) continue;

            String label = prediction == 1 ? "BUY" : "SELL";
            StockOrder.OrderType orderType = prediction == 1
                    ? StockOrder.OrderType.BUY : StockOrder.OrderType.SELL;

            try {
                CandleData candles = candleCache.get(item.getTicker());
                BigDecimal price = BigDecimal.ZERO;
                if (candles != null && !candles.minute().isEmpty()) {
                    price = candles.minute().get(candles.minute().size() - 1).getClose();
                }

                if (price.compareTo(BigDecimal.ZERO) <= 0) continue;

                BrokerApiPort.OrderResult orderResult = executeOrder(user, item, orderType, price);
                if (orderResult.success()) {
                    String text = String.format("[Trading] %s %s (confidence: %.1f%%)", item.getTicker(), label, rawResult.confidence() * 100);
                    notificationPort.sendMessage(user.getUserId(), text);
                }
            } catch (Exception e) {
                log.error("[Order] Execution failed for {}: {}", item.getTicker(), e.getMessage());
            }
        }
    }

    private List<TradingTarget> executeStopLoss(List<TradingTarget> activeItems, Map<Long, User> userMap) {
        List<TradingTarget> stopLossItems = new ArrayList<>();

        for (TradingTarget item : activeItems) {
            User user = userMap.get(item.getUserId());
            if (user == null) continue;

            Asset asset = assetUseCase.getAccountAsset(user.getUserId());
            if (asset == null || asset.getOwnedStocks() == null) continue;

            Optional<Asset.OwnedStock> holdingOpt = asset.getOwnedStocks().stream()
                    .filter(s -> s.getStockCode().equals(item.getTicker()))
                    .findFirst();

            if (holdingOpt.isEmpty()) continue;

            Asset.OwnedStock holding = holdingOpt.get();
            BigDecimal profitRate = holding.getProfitRate();

            if (item.isStopLossTriggered(profitRate)) {
                log.warn("[StopLoss] {} triggered! P&L: {}% (threshold: {}%)",
                        item.getTicker(), profitRate, item.getStopLossThreshold());

                executeOrder(user, item, StockOrder.OrderType.SELL, holding.getCurrentPrice());
                tradingTargetPort.save(item.toBuilder().active(false).build());
                stopLossItems.add(item);

                log.warn("[StopLoss] {} auto-trading disabled", item.getTicker());
            }
        }

        return stopLossItems;
    }

    private void executeTrailingStop(List<TradingTarget> activeItems, Map<Long, User> userMap,
                                      Map<String, CandleData> candleCache) {
        for (TradingTarget item : activeItems) {
            if (!item.isTrailingStopEnabled()) continue;

            User user = userMap.get(item.getUserId());
            if (user == null) continue;

            Asset asset = assetUseCase.getAccountAsset(user.getUserId());
            if (asset == null || asset.getOwnedStocks() == null) continue;

            Optional<Asset.OwnedStock> holdingOpt = asset.getOwnedStocks().stream()
                    .filter(s -> s.getStockCode().equals(item.getTicker()))
                    .findFirst();

            if (holdingOpt.isEmpty()) continue;

            Asset.OwnedStock holding = holdingOpt.get();
            BigDecimal currentProfitRate = holding.getProfitRate();

            if (currentProfitRate.compareTo(BigDecimal.ZERO) <= 0) continue;

            CandleData data = candleCache.get(item.getTicker());
            List<StockCandle> candles = data != null ? data.minute() : null;
            if (candles == null || candles.isEmpty()) continue;

            BigDecimal windowHigh = StockCandle.windowHigh(candles, item.getTrailingWindowMinutes());
            BigDecimal currentPrice = holding.getCurrentPrice();

            if (item.isTrailingStopTriggered(windowHigh, currentPrice)) {
                log.warn("[TrailingStop] {} triggered! {}min high: {} -> current: {} (threshold: {}%)",
                        item.getTicker(), item.getTrailingWindowMinutes(),
                        windowHigh, currentPrice, item.getTrailingStopPercentage());

                executeOrder(user, item, StockOrder.OrderType.SELL, currentPrice);

                log.info("[TrailingStop] {} sold (P&L: {}%), auto-trading maintained",
                        item.getTicker(), currentProfitRate);
            }
        }
    }

    private int getSellQuantity(User user, TradingTarget item) {
        try {
            Asset asset = assetUseCase.getAccountAsset(user.getUserId());
            if (asset == null || asset.getOwnedStocks() == null) return 0;

            return asset.getOwnedStocks().stream()
                    .filter(s -> s.getStockCode().equals(item.getTicker()))
                    .findFirst()
                    .map(Asset.OwnedStock::getQuantity)
                    .orElse(0);
        } catch (Exception e) {
            log.error("[Quantity] Failed to get holdings: {}", e.getMessage());
            return 0;
        }
    }

    private void cleanupTraining() {
        List<TrainingHistory> trainingList = aiTrainingHistoryPort.findAll().stream()
                .filter(h -> h.getStatus() == TrainingStatus.TRAINING)
                .toList();

        for (TrainingHistory history : trainingList) {
            log.info("[Init] Marking stalled training as failed: {}", history.getTicker());
            aiTrainingHistoryPort.save(history.withStatus(TrainingStatus.FAILED, "server restart"));
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

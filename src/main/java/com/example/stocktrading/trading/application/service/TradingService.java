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
    public void executeRiskManagement() {
        List<TradingTarget> activeItems = new ArrayList<>(tradingTargetPort.findActiveItems());
        if (activeItems.isEmpty()) return;

        Map<Long, User> userMap = buildUserMap(activeItems);
        activeItems = filterByTradingHours(activeItems, userMap);
        if (activeItems.isEmpty()) return;

        log.info("[Risk] ========== Risk Management Start (Active: {}) ==========", activeItems.size());

        // 1. PENDING 주문 확인/취소
        handlePendingOrdersInternal();

        // 2. PENDING SELL 가드: 이미 매도 주문이 진행중인 ticker 제외 (중복 매도 방지)
        activeItems.removeIf(item ->
                tradeLogPort.hasPendingSell(item.getUserId(), item.getTicker()));
        if (activeItems.isEmpty()) {
            log.info("[Risk] ========== Risk Management End (all have pending sells) ==========");
            return;
        }

        // 3. 보유 타임아웃 강제매도
        executeTimeout(activeItems, userMap);

        // 4. 손절 체크 + 매도 (isActive=false 설정)
        List<TradingTarget> stopLossItems = executeStopLoss(activeItems, userMap);
        activeItems.removeAll(stopLossItems);

        // 5. 트레일링스톱 (1분봉)
        executeTrailingStop(activeItems, userMap);

        log.info("[Risk] ========== Risk Management End ==========");
    }

    @Override
    @Transactional
    public void executeAiTrading() {
        List<TradingTarget> activeItems = new ArrayList<>(tradingTargetPort.findActiveItems());
        if (activeItems.isEmpty()) return;

        Map<Long, User> userMap = buildUserMap(activeItems);
        activeItems = filterByTradingHours(activeItems, userMap);
        if (activeItems.isEmpty()) return;

        log.info("[AI] ========== AI Trading Start (Active: {}) ==========", activeItems.size());

        // 1. PENDING SELL 가드: PENDING SELL이 있는 ticker 제외
        activeItems.removeIf(item ->
                tradeLogPort.hasPendingSell(item.getUserId(), item.getTicker()));

        if (activeItems.isEmpty()) {
            log.info("[AI] ========== AI Trading End (all filtered) ==========");
            return;
        }

        // 2. 캔들 fetch (1min + 5min)
        Map<String, CandleData> candleCache = fetchAllCandles(activeItems, userMap);

        // 3. AI 예측
        Map<String, AiModelPort.PredictionResult> predictions = fetchPredictions(activeItems, candleCache);

        // 4. 주문 실행 + 알림
        executeOrderByPrediction(activeItems, userMap, predictions, candleCache);

        log.info("[AI] ========== AI Trading End ==========");
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

    private Map<Long, User> buildUserMap(List<TradingTarget> items) {
        return items.stream()
                .map(item -> userPort.findById(item.getUserId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getUserId, Function.identity(), (a, b) -> a));
    }

    /**
     * PENDING 주문 확인/취소 처리 (BUY/SELL 분리)
     */
    private void handlePendingOrdersInternal() {
        ZonedDateTime threshold = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(CANCEL_TIMEOUT_MINUTES);
        List<TradeLog> pendings = tradeLogPort.findPendingBefore(threshold);
        if (pendings.isEmpty()) return;

        handlePendingBuys(pendings.stream()
                .filter(p -> p.getAction() == StockOrder.OrderType.BUY).toList());
        handlePendingSells(pendings.stream()
                .filter(p -> p.getAction() == StockOrder.OrderType.SELL).toList());
    }

    /**
     * BUY PENDING 처리: 미체결이면 취소, 체결이면 FILLED
     */
    private void handlePendingBuys(List<TradeLog> pendingBuys) {
        for (TradeLog buy : pendingBuys) {
            if (buy.getOrderId() == null) {
                tradeLogPort.updateStatus(buy.getId(), TradeLog.OrderStatus.FAILED);
                continue;
            }

            User user = userPort.findById(buy.getUserId()).orElse(null);
            if (user == null) continue;

            log.info("[PendingBuy] {} orderId: {}", buy.getTicker(), buy.getOrderId());
            BrokerApiPort.CancelResult result = brokerApiPort.cancelOrder(user, buy.getOrderId());

            if (result.success()) {
                tradeLogPort.updateStatus(buy.getId(), TradeLog.OrderStatus.CANCELLED);
                log.info("[PendingBuy] 취소 성공: {}", buy.getOrderId());
            } else {
                tradeLogPort.updateStatus(buy.getId(), TradeLog.OrderStatus.FILLED);
                log.info("[PendingBuy] 체결 확인: {}", buy.getOrderId());
            }
        }
    }

    /**
     * SELL PENDING 처리: 중복 제거 → 보유 확인 → 체결 시 BUY FILLED→CLOSED
     */
    private void handlePendingSells(List<TradeLog> pendingSells) {
        if (pendingSells.isEmpty()) return;

        // 1. (userId, ticker) 기준 그룹핑
        Map<String, List<TradeLog>> groups = pendingSells.stream()
                .collect(Collectors.groupingBy(p -> p.getUserId() + ":" + p.getTicker()));

        for (List<TradeLog> group : groups.values()) {
            // 2. 시간순 정렬 → 가장 오래된 1개만 처리, 나머지 중복 취소
            group.sort(Comparator.comparing(TradeLog::getTimestamp));
            for (int i = 1; i < group.size(); i++) {
                TradeLog dup = group.get(i);
                tradeLogPort.updateStatus(dup.getId(), TradeLog.OrderStatus.CANCELLED);
                log.warn("[PendingSell] 중복 SELL 취소: {} id={}", dup.getTicker(), dup.getId());
            }

            TradeLog sell = group.get(0);

            if (sell.getOrderId() == null) {
                tradeLogPort.updateStatus(sell.getId(), TradeLog.OrderStatus.FAILED);
                continue;
            }

            User user = userPort.findById(sell.getUserId()).orElse(null);
            if (user == null) continue;

            // 3. 보유 수량 확인
            int holding = tradeLogPort.getHoldingCount(sell.getUserId(), sell.getTicker());

            if (holding <= 0) {
                // 보유 없음 → 브로커 취소 시도 후 CANCELLED (유령 SELL 방지)
                brokerApiPort.cancelOrder(user, sell.getOrderId());
                tradeLogPort.updateStatus(sell.getId(), TradeLog.OrderStatus.CANCELLED);
                log.warn("[PendingSell] 보유 없는 SELL 취소: {} id={}", sell.getTicker(), sell.getId());
                continue;
            }

            // 4. 보유 있음 → 브로커 취소 시도
            log.info("[PendingSell] {} orderId: {} holding: {}", sell.getTicker(), sell.getOrderId(), holding);
            BrokerApiPort.CancelResult result = brokerApiPort.cancelOrder(user, sell.getOrderId());

            if (result.success()) {
                // 취소 성공 → 아직 보유 중
                tradeLogPort.updateStatus(sell.getId(), TradeLog.OrderStatus.CANCELLED);
                log.info("[PendingSell] 취소 성공: {}", sell.getOrderId());
            } else {
                // 취소 실패 = 이미 체결됨 → BUY FILLED → CLOSED
                tradeLogPort.updateStatus(sell.getId(), TradeLog.OrderStatus.FILLED);
                closeMatchingBuys(sell.getUserId(), sell.getTicker());
                log.info("[PendingSell] 체결 확인: {} → BUY {}건 CLOSED", sell.getOrderId(), holding);
            }
        }
    }

    private void executeTimeout(List<TradingTarget> activeItems, Map<Long, User> userMap) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

        for (TradingTarget item : activeItems) {
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

    private void executeTrailingStop(List<TradingTarget> activeItems, Map<Long, User> userMap) {
        for (TradingTarget item : activeItems) {
            if (!item.isTrailingStopEnabled()) continue;

            User user = userMap.get(item.getUserId());
            if (user == null) continue;

            try {
                Asset asset = assetUseCase.getAccountAsset(user.getUserId());
                if (asset == null || asset.getOwnedStocks() == null) continue;

                Optional<Asset.OwnedStock> holdingOpt = asset.getOwnedStocks().stream()
                        .filter(s -> s.getStockCode().equals(item.getTicker()))
                        .findFirst();

                if (holdingOpt.isEmpty()) continue;

                Asset.OwnedStock holding = holdingOpt.get();
                BigDecimal currentProfitRate = holding.getProfitRate();

                if (currentProfitRate.compareTo(BigDecimal.ZERO) <= 0) continue;

                List<StockCandle> candles = brokerApiPort.getRecentCandles(user, item.getTicker(), 60);
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
            } catch (Exception e) {
                log.error("[TrailingStop] {} failed: {}", item.getTicker(), e.getMessage());
            }
        }
    }

    private Map<String, CandleData> fetchAllCandles(List<TradingTarget> items, Map<Long, User> userMap) {
        Map<String, User> tickerToUser = new LinkedHashMap<>();
        for (TradingTarget item : items) {
            User user = userMap.get(item.getUserId());
            if (user != null) {
                String predTicker = item.getPredictionTicker();
                tickerToUser.putIfAbsent(predTicker, user);
                tickerToUser.putIfAbsent(item.getTicker(), user);
            }
        }

        Map<String, CandleData> cache = new HashMap<>();

        for (Map.Entry<String, User> entry : tickerToUser.entrySet()) {
            String ticker = entry.getKey();
            User user = entry.getValue();

            try {
                List<StockCandle> minute = brokerApiPort.getRecentCandles(user, ticker, 60);
                minute = minute != null ? minute : List.of();
                sleep(100);
                List<StockCandle> fiveMin = brokerApiPort.getRecentCandles5Min(user, ticker, 120);
                fiveMin = fiveMin != null ? fiveMin : List.of();
                sleep(100);

                cache.put(ticker, new CandleData(minute, fiveMin));
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

    private void closeMatchingBuys(Long userId, String ticker) {
        List<TradeLog> filledBuys = tradeLogPort.findFilledBuys(userId, ticker);
        for (TradeLog buy : filledBuys) {
            tradeLogPort.updateStatus(buy.getId(), TradeLog.OrderStatus.CLOSED);
        }
        if (!filledBuys.isEmpty()) {
            log.info("[PendingSell] Closed {} BUY records for {} {}", filledBuys.size(), userId, ticker);
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

    private void cleanupPendingOrders() {
        ZonedDateTime threshold = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(STALE_PENDING_MINUTES);
        List<TradeLog> stalePendings = tradeLogPort.findPendingBefore(threshold);
        if (stalePendings.isEmpty()) return;

        log.info("[Init] Stale PENDING 정리: {}건", stalePendings.size());
        handlePendingBuys(stalePendings.stream()
                .filter(p -> p.getAction() == StockOrder.OrderType.BUY).toList());
        handlePendingSells(stalePendings.stream()
                .filter(p -> p.getAction() == StockOrder.OrderType.SELL).toList());
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

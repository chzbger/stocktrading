package com.example.stocktrading.trading.application.service;

import com.example.stocktrading.trading.application.port.in.AssetUseCase;
import com.example.stocktrading.trading.application.port.in.TradingUseCase;
import com.example.stocktrading.trading.application.port.out.*;
import com.example.stocktrading.trading.domain.*;
import com.example.stocktrading.user.application.port.out.NotificationPort;
import com.example.stocktrading.user.application.port.out.UserPort;
import com.example.stocktrading.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final BrokerApiPort brokerApiPort;
    private final AssetUseCase assetUseCase;
    private final AiModelPort aiModelPort;
    private final NotificationPort notificationPort;

    private static final int PENDING_TIMEOUT_MINUTES = 2;
    private static final int MAX_HOLDING_MINUTES = 25;

    private record CandleData(List<StockCandle> minute, List<StockCandle> fiveMin) {}

    @Override
    public void initialize() {
        handlePendingOrder();
    }

    @Override
    public void executeRiskManagement() {
        List<TradingTarget> activeItems = new ArrayList<>(tradingTargetPort.findActiveItems());
        if (activeItems.isEmpty()) return;

        Map<Long, User> userMap = buildUserMap(activeItems);
        activeItems = filterByTradingHours(activeItems, userMap);
        if (activeItems.isEmpty()) return;

        log.info("[Risk] ========== Risk Management Start (Active: {}) ==========", activeItems.size());

        // 1. 오래된 PENDING 취소
        handlePendingOrder();

        // 2. 이미 매도 주문이 진행중인 ticker 제외 (중복 매도 방지)
        activeItems.removeIf(item -> tradeLogPort.hasPendingSell(item.getUserId(), item.getTicker()));
        if (activeItems.isEmpty()) {
            log.info("[Risk] ========== Risk Management End (all have pending sells) ==========");
            return;
        }

        // 3. 보유 타임아웃 강제매도
        executeTimeout(activeItems, userMap);

        // 4. 손절 체크 + 매도
        executeStopLoss(activeItems, userMap);

        // 5. 트레일링스톱 (1분봉)
        executeTrailingStop(activeItems, userMap);

        log.info("[Risk] ========== Risk Management End ==========");
    }

    @Override
    public void executeAiTrading() {
        List<TradingTarget> activeItems = new ArrayList<>(tradingTargetPort.findActiveItems());
        if (activeItems.isEmpty()) return;

        Map<Long, User> userMap = buildUserMap(activeItems);
        activeItems = filterByTradingHours(activeItems, userMap);
        if (activeItems.isEmpty()) return;

        log.info("[AI] ========== AI Trading Start (Active: {}) ==========", activeItems.size());

        // 1. 캔들 fetch (1min + 5min)
        Map<String, CandleData> candleCache = fetchAllCandles(activeItems, userMap);

        // 2. AI 예측
        Map<String, AiModelPort.PredictionResult> predictions = fetchPredictions(activeItems, candleCache);

        // 3. 주문 실행 + 알림
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
            TradeLog tradeLog = TradeLog.createPending(
                    item.getUserId(), item.getTicker(), orderType, price, result.orderId());
            tradeLogPort.save(tradeLog);
        } else {
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
    public void handlePendingOrder() {
        ZonedDateTime threshold = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).minusMinutes(PENDING_TIMEOUT_MINUTES);
        List<TradeLog> expiredPendings = tradeLogPort.findPendingBefore(threshold);
        if (expiredPendings.isEmpty()) return;

        for (TradeLog expiredPending : expiredPendings) {
            if (expiredPending.getAction() == StockOrder.OrderType.BUY) {
                handlePendingBuy(expiredPending);
            } else if (expiredPending.getAction() == StockOrder.OrderType.SELL) {
                handlePendingSell(expiredPending);
            }
        }
    }

    /**
     * BUY PENDING 처리: 미체결이면 취소, 체결이면 FILLED
     */
    public void handlePendingBuy(TradeLog pendingBuy) {
        try {
            User user = userPort.findById(pendingBuy.getUserId()).orElse(null);
            if (pendingBuy.getOrderId() == null || user == null) {
                tradeLogPort.updateStatus(pendingBuy.getId(), TradeLog.OrderStatus.FAILED);
                return;
            }

            log.info("[PendingBuy] {} orderId: {}", pendingBuy.getTicker(), pendingBuy.getOrderId());
            BrokerApiPort.CancelResult result = brokerApiPort.cancelOrder(user, pendingBuy.getOrderId());
            if (result.success()) {
                tradeLogPort.updateStatus(pendingBuy.getId(), TradeLog.OrderStatus.CANCELLED);
                log.info("[PendingBuy] 취소 성공: {}", pendingBuy.getOrderId());
            } else {
                tradeLogPort.updateStatus(pendingBuy.getId(), TradeLog.OrderStatus.FILLED);
                log.info("[PendingBuy] 체결 확인: {}", pendingBuy.getOrderId());
            }
        } catch (Exception e) {
            log.error("[PendingBuy] {} id={} failed: {}", pendingBuy.getTicker(), pendingBuy.getId(), e.getMessage());
        }
    }

    /**
     * SELL PENDING 처리: 중복 제거 → 보유 확인 → 체결 시 BUY FILLED→CLOSED
     */
    public void handlePendingSell(TradeLog pendingSell) {
        try {
            User user = userPort.findById(pendingSell.getUserId()).orElse(null);
            if (pendingSell.getOrderId() == null || user == null) {
                tradeLogPort.updateStatus(pendingSell.getId(), TradeLog.OrderStatus.FAILED);
                return;
            }
            log.info("[PendingSell] {} orderId: {}", pendingSell.getTicker(), pendingSell.getOrderId());
            BrokerApiPort.CancelResult result = brokerApiPort.cancelOrder(user, pendingSell.getOrderId());
            if (result.success()) {
                tradeLogPort.updateStatus(pendingSell.getId(), TradeLog.OrderStatus.CANCELLED);
                log.info("[PendingSell] 취소 성공: {}", pendingSell.getOrderId());
                return;
            }

            tradeLogPort.updateStatus(pendingSell.getId(), TradeLog.OrderStatus.FILLED);
            log.info("[PendingSell] 체결 확인: {}", pendingSell.getOrderId());

            // SELL id 이전의 FILLED BUY만 CLOSED
            int closed = tradeLogPort.closeFilledBuysBefore(pendingSell.getUserId(), pendingSell.getTicker(), pendingSell.getId());
            log.info("[PendingSell] Closed {} BUY Ticker({}) {}", closed, pendingSell.getTicker(), pendingSell.getUserId());

            notificationPort.sendMessage(user.getUserId(), String.format("[PendingSell] %s ", pendingSell.getTicker()));
        } catch (Exception e) {
            log.error("[PendingSell] {} id={} failed: {}", pendingSell.getTicker(), pendingSell.getId(), e.getMessage());
        }
    }

    private void executeTimeout(List<TradingTarget> activeItems, Map<Long, User> userMap) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        for (TradingTarget item : new ArrayList<>(activeItems)) {
            try {
                User user = userMap.get(item.getUserId());
                if (user == null) continue;
                int holding = tradeLogPort.getHoldingCount(item.getUserId(), item.getTicker());
                if (holding <= 0) continue;
                ZonedDateTime openedAt = tradeLogPort.getPositionOpenedAt(item.getUserId(), item.getTicker());
                if (openedAt == null) continue;
                long minutes = Duration.between(openedAt, now).toMinutes();
                if (minutes < MAX_HOLDING_MINUTES) continue;

                BigDecimal currentPrice = brokerApiPort.getCurrentPrice(user, item.getTicker());
                if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                    executeOrder(user, item, StockOrder.OrderType.SELL, currentPrice);
                    activeItems.remove(item);

                    notificationPort.sendMessage(user.getUserId(), String.format("[Timeout] %s SELL Order (%d min)", item.getTicker(), minutes));
                }
            } catch (Exception e) {
                log.error("[Timeout] {} force sell failed: {}", item.getTicker(), e.getMessage());
            }
        }
    }

    private void executeStopLoss(List<TradingTarget> activeItems, Map<Long, User> userMap) {
        for (TradingTarget item : new ArrayList<>(activeItems)) {
            try {
                User user = userMap.get(item.getUserId());
                if (user == null) continue;
                Asset asset = assetUseCase.getAccountAsset(user.getUserId());
                if (asset == null || asset.getOwnedStocks() == null) continue;

                Asset.OwnedStock holding = asset.getOwnedStocks().stream()
                        .filter(s -> s.getStockCode().equals(item.getTicker()))
                        .findFirst()
                        .orElse(null);
                if (holding == null) continue;

                BigDecimal profitRate = holding.getProfitRate();

                if (item.isStopLossTriggered(profitRate)) {
                    executeOrder(user, item, StockOrder.OrderType.SELL, holding.getCurrentPrice());
                    activeItems.remove(item);

                    notificationPort.sendMessage(user.getUserId(), String.format("[StopLoss] %s 손절 매도 (P&L: %s%%)", item.getTicker(), profitRate));
                }
            } catch (Exception e) {
                log.error("[StopLoss] {} failed: {}", item.getTicker(), e.getMessage());
            }
        }
    }

    private void executeTrailingStop(List<TradingTarget> activeItems, Map<Long, User> userMap) {
        for (TradingTarget item : activeItems) {
            try {
                if (!item.isTrailingStopEnabled()) continue;
                User user = userMap.get(item.getUserId());
                if (user == null) continue;
                Asset asset = assetUseCase.getAccountAsset(user.getUserId());
                if (asset == null || asset.getOwnedStocks() == null) continue;

                Asset.OwnedStock holding = asset.getOwnedStocks().stream()
                        .filter(s -> s.getStockCode().equals(item.getTicker()))
                        .findFirst()
                        .orElse(null);
                if (holding == null) continue;

                BigDecimal currentProfitRate = holding.getProfitRate();
                if (currentProfitRate.compareTo(BigDecimal.ZERO) <= 0) continue;

                List<StockCandle> candles = brokerApiPort.getRecentCandles(user, item.getTicker(), 60);
                if (candles == null || candles.isEmpty()) continue;

                BigDecimal windowHigh = StockCandle.windowHigh(candles, item.getTrailingWindowMinutes());
                BigDecimal currentPrice = holding.getCurrentPrice();

                if (item.isTrailingStopTriggered(windowHigh, currentPrice)) {
                    executeOrder(user, item, StockOrder.OrderType.SELL, currentPrice);

                    notificationPort.sendMessage(user.getUserId(), String.format("[TrailingStop] %s", item.getTicker()));
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
                List<StockCandle> minute = brokerApiPort.getRecentCandles(user, ticker, 200);
                minute = minute != null ? minute : List.of();
                sleep(100);
                List<StockCandle> fiveMin = brokerApiPort.getRecentCandles5Min(user, ticker, 300);
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

            try {
                TradingTarget firstItem = entry.getValue().getFirst();
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
            try {
                User user = userMap.get(item.getUserId());
                if (user == null) continue;

                String predTicker = item.getPredictionTicker();
                AiModelPort.PredictionResult rawResult = predictions.get(predTicker);
                if (rawResult == null || rawResult.prediction() == 0) continue;

                int prediction = item.applyInverse(rawResult.prediction());
                if (prediction == 0) continue;

                String label = prediction == 1 ? "BUY" : "SELL";
                StockOrder.OrderType orderType = prediction == 1 ? StockOrder.OrderType.BUY : StockOrder.OrderType.SELL;

                CandleData candles = candleCache.get(item.getTicker());
                BigDecimal price = BigDecimal.ZERO;
                if (candles != null && !candles.minute().isEmpty()) {
                    price = candles.minute().getLast().getClose();
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

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

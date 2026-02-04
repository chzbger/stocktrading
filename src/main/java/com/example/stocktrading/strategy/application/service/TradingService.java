package com.example.stocktrading.strategy.application.service;

import com.example.stocktrading.auth.application.port.out.UserPort;
import com.example.stocktrading.auth.domain.User;
import com.example.stocktrading.strategy.application.port.in.AssetUseCase;
import com.example.stocktrading.strategy.application.port.in.TradingUseCase;
import com.example.stocktrading.strategy.application.port.out.*;
import com.example.stocktrading.strategy.application.port.out.AiTrainingHistoryPort.TrainingHistory;
import com.example.stocktrading.strategy.application.port.out.AiTrainingHistoryPort.TrainingStatus;
import com.example.stocktrading.strategy.application.port.out.WatchlistPort.WatchlistItem;
import com.example.stocktrading.strategy.domain.Asset;
import com.example.stocktrading.strategy.domain.StockCandle;
import com.example.stocktrading.strategy.domain.StockOrder;
import com.example.stocktrading.strategy.domain.TradeLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TradingService implements TradingUseCase {

    private final UserPort userPort;
    private final TradeLogPort tradeLogPort;
    private final WatchlistPort watchlistPort;
    private final AiTrainingHistoryPort aiTrainingHistoryPort;
    private final BrokerApiPort brokerApiPort;
    private final AiModelPort aiModelPort;
    private final AssetUseCase assetUseCase;

    record CandleData(
            Map<String, List<StockCandle>> minuteCandles,
            Map<String, List<StockCandle>> fiveMinCandles,
            Map<String, BigDecimal> currentPrices
    ) {}

    record TradeSignal(
            WatchlistItem item,
            User user,
            StockOrder.OrderType orderType,
            BigDecimal price,
            int quantity
    ) {}

    @Override
    public void initialize() {
        cleanupStalledTraining();
    }

    @Override
    public void executeTradingCycle() {
        log.info("[Trading] ========== Cycle Start ==========");
        List<WatchlistItem> activeItems = new ArrayList<>(watchlistPort.findActiveItems());
        if (activeItems.isEmpty()) {
            log.info("[Trading] ========== Cycle End (no active) ==========");
            return;
        }

        Map<Long, User> userMap = activeItems.stream()
                .map(item -> userPort.findById(item.userId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getUserId, u -> u, (a, b) -> a));

        log.info("[Trading] ========== Cycle Start (Active: {}) ==========", activeItems.size());

        // 1. 손절 체크 및 실행
        List<WatchlistItem> stopLossItems = executeStopLoss(activeItems, userMap);
        activeItems.removeAll(stopLossItems);

        if (activeItems.isEmpty()) {
            log.info("[Trading] ========== Cycle End (전부 손절) ==========");
            return;
        }

        // 2. 캔들 데이터 fetch
        CandleData candles = fetchCandles(activeItems, userMap);

        // 3. AI 예측 → 매매 신호 생성
        List<TradeSignal> signals = generateSignals(activeItems, candles, userMap);

        // 4. 주문 실행
        executeOrders(signals);

        log.info("[Trading] ========== Cycle End ==========");
    }

    private List<WatchlistItem> executeStopLoss(List<WatchlistItem> activeItems, Map<Long, User> userMap) {
        List<WatchlistItem> stopLossItems = new ArrayList<>();

        for (WatchlistItem item : activeItems) {
            User user = userMap.get(item.userId());
            if (user == null) continue;

            Asset asset = assetUseCase.getAccountAsset(user.getUserId());
            if (asset == null || asset.getOwnedStocks() == null) continue;

            Optional<Asset.OwnedStock> holdingOpt = asset.getOwnedStocks().stream()
                    .filter(s -> s.getStockCode().equals(item.ticker()))
                    .findFirst();

            if (holdingOpt.isEmpty()) continue;

            Asset.OwnedStock holding = holdingOpt.get();
            BigDecimal profitRate = holding.getProfitRate();
            BigDecimal stopLossThreshold = new BigDecimal(
                    Optional.ofNullable(item.stopLossPercentage()).orElse("3.0")
            ).negate();

            if (profitRate.compareTo(stopLossThreshold) <= 0) {
                log.warn("[StopLoss] {} 손절 발동! 수익률: {}% (기준: {}%)",
                        item.ticker(), profitRate, stopLossThreshold);

                // 전량 매도
                StockOrder order = StockOrder.builder()
                        .ticker(item.ticker())
                        .type(StockOrder.OrderType.SELL)
                        .quantity(holding.getQuantity())
                        .price(holding.getCurrentPrice())
                        .build();

                brokerApiPort.sendOrder(user, order);
                tradeLogPort.save(TradeLog.create(user.getUserId(), item.ticker(), StockOrder.OrderType.SELL, holding.getCurrentPrice()));

                // 자동매매 비활성화
                watchlistPort.save(item.withActive(false));
                stopLossItems.add(item);

                log.warn("[StopLoss] {} 자동매매 비활성화", item.ticker());
            }
        }

        return stopLossItems;
    }

    private CandleData fetchCandles(List<WatchlistItem> activeItems, Map<Long, User> userMap) {
        // ticker → User 매핑
        Map<String, User> tickerToUser = new HashMap<>();
        for (WatchlistItem item : activeItems) {
            User user = userMap.get(item.userId());
            if (user != null) {
                tickerToUser.putIfAbsent(item.ticker(), user);
                tickerToUser.putIfAbsent(item.getPredictionTicker(), user);
            }
        }

        Map<String, List<StockCandle>> minuteCandles = new HashMap<>();
        Map<String, List<StockCandle>> fiveMinCandles = new HashMap<>();
        Map<String, BigDecimal> currentPrices = new HashMap<>();

        for (Map.Entry<String, User> entry : tickerToUser.entrySet()) {
            String ticker = entry.getKey();
            User user = entry.getValue();

            try {
                // 1분봉 (60개)
                List<StockCandle> minute = brokerApiPort.getRecentCandles(user, ticker, 60);
                if (minute != null && !minute.isEmpty()) {
                    minuteCandles.put(ticker, minute);
                    currentPrices.put(ticker, minute.get(minute.size() - 1).getClose());
                }

                sleep(100);

                // 5분봉 (120개)
                List<StockCandle> fiveMin = brokerApiPort.getRecentCandles5Min(user, ticker, 120);
                if (fiveMin != null && !fiveMin.isEmpty()) {
                    fiveMinCandles.put(ticker, fiveMin);
                }

                sleep(100);
            } catch (Exception e) {
                log.error("[Fetch] {} 실패: {}", ticker, e.getMessage());
            }
        }

        return new CandleData(minuteCandles, fiveMinCandles, currentPrices);
    }

    private List<TradeSignal> generateSignals(List<WatchlistItem> activeItems, CandleData candles, Map<Long, User> userMap) {
        List<TradeSignal> signals = new ArrayList<>();

        // ticker별로 그룹핑
        Map<String, List<WatchlistItem>> tickerToItems = new HashMap<>();
        for (WatchlistItem item : activeItems) {
            tickerToItems.computeIfAbsent(item.ticker(), k -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<WatchlistItem>> entry : tickerToItems.entrySet()) {
            String ticker = entry.getKey();
            List<WatchlistItem> items = entry.getValue();

            BigDecimal currentPrice = candles.currentPrices().get(ticker);
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) == 0) {
                log.warn("[Signal] {} - 가격 데이터 없음", ticker);
                continue;
            }

            // 각 사용자별 AI 예측 및 신호 생성
            for (WatchlistItem item : items) {
                User user = userMap.get(item.userId());
                if (user == null || !isMarketOpenForUser(user)) continue;

                int prediction = predictWithAI(ticker, item, candles);
                if (prediction == 0) continue; // HOLD

                StockOrder.OrderType orderType = (prediction == 1) ? StockOrder.OrderType.BUY : StockOrder.OrderType.SELL;

                int quantity = calculateQuantity(user, item, orderType, currentPrice);
                if (quantity <= 0) continue;

                signals.add(new TradeSignal(item, user, orderType, currentPrice, quantity));
            }
        }

        return signals;
    }

    private int predictWithAI(String ticker, WatchlistItem item, CandleData candles) {
        String predictionTicker = item.getPredictionTicker();
        boolean isInverse = item.isInverseOrDefault();

        List<StockCandle> minuteHistory = candles.minuteCandles().get(predictionTicker);
        List<StockCandle> fiveMinHistory = candles.fiveMinCandles().get(predictionTicker);

        // baseTicker 데이터 없으면 원래 ticker 사용
        if ((minuteHistory == null || minuteHistory.isEmpty()) && !predictionTicker.equals(ticker)) {
            minuteHistory = candles.minuteCandles().get(ticker);
            fiveMinHistory = candles.fiveMinCandles().get(ticker);
            predictionTicker = ticker;
            isInverse = false;
        }

        if (minuteHistory == null || minuteHistory.isEmpty()) {
            log.warn("[AI] {} - 분봉 데이터 없음", ticker);
            return 0;
        }

        int buyThreshold = Optional.ofNullable(item.buyThreshold()).orElse(60);
        int sellThreshold = Optional.ofNullable(item.sellThreshold()).orElse(60);

        int rawPrediction = aiModelPort.predict(predictionTicker, minuteHistory, fiveMinHistory, buyThreshold, sellThreshold);
        int prediction = applyInverseLogic(rawPrediction, isInverse);

        // 트렌드 필터
        List<StockCandle> tickerMinuteHistory = candles.minuteCandles().get(ticker);
        int currentTrend = detectCurrentTrend(tickerMinuteHistory);

        if (!isTrendAligned(prediction, currentTrend)) {
            log.info("[AI] {} {} 차단 - {} 트렌드와 반대", ticker, predictionToString(prediction), trendToString(currentTrend));
            return 0;
        }

        log.info("[AI] {} 예측: {} (B={}, S={}, Trend={})",
                ticker, predictionToString(prediction), buyThreshold, sellThreshold, trendToString(currentTrend));

        return prediction;
    }

    private int calculateQuantity(User user, WatchlistItem item, StockOrder.OrderType orderType, BigDecimal price) {
        if (orderType == StockOrder.OrderType.BUY) {
            return 1; // 매수는 1주
        }

        // 매도는 전량
        try {
            Asset asset = assetUseCase.getAccountAsset(user.getUserId());
            if (asset == null || asset.getOwnedStocks() == null) return 0;

            return asset.getOwnedStocks().stream()
                    .filter(s -> s.getStockCode().equals(item.ticker()))
                    .findFirst()
                    .map(Asset.OwnedStock::getQuantity)
                    .orElse(0);
        } catch (Exception e) {
            log.error("[Quantity] 보유수량 조회 실패: {}", e.getMessage());
            return 0;
        }
    }

    private void executeOrders(List<TradeSignal> signals) {
        for (TradeSignal signal : signals) {
            try {
                log.info("[Order] {} {} {} {}주 @ {}",
                        signal.user().getUsername(), signal.item().ticker(),
                        signal.orderType(), signal.quantity(), signal.price());

                StockOrder order = StockOrder.builder()
                        .ticker(signal.item().ticker())
                        .type(signal.orderType())
                        .quantity(signal.quantity())
                        .price(signal.price())
                        .build();

                BrokerApiPort.OrderResult result = brokerApiPort.sendOrder(signal.user(), order);

                TradeLog tradeLog = TradeLog.createWithStatus(
                        signal.item().userId(), signal.item().ticker(),
                        signal.orderType(), signal.price(), result.status());
                tradeLogPort.save(tradeLog);

                if (!result.success()) {
                    log.warn("[Order] 실패 {}: {} - {}", signal.user().getUsername(), result.status(), result.message());
                }
            } catch (Exception e) {
                log.error("[Order] 예외 {}: {}", signal.item().ticker(), e.getMessage());
            }
        }
    }

    private void cleanupStalledTraining() {
        List<TrainingHistory> stale = aiTrainingHistoryPort.findAll().stream()
                .filter(h -> h.status() == TrainingStatus.TRAINING)
                .toList();

        for (TrainingHistory history : stale) {
            log.info("[Init] 학습중 상태 실패처리: {}", history.ticker());
            aiTrainingHistoryPort.save(history.withStatus(TrainingStatus.FAILED, "server restart"));
        }
    }

    private boolean isMarketOpenForUser(User user) {
        LocalTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalTime();
        LocalTime start = user.getTradingStartTime();
        LocalTime end = user.getTradingEndTime();

        if (start == null || end == null) return true;

        if (start.isAfter(end)) {
            // 야간 거래 (예: 22:00 ~ 06:00)
            return !now.isBefore(start) || now.isBefore(end);
        } else {
            // 주간 거래 (예: 09:00 ~ 15:30)
            return !now.isBefore(start) && now.isBefore(end);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static final int TREND_LOOKBACK_CANDLES = 3;
    private static final double TREND_MIN_CHANGE_PERCENT = 0.05;

    private int applyInverseLogic(int prediction, boolean isInverse) {
        if (!isInverse) return prediction;
        return switch (prediction) {
            case 1 -> 2;
            case 2 -> 1;
            default -> prediction;
        };
    }

    private int detectCurrentTrend(List<StockCandle> candles) {
        if (candles == null || candles.size() < TREND_LOOKBACK_CANDLES) return 0;

        int size = candles.size();
        List<StockCandle> recent = candles.subList(size - TREND_LOOKBACK_CANDLES, size);

        int bullish = 0, bearish = 0;
        for (StockCandle c : recent) {
            BigDecimal body = c.getClose().subtract(c.getOpen());
            if (body.compareTo(BigDecimal.ZERO) > 0) bullish++;
            else if (body.compareTo(BigDecimal.ZERO) < 0) bearish++;
        }

        BigDecimal startPrice = recent.getFirst().getOpen();
        BigDecimal endPrice = recent.getLast().getClose();
        double change = endPrice.subtract(startPrice)
                .divide(startPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        if (bullish >= 2 && change > TREND_MIN_CHANGE_PERCENT) return 1;
        if (bearish >= 2 && change < -TREND_MIN_CHANGE_PERCENT) return -1;
        return 0;
    }

    private boolean isTrendAligned(int prediction, int trend) {
        if (prediction == 0 || trend == 0) return true;
        return (prediction == 1 && trend == 1) || (prediction == 2 && trend == -1);
    }

    private String trendToString(int trend) {
        return switch (trend) {
            case 1 -> "BULLISH";
            case -1 -> "BEARISH";
            default -> "NEUTRAL";
        };
    }

    private String predictionToString(int prediction) {
        return switch (prediction) {
            case 1 -> "BUY";
            case 2 -> "SELL";
            default -> "HOLD";
        };
    }
}

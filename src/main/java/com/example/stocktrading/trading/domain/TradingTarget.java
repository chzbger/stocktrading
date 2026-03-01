package com.example.stocktrading.trading.domain;

import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TradingTarget {
    private Long id;
    private Long userId;
    private String ticker;
    @Builder.Default
    private boolean active = false;
    @Builder.Default
    private int buyThreshold = 10;
    @Builder.Default
    private int sellThreshold = 10;
    @Builder.Default
    private BigDecimal stopLossPercentage = new BigDecimal("3.0");
    private String baseTicker;
    @Builder.Default
    private boolean inverse = false;
    @Builder.Default
    private BigDecimal trailingStopPercentage = new BigDecimal("2.0");
    @Builder.Default
    private boolean trailingStopEnabled = true;
    @Builder.Default
    private int trailingWindowMinutes = 10;
    private Long brokerId;
    @Builder.Default
    private double profitAtr = 0.6;
    @Builder.Default
    private double stopAtr = 0.4;
    @Builder.Default
    private int maxHolding = 5;
    @Builder.Default
    private double minThreshold = 0.2;
    @Builder.Default
    private int trainingPeriodYears = 4;
    @Builder.Default
    private int tuningTrials = 30;

    public static TradingTarget ofCreate(Long userId, String ticker, Long brokerId) {
        return TradingTarget.builder()
                .userId(userId)
                .ticker(ticker)
                .brokerId(brokerId)
                .build();
    }

    public String getPredictionTicker() {
        return baseTicker != null && !baseTicker.isBlank() ? baseTicker : ticker;
    }

    public BigDecimal getStopLossThreshold() {
        return stopLossPercentage.negate();
    }

    public boolean isStopLossTriggered(BigDecimal profitRate) {
        return profitRate.compareTo(getStopLossThreshold()) <= 0;
    }

    public boolean isTrailingStopTriggered(BigDecimal windowHigh, BigDecimal currentPrice) {
        if (!trailingStopEnabled) return false;
        if (windowHigh.compareTo(BigDecimal.ZERO) == 0) return false;

        BigDecimal dropPercent = windowHigh.subtract(currentPrice)
                .divide(windowHigh, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return dropPercent.compareTo(trailingStopPercentage) >= 0;
    }

    public int applyInverse(int prediction) {
        if (inverse && prediction != 0) {
            return prediction == 1 ? 2 : 1;
        }
        return prediction;
    }
}

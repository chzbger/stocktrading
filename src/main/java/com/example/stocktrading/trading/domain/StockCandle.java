package com.example.stocktrading.trading.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StockCandle {
    private ZonedDateTime timestamp;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;

    public static BigDecimal windowHigh(List<StockCandle> candles, int windowMinutes) {
        if (candles == null || candles.isEmpty()) return BigDecimal.ZERO;

        int startIndex = Math.max(0, candles.size() - windowMinutes);
        BigDecimal high = BigDecimal.ZERO;

        for (int i = startIndex; i < candles.size(); i++) {
            BigDecimal candleHigh = candles.get(i).getHigh();
            if (candleHigh.compareTo(high) > 0) {
                high = candleHigh;
            }
        }

        return high;
    }
}

package com.example.stocktrading.trading.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TradeLog {

    private Long id;
    private Long userId;
    private String ticker;
    private StockOrder.OrderType action;
    private BigDecimal price;
    private Integer profitRate;
    private ZonedDateTime timestamp;
    private String orderId;
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    public enum OrderStatus {
        PENDING,
        FILLED,
        CLOSED,
        CANCELLED,
        FAILED
    }

    public static TradeLog createPending(Long userId, String ticker, StockOrder.OrderType action,
                                         BigDecimal price, String orderId) {
        return TradeLog.builder()
                .userId(userId)
                .ticker(ticker)
                .action(action)
                .price(price)
                .orderId(orderId)
                .status(OrderStatus.PENDING)
                .timestamp(ZonedDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
    }

    public static TradeLog createFailed(Long userId, String ticker, StockOrder.OrderType action,
                                        BigDecimal price) {
        return TradeLog.builder()
                .userId(userId)
                .ticker(ticker)
                .action(action)
                .price(price)
                .status(OrderStatus.FAILED)
                .timestamp(ZonedDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
    }
}

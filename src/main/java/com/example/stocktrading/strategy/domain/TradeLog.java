package com.example.stocktrading.strategy.domain;

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
    @Builder.Default
    private OrderStatus status = OrderStatus.SUCCESS;

    public enum OrderStatus {
        SUCCESS,
        INSUFFICIENT_BALANCE, // 보유금액 부족
        INSUFFICIENT_STOCK,   // 보유수량 부족
        FAILED
    }

    public static TradeLog create(Long userId, String ticker, StockOrder.OrderType action, BigDecimal price) {
        return TradeLog.builder()
                .userId(userId)
                .ticker(ticker)
                .action(action)
                .price(price)
                .status(OrderStatus.SUCCESS)
                .timestamp(ZonedDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
    }

    public static TradeLog createWithStatus(Long userId, String ticker, StockOrder.OrderType action,
                                            BigDecimal price, OrderStatus status) {
        return TradeLog.builder()
                .userId(userId)
                .ticker(ticker)
                .action(action)
                .price(price)
                .status(status)
                .timestamp(ZonedDateTime.now(ZoneId.of("Asia/Seoul")))
                .build();
    }
}

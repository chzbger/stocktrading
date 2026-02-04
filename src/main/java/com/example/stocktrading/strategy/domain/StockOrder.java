package com.example.stocktrading.strategy.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOrder {
    private String orderId;
    private String ticker;
    private int quantity;
    private BigDecimal price;
    private OrderType type;
    private OrderStatus status;
    private LocalDateTime orderTime;
    private String message;

    public enum OrderType {
        BUY,
        SELL
    }

    public enum OrderStatus {
        SUCCESS,
        FAILURE,
        PENDING
    }
}

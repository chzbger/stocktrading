package com.example.stocktrading.trading.domain;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingOrder {
    private Long id;
    private Long userId;
    private String ticker;
    private String orderId;
    private String orderType;
    private LocalDateTime orderTime;
}

package com.example.stocktrading.trading.adapter.out.persistence;

import com.example.stocktrading.trading.domain.StockOrder;
import com.example.stocktrading.trading.domain.TradeLog;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "trade_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockOrder.OrderType action;

    @Column(nullable = false)
    private BigDecimal price;

    private Integer profitRate;

    @Column(nullable = false)
    private ZonedDateTime timestamp;

    @Column(length = 100)
    private String orderId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(30) DEFAULT 'PENDING'")
    @Builder.Default
    private TradeLog.OrderStatus status = TradeLog.OrderStatus.PENDING;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = ZonedDateTime.now();
        }
    }
}

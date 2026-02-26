package com.example.stocktrading.trading.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;

@Entity
@Table(name = "trading_target")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingTargetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String ticker;

    @Column(columnDefinition = "INT DEFAULT 10")
    @Builder.Default
    private Integer buyThreshold = 10;

    @Column(columnDefinition = "INT DEFAULT 10")
    @Builder.Default
    private Integer sellThreshold = 10;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isActive = false;

    @Column(columnDefinition = "VARCHAR(20) DEFAULT '3.0'")
    @Builder.Default
    private String stopLossPercentage = "3.0";

    @Column(name = "base_ticker")
    private String baseTicker;

    @Column(columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isInverse = false;

    @Column(name = "trailing_stop_percentage", columnDefinition = "VARCHAR(20) DEFAULT '2.0'")
    @Builder.Default
    private String trailingStopPercentage = "2.0";

    @Column(name = "trailing_stop_enabled", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean trailingStopEnabled = true;

    @Column(name = "trailing_window_minutes", columnDefinition = "INT DEFAULT 10")
    @Builder.Default
    private Integer trailingWindowMinutes = 10;

    @Column(name = "broker_id")
    private Long brokerId;

    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now();
    }
}

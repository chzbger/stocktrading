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

    @Column(name = "profit_atr", columnDefinition = "DOUBLE DEFAULT 0.6")
    @Builder.Default
    private Double profitAtr = 0.6;

    @Column(name = "stop_atr", columnDefinition = "DOUBLE DEFAULT 0.4")
    @Builder.Default
    private Double stopAtr = 0.4;

    @Column(name = "max_holding", columnDefinition = "INT DEFAULT 5")
    @Builder.Default
    private Integer maxHolding = 5;

    @Column(name = "min_threshold", columnDefinition = "DOUBLE DEFAULT 0.2")
    @Builder.Default
    private Double minThreshold = 0.2;

    @Column(name = "training_period_years", columnDefinition = "INT DEFAULT 4")
    @Builder.Default
    private Integer trainingPeriodYears = 4;

    @Column(name = "tuning_trials", columnDefinition = "INT DEFAULT 30")
    @Builder.Default
    private Integer tuningTrials = 30;

    private ZonedDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = ZonedDateTime.now();
    }
}

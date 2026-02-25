package com.example.stocktrading.trading.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingHistory {
    private Long id;
    private String ticker;
    private String trainDate;
    private Long userId;
    private TrainingStatus status;
    private String message;

    public TrainingHistory withStatus(TrainingStatus status) {
        return withStatus(status, null);
    }

    public TrainingHistory withStatus(TrainingStatus status, String message) {
        this.status = status;
        this.message = message;
        return this;
    }

    public enum TrainingStatus {
        PENDING,
        TRAINING,
        COMPLETED,
        FAILED
    }
}

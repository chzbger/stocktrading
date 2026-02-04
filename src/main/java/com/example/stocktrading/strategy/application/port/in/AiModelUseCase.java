package com.example.stocktrading.strategy.application.port.in;

public interface AiModelUseCase {

    void trainAi(Long userId, String ticker);

    TrainingStatusInfo getTrainingStatus(String ticker);

    void deleteTraining(String ticker);

    String getTrainingLog(String ticker);

    record TrainingStatusInfo(
            Long id,
            String ticker,
            String trainDate,
            String status,
            String message) {
    }
}

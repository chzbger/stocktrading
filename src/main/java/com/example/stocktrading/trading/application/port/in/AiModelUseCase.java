package com.example.stocktrading.trading.application.port.in;

import com.example.stocktrading.trading.domain.TrainingHistory;

public interface AiModelUseCase {

    void trainAi(Long userId, String ticker);

    void deleteTraining(Long userId, String ticker);

    String getTrainingLog(Long userId, String ticker);

    TrainingHistory checkAndUpdateTrainingStatus(Long userId, String ticker);
}

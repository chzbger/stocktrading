package com.example.stocktrading.trading.application.port.out;

import com.example.stocktrading.trading.domain.TrainingHistory;

import java.util.List;

public interface AiTrainingHistoryPort {

    TrainingHistory save(TrainingHistory history);

    TrainingHistory findLatestByTickerAndUserId(String ticker, Long userId);

    List<TrainingHistory> findAll();

    List<TrainingHistory> findByTickerAndUserId(String ticker, Long userId);

    void deleteAll(List<TrainingHistory> histories);

    void deleteById(Long id);

    List<TrainingHistory> findIngHistoryByUserIdAndTicker(Long userId, String ticker);
}

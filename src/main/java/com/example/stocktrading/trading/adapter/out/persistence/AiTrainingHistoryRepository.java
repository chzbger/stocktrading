package com.example.stocktrading.trading.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiTrainingHistoryRepository extends JpaRepository<AiTrainingHistoryEntity, Long> {
    Optional<AiTrainingHistoryEntity> findTopByTickerAndUserIdOrderByTrainDateDesc(String ticker, Long userId);

    List<AiTrainingHistoryEntity> findByTickerAndUserId(String ticker, Long userId);

    List<AiTrainingHistoryEntity> findByUserIdAndTickerAndStatusIn(Long userId, String ticker, List<AiTrainingHistoryEntity.TrainingStatus> statuses);
}

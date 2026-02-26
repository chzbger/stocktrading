package com.example.stocktrading.trading.adapter.out.persistence;

import com.example.stocktrading.trading.application.port.out.AiTrainingHistoryPort;
import com.example.stocktrading.trading.domain.TrainingHistory;
import com.example.stocktrading.trading.domain.TrainingHistory.TrainingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiTrainingHistoryPersistenceAdapter implements AiTrainingHistoryPort {

    private final AiTrainingHistoryRepository repository;

    @Override
    @Transactional
    public TrainingHistory save(TrainingHistory history) {
        AiTrainingHistoryEntity entity = mapToEntity(history);
        AiTrainingHistoryEntity saved = repository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public TrainingHistory findLatestByTickerAndUserId(String ticker, Long userId) {
        return repository.findTopByTickerAndUserIdOrderByTrainDateDesc(ticker, userId)
                .map(this::mapToDomain).orElse(null);
    }

    @Override
    public List<TrainingHistory> findAll() {
        return repository.findAll().stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public List<TrainingHistory> findByTickerAndUserId(String ticker, Long userId) {
        return repository.findByTickerAndUserId(ticker, userId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteAll(List<TrainingHistory> histories) {
        List<Long> ids = histories.stream()
                .map(TrainingHistory::getId)
                .filter(Objects::nonNull)
                .toList();
        repository.deleteAllById(ids);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<TrainingHistory> findIngHistoryByUserIdAndTicker(Long userId, String ticker) {
        return repository.findByUserIdAndTickerAndStatusIn(userId, ticker,
                        List.of(AiTrainingHistoryEntity.TrainingStatus.PENDING, AiTrainingHistoryEntity.TrainingStatus.TRAINING))
                .stream()
                .map(this::mapToDomain)
                .toList();
    }

    private TrainingHistory mapToDomain(AiTrainingHistoryEntity entity) {
        if (entity == null) return null;
        return TrainingHistory.builder()
                .id(entity.getId())
                .ticker(entity.getTicker())
                .trainDate(entity.getTrainDate())
                .userId(entity.getUserId())
                .status(switch (entity.getStatus()) {
                    case PENDING -> TrainingStatus.PENDING;
                    case TRAINING -> TrainingStatus.TRAINING;
                    case COMPLETED -> TrainingStatus.COMPLETED;
                    case FAILED -> TrainingStatus.FAILED;
                })
                .message(entity.getMessage())
                .build();
    }

    private AiTrainingHistoryEntity mapToEntity(TrainingHistory history) {
        if (history == null) return null;
        AiTrainingHistoryEntity entity;
        if (history.getId() != null) {
            entity = repository.findById(history.getId()).orElse(new AiTrainingHistoryEntity());
        } else {
            entity = new AiTrainingHistoryEntity();
        }
        entity.setTicker(history.getTicker());
        entity.setTrainDate(history.getTrainDate());
        entity.setUserId(history.getUserId());
        entity.setStatus(switch (history.getStatus()) {
            case PENDING -> AiTrainingHistoryEntity.TrainingStatus.PENDING;
            case TRAINING -> AiTrainingHistoryEntity.TrainingStatus.TRAINING;
            case COMPLETED -> AiTrainingHistoryEntity.TrainingStatus.COMPLETED;
            case FAILED -> AiTrainingHistoryEntity.TrainingStatus.FAILED;
        });
        entity.setMessage(history.getMessage());
        return entity;
    }
}

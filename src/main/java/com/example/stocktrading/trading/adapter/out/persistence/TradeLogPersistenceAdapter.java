package com.example.stocktrading.trading.adapter.out.persistence;

import com.example.stocktrading.trading.application.port.out.TradeLogPort;
import com.example.stocktrading.trading.domain.TradeLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradeLogPersistenceAdapter implements TradeLogPort {

    private final TradeLogRepository tradeLogRepository;

    @Override
    @Transactional
    public TradeLog save(TradeLog tradeLog) {
        TradeLogEntity entity = mapToEntity(tradeLog);
        TradeLogEntity saved = tradeLogRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public List<TradeLog> findByUserIdOrderByTimestampAsc(Long userId) {
        return tradeLogRepository.findByUserIdOrderByTimestampAsc(userId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public List<TradeLog> findRecentByUserId(Long userId, int limit) {
        return tradeLogRepository.findByUserIdOrderByTimestampDesc(userId, PageRequest.of(0, limit)).stream()
                .map(this::mapToDomain)
                .toList();
    }

    public TradeLog mapToDomain(TradeLogEntity entity) {
        if (entity == null) return null;
        return TradeLog.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .ticker(entity.getTicker())
                .action(entity.getAction())
                .price(entity.getPrice())
                .profitRate(entity.getProfitRate())
                .timestamp(entity.getTimestamp())
                .status(entity.getStatus() != null ? entity.getStatus() : TradeLog.OrderStatus.SUCCESS)
                .build();
    }

    public static TradeLogEntity mapToEntity(TradeLog tradeLog) {
        if (tradeLog == null) return null;
        return TradeLogEntity.builder()
                .id(tradeLog.getId())
                .userId(tradeLog.getUserId())
                .ticker(tradeLog.getTicker())
                .action(tradeLog.getAction())
                .price(tradeLog.getPrice())
                .profitRate(tradeLog.getProfitRate())
                .timestamp(tradeLog.getTimestamp())
                .status(tradeLog.getStatus() != null ? tradeLog.getStatus() : TradeLog.OrderStatus.SUCCESS)
                .build();
    }
}

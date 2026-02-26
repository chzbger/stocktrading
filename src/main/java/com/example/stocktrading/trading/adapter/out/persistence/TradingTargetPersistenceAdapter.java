package com.example.stocktrading.trading.adapter.out.persistence;

import com.example.stocktrading.trading.application.port.out.TradingTargetPort;
import com.example.stocktrading.trading.domain.TradingTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TradingTargetPersistenceAdapter implements TradingTargetPort {

    private final TradingTargetRepository tradingTargetRepository;

    @Override
    @Transactional
    public TradingTarget save(TradingTarget item) {
        TradingTargetEntity entity = mapToEntity(item);
        TradingTargetEntity saved = tradingTargetRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public TradingTarget findById(Long id) {
        return tradingTargetRepository.findById(id).map(this::mapToDomain).orElse(null);
    }

    @Override
    public TradingTarget findByUserIdAndTicker(Long userId, String ticker) {
        TradingTargetEntity entity = tradingTargetRepository.findByUserIdAndTicker(userId, ticker).orElse(null);
        return mapToDomain(entity);
    }

    @Override
    public List<TradingTarget> findByUserId(Long userId) {
        return tradingTargetRepository.findByUserIdOrderByIdAsc(userId).stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    @Transactional
    public void delete(TradingTarget item) {
        tradingTargetRepository.findById(item.getId()).ifPresent(tradingTargetRepository::delete);
    }

    @Override
    public List<TradingTarget> findActiveItems() {
        return tradingTargetRepository.findByIsActiveTrue().stream()
                .map(this::mapToDomain)
                .toList();
    }

    private TradingTarget mapToDomain(TradingTargetEntity entity) {
        if (entity == null) return null;
        return TradingTarget.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .ticker(entity.getTicker())
                .active(Boolean.TRUE.equals(entity.getIsActive()))
                .buyThreshold(entity.getBuyThreshold() != null ? entity.getBuyThreshold() : 10)
                .sellThreshold(entity.getSellThreshold() != null ? entity.getSellThreshold() : 10)
                .stopLossPercentage(toBigDecimal(entity.getStopLossPercentage(), "3.0"))
                .baseTicker(entity.getBaseTicker())
                .inverse(Boolean.TRUE.equals(entity.getIsInverse()))
                .trailingStopPercentage(toBigDecimal(entity.getTrailingStopPercentage(), "2.0"))
                .trailingStopEnabled(entity.getTrailingStopEnabled() == null || entity.getTrailingStopEnabled())
                .trailingWindowMinutes(entity.getTrailingWindowMinutes() != null ? entity.getTrailingWindowMinutes() : 10)
                .brokerId(entity.getBrokerId())
                .holdingQuantity(entity.getHoldingQuantity() != null ? entity.getHoldingQuantity() : 0)
                .build();
    }

    private TradingTargetEntity mapToEntity(TradingTarget item) {
        if (item == null) return null;
        TradingTargetEntity entity;
        if (item.getId() != null) {
            entity = tradingTargetRepository.findById(item.getId()).orElse(new TradingTargetEntity());
        } else {
            entity = new TradingTargetEntity();
        }
        entity.setUserId(item.getUserId());
        entity.setTicker(item.getTicker());
        entity.setIsActive(item.isActive());
        entity.setBuyThreshold(item.getBuyThreshold());
        entity.setSellThreshold(item.getSellThreshold());
        entity.setStopLossPercentage(item.getStopLossPercentage().toPlainString());
        entity.setBaseTicker(item.getBaseTicker());
        entity.setIsInverse(item.isInverse());
        entity.setTrailingStopPercentage(item.getTrailingStopPercentage().toPlainString());
        entity.setTrailingStopEnabled(item.isTrailingStopEnabled());
        entity.setTrailingWindowMinutes(item.getTrailingWindowMinutes());
        entity.setBrokerId(item.getBrokerId());
        entity.setHoldingQuantity(item.getHoldingQuantity());
        return entity;
    }

    private BigDecimal toBigDecimal(String value, String defaultValue) {
        return new BigDecimal(value != null ? value : defaultValue);
    }
}

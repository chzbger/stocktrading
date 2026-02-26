package com.example.stocktrading.trading.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TradingTargetRepository extends JpaRepository<TradingTargetEntity, Long> {
    List<TradingTargetEntity> findByUserIdOrderByIdAsc(Long userId);

    Optional<TradingTargetEntity> findByUserIdAndTicker(Long userId, String ticker);

    List<TradingTargetEntity> findByIsActiveTrue();
}

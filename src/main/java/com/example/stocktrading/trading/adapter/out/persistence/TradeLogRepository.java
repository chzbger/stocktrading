package com.example.stocktrading.trading.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeLogRepository extends JpaRepository<TradeLogEntity, Long> {
    List<TradeLogEntity> findByUserIdOrderByTimestampAsc(Long userId);

    List<TradeLogEntity> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
}

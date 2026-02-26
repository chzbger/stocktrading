package com.example.stocktrading.trading.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PendingOrderRepository extends JpaRepository<PendingOrderEntity, Long> {

    List<PendingOrderEntity> findByOrderTimeBefore(LocalDateTime threshold);
}

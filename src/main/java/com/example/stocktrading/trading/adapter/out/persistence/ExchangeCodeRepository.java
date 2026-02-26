package com.example.stocktrading.trading.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeCodeRepository extends JpaRepository<ExchangeCodeEntity, Long> {

    Optional<ExchangeCodeEntity> findByTicker(String ticker);
}

package com.example.stocktrading.trading.application.port.out;

import com.example.stocktrading.trading.domain.TradingTarget;

import java.util.List;

public interface TradingTargetPort {

    TradingTarget save(TradingTarget item);

    TradingTarget findById(Long id);

    TradingTarget findByUserIdAndTicker(Long userId, String ticker);

    List<TradingTarget> findByUserId(Long userId);

    void delete(TradingTarget item);

    List<TradingTarget> findActiveItems();
}

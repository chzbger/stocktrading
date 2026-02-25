package com.example.stocktrading.trading.application.port.in;

import com.example.stocktrading.trading.domain.TradingTarget;

import java.util.List;

public interface TradingTargetUseCase {

    TradingTarget add(Long userId, String ticker, Long brokerId);

    void remove(Long userId, Long tradingTargetId);

    List<TradingTarget> findByUserId(Long userId);

    TradingTarget updateSettings(Long id, TradingTarget settings);

    void setActive(Long userId, String ticker, boolean active);
}

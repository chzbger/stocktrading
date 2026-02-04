package com.example.stocktrading.strategy.application.port.in;

public interface TradingUseCase {

    void initialize();

    void executeTradingCycle();
}

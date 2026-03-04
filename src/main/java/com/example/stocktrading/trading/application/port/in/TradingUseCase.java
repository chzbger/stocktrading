package com.example.stocktrading.trading.application.port.in;

public interface TradingUseCase {

    void initialize();

    void executeRiskManagement();

    void executeAiTrading();
}

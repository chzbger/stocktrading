package com.example.stocktrading.trading.application.port.in;

public interface TradingUseCase {

    void initialize();

    void executeTradingCycle();

    void handlePendingOrders();
}

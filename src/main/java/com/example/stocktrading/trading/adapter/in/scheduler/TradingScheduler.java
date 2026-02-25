package com.example.stocktrading.trading.adapter.in.scheduler;

import com.example.stocktrading.trading.application.port.in.TradingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradingScheduler implements ApplicationRunner {

    private final TradingUseCase tradingUseCase;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[Scheduler] Initializing trading service...");
        tradingUseCase.initialize();
        log.info("[Scheduler] Initializing trading service done");
    }

    @Scheduled(fixedDelay = 60000)
    public void executeTradingCycle() {
        try {
            tradingUseCase.executeTradingCycle();
        } catch (Exception e) {
            log.error("[Scheduler] Trading cycle failed", e);
        }
    }

    @Scheduled(fixedDelay = 120000)
    public void executeCancelOpenOrderCycle() {
        try {
            tradingUseCase.cancelOpenOrders();
        } catch (Exception e) {
            log.error("[Scheduler] Cancel open orders failed", e);
        }
    }

    @Scheduled(fixedDelay = 300000)
    public void syncHoldingQuantities() {
        try {
            tradingUseCase.syncHoldingQuantities();
        } catch (Exception e) {
            log.error("[Scheduler] Sync holding quantities failed", e);
        }
    }
}

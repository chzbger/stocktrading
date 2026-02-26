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

    /**
     * AI 예측 자동 매매
     */
    @Scheduled(fixedDelay = 60000)
    public void executeTradingCycle() {
        try {
            tradingUseCase.executeTradingCycle();
        } catch (Exception e) {
            log.error("[Scheduler] Trading cycle failed", e);
        }
    }

    /**
     * 미체결 주문을 체결or실패 처리
     */
    @Scheduled(fixedDelay = 120000)
    public void handlePendingOrders() {
        try {
            tradingUseCase.handlePendingOrders();
        } catch (Exception e) {
            log.error("[Scheduler] Confirm/cancel pending orders failed", e);
        }
    }
}

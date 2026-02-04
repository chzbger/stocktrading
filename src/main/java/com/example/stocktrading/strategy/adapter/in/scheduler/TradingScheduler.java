package com.example.stocktrading.strategy.adapter.in.scheduler;

import com.example.stocktrading.strategy.application.port.in.TradingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void executeTradingCycle() {
        try {
            tradingUseCase.executeTradingCycle();
        } catch (Exception e) {
            log.error("[Scheduler] Trading cycle failed, will retry on next cycle", e);
        }
    }
}

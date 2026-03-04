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
     * 리스크 관리 (미체결 처리, 타임아웃, 손절, 트레일링스톱)
     */
    @Scheduled(fixedDelay = 60000)
    public void executeRiskManagement() {
        try {
            tradingUseCase.executeRiskManagement();
        } catch (Exception e) {
            log.error("[Scheduler] Risk management failed", e);
        }
    }

    /**
     * AI 매매 (캔들 조회, AI 예측, 주문 실행)
     */
    @Scheduled(fixedDelay = 60000)
    public void executeAiTrading() {
        try {
            tradingUseCase.executeAiTrading();
        } catch (Exception e) {
            log.error("[Scheduler] AI trading failed", e);
        }
    }
}

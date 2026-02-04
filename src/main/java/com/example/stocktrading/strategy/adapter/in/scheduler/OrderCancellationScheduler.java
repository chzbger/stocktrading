package com.example.stocktrading.strategy.adapter.in.scheduler;

import com.example.stocktrading.strategy.application.port.in.OrderCancellationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancellationScheduler {

    private final OrderCancellationUseCase orderCancellationUseCase;

    /**
     * 5분마다 실행 - 10분 이상 된 미체결 주문 취소
     */
    @Scheduled(cron = "0 */5 * * * *")
    public void cancelStaleOrders() {
        try {
            orderCancellationUseCase.cancelStaleOrders();
        } catch (Exception e) {
            log.error("[Scheduler] Order cancellation failed", e);
        }
    }
}

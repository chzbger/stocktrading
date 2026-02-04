package com.example.stocktrading.strategy.application.service;

import com.example.stocktrading.auth.application.port.out.UserPort;
import com.example.stocktrading.auth.domain.User;
import com.example.stocktrading.strategy.application.port.in.OrderCancellationUseCase;
import com.example.stocktrading.strategy.application.port.out.BrokerApiPort;
import com.example.stocktrading.strategy.application.port.out.BrokerApiPort.CancelResult;
import com.example.stocktrading.strategy.application.port.out.PendingOrderPort;
import com.example.stocktrading.strategy.application.port.out.PendingOrderPort.PendingOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCancellationService implements OrderCancellationUseCase {

    private final PendingOrderPort pendingOrderPort;
    private final BrokerApiPort brokerApiPort;
    private final UserPort userPort;

    private static final int STALE_MINUTES = 10;

    @Override
    public void savePendingOrder(Long userId, String ticker, String orderId, String orderType) {
        PendingOrder order = new PendingOrder(
                null,
                userId,
                ticker,
                orderId,
                orderType,
                LocalDateTime.now()
        );
        pendingOrderPort.save(order);
        log.info("[PendingOrder] 저장: userId={}, ticker={}, orderId={}", userId, ticker, orderId);
    }

    @Override
    public void cancelStaleOrders() {
        log.info("[OrderCancellation] ========== 미체결 주문 취소 시작 ==========");

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STALE_MINUTES);
        List<PendingOrder> staleOrders = pendingOrderPort.findOrdersOlderThan(threshold);

        if (staleOrders.isEmpty()) {
            log.info("[OrderCancellation] 취소 대상 없음");
            return;
        }

        log.info("[OrderCancellation] 취소 대상: {}건", staleOrders.size());

        for (PendingOrder order : staleOrders) {
            try {
                User user = userPort.findById(order.userId()).orElse(null);
                if (user == null) {
                    log.warn("[OrderCancellation] 사용자 없음: userId={}", order.userId());
                    pendingOrderPort.delete(order.id());
                    continue;
                }

                // TODO: 실제 미체결 여부 확인 (brokerApiPort.getUnfilledOrders)
                // TODO: 체결된 주문이면 pendingOrderPort.delete만 수행

                CancelResult result = brokerApiPort.cancelOrder(user, order.orderId());

                if (result.success()) {
                    log.info("[OrderCancellation] 취소 성공: ticker={}, orderId={}", order.ticker(), order.orderId());
                } else {
                    log.warn("[OrderCancellation] 취소 실패: ticker={}, orderId={}, msg={}",
                            order.ticker(), order.orderId(), result.message());
                }

                pendingOrderPort.delete(order.id());

            } catch (Exception e) {
                log.error("[OrderCancellation] 예외: orderId={}, error={}", order.orderId(), e.getMessage());
            }
        }

        log.info("[OrderCancellation] ========== 미체결 주문 취소 종료 ==========");
    }
}

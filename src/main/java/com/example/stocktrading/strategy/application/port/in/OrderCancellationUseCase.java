package com.example.stocktrading.strategy.application.port.in;

public interface OrderCancellationUseCase {

    /** 주문 실행 후 pending order 저장 */
    void savePendingOrder(Long userId, String ticker, String orderId, String orderType);

    /** 10분 지난 미체결 주문 취소 */
    void cancelStaleOrders();
}

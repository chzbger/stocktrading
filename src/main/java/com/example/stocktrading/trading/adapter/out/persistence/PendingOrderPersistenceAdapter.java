package com.example.stocktrading.trading.adapter.out.persistence;

import com.example.stocktrading.trading.application.port.out.PendingOrderPort;
import com.example.stocktrading.trading.domain.PendingOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PendingOrderPersistenceAdapter implements PendingOrderPort {

    private final PendingOrderRepository repository;

    @Override
    public void save(PendingOrder order) {
        PendingOrderEntity entity = PendingOrderEntity.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .ticker(order.getTicker())
                .orderId(order.getOrderId())
                .orderType(order.getOrderType())
                .orderTime(order.getOrderTime())
                .build();
        repository.save(entity);
    }

    @Override
    public List<PendingOrder> findOrdersOlderThan(LocalDateTime threshold) {
        return repository.findByOrderTimeBefore(threshold).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private PendingOrder toDomain(PendingOrderEntity entity) {
        return PendingOrder.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .ticker(entity.getTicker())
                .orderId(entity.getOrderId())
                .orderType(entity.getOrderType())
                .orderTime(entity.getOrderTime())
                .build();
    }
}

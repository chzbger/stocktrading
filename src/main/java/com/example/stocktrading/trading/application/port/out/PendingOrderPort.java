package com.example.stocktrading.trading.application.port.out;

import com.example.stocktrading.trading.domain.PendingOrder;

import java.time.LocalDateTime;
import java.util.List;

public interface PendingOrderPort {

    void save(PendingOrder order);

    List<PendingOrder> findOrdersOlderThan(LocalDateTime threshold);

    void delete(Long id);
}

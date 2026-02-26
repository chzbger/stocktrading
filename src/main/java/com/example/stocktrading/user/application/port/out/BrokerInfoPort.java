package com.example.stocktrading.user.application.port.out;

import com.example.stocktrading.user.domain.BrokerInfo;

import java.util.Optional;

public interface BrokerInfoPort {

    BrokerInfo save(BrokerInfo brokerInfo);

    Optional<BrokerInfo> findById(Long id);

    void deleteById(Long id);
}

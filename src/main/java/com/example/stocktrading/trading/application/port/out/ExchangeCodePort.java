package com.example.stocktrading.trading.application.port.out;

import java.util.Optional;

public interface ExchangeCodePort {

    Optional<ExchangeCode> findByTicker(String ticker);

    record ExchangeCode(
            String ticker,
            String exchangeMic,
            String kisPriceCode,
            String kisOrderCode
    ) {}
}

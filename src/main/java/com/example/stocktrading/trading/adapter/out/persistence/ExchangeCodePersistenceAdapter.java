package com.example.stocktrading.trading.adapter.out.persistence;

import com.example.stocktrading.trading.application.port.out.ExchangeCodePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ExchangeCodePersistenceAdapter implements ExchangeCodePort {

    private final ExchangeCodeRepository exchangeCodeRepository;

    @Override
    public Optional<ExchangeCode> findByTicker(String ticker) {
        return exchangeCodeRepository.findByTicker(ticker)
                .map(e -> new ExchangeCode(e.getTicker(), e.getExchangeMic(),
                        e.getKisPriceCode(), e.getKisOrderCode()));
    }
}

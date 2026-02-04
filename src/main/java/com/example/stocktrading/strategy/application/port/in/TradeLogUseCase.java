package com.example.stocktrading.strategy.application.port.in;

import com.example.stocktrading.strategy.domain.TradeLog;

import java.math.BigDecimal;
import java.util.List;

public interface TradeLogUseCase {

    List<TradeLog> getRecentTradeLogs(Long userId);

    ProfitStats calculateProfitStats(Long userId);

    record ProfitStats(BigDecimal realizedProfit) {
    }
}

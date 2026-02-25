package com.example.stocktrading.trading.application.port.in;

import com.example.stocktrading.trading.domain.TradeLog;

import java.math.BigDecimal;
import java.util.List;

public interface TradeLogUseCase {

    List<TradeLog> getRecentTradeLogs(Long userId);

    BigDecimal calculateProfitStats(Long userId);
}

package com.example.stocktrading.trading.application.port.out;

import com.example.stocktrading.trading.domain.TradeLog;

import java.util.List;

public interface TradeLogPort {

    TradeLog save(TradeLog tradeLog);

    List<TradeLog> findByUserIdOrderByTimestampAsc(Long userId);

    List<TradeLog> findRecentByUserId(Long userId, int limit);
}

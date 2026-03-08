package com.example.stocktrading.trading.application.port.out;

import com.example.stocktrading.trading.domain.TradeLog;

import java.time.ZonedDateTime;
import java.util.List;

public interface TradeLogPort {

    TradeLog save(TradeLog tradeLog);

    List<TradeLog> findByUserIdOrderByTimestampAsc(Long userId);

    List<TradeLog> findRecentByUserId(Long userId, int limit);

    TradeLog updateStatus(Long tradeLogId, TradeLog.OrderStatus newStatus);

    List<TradeLog> findPendingBefore(ZonedDateTime threshold);

    int closeFilledBuysBefore(Long userId, String ticker, Long beforeSellId);

    boolean hasPendingSell(Long userId, String ticker);

    int getHoldingCount(Long userId, String ticker);

    ZonedDateTime getPositionOpenedAt(Long userId, String ticker);
}

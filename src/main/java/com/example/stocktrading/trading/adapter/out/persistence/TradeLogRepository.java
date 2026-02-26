package com.example.stocktrading.trading.adapter.out.persistence;

import com.example.stocktrading.trading.domain.StockOrder;
import com.example.stocktrading.trading.domain.TradeLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface TradeLogRepository extends JpaRepository<TradeLogEntity, Long> {
    List<TradeLogEntity> findByUserIdOrderByTimestampAsc(Long userId);

    List<TradeLogEntity> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    // PENDING 주문 조회 (cancel check용, pending_orders 대체)
    List<TradeLogEntity> findByStatusAndTimestampBefore(TradeLog.OrderStatus status, ZonedDateTime threshold);

    // FILLED BUY 조회 (SELL 체결 시 CLOSED로 전환용)
    List<TradeLogEntity> findByUserIdAndTickerAndActionAndStatus(
            Long userId, String ticker, StockOrder.OrderType action, TradeLog.OrderStatus status);

    // PENDING SELL 존재 여부 (충돌 방지 가드)
    boolean existsByUserIdAndTickerAndActionAndStatus(
            Long userId, String ticker, StockOrder.OrderType action, TradeLog.OrderStatus status);

    // 보유 수량 (FILLED BUY 개수)
    @Query("SELECT COUNT(t) FROM TradeLogEntity t WHERE t.userId = :uid AND t.ticker = :ticker AND t.action = 'BUY' AND t.status = 'FILLED'")
    int countFilledBuys(@Param("uid") Long uid, @Param("ticker") String ticker);

    // 포지션 시작시각 (FILLED BUY 중 가장 오래된 timestamp)
    @Query("SELECT MIN(t.timestamp) FROM TradeLogEntity t WHERE t.userId = :uid AND t.ticker = :ticker AND t.action = 'BUY' AND t.status = 'FILLED'")
    ZonedDateTime findEarliestFilledBuyTimestamp(@Param("uid") Long uid, @Param("ticker") String ticker);
}

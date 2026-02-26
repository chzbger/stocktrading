package com.example.stocktrading.trading.application.service;

import com.example.stocktrading.trading.application.port.in.TradeLogUseCase;
import com.example.stocktrading.trading.application.port.out.TradeLogPort;
import com.example.stocktrading.trading.domain.StockOrder;
import com.example.stocktrading.trading.domain.TradeLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TradeLogService implements TradeLogUseCase {

    private static final int RECENT_LOGS_LIMIT = 20;

    private final TradeLogPort tradeLogPort;

    @Override
    public List<TradeLog> getRecentTradeLogs(Long userId) {
        return tradeLogPort.findRecentByUserId(userId, RECENT_LOGS_LIMIT);
    }

    @Override
    public BigDecimal calculateProfitStats(Long userId) {
        List<TradeLog> logsAsc = tradeLogPort.findByUserIdOrderByTimestampAsc(userId);

        Map<String, List<TradeLog>> logsByTicker = logsAsc.stream()
                .filter(log -> log.getStatus() == TradeLog.OrderStatus.CLOSED
                        || (log.getAction() == StockOrder.OrderType.SELL
                            && log.getStatus() == TradeLog.OrderStatus.FILLED))
                .collect(Collectors.groupingBy(TradeLog::getTicker));

        BigDecimal totalProfit = BigDecimal.ZERO;
        for (List<TradeLog> tickerLogsAsc : logsByTicker.values()) {
            totalProfit = totalProfit.add(calculateTickerProfit(tickerLogsAsc));
        }

        return totalProfit;
    }

    @Override
    public int getHoldingCount(Long userId, String ticker) {
        return tradeLogPort.getHoldingCount(userId, ticker);
    }

    private BigDecimal calculateTickerProfit(List<TradeLog> tickerLogsAsc) {
        BigDecimal profit = BigDecimal.ZERO;
        List<BigDecimal> buyPrices = new ArrayList<>();

        // CLOSED BUYs + FILLED SELLs만 사용
        for (TradeLog log : tickerLogsAsc) {
            if (log.getAction() == StockOrder.OrderType.BUY && log.getStatus() == TradeLog.OrderStatus.CLOSED) {
                buyPrices.add(log.getPrice());
            } else if (log.getAction() == StockOrder.OrderType.SELL && log.getStatus() == TradeLog.OrderStatus.FILLED) {
                if (!buyPrices.isEmpty()) {
                    BigDecimal totalBuyPrice = buyPrices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                    profit = profit.add(log.getPrice().multiply(BigDecimal.valueOf(buyPrices.size())).subtract(totalBuyPrice));
                    buyPrices.clear();
                }
            }
        }

        return profit;
    }
}

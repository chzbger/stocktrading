package com.example.stocktrading.strategy.application.service;

import com.example.stocktrading.strategy.application.port.in.TradeLogUseCase;
import com.example.stocktrading.strategy.application.port.out.TradeLogPort;
import com.example.stocktrading.strategy.domain.StockOrder;
import com.example.stocktrading.strategy.domain.TradeLog;
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
    public ProfitStats calculateProfitStats(Long userId) {
        List<TradeLog> logsAsc = tradeLogPort.findByUserIdOrderByTimestampAsc(userId);

        List<TradeLog> successLogsAsc = logsAsc.stream()
                .filter(log -> log.getStatus() == TradeLog.OrderStatus.SUCCESS)
                .toList();

        Map<String, List<TradeLog>> logsByTicker = successLogsAsc.stream()
                .collect(Collectors.groupingBy(TradeLog::getTicker));

        // 티커별 계산
        BigDecimal totalProfit = BigDecimal.ZERO;
        for (List<TradeLog> tickerLogsAsc : logsByTicker.values()) {
            totalProfit = totalProfit.add(calculateTickerProfit(tickerLogsAsc));
        }

        return new ProfitStats(totalProfit);
    }

    private BigDecimal calculateTickerProfit(List<TradeLog> tickerLogsAsc) {
        BigDecimal profit = BigDecimal.ZERO;
        BigDecimal buyPrice = BigDecimal.ZERO;

        // 매수는 1개씩 한다고 가정
        // 매도는 일괄
        // 현재 보유수량(매도 안한것)은 수익으로 계산 안함
        for (TradeLog log : tickerLogsAsc) {
            if (log.getAction() == StockOrder.OrderType.BUY) {
                buyPrice = buyPrice.add(log.getPrice());
            } else if (log.getAction() == StockOrder.OrderType.SELL) {
                BigDecimal sellPrice = log.getPrice();
                profit = profit.add(sellPrice.subtract(buyPrice));
                buyPrice = BigDecimal.ZERO;
            }
        }

        return profit;
    }
}

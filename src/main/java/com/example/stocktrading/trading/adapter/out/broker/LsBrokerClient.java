package com.example.stocktrading.trading.adapter.out.broker;

import com.example.stocktrading.trading.application.port.out.BrokerApiPort;
import com.example.stocktrading.trading.application.port.out.BrokerClient;
import com.example.stocktrading.trading.domain.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * TODO
 */
@Slf4j
@Component
public class LsBrokerClient implements BrokerClient {

    @Override
    public BrokerApiPort.OrderResult sendOrder(BrokerContext ctx, StockOrder stockOrder) {
        log.info("[LS] Stub: sendOrder - {}", stockOrder);
        return new BrokerApiPort.OrderResult(false, TradeLog.OrderStatus.FAILED, "");
    }

    @Override
    public BigDecimal getCurrentPrice(BrokerContext ctx, String ticker) {
        log.info("[LS] Stub: getCurrentPrice - {}", ticker);
        return BigDecimal.ZERO;
    }

    @Override
    public List<StockCandle> getRecentCandles(BrokerContext ctx, String ticker, int limit) {
        log.info("[LS] Stub: getRecentCandles");
        return Collections.emptyList();
    }

    @Override
    public List<StockCandle> getRecentCandles5Min(BrokerContext ctx, String ticker, int limit) {
        log.info("[LS] Stub: getRecentCandles5Min");
        return Collections.emptyList();
    }

    @Override
    public Asset getAccountAsset(BrokerContext ctx) {
        log.info("[LS] Stub: getAccountAsset");
        return Asset.builder()
                .accountNo("LS-STUB")
                .totalAsset(BigDecimal.ZERO)
                .usdDeposit(BigDecimal.ZERO)
                .ownedStocks(Collections.emptyList())
                .build();
    }

    @Override
    public BrokerApiPort.CancelResult cancelOrder(BrokerContext ctx, String orderId) {
        // TODO: LS API 주문 취소 구현
        log.info("[LS] Stub: cancelOrder - TODO, orderId={}", orderId);
        return new BrokerApiPort.CancelResult(false, "Not implemented");
    }
}

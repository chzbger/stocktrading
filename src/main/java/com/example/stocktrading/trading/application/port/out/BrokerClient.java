package com.example.stocktrading.trading.application.port.out;

import com.example.stocktrading.trading.domain.Asset;
import com.example.stocktrading.trading.domain.BrokerContext;
import com.example.stocktrading.trading.domain.StockCandle;
import com.example.stocktrading.trading.domain.StockOrder;

import java.math.BigDecimal;
import java.util.List;

public interface BrokerClient {

    BrokerApiPort.OrderResult sendOrder(BrokerContext ctx, StockOrder stockOrder);

    BigDecimal getCurrentPrice(BrokerContext ctx, String ticker);

    List<StockCandle> getRecentCandles(BrokerContext ctx, String ticker, int limit);

    List<StockCandle> getRecentCandles5Min(BrokerContext ctx, String ticker, int limit);

    Asset getAccountAsset(BrokerContext ctx);

    /** 주문 취소 */
    BrokerApiPort.CancelResult cancelOrder(BrokerContext ctx, String orderId);
}

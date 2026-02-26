package com.example.stocktrading.trading.application.port.out;

import com.example.stocktrading.user.domain.User;
import com.example.stocktrading.trading.domain.Asset;
import com.example.stocktrading.trading.domain.StockCandle;
import com.example.stocktrading.trading.domain.StockOrder;

import java.math.BigDecimal;
import java.util.List;

public interface BrokerApiPort {
    OrderResult sendOrder(User user, StockOrder stockOrder);

    BigDecimal getCurrentPrice(User user, String ticker);

    List<StockCandle> getRecentCandles(User user, String ticker, int limit);

    List<StockCandle> getRecentCandles5Min(User user, String ticker, int limit);

    Asset getAccountAsset(User user);

    CancelResult cancelOrder(User user, String orderId);

    record OrderResult(boolean success, String message, String orderId) {
        public OrderResult(boolean success, String message) {
            this(success, message, null);
        }
    }

    record CancelResult(boolean success, String message) {}
}

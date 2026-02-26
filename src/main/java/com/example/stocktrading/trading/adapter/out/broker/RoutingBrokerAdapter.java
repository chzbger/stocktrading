package com.example.stocktrading.trading.adapter.out.broker;

import com.example.stocktrading.user.domain.BrokerInfo;
import com.example.stocktrading.user.domain.User;
import com.example.stocktrading.trading.application.port.out.BrokerApiPort;
import com.example.stocktrading.trading.application.port.out.BrokerClient;
import com.example.stocktrading.trading.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class RoutingBrokerAdapter implements BrokerApiPort {

    private final KisBrokerClient kisClient;
    private final LsBrokerClient lsClient;

    private Optional<BrokerContext> resolveContext(User user) {
        if (user == null || user.getActiveBrokerId() == null) {
            throw new RuntimeException(
                    "[Router] Invalid User: " + Optional.ofNullable(user).map(User::getUsername).orElse("null"));
        }
        return user.getBrokerInfos().stream()
                .filter(bi -> bi.getId().equals(user.getActiveBrokerId()))
                .filter(this::hasValidCredentials)
                .map(brokerInfo -> {
                    String[] parts = brokerInfo.getAccountNumber().split("-");
                    return BrokerContext.builder()
                            .appKey(brokerInfo.getAppKey())
                            .appSecret(brokerInfo.getAppSecret())
                            .accountNo(brokerInfo.getAccountNumber())
                            .cano(parts[0])
                            .acntPrdtCd(parts.length > 1 ? parts[1] : "01")
                            .brokerType(brokerInfo.getBrokerType())
                            .build();
                })
                .findFirst();
    }

    private boolean hasValidCredentials(BrokerInfo bi) {
        boolean valid = bi.getAppKey() != null && !bi.getAppKey().isEmpty()
                && bi.getAppSecret() != null && !bi.getAppSecret().isEmpty()
                && bi.getAccountNumber() != null && !bi.getAccountNumber().isEmpty();
        if (!valid) {
            log.warn("[Router] Invalid credentials: {}", bi.getBrokerType().name());
        }
        return valid;
    }

    private BrokerClient getClient(BrokerContext ctx) {
        return switch (ctx.getBrokerType()) {
            case KIS -> kisClient;
            case LS -> lsClient;
        };
    }

    @Override
    public OrderResult sendOrder(User user, StockOrder stockOrder) {
        return resolveContext(user)
                .map(ctx -> getClient(ctx).sendOrder(ctx, stockOrder))
                .orElse(new OrderResult(false, TradeLog.OrderStatus.FAILED, "sendOrder orElse"));
    }

    @Override
    public BigDecimal getCurrentPrice(User user, String ticker) {
        return resolveContext(user)
                .map(ctx -> getClient(ctx).getCurrentPrice(ctx, ticker))
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public List<StockCandle> getRecentCandles(User user, String ticker, int limit) {
        return resolveContext(user)
                .map(ctx -> getClient(ctx).getRecentCandles(ctx, ticker, limit))
                .orElse(Collections.emptyList());
    }

    @Override
    public List<StockCandle> getRecentCandles5Min(User user, String ticker, int limit) {
        return resolveContext(user)
                .map(ctx -> getClient(ctx).getRecentCandles5Min(ctx, ticker, limit))
                .orElse(Collections.emptyList());
    }

    @Override
    public Asset getAccountAsset(User user) {
        return resolveContext(user)
                .map(ctx -> getClient(ctx).getAccountAsset(ctx))
                .orElse(Asset.builder()
                        .totalAsset(BigDecimal.ZERO)
                        .usdDeposit(BigDecimal.ZERO)
                        .ownedStocks(Collections.emptyList())
                        .build());
    }

    @Override
    public CancelResult cancelOrder(User user, String orderId) {
        return resolveContext(user)
                .map(ctx -> getClient(ctx).cancelOrder(ctx, orderId))
                .orElse(new CancelResult(false, "No broker context"));
    }
}

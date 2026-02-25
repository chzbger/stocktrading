package com.example.stocktrading.trading.application.service;

import com.example.stocktrading.common.StockConst;
import com.example.stocktrading.common.security.AuthContext;
import com.example.stocktrading.trading.application.port.in.TradingTargetUseCase;
import com.example.stocktrading.trading.application.port.out.TradingTargetPort;
import com.example.stocktrading.trading.domain.TradingTarget;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradingTargetService implements TradingTargetUseCase {

    private final TradingTargetPort tradingTargetPort;

    @Override
    @Transactional
    public TradingTarget add(Long userId, String ticker, Long brokerId) {
        StockConst.VALIDATE_TICKER.accept(ticker);
        TradingTarget newItem = TradingTarget.ofCreate(userId, ticker, brokerId);
        return tradingTargetPort.save(newItem);
    }

    @Override
    public void remove(Long userId, Long tradingTargetId) {
        TradingTarget tradingTarget = tradingTargetPort.findById(tradingTargetId);
        if (tradingTarget != null && tradingTarget.getUserId().equals(userId)) {
            tradingTargetPort.delete(tradingTarget);
        }
    }

    @Override
    public List<TradingTarget> findByUserId(Long userId) {
        return tradingTargetPort.findByUserId(userId);
    }

    @Override
    public TradingTarget updateSettings(Long id, TradingTarget settings) {
        TradingTarget existing = tradingTargetPort.findById(id);
        if (existing == null || !existing.getUserId().equals(AuthContext.getUserId())) {
            throw new IllegalArgumentException("없는 매매 대상이거나 유저의 티커가 아님");
        }

        String baseTicker = settings.getBaseTicker();
        if (baseTicker != null && !baseTicker.isBlank()) {
            baseTicker = baseTicker.toUpperCase();
        } else {
            baseTicker = null;
        }

        TradingTarget toUpdate = existing.toBuilder()
                .buyThreshold(settings.getBuyThreshold())
                .sellThreshold(settings.getSellThreshold())
                .stopLossPercentage(settings.getStopLossPercentage())
                .baseTicker(baseTicker)
                .inverse(settings.isInverse())
                .trailingStopPercentage(settings.getTrailingStopPercentage())
                .trailingStopEnabled(settings.isTrailingStopEnabled())
                .trailingWindowMinutes(settings.getTrailingWindowMinutes())
                .brokerId(settings.getBrokerId())
                .holdingQuantity(settings.getHoldingQuantity())
                .build();

        return tradingTargetPort.save(toUpdate);
    }

    @Override
    public void setActive(Long userId, String ticker, boolean active) {
        TradingTarget item = tradingTargetPort.findByUserIdAndTicker(userId, ticker);
        if (item == null) {
            throw new IllegalStateException("사용자에게 등록되지 않은 티커");
        }
        tradingTargetPort.save(item.toBuilder().active(active).build());
    }
}

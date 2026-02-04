package com.example.stocktrading.strategy.application.service;

import com.example.stocktrading.common.StockConst;
import com.example.stocktrading.strategy.application.port.in.WatchlistUseCase;
import com.example.stocktrading.strategy.application.port.out.WatchlistPort;
import com.example.stocktrading.strategy.application.port.out.WatchlistPort.WatchlistItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WatchlistService implements WatchlistUseCase {

    private final WatchlistPort watchlistPort;

    @Override
    public WatchlistItem addToWatchlist(Long userId, String ticker) {
        StockConst.VALIDATE_TICKER.accept(ticker);

        Optional<WatchlistItem> existing = watchlistPort.findByUserIdAndTicker(userId, ticker);
        if (existing.isPresent()) {
            return existing.get();
        }

        WatchlistItem newItem = new WatchlistItem(
                null,
                userId,
                ticker,
                false,  // 자동매매 활성화여부
                60,     // 매수 기준 %
                60,     // 매도 기준 %
                "3.0",  // 손실 % 나면 매도할 기준
                null,
                false,  // 인버스 여부
                4       // 학습 년 수
        );

        return watchlistPort.save(newItem);
    }

    @Override
    public void removeFromWatchlist(Long userId, Long watchlistId) {
        watchlistPort.findById(watchlistId).ifPresent(item -> {
            if (item.userId().equals(userId)) {
                watchlistPort.delete(item);
            }
        });
    }

    @Override
    public List<WatchlistItem> getWatchlist(Long userId) {
        return watchlistPort.findByUserId(userId);
    }

    @Override
    public Optional<WatchlistItem> getWatchlistItem(Long id) {
        return watchlistPort.findById(id);
    }

    @Override
    public WatchlistItem updateSettings(Long id, Integer buyThreshold, Integer sellThreshold,
                                         String stopLossPercentage, String baseTicker,
                                         Boolean isInverse, Integer trainingPeriodYears) {
        WatchlistItem item = watchlistPort.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist item not found"));


        WatchlistItem updated = new WatchlistItem(
                item.id(),
                item.userId(),
                item.ticker(),
                item.isActive(),
                buyThreshold != null ? buyThreshold : item.buyThreshold(),
                sellThreshold != null ? sellThreshold : item.sellThreshold(),
                stopLossPercentage != null ? stopLossPercentage : item.stopLossPercentage(),
                baseTicker != null ? (baseTicker.isEmpty() ? null : baseTicker.toUpperCase()) : item.baseTicker(),
                baseTicker != null ? (isInverse != null ? isInverse : false) : item.isInverse(),
                trainingPeriodYears != null ? trainingPeriodYears : item.trainingPeriodYears()
        );

        return watchlistPort.save(updated);
    }

    @Override
    public void setActive(Long userId, String ticker, boolean active) {
        watchlistPort.findByUserIdAndTicker(userId, ticker)
                .ifPresent(item -> watchlistPort.save(item.withActive(active)));
    }
}

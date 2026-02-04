package com.example.stocktrading.strategy.application.port.in;

import com.example.stocktrading.strategy.application.port.out.WatchlistPort.WatchlistItem;

import java.util.List;
import java.util.Optional;

public interface WatchlistUseCase {

    WatchlistItem addToWatchlist(Long userId, String ticker);

    void removeFromWatchlist(Long userId, Long watchlistId);

    List<WatchlistItem> getWatchlist(Long userId);

    Optional<WatchlistItem> getWatchlistItem(Long id);

    WatchlistItem updateSettings(Long id, Integer buyThreshold, Integer sellThreshold,
                                  String stopLossPercentage, String baseTicker,
                                  Boolean isInverse, Integer trainingPeriodYears);

    void setActive(Long userId, String ticker, boolean active);
}

package com.example.stocktrading.strategy.adapter.in.web;

import com.example.stocktrading.common.ApiResponse;
import com.example.stocktrading.common.security.AuthContext;
import com.example.stocktrading.common.security.RequireAuth;
import com.example.stocktrading.strategy.application.port.in.AssetUseCase;
import com.example.stocktrading.strategy.application.port.in.WatchlistUseCase;
import com.example.stocktrading.strategy.application.port.out.WatchlistPort.WatchlistItem;
import com.example.stocktrading.strategy.domain.Asset;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@RequireAuth
public class StockController {

    private final WatchlistUseCase watchlistUseCase;
    private final AssetUseCase assetUseCase;

    @GetMapping
    public ApiResponse<List<StockResponse>> getStocks() {
        Long userId = AuthContext.getUserId();
        List<WatchlistItem> watchlist = watchlistUseCase.getWatchlist(userId);

        Asset asset = assetUseCase.getAccountAsset(userId);
        Map<String, Asset.OwnedStock> tickerToStock = asset.getOwnedStocks().stream()
                .collect(Collectors.toMap(Asset.OwnedStock::getStockCode, Function.identity(), (a, b) -> a));

        List<StockResponse> stocks = watchlist.stream().map(item -> {
            Optional<Asset.OwnedStock> ownedStock = Optional.ofNullable(tickerToStock.get(item.ticker()));

            return StockResponse.builder()
                    .id(item.id())
                    .ticker(item.ticker())
                    .currentPrice(ownedStock.map(Asset.OwnedStock::getCurrentPrice).orElse(BigDecimal.ZERO))
                    .quantity(ownedStock.map(Asset.OwnedStock::getQuantity).orElse(0))
                    .profitRate(ownedStock.map(Asset.OwnedStock::getProfitRate).orElse(BigDecimal.ZERO))
                    .isTrading(Boolean.TRUE.equals(item.isActive()))
                    .buyThreshold(Optional.ofNullable(item.buyThreshold()).orElse(60))
                    .sellThreshold(Optional.ofNullable(item.sellThreshold()).orElse(60))
                    .stopLossPercentage(Optional.ofNullable(item.stopLossPercentage()).orElse("3.0"))
                    .baseTicker(item.baseTicker())
                    .isInverse(item.isInverseOrDefault())
                    .trainingPeriodYears(item.trainingPeriodYears())
                    .build();
        }).toList();

        return ApiResponse.success(stocks);
    }

    @PostMapping
    public ApiResponse<StockResponse> addStock(@RequestBody AddStockRequest request) {
        Long userId = AuthContext.getUserId();
        WatchlistItem item = watchlistUseCase.addToWatchlist(userId, request.ticker());

        return ApiResponse.success(StockResponse.builder()
                .id(item.id())
                .ticker(item.ticker())
                .currentPrice(BigDecimal.ZERO)
                .quantity(0)
                .profitRate(BigDecimal.ZERO)
                .isTrading(false)
                .buyThreshold(60)
                .sellThreshold(60)
                .stopLossPercentage("3.0")
                .baseTicker(null)
                .isInverse(false)
                .trainingPeriodYears(4)
                .build());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> removeStock(@PathVariable Long id) {
        Long userId = AuthContext.getUserId();
        watchlistUseCase.getWatchlistItem(id).ifPresent(item -> {
            watchlistUseCase.removeFromWatchlist(userId, id);
        });
        return ApiResponse.success(null);
    }

    @PatchMapping("/{id}/thresholds")
    public ApiResponse<StockResponse> updateThresholds(@PathVariable Long id,
            @RequestBody UpdateThresholdRequest request) {

        WatchlistItem item = watchlistUseCase.updateSettings(
                id,
                request.buyThreshold(),
                request.sellThreshold(),
                request.stopLossPercentage(),
                request.baseTicker(),
                request.isInverse(),
                request.trainingPeriodYears()
        );

        return ApiResponse.success(StockResponse.builder()
                .id(item.id())
                .ticker(item.ticker())
                .buyThreshold(item.buyThreshold())
                .sellThreshold(item.sellThreshold())
                .stopLossPercentage(item.stopLossPercentage())
                .baseTicker(item.baseTicker())
                .isInverse(item.isInverseOrDefault())
                .trainingPeriodYears(item.trainingPeriodYears())
                .build());
    }

    @PatchMapping("/{ticker}/trading")
    public ApiResponse<StockResponse> toggleTrading(@PathVariable String ticker,
            @RequestParam boolean active) {
        Long userId = AuthContext.getUserId();
        watchlistUseCase.setActive(userId, ticker, active);
        return ApiResponse.success(StockResponse.builder()
                .ticker(ticker)
                .isTrading(active)
                .build());
    }

    public record AddStockRequest(String ticker) {
    }

    public record UpdateThresholdRequest(
            Integer buyThreshold,
            Integer sellThreshold,
            String stopLossPercentage,
            String baseTicker,
            Boolean isInverse,
            Integer trainingPeriodYears
    ) {
    }

    @Getter
    @Builder
    public static class StockResponse {
        private Long id;
        private String ticker;
        private BigDecimal currentPrice;
        private Integer quantity;
        private BigDecimal profitRate;
        @JsonProperty("isTrading")
        private boolean isTrading;
        private Integer buyThreshold;
        private Integer sellThreshold;
        private String stopLossPercentage;
        private String baseTicker;
        @JsonProperty("isInverse")
        private boolean isInverse;
        private Integer trainingPeriodYears;
    }
}

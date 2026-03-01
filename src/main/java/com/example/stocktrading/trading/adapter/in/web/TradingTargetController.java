package com.example.stocktrading.trading.adapter.in.web;

import com.example.stocktrading.common.ApiResponse;
import com.example.stocktrading.common.security.AuthContext;
import com.example.stocktrading.common.security.RequireAuth;
import com.example.stocktrading.trading.application.port.in.TradingTargetUseCase;
import com.example.stocktrading.trading.domain.TradingTarget;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/trading-target")
@RequiredArgsConstructor
@RequireAuth
public class TradingTargetController {

    private final TradingTargetUseCase tradingTargetUseCase;

    @GetMapping
    public ApiResponse<List<TradingTargetResponse>> getAll() {
        Long userId = AuthContext.getUserId();
        List<TradingTargetResponse> targets = tradingTargetUseCase.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
        return ApiResponse.success(targets);
    }

    @PostMapping
    public ApiResponse<TradingTargetResponse> add(@RequestBody AddRequest request) {
        Long userId = AuthContext.getUserId();
        TradingTarget item = tradingTargetUseCase.add(userId, request.ticker(), request.brokerId());
        return ApiResponse.success(toResponse(item));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        Long userId = AuthContext.getUserId();
        tradingTargetUseCase.remove(userId, id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<TradingTargetResponse> update(@PathVariable Long id,
                                                     @RequestBody UpdateRequest request) {

        TradingTarget settings = TradingTarget.builder()
                .buyThreshold(request.buyThreshold())
                .sellThreshold(request.sellThreshold())
                .stopLossPercentage(new BigDecimal(request.stopLossPercentage()))
                .baseTicker(request.baseTicker())
                .inverse(request.isInverse())
                .trailingStopPercentage(new BigDecimal(request.trailingStopPercentage()))
                .trailingStopEnabled(request.trailingStopEnabled())
                .trailingWindowMinutes(request.trailingWindowMinutes())
                .brokerId(request.brokerId())
                .profitAtr(request.profitAtr())
                .stopAtr(request.stopAtr())
                .maxHolding(request.maxHolding())
                .minThreshold(request.minThreshold())
                .trainingPeriodYears(request.trainingPeriodYears())
                .tuningTrials(request.tuningTrials())
                .build();

        TradingTarget item = tradingTargetUseCase.updateSettings(id, settings);
        return ApiResponse.success(toResponse(item));
    }

    @PatchMapping("/{ticker}/trading")
    public ApiResponse<TradingTargetResponse> toggleTrading(@PathVariable String ticker,
                                                            @RequestParam boolean active) {
        Long userId = AuthContext.getUserId();
        tradingTargetUseCase.setActive(userId, ticker, active);
        return ApiResponse.success(TradingTargetResponse.builder()
                .ticker(ticker)
                .isTrading(active)
                .build());
    }

    private TradingTargetResponse toResponse(TradingTarget item) {
        return TradingTargetResponse.builder()
                .id(item.getId())
                .ticker(item.getTicker())
                .isTrading(item.isActive())
                .buyThreshold(item.getBuyThreshold())
                .sellThreshold(item.getSellThreshold())
                .stopLossPercentage(item.getStopLossPercentage().toPlainString())
                .baseTicker(item.getBaseTicker())
                .isInverse(item.isInverse())
                .trailingStopPercentage(item.getTrailingStopPercentage().toPlainString())
                .trailingStopEnabled(item.isTrailingStopEnabled())
                .trailingWindowMinutes(item.getTrailingWindowMinutes())
                .brokerId(item.getBrokerId())
                .profitAtr(item.getProfitAtr())
                .stopAtr(item.getStopAtr())
                .maxHolding(item.getMaxHolding())
                .minThreshold(item.getMinThreshold())
                .trainingPeriodYears(item.getTrainingPeriodYears())
                .tuningTrials(item.getTuningTrials())
                .build();
    }

    public record AddRequest(String ticker, Long brokerId) {
    }

    public record UpdateRequest(
            int buyThreshold,
            int sellThreshold,
            String stopLossPercentage,
            String baseTicker,
            boolean isInverse,
            String trailingStopPercentage,
            boolean trailingStopEnabled,
            int trailingWindowMinutes,
            Long brokerId,
            double profitAtr,
            double stopAtr,
            int maxHolding,
            double minThreshold,
            int trainingPeriodYears,
            int tuningTrials
    ) {
    }

    @Getter
    @Builder
    public static class TradingTargetResponse {
        private Long id;
        private String ticker;
        @JsonProperty("isTrading")
        private boolean isTrading;
        private Integer buyThreshold;
        private Integer sellThreshold;
        private String stopLossPercentage;
        private String baseTicker;
        @JsonProperty("isInverse")
        private boolean isInverse;
        private String trailingStopPercentage;
        @JsonProperty("trailingStopEnabled")
        private boolean trailingStopEnabled;
        private Integer trailingWindowMinutes;
        private Long brokerId;
        private Double profitAtr;
        private Double stopAtr;
        private Integer maxHolding;
        private Double minThreshold;
        private Integer trainingPeriodYears;
        private Integer tuningTrials;
    }
}

package com.example.stocktrading.strategy.adapter.in.web;

import com.example.stocktrading.common.ApiResponse;
import com.example.stocktrading.common.security.AuthContext;
import com.example.stocktrading.common.security.RequireAuth;
import com.example.stocktrading.strategy.application.port.in.TradeLogUseCase;
import com.example.stocktrading.strategy.application.port.in.TradeLogUseCase.ProfitStats;
import com.example.stocktrading.strategy.domain.TradeLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/trade-log")
@RequiredArgsConstructor
@RequireAuth
public class TradeLogController {

    private final TradeLogUseCase tradeLogUseCase;

    @GetMapping("/recent")
    public ApiResponse<List<TradeLog>> getRecentLogs() {
        Long userId = AuthContext.getUserId();
        return ApiResponse.success(tradeLogUseCase.getRecentTradeLogs(userId));
    }

    @GetMapping("/stats")
    public ApiResponse<ProfitStatsResponse> getProfitStats() {
        Long userId = AuthContext.getUserId();
        ProfitStats stats = tradeLogUseCase.calculateProfitStats(userId);
        return ApiResponse.success(new ProfitStatsResponse(stats.realizedProfit()));
    }

    public record ProfitStatsResponse(BigDecimal realizedProfit) {
    }
}

package com.example.stocktrading.trading.adapter.in.web;

import com.example.stocktrading.common.ApiResponse;
import com.example.stocktrading.common.security.AuthContext;
import com.example.stocktrading.common.security.RequireAuth;
import com.example.stocktrading.trading.application.port.in.AiModelUseCase;
import com.example.stocktrading.trading.domain.TrainingHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@RequireAuth
public class AiModelController {

    private final AiModelUseCase aiModelUseCase;

    @PostMapping("/{ticker}/train")
    public ApiResponse<Void> trainAi(@PathVariable String ticker) {
        Long userId = AuthContext.getUserId();
        aiModelUseCase.trainAi(userId, ticker);
        return ApiResponse.success();
    }

    @DeleteMapping("/{ticker}/train")
    public ApiResponse<Void> resetTrainingStatus(@PathVariable String ticker) {
        Long userId = AuthContext.getUserId();
        aiModelUseCase.deleteTraining(userId, ticker);
        return ApiResponse.success();
    }

    @GetMapping("/{ticker}/logs")
    public ApiResponse<String> getTrainingLog(@PathVariable String ticker) {
        Long userId = AuthContext.getUserId();
        String content = aiModelUseCase.getTrainingLog(userId, ticker);
        return ApiResponse.success(content);
    }

    @PostMapping("/{ticker}/training-status")
    public ApiResponse<TrainingHistory> checkAndUpdateTrainingStatus(@PathVariable String ticker) {
        Long userId = AuthContext.getUserId();
        TrainingHistory history = aiModelUseCase.checkAndUpdateTrainingStatus(userId, ticker);
        return ApiResponse.success(history);
    }
}

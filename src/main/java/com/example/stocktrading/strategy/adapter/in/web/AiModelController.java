package com.example.stocktrading.strategy.adapter.in.web;

import com.example.stocktrading.common.ApiResponse;
import com.example.stocktrading.common.security.AuthContext;
import com.example.stocktrading.common.security.RequireAuth;
import com.example.stocktrading.strategy.application.port.in.AiModelUseCase;
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
        aiModelUseCase.deleteTraining(ticker);
        return ApiResponse.success();
    }

    @GetMapping("/{ticker}/logs")
    public ApiResponse<String> getTrainingLog(@PathVariable String ticker) {
        String content = aiModelUseCase.getTrainingLog(ticker);
        return ApiResponse.success(content);
    }

    @GetMapping("/{ticker}/status")
    public ApiResponse<AiModelUseCase.TrainingStatusInfo> getTrainingStatus(@PathVariable String ticker) {
        AiModelUseCase.TrainingStatusInfo status = aiModelUseCase.getTrainingStatus(ticker);
        return ApiResponse.success(status);
    }
}

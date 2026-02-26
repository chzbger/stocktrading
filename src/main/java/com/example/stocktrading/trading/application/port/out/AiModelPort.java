package com.example.stocktrading.trading.application.port.out;

import java.util.List;
import com.example.stocktrading.trading.domain.StockCandle;

public interface AiModelPort {

    PredictionResult predict(String ticker, String strategy, Long userId,
                              List<StockCandle> minuteStockCandles, List<StockCandle> fiveMinStockCandles,
                              int buyThreshold, int sellThreshold);

    String trainModel(String ticker, Long userId);

    record PredictionResult(
            int prediction,      // 0=HOLD, 1=BUY, 2=SELL
            double confidence,
            List<Double> probabilities
    ) {
        public String predictionLabel() {
            return switch (prediction) {
                case 1 -> "BUY";
                case 2 -> "SELL";
                default -> "HOLD";
            };
        }
    }

    TrainingJobStatus getTrainingStatus(String ticker, Long userId);

    String getTrainingLog(String ticker, Long userId);

    void deleteModel(String ticker, Long userId);

    record TrainingJobStatus(String status, String errorMessage) {
        public boolean isCompleted() {
            return "completed".equals(status);
        }
        public boolean isFailed() {
            return "failed".equals(status);
        }
        public boolean isError() {
            return "error".equals(status) || "unknown".equals(status);
        }
        public boolean isRunning() {
            return "running".equals(status) || "pending".equals(status);
        }
    }
}

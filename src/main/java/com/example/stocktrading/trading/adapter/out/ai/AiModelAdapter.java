package com.example.stocktrading.trading.adapter.out.ai;

import com.example.stocktrading.trading.application.port.out.AiModelPort;
import com.example.stocktrading.trading.domain.StockCandle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiModelAdapter implements AiModelPort {

    private final RestClient restClient;

    public AiModelAdapter(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public PredictionResult predict(String ticker, String strategy, Long userId,
                                     List<StockCandle> minuteStockCandles, List<StockCandle> fiveMinStockCandles,
                                     int buyThreshold, int sellThreshold) {
        if (minuteStockCandles == null || minuteStockCandles.isEmpty()
                || fiveMinStockCandles == null || fiveMinStockCandles.isEmpty()) {
            throw new RuntimeException("[Ai] minute and 5min candles required");
        }

        try {
            List<Map<String, Object>> minuteData = mapCandles(minuteStockCandles);
            List<Map<String, Object>> fiveMinData = mapCandles(fiveMinStockCandles);

            if (minuteData.isEmpty() || fiveMinData.isEmpty()) {
                log.warn("[AI] Insufficient data for {}: minute={}, 5min={}. Defaulting to HOLD.",
                        ticker, minuteData.size(), fiveMinData.size());
                return new PredictionResult(0, 0.0, List.of(1.0, 0.0, 0.0));
            }

            Map<String, Object> request = new HashMap<>();
            request.put("ticker", ticker);
            request.put("strategy", "scalping");
            if (userId != null) {
                request.put("user_id", userId);
            }
            request.put("minute_candles", minuteData);
            request.put("fivemin_candles", fiveMinData);
            request.put("min_buy_threshold", buyThreshold / 100.0);
            request.put("min_sell_threshold", sellThreshold / 100.0);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/predict")
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("prediction")) {
                int prediction = (Integer) response.get("prediction");

                @SuppressWarnings("unchecked")
                List<Double> probs = (List<Double>) response.get("probabilities");
                double confidence = response.containsKey("confidence")
                        ? ((Number) response.get("confidence")).doubleValue() : 0.0;

                String probStr = probs != null
                        ? String.format("HOLD=%.1f%% BUY=%.1f%% SELL=%.1f%%",
                        probs.get(0) * 100, probs.get(1) * 100, probs.get(2) * 100)
                        : "N/A";

                log.info("[AI] {} -> {} | {} (conf={}, B>{}%, S>{}%, data: 1m={}, 5m={})",
                        ticker, prediction == 0 ? "HOLD" : prediction == 1 ? "BUY" : "SELL",
                        probStr, String.format("%.2f", confidence), buyThreshold, sellThreshold,
                        minuteStockCandles.size(), fiveMinStockCandles.size());

                return new PredictionResult(prediction, confidence, probs);
            }

        } catch (Exception e) {
            log.error("[AI] Prediction failed for {}, defaulting to HOLD", ticker, e);
        }

        return new PredictionResult(0, 0.0, List.of(1.0, 0.0, 0.0));
    }

    private List<Map<String, Object>> mapCandles(List<StockCandle> stockCandles) {
        return stockCandles.stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("timestamp", c.getTimestamp().toString());
            map.put("open", c.getOpen());
            map.put("high", c.getHigh());
            map.put("low", c.getLow());
            map.put("close", c.getClose());
            map.put("volume", c.getVolume());
            return map;
        }).toList();
    }

    @Override
    public String trainModel(String ticker, Long userId) {
        log.info("[AI] Starting training via API for ticker: {} user: {}", ticker, userId);

        try {
            Map<String, Object> request = new HashMap<>();
            if (userId != null) {
                request.put("user_id", userId);
            }
            request.put("enable_tuning", true);
            request.put("tuning_trials", 30);
            request.put("multitimeframe", true);
            request.put("ensemble", true);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/train/{ticker}", ticker)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                throw new RuntimeException("Training API returned null response");
            }

            String jobId = (String) response.get("job_id");
            String status = (String) response.get("status");
            log.info("[AI] Training job queued: jobId={}, status={}", jobId, status);

            return jobId;
        } catch (Exception e) {
            log.error("[AI] Training API call failed", e);
            throw new RuntimeException("Training execution failed", e);
        }
    }

    @Override
    public AiModelPort.TrainingJobStatus getTrainingStatus(String ticker, Long userId) {
        try {
            String uri = userId != null
                    ? "/train/" + ticker + "/status?user_id=" + userId
                    : "/train/" + ticker + "/status";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                return new AiModelPort.TrainingJobStatus("unknown", null);
            }

            String status = (String) response.get("status");
            String errorMessage = (String) response.get("error_message");
            return new AiModelPort.TrainingJobStatus(status, errorMessage);
        } catch (Exception e) {
            log.error("[AI] Failed to get training status for {} (user={}): {}", ticker, userId, e.getMessage());
            return new AiModelPort.TrainingJobStatus("error", e.getMessage());
        }
    }

    @Override
    public String getTrainingLog(String ticker, Long userId) {
        try {
            String uri = userId != null
                    ? "/train/" + ticker + "/logs?user_id=" + userId
                    : "/train/" + ticker + "/logs";

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("content")) {
                return (String) response.get("content");
            }
            return "No log content available";
        } catch (Exception e) {
            log.error("[AI] Failed training log " + ticker, e);
            return "Failed to retrieve log: " + e.getMessage();
        }
    }

    @Override
    public void deleteModel(String ticker, Long userId) {
        try {
            String uri = userId != null
                    ? "/models/" + ticker + "?user_id=" + userId
                    : "/models/" + ticker;

            restClient.delete()
                    .uri(uri)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[AI] Deleted model files for {} (user={})", ticker, userId);
        } catch (Exception e) {
            log.warn("[AI] Failed to delete model files for {}: {}", ticker, e.getMessage());
        }
    }
}

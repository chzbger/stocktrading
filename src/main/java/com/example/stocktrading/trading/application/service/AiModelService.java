package com.example.stocktrading.trading.application.service;

import com.example.stocktrading.common.StockConst;
import com.example.stocktrading.trading.application.port.in.AiModelUseCase;
import com.example.stocktrading.trading.application.port.out.AiModelPort;
import com.example.stocktrading.trading.application.port.out.AiTrainingHistoryPort;
import com.example.stocktrading.trading.application.port.out.TradingTargetPort;
import com.example.stocktrading.trading.domain.TradingTarget;
import com.example.stocktrading.trading.domain.TrainingHistory;
import com.example.stocktrading.trading.domain.TrainingHistory.TrainingStatus;
import com.example.stocktrading.user.application.port.out.NotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiModelService implements AiModelUseCase {

    private final TradingTargetPort tradingTargetPort;
    private final AiTrainingHistoryPort aiTrainingHistoryPort;
    private final AiModelPort aiModelPort;
    private final NotificationPort notificationPort;

    @Override
    public void trainAi(Long userId, String ticker) {
        StockConst.VALIDATE_TICKER.accept(ticker);
        TradingTarget item = tradingTargetPort.findByUserIdAndTicker(userId, ticker);
        if (item == null) {
            throw new IllegalStateException("사용자에게 등록되지 않은 티커입니다.");
        }

        // 1. PENDING 상태로 저장 (UK 위반 → 하루 1번 초과)
        String today = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        TrainingHistory pending = TrainingHistory.builder()
                .ticker(ticker).trainDate(today).userId(userId)
                .status(TrainingStatus.PENDING)
                .build();
        TrainingHistory saved = aiTrainingHistoryPort.save(pending);

        // 2. 같은 티커의 진행중 레코드 조회 → 내 id 외에 있으면 중복 학습 방지
        List<TrainingHistory> ingHistoryList = aiTrainingHistoryPort.findIngHistoryByUserIdAndTicker(userId, ticker);
        boolean hasOther = ingHistoryList.stream()
                .anyMatch(h -> !h.getId().equals(saved.getId()));
        if (hasOther) {
            aiTrainingHistoryPort.deleteById(saved.getId());
            throw new IllegalStateException("다른 학습이 진행 중입니다.");
        }

        // 3. Python 학습 요청 → TRAINING 상태 변경
        log.info("[AiModelService] Training for: {} (User: {})", ticker, userId);
        try {
            AiModelPort.TrainingParams params = new AiModelPort.TrainingParams(
                    item.getProfitAtr(), item.getStopAtr(), item.getMaxHolding(),
                    item.getMinThreshold(), item.getTrainingPeriodYears(), item.getTuningTrials()
            );
            aiModelPort.trainModel(ticker, userId, params);
            aiTrainingHistoryPort.save(saved.withStatus(TrainingStatus.TRAINING));
            log.info("[AiModelService] Training started for {}", ticker);
        } catch (Exception e) {
            log.error("[AiModelService] Training failed to start: {}", ticker, e);
            aiTrainingHistoryPort.save(saved.withStatus(TrainingStatus.FAILED, e.getMessage()));
        }
    }

    @Override
    public TrainingHistory checkAndUpdateTrainingStatus(Long userId, String ticker) {
        TrainingHistory history = aiTrainingHistoryPort.findLatestByTickerAndUserId(ticker, userId);
        if (history == null || history.getStatus() != TrainingStatus.TRAINING) {
            return history;
        }

        AiModelPort.TrainingJobStatus pythonStatus = aiModelPort.getTrainingStatus(ticker, userId);
        log.info("[AiModelService] Checked training status for {} (user={}): {}", ticker, userId, pythonStatus.status());

        if (pythonStatus.isCompleted()) {
            aiTrainingHistoryPort.save(history.withStatus(TrainingStatus.COMPLETED));
            log.info("[AiModelService] Training completed for {}", ticker);

            // Auto-update thresholds from recommended values
            try {
                AiModelPort.RecommendedThresholds recommended =
                        aiModelPort.getRecommendedThresholds(ticker, userId);

                TradingTarget target = tradingTargetPort.findByUserIdAndTicker(userId, ticker);
                if (target != null) {
                    int oldBuy = target.getBuyThreshold();
                    int oldSell = target.getSellThreshold();
                    TradingTarget updated = target.toBuilder()
                            .buyThreshold(recommended.buyThreshold())
                            .sellThreshold(recommended.sellThreshold())
                            .build();
                    tradingTargetPort.save(updated);

                    log.info("[AiModelService] Auto-updated thresholds for {}: buy {}→{}, sell {}→{}",
                            ticker, oldBuy, recommended.buyThreshold(), oldSell, recommended.sellThreshold());

                    String text = String.format(
                            "[Trading] %s 학습 완료. Threshold 자동 업데이트: BUY %d→%d, SELL %d→%d",
                            ticker, oldBuy, recommended.buyThreshold(), oldSell, recommended.sellThreshold());
                    notificationPort.sendMessage(userId, text);
                }
            } catch (Exception e) {
                log.warn("[AiModelService] Failed to auto-update thresholds for {}: {}", ticker, e.getMessage());
                String text = String.format("[Trading] %s 모델 학습이 완료되었습니다. (Threshold 자동 업데이트 실패)", ticker);
                notificationPort.sendMessage(userId, text);
            }
        } else if (pythonStatus.isFailed() || pythonStatus.isError()) {
            String errorMsg = pythonStatus.errorMessage() != null ? pythonStatus.errorMessage() : "Training failed or lost";
            aiTrainingHistoryPort.save(history.withStatus(TrainingStatus.FAILED, errorMsg));
            log.warn("[AiModelService] Training failed for {}: {}", ticker, errorMsg);
        }

        return history;
    }

    @Override
    public void deleteTraining(Long userId, String ticker) {
        List<TrainingHistory> history = aiTrainingHistoryPort.findByTickerAndUserId(ticker, userId);
        if (!history.isEmpty()) {
            aiTrainingHistoryPort.deleteAll(history);
        }
        aiModelPort.deleteModel(ticker, userId);
    }

    @Override
    public String getTrainingLog(Long userId, String ticker) {
        StockConst.VALIDATE_TICKER.accept(ticker);
        return aiModelPort.getTrainingLog(ticker, userId);
    }
}

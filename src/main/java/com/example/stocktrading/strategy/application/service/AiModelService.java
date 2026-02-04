package com.example.stocktrading.strategy.application.service;

import com.example.stocktrading.common.StockConst;
import com.example.stocktrading.strategy.application.port.in.AiModelUseCase;
import com.example.stocktrading.strategy.application.port.out.AiModelPort;
import com.example.stocktrading.strategy.application.port.out.AiTrainingHistoryPort;
import com.example.stocktrading.strategy.application.port.out.AiTrainingHistoryPort.TrainingHistory;
import com.example.stocktrading.strategy.application.port.out.AiTrainingHistoryPort.TrainingStatus;
import com.example.stocktrading.strategy.application.port.out.WatchlistPort;
import com.example.stocktrading.strategy.application.port.out.WatchlistPort.WatchlistItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiModelService implements AiModelUseCase {

    private final WatchlistPort watchlistPort;
    private final AiTrainingHistoryPort aiTrainingHistoryPort;
    private final AiModelPort aiModelPort;

    @Override
    public void trainAi(Long userId, String ticker) {
        StockConst.VALIDATE_TICKER.accept(ticker);
        String today = ZonedDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int requested = aiTrainingHistoryPort.requestTraining(ticker, today);
        if (requested == 0) {
            return;
        }

        int claimed = aiTrainingHistoryPort.claimPendingTraining(ticker, today);
        if (claimed == 0) {
            return;
        }

        log.info("[AiModelService] Training claimed for: {} (User: {})", ticker, userId);

        WatchlistItem watchlistItem = watchlistPort.findByUserIdAndTicker(userId, ticker).orElse(null);
        final int periodYears = watchlistItem != null ? watchlistItem.trainingPeriodYears() : 4;

        CompletableFuture.runAsync(() -> {
            TrainingHistory history = aiTrainingHistoryPort.findByTickerAndTrainDate(ticker, today);
            try {
                aiModelPort.trainModel(ticker, periodYears);
                aiTrainingHistoryPort.save(history.withStatus(TrainingStatus.COMPLETED, "Training successful"));
            } catch (Exception e) {
                log.error("[AiModelService] Training failed " + ticker, e);
                aiTrainingHistoryPort.save(history.withStatus(TrainingStatus.FAILED, e.getMessage()));
            }
        });
    }

    @Override
    public TrainingStatusInfo getTrainingStatus(String ticker) {
        String today = ZonedDateTime.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        TrainingHistory todayHis = aiTrainingHistoryPort.findByTickerAndTrainDate(ticker, today);
        if (todayHis != null) {
            return new TrainingStatusInfo(todayHis.id(), todayHis.ticker(), todayHis.trainDate(), todayHis.status().name(), todayHis.message());
        }

        TrainingHistory lastHis = aiTrainingHistoryPort.findLatestByTicker(ticker);
        if (lastHis != null) {
            return new TrainingStatusInfo(lastHis.id(), lastHis.ticker(), lastHis.trainDate(), lastHis.status().name(), lastHis.message());
        }
        return null;
    }

    @Override
    public void deleteTraining(String ticker) {
        List<TrainingHistory> history = aiTrainingHistoryPort.findByTicker(ticker);
        if (!history.isEmpty()) {
            aiTrainingHistoryPort.deleteAll(history);
        }
    }

    @Override
    public String getTrainingLog(String ticker) {
        StockConst.VALIDATE_TICKER.accept(ticker);
        return aiModelPort.getTrainingLog(ticker);
    }
}

package com.example.stocktrading.user.adapter.out.notification;

import com.example.stocktrading.user.application.port.out.NotificationClient;
import com.example.stocktrading.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramClient implements NotificationClient {
    private final RestClient restClient;

    @Override
    public void sendMessage(User user, String text) {
        if (user.getTelegramBotToken() == null || user.getTelegramBotToken().isBlank()
                || user.getTelegramChatId() == null || user.getTelegramChatId().isBlank()) {
            return;
        }

        try {
            restClient.post()
                    .uri("https://api.telegram.org/bot{token}/sendMessage", user.getTelegramBotToken())
                    .body(Map.of(
                            "chat_id", user.getTelegramChatId(),
                            "text", text))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[Telegram] Notification sent to chatId={}", user.getTelegramChatId());
        } catch (Exception e) {
            log.warn("[Telegram] Failed to send notification: {}", e.getMessage());
        }
    }
}

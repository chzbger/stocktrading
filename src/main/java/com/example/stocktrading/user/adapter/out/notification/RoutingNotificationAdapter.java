package com.example.stocktrading.user.adapter.out.notification;

import com.example.stocktrading.user.application.port.out.NotificationClient;
import com.example.stocktrading.user.application.port.out.NotificationPort;
import com.example.stocktrading.user.application.port.out.UserPort;
import com.example.stocktrading.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RoutingNotificationAdapter implements NotificationPort {
    private final UserPort userPort;
    private final TelegramClient telegramClient;

    private NotificationClient getClient(User user) {
        return telegramClient;
    }

    @Override
    public void sendMessage(Long userId, String text) {
        User user = userPort.findById(userId).orElse(null);
        if (user == null || Boolean.FALSE.equals(user.getNotificationEnabled())) {
            return;
        }
        getClient(user).sendMessage(user, text);
    }
}

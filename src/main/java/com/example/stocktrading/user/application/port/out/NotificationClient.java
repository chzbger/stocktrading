package com.example.stocktrading.user.application.port.out;

import com.example.stocktrading.user.domain.User;

public interface NotificationClient {
    void sendMessage(User user, String text);
}

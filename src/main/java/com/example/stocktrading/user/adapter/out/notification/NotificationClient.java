package com.example.stocktrading.user.adapter.out.notification;

import com.example.stocktrading.user.domain.User;

public interface NotificationClient {
    void sendMessage(User user, String text);
}

package com.example.stocktrading.user.application.port.out;

public interface NotificationPort {
    void sendMessage(Long userId, String text);
}

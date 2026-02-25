package com.example.stocktrading.user.application.port.in;

import com.example.stocktrading.user.domain.BrokerType;
import com.example.stocktrading.user.domain.User;

import java.time.LocalTime;
import java.util.List;

public interface UserUseCase {

    void register(String username, String password);

    LoginResult login(String username, String password);

    User getUser(Long userId);

    List<UserSummary> getAllUsers();

    void approveUser(Long userId);

    void deleteUser(Long userId);

    void setActiveBroker(Long userId, Long brokerInfoId);

    void addBrokerInfo(Long userId, BrokerType brokerType, String appKey, String appSecret, String accountNumber);

    void deleteBrokerInfo(Long userId, Long brokerInfoId);

    void updateTradingHours(Long userId, LocalTime start, LocalTime end);

    void updateNotificationSettings(Long userId, boolean enabled, String botToken, String chatId);

    record LoginResult(String token, String role) {
    }

    record UserSummary(Long id, String username, String status) {
    }
}

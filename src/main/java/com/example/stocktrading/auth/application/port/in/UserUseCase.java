package com.example.stocktrading.auth.application.port.in;

import com.example.stocktrading.auth.domain.BrokerType;
import com.example.stocktrading.auth.domain.User;

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

    record LoginResult(String token, String role) {
    }

    record UserSummary(Long id, String username, String status) {
    }
}

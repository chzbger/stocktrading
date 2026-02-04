package com.example.stocktrading.auth.application.service;

import com.example.stocktrading.auth.application.port.in.UserUseCase;
import com.example.stocktrading.auth.application.port.out.BrokerInfoPort;
import com.example.stocktrading.auth.application.port.out.PasswordEncoderPort;
import com.example.stocktrading.auth.application.port.out.UserPort;
import com.example.stocktrading.auth.domain.BrokerInfo;
import com.example.stocktrading.auth.domain.BrokerType;
import com.example.stocktrading.auth.domain.User;
import com.example.stocktrading.auth.domain.UserRole;
import com.example.stocktrading.common.error.CustomBadRequestException;
import com.example.stocktrading.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final UserPort userPort;
    private final BrokerInfoPort brokerInfoPort;
    private final PasswordEncoderPort passwordEncoder;
    private final JwtService jwtService;

    @Override
    public void register(String username, String password) {
        if (username == null || username.length() < 4) {
            throw new CustomBadRequestException("아이디는 4자리 이상이어야 합니다.");
        }
        if (password == null || password.length() < 4) {
            throw new CustomBadRequestException("비밀번호는 4자리 이상이어야 합니다.");
        }
        if (userPort.existsByUsername(username)) {
            throw new CustomBadRequestException("이미 존재하는 아이디입니다.");
        }

        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .status(User.UserStatus.PENDING)
                .role(UserRole.ROLE_USER)
                .build();

        userPort.save(user);
    }

    @Override
    public LoginResult login(String username, String password) {
        User user = userPort.findByUsername(username)
                .orElseThrow(() -> new CustomBadRequestException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new CustomBadRequestException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        if (!user.isActive()) {
            throw new CustomBadRequestException("가입 승인 대기 중입니다. 관리자에게 문의하세요.");
        }

        String token = jwtService.generateToken(user.getUserId(), user.getRole().name());
        return new LoginResult(token, user.getRole().name());
    }

    @Override
    public User getUser(Long userId) {
        return userPort.findById(userId)
                .orElseThrow(() -> new CustomBadRequestException("User not found"));
    }

    @Override
    public List<UserSummary> getAllUsers() {
        return userPort.findAll().stream()
                .map(u -> new UserSummary(u.getUserId(), u.getUsername(), u.getStatus().name()))
                .toList();
    }

    @Override
    public void approveUser(Long userId) {
        User user = userPort.findById(userId)
                .orElseThrow(() -> new CustomBadRequestException("User not found"));

        user.setStatus(User.UserStatus.ACTIVE);
        userPort.save(user);
    }

    @Override
    public void deleteUser(Long userId) {
        userPort.deleteById(userId);
    }

    @Override
    public void setActiveBroker(Long userId, Long brokerInfoId) {
        User user = userPort.findById(userId)
                .orElseThrow(() -> new CustomBadRequestException("User not found"));

        BrokerInfo brokerInfo = brokerInfoPort.findById(brokerInfoId)
                .orElseThrow(() -> new CustomBadRequestException("Broker not found"));

        if (!brokerInfo.getUserId().equals(userId)) {
            throw new CustomBadRequestException("Unauthorized");
        }

        user.setActiveBrokerId(brokerInfoId);
        userPort.save(user);
    }

    @Override
    public void addBrokerInfo(Long userId, BrokerType brokerType, String appKey, String appSecret, String accountNumber) {
        User user = userPort.findById(userId)
                .orElseThrow(() -> new CustomBadRequestException("User not found"));

        BrokerInfo brokerInfo = new BrokerInfo(
                null,
                userId,
                brokerType,
                appKey,
                appSecret,
                accountNumber
        );
        BrokerInfo saved = brokerInfoPort.save(brokerInfo);

        if (user.getActiveBrokerId() == null) {
            user.setActiveBrokerId(saved.getId());
            userPort.save(user);
        }
    }

    @Override
    public void deleteBrokerInfo(Long userId, Long brokerInfoId) {
        BrokerInfo brokerInfo = brokerInfoPort.findById(brokerInfoId)
                .orElseThrow(() -> new CustomBadRequestException("Broker not found"));

        if (!brokerInfo.getUserId().equals(userId)) {
            throw new CustomBadRequestException("Unauthorized");
        }

        User user = userPort.findById(userId)
                .orElseThrow(() -> new CustomBadRequestException("User not found"));

        if (brokerInfoId.equals(user.getActiveBrokerId())) {
            user.setActiveBrokerId(null);
            userPort.save(user);
        }

        brokerInfoPort.deleteById(brokerInfoId);
    }

    @Override
    public void updateTradingHours(Long userId, LocalTime start, LocalTime end) {
        User user = userPort.findById(userId)
                .orElseThrow(() -> new CustomBadRequestException("User not found"));

        user.setTradingStartTime(start);
        user.setTradingEndTime(end);
        userPort.save(user);
    }
}

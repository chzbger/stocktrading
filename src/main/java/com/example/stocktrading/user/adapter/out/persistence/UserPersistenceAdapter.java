package com.example.stocktrading.user.adapter.out.persistence;

import com.example.stocktrading.user.application.port.out.UserPort;
import com.example.stocktrading.user.domain.BrokerInfo;
import com.example.stocktrading.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPersistenceAdapter implements UserPort {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = mapToEntity(user);
        UserEntity saved = userRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username).map(this::mapToDomain);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll().stream()
                .map(this::mapToDomain)
                .toList();
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public User mapToDomain(UserEntity entity) {
        if (entity == null) return null;
        return User.builder()
                .userId(entity.getId())
                .username(entity.getUsername())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRole())
                .status(switch (entity.getStatus()) {
                    case PENDING -> User.UserStatus.PENDING;
                    case ACTIVE -> User.UserStatus.ACTIVE;
                })
                .activeBrokerId(entity.getActiveBrokerId())
                .tradingStartTime(entity.getTradingStartTime())
                .tradingEndTime(entity.getTradingEndTime())
                .notificationEnabled(entity.getNotificationEnabled())
                .telegramBotToken(entity.getTelegramBotToken())
                .telegramChatId(entity.getTelegramChatId())
                .brokerInfos(entity.getBrokerInfos().stream()
                        .map(bi -> new BrokerInfo(
                                bi.getId(),
                                bi.getUser().getId(),
                                bi.getBrokerType(),
                                bi.getAppKey(),
                                bi.getAppSecret(),
                                bi.getAccountNumber()))
                        .toList())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserEntity mapToEntity(User user) {
        if (user == null) return null;
        UserEntity entity = new UserEntity();
        if (user.getUserId() != null) {
            entity = userRepository.findById(user.getUserId()).orElse(new UserEntity());
        }
        entity.setUsername(user.getUsername());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setRole(user.getRole());
        entity.setStatus(switch (user.getStatus() == null ? User.UserStatus.PENDING : user.getStatus()) {
            case PENDING -> UserEntity.UserStatus.PENDING;
            case ACTIVE -> UserEntity.UserStatus.ACTIVE;
        });
        entity.setActiveBrokerId(user.getActiveBrokerId());
        entity.setTradingStartTime(user.getTradingStartTime());
        entity.setTradingEndTime(user.getTradingEndTime());
        entity.setNotificationEnabled(user.getNotificationEnabled());
        entity.setTelegramBotToken(user.getTelegramBotToken());
        entity.setTelegramChatId(user.getTelegramChatId());
        return entity;
    }
}

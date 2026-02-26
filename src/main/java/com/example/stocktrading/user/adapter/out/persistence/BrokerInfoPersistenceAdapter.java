package com.example.stocktrading.user.adapter.out.persistence;

import com.example.stocktrading.user.application.port.out.BrokerInfoPort;
import com.example.stocktrading.user.domain.BrokerInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrokerInfoPersistenceAdapter implements BrokerInfoPort {

    private final BrokerInfoRepository brokerInfoRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BrokerInfo save(BrokerInfo brokerInfo) {
        BrokerInfoEntity entity = mapToEntity(brokerInfo);
        BrokerInfoEntity saved = brokerInfoRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<BrokerInfo> findById(Long id) {
        return brokerInfoRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        brokerInfoRepository.deleteById(id);
    }

    private BrokerInfo mapToDomain(BrokerInfoEntity entity) {
        if (entity == null) return null;
        return BrokerInfo.builder()
                .id(entity.getId())
                .userId(entity.getUser().getId())
                .brokerType(entity.getBrokerType())
                .appKey(entity.getAppKey())
                .appSecret(entity.getAppSecret())
                .accountNumber(entity.getAccountNumber())
                .build();
    }

    private BrokerInfoEntity mapToEntity(BrokerInfo brokerInfo) {
        if (brokerInfo == null) return null;
        BrokerInfoEntity entity = new BrokerInfoEntity();
        if (brokerInfo.getId() != null) {
            entity = brokerInfoRepository.findById(brokerInfo.getId()).orElse(new BrokerInfoEntity());
        }

        entity.setUser(userRepository.getReferenceById(brokerInfo.getUserId()));
        entity.setBrokerType(brokerInfo.getBrokerType());
        entity.setAppKey(brokerInfo.getAppKey());
        entity.setAppSecret(brokerInfo.getAppSecret());
        entity.setAccountNumber(brokerInfo.getAccountNumber());
        return entity;
    }
}

package com.example.stocktrading.user.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BrokerInfoRepository extends JpaRepository<BrokerInfoEntity, Long> {
}

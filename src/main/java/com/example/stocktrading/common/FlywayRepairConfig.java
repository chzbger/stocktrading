package com.example.stocktrading.common;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairConfig {

//    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Checksum 불일치 문제를 해결하기 위해 스키마 히스토리를 로컬 파일 기준으로 갱신
            flyway.repair();
            // 새로운 마이그레이션이 있다면 실행
            flyway.migrate();
        };
    }
}

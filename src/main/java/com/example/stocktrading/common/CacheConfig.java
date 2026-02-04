package com.example.stocktrading.common;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String KIS_TOKEN_CACHE = "kisTokenCache";
    public static final String USER_ASSET_CACHE = "userAssetCache";

    @Bean
    public CacheManager ehCacheManager() throws Exception {
        CachingProvider cachingProvider = Caching.getCachingProvider();
        return cachingProvider.getCacheManager(
                new ClassPathResource("ehcache.xml").getURI(),
                getClass().getClassLoader()
        );
    }

    @Bean
    public org.springframework.cache.CacheManager cacheManager(CacheManager ehCacheManager) {
        return new JCacheCacheManager(ehCacheManager);
    }
}

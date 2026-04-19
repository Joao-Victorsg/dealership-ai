package br.com.dealership.car.api.config;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RedisConfig {

    private static final Duration CAR_BY_ID_TTL = Duration.ofHours(24);
    private static final Duration CAR_LISTINGS_TTL = Duration.ofMinutes(10);

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var jsonSerializer = GenericJacksonJsonRedisSerializer.builder().build();

        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(CAR_BY_ID_TTL)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("car-by-id", defaultConfig.entryTtl(CAR_BY_ID_TTL))
                .withCacheConfiguration("car-listings", defaultConfig.entryTtl(CAR_LISTINGS_TTL))
                .build();
    }
}


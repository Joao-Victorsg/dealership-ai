package br.com.dealership.clientapi.config;

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

    private static final Duration CLIENT_TTL = Duration.ofHours(24);

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        final var jsonSerializer = GenericJacksonJsonRedisSerializer.builder().build();

        final var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(CLIENT_TTL)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("clients", defaultConfig.entryTtl(CLIENT_TTL))
                .build();
    }
}

package br.com.dealership.dealershibff.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;

// Spring Boot 4.x removed Spring Session auto-configuration, so we must explicitly enable it.
// maxInactiveIntervalInSeconds must match server.servlet.session.timeout (1800s = 30 min).
// redisNamespace is resolved via EmbeddedValueResolver from application.properties.
@EnableRedisHttpSession(
        maxInactiveIntervalInSeconds = 1800,
        redisNamespace = "${spring.session.redis.namespace:spring:session}"
)
@Configuration
@EnableCaching
public class RedisConfig {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        final var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();
        final var jsonSerializer = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(ptv)
                .build();
        final var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("car-by-id", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("car-listings", defaultConfig.entryTtl(Duration.ofMinutes(5)))
                .build();
    }
}

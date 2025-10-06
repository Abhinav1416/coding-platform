package com.Abhinav.backend.config;

import com.Abhinav.backend.features.match.repository.LiveMatchStateRepository;
import com.Abhinav.backend.features.match.service.MatchExpirationHandler;
import com.Abhinav.backend.features.problem.service.ProblemService;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@Slf4j
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }


    @Bean
    public RedisMessageListenerContainer keyExpirationListenerContainer(
            RedisConnectionFactory connectionFactory,
            ProblemService problemService,
            MatchExpirationHandler matchExpirationHandler // Inject our new handler
    ) {
        RedisMessageListenerContainer listenerContainer = new RedisMessageListenerContainer();
        listenerContainer.setConnectionFactory(connectionFactory);

        // Your existing listener (no changes needed here)
        listenerContainer.addMessageListener(
                new RedisExpirationListener(listenerContainer, problemService),
                new PatternTopic("__keyevent@*__:expired")
        );

        // Add our new listener for match expirations
        listenerContainer.addMessageListener(
                new MatchKeyListener(matchExpirationHandler),
                new PatternTopic("__keyevent@*__:expired")
        );
        log.info("Subscribed to Redis key expiration events.");

        return listenerContainer;
    }


    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        GenericJackson2JsonRedisSerializer redisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisSerializer));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(
                "userProfiles",
                defaultConfig.entryTtl(Duration.ofMinutes(30))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    // Inner class to handle match expiration messages
    private static class MatchKeyListener implements MessageListener {
        private final MatchExpirationHandler matchExpirationHandler;

        public MatchKeyListener(MatchExpirationHandler matchExpirationHandler) {
            this.matchExpirationHandler = matchExpirationHandler;
        }

        @Override
        public void onMessage(Message message, byte[] pattern) {
            String expiredKey = new String(message.getBody());

            if (expiredKey.startsWith(LiveMatchStateRepository.LIVE_MATCH_KEY_PREFIX)) {
                try {
                    String matchIdStr = expiredKey.substring(LiveMatchStateRepository.LIVE_MATCH_KEY_PREFIX.length());
                    UUID matchId = UUID.fromString(matchIdStr);
                    // Delegate the actual work to our transactional service
                    matchExpirationHandler.handleExpiration(matchId);
                } catch (Exception e) {
                    log.error("Error processing expired match key: '{}'", expiredKey, e);
                }
            }
        }
    }
}
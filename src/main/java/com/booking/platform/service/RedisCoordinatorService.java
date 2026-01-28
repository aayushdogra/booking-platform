package com.booking.platform.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisCoordinatorService {

    private final StringRedisTemplate redisTemplate;

    public RedisCoordinatorService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Event Idempotency
    public boolean markEventIfAbsent(String key, Duration ttl) {
        Boolean success = redisTemplate
                .opsForValue()
                .setIfAbsent(key, "PROCESSED", ttl);

        return Boolean.TRUE.equals(success);
    }

    // Retry Counter
    public long incrementRetry(String key, Duration ttlIfFirst) {
        Long value = redisTemplate.opsForValue().increment(key);

        if (value != null && value == 1) {
            redisTemplate.expire(key, ttlIfFirst);
        }

        return value == null ? 0 : value;
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void setExpiry(String key, Duration ttl) {
        redisTemplate.expire(key, ttl);
    }

    // Lightweight Distributed Lock
    public boolean acquireLock(String key, Duration ttl) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "LOCKED", ttl);

        return Boolean.TRUE.equals(success);
    }

    public void releaseLock(String key) {
        redisTemplate.delete(key);
    }

    // Utility
    public Optional<String> get(String key) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }
}

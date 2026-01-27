package com.booking.platform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class RedisCoordinatorService {

    @Autowired
    private final StringRedisTemplate redisTemplate;

    public RedisCoordinatorService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Event Idempotency
    public boolean markEventIfAbsent(String key, Duration ttl) {
        Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);

        return Boolean.TRUE.equals(success);
    }

    // Retry Counter
    public long incrementRetry(String key) {
        Long value =  redisTemplate.opsForValue().increment(key);
        return value == null ? 0 : value;
    }

    public void setExpiry(String key, Duration ttl) {
        redisTemplate.expire(key, ttl);
    }

    // Lightweight Lock
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

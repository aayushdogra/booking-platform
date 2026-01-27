package com.booking.platform.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisGuardServiceImpl implements RedisGuardService {

    private final StringRedisTemplate redisTemplate;

    public RedisGuardServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean acquireEventLock(String key, Duration ttl) {

        Boolean success = redisTemplate
                .opsForValue()
                .setIfAbsent(key, "1", ttl);

        return Boolean.TRUE.equals(success);
    }

    @Override
    public long incrementCounter(String key, Duration ttl) {
        Long value = redisTemplate.opsForValue().increment(key);

        if (value != null && value == 1) {
            redisTemplate.expire(key, ttl);
        }

        return value == null ? 0 : value;
    }
}

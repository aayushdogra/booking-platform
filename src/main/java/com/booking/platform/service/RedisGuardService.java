package com.booking.platform.service;

import java.time.Duration;

public interface RedisGuardService {
    boolean acquireEventLock(String key, Duration ttl);
    long incrementCounter(String key, Duration ttl);
    void delete(String key);
}

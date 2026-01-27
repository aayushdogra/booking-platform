package com.booking.platform.service;

public interface HealthService {
    boolean isDatabaseUp();
    boolean isRedisUp();
    boolean isPaymentsUp();
}
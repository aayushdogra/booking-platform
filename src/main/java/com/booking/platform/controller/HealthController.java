package com.booking.platform.controller;

import com.booking.platform.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/bookingservice")
    public Map<String, String> appHealth() {
        return Map.of("status", "UP",
                "message", "Booking Service is Up");
    }

    @GetMapping("/db")
    public Map<String, String> dbHealth() {
        boolean up = healthService.isDatabaseUp();

        if(up) {
            return Map.of("status", "UP",
                    "database", "PostgreSQL",
                    "message", "Database is up");
        }

        return Map.of("status", "DOWN",
                "error", "Database not reachable");
    }

    @GetMapping("/redis")
    public Map<String, String> redisHealth() {
        boolean up = healthService.isRedisUp();

        if (up) {
            return Map.of(
                    "status", "UP",
                    "redis", "Connected"
            );
        }

        return Map.of(
                "status", "DOWN",
                "error", "Redis not reachable"
        );
    }

    @GetMapping("/paymentservice")
    public Map<String, String> paymentsHealth() {
        boolean up = healthService.isPaymentsUp();

        if (up) {
            return Map.of(
                    "status", "UP",
                    "paymentGateway", "Available"
            );
        }

        return Map.of(
                "status", "DOWN",
                "error", "Payment gateway unavailable"
        );
    }
}
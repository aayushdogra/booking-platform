package com.booking.platform.controller;

import com.booking.platform.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public Map<String, String> appHealth() {
        return Map.of("status", "UP",
                "message", "Booking Service is Up");
    }

    @GetMapping("/health/db")
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
}
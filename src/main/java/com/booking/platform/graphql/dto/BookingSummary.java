package com.booking.platform.graphql.dto;

import java.time.Instant;

public record BookingSummary(Long id, String status, String userName, Instant createdAt) {
}

package com.booking.platform.domain;

import java.util.UUID;

public class Hotel {
    private final UUID id;
    private final String name;
    private final String city;

    public Hotel(UUID id, String name, String city) {
        this.id = id;
        this.name = name;
        this.city = city;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }
}
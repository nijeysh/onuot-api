package com.onuot.api.modules.weather.domain.model;

public record WeatherLocation(
        double latitude,
        double longitude,
        String city,
        String district
) {}

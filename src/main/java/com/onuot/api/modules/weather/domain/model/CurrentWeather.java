package com.onuot.api.modules.weather.domain.model;

public record CurrentWeather(
        double temperature,
        double feelsLike,
        int humidity,
        double windSpeed,
        WeatherCondition condition,
        String description
) {}

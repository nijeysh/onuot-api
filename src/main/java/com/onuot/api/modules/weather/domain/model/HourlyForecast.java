package com.onuot.api.modules.weather.domain.model;

import java.time.LocalDateTime;

public record HourlyForecast(
        LocalDateTime time,
        double temperature,
        WeatherCondition condition,
        double precipitation
) {}

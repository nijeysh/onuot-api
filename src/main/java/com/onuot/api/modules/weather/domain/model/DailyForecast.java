package com.onuot.api.modules.weather.domain.model;

import java.time.LocalDate;

public record DailyForecast(
        LocalDate date,
        double minTemp,
        double maxTemp,
        WeatherCondition condition,
        double precipitation
) {}

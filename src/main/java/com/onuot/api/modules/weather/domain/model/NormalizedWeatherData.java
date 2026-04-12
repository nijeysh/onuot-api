package com.onuot.api.modules.weather.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public record NormalizedWeatherData(
        WeatherLocation location,
        CurrentWeather current,
        List<HourlyForecast> hourly,
        List<DailyForecast> daily,
        AirQuality airQuality,
        Double uvIndex,
        LocalDateTime updatedAt
) {

    public NormalizedWeatherData mergeWith(NormalizedWeatherData other) {
        if (other == null) {
            return this;
        }
        return new NormalizedWeatherData(
                this.location != null ? this.location : other.location,
                this.current != null ? this.current : other.current,
                this.hourly != null && !this.hourly.isEmpty() ? this.hourly : other.hourly,
                this.daily != null && !this.daily.isEmpty() ? this.daily : other.daily,
                this.airQuality != null ? this.airQuality : other.airQuality,
                this.uvIndex != null ? this.uvIndex : other.uvIndex,
                this.updatedAt != null ? this.updatedAt : other.updatedAt
        );
    }
}

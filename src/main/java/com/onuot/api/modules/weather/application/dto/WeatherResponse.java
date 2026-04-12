package com.onuot.api.modules.weather.application.dto;

import com.onuot.api.modules.weather.domain.model.NormalizedWeatherData;

import java.time.format.DateTimeFormatter;
import java.util.List;

public record WeatherResponse(
        LocationDto location,
        CurrentDto current,
        List<HourlyDto> hourly,
        List<DailyDto> daily,
        String updatedAt
) {

    public record LocationDto(
            double latitude,
            double longitude,
            String city,
            String district
    ) {}

    public record CurrentDto(
            double temperature,
            double feelsLike,
            int humidity,
            double windSpeed,
            String condition,
            String description,
            Integer pm10,
            Integer pm25,
            Double uvIndex
    ) {}

    public record HourlyDto(
            String time,
            double temperature,
            String condition,
            double precipitation
    ) {}

    public record DailyDto(
            String date,
            double minTemp,
            double maxTemp,
            String condition,
            double precipitation
    ) {}

    public static WeatherResponse from(NormalizedWeatherData data) {
        var location = data.location() != null
                ? new LocationDto(
                    data.location().latitude(),
                    data.location().longitude(),
                    data.location().city(),
                    data.location().district())
                : null;

        var current = data.current() != null
                ? new CurrentDto(
                    data.current().temperature(),
                    data.current().feelsLike(),
                    data.current().humidity(),
                    data.current().windSpeed(),
                    data.current().condition() != null ? data.current().condition().name() : null,
                    data.current().description(),
                    data.airQuality() != null ? data.airQuality().pm10() : null,
                    data.airQuality() != null ? data.airQuality().pm25() : null,
                    data.uvIndex())
                : null;

        var hourly = data.hourly() != null
                ? data.hourly().stream()
                    .map(h -> new HourlyDto(
                            h.time().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                            h.temperature(),
                            h.condition() != null ? h.condition().name() : null,
                            h.precipitation()))
                    .toList()
                : List.<HourlyDto>of();

        var daily = data.daily() != null
                ? data.daily().stream()
                    .map(d -> new DailyDto(
                            d.date().format(DateTimeFormatter.ISO_LOCAL_DATE),
                            d.minTemp(),
                            d.maxTemp(),
                            d.condition() != null ? d.condition().name() : null,
                            d.precipitation()))
                    .toList()
                : List.<DailyDto>of();

        String updatedAt = data.updatedAt() != null
                ? data.updatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;

        return new WeatherResponse(location, current, hourly, daily, updatedAt);
    }
}

package com.onuot.api.domain.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class WeatherResponse {

    private String region;
    private double temperature;
    private int humidity;
    private String sky;
    private String precipitation;
    private String forecastDate;
    private String forecastTime;
}

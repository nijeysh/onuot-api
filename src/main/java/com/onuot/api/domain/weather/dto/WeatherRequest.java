package com.onuot.api.domain.weather.dto;

import lombok.Getter;

@Getter
public class WeatherRequest {

    private String regionCode;
    private double latitude;
    private double longitude;
}

package com.onuot.api.domain.weather.service;

import com.onuot.api.domain.weather.client.WeatherApiClient;
import com.onuot.api.domain.weather.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherApiClient weatherApiClient;

    public WeatherResponse getWeather(double latitude, double longitude) {
        // TODO: Redis 캐싱 적용
        return weatherApiClient.getWeather(latitude, longitude);
    }
}

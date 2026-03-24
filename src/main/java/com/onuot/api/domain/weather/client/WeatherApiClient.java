package com.onuot.api.domain.weather.client;

import com.onuot.api.domain.weather.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class WeatherApiClient {

    private final RestClient restClient;

    @Value("${weather.api.base-url}")
    private String baseUrl;

    @Value("${weather.api.service-key}")
    private String serviceKey;

    // TODO: 기상청 API 결정 후 구현
    public WeatherResponse getWeather(double latitude, double longitude) {
        return WeatherResponse.builder()
                .region("서울")
                .temperature(20.0)
                .humidity(60)
                .sky("맑음")
                .precipitation("없음")
                .forecastDate("20260324")
                .forecastTime("1200")
                .build();
    }
}

package com.onuot.api.domain.weather.controller;

import com.onuot.api.domain.weather.dto.WeatherResponse;
import com.onuot.api.domain.weather.service.WeatherService;
import com.onuot.api.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public ApiResponse<WeatherResponse> getWeather(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        return ApiResponse.ok(weatherService.getWeather(latitude, longitude));
    }
}

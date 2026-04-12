package com.onuot.api.modules.weather.interfaces;

import com.onuot.api.global.common.ApiResponse;
import com.onuot.api.modules.weather.application.WeatherAggregationService;
import com.onuot.api.modules.weather.application.dto.WeatherResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherAggregationService weatherAggregationService;

    @GetMapping
    public ResponseEntity<ApiResponse<WeatherResponse>> getWeather(
            @RequestParam double latitude,
            @RequestParam double longitude) {
        WeatherResponse response = weatherAggregationService.getWeather(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}

package com.onuot.api.modules.weather.application;

import com.onuot.api.global.exception.BusinessException;
import com.onuot.api.global.exception.ErrorCode;
import com.onuot.api.modules.weather.application.dto.WeatherResponse;
import com.onuot.api.modules.weather.domain.WeatherProvider;
import com.onuot.api.modules.weather.domain.model.NormalizedWeatherData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final List<WeatherProvider> providers;
    private final WeatherCacheService cacheService;

    public WeatherResponse getWeather(double latitude, double longitude) {
        return cacheService.get(latitude, longitude)
                .map(WeatherResponse::from)
                .orElseGet(() -> {
                    NormalizedWeatherData data = fetchAndMerge(latitude, longitude);
                    cacheService.put(latitude, longitude, data);
                    return WeatherResponse.from(data);
                });
    }

    private NormalizedWeatherData fetchAndMerge(double latitude, double longitude) {
        return providers.stream()
                .map(provider -> tryFetch(provider, latitude, longitude))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce(NormalizedWeatherData::mergeWith)
                .orElseThrow(() -> new BusinessException(ErrorCode.WEATHER_ALL_PROVIDERS_FAILED));
    }

    private Optional<NormalizedWeatherData> tryFetch(WeatherProvider provider, double lat, double lon) {
        try {
            return Optional.of(provider.fetch(lat, lon));
        } catch (Exception e) {
            log.warn("Provider 호출 실패: {}", provider.getType(), e);
            return Optional.empty();
        }
    }
}

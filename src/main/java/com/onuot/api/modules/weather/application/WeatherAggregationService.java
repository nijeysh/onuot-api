package com.onuot.api.modules.weather.application;

import com.onuot.api.modules.weather.application.dto.WeatherResponse;
import com.onuot.api.modules.weather.domain.WeatherDataCapability;
import com.onuot.api.modules.weather.domain.WeatherProvider;
import com.onuot.api.modules.weather.domain.WeatherProviderType;
import com.onuot.api.modules.weather.domain.model.NormalizedWeatherData;
import com.onuot.api.modules.weather.domain.model.WeatherLocation;
import com.onuot.api.modules.weather.infrastructure.config.WeatherProviderConfig;
import com.onuot.api.global.exception.BusinessException;
import com.onuot.api.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WeatherAggregationService {

    private final Map<WeatherProviderType, WeatherProvider> providerMap;
    private final WeatherCacheService cacheService;
    private final WeatherProviderConfig config;

    public WeatherAggregationService(
            List<WeatherProvider> providers,
            WeatherCacheService cacheService,
            WeatherProviderConfig config) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(WeatherProvider::getType, p -> p));
        this.cacheService = cacheService;
        this.config = config;
    }

    public WeatherResponse getWeather(double latitude, double longitude) {
        Optional<NormalizedWeatherData> cached = cacheService.get(latitude, longitude);
        if (cached.isPresent()) {
            log.debug("캐시 hit: ({}, {})", latitude, longitude);
            return WeatherResponse.from(cached.get());
        }

        Map<WeatherProviderType, Set<WeatherDataCapability>> providerTasks = resolveProviderTasks();

        Map<WeatherProviderType, NormalizedWeatherData> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<WeatherProviderType, Set<WeatherDataCapability>> entry : providerTasks.entrySet()) {
            WeatherProviderType type = entry.getKey();
            WeatherProvider provider = providerMap.get(type);

            if (provider == null) {
                log.warn("Provider를 찾을 수 없음: {}", type);
                continue;
            }

            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    NormalizedWeatherData data = provider.fetch(latitude, longitude);
                    if (data != null) {
                        results.put(type, data);
                    }
                } catch (Exception e) {
                    log.error("Provider {} 호출 실패", type, e);
                    tryFallback(type, latitude, longitude, results);
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        if (results.isEmpty()) {
            throw new BusinessException(ErrorCode.WEATHER_ALL_PROVIDERS_FAILED);
        }

        NormalizedWeatherData merged = mergeResults(results, latitude, longitude);
        cacheService.put(latitude, longitude, merged);
        return WeatherResponse.from(merged);
    }

    private Map<WeatherProviderType, Set<WeatherDataCapability>> resolveProviderTasks() {
        Map<WeatherProviderType, Set<WeatherDataCapability>> tasks = new LinkedHashMap<>();
        WeatherProviderConfig.StrategyConfig strategy = config.getStrategy();

        addTask(tasks, strategy.getCurrent(), WeatherDataCapability.CURRENT);
        addTask(tasks, strategy.getHourlyForecast(), WeatherDataCapability.HOURLY_FORECAST);
        addTask(tasks, strategy.getDailyForecast(), WeatherDataCapability.DAILY_FORECAST);
        addTask(tasks, strategy.getAirQuality(), WeatherDataCapability.AIR_QUALITY);
        addTask(tasks, strategy.getUvIndex(), WeatherDataCapability.UV_INDEX);

        return tasks;
    }

    private void addTask(Map<WeatherProviderType, Set<WeatherDataCapability>> tasks,
                         WeatherProviderType type, WeatherDataCapability capability) {
        if (type != null && isProviderEnabled(type)) {
            tasks.computeIfAbsent(type, k -> new LinkedHashSet<>()).add(capability);
        }
    }

    private boolean isProviderEnabled(WeatherProviderType type) {
        WeatherProviderConfig.ProviderConfig providerConfig = config.getProvider(type);
        return providerConfig != null && providerConfig.isEnabled();
    }

    private void tryFallback(WeatherProviderType failedType, double latitude, double longitude,
                             Map<WeatherProviderType, NormalizedWeatherData> results) {
        WeatherProviderType fallbackType = config.getStrategy().getFallback();
        if (fallbackType == null || fallbackType == failedType || results.containsKey(fallbackType)) {
            return;
        }

        WeatherProvider fallback = providerMap.get(fallbackType);
        if (fallback == null || !isProviderEnabled(fallbackType)) {
            return;
        }

        try {
            log.info("Fallback provider {} 시도 (실패: {})", fallbackType, failedType);
            NormalizedWeatherData data = fallback.fetch(latitude, longitude);
            if (data != null) {
                results.put(fallbackType, data);
            }
        } catch (Exception e) {
            log.error("Fallback provider {} 호출도 실패", fallbackType, e);
        }
    }

    private NormalizedWeatherData mergeResults(Map<WeatherProviderType, NormalizedWeatherData> results,
                                               double latitude, double longitude) {
        NormalizedWeatherData merged = new NormalizedWeatherData(
                new WeatherLocation(latitude, longitude, null, null),
                null, null, null, null, null, LocalDateTime.now()
        );

        for (NormalizedWeatherData data : results.values()) {
            merged = merged.mergeWith(data);
        }

        return merged;
    }
}

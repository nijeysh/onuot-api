package com.onuot.api.modules.weather.application;

import tools.jackson.databind.ObjectMapper;
import com.onuot.api.modules.weather.domain.model.NormalizedWeatherData;
import com.onuot.api.modules.weather.infrastructure.config.WeatherProviderConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final WeatherProviderConfig config;

    private static final String KEY_PREFIX = "weather:";

    public Optional<NormalizedWeatherData> get(double latitude, double longitude) {
        String key = buildKey(latitude, longitude);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, NormalizedWeatherData.class));
        } catch (Exception e) {
            log.warn("Redis 캐시 조회 실패: key={}", key, e);
            return Optional.empty();
        }
    }

    public void put(double latitude, double longitude, NormalizedWeatherData data) {
        String key = buildKey(latitude, longitude);
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, config.getCache().getCurrentTtl(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis 캐시 저장 실패: key={}", key, e);
        }
    }

    private String buildKey(double latitude, double longitude) {
        return KEY_PREFIX + String.format("%.2f:%.2f", latitude, longitude);
    }
}

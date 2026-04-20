package com.onuot.api.modules.weather.infrastructure.config;

import com.onuot.api.modules.weather.domain.WeatherProviderType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "weather")
public class WeatherProviderConfig {

    private CacheConfig cache = new CacheConfig();
    private Map<String, ProviderConfig> providers;

    @Getter
    @Setter
    public static class CacheConfig {
        private int currentTtl = 1800;
        private int forecastTtl = 3600;
    }

    @Getter
    @Setter
    public static class ProviderConfig {
        private boolean enabled;
        private String baseUrl;
        private String serviceKey;
        private String apiKey;
    }

    public ProviderConfig getProvider(WeatherProviderType type) {
        if (providers == null) {
            return null;
        }
        return providers.get(type.name().toLowerCase());
    }
}

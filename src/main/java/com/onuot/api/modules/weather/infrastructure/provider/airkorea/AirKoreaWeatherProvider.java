package com.onuot.api.modules.weather.infrastructure.provider.airkorea;

import com.onuot.api.modules.weather.domain.WeatherDataCapability;
import com.onuot.api.modules.weather.domain.WeatherProvider;
import com.onuot.api.modules.weather.domain.WeatherProviderType;
import com.onuot.api.modules.weather.domain.model.AirQuality;
import com.onuot.api.modules.weather.domain.model.NormalizedWeatherData;
import com.onuot.api.modules.weather.infrastructure.provider.airkorea.dto.AirKoreaNearbyStationResponse;
import com.onuot.api.modules.weather.infrastructure.provider.airkorea.dto.AirKoreaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AirKoreaWeatherProvider implements WeatherProvider {

    private final AirKoreaApiClient apiClient;

    @Override
    public WeatherProviderType getType() {
        return WeatherProviderType.AIRKOREA;
    }

    @Override
    public Set<WeatherDataCapability> getCapabilities() {
        return Set.of(WeatherDataCapability.AIR_QUALITY);
    }

    @Override
    public NormalizedWeatherData fetch(double latitude, double longitude) {
        String stationName = findNearestStation(longitude, latitude);
        AirQuality airQuality = stationName != null ? fetchAirQuality(stationName) : null;

        return new NormalizedWeatherData(null, null, null, null, airQuality, null, LocalDateTime.now());
    }

    private String findNearestStation(double tmX, double tmY) {
        try {
            AirKoreaNearbyStationResponse response = apiClient.getNearbyStation(tmX, tmY);
            if (response != null && response.getResponse() != null
                    && response.getResponse().getBody() != null
                    && response.getResponse().getBody().getItems() != null
                    && !response.getResponse().getBody().getItems().isEmpty()) {
                return response.getResponse().getBody().getItems().get(0).getStationName();
            }
        } catch (Exception e) {
            log.warn("AirKorea 근접측정소 조회 실패", e);
        }
        return null;
    }

    private AirQuality fetchAirQuality(String stationName) {
        try {
            AirKoreaResponse response = apiClient.getAirQuality(stationName);
            if (response != null && response.getResponse() != null
                    && response.getResponse().getBody() != null
                    && response.getResponse().getBody().getItems() != null
                    && !response.getResponse().getBody().getItems().isEmpty()) {
                AirKoreaResponse.Item item = response.getResponse().getBody().getItems().get(0);
                return new AirQuality(parseIntSafe(item.getPm10Value()), parseIntSafe(item.getPm25Value()));
            }
        } catch (Exception e) {
            log.warn("AirKorea 대기질 조회 실패: station={}", stationName, e);
        }
        return null;
    }

    private int parseIntSafe(String value) {
        try { return value != null && !value.equals("-") ? Integer.parseInt(value) : 0; }
        catch (NumberFormatException e) { return 0; }
    }
}

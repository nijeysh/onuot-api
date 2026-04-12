package com.onuot.api.modules.weather.infrastructure.provider.openweathermap;

import com.onuot.api.modules.weather.domain.WeatherProviderType;
import com.onuot.api.modules.weather.infrastructure.config.WeatherProviderConfig;
import com.onuot.api.modules.weather.infrastructure.provider.openweathermap.dto.OwmOneCallResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class OwmApiClient {

    private final RestClient restClient;
    private final WeatherProviderConfig config;

    public OwmOneCallResponse getOneCall(double latitude, double longitude) {
        WeatherProviderConfig.ProviderConfig owmConfig = config.getProvider(WeatherProviderType.OPENWEATHERMAP);

        String url = owmConfig.getBaseUrl() + "/onecall"
                + "?lat=" + latitude
                + "&lon=" + longitude
                + "&appid=" + owmConfig.getApiKey()
                + "&units=metric"
                + "&lang=kr";

        log.debug("OWM One Call API 호출: lat={}, lon={}", latitude, longitude);

        return restClient.get().uri(url).retrieve().body(OwmOneCallResponse.class);
    }
}

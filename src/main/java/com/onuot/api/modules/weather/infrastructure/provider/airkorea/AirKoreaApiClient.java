package com.onuot.api.modules.weather.infrastructure.provider.airkorea;

import com.onuot.api.modules.weather.domain.WeatherProviderType;
import com.onuot.api.modules.weather.infrastructure.config.WeatherProviderConfig;
import com.onuot.api.modules.weather.infrastructure.provider.airkorea.dto.AirKoreaNearbyStationResponse;
import com.onuot.api.modules.weather.infrastructure.provider.airkorea.dto.AirKoreaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AirKoreaApiClient {

    private final RestClient restClient;
    private final WeatherProviderConfig config;

    private static final String NEARBY_STATION_URL =
            "https://apis.data.go.kr/B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList";

    public AirKoreaNearbyStationResponse getNearbyStation(double tmX, double tmY) {
        WeatherProviderConfig.ProviderConfig airConfig = config.getProvider(WeatherProviderType.AIRKOREA);

        String url = NEARBY_STATION_URL
                + "?serviceKey=" + airConfig.getServiceKey()
                + "&returnType=json"
                + "&tmX=" + tmX
                + "&tmY=" + tmY;

        log.debug("AirKorea 근접측정소 조회: tmX={}, tmY={}", tmX, tmY);

        return restClient.get().uri(url).retrieve().body(AirKoreaNearbyStationResponse.class);
    }

    public AirKoreaResponse getAirQuality(String stationName) {
        WeatherProviderConfig.ProviderConfig airConfig = config.getProvider(WeatherProviderType.AIRKOREA);

        String url = airConfig.getBaseUrl() + "/getMsrstnAcctoRltmMesureDnsty"
                + "?serviceKey=" + airConfig.getServiceKey()
                + "&returnType=json"
                + "&numOfRows=1"
                + "&pageNo=1"
                + "&stationName=" + stationName
                + "&dataTerm=DAILY"
                + "&ver=1.3";

        log.debug("AirKorea 대기질 조회: stationName={}", stationName);

        return restClient.get().uri(url).retrieve().body(AirKoreaResponse.class);
    }
}

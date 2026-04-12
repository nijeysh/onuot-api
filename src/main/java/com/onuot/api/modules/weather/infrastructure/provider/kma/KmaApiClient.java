package com.onuot.api.modules.weather.infrastructure.provider.kma;

import com.onuot.api.modules.weather.domain.WeatherProviderType;
import com.onuot.api.modules.weather.infrastructure.config.WeatherProviderConfig;
import com.onuot.api.modules.weather.infrastructure.provider.kma.dto.KmaApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
public class KmaApiClient {

    private final RestClient restClient;
    private final WeatherProviderConfig config;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");

    public KmaApiResponse getUltraShortNow(int nx, int ny) {
        WeatherProviderConfig.ProviderConfig kmaConfig = config.getProvider(WeatherProviderType.KMA);
        LocalDateTime now = resolveBaseTime();

        String url = kmaConfig.getBaseUrl() + "/getUltraSrtNcst"
                + "?serviceKey=" + kmaConfig.getServiceKey()
                + "&pageNo=1"
                + "&numOfRows=100"
                + "&dataType=JSON"
                + "&base_date=" + now.format(DATE_FMT)
                + "&base_time=" + now.format(TIME_FMT)
                + "&nx=" + nx
                + "&ny=" + ny;

        log.debug("KMA 초단기실황 호출: nx={}, ny={}", nx, ny);

        return restClient.get().uri(url).retrieve().body(KmaApiResponse.class);
    }

    public KmaApiResponse getShortForecast(int nx, int ny) {
        WeatherProviderConfig.ProviderConfig kmaConfig = config.getProvider(WeatherProviderType.KMA);
        LocalDateTime baseTime = resolveShortForecastBaseTime();

        String url = kmaConfig.getBaseUrl() + "/getVilageFcst"
                + "?serviceKey=" + kmaConfig.getServiceKey()
                + "&pageNo=1"
                + "&numOfRows=1000"
                + "&dataType=JSON"
                + "&base_date=" + baseTime.format(DATE_FMT)
                + "&base_time=" + baseTime.format(TIME_FMT)
                + "&nx=" + nx
                + "&ny=" + ny;

        log.debug("KMA 단기예보 호출: nx={}, ny={}, baseDate={}, baseTime={}",
                nx, ny, baseTime.format(DATE_FMT), baseTime.format(TIME_FMT));

        return restClient.get().uri(url).retrieve().body(KmaApiResponse.class);
    }

    private LocalDateTime resolveBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        if (now.getMinute() < 10) {
            now = now.minusHours(1);
        }
        return now.withMinute(0).withSecond(0).withNano(0);
    }

    private LocalDateTime resolveShortForecastBaseTime() {
        int[] baseTimes = {2, 5, 8, 11, 14, 17, 20, 23};
        LocalDateTime now = LocalDateTime.now();
        int currentHour = now.getHour();
        int currentMinute = now.getMinute();

        int selectedHour = baseTimes[0];
        LocalDateTime baseDate = now;

        for (int i = baseTimes.length - 1; i >= 0; i--) {
            if (currentHour > baseTimes[i] || (currentHour == baseTimes[i] && currentMinute >= 10)) {
                selectedHour = baseTimes[i];
                break;
            }
            if (i == 0) {
                selectedHour = baseTimes[baseTimes.length - 1];
                baseDate = now.minusDays(1);
            }
        }

        return baseDate.withHour(selectedHour).withMinute(0).withSecond(0).withNano(0);
    }
}

package com.onuot.api.modules.weather.infrastructure.provider.openweathermap;

import com.onuot.api.modules.weather.domain.WeatherDataCapability;
import com.onuot.api.modules.weather.domain.WeatherProvider;
import com.onuot.api.modules.weather.domain.WeatherProviderType;
import com.onuot.api.modules.weather.domain.model.*;
import com.onuot.api.modules.weather.infrastructure.provider.openweathermap.dto.OwmOneCallResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OwmWeatherProvider implements WeatherProvider {

    private final OwmApiClient apiClient;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public WeatherProviderType getType() {
        return WeatherProviderType.OPENWEATHERMAP;
    }

    @Override
    public Set<WeatherDataCapability> getCapabilities() {
        return Set.of(
                WeatherDataCapability.CURRENT,
                WeatherDataCapability.HOURLY_FORECAST,
                WeatherDataCapability.DAILY_FORECAST,
                WeatherDataCapability.AIR_QUALITY,
                WeatherDataCapability.UV_INDEX
        );
    }

    @Override
    public NormalizedWeatherData fetch(double latitude, double longitude) {
        OwmOneCallResponse response = apiClient.getOneCall(latitude, longitude);

        return new NormalizedWeatherData(
                new WeatherLocation(latitude, longitude, null, null),
                parseCurrent(response.getCurrent()),
                parseHourly(response.getHourly()),
                parseDaily(response.getDaily()),
                null,
                response.getCurrent() != null ? response.getCurrent().getUvi() : null,
                LocalDateTime.now()
        );
    }

    private CurrentWeather parseCurrent(OwmOneCallResponse.Current current) {
        if (current == null) return null;

        WeatherCondition condition = WeatherCondition.UNKNOWN;
        String description = "";
        if (current.getWeather() != null && !current.getWeather().isEmpty()) {
            OwmOneCallResponse.Weather w = current.getWeather().get(0);
            condition = mapWeatherIdToCondition(w.getId());
            description = w.getDescription();
        }

        return new CurrentWeather(current.getTemp(), current.getFeelsLike(),
                current.getHumidity(), current.getWindSpeed(), condition, description);
    }

    private List<HourlyForecast> parseHourly(List<OwmOneCallResponse.Hourly> hourlyList) {
        if (hourlyList == null) return List.of();

        List<HourlyForecast> result = new ArrayList<>();
        for (int i = 0; i < Math.min(hourlyList.size(), 24); i++) {
            OwmOneCallResponse.Hourly h = hourlyList.get(i);
            LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochSecond(h.getDt()), KST);
            WeatherCondition condition = WeatherCondition.UNKNOWN;
            if (h.getWeather() != null && !h.getWeather().isEmpty()) {
                condition = mapWeatherIdToCondition(h.getWeather().get(0).getId());
            }
            double precipitation = 0;
            if (h.getRain() != null) precipitation += h.getRain().getOneHour();
            if (h.getSnow() != null) precipitation += h.getSnow().getOneHour();
            result.add(new HourlyForecast(time, h.getTemp(), condition, precipitation));
        }
        return result;
    }

    private List<DailyForecast> parseDaily(List<OwmOneCallResponse.Daily> dailyList) {
        if (dailyList == null) return List.of();

        List<DailyForecast> result = new ArrayList<>();
        for (int i = 0; i < Math.min(dailyList.size(), 7); i++) {
            OwmOneCallResponse.Daily d = dailyList.get(i);
            LocalDate date = Instant.ofEpochSecond(d.getDt()).atZone(KST).toLocalDate();
            WeatherCondition condition = WeatherCondition.UNKNOWN;
            if (d.getWeather() != null && !d.getWeather().isEmpty()) {
                condition = mapWeatherIdToCondition(d.getWeather().get(0).getId());
            }
            result.add(new DailyForecast(date, d.getTemp().getMin(), d.getTemp().getMax(),
                    condition, d.getRain() + d.getSnow()));
        }
        return result;
    }

    private WeatherCondition mapWeatherIdToCondition(int id) {
        if (id >= 200 && id < 300) return WeatherCondition.THUNDERSTORM;
        if (id >= 300 && id < 400) return WeatherCondition.RAIN;
        if (id >= 500 && id < 510) return WeatherCondition.RAIN;
        if (id >= 510 && id < 600) return WeatherCondition.HEAVY_RAIN;
        if (id >= 600 && id < 700) return WeatherCondition.SNOW;
        if (id >= 700 && id < 800) return WeatherCondition.FOG;
        if (id == 800) return WeatherCondition.CLEAR;
        if (id == 801) return WeatherCondition.PARTLY_CLOUDY;
        if (id == 802) return WeatherCondition.CLOUDY;
        if (id >= 803) return WeatherCondition.OVERCAST;
        return WeatherCondition.UNKNOWN;
    }
}

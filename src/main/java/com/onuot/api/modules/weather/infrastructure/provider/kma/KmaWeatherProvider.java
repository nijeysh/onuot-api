package com.onuot.api.modules.weather.infrastructure.provider.kma;

import com.onuot.api.modules.weather.domain.WeatherDataCapability;
import com.onuot.api.modules.weather.domain.WeatherProvider;
import com.onuot.api.modules.weather.domain.WeatherProviderType;
import com.onuot.api.modules.weather.domain.model.*;
import com.onuot.api.modules.weather.infrastructure.provider.kma.dto.KmaApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KmaWeatherProvider implements WeatherProvider {

    private final KmaApiClient apiClient;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public WeatherProviderType getType() {
        return WeatherProviderType.KMA;
    }

    @Override
    public Set<WeatherDataCapability> getCapabilities() {
        return Set.of(
                WeatherDataCapability.CURRENT,
                WeatherDataCapability.HOURLY_FORECAST,
                WeatherDataCapability.DAILY_FORECAST
        );
    }

    @Override
    public NormalizedWeatherData fetch(double latitude, double longitude) {
        KmaGridConverter.Grid grid = KmaGridConverter.toGrid(latitude, longitude);
        log.debug("KMA 격자 변환: ({}, {}) → ({}, {})", latitude, longitude, grid.nx(), grid.ny());

        KmaApiResponse ultraShort = apiClient.getUltraShortNow(grid.nx(), grid.ny());
        KmaApiResponse forecast = apiClient.getShortForecast(grid.nx(), grid.ny());

        return new NormalizedWeatherData(
                new WeatherLocation(latitude, longitude, null, null),
                parseCurrentWeather(ultraShort),
                parseHourlyForecast(forecast),
                parseDailyForecast(forecast),
                null,
                null,
                LocalDateTime.now()
        );
    }

    private CurrentWeather parseCurrentWeather(KmaApiResponse response) {
        if (!isValidResponse(response)) {
            log.warn("KMA 초단기실황 응답이 비정상입니다.");
            return new CurrentWeather(0, 0, 0, 0, WeatherCondition.UNKNOWN, "데이터 없음");
        }

        Map<String, String> values = new HashMap<>();
        for (KmaApiResponse.Item item : response.getResponse().getBody().getItems().getItem()) {
            values.put(item.getCategory(), item.getObsrValue());
        }

        double temperature = parseDouble(values.get("T1H"));
        int humidity = parseInt(values.get("REH"));
        double windSpeed = parseDouble(values.get("WSD"));
        int pty = parseInt(values.get("PTY"));

        WeatherCondition condition = mapPtyToCondition(pty);
        return new CurrentWeather(temperature, temperature, humidity, windSpeed, condition, describeCondition(condition));
    }

    private List<HourlyForecast> parseHourlyForecast(KmaApiResponse response) {
        if (!isValidResponse(response)) return List.of();

        Map<String, Map<String, String>> grouped = new LinkedHashMap<>();
        for (KmaApiResponse.Item item : response.getResponse().getBody().getItems().getItem()) {
            String key = item.getFcstDate() + item.getFcstTime();
            grouped.computeIfAbsent(key, k -> new HashMap<>()).put(item.getCategory(), item.getFcstValue());
        }

        List<HourlyForecast> hourly = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : grouped.entrySet()) {
            if (hourly.size() >= 24) break;
            Map<String, String> vals = entry.getValue();
            if (!vals.containsKey("TMP")) continue;

            LocalDateTime time = LocalDateTime.parse(entry.getKey(),
                    DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            int pty = parseInt(vals.getOrDefault("PTY", "0"));
            int sky = parseInt(vals.getOrDefault("SKY", "1"));
            WeatherCondition condition = pty > 0 ? mapPtyToCondition(pty) : mapSkyToCondition(sky);

            hourly.add(new HourlyForecast(time, parseDouble(vals.get("TMP")), condition,
                    parsePrecipitation(vals.getOrDefault("PCP", "강수없음"))));
        }
        return hourly;
    }

    private List<DailyForecast> parseDailyForecast(KmaApiResponse response) {
        if (!isValidResponse(response)) return List.of();

        Map<String, Map<String, String>> dailyData = new LinkedHashMap<>();
        Map<String, List<Double>> dailyTemps = new LinkedHashMap<>();
        Map<String, Integer> dailyPty = new LinkedHashMap<>();
        Map<String, Integer> dailySky = new LinkedHashMap<>();
        Map<String, Double> dailyPcp = new LinkedHashMap<>();

        for (KmaApiResponse.Item item : response.getResponse().getBody().getItems().getItem()) {
            String date = item.getFcstDate();
            switch (item.getCategory()) {
                case "TMN" -> dailyData.computeIfAbsent(date, k -> new HashMap<>()).put("TMN", item.getFcstValue());
                case "TMX" -> dailyData.computeIfAbsent(date, k -> new HashMap<>()).put("TMX", item.getFcstValue());
                case "TMP" -> dailyTemps.computeIfAbsent(date, k -> new ArrayList<>()).add(parseDouble(item.getFcstValue()));
                case "PTY" -> { if (parseInt(item.getFcstValue()) > 0) dailyPty.merge(date, parseInt(item.getFcstValue()), (a, b) -> a); }
                case "SKY" -> dailySky.putIfAbsent(date, parseInt(item.getFcstValue()));
                case "PCP" -> dailyPcp.merge(date, parsePrecipitation(item.getFcstValue()), Double::sum);
            }
        }

        List<DailyForecast> daily = new ArrayList<>();
        Set<String> allDates = new LinkedHashSet<>();
        allDates.addAll(dailyData.keySet());
        allDates.addAll(dailyTemps.keySet());

        for (String date : allDates) {
            if (daily.size() >= 7) break;
            Map<String, String> data = dailyData.getOrDefault(date, Map.of());
            List<Double> temps = dailyTemps.getOrDefault(date, List.of());

            double minTemp, maxTemp;
            if (data.containsKey("TMN") && data.containsKey("TMX")) {
                minTemp = parseDouble(data.get("TMN"));
                maxTemp = parseDouble(data.get("TMX"));
            } else if (!temps.isEmpty()) {
                minTemp = temps.stream().mapToDouble(Double::doubleValue).min().orElse(0);
                maxTemp = temps.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            } else {
                continue;
            }

            int pty = dailyPty.getOrDefault(date, 0);
            int sky = dailySky.getOrDefault(date, 1);
            WeatherCondition condition = pty > 0 ? mapPtyToCondition(pty) : mapSkyToCondition(sky);
            daily.add(new DailyForecast(LocalDate.parse(date, DATE_FMT), minTemp, maxTemp, condition,
                    dailyPcp.getOrDefault(date, 0.0)));
        }
        return daily;
    }

    private boolean isValidResponse(KmaApiResponse response) {
        return response != null && response.getResponse() != null
                && response.getResponse().getBody() != null
                && response.getResponse().getBody().getItems() != null
                && response.getResponse().getBody().getItems().getItem() != null;
    }

    private WeatherCondition mapPtyToCondition(int pty) {
        return switch (pty) {
            case 1 -> WeatherCondition.RAIN;
            case 2 -> WeatherCondition.SLEET;
            case 3 -> WeatherCondition.SNOW;
            case 4 -> WeatherCondition.RAIN;
            default -> WeatherCondition.CLEAR;
        };
    }

    private WeatherCondition mapSkyToCondition(int sky) {
        return switch (sky) {
            case 1 -> WeatherCondition.CLEAR;
            case 3 -> WeatherCondition.PARTLY_CLOUDY;
            case 4 -> WeatherCondition.CLOUDY;
            default -> WeatherCondition.UNKNOWN;
        };
    }

    private String describeCondition(WeatherCondition condition) {
        return switch (condition) {
            case CLEAR -> "맑음";
            case PARTLY_CLOUDY -> "구름 조금";
            case CLOUDY -> "흐림";
            case OVERCAST -> "완전 흐림";
            case RAIN -> "비";
            case HEAVY_RAIN -> "폭우";
            case SNOW -> "눈";
            case SLEET -> "진눈깨비";
            case FOG -> "안개";
            case THUNDERSTORM -> "뇌우";
            case UNKNOWN -> "알 수 없음";
        };
    }

    private double parseDouble(String value) {
        try { return value != null ? Double.parseDouble(value) : 0; } catch (NumberFormatException e) { return 0; }
    }

    private int parseInt(String value) {
        try { return value != null ? Integer.parseInt(value) : 0; } catch (NumberFormatException e) { return 0; }
    }

    private double parsePrecipitation(String value) {
        if (value == null || value.equals("강수없음") || value.equals("-")) return 0;
        try { return Double.parseDouble(value.replace("mm", "").trim()); } catch (NumberFormatException e) { return 0; }
    }
}

package com.onuot.api.modules.weather.infrastructure.provider.openweathermap.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OwmOneCallResponse {

    private double lat;
    private double lon;
    private String timezone;
    private Current current;
    private List<Hourly> hourly;
    private List<Daily> daily;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Current {
        private long dt;
        private double temp;
        @JsonProperty("feels_like") private double feelsLike;
        private int humidity;
        @JsonProperty("wind_speed") private double windSpeed;
        private double uvi;
        private List<Weather> weather;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hourly {
        private long dt;
        private double temp;
        @JsonProperty("feels_like") private double feelsLike;
        private int humidity;
        @JsonProperty("wind_speed") private double windSpeed;
        private List<Weather> weather;
        private Rain rain;
        private Snow snow;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Daily {
        private long dt;
        private Temp temp;
        private int humidity;
        @JsonProperty("wind_speed") private double windSpeed;
        private List<Weather> weather;
        private double rain;
        private double snow;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Temp {
        private double min;
        private double max;
        private double day;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weather {
        private int id;
        private String main;
        private String description;
        private String icon;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rain {
        @JsonProperty("1h") private double oneHour;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snow {
        @JsonProperty("1h") private double oneHour;
    }
}

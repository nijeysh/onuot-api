package com.onuot.api.modules.weather.domain;

import com.onuot.api.modules.weather.domain.model.NormalizedWeatherData;

import java.util.Set;

public interface WeatherProvider {

    WeatherProviderType getType();

    Set<WeatherDataCapability> getCapabilities();

    NormalizedWeatherData fetch(double latitude, double longitude);
}

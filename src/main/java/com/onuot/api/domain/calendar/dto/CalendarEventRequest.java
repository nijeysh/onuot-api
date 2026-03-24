package com.onuot.api.domain.calendar.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class CalendarEventRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private LocalDate eventDate;

    private LocalTime startTime;

    private LocalTime endTime;
}

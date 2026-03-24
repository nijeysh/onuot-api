package com.onuot.api.domain.calendar.controller;

import com.onuot.api.domain.calendar.dto.CalendarEventRequest;
import com.onuot.api.domain.calendar.dto.CalendarEventResponse;
import com.onuot.api.domain.calendar.service.CalendarService;
import com.onuot.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @PostMapping("/events")
    public ApiResponse<CalendarEventResponse> createEvent(
            Authentication authentication,
            @Valid @RequestBody CalendarEventRequest request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(calendarService.createEvent(memberId, request));
    }

    @GetMapping("/events")
    public ApiResponse<List<CalendarEventResponse>> getEvents(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(calendarService.getEvents(memberId, startDate, endDate));
    }

    @PutMapping("/events/{eventId}")
    public ApiResponse<CalendarEventResponse> updateEvent(
            Authentication authentication,
            @PathVariable Long eventId,
            @Valid @RequestBody CalendarEventRequest request) {
        Long memberId = (Long) authentication.getPrincipal();
        return ApiResponse.ok(calendarService.updateEvent(memberId, eventId, request));
    }

    @DeleteMapping("/events/{eventId}")
    public ApiResponse<Void> deleteEvent(
            Authentication authentication,
            @PathVariable Long eventId) {
        Long memberId = (Long) authentication.getPrincipal();
        calendarService.deleteEvent(memberId, eventId);
        return ApiResponse.ok();
    }
}

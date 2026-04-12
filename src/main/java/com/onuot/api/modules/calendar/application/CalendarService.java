package com.onuot.api.modules.calendar.application;

import com.onuot.api.modules.calendar.application.dto.CalendarEventRequest;
import com.onuot.api.modules.calendar.application.dto.CalendarEventResponse;
import com.onuot.api.modules.calendar.domain.CalendarEvent;
import com.onuot.api.modules.calendar.domain.CalendarEventRepository;
import com.onuot.api.global.exception.BusinessException;
import com.onuot.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarService {

    private final CalendarEventRepository calendarEventRepository;

    @Transactional
    public CalendarEventResponse createEvent(Long memberId, CalendarEventRequest request) {
        CalendarEvent event = CalendarEvent.builder()
                .memberId(memberId)
                .title(request.getTitle())
                .description(request.getDescription())
                .eventDate(request.getEventDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        return CalendarEventResponse.from(calendarEventRepository.save(event));
    }

    public List<CalendarEventResponse> getEvents(Long memberId, LocalDate startDate, LocalDate endDate) {
        return calendarEventRepository
                .findByMemberIdAndEventDateBetweenOrderByEventDateAscStartTimeAsc(memberId, startDate, endDate)
                .stream()
                .map(CalendarEventResponse::from)
                .toList();
    }

    @Transactional
    public CalendarEventResponse updateEvent(Long memberId, Long eventId, CalendarEventRequest request) {
        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_EVENT_NOT_FOUND));

        if (!event.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        event.update(
                request.getTitle(),
                request.getDescription(),
                request.getEventDate(),
                request.getStartTime(),
                request.getEndTime()
        );

        return CalendarEventResponse.from(event);
    }

    @Transactional
    public void deleteEvent(Long memberId, Long eventId) {
        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALENDAR_EVENT_NOT_FOUND));

        if (!event.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        calendarEventRepository.delete(event);
    }
}

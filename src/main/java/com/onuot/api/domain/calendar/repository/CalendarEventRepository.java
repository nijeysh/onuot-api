package com.onuot.api.domain.calendar.repository;

import com.onuot.api.domain.calendar.entity.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByMemberIdAndEventDateBetweenOrderByEventDateAscStartTimeAsc(
            Long memberId, LocalDate startDate, LocalDate endDate);

    List<CalendarEvent> findByMemberIdAndEventDateOrderByStartTimeAsc(Long memberId, LocalDate eventDate);
}

package pl.projekt.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.projekt.backend.model.Calendar;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.CalendarRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TutorBookingValidatorTest {

    @Mock
    private CalendarRepository calendarRepository;

    @Mock
    private GoogleCalendarService googleCalendarService;

    private TutorBookingValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TutorBookingValidator(calendarRepository, googleCalendarService);
    }

    @Test
    void shouldRejectWhenDayNotPreferred() {
        Tutor tutor = new Tutor();
        tutor.setPreferredDays("{\"mon\":false,\"tue\":true}");

        LocalDateTime start = LocalDateTime.of(2025, 3, 24, 10, 0); // Monday
        LocalDateTime end = start.plusHours(1);

        Optional<String> result = validator.validate(tutor, start, end, List.of(), Locale.forLanguageTag("pl-PL"));

        assertTrue(result.isPresent());
        assertTrue(result.get().toLowerCase().contains("przyjmuje"));
    }

    @Test
    void shouldRejectWhenMaxLessonsReached() {
        Tutor tutor = new Tutor();
        tutor.setMaxLessonsPerDay(2);

        Lesson first = buildLesson(LocalDateTime.of(2025, 3, 25, 10, 0), LocalDateTime.of(2025, 3, 25, 11, 0), LessonStatus.SCHEDULED);
        Lesson second = buildLesson(LocalDateTime.of(2025, 3, 25, 12, 0), LocalDateTime.of(2025, 3, 25, 13, 0), LessonStatus.RESCHEDULED);

        LocalDateTime start = LocalDateTime.of(2025, 3, 25, 15, 0);
        LocalDateTime end = start.plusHours(1);

        Optional<String> result = validator.validate(tutor, start, end, List.of(first, second), Locale.forLanguageTag("pl-PL"));

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("limit"));
    }

    @Test
    void shouldRejectWhenBufferConflictOccurs() {
        Tutor tutor = new Tutor();
        tutor.setBufferTime(15);

        Lesson existing = buildLesson(LocalDateTime.of(2025, 3, 26, 10, 0),
                LocalDateTime.of(2025, 3, 26, 11, 0), LessonStatus.SCHEDULED);

        LocalDateTime start = LocalDateTime.of(2025, 3, 26, 11, 5);
        LocalDateTime end = start.plusHours(1);

        Optional<String> result = validator.validate(tutor, start, end, List.of(existing), Locale.forLanguageTag("pl-PL"));

        assertTrue(result.isPresent());
        assertTrue(result.get().toLowerCase().contains("zajęty"));
    }

    @Test
    void shouldRejectWhenExternalCalendarBlocksSlot() {
        Tutor tutor = new Tutor();
        tutor.setId(42L);

        Calendar calendar = new Calendar();
        calendar.setId(1L);
        calendar.setCalendarUrl("https://example.com/test.ics");
        calendar.setActive(true);

        when(calendarRepository.findByUserIdAndActiveTrue(42L)).thenReturn(List.of(calendar));
        when(googleCalendarService.fetchBusyTimes(calendar.getCalendarUrl()))
                .thenReturn(List.of(new GoogleCalendarService.BusyTime(
                        LocalDateTime.of(2025, 3, 27, 9, 0),
                        LocalDateTime.of(2025, 3, 27, 10, 30)
                )));

        LocalDateTime start = LocalDateTime.of(2025, 3, 27, 10, 0);
        LocalDateTime end = start.plusHours(1);

        Optional<String> result = validator.validate(tutor, start, end, List.of(), Locale.ENGLISH);

        assertTrue(result.isPresent());
    }

    @Test
    void shouldAllowWhenAllRulesSatisfied() {
        Tutor tutor = new Tutor();
        tutor.setId(7L);
        tutor.setPreferredDays("{\"mon\":true,\"tue\":true,\"wed\":true}");
        tutor.setMaxLessonsPerDay(3);
        tutor.setBufferTime(10);

        Lesson existing = buildLesson(LocalDateTime.of(2025, 3, 24, 8, 0),
                LocalDateTime.of(2025, 3, 24, 9, 0), LessonStatus.SCHEDULED);

        when(calendarRepository.findByUserIdAndActiveTrue(7L)).thenReturn(Collections.emptyList());

        LocalDateTime start = LocalDateTime.of(2025, 3, 24, 10, 0);
        LocalDateTime end = start.plusHours(1);

        Optional<String> result = validator.validate(tutor, start, end, List.of(existing), Locale.ENGLISH);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEnglishMessageWhenLocaleIsEnglish() {
        Tutor tutor = new Tutor();
        tutor.setPreferredDays("{\"mon\":false,\"tue\":true}");

        LocalDateTime start = LocalDateTime.of(2025, 3, 24, 10, 0);
        LocalDateTime end = start.plusHours(1);

        Optional<String> result = validator.validate(tutor, start, end, List.of(), Locale.ENGLISH);

        assertTrue(result.isPresent());
        assertTrue(result.get().contains("tutor accepts"));
    }

    private Lesson buildLesson(LocalDateTime start, LocalDateTime end, LessonStatus status) {
        Lesson lesson = new Lesson();
        lesson.setStartTime(start);
        lesson.setEndTime(end);
        lesson.setStatus(status);
        return lesson;
    }
}



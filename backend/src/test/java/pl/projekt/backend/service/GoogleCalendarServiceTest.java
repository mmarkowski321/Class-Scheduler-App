package pl.projekt.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceTest {

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    private GoogleCalendarService service;
    private Lesson lesson;
    private Tutor tutor;
    private Student student;

    @BeforeEach
    void setUp() {
        when(restTemplateBuilder.setConnectTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.setReadTimeout(any())).thenReturn(restTemplateBuilder);
        when(restTemplateBuilder.build()).thenReturn(restTemplate);

        service = new GoogleCalendarService("", "", "", "primary", "EduScheduler", restTemplateBuilder);

        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setFirstName("Anna");
        tutor.setLastName("Nowak");
        tutor.setBirthDate(LocalDate.of(1990, 1, 1));

        student = new Student();
        student.setEmail("student@example.com");
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));

        lesson = new Lesson();
        lesson.setId(1L);
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        lesson.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        lesson.setStatus(LessonStatus.REQUESTED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
    }

    // Indicates integration is disabled without credentials
    @Test
    void shouldReturnFalseWhenNotEnabled() {
        assertThat(service.isEnabled()).isFalse();
    }

    // Does not attempt to create events when service is not enabled
    @Test
    void shouldReturnEmptyWhenServiceNotEnabled() {
        var result = service.createLessonEvent(lesson);

        assertThat(result).isEmpty();
    }

    // Returns empty Optional for null lessons
    @Test
    void shouldReturnEmptyWhenLessonIsNull() {
        var result = service.createLessonEvent(null);

        assertThat(result).isEmpty();
    }

    // Returns empty Optional for missing start time
    @Test
    void shouldReturnEmptyWhenLessonStartTimeIsNull() {
        lesson.setStartTime(null);
        var result = service.createLessonEvent(lesson);

        assertThat(result).isEmpty();
    }

    // Returns empty Optional for missing end time
    @Test
    void shouldReturnEmptyWhenLessonEndTimeIsNull() {
        lesson.setEndTime(null);
        var result = service.createLessonEvent(lesson);

        assertThat(result).isEmpty();
    }

    // Silently ignores delete when not enabled
    @Test
    void shouldDeleteEventSilentlyWhenNotEnabled() {
        lesson.setGoogleEventId("event-123");
        
        service.deleteLessonEvent(lesson);
    }

    // Silently ignores delete when event id is null
    @Test
    void shouldDeleteEventSilentlyWhenGoogleEventIdIsNull() {
        lesson.setGoogleEventId(null);
        
        service.deleteLessonEvent(lesson);
    }

    // Silently ignores delete for null lesson
    @Test
    void shouldDeleteEventSilentlyWhenLessonIsNull() {
        service.deleteLessonEvent(null);
    }

    // Parses a single VEVENT from ICS feed correctly
    @Test
    void shouldFetchBusyTimesFromIcsUrl() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test Event\n" +
                "DESCRIPTION:Test Description\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
        assertThat(busyTimes.get(0).getStart()).isNotNull();
        assertThat(busyTimes.get(0).getEnd()).isNotNull();
        assertThat(busyTimes.get(0).getTitle()).isEqualTo("Test Event");
        assertThat(busyTimes.get(0).getDescription()).isEqualTo("Test Description");
    }

    // Returns empty list for blank calendar URL
    @Test
    void shouldReturnEmptyListForEmptyCalendarUrl() {
        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("");

        assertThat(busyTimes).isEmpty();
    }

    // Returns empty list for null calendar URL
    @Test
    void shouldReturnEmptyListForNullCalendarUrl() {
        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes(null);

        assertThat(busyTimes).isEmpty();
    }

    // Parses multiple VEVENT entries from ICS
    @Test
    void shouldHandleMultipleEventsInIcs() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Event 1\n" +
                "END:VEVENT\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241216T100000Z\n" +
                "DTEND:20241216T110000Z\n" +
                "SUMMARY:Event 2\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(2);
    }

    // Detects that a given slot overlaps a busy ICS event
    @Test
    void shouldCheckTimeSlotBusy() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Busy Event\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        // Convert UTC times to system default timezone for comparison
        java.time.ZoneId systemZone = java.time.ZoneId.systemDefault();
        java.time.ZonedDateTime utcStart = java.time.ZonedDateTime.of(2024, 12, 15, 14, 0, 0, 0, java.time.ZoneId.of("UTC"));
        java.time.ZonedDateTime utcEnd = java.time.ZonedDateTime.of(2024, 12, 15, 15, 0, 0, 0, java.time.ZoneId.of("UTC"));
        java.time.LocalDateTime busyStartInSystemZone = utcStart.withZoneSameInstant(systemZone).toLocalDateTime();
        java.time.LocalDateTime busyEndInSystemZone = utcEnd.withZoneSameInstant(systemZone).toLocalDateTime();
        
        // Check slot that overlaps (15 minutes after start, 15 minutes before end)
        LocalDateTime start = busyStartInSystemZone.plusMinutes(15);
        LocalDateTime end = busyEndInSystemZone.minusMinutes(15);

        boolean isBusy = service.isTimeSlotBusy("https://calendar.example.com/feed.ics", start, end);

        assertThat(isBusy).isTrue();
    }

    // Detects that a given slot does not overlap busy ICS events
    @Test
    void shouldCheckTimeSlotNotBusy() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Busy Event\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        LocalDateTime start = LocalDateTime.of(2024, 12, 16, 10, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 16, 11, 0);

        boolean isBusy = service.isTimeSlotBusy("https://calendar.example.com/feed.ics", start, end);

        assertThat(isBusy).isFalse();
    }

    // Returns false for busy check when ICS fetch fails
    @Test
    void shouldReturnFalseWhenTimeSlotCheckFails() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        LocalDateTime start = LocalDateTime.of(2024, 12, 15, 14, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 15, 15, 0);

        boolean isBusy = service.isTimeSlotBusy("https://calendar.example.com/feed.ics", start, end);

        assertThat(isBusy).isFalse();
    }

    // Handles folded lines in ICS (unfolds correctly)
    @Test
    void shouldHandleIcsWithUnfoldedLines() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;TZID=Europe/Warsaw:20241215T140000\n" +
                "DTEND;TZID=Europe/Warsaw:20241215T150000\n" +
                "SUMMARY:Test\n" +
                "  Event\n" +
                "DESCRIPTION:Test\n" +
                "  Description\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Parses ICS dates without explicit timezone or 'Z' suffix
    @Test
    void shouldHandleIcsWithoutTimezone() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000\n" +
                "DTEND:20241215T150000\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }
}


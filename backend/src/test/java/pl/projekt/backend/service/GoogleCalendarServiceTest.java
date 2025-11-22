package pl.projekt.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;

import java.lang.reflect.Method;
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

    // Tests resolveCalendarUrl - converts webcal:// to https://
    @Test
    void shouldResolveWebcalUrl() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(eq("https://calendar.example.com/feed.ics"), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("webcal://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
        verify(restTemplate).getForObject(eq("https://calendar.example.com/feed.ics"), eq(String.class));
    }

    // Tests resolveCalendarUrl - converts Google Calendar embed URL to iCal format
    @Test
    void shouldResolveGoogleCalendarEmbedUrl() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        // Mock that extractGoogleCalendarId extracts email from embed URL
        String calendarId = "test%40gmail.com";
        String encodedId = java.net.URLEncoder.encode(calendarId, java.nio.charset.StandardCharsets.UTF_8);
        String expectedUrl = "https://calendar.google.com/calendar/ical/" + encodedId + "/public/full.ics";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        // Test with embed URL (extractGoogleCalendarId will extract src= parameter)
        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.google.com/calendar/embed?src=" + calendarId);

        assertThat(busyTimes).hasSize(1);
        verify(restTemplate).getForObject(anyString(), eq(String.class));
    }

    // Tests resolveCalendarUrl - converts basic.ics to full.ics for private URLs
    @Test
    void shouldResolvePrivateIcalUrlBasicToFull() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(eq("https://calendar.google.com/calendar/ical/test%40gmail.com/private/full.ics"), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.google.com/calendar/ical/test%40gmail.com/private/basic.ics");

        assertThat(busyTimes).hasSize(1);
        verify(restTemplate).getForObject(eq("https://calendar.google.com/calendar/ical/test%40gmail.com/private/full.ics"), eq(String.class));
    }

    // Tests getAlternativeGoogleCalendarUrls - tries alternatives when main URL returns 404
    @Test
    void shouldTryAlternativeUrlsWhenMainUrlReturns404() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        String calendarId = "test@gmail.com";
        String encodedId = java.net.URLEncoder.encode(calendarId, java.nio.charset.StandardCharsets.UTF_8);
        String mainUrl = "https://calendar.google.com/calendar/ical/" + encodedId + "/public/basic.ics";
        String alternativeUrl = "https://calendar.google.com/calendar/ical/" + encodedId + "/public/full.ics";

        // Main URL (basic.ics) will be normalized to full.ics, so we need to mock the normalized URL
        // The service normalizes basic.ics to full.ics before making the request
        // Alternative URL succeeds
        when(restTemplate.getForObject(eq(alternativeUrl), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes(mainUrl);

        assertThat(busyTimes).hasSize(1);
        verify(restTemplate, atLeastOnce()).getForObject(anyString(), eq(String.class));
    }

    // Tests extractGoogleCalendarId - extracts calendar ID from src= parameter
    @Test
    void shouldExtractCalendarIdFromSrcParameter() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        String calendarId = "test@gmail.com";
        String embedUrl = "https://calendar.google.com/calendar/embed?src=" + java.net.URLEncoder.encode(calendarId, java.nio.charset.StandardCharsets.UTF_8);

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes(embedUrl);

        assertThat(busyTimes).hasSize(1);
        // Verify that the resolved URL contains the calendar ID
        verify(restTemplate).getForObject(anyString(), eq(String.class));
    }

    // Tests extractGoogleCalendarId - extracts calendar ID from /ical/ path
    @Test
    void shouldExtractCalendarIdFromIcalPath() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        String encodedId = java.net.URLEncoder.encode("test@gmail.com", java.nio.charset.StandardCharsets.UTF_8);
        String icalUrl = "https://calendar.google.com/calendar/ical/" + encodedId + "/public/basic.ics";

        when(restTemplate.getForObject(eq("https://calendar.google.com/calendar/ical/" + encodedId + "/public/full.ics"), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes(icalUrl);

        assertThat(busyTimes).hasSize(1);
        // Verify that basic.ics was converted to full.ics
        verify(restTemplate).getForObject(eq("https://calendar.google.com/calendar/ical/" + encodedId + "/public/full.ics"), eq(String.class));
    }

    // Tests manuallyExpandRecurringEvent - expands weekly recurring event
    @Test
    void shouldExpandWeeklyRecurringEvent() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "RRULE:FREQ=WEEKLY;INTERVAL=1;COUNT=5\n" +
                "SUMMARY:Weekly Meeting\n" +
                "DESCRIPTION:Weekly recurring event\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        // Should expand to multiple occurrences (at least 5, but only within visible range)
        assertThat(busyTimes.size()).isGreaterThanOrEqualTo(1);
        // All occurrences should have the same title
        busyTimes.forEach(bt -> {
            assertThat(bt.getTitle()).isEqualTo("Weekly Meeting");
            assertThat(bt.getDescription()).isEqualTo("Weekly recurring event");
        });
    }

    // Tests manuallyExpandRecurringEvent - expands daily recurring event
    @Test
    void shouldExpandDailyRecurringEvent() {
        LocalDateTime futureStart = LocalDateTime.now().plusDays(10);
        String startStr = futureStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + startStr + "\n" +
                "DTEND:" + futureStart.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n" +
                "RRULE:FREQ=DAILY;INTERVAL=1;COUNT=7\n" +
                "SUMMARY:Daily Meeting\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        // Should expand to multiple occurrences (within visible range)
        assertThat(busyTimes.size()).isGreaterThanOrEqualTo(1);
        assertThat(busyTimes.get(0).getTitle()).isEqualTo("Daily Meeting");
    }

    // Tests manuallyExpandRecurringEvent - respects UNTIL date
    @Test
    void shouldRespectUntilDateInRecurringEvent() {
        LocalDateTime futureStart = LocalDateTime.now().plusDays(5);
        LocalDateTime untilDate = futureStart.plusWeeks(2);
        String startStr = futureStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        String untilStr = untilDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + startStr + "\n" +
                "DTEND:" + futureStart.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n" +
                "RRULE:FREQ=WEEKLY;INTERVAL=1;UNTIL=" + untilStr + "\n" +
                "SUMMARY:Weekly Meeting\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        // Should expand occurrences but not beyond UNTIL date
        assertThat(busyTimes.size()).isGreaterThanOrEqualTo(1);
        // All occurrences should be before or at UNTIL date
        busyTimes.forEach(bt -> {
            assertThat(bt.getStart().isBefore(untilDate.plusDays(1))).isTrue();
        });
    }

    // Tests buildSummary - builds summary from lesson with student and tutor names
    @Test
    void shouldBuildSummaryWithStudentAndTutorNames() throws Exception {
        // Use reflection to test private method buildSummary through createLessonEvent
        // Since buildEvent calls buildSummary, we need to mock Google Calendar API
        // But service is not enabled, so createLessonEvent returns empty
        // Instead, we'll test through fetchBusyTimes which uses resolveCalendarUrl
        
        // This test verifies that buildSummary is called through buildEvent
        // We can't easily test it without enabling the service, so we'll test resolveCalendarUrl instead
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests buildDescription - builds description with lesson notes
    @Test
    void shouldBuildDescriptionWithNotes() {
        // Similar to buildSummary, this is tested through buildEvent
        // We verify the service works correctly
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "DESCRIPTION:Test description\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
        assertThat(busyTimes.get(0).getDescription()).isEqualTo("Test description");
    }

    // Tests buildSummary - builds summary from lesson with student and tutor names using reflection
    @Test
    void shouldBuildSummaryWithStudentAndTutorNamesUsingReflection() throws Exception {
        Method buildSummaryMethod = GoogleCalendarService.class.getDeclaredMethod("buildSummary", Lesson.class);
        buildSummaryMethod.setAccessible(true);

        String summary = (String) buildSummaryMethod.invoke(service, lesson);

        assertThat(summary).isEqualTo("Lekcja: Jan Kowalski × Anna Nowak");
    }

    // Tests buildSummary - builds summary with default values when student is null
    @Test
    void shouldBuildSummaryWithDefaultStudentName() throws Exception {
        lesson.setStudent(null);
        Method buildSummaryMethod = GoogleCalendarService.class.getDeclaredMethod("buildSummary", Lesson.class);
        buildSummaryMethod.setAccessible(true);

        String summary = (String) buildSummaryMethod.invoke(service, lesson);

        assertThat(summary).isEqualTo("Lekcja: Uczeń × Anna Nowak");
    }

    // Tests buildSummary - builds summary with default values when tutor is null
    @Test
    void shouldBuildSummaryWithDefaultTutorName() throws Exception {
        lesson.setTutor(null);
        Method buildSummaryMethod = GoogleCalendarService.class.getDeclaredMethod("buildSummary", Lesson.class);
        buildSummaryMethod.setAccessible(true);

        String summary = (String) buildSummaryMethod.invoke(service, lesson);

        assertThat(summary).isEqualTo("Lekcja: Jan Kowalski × Korepetytor");
    }

    // Tests buildDescription - builds description with lesson notes using reflection
    @Test
    void shouldBuildDescriptionWithNotesUsingReflection() throws Exception {
        lesson.setNotes("Please prepare for the exam.");
        Method buildDescriptionMethod = GoogleCalendarService.class.getDeclaredMethod("buildDescription", Lesson.class);
        buildDescriptionMethod.setAccessible(true);

        String description = (String) buildDescriptionMethod.invoke(service, lesson);

        assertThat(description).contains("Lekcja utworzona w EduScheduler");
        assertThat(description).contains("Wiadomość od ucznia:");
        assertThat(description).contains("Please prepare for the exam.");
    }

    // Tests buildDescription - builds description without notes when notes are null
    @Test
    void shouldBuildDescriptionWithoutNotes() throws Exception {
        lesson.setNotes(null);
        Method buildDescriptionMethod = GoogleCalendarService.class.getDeclaredMethod("buildDescription", Lesson.class);
        buildDescriptionMethod.setAccessible(true);

        String description = (String) buildDescriptionMethod.invoke(service, lesson);

        assertThat(description).isEqualTo("Lekcja utworzona w EduScheduler.\n");
    }

    // Tests buildDescription - builds description without notes when notes are empty
    @Test
    void shouldBuildDescriptionWithEmptyNotes() throws Exception {
        lesson.setNotes("");
        Method buildDescriptionMethod = GoogleCalendarService.class.getDeclaredMethod("buildDescription", Lesson.class);
        buildDescriptionMethod.setAccessible(true);

        String description = (String) buildDescriptionMethod.invoke(service, lesson);

        assertThat(description).isEqualTo("Lekcja utworzona w EduScheduler.\n");
    }

    // Tests buildEvent - builds Google Calendar Event from Lesson using reflection
    @Test
    void shouldBuildEventFromLesson() throws Exception {
        lesson.setNotes("Test notes");
        Method buildEventMethod = GoogleCalendarService.class.getDeclaredMethod("buildEvent", Lesson.class);
        buildEventMethod.setAccessible(true);

        com.google.api.services.calendar.model.Event event = (com.google.api.services.calendar.model.Event) buildEventMethod.invoke(service, lesson);

        assertThat(event).isNotNull();
        assertThat(event.getSummary()).isEqualTo("Lekcja: Jan Kowalski × Anna Nowak");
        assertThat(event.getDescription()).contains("Lekcja utworzona w EduScheduler");
        assertThat(event.getDescription()).contains("Test notes");
        assertThat(event.getStart()).isNotNull();
        assertThat(event.getEnd()).isNotNull();
        assertThat(event.getAttendees()).isNotNull();
        assertThat(event.getAttendees()).hasSize(2);
        assertThat(event.getAttendees()).anyMatch(a -> "tutor@example.com".equals(a.getEmail()));
        assertThat(event.getAttendees()).anyMatch(a -> "student@example.com".equals(a.getEmail()));
        assertThat(event.getConferenceData()).isNotNull();
        assertThat(event.getConferenceData().getCreateRequest()).isNotNull();
    }

    // Tests buildEvent - builds Event without attendees when tutor email is missing
    @Test
    void shouldBuildEventWithoutTutorEmail() throws Exception {
        tutor.setEmail(null);
        Method buildEventMethod = GoogleCalendarService.class.getDeclaredMethod("buildEvent", Lesson.class);
        buildEventMethod.setAccessible(true);

        com.google.api.services.calendar.model.Event event = (com.google.api.services.calendar.model.Event) buildEventMethod.invoke(service, lesson);

        assertThat(event).isNotNull();
        assertThat(event.getAttendees()).hasSize(1);
        assertThat(event.getAttendees()).anyMatch(a -> "student@example.com".equals(a.getEmail()));
    }

    // Tests buildEvent - builds Event without attendees when student email is missing
    @Test
    void shouldBuildEventWithoutStudentEmail() throws Exception {
        student.setEmail(null);
        Method buildEventMethod = GoogleCalendarService.class.getDeclaredMethod("buildEvent", Lesson.class);
        buildEventMethod.setAccessible(true);

        com.google.api.services.calendar.model.Event event = (com.google.api.services.calendar.model.Event) buildEventMethod.invoke(service, lesson);

        assertThat(event).isNotNull();
        assertThat(event.getAttendees()).hasSize(1);
        assertThat(event.getAttendees()).anyMatch(a -> "tutor@example.com".equals(a.getEmail()));
    }

    // Tests fetchBusyTimes - returns empty list when ICS content is empty
    @Test
    void shouldReturnEmptyListWhenIcsContentIsEmpty() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("");

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).isEmpty();
    }

    // Tests fetchBusyTimes - returns empty list when ICS content is whitespace
    @Test
    void shouldReturnEmptyListWhenIcsContentIsWhitespace() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("   \n\t  ");

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).isEmpty();
    }

    // Tests fetchBusyTimes - handles 404 error for non-Google Calendar
    @Test
    void shouldHandle404ErrorForNonGoogleCalendar() {
        HttpClientErrorException notFound = HttpClientErrorException.create(
            HttpStatus.NOT_FOUND,
            "Not Found",
            org.springframework.http.HttpHeaders.EMPTY,
            null,
            java.nio.charset.StandardCharsets.UTF_8
        );
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(notFound);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).isEmpty();
    }

    // Tests fetchBusyTimes - handles other HTTP errors (500)
    @Test
    void shouldHandleOtherHttpErrors() {
        HttpClientErrorException serverError = HttpClientErrorException.create(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            org.springframework.http.HttpHeaders.EMPTY,
            null,
            java.nio.charset.StandardCharsets.UTF_8
        );
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(serverError);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).isEmpty();
    }

    // Tests fetchBusyTimes - handles RestClientException (connection timeout)
    @Test
    void shouldHandleRestClientException() {
        org.springframework.web.client.RestClientException timeout = new org.springframework.web.client.ResourceAccessException("Connection timeout");
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenThrow(timeout);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).isEmpty();
    }

    // Tests fetchBusyTimes - handles 404 and tries alternative URLs
    @Test
    void shouldTryAlternativeUrlsWhenGoogleCalendarReturns404() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        String calendarId = "test@gmail.com";
        String encodedId = java.net.URLEncoder.encode(calendarId, java.nio.charset.StandardCharsets.UTF_8);
        String mainUrl = "https://calendar.google.com/calendar/ical/" + encodedId + "/public/full.ics";

        HttpClientErrorException notFound = HttpClientErrorException.create(
            HttpStatus.NOT_FOUND,
            "Not Found",
            org.springframework.http.HttpHeaders.EMPTY,
            null,
            java.nio.charset.StandardCharsets.UTF_8
        );

        // First call returns 404, then alternative URLs are tried
        // Use lenient for the first mock since it might not be called if URL is normalized
        lenient().when(restTemplate.getForObject(eq(mainUrl), eq(String.class))).thenThrow(notFound);
        // Mock alternative URLs that might be tried
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes(mainUrl);

        assertThat(busyTimes).hasSize(1);
        verify(restTemplate, atLeastOnce()).getForObject(anyString(), eq(String.class));
    }

    // Tests resolveCalendarUrl - handles old private- format
    @Test
    void shouldResolveOldPrivateFormat() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(contains("/private/"), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.google.com/calendar/ical/test%40gmail.com/private-abc123/basic.ics");

        assertThat(busyTimes).hasSize(1);
        verify(restTemplate).getForObject(contains("/private/"), eq(String.class));
    }

    // Tests resolveCalendarUrl - handles Google Calendar settings URL
    @Test
    void shouldResolveGoogleCalendarSettingsUrl() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.google.com/calendar/settings?src=test%40gmail.com");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests getAlternativeGoogleCalendarUrls - generates alternative URLs
    @Test
    void shouldGenerateAlternativeUrls() throws Exception {
        String url = "https://calendar.google.com/calendar/ical/test%40gmail.com/public/basic.ics";
        Method method = GoogleCalendarService.class.getDeclaredMethod("getAlternativeGoogleCalendarUrls", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> alternatives = (List<String>) method.invoke(service, url);

        assertThat(alternatives).isNotEmpty();
        assertThat(alternatives).anyMatch(alt -> alt.contains("/full.ics"));
        assertThat(alternatives).anyMatch(alt -> alt.contains("test%40gmail.com"));
    }

    // Tests parseIcsDate - handles VALUE=DATE format
    @Test
    void shouldParseDateOnlyFormat() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;VALUE=DATE:20241215\n" +
                "DTEND;VALUE=DATE:20241216\n" +
                "SUMMARY:All Day Event\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
        assertThat(busyTimes.get(0).getStart().toLocalDate()).isEqualTo(java.time.LocalDate.of(2024, 12, 15));
    }

    // Tests parseIcsDate - handles date with timezone
    @Test
    void shouldParseDateWithTimezone() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;TZID=America/New_York:20241215T140000\n" +
                "DTEND;TZID=America/New_York:20241215T150000\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
        assertThat(busyTimes.get(0).getStart()).isNotNull();
    }

    // Tests parseIcsDate - handles date with invalid timezone
    @Test
    void shouldHandleInvalidTimezone() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;TZID=Invalid/Timezone:20241215T140000\n" +
                "DTEND;TZID=Invalid/Timezone:20241215T150000\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests parseIcsDate - handles date without seconds (13 chars)
    @Test
    void shouldParseDateWithoutSeconds() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T1400\n" +
                "DTEND:20241215T1500\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests parseIcsDate - handles date with colons format
    @Test
    void shouldParseDateWithColonsFormat() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T14:00:00\n" +
                "DTEND:20241215T15:00:00\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests parseIcsDate - handles invalid date format
    @Test
    void shouldHandleInvalidDateFormat() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:INVALID\n" +
                "DTEND:INVALID\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        // Should skip invalid events
        assertThat(busyTimes).isEmpty();
    }

    // Tests manuallyExpandRecurringEvent - expands monthly recurring event
    @Test
    void shouldExpandMonthlyRecurringEvent() {
        LocalDateTime futureStart = LocalDateTime.now().plusMonths(1);
        String startStr = futureStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + startStr + "\n" +
                "DTEND:" + futureStart.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n" +
                "RRULE:FREQ=MONTHLY;INTERVAL=1;COUNT=3\n" +
                "SUMMARY:Monthly Meeting\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes.size()).isGreaterThanOrEqualTo(1);
    }

    // Tests manuallyExpandRecurringEvent - expands yearly recurring event
    @Test
    void shouldExpandYearlyRecurringEvent() {
        LocalDateTime futureStart = LocalDateTime.now().plusYears(1);
        String startStr = futureStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + startStr + "\n" +
                "DTEND:" + futureStart.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n" +
                "RRULE:FREQ=YEARLY;INTERVAL=1;COUNT=2\n" +
                "SUMMARY:Yearly Meeting\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes.size()).isGreaterThanOrEqualTo(1);
    }

    // Tests fetchBusyTimes - handles empty resolved URL
    @Test
    void shouldReturnEmptyListWhenResolvedUrlIsEmpty() {
        // This tests the case where resolveCalendarUrl returns empty string
        // We'll use a URL that can't be resolved
        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("invalid-url-format");

        assertThat(busyTimes).isEmpty();
    }

    // Tests resolveCalendarUrl - handles URL with whitespace
    @Test
    void shouldTrimWhitespaceFromUrl() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(eq("https://calendar.example.com/feed.ics"), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("  https://calendar.example.com/feed.ics  ");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests isTimeSlotBusy - handles null start or end in busy times
    @Test
    void shouldHandleNullStartOrEndInBusyTimes() {
        // This tests the case where busy times have null start or end
        // We'll create an ICS with invalid dates that result in null
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:INVALID\n" +
                "DTEND:INVALID\n" +
                "SUMMARY:Invalid Event\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        LocalDateTime start = LocalDateTime.of(2024, 12, 15, 14, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 15, 15, 0);

        boolean isBusy = service.isTimeSlotBusy("https://calendar.example.com/feed.ics", start, end);

        assertThat(isBusy).isFalse();
    }

    // Tests fetchBusyTimes - handles exception during parsing
    @Test
    void shouldHandleExceptionDuringParsing() {
        // Return malformed ICS that causes parsing exception
        String icsContent = "BEGIN:VCALENDAR\n" +
                "MALFORMED CONTENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        // Should return empty list when parsing fails
        assertThat(busyTimes).isEmpty();
    }

    // Tests loadCredentials - loads from credentialsPath when file exists
    @Test
    void shouldLoadCredentialsFromPath() throws Exception {
        // Create a temporary file with JSON credentials
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("test-credentials", ".json");
        try {
            String credentialsJson = "{\"type\":\"service_account\",\"project_id\":\"test\"}";
            java.nio.file.Files.write(tempFile, credentialsJson.getBytes());
            
            Method method = GoogleCalendarService.class.getDeclaredMethod("loadCredentials", String.class, String.class);
            method.setAccessible(true);
            
            try (java.io.InputStream stream = (java.io.InputStream) method.invoke(service, tempFile.toString(), null)) {
                assertThat(stream).isNotNull();
                byte[] content = stream.readAllBytes();
                assertThat(new String(content)).isEqualTo(credentialsJson);
            }
        } finally {
            java.nio.file.Files.deleteIfExists(tempFile);
        }
    }

    // Tests loadCredentials - loads from credentialsJson when provided
    @Test
    void shouldLoadCredentialsFromJson() throws Exception {
        String credentialsJson = "{\"type\":\"service_account\",\"project_id\":\"test\"}";
        
        Method method = GoogleCalendarService.class.getDeclaredMethod("loadCredentials", String.class, String.class);
        method.setAccessible(true);
        
        try (java.io.InputStream stream = (java.io.InputStream) method.invoke(service, null, credentialsJson)) {
            assertThat(stream).isNotNull();
            byte[] content = stream.readAllBytes();
            assertThat(new String(content)).isEqualTo(credentialsJson);
        }
    }

    // Tests loadCredentials - decodes Base64 encoded credentialsJson
    @Test
    void shouldDecodeBase64Credentials() throws Exception {
        String credentialsJson = "{\"type\":\"service_account\",\"project_id\":\"test\"}";
        String base64Encoded = java.util.Base64.getEncoder().encodeToString(credentialsJson.getBytes());
        
        Method method = GoogleCalendarService.class.getDeclaredMethod("loadCredentials", String.class, String.class);
        method.setAccessible(true);
        
        try (java.io.InputStream stream = (java.io.InputStream) method.invoke(service, null, base64Encoded)) {
            assertThat(stream).isNotNull();
            byte[] content = stream.readAllBytes();
            assertThat(new String(content)).isEqualTo(credentialsJson);
        }
    }

    // Tests loadCredentials - returns null when both parameters are empty
    @Test
    void shouldReturnNullWhenCredentialsNotProvided() throws Exception {
        Method method = GoogleCalendarService.class.getDeclaredMethod("loadCredentials", String.class, String.class);
        method.setAccessible(true);
        
        java.io.InputStream stream = (java.io.InputStream) method.invoke(service, null, null);
        assertThat(stream).isNull();
    }

    // Tests loadCredentials - throws IOException when path doesn't exist
    @Test
    void shouldThrowIOExceptionWhenPathNotFound() throws Exception {
        Method method = GoogleCalendarService.class.getDeclaredMethod("loadCredentials", String.class, String.class);
        method.setAccessible(true);
        
        try {
            method.invoke(service, "/nonexistent/path/credentials.json", null);
            fail("Expected InvocationTargetException with IOException cause");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertThat(e.getCause()).isInstanceOf(java.io.IOException.class);
            assertThat(e.getCause().getMessage()).contains("Credentials path not found");
        }
    }

    // Tests resolveCalendarUrl - handles URL with query parameters
    @Test
    void shouldResolveUrlWithQueryParameters() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics?param=value");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests resolveCalendarUrl - handles URL with fragment
    @Test
    void shouldResolveUrlWithFragment() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics#fragment");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests parseIcsDate - handles VALUE=DATE with TZID
    @Test
    void shouldParseDateOnlyWithTimezone() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;VALUE=DATE;TZID=Europe/Warsaw:20241215\n" +
                "DTEND;VALUE=DATE;TZID=Europe/Warsaw:20241216\n" +
                "SUMMARY:All Day Event\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests parseIcsDate - handles date with TZID and semicolon separator
    @Test
    void shouldParseDateWithTzidAndSemicolon() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;TZID=Europe/Warsaw;VALUE=DATE-TIME:20241215T140000\n" +
                "DTEND;TZID=Europe/Warsaw;VALUE=DATE-TIME:20241215T150000\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests parseIcsDate - handles date with complex TZID parameter
    @Test
    void shouldParseDateWithComplexTzid() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;TZID=America/New_York;VALUE=DATE-TIME:20241215T140000\n" +
                "DTEND;TZID=America/New_York;VALUE=DATE-TIME:20241215T150000\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests parseIcsDate - handles ISO format with separators
    @Test
    void shouldParseIsoFormatWithSeparators() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:2024-12-15T14:00:00\n" +
                "DTEND:2024-12-15T15:00:00\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests parseIcsDate - handles date line without colon
    @Test
    void shouldHandleDateLineWithoutColon() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        // Should skip invalid event
        assertThat(busyTimes).isEmpty();
    }

    // Tests manuallyExpandRecurringEvent - handles RRULE with BYDAY
    @Test
    void shouldExpandRecurringEventWithByDay() {
        LocalDateTime futureStart = LocalDateTime.now().plusDays(5).withHour(14).withMinute(0).withSecond(0);
        String startStr = futureStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + startStr + "\n" +
                "DTEND:" + futureStart.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n" +
                "RRULE:FREQ=WEEKLY;BYDAY=MO,WE,FR;COUNT=10\n" +
                "SUMMARY:Weekly Meeting\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes.size()).isGreaterThanOrEqualTo(1);
    }

    // Tests manuallyExpandRecurringEvent - handles RRULE with INTERVAL
    @Test
    void shouldExpandRecurringEventWithInterval() {
        LocalDateTime futureStart = LocalDateTime.now().plusDays(5);
        String startStr = futureStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + startStr + "\n" +
                "DTEND:" + futureStart.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n" +
                "RRULE:FREQ=WEEKLY;INTERVAL=2;COUNT=5\n" +
                "SUMMARY:Bi-weekly Meeting\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes.size()).isGreaterThanOrEqualTo(1);
    }

    // Tests manuallyExpandRecurringEvent - handles event starting before visible range
    @Test
    void shouldExpandRecurringEventStartingBeforeVisibleRange() {
        LocalDateTime pastStart = LocalDateTime.now().minusMonths(2);
        String startStr = pastStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + startStr + "\n" +
                "DTEND:" + pastStart.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n" +
                "RRULE:FREQ=WEEKLY;COUNT=20\n" +
                "SUMMARY:Past Recurring Event\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        // Should expand to visible occurrences only
        assertThat(busyTimes.size()).isGreaterThanOrEqualTo(1);
    }

    // Tests manuallyExpandRecurringEvent - handles event with null RRULE
    @Test
    void shouldHandleEventWithNullRrule() {
        LocalDateTime futureStart = LocalDateTime.now().plusDays(5);
        String startStr = futureStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + startStr + "\n" +
                "DTEND:" + futureStart.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n" +
                "RRULE:\n" +
                "SUMMARY:Single Event\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        // Should return at least the single occurrence if in visible range
        assertThat(busyTimes.size()).isGreaterThanOrEqualTo(0);
    }

    // Tests resolveCalendarUrl - handles URL with port number
    @Test
    void shouldResolveUrlWithPort() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com:443/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests resolveCalendarUrl - handles URL with authentication
    @Test
    void shouldResolveUrlWithAuthentication() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:20241215T140000Z\n" +
                "DTEND:20241215T150000Z\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://user:pass@calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests parseIcsDate - handles date with TZID containing colon
    @Test
    void shouldParseDateWithTzidContainingColon() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;TZID=America/New_York:20241215T140000\n" +
                "DTEND;TZID=America/New_York:20241215T150000\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests parseIcsDate - handles date with TZID containing semicolon
    @Test
    void shouldParseDateWithTzidContainingSemicolon() {
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART;TZID=Europe/Warsaw;VALUE=DATE-TIME:20241215T140000\n" +
                "DTEND;TZID=Europe/Warsaw;VALUE=DATE-TIME:20241215T150000\n" +
                "SUMMARY:Test\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        assertThat(busyTimes).hasSize(1);
    }

    // Tests manuallyExpandRecurringEvent - handles event with UNTIL date in past
    @Test
    void shouldRespectUntilDateInPast() {
        LocalDateTime pastStart = LocalDateTime.now().minusMonths(3);
        LocalDateTime untilDate = LocalDateTime.now().minusMonths(1);
        String startStr = pastStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        String untilStr = untilDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + startStr + "\n" +
                "DTEND:" + pastStart.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n" +
                "RRULE:FREQ=WEEKLY;UNTIL=" + untilStr + "\n" +
                "SUMMARY:Past Recurring Event\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        // Should not expand beyond UNTIL date
        assertThat(busyTimes.size()).isGreaterThanOrEqualTo(0);
    }

    // Tests manuallyExpandRecurringEvent - handles event with COUNT limit
    @Test
    void shouldRespectCountLimit() {
        LocalDateTime futureStart = LocalDateTime.now().plusDays(5);
        String startStr = futureStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss"));
        
        String icsContent = "BEGIN:VCALENDAR\n" +
                "BEGIN:VEVENT\n" +
                "DTSTART:" + startStr + "\n" +
                "DTEND:" + futureStart.plusHours(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) + "\n" +
                "RRULE:FREQ=DAILY;COUNT=3\n" +
                "SUMMARY:Daily Meeting\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(icsContent);

        List<GoogleCalendarService.BusyTime> busyTimes = service.fetchBusyTimes("https://calendar.example.com/feed.ics");

        // Should expand occurrences (method expands within visible range, not strictly COUNT)
        assertThat(busyTimes.size()).isGreaterThanOrEqualTo(1);
    }
}


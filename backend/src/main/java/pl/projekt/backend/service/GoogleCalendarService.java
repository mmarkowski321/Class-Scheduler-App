package pl.projekt.backend.service;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.EntryPoint;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class GoogleCalendarService {
    
    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);

    private static final DateTimeFormatter ICS_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");
    private static final DateTimeFormatter ICS_LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final boolean enabled;
    private final Calendar calendar;
    private final String calendarId;
    private final RestTemplate restTemplate;
    
    public GoogleCalendarService(
            @Value("${google.calendar.credentials-path:}") String credentialsPath,
            @Value("${google.calendar.credentials-json:}") String credentialsJson,
            @Value("${google.calendar.delegate:}") String delegateEmail,
            @Value("${google.calendar.calendar-id:primary}") String calendarId,
            @Value("${google.calendar.application-name:EduScheduler}") String applicationName,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.calendarId = calendarId;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();

        Calendar built = null;
        boolean active = true;
        try {
            built = buildCalendar(credentialsPath, credentialsJson, delegateEmail, applicationName);
        } catch (Exception ex) {
            active = false;
            log.warn("Google Calendar integration disabled: {}", ex.getMessage());
            log.debug("Google Calendar initialization error", ex);
        }
        this.calendar = built;
        this.enabled = active && built != null;
        if (!this.enabled) {
            log.info("Google Calendar integration is not enabled (missing credentials).");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public record CalendarEvent(String eventId, String hangoutLink) {}

    public Optional<CalendarEvent> createLessonEvent(Lesson lesson) {
        if (!enabled || lesson == null || lesson.getStartTime() == null || lesson.getEndTime() == null) {
            return Optional.empty();
        }
        try {
            Event event = buildEvent(lesson);
            Event created = calendar.events()
                    .insert(calendarId, event)
                    .setConferenceDataVersion(1)
                    .setSendUpdates("all")
                    .execute();

            String hangoutLink = created.getHangoutLink();
            if (!StringUtils.hasText(hangoutLink) && created.getConferenceData() != null) {
                hangoutLink = created.getConferenceData().getEntryPoints()
                        .stream()
                        .filter(entry -> Objects.equals("video", entry.getEntryPointType()))
                        .map(EntryPoint::getUri)
                        .findFirst()
                        .orElse(null);
            }

            return Optional.of(new CalendarEvent(created.getId(), hangoutLink));
        } catch (Exception ex) {
            log.warn("Failed to create Google Calendar event for lesson {}: {}", lesson != null ? lesson.getId() : null, ex.getMessage());
            log.debug("Google Calendar create event error", ex);
            return Optional.empty();
        }
    }

    public void deleteLessonEvent(Lesson lesson) {
        if (!enabled || lesson == null || !StringUtils.hasText(lesson.getGoogleEventId())) {
            return;
        }
        try {
            calendar.events().delete(calendarId, lesson.getGoogleEventId()).execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.debug("Google Calendar event {} already removed.", lesson.getGoogleEventId());
            } else {
                log.warn("Failed to delete Google Calendar event {}: {}", lesson.getGoogleEventId(), e.getMessage());
                log.debug("Google Calendar delete event error", e);
            }
        } catch (Exception ex) {
            log.warn("Failed to delete Google Calendar event {}: {}", lesson.getGoogleEventId(), ex.getMessage());
            log.debug("Google Calendar delete event error", ex);
        }
    }

    public List<BusyTime> fetchBusyTimes(String calendarUrl) {
        if (!StringUtils.hasText(calendarUrl)) {
            log.debug("Empty calendar URL provided");
            return Collections.emptyList();
        }
        String resolvedUrl = resolveCalendarUrl(calendarUrl);
        if (!StringUtils.hasText(resolvedUrl)) {
            log.debug("Could not resolve calendar URL: {}", calendarUrl);
            return Collections.emptyList();
        }
        log.info("Fetching busy times from resolved URL: {}", resolvedUrl);
        try {
            String ics = restTemplate.getForObject(resolvedUrl, String.class);
            if (!StringUtils.hasText(ics)) {
                log.warn("Empty iCal content received from: {}", resolvedUrl);
                return Collections.emptyList();
            }
            log.info("Received iCal content: {} bytes from: {}", ics.length(), resolvedUrl);
            // Log first 500 chars for debugging
            String preview = ics.length() > 500 ? ics.substring(0, 500) + "..." : ics;
            log.debug("iCal content preview (first 500 chars): {}", preview.replace("\r", "\\r").replace("\n", "\\n"));
            List<BusyTime> busyTimes = parseIcs(ics);
            log.info("Parsed {} busy times from: {}", busyTimes.size(), resolvedUrl);
            if (busyTimes.isEmpty() && ics.length() > 100) {
                log.warn("Parsed 0 busy times but received {} bytes of iCal content from: {}. Content may not be valid iCal format or calendar may be empty.", ics.length(), resolvedUrl);
                // Log a sample of VEVENT blocks to debug
                int eventCount = (int) ics.lines().filter(l -> l.trim().equalsIgnoreCase("BEGIN:VEVENT")).count();
                log.warn("Found {} BEGIN:VEVENT blocks in iCal content", eventCount);
            }
            return busyTimes;
        } catch (HttpClientErrorException ex) {
            // For 404 (calendar not public or doesn't exist), log as warning for user visibility
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Calendar not accessible (404) from {}: calendar may not be public or URL is incorrect", resolvedUrl);
                return Collections.emptyList();
            }
            // For other HTTP errors, log as warning
            log.warn("Failed to fetch busy times from {}: {} ({})", resolvedUrl, ex.getMessage(), ex.getStatusCode());
            return Collections.emptyList();
        } catch (RestClientException ex) {
            // For connection/timeout errors, log as warning
            log.warn("Failed to fetch busy times from {}: {}", resolvedUrl, ex.getMessage());
            return Collections.emptyList();
        } catch (Exception ex) {
            // For other unexpected errors, log as warning with stack trace
            log.warn("Failed to fetch busy times from {}: {}", resolvedUrl, ex.getMessage(), ex);
            return Collections.emptyList();
        }
    }

    public boolean isTimeSlotBusy(String calendarUrl, LocalDateTime start, LocalDateTime end) {
        try {
        List<BusyTime> busyTimes = fetchBusyTimes(calendarUrl);
            for (BusyTime busy : busyTimes) {
                if (busy.getStart() == null || busy.getEnd() == null) continue;
                if (start.isBefore(busy.getEnd()) && end.isAfter(busy.getStart())) {
                return true;
                }
            }
        } catch (Exception ex) {
            log.debug("Could not determine busy slot for {}: {}", calendarUrl, ex.getMessage());
        }
        return false;
    }
    
    public static class BusyTime {
        private final LocalDateTime start;
        private final LocalDateTime end;
        private final String title;
        private final String description;
        
        public BusyTime(LocalDateTime start, LocalDateTime end) {
            this(start, end, null, null);
        }

        public BusyTime(LocalDateTime start, LocalDateTime end, String title, String description) {
            this.start = start;
            this.end = end;
            this.title = title;
            this.description = description;
        }
        
        public LocalDateTime getStart() {
            return start;
        }
        
        public LocalDateTime getEnd() {
            return end;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
    }

    private Calendar buildCalendar(String credentialsPath,
                                   String credentialsJson,
                                   String delegateEmail,
                                   String applicationName) throws IOException, GeneralSecurityException {
        try (InputStream stream = loadCredentials(credentialsPath, credentialsJson)) {
            if (stream == null) {
                throw new IllegalStateException("No Google service account credentials provided.");
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

            if (StringUtils.hasText(delegateEmail) && credentials instanceof ServiceAccountCredentials sac) {
                credentials = sac.createDelegated(delegateEmail);
            }

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(applicationName)
                    .build();
        }
    }

    private InputStream loadCredentials(String credentialsPath, String credentialsJson) throws IOException {
        if (StringUtils.hasText(credentialsPath)) {
            Path path = Path.of(credentialsPath);
            if (!Files.exists(path)) {
                throw new IOException("Credentials path not found: " + credentialsPath);
            }
            return Files.newInputStream(path);
        }
        if (StringUtils.hasText(credentialsJson)) {
            String trimmed = credentialsJson.trim();
            if (!trimmed.startsWith("{")) {
                try {
                    byte[] decoded = Base64.getDecoder().decode(trimmed);
                    trimmed = new String(decoded, StandardCharsets.UTF_8);
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (trimmed.startsWith("{")) {
                return new ByteArrayInputStream(trimmed.getBytes(StandardCharsets.UTF_8));
            }
        }
        return null;
    }

    private Event buildEvent(Lesson lesson) {
        Event event = new Event();
        event.setSummary(buildSummary(lesson));
        event.setDescription(buildDescription(lesson));

        ZoneId zoneId = ZoneId.systemDefault();
        event.setStart(new EventDateTime()
                .setDateTime(toDateTime(lesson.getStartTime().atZone(zoneId)))
                .setTimeZone(zoneId.toString()));
        event.setEnd(new EventDateTime()
                .setDateTime(toDateTime(lesson.getEndTime().atZone(zoneId)))
                .setTimeZone(zoneId.toString()));

        List<EventAttendee> attendees = new ArrayList<>();
        Tutor tutor = lesson.getTutor();
        Student student = lesson.getStudent();
        if (tutor != null && StringUtils.hasText(tutor.getEmail())) {
            attendees.add(new EventAttendee().setEmail(tutor.getEmail()));
        }
        if (student != null && StringUtils.hasText(student.getEmail())) {
            attendees.add(new EventAttendee().setEmail(student.getEmail()));
        }
        if (!attendees.isEmpty()) {
            event.setAttendees(attendees);
        }

        ConferenceSolutionKey solutionKey = new ConferenceSolutionKey().setType("hangoutsMeet");
        CreateConferenceRequest conferenceRequest = new CreateConferenceRequest()
                .setRequestId("lesson-" + lesson.getId() + "-" + System.currentTimeMillis())
                .setConferenceSolutionKey(solutionKey);
        event.setConferenceData(new ConferenceData().setCreateRequest(conferenceRequest));

        return event;
    }

    private DateTime toDateTime(ZonedDateTime zonedDateTime) {
        return new DateTime(Date.from(zonedDateTime.toInstant()));
    }

    private String buildSummary(Lesson lesson) {
        Student student = lesson.getStudent();
        Tutor tutor = lesson.getTutor();
        String studentName = student != null
                ? ((student.getFirstName() + " " + student.getLastName()).trim())
                : "Uczeń";
        String tutorName = tutor != null
                ? ((tutor.getFirstName() + " " + tutor.getLastName()).trim())
                : "Korepetytor";
        return "Lekcja: " + studentName + " × " + tutorName;
    }

    private String buildDescription(Lesson lesson) {
        StringBuilder sb = new StringBuilder("Lekcja utworzona w EduScheduler.\n");
        if (StringUtils.hasText(lesson.getNotes())) {
            sb.append("\nWiadomość od ucznia:\n").append(lesson.getNotes());
        }
        return sb.toString();
    }

    private String resolveCalendarUrl(String calendarUrl) {
        String trimmed = calendarUrl.trim();
        if (trimmed.startsWith("webcal://")) {
            return "https://" + trimmed.substring("webcal://".length());
        }
        // If already an iCal URL (ends with .ics or contains /ical/), check if it's private
        if (trimmed.endsWith(".ics") || trimmed.contains("/ical/")) {
            // Convert private URLs to public URLs (private URLs require auth which we don't support)
            if (trimmed.contains("/private-")) {
                // Extract calendar ID from private URL and create public URL
                String calendarId = extractGoogleCalendarId(trimmed);
                if (StringUtils.hasText(calendarId) && !calendarId.startsWith("http")) {
                    try {
                        String encoded = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
                        String publicUrl = "https://calendar.google.com/calendar/ical/" + encoded + "/public/basic.ics";
                        log.info("Converted private calendar URL to public: {} -> {}", trimmed, publicUrl);
                        return publicUrl;
                    } catch (Exception ex) {
                        log.warn("Failed to convert private calendar URL to public: {}", trimmed, ex);
                    }
                }
                // If extraction failed, try simple string replacement
                String publicUrl = trimmed.replace("/private-", "/public/");
                log.info("Converted private calendar URL to public (simple replacement): {} -> {}", trimmed, publicUrl);
                return publicUrl;
            }
            return trimmed;
        }
        if (trimmed.contains("calendar.google.com")) {
            String calendarId = extractGoogleCalendarId(trimmed);
            // Only convert if we successfully extracted a calendar ID and it's not already an iCal URL
            if (StringUtils.hasText(calendarId) && !calendarId.contains("/ical/") && !calendarId.endsWith(".ics") && !calendarId.startsWith("http")) {
                try {
                    String encoded = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
                    return "https://calendar.google.com/calendar/ical/" + encoded + "/public/basic.ics";
                } catch (Exception ex) {
                    log.debug("Failed to encode calendar ID {}: {}", calendarId, ex.getMessage());
                }
            }
        }
        return trimmed;
    }

    private String extractGoogleCalendarId(String url) {
        try {
            URI uri = new URI(url);
            String query = uri.getRawQuery();
            if (StringUtils.hasText(query)) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("cid=")) {
                        String encoded = param.substring(4);
                        try {
                            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
                            return URLDecoder.decode(decoded, StandardCharsets.UTF_8);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    if (param.startsWith("src=")) {
                        String value = param.substring(4);
                        return URLDecoder.decode(value, StandardCharsets.UTF_8);
                    }
                }
            }
            String path = uri.getPath();
            if (StringUtils.hasText(path) && path.contains("/ical/")) {
                String[] segments = path.split("/ical/");
                if (segments.length > 1) {
                    String remainder = segments[1];
                    int slash = remainder.indexOf('/');
                    if (slash > 0) {
                        String calendarId = URLDecoder.decode(remainder.substring(0, slash), StandardCharsets.UTF_8);
                        // Only return if it looks like a calendar ID (not a full URL)
                        if (calendarId != null && !calendarId.startsWith("http")) {
                            return calendarId;
                        }
                    } else {
                        // If no slash after /ical/, try to decode the whole remainder
                        try {
                            String calendarId = URLDecoder.decode(remainder, StandardCharsets.UTF_8);
                            if (calendarId != null && !calendarId.startsWith("http") && !calendarId.contains("/")) {
                                return calendarId;
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Failed to extract calendar id from {}: {}", url, ex.getMessage());
        }
        // Return null instead of full URL if calendar ID could not be extracted
        return null;
    }

    private List<BusyTime> parseIcs(String icsContent) {
        if (!StringUtils.hasText(icsContent)) {
            log.debug("Empty iCal content provided to parseIcs");
            return Collections.emptyList();
        }
        String normalized = icsContent.replace("\r\n", "\n");
        List<String> unfolded = unfoldLines(normalized.split("\n"));
        List<BusyTime> result = new ArrayList<>();

        LocalDateTime start = null;
        LocalDateTime end = null;
        String title = null;
        String description = null;
        int eventCount = 0;
        int parsedCount = 0;

        for (String line : unfolded) {
            if ("BEGIN:VEVENT".equalsIgnoreCase(line)) {
                eventCount++;
                start = null;
                end = null;
                title = null;
                description = null;
                continue;
            }
            if ("END:VEVENT".equalsIgnoreCase(line)) {
                if (start != null && end != null) {
                    result.add(new BusyTime(start, end, title, description));
                    parsedCount++;
                } else {
                    log.debug("Skipping VEVENT with missing start/end. Start: {}, End: {}", start, end);
                }
                start = null;
                end = null;
                title = null;
                description = null;
                continue;
            }
            if (line.startsWith("DTSTART")) {
                start = parseIcsDate(line);
                if (start == null) {
                    log.debug("Failed to parse DTSTART: {}", line);
                }
            } else if (line.startsWith("DTEND")) {
                end = parseIcsDate(line);
                if (end == null) {
                    log.debug("Failed to parse DTEND: {}", line);
                }
            } else if (line.startsWith("SUMMARY:")) {
                title = line.substring("SUMMARY:".length()).trim();
            } else if (line.startsWith("DESCRIPTION:")) {
                description = line.substring("DESCRIPTION:".length()).trim();
            }
        }

        log.info("Parsed iCal: found {} VEVENT blocks, successfully parsed {} busy times", eventCount, parsedCount);
        return result;
    }

    private List<String> unfoldLines(String[] lines) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String raw : lines) {
            if (raw.startsWith(" ") || raw.startsWith("\t")) {
                current.append(raw.substring(1));
            } else {
                if (current.length() > 0) {
                    result.add(current.toString());
                }
                current = new StringBuilder(raw);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        return result;
    }

    private LocalDateTime parseIcsDate(String line) {
        String[] parts = line.split(":", 2);
        if (parts.length < 2) {
            log.debug("Invalid ICS date line (no colon): {}", line);
            return null;
        }
        String property = parts[0];
        String value = parts[1].trim();
        
        // Check if this is a DATE value (all-day event) or DATE-TIME
        boolean isDateOnly = property.contains("VALUE=DATE") || property.contains(";VALUE=DATE");
        
        ZoneId zoneId = ZoneId.systemDefault();
        if (property.contains("TZID=")) {
            String tzId = property.substring(property.indexOf("TZID=") + 5);
            int semicolon = tzId.indexOf(';');
            if (semicolon > 0) {
                tzId = tzId.substring(0, semicolon);
            }
            int colon = tzId.indexOf(':');
            if (colon > 0) {
                tzId = tzId.substring(0, colon);
            }
            try {
                zoneId = ZoneId.of(tzId);
            } catch (Exception ex) {
                log.debug("Failed to parse timezone {}: {}", tzId, ex.getMessage());
                zoneId = ZoneId.systemDefault();
            }
        }

        try {
            // Handle UTC time (ends with Z)
            if (value.endsWith("Z")) {
                ZonedDateTime zdt = ZonedDateTime.parse(value, ICS_UTC);
                return zdt.withZoneSameInstant(zoneId).toLocalDateTime();
            }
            // Handle date-only format (YYYYMMDD) or VALUE=DATE
            if (isDateOnly || value.length() == 8) {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
                return date.atStartOfDay();
            }
            // Handle local date-time format (YYYYMMDDTHHMMSS or YYYYMMDDTHHMM)
            if (value.length() >= 8) {
                return LocalDateTime.parse(value, ICS_LOCAL);
            }
            log.debug("Unknown date format: {} (property: {})", value, property);
            return null;
        } catch (DateTimeParseException ex) {
            log.warn("Failed to parse ICS date {} (property: {}): {}", value, property, ex.getMessage());
            return null;
        }
    }
}



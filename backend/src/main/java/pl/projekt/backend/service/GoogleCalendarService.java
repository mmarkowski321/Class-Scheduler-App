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
import java.io.StringReader;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.DayOfWeek;
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

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Description;

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
            // For 404 (calendar not public or doesn't exist), try alternative formats for Google Calendar
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                if (resolvedUrl.contains("calendar.google.com")) {
                    // Try alternative iCal URL formats
                    List<String> alternativeUrls = getAlternativeGoogleCalendarUrls(resolvedUrl);
                    for (String altUrl : alternativeUrls) {
                        if (altUrl != null && !altUrl.equals(resolvedUrl)) {
                            log.info("Trying alternative Google Calendar URL format: {}", altUrl);
                            try {
                                String ics = restTemplate.getForObject(altUrl, String.class);
                                if (StringUtils.hasText(ics)) {
                                    log.info("Successfully fetched iCal content from alternative URL: {} bytes", ics.length());
                                    List<BusyTime> busyTimes = parseIcs(ics);
                                    log.info("Parsed {} busy times from alternative URL: {}", busyTimes.size(), altUrl);
                                    return busyTimes;
                                }
                            } catch (Exception altEx) {
                                log.debug("Alternative URL {} also failed: {}", altUrl, altEx.getMessage());
                            }
                        }
                    }
                    
                    // If Google Calendar API is enabled, try using API to fetch events
                    if (enabled && calendar != null) {
                        List<BusyTime> apiBusyTimes = fetchBusyTimesFromGoogleCalendarApi(calendarUrl);
                        if (!apiBusyTimes.isEmpty()) {
                            log.info("Successfully fetched {} busy times from Google Calendar API", apiBusyTimes.size());
                            return apiBusyTimes;
                        }
                    }
                    
                    log.warn("Google Calendar not accessible (404) from {}: " +
                            "The calendar may not be public. " +
                            "To make it public: Go to Google Calendar → Settings → Share with specific people → 'Make available to public'. " +
                            "Or get the public iCal URL from: Google Calendar → Settings → Integrate calendar → Public URL to iCal format. " +
                            "Note: Embed URLs require the calendar to be public to fetch events.", resolvedUrl);
                } else {
                    log.warn("Calendar not accessible (404) from {}: calendar may not be public or URL is incorrect", resolvedUrl);
                }
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
        
        // Handle Google Calendar embed URLs - convert to iCal format
        if (trimmed.contains("calendar.google.com") && (trimmed.contains("/embed") || trimmed.contains("?src="))) {
            String calendarId = extractGoogleCalendarId(trimmed);
            if (StringUtils.hasText(calendarId) && !calendarId.startsWith("http")) {
                try {
                    // Try public iCal URL first
                    String encoded = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
                    // Use full.ics to get all events instead of basic.ics which may be limited
                    String iCalUrl = "https://calendar.google.com/calendar/ical/" + encoded + "/public/full.ics";
                    log.info("Converting Google Calendar embed URL to iCal: {} -> {} (calendar ID: {})", trimmed, iCalUrl, calendarId);
                    return iCalUrl;
                } catch (Exception ex) {
                    log.warn("Failed to convert embed URL {} to iCal: {}", trimmed, ex.getMessage());
                }
            }
        }
        
        // If already an iCal URL (ends with .ics or contains /ical/), normalize it
        if (trimmed.endsWith(".ics") || trimmed.contains("/ical/")) {
            // Handle private/secret iCal URLs (tajny adres iCal) - use them as-is, just convert basic.ics to full.ics
            if (trimmed.contains("/private/")) {
                // Private URLs work without making calendar public - use them directly
                // Just convert basic.ics to full.ics to get all events
                if (trimmed.contains("/basic.ics")) {
                    String fullUrl = trimmed.replace("/basic.ics", "/full.ics");
                    log.info("Converting private iCal URL from basic to full: {} -> {} (to get all events)", trimmed, fullUrl);
                    return fullUrl;
                }
                log.info("Using private iCal URL as-is: {}", trimmed);
                return trimmed;
            }
            
            // Handle old format /private- (deprecated) - convert to /private/ format
            if (trimmed.contains("/private-")) {
                // Old format uses /private-xxx, try to convert to /private/xxx
                String privateUrl = trimmed.replace("/private-", "/private/");
                // Also convert basic.ics to full.ics
                if (privateUrl.contains("/basic.ics")) {
                    privateUrl = privateUrl.replace("/basic.ics", "/full.ics");
                }
                log.info("Converted old private- URL format: {} -> {}", trimmed, privateUrl);
                return privateUrl;
            }
            // Normalize public iCal URLs: decode, extract calendar ID, re-encode
            if (trimmed.contains("calendar.google.com") && trimmed.contains("/ical/")) {
                String calendarId = extractGoogleCalendarId(trimmed);
                log.info("Extracted calendar ID from URL {}: {}", trimmed, calendarId);
                if (StringUtils.hasText(calendarId) && !calendarId.startsWith("http")) {
                    try {
                        // Re-encode the calendar ID to ensure proper URL encoding
                        String encoded = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
                        // Convert basic.ics to full.ics to get more events if it's basic.ics
                        String normalizedUrl = trimmed.contains("/basic.ics") 
                            ? trimmed.replace("/basic.ics", "/full.ics")
                            : "https://calendar.google.com/calendar/ical/" + encoded + "/public/full.ics";
                        if (!normalizedUrl.equals(trimmed)) {
                            log.info("Normalized calendar URL: {} -> {} (calendar ID: {})", trimmed, normalizedUrl, calendarId);
                        } else {
                            log.info("Calendar URL already normalized: {} (calendar ID: {})", trimmed, calendarId);
                        }
                        return normalizedUrl;
                    } catch (Exception ex) {
                        log.warn("Failed to normalize calendar URL {}: {}", trimmed, ex.getMessage(), ex);
                    }
                } else {
                    log.warn("Could not extract valid calendar ID from URL: {} (extracted: {})", trimmed, calendarId);
                    // Try manual extraction from URL pattern
                    try {
                        int icalIndex = trimmed.indexOf("/ical/");
                        if (icalIndex > 0) {
                            String afterIcal = trimmed.substring(icalIndex + "/ical/".length());
                            int slashIndex = afterIcal.indexOf('/');
                            if (slashIndex > 0) {
                                String encodedId = afterIcal.substring(0, slashIndex);
                                String decodedId = URLDecoder.decode(encodedId, StandardCharsets.UTF_8);
                                log.info("Manually extracted calendar ID: {} -> {}", encodedId, decodedId);
                                String reencoded = URLEncoder.encode(decodedId, StandardCharsets.UTF_8);
                                // Convert to full.ics to get all events
                                String normalizedUrl = trimmed.contains("/basic.ics") 
                                    ? trimmed.replace("/basic.ics", "/full.ics")
                                    : "https://calendar.google.com/calendar/ical/" + reencoded + "/public/full.ics";
                                log.info("Manually normalized calendar URL: {} -> {}", trimmed, normalizedUrl);
                                return normalizedUrl;
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("Failed to manually extract calendar ID from URL {}: {}", trimmed, ex.getMessage());
                    }
                }
            }
            return trimmed;
        }
        if (trimmed.contains("calendar.google.com")) {
            String calendarId = extractGoogleCalendarId(trimmed);
            // Only convert if we successfully extracted a calendar ID and it's not already an iCal URL
            if (StringUtils.hasText(calendarId) && !calendarId.contains("/ical/") && !calendarId.endsWith(".ics") && !calendarId.startsWith("http")) {
                try {
                    // Google Calendar iCal feed requires the calendar ID to be URL-encoded
                    // For email addresses, @ should be encoded as %40
                    String encoded = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
                    // Use full.ics instead of basic.ics to get more events (full calendar vs basic view)
                    String iCalUrl = "https://calendar.google.com/calendar/ical/" + encoded + "/public/full.ics";
                    log.info("Converted Google Calendar embed/settings URL to iCal: {} -> {} (calendar ID: {})", trimmed, iCalUrl, calendarId);
                    return iCalUrl;
                } catch (Exception ex) {
                    log.warn("Failed to encode calendar ID {}: {}", calendarId, ex.getMessage());
                }
            } else if (trimmed.contains("/embed") || trimmed.contains("?src=")) {
                // If extraction failed but it's an embed/settings URL, log warning
                log.warn("Could not extract calendar ID from Google Calendar URL: {}", trimmed);
            }
        }
        return trimmed;
    }

    private List<String> getAlternativeGoogleCalendarUrls(String url) {
        List<String> alternatives = new ArrayList<>();
        String calendarId = extractGoogleCalendarId(url);
        if (StringUtils.hasText(calendarId) && !calendarId.startsWith("http")) {
            try {
                String encoded = URLEncoder.encode(calendarId, StandardCharsets.UTF_8);
                
                // Try different URL formats to get more events:
                // Prefer full.ics over basic.ics to get all events (basic.ics may be limited)
                // 1. Try full calendar URL first (all events)
                if (url.contains("/basic.ics")) {
                    // Try full.ics first
                    alternatives.add(url.replace("/basic.ics", "/full.ics"));
                    // Also try without /public/ prefix
                    alternatives.add(url.replace("/public/basic.ics", "/full.ics"));
                    alternatives.add(url.replace("/public/basic.ics", "/basic.ics"));
                }
                alternatives.add("https://calendar.google.com/calendar/ical/" + encoded + "/public/full.ics");
                alternatives.add("https://calendar.google.com/calendar/ical/" + encoded + "/full.ics");
                alternatives.add("https://calendar.google.com/calendar/ical/" + encoded + "/basic.ics");
                
                // 2. Try with /private/ prefix (for authenticated access if API is enabled)
                if (enabled) {
                    alternatives.add("https://calendar.google.com/calendar/ical/" + encoded + "/private/full.ics");
                    alternatives.add("https://calendar.google.com/calendar/ical/" + encoded + "/private/basic.ics");
                }
                
                // 3. Try with email as-is (sometimes Google accepts it)
                if (calendarId.contains("@")) {
                    // Prefer full.ics to get all events
                    alternatives.add("https://calendar.google.com/calendar/ical/" + calendarId + "/public/full.ics");
                    alternatives.add("https://calendar.google.com/calendar/ical/" + calendarId + "/full.ics");
                    alternatives.add("https://calendar.google.com/calendar/ical/" + calendarId + "/public/basic.ics");
                    alternatives.add("https://calendar.google.com/calendar/ical/" + calendarId + "/basic.ics");
                }
                
                // 4. Try with primary calendar ID
                alternatives.add("https://calendar.google.com/calendar/ical/primary/public/full.ics");
                alternatives.add("https://calendar.google.com/calendar/ical/primary/public/basic.ics");
                
            } catch (Exception ex) {
                log.debug("Failed to create alternative URLs: {}", ex.getMessage());
            }
        }
        return alternatives;
    }
    
    private List<BusyTime> fetchBusyTimesFromGoogleCalendarApi(String calendarUrl) {
        if (!enabled || calendar == null) {
            return Collections.emptyList();
        }
        
        try {
            String calendarIdFromUrl = extractGoogleCalendarId(calendarUrl);
            String targetCalendarId = StringUtils.hasText(calendarIdFromUrl) ? calendarIdFromUrl : this.calendarId;
            
            log.info("Attempting to fetch events from Google Calendar API for calendar: {}", targetCalendarId);
            
            // Get current time and fetch events for next 30 days
            com.google.api.client.util.DateTime now = new com.google.api.client.util.DateTime(System.currentTimeMillis());
            com.google.api.client.util.DateTime future = new com.google.api.client.util.DateTime(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000));
            
            com.google.api.services.calendar.model.Events events = calendar.events()
                    .list(targetCalendarId)
                    .setTimeMin(now)
                    .setTimeMax(future)
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .execute();
            
            List<BusyTime> busyTimes = new ArrayList<>();
            for (Event event : events.getItems()) {
                EventDateTime start = event.getStart();
                EventDateTime end = event.getEnd();
                
                if (start != null && end != null && start.getDateTime() != null && end.getDateTime() != null) {
                    LocalDateTime startTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(start.getDateTime().getValue()),
                            ZoneId.systemDefault()
                    );
                    LocalDateTime endTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(end.getDateTime().getValue()),
                            ZoneId.systemDefault()
                    );
                    busyTimes.add(new BusyTime(startTime, endTime, event.getSummary(), event.getDescription()));
                }
            }
            
            log.info("Fetched {} events from Google Calendar API", busyTimes.size());
            return busyTimes;
            
        } catch (Exception ex) {
            log.debug("Failed to fetch events from Google Calendar API: {}", ex.getMessage());
            return Collections.emptyList();
        }
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
                        // Decode the calendar ID part (before the slash)
                        String encodedCalendarId = remainder.substring(0, slash);
                        try {
                            String calendarId = URLDecoder.decode(encodedCalendarId, StandardCharsets.UTF_8);
                            // Only return if it looks like a calendar ID (not a full URL)
                            if (calendarId != null && !calendarId.startsWith("http") && !calendarId.contains("/")) {
                                log.debug("Extracted calendar ID from path: {} -> {}", encodedCalendarId, calendarId);
                                return calendarId;
                            }
                        } catch (Exception ex) {
                            log.debug("Failed to decode calendar ID from path segment {}: {}", encodedCalendarId, ex.getMessage());
                        }
                    } else {
                        // If no slash after /ical/, try to decode the whole remainder
                        try {
                            String calendarId = URLDecoder.decode(remainder, StandardCharsets.UTF_8);
                            if (calendarId != null && !calendarId.startsWith("http") && !calendarId.contains("/")) {
                                log.debug("Extracted calendar ID from path (no slash): {} -> {}", remainder, calendarId);
                                return calendarId;
                            }
                        } catch (Exception ex) {
                            log.debug("Failed to decode calendar ID from remainder {}: {}", remainder, ex.getMessage());
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
        
        List<BusyTime> result = new ArrayList<>();
        
        try {
            // Use iCal4j library to parse iCal content
            CalendarBuilder builder = new CalendarBuilder();
            net.fortuna.ical4j.model.Calendar calendar = builder.build(new StringReader(icsContent));
            
            int eventCount = 0;
            int parsedCount = 0;
            
            // Iterate through all VEVENT components
            for (Component component : calendar.getComponents()) {
                if (component instanceof VEvent) {
                    eventCount++;
                    VEvent event = (VEvent) component;
                    
                    try {
                        // Extract start date
                        DtStart dtStart = event.getStartDate();
                        if (dtStart == null) {
                            log.debug("Skipping VEVENT with no DTSTART");
                            continue;
                        }
                        
                        // Extract end date
                        DtEnd dtEnd = event.getEndDate();
                        if (dtEnd == null) {
                            log.debug("Skipping VEVENT with no DTEND");
                            continue;
                        }
                        
                        // Convert to LocalDateTime
                        LocalDateTime start = convertToLocalDateTime(dtStart);
                        LocalDateTime end = convertToLocalDateTime(dtEnd);
                        
                        if (start == null || end == null) {
                            log.debug("Skipping VEVENT with invalid dates. Start: {}, End: {}", start, end);
                            continue;
                        }
                        
                        // Extract summary and description
                        Summary summary = event.getSummary();
                        Description desc = event.getDescription();
                        String title = summary != null ? summary.getValue() : null;
                        String description = desc != null ? desc.getValue() : null;
                        
                        // Check if event is recurring (has RRULE)
                        RRule rrule = event.getProperty(RRule.RRULE);
                        if (rrule != null && rrule.getRecur() != null) {
                            // Expand recurring events to get all occurrences
                            try {
                                List<BusyTime> recurringEvents = expandRecurringEvent(event, start, end, title, description);
                                result.addAll(recurringEvents);
                                parsedCount += recurringEvents.size();
                                log.debug("Expanded recurring VEVENT '{}' to {} occurrences", title, recurringEvents.size());
                            } catch (Exception ex) {
                                log.warn("Failed to expand recurring event '{}': {}. Adding single occurrence.", title, ex.getMessage());
                                // If expansion fails, add at least the first occurrence
                                result.add(new BusyTime(start, end, title, description));
                                parsedCount++;
                            }
                        } else {
                            // Non-recurring event - add single occurrence
                            result.add(new BusyTime(start, end, title, description));
                            parsedCount++;
                            log.debug("Parsed VEVENT: {} to {} ({})", start, end, title);
                        }
                        
                    } catch (Exception ex) {
                        log.warn("Failed to parse VEVENT: {}", ex.getMessage());
                    }
                }
            }
            
            log.info("Parsed iCal using iCal4j: found {} VEVENT blocks, successfully parsed {} busy times", eventCount, parsedCount);
            return result;
            
        } catch (ParserException | IOException ex) {
            log.warn("Failed to parse iCal content using iCal4j: {}. Falling back to manual parsing.", ex.getMessage());
            // Fallback to manual parsing if iCal4j fails
            return parseIcsManual(icsContent);
        } catch (Exception ex) {
            log.warn("Unexpected error parsing iCal content: {}. Falling back to manual parsing.", ex.getMessage());
            // Fallback to manual parsing on any other error
            return parseIcsManual(icsContent);
        }
    }
    
    private LocalDateTime convertToLocalDateTime(DtStart dtStart) {
        if (dtStart == null || dtStart.getDate() == null) {
            return null;
        }
        net.fortuna.ical4j.model.Date date = dtStart.getDate();
        if (date instanceof net.fortuna.ical4j.model.DateTime dateTime) {
            if (dateTime.isUtc()) {
                return LocalDateTime.ofInstant(dateTime.toInstant(), ZoneId.of("UTC")).atZone(ZoneId.of("UTC"))
                        .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            }
            return LocalDateTime.ofInstant(dateTime.toInstant(), ZoneId.systemDefault());
        }
        // For date-only values, treat as start of day in system timezone
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate().atStartOfDay();
    }
    
    private LocalDateTime convertToLocalDateTime(DtEnd dtEnd) {
        if (dtEnd == null || dtEnd.getDate() == null) {
            return null;
        }
        net.fortuna.ical4j.model.Date date = dtEnd.getDate();
        if (date instanceof net.fortuna.ical4j.model.DateTime dateTime) {
            if (dateTime.isUtc()) {
                return LocalDateTime.ofInstant(dateTime.toInstant(), ZoneId.of("UTC")).atZone(ZoneId.of("UTC"))
                        .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            }
            return LocalDateTime.ofInstant(dateTime.toInstant(), ZoneId.systemDefault());
        }
        // For date-only values, treat as start of day in system timezone
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()).toLocalDate().atStartOfDay();
    }
    
    /**
     * Expand recurring events to get all occurrences up to 2 years in the future
     */
    private List<BusyTime> expandRecurringEvent(VEvent event, LocalDateTime firstStart, LocalDateTime firstEnd,
                                                String title, String description) {
        List<BusyTime> occurrences = new ArrayList<>();
        
        try {
            RRule rrule = event.getProperty(RRule.RRULE);
            if (rrule == null || rrule.getRecur() == null) {
                // Not a recurring event, return single occurrence
                occurrences.add(new BusyTime(firstStart, firstEnd, title, description));
                return occurrences;
            }
            
            // Manually expand recurring events using RRULE
            // iCal4j doesn't have a simple getRecurrenceDates method, so we'll use manual expansion
            occurrences.addAll(manuallyExpandRecurringEvent(
                event, firstStart, firstEnd, title, description, rrule
            ));
            
            log.debug("Expanded recurring event '{}' to {} occurrences", title, occurrences.size());
            
            // Ensure at least one occurrence is returned
            if (occurrences.isEmpty()) {
                occurrences.add(new BusyTime(firstStart, firstEnd, title, description));
            }
            
            return occurrences;
            
        } catch (Exception ex) {
            log.warn("Failed to expand recurring event '{}': {}", title, ex.getMessage(), ex);
            // Return at least the first occurrence if expansion fails
            if (occurrences.isEmpty()) {
                occurrences.add(new BusyTime(firstStart, firstEnd, title, description));
            }
            return occurrences;
        }
    }
    
    /**
     * Manually expand recurring events when iCal4j's getRecurrenceDates doesn't work
     * Only expands events for visible date range (current month +/- 1 month)
     * Respects UNTIL limit from RRULE
     */
    private List<BusyTime> manuallyExpandRecurringEvent(VEvent event, LocalDateTime firstStart, LocalDateTime firstEnd,
                                                        String title, String description, RRule rrule) {
        List<BusyTime> occurrences = new ArrayList<>();
        
        try {
            net.fortuna.ical4j.model.Recur recur = rrule.getRecur();
            if (recur == null) {
                // Not recurring, return single occurrence only if it's in visible range
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime visibleStart = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
                LocalDateTime visibleEnd = now.plusMonths(2).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).minusSeconds(1);
                if (firstStart.isBefore(visibleEnd) && firstEnd.isAfter(visibleStart)) {
                    occurrences.add(new BusyTime(firstStart, firstEnd, title, description));
                }
                return occurrences;
            }
            
            // Check UNTIL limit from RRULE (if exists)
            // UNTIL is stored in the Recur object, not in RRule directly
            LocalDateTime untilDateTime = null;
            if (recur.getUntil() != null) {
                net.fortuna.ical4j.model.Date untilDate = recur.getUntil();
                untilDateTime = convertToLocalDateTime(new DtStart(untilDate));
            }
            
            // Calculate duration
            long durationMinutes = java.time.Duration.between(firstStart, firstEnd).toMinutes();
            
            LocalDateTime now = LocalDateTime.now();
            // Expand only for visible range: 1 month before current month start to 2 months in future
            LocalDateTime visibleStart = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime visibleEnd = now.plusMonths(2).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).minusSeconds(1);
            
            // Use UNTIL as end limit if it's earlier than visibleEnd
            LocalDateTime endExpansion = visibleEnd;
            if (untilDateTime != null && untilDateTime.isBefore(visibleEnd)) {
                endExpansion = untilDateTime;
            }
            
            // Start from first occurrence
            LocalDateTime current = firstStart;
            
            // If first occurrence is before visible start, skip forward
            if (current.isBefore(visibleStart)) {
                net.fortuna.ical4j.model.Recur.Frequency freq = recur.getFrequency();
                int interval = recur.getInterval() > 0 ? recur.getInterval() : 1;
                
                if (freq != null) {
                    // For WEEKLY with BYDAY, use the first occurrence's day of week
                    // Since firstStart already has the correct day, we'll just skip forward by weeks
                    if (freq == net.fortuna.ical4j.model.Recur.Frequency.WEEKLY) {
                        // Calculate weeks to skip to get to visibleStart
                        long daysToSkip = java.time.Duration.between(current, visibleStart).toDays();
                        long weeksToSkip = Math.max(0, daysToSkip / (7 * interval));
                        current = current.plusWeeks(weeksToSkip * interval);
                        // If still before visibleStart, add one more week
                        if (current.isBefore(visibleStart)) {
                            current = current.plusWeeks(interval);
                        }
                    } else {
                        // For other frequencies, skip intervals
                        long daysToSkip = java.time.Duration.between(current, visibleStart).toDays();
                        switch (freq) {
                            case DAILY:
                                long intervalsToSkip = daysToSkip / interval;
                                current = current.plusDays(intervalsToSkip * interval);
                                break;
                            case WEEKLY:
                                long weeksToSkip = daysToSkip / (7 * interval);
                                current = current.plusWeeks(weeksToSkip * interval);
                                break;
                            case MONTHLY:
                                long monthsToSkip = java.time.temporal.ChronoUnit.MONTHS.between(current, visibleStart) / interval;
                                current = current.plusMonths(monthsToSkip * interval);
                                break;
                            case YEARLY:
                                long yearsToSkip = java.time.temporal.ChronoUnit.YEARS.between(current, visibleStart) / interval;
                                current = current.plusYears(yearsToSkip * interval);
                                break;
                        }
                    }
                }
            }
            
            int maxOccurrences = 100; // Reduced limit since we're only expanding visible range
            int count = 0;
            int interval = recur.getInterval() > 0 ? recur.getInterval() : 1;
            net.fortuna.ical4j.model.Recur.Frequency freq = recur.getFrequency();
            java.util.List<net.fortuna.ical4j.model.WeekDay> byDay = recur.getDayList();
            
            // Expand occurrences only for visible range and respect UNTIL
            while (current.isBefore(endExpansion) && count < maxOccurrences) {
                // Only add if it's within visible range
                if (current.isBefore(visibleEnd) && current.isAfter(visibleStart.minusDays(1))) {
                    LocalDateTime currentEnd = current.plusMinutes(durationMinutes);
                    occurrences.add(new BusyTime(current, currentEnd, title, description));
                }
                
                // Calculate next occurrence based on RRULE frequency and BYDAY
                if (freq == null) {
                    break;
                }
                
                if (freq == net.fortuna.ical4j.model.Recur.Frequency.WEEKLY) {
                    // For weekly, just add one interval of weeks
                    // BYDAY is already handled by using the first occurrence's day of week
                    current = current.plusWeeks(interval);
                } else {
                    // For other frequencies
                    switch (freq) {
                        case DAILY:
                            current = current.plusDays(interval);
                            break;
                        case WEEKLY:
                            current = current.plusWeeks(interval);
                            break;
                        case MONTHLY:
                            current = current.plusMonths(interval);
                            break;
                        case YEARLY:
                            current = current.plusYears(interval);
                            break;
                        default:
                            current = endExpansion;
                            break;
                    }
                }
                
                count++;
            }
            
            log.debug("Manually expanded recurring event '{}' to {} occurrences (visible: {} to {}, UNTIL: {})", 
                title, occurrences.size(), visibleStart, visibleEnd, untilDateTime);
            
        } catch (Exception ex) {
            log.warn("Manual expansion failed for '{}': {}", title, ex.getMessage(), ex);
            // Return at least first occurrence if it's in visible range
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime visibleStart = now.minusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime visibleEnd = now.plusMonths(2).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).minusSeconds(1);
            if (firstStart.isBefore(visibleEnd) && firstEnd.isAfter(visibleStart)) {
                occurrences.add(new BusyTime(firstStart, firstEnd, title, description));
            }
        }
        
        return occurrences;
    }
    
    // Fallback manual parsing method (keep existing logic)
    private List<BusyTime> parseIcsManual(String icsContent) {
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

        log.info("Parsed iCal manually (fallback): found {} VEVENT blocks, successfully parsed {} busy times", eventCount, parsedCount);
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
            // 1. Handle UTC time (ends with Z) - Google Calendar and other formats
            if (value.endsWith("Z")) {
                ZonedDateTime zdt = ZonedDateTime.parse(value, ICS_UTC);
                return zdt.withZoneSameInstant(zoneId).toLocalDateTime();
            }
            
            // 2. Handle date-only format (YYYYMMDD) or VALUE=DATE
            if (isDateOnly || value.length() == 8) {
                LocalDate date = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
                return date.atStartOfDay();
            }
            
            // 3. Handle date-time formats - try multiple parsing strategies
            if (value.length() >= 8 && value.contains("T")) {
                log.debug("Attempting to parse date: '{}' (length: {}, contains T: {})", value, value.length(), value.contains("T"));
                // 3a. Try manual parsing for USOS format: YYYYMMDDTHHMMSS (15 chars, no separators)
                if (value.length() == 15) {
                    log.debug("Value length is 15, attempting manual USOS parsing...");
                    try {
                        String yearStr = value.substring(0, 4);
                        String monthStr = value.substring(4, 6);
                        String dayStr = value.substring(6, 8);
                        String hourStr = value.substring(9, 11);
                        String minuteStr = value.substring(11, 13);
                        String secondStr = value.substring(13, 15);
                        log.info("Extracting from date '{}': year={}, month={}, day={}, hour={}, minute={}, second={}", 
                                value, yearStr, monthStr, dayStr, hourStr, minuteStr, secondStr);
                        int year = Integer.parseInt(yearStr);
                        int month = Integer.parseInt(monthStr);
                        int day = Integer.parseInt(dayStr);
                        int hour = Integer.parseInt(hourStr);
                        int minute = Integer.parseInt(minuteStr);
                        int second = Integer.parseInt(secondStr);
                        LocalDateTime result = LocalDateTime.of(year, month, day, hour, minute, second);
                        log.info("✓ SUCCESS: Manually parsed USOS date {}: {}-{}-{} {}:{}:{}", value, year, month, day, hour, minute, second);
                        return result;
                    } catch (NumberFormatException ex) {
                        log.warn("NumberFormatException parsing USOS date {}: {}", value, ex.getMessage());
                        // Continue to try other formats
                    } catch (Exception ex) {
                        log.warn("Exception parsing USOS date {}: {} - {}", value, ex.getClass().getSimpleName(), ex.getMessage());
                        // Continue to try other formats
                    }
                }
                // 3b. Try manual parsing for format without seconds: YYYYMMDDTHHMM (13 chars)
                if (value.length() == 13) {
                    try {
                        int year = Integer.parseInt(value.substring(0, 4));
                        int month = Integer.parseInt(value.substring(4, 6));
                        int day = Integer.parseInt(value.substring(6, 8));
                        int hour = Integer.parseInt(value.substring(9, 11));
                        int minute = Integer.parseInt(value.substring(11, 13));
                        log.debug("Manually parsed date {} (no seconds): {}-{}-{} {}:{}", value, year, month, day, hour, minute);
                        return LocalDateTime.of(year, month, day, hour, minute, 0);
                    } catch (Exception ex) {
                        // Continue to try other formats
                    }
                }
                
                // 3c. Try standard iCal format: yyyyMMdd'T'HHmmss (Google Calendar, standard iCal)
                try {
                    LocalDateTime result = LocalDateTime.parse(value, ICS_LOCAL);
                    log.debug("Parsed date using standard iCal format: {}", value);
                    return result;
                } catch (DateTimeParseException e) {
                    // Continue to try other formats
                }
                
                // 3d. Try ISO format with separators: yyyy-MM-dd'T'HH:mm:ss (some calendars)
                try {
                    LocalDateTime result = LocalDateTime.parse(value);
                    log.debug("Parsed date using ISO format: {}", value);
                    return result;
                } catch (DateTimeParseException e) {
                    // Continue to try other formats
                }
                
                // 3e. Try format with colons: yyyyMMdd'T'HH:mm:ss (some variations)
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HH:mm:ss");
                    LocalDateTime result = LocalDateTime.parse(value, formatter);
                    log.debug("Parsed date using colon format: {}", value);
                    return result;
                } catch (DateTimeParseException e) {
                    // All parsing attempts failed
                    log.warn("Failed to parse date {} after trying all formats. Property: {}", value, property);
                }
            }
            
            log.debug("Unknown date format: {} (property: {}, length: {})", value, property, value.length());
            return null;
        } catch (Exception ex) {
            log.warn("Failed to parse ICS date {} (property: {}): {}", value, property, ex.getMessage());
            return null;
        }
    }
}



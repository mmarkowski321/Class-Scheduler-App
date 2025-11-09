package pl.projekt.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GoogleCalendarService {
    
    private final RestTemplate restTemplate;
    
    public GoogleCalendarService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }
    
    /**
     * Extract calendar ID from Google Calendar URL
     * Supports formats:
     * - https://calendar.google.com/calendar/u/0?cid=ENCODED_EMAIL
     * - https://calendar.google.com/calendar/embed?src=EMAIL
     * - https://calendar.google.com/calendar/ical/EMAIL/basic.ics
     */
    public String extractCalendarId(String calendarUrl) {
        if (calendarUrl == null || calendarUrl.isBlank()) {
            return null;
        }
        
        try {
            // Format 1: embed?src= parameter (direct email in URL encoded format)
            if (calendarUrl.contains("embed?src=") || calendarUrl.contains("embed&src=")) {
                String srcParam;
                if (calendarUrl.contains("embed?src=")) {
                    srcParam = calendarUrl.substring(calendarUrl.indexOf("embed?src=") + 10);
                } else {
                    srcParam = calendarUrl.substring(calendarUrl.indexOf("embed&src=") + 10);
                }
                // Remove any additional parameters
                if (srcParam.contains("&")) {
                    srcParam = srcParam.substring(0, srcParam.indexOf("&"));
                }
                
                // Decode URL encoding
                try {
                    String decodedEmail = URLDecoder.decode(srcParam, StandardCharsets.UTF_8);
                    // If still encoded, decode again
                    if (decodedEmail.contains("%")) {
                        decodedEmail = URLDecoder.decode(decodedEmail, StandardCharsets.UTF_8);
                    }
                    return decodedEmail.replaceAll("%40", "@").replaceAll("%2E", ".");
                } catch (Exception e) {
                    return srcParam.replaceAll("%40", "@").replaceAll("%2E", ".");
                }
            }
            
            // Format 2: cid parameter (base64 encoded email)
            if (calendarUrl.contains("cid=")) {
                String cidParam = calendarUrl.substring(calendarUrl.indexOf("cid=") + 4);
                // Remove any additional parameters
                if (cidParam.contains("&")) {
                    cidParam = cidParam.substring(0, cidParam.indexOf("&"));
                }
                
                // First, try to decode URL encoding (in case it's double-encoded)
                String decodedCid = cidParam;
                try {
                    decodedCid = URLDecoder.decode(cidParam, StandardCharsets.UTF_8);
                    // If still encoded, decode again
                    if (decodedCid.contains("%")) {
                        decodedCid = URLDecoder.decode(decodedCid, StandardCharsets.UTF_8);
                    }
                } catch (Exception e) {
                    // If URL decode fails, use original
                }
                
                // Try base64 decode
                try {
                    byte[] decoded = Base64.getDecoder().decode(decodedCid);
                    String email = new String(decoded, StandardCharsets.UTF_8);
                    // Clean up email - remove any remaining encoding
                    email = email.replaceAll("%40", "@").replaceAll("%2E", ".");
                    return email;
                } catch (Exception e) {
                    // If base64 decode fails, try using decoded URL as-is
                    if (decodedCid.contains("@")) {
                        return decodedCid.replaceAll("%40", "@").replaceAll("%2E", ".");
                    }
                    return decodedCid;
                }
            }
            
            // Format 3: ical URL format
            if (calendarUrl.contains("/ical/")) {
                String icalPart = calendarUrl.substring(calendarUrl.indexOf("/ical/") + 6);
                if (icalPart.contains("/")) {
                    String email = icalPart.substring(0, icalPart.indexOf("/"));
                    // Decode URL encoding multiple times if needed
                    String decodedEmail = email;
                    try {
                        decodedEmail = URLDecoder.decode(email, StandardCharsets.UTF_8);
                        if (decodedEmail.contains("%")) {
                            decodedEmail = URLDecoder.decode(decodedEmail, StandardCharsets.UTF_8);
                        }
                        return decodedEmail.replaceAll("%40", "@").replaceAll("%2E", ".");
                    } catch (Exception e) {
                        return email.replaceAll("%40", "@").replaceAll("%2E", ".");
                    }
                }
            }
            
            // Format 4: Direct email format
            if (calendarUrl.contains("@")) {
                String email = calendarUrl.replaceAll("^.*mailto:|^.*calendar/", "").replaceAll("\\?.*$", "");
                return email.replaceAll("%40", "@").replaceAll("%2E", ".");
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting calendar ID: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Get iCal feed URL from calendar ID (email)
     * Returns URI object to prevent double encoding
     * Note: Google Calendar public iCal feed has limited date range (usually 30-60 days)
     * For wider range, user should use private iCal URL or Google Calendar API
     */
    public URI getICalFeedUri(String calendarId) {
        if (calendarId == null || calendarId.isBlank()) {
            return null;
        }
        
        // Clean email - remove any existing encoding
        String cleanEmail = calendarId.replaceAll("%40", "@").replaceAll("%2E", ".").replaceAll("%2540", "@");
        
        // Build URI using UriComponentsBuilder to handle encoding properly
        // Note: Google Calendar public feed may have limited date range
        // We can try adding query parameters but they may not be supported
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://calendar.google.com")
                .path("/calendar/ical/{email}/public/basic.ics");
            
            // Try to extend date range - Google may ignore these but it's worth trying
            // Note: These parameters may not work with public feeds
            builder.queryParam("singleevents", "true"); // Expand recurring events
            builder.queryParam("max-results", "2500"); // Maximum number of events
            
            return builder.buildAndExpand(cleanEmail).toUri();
        } catch (Exception e) {
            System.err.println("Error building URI for calendar ID: " + calendarId);
            return null;
        }
    }
    
    /**
     * Fetch busy times from calendar public iCal feed
     * Supports Google Calendar (extracts calendar ID and builds iCal URL) and direct iCal URLs
     */
    public List<BusyTime> fetchBusyTimes(String calendarUrl) {
        List<BusyTime> busyTimes = new ArrayList<>();
        
        try {
            URI icalUri = null;
            String calendarId = null; // Define here for use in catch block
            
            // Check if URL is already a direct iCal feed (ends with .ics or contains /ical/ or /upcoming_ical)
            if (calendarUrl.contains(".ics") || 
                calendarUrl.contains("/ical/") || 
                calendarUrl.contains("/upcoming_ical") ||
                calendarUrl.contains("ical=")) {
                // Direct iCal feed URL - use it directly
                try {
                    icalUri = new URI(calendarUrl);
                    System.out.println("Using direct iCal URL: " + icalUri.toString());
                } catch (Exception e) {
                    System.err.println("Invalid direct iCal URL: " + calendarUrl);
                    return busyTimes;
                }
            } else {
                // Try to extract Google Calendar ID and build iCal feed URL
                calendarId = extractCalendarId(calendarUrl);
                if (calendarId == null) {
                    System.err.println("Could not extract calendar ID from URL: " + calendarUrl);
                    System.err.println("Note: URL must be a Google Calendar embed URL or a direct iCal feed URL (.ics or /ical/)");
                    return busyTimes;
                }
                
                icalUri = getICalFeedUri(calendarId);
                if (icalUri == null) {
                    System.err.println("Could not generate iCal URI for calendar ID: " + calendarId);
                    return busyTimes;
                }
                
                System.out.println("Fetching iCal from Google Calendar: " + icalUri.toString());
            }
            
            // Fetch iCal feed with error handling
            // Use URI object to prevent RestTemplate from double-encoding the URL
            try {
                String icalContent = restTemplate.getForObject(icalUri, String.class);
                if (icalContent == null || icalContent.isBlank()) {
                    System.err.println("Empty iCal feed response from: " + icalUri.toString());
                    return busyTimes;
                }
                
                // Check if response is HTML (error page or regular web page)
                if (icalContent.trim().startsWith("<html") || 
                    icalContent.contains("Error 404") ||
                    icalContent.contains("<!DOCTYPE") ||
                    (icalContent.contains("<body") && !icalContent.contains("BEGIN:VCALENDAR"))) {
                    System.err.println("Got HTML page instead of iCal feed. This is likely a web page URL, not an iCal feed URL.");
                    System.err.println("For USOS: Use the iCal feed URL (e.g., https://apps.usos.pwr.edu.pl/services/tt/upcoming_ical?...), not the HTML page URL.");
                    System.err.println("URL attempted: " + icalUri.toString());
                    throw new RuntimeException("URL is a web page (HTML), not an iCal feed. Please use the iCal feed URL instead.");
                }
                
                // Parse iCal content
                System.out.println("Parsing iCal content, length: " + icalContent.length() + " characters");
                System.out.println("iCal feed URL: " + icalUri);
                busyTimes = parseICal(icalContent);
                System.out.println("Parsed " + busyTimes.size() + " events from iCal feed");
                
                // Log date range of events
                if (!busyTimes.isEmpty()) {
                    LocalDateTime minDate = busyTimes.stream()
                        .map(BusyTime::getStart)
                        .min(LocalDateTime::compareTo)
                        .orElse(null);
                    LocalDateTime maxDate = busyTimes.stream()
                        .map(BusyTime::getEnd)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
                    System.out.println("Event date range: " + minDate + " to " + maxDate);
                } else {
                    System.out.println("WARNING: No events found in iCal feed!");
                }
                
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                System.err.println("HTTP error fetching calendar: " + e.getStatusCode() + " - " + e.getMessage());
                if (calendarId != null) {
                    System.err.println("Calendar ID: " + calendarId);
                }
                if (icalUri != null) {
                    System.err.println("iCal URI: " + icalUri.toString());
                }
                System.err.println("NOTE: Calendar must be public and accessible. Check calendar settings.");
                // Return empty list instead of throwing
                return busyTimes;
            }
            
        } catch (Exception e) {
            System.err.println("Error fetching busy times: " + e.getMessage());
            e.printStackTrace();
        }
        
        return busyTimes;
    }
    
    /**
     * Generate recurring event occurrences based on RRULE
     * Supports: FREQ=WEEKLY, FREQ=DAILY, FREQ=MONTHLY with BYDAY
     * Generates occurrences up to 2 years from now
     */
    private List<LocalDateTime> generateRecurrences(LocalDateTime startDate, String rrule) {
        System.out.println("generateRecurrences called with startDate: " + startDate + ", rrule: " + rrule);
        List<LocalDateTime> occurrences = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxDate = now.plusYears(2); // Generate up to 2 years ahead
        System.out.println("Max date for generation: " + maxDate);
        
        // Parse RRULE parameters
        String[] parts = rrule.split(";");
        String freq = null;
        String byDay = null;
        LocalDateTime until = null;
        Integer count = null;
        
        System.out.println("Parsing RRULE parts: " + java.util.Arrays.toString(parts));
        
        for (String part : parts) {
            if (part.startsWith("FREQ=")) {
                freq = part.substring(5);
                System.out.println("Found FREQ: " + freq);
            } else if (part.startsWith("BYDAY=")) {
                byDay = part.substring(6);
                System.out.println("Found BYDAY: " + byDay);
            } else if (part.startsWith("UNTIL=")) {
                String untilStr = part.substring(6);
                until = parseDateTime(untilStr, true);
                System.out.println("Found UNTIL: " + untilStr + " -> " + until);
            } else if (part.startsWith("COUNT=")) {
                count = Integer.parseInt(part.substring(6));
                System.out.println("Found COUNT: " + count);
            } else {
                System.out.println("Unknown RRULE part: " + part);
            }
        }
        
        System.out.println("Parsed RRULE - FREQ: " + freq + ", BYDAY: " + byDay + ", UNTIL: " + until + ", COUNT: " + count);
        
        // Determine end date
        LocalDateTime endDate = maxDate;
        if (until != null && until.isBefore(endDate)) {
            endDate = until;
        }
        
        // Generate occurrences based on frequency
        if ("WEEKLY".equals(freq)) {
            List<DayOfWeek> daysOfWeek = new ArrayList<>();
            if (byDay != null) {
                // Parse BYDAY (e.g., "TU" or "MO,WE,FR")
                String[] dayCodes = byDay.split(",");
                for (String dayCode : dayCodes) {
                    DayOfWeek day = parseDayOfWeek(dayCode.trim());
                    if (day != null) {
                        daysOfWeek.add(day);
                    }
                }
            } else {
                // If no BYDAY specified, use the day of the start date
                daysOfWeek.add(startDate.getDayOfWeek());
            }
            
            // Generate occurrences for each specified day of week
            // Start from startDate's week
            LocalDateTime weekStart = startDate.minusDays(startDate.getDayOfWeek().getValue() - 1); // Start of week (Monday)
            
            System.out.println("WEEKLY recurrence - startDate: " + startDate + ", weekStart: " + weekStart);
            System.out.println("Target days of week: " + daysOfWeek);
            System.out.println("End date: " + endDate);
            
            int generatedCount = 0;
            int maxIterations = 104; // 2 years * 52 weeks
            int iteration = 0;
            
            while (weekStart.isBefore(endDate) && (count == null || generatedCount < count) && iteration < maxIterations) {
                // Check each target day of week in this week
                for (DayOfWeek targetDay : daysOfWeek) {
                    LocalDateTime occurrence = weekStart.plusDays(targetDay.getValue() - 1); // Day of week (1=Monday, 7=Sunday)
                    
                    // Only add if it's on or after startDate and before endDate
                    if (!occurrence.isBefore(startDate) && occurrence.isBefore(endDate) && 
                        (count == null || generatedCount < count)) {
                        occurrences.add(occurrence);
                        generatedCount++;
                        System.out.println("  Generated occurrence #" + generatedCount + ": " + occurrence);
                    }
                }
                // Move to next week
                weekStart = weekStart.plusWeeks(1);
                iteration++;
            }
            
            System.out.println("Total occurrences generated: " + occurrences.size());
        } else if ("DAILY".equals(freq)) {
            LocalDateTime current = startDate;
            int generatedCount = 0;
            while (current.isBefore(endDate) && (count == null || generatedCount < count)) {
                occurrences.add(current);
                current = current.plusDays(1);
                generatedCount++;
            }
        } else if ("MONTHLY".equals(freq)) {
            LocalDateTime current = startDate;
            int generatedCount = 0;
            while (current.isBefore(endDate) && (count == null || generatedCount < count)) {
                occurrences.add(current);
                current = current.plusMonths(1);
                generatedCount++;
            }
        } else {
            // Unknown frequency, just add the start date
            System.err.println("Unknown RRULE frequency: " + freq + ". Adding only start date.");
            occurrences.add(startDate);
        }
        
        return occurrences;
    }
    
    /**
     * Parse day of week code from RRULE BYDAY
     * Supports: SU, MO, TU, WE, TH, FR, SA
     */
    private DayOfWeek parseDayOfWeek(String dayCode) {
        switch (dayCode.toUpperCase()) {
            case "SU": return DayOfWeek.SUNDAY;
            case "MO": return DayOfWeek.MONDAY;
            case "TU": return DayOfWeek.TUESDAY;
            case "WE": return DayOfWeek.WEDNESDAY;
            case "TH": return DayOfWeek.THURSDAY;
            case "FR": return DayOfWeek.FRIDAY;
            case "SA": return DayOfWeek.SATURDAY;
            default:
                System.err.println("Unknown day code: " + dayCode);
                return null;
        }
    }
    
    /**
     * Parse iCal content and extract busy times
     */
    private List<BusyTime> parseICal(String icalContent) {
        List<BusyTime> busyTimes = new ArrayList<>();
        
        // Pattern to match DTSTART and DTEND
        Pattern dtStartPattern = Pattern.compile("DTSTART[^:]*:(.+)");
        Pattern dtEndPattern = Pattern.compile("DTEND[^:]*:(.+)");
        Pattern dtStartDatePattern = Pattern.compile("DTSTART[^;]*;VALUE=DATE:(.+)");
        Pattern dtEndDatePattern = Pattern.compile("DTEND[^;]*;VALUE=DATE:(.+)");
        
        String[] lines = icalContent.split("\\r?\\n");
        String currentStart = null;
        String currentEnd = null;
        String currentTitle = null;
        String currentDescription = null;
        String currentRRule = null;
        boolean inEvent = false;
        
        // Pattern to match SUMMARY (title)
        Pattern summaryPattern = Pattern.compile("SUMMARY[^:]*:(.+)");
        // Pattern to match DESCRIPTION
        Pattern descriptionPattern = Pattern.compile("DESCRIPTION[^:]*:(.+)");
        // Pattern to match RRULE (recurrence rule)
        Pattern rrulePattern = Pattern.compile("RRULE[^:]*:(.+)");
        
        for (String line : lines) {
            if (line.startsWith("BEGIN:VEVENT")) {
                inEvent = true;
                currentStart = null;
                currentEnd = null;
                currentTitle = null;
                currentDescription = null;
                currentRRule = null;
            } else if (line.startsWith("END:VEVENT")) {
                if (inEvent) {
                    if (currentStart == null || currentEnd == null) {
                        System.err.println("Skipping event - missing DTSTART or DTEND. Title: " + currentTitle);
                    } else {
                        try {
                            // Check if both are date-only (all-day events)
                            boolean startIsDateOnly = currentStart.endsWith("DATE");
                            boolean endIsDateOnly = currentEnd.endsWith("DATE");
                            
                            LocalDateTime start = parseDateTime(currentStart, true);
                            LocalDateTime end = parseDateTime(currentEnd, false);
                            
                            // For all-day events, DTEND is exclusive
                            // If DTEND is date-only, it means the event ends at the beginning of that day
                            // So we should use the end of the previous day (23:59:59)
                            if (endIsDateOnly && end != null) {
                                // Subtract 1 day and set to end of day
                                end = end.minusDays(1).withHour(23).withMinute(59).withSecond(59);
                            }
                            
                            if (start != null && end != null && end.isAfter(start)) {
                                // Use title if available, otherwise use default
                                // Decode the final title and description after all continuations
                                String title = currentTitle != null && !currentTitle.trim().isEmpty() 
                                    ? decodeICalText(currentTitle).trim() 
                                    : "Wydarzenie";
                                String description = currentDescription != null && !currentDescription.trim().isEmpty()
                                    ? decodeICalText(currentDescription).trim()
                                    : null;
                                
                                // Google Calendar public iCal feed already includes all occurrences of recurring events
                                // So we just add every event we find, regardless of RRULE
                                // RRULE is only used if we need to generate occurrences (which we don't for public feeds)
                                busyTimes.add(new BusyTime(start, end, title, description));
                                System.out.println("Added event: " + title + " from " + start + " to " + end + 
                                    (currentRRule != null ? " (has RRULE but using direct occurrence)" : ""));
                            } else {
                                System.err.println("Skipping event - invalid dates. Title: " + currentTitle + ", Start: " + start + ", End: " + end);
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing event: " + e.getMessage());
                            System.err.println("Event title: " + currentTitle);
                            System.err.println("Event start: " + currentStart);
                            System.err.println("Event end: " + currentEnd);
                            e.printStackTrace();
                        }
                    }
                }
                inEvent = false;
            } else if (inEvent) {
                // Handle line continuation (iCal format: lines starting with space are continuation)
                if (line.startsWith(" ") && (currentTitle != null || currentDescription != null)) {
                    String continuation = line.substring(1);
                    if (currentTitle != null) {
                        currentTitle += continuation;
                    } else if (currentDescription != null) {
                        currentDescription += continuation;
                    }
                    continue;
                }
                
                // Extract SUMMARY (title)
                Matcher summaryMatcher = summaryPattern.matcher(line);
                if (summaryMatcher.find()) {
                    currentTitle = summaryMatcher.group(1);
                    // Don't decode yet - wait for all continuations
                    continue;
                }
                
                // Extract DESCRIPTION
                Matcher descriptionMatcher = descriptionPattern.matcher(line);
                if (descriptionMatcher.find()) {
                    currentDescription = descriptionMatcher.group(1);
                    // Don't decode yet - wait for all continuations
                    continue;
                }
                
                // Extract RRULE (recurrence rule)
                Matcher rruleMatcher = rrulePattern.matcher(line);
                if (rruleMatcher.find()) {
                    currentRRule = rruleMatcher.group(1);
                    System.out.println("Found RRULE: " + currentRRule);
                    continue;
                }
                
                // Handle line continuation for RRULE (lines starting with space are continuation)
                if (line.startsWith(" ") && currentRRule != null) {
                    currentRRule += line.substring(1);
                    continue;
                }
                
                // Check for VALUE=DATE patterns first (all-day events)
                Matcher startDateMatcher = dtStartDatePattern.matcher(line);
                if (startDateMatcher.find()) {
                    // Mark as date-only format
                    String startDateStr = startDateMatcher.group(1);
                    if (startDateStr.length() == 8) {
                        currentStart = startDateStr + "DATE"; // Marker to indicate date-only
                    } else {
                        currentStart = startDateStr;
                    }
                } else {
                    // Check for regular DTSTART
                    Matcher startMatcher = dtStartPattern.matcher(line);
                    if (startMatcher.find()) {
                        String startValue = startMatcher.group(1);
                        // Check if it's a date-only format (8 digits) even without VALUE=DATE
                        if (startValue.length() == 8 && startValue.matches("\\d{8}")) {
                            currentStart = startValue + "DATE"; // Mark as date-only
                        } else {
                            currentStart = startValue;
                        }
                    }
                }
                
                // Check for VALUE=DATE patterns first (all-day events)
                Matcher endDateMatcher = dtEndDatePattern.matcher(line);
                if (endDateMatcher.find()) {
                    // For all-day events, DTEND is exclusive, so we need to add 1 day
                    // But first check if it's already a date-only format
                    String endDateStr = endDateMatcher.group(1);
                    if (endDateStr.length() == 8) {
                        // This is a date-only format, mark it as such
                        currentEnd = endDateStr + "DATE"; // Marker to indicate date-only
                    } else {
                        currentEnd = endDateStr;
                    }
                } else {
                    // Check for regular DTEND
                    Matcher endMatcher = dtEndPattern.matcher(line);
                    if (endMatcher.find()) {
                        String endValue = endMatcher.group(1);
                        // Check if it's a date-only format (8 digits) even without VALUE=DATE
                        if (endValue.length() == 8 && endValue.matches("\\d{8}")) {
                            currentEnd = endValue + "DATE"; // Mark as date-only
                        } else {
                            currentEnd = endValue;
                        }
                    }
                }
            }
        }
        
        return busyTimes;
    }
    
    /**
     * Parse iCal datetime format
     * Formats: 20240101T120000Z, 20240101T120000, 20240101 (date-only)
     * @param dateTimeStr The datetime string to parse
     * @param isStart Whether this is DTSTART (true) or DTEND (false)
     */
    private LocalDateTime parseDateTime(String dateTimeStr, boolean isStart) {
        try {
            // Check if this is a date-only format (marked with "DATE" suffix)
            boolean isDateOnly = dateTimeStr.endsWith("DATE");
            if (isDateOnly) {
                dateTimeStr = dateTimeStr.replace("DATE", "");
            }
            
            // Remove timezone indicator if present
            dateTimeStr = dateTimeStr.replace("Z", "");
            
            // Handle date-only format (YYYYMMDD) - all-day events
            if (dateTimeStr.length() == 8) {
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                java.time.LocalDate date = java.time.LocalDate.parse(dateTimeStr, dateFormatter);
                
                // For all-day events:
                // - DTSTART: beginning of the day (00:00:00)
                // - DTEND: beginning of the next day (00:00:00), but it's exclusive
                //   So we'll handle the exclusive nature in the calling code
                return date.atStartOfDay();
            }
            
            // Handle datetime format (YYYYMMDDTHHMMSS)
            if (dateTimeStr.length() == 15 && dateTimeStr.contains("T")) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
                return LocalDateTime.parse(dateTimeStr, formatter);
            }
            
            // Handle datetime format with timezone (YYYYMMDDTHHMMSSZ or YYYYMMDDTHHMMSS+0000)
            if (dateTimeStr.length() >= 15 && dateTimeStr.contains("T")) {
                // Try to parse without timezone first
                String dateTimeWithoutTz = dateTimeStr.replaceAll("[+-]\\d{4}$", "").replace("Z", "");
                if (dateTimeWithoutTz.length() == 15) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
                    return LocalDateTime.parse(dateTimeWithoutTz, formatter);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error parsing datetime: " + dateTimeStr + " - " + e.getMessage());
            System.err.println("Datetime string length: " + (dateTimeStr != null ? dateTimeStr.length() : 0));
        }
        
        return null;
    }
    
    /**
     * Decode iCal text - handle escape sequences
     * iCal uses \, for comma, \; for semicolon, \\ for backslash, \n for newline
     */
    private String decodeICalText(String text) {
        if (text == null) return null;
        return text
            .replace("\\,", ",")
            .replace("\\;", ";")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\N", "\n");
    }
    
    /**
     * Check if a time slot is busy
     */
    public boolean isTimeSlotBusy(String calendarUrl, LocalDateTime start, LocalDateTime end) {
        List<BusyTime> busyTimes = fetchBusyTimes(calendarUrl);
        
        for (BusyTime busyTime : busyTimes) {
            // Check if requested time overlaps with busy time
            if (start.isBefore(busyTime.getEnd()) && end.isAfter(busyTime.getStart())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Inner class to represent busy time slot with event details
     */
    public static class BusyTime {
        private LocalDateTime start;
        private LocalDateTime end;
        private String title;
        private String description;
        
        public BusyTime(LocalDateTime start, LocalDateTime end) {
            this.start = start;
            this.end = end;
            this.title = null;
            this.description = null;
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
}


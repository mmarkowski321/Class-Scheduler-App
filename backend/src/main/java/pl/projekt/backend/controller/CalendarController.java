package pl.projekt.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.projekt.backend.model.Calendar;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.CalendarRepository;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.GoogleCalendarService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import pl.projekt.backend.util.AvailabilityUtils;
@RestController
@RequestMapping("/api/calendar")
@CrossOrigin(origins = "http://localhost:5173")
public class CalendarController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private GoogleCalendarService googleCalendarService;
    
    @Autowired
    private LessonRepository lessonRepository;
    
    @Autowired
    private CalendarRepository calendarRepository;
    
    /**
     * Sync calendar and get busy times (works for both tutors and students)
     * If calendarUrl is provided as query parameter, it will be used instead of the one from database
     */
    @GetMapping("/sync/{userId}")
    public ResponseEntity<?> syncCalendar(
            @PathVariable Long userId,
            @RequestParam(required = false) String calendarUrl) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user ID"));
            }
            
            User user = userOpt.get();
            
            // Collect all calendar URLs to sync
            List<String> calendarUrls = new ArrayList<>();
            
            // If calendarUrl provided in query, use it (for compatibility and testing)
            if (calendarUrl != null && !calendarUrl.isBlank()) {
                calendarUrls.add(calendarUrl);
            } else {
                // Get calendars from Calendar table
                List<Calendar> calendars = calendarRepository.findByUserIdAndActiveTrue(userId);
                for (Calendar cal : calendars) {
                    if (cal.getCalendarUrl() != null && !cal.getCalendarUrl().isBlank()) {
                        calendarUrls.add(cal.getCalendarUrl());
                    }
                }
            }
            
            if (calendarUrls.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No calendar URLs configured"));
            }
            
            // Fetch busy times from all calendars
            List<GoogleCalendarService.BusyTime> allBusyTimes = new ArrayList<>();
            for (String url : calendarUrls) {
                try {
                    List<GoogleCalendarService.BusyTime> busyTimes = googleCalendarService.fetchBusyTimes(url);
                    allBusyTimes.addAll(busyTimes);
                } catch (Exception e) {
                    // Log error but continue with other calendars
                    System.err.println("Error syncing calendar " + url + ": " + e.getMessage());
                }
            }
            
            // Convert to JSON-friendly format with event details
            List<Map<String, Object>> busyTimesJson = allBusyTimes.stream()
                .map(bt -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("start", bt.getStart().toString());
                    map.put("end", bt.getEnd().toString());
                    if (bt.getTitle() != null) {
                        map.put("title", bt.getTitle());
                    }
                    if (bt.getDescription() != null) {
                        map.put("description", bt.getDescription());
                    }
                    return map;
                })
                .collect(Collectors.toList());
            
            // If no busy times found, provide helpful error message
            if (allBusyTimes.isEmpty()) {
                // Check if it's a USOS HTML URL
                String warningMessage = "No events found or calendar may not be publicly accessible.";
                if (calendarUrls.stream().anyMatch(url -> url.contains("web.usos.pwr.edu.pl"))) {
                    warningMessage = "The URL appears to be a USOS web page (HTML), not an iCal feed. Please use the iCal feed URL from USOS (e.g., https://apps.usos.pwr.edu.pl/services/tt/upcoming_ical?...).";
                } else if (calendarUrls.stream().anyMatch(url -> url.contains(".php") || url.contains("kontroler.php"))) {
                    warningMessage = "The URL appears to be a web page (HTML), not an iCal feed. Please use the iCal feed URL (.ics) instead.";
                } else {
                    warningMessage = "No events found or calendar may not be publicly accessible. Make sure the calendar is set to public in Google Calendar settings.";
                }
                
                return ResponseEntity.ok(Map.of(
                    "busyTimes", busyTimesJson,
                    "count", 0,
                    "warning", warningMessage
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "busyTimes", busyTimesJson,
                "count", allBusyTimes.size()
            ));
            
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if (errorMessage != null) {
                if (errorMessage.contains("404")) {
                    errorMessage = "Calendar not found. Please ensure the calendar is set to 'Public' in Google Calendar settings.";
                } else if (errorMessage.contains("HTML") || errorMessage.contains("web page")) {
                    errorMessage = "The URL is a web page (HTML), not an iCal feed. For USOS, use the iCal feed URL (https://apps.usos.pwr.edu.pl/services/tt/upcoming_ical?...), not the HTML page URL.";
                }
            }
            return ResponseEntity.badRequest().body(Map.of("error", errorMessage != null ? errorMessage : "Failed to sync calendar"));
        }
    }
    
    /**
     * Check if a specific time slot is busy (works for both tutors and students)
     */
    @PostMapping("/check-busy/{userId}")
    public ResponseEntity<?> checkBusy(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user ID"));
            }
            
            User user = userOpt.get();
            
            if (!(user instanceof Tutor) && !(user instanceof Student)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Calendar check is only available for tutors and students"));
            }
            
            // Get calendars from Calendar table
            List<Calendar> calendars = calendarRepository.findByUserIdAndActiveTrue(userId);
            if (calendars.isEmpty()) {
                return ResponseEntity.ok(Map.of("busy", false, "message", "No calendar configured"));
            }
            
            LocalDateTime start = LocalDateTime.parse(request.get("start"));
            LocalDateTime end = LocalDateTime.parse(request.get("end"));
            
            // Check if time slot is busy in any of the calendars
            boolean isBusy = false;
            for (Calendar cal : calendars) {
                if (cal.getCalendarUrl() != null && !cal.getCalendarUrl().isBlank()) {
                    if (googleCalendarService.isTimeSlotBusy(cal.getCalendarUrl(), start, end)) {
                        isBusy = true;
                        break;
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of("busy", isBusy));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get user's own lessons with full details for calendar display
     * Only accessible by the lesson owner (tutor or student)
     */
    @GetMapping("/lessons/{userId}")
    public ResponseEntity<?> getUserLessons(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user ID"));
            }
            
            User user = userOpt.get();
            List<Lesson> lessons;
            
            // Get lessons based on user type
            if (user instanceof Tutor) {
                lessons = lessonRepository.findByTutorId(userId);
            } else if (user instanceof Student) {
                lessons = lessonRepository.findByStudentId(userId);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Lessons are only available for tutors and students"));
            }
            
            // Convert to JSON format with full details
            lessons.forEach(this::ensureLessonStatus);

            List<Map<String, Object>> lessonsJson = lessons.stream()
                .filter(lesson -> lesson.getStatus() != LessonStatus.CANCELLED
                        && lesson.getStatus() != LessonStatus.REQUESTED)
                .map(lesson -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", lesson.getId());
                    map.put("type", "lesson");
                    if (user instanceof Tutor) {
                        map.put("title", lesson.getStudent().getFirstName() + " " + lesson.getStudent().getLastName());
                        map.put("studentId", lesson.getStudent().getId());
                    } else {
                        map.put("title", lesson.getTutor().getFirstName() + " " + lesson.getTutor().getLastName());
                        map.put("tutorId", lesson.getTutor().getId());
                    }
                    map.put("start", lesson.getStartTime().toString());
                    map.put("end", lesson.getEndTime().toString());
                    map.put("status", lesson.getStatus().toString());
                    map.put("meetingLink", lesson.getMeetingLink());
                    map.put("deliveryMode", lesson.getDeliveryMode() != null ? lesson.getDeliveryMode().name() : null);
                    map.put("onsiteCity", lesson.getOnsiteCity());
                    map.put("onsitePostalCode", lesson.getOnsitePostalCode());
                    map.put("onsiteStreet", lesson.getOnsiteStreet());
                    map.put("onsiteBuilding", lesson.getOnsiteBuilding());
                    map.put("onsiteApartment", lesson.getOnsiteApartment());
                    map.put("notes", lesson.getNotes());
                    return map;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of("lessons", lessonsJson));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get public busy times for a user (without details)
     * Used for viewing other users' profiles - shows only busy slots, not lesson details
     */
    @GetMapping("/public/{userId}")
    public ResponseEntity<?> getPublicBusyTimes(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid user ID"));
            }
            
            User user = userOpt.get();
            
            if (!(user instanceof Tutor) && !(user instanceof Student)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Public calendar is only available for tutors and students"));
            }
            
            // Get lessons (confirmed/scheduled only) - no details, just time slots
            List<Lesson> lessons;
            if (user instanceof Tutor) {
                lessons = lessonRepository.findByTutorId(userId);
            } else {
                lessons = lessonRepository.findByStudentId(userId);
            }
            
            lessons.forEach(this::ensureLessonStatus);

            Map<LocalDate, Long> lessonCounts = lessons.stream()
                .filter(lesson -> lesson.getStartTime() != null)
                .filter(lesson -> lesson.getStatus() == LessonStatus.SCHEDULED
                        || lesson.getStatus() == LessonStatus.RESCHEDULED
                        || lesson.getStatus() == LessonStatus.IN_PROGRESS)
                .collect(Collectors.groupingBy(lesson -> lesson.getStartTime().toLocalDate(), Collectors.counting()));

            Integer maxLessonsPerDay = null;
            int bufferMinutes = 0;
            Set<DayOfWeek> preferredDays = Set.of();
            if (user instanceof Tutor tutor) {
                maxLessonsPerDay = tutor.getMaxLessonsPerDay();
                bufferMinutes = tutor.getBufferTime() != null ? Math.max(0, tutor.getBufferTime()) : 0;
                preferredDays = AvailabilityUtils.parsePreferredDays(tutor.getPreferredDays());
            }
            final Integer effectiveMaxLessonsPerDay = maxLessonsPerDay;
            final int effectiveBufferMinutes = bufferMinutes;
            final Set<DayOfWeek> effectivePreferredDays = preferredDays;

            final Set<LocalDate> limitReachedDays = new HashSet<>();
            if (effectiveMaxLessonsPerDay != null && effectiveMaxLessonsPerDay > 0) {
                lessonCounts.forEach((date, count) -> {
                    if (count >= effectiveMaxLessonsPerDay) {
                        limitReachedDays.add(date);
                    }
                });
            }

        // Convert to busy time slots (without details)
        List<Map<String, String>> busySlots = lessons.stream()
                .filter(lesson -> lesson.getStatus() == LessonStatus.SCHEDULED || 
                                 lesson.getStatus() == LessonStatus.RESCHEDULED ||
                                 lesson.getStatus() == LessonStatus.IN_PROGRESS)
                .filter(lesson -> {
                    if (lesson.getStartTime() == null) {
                        return true;
                    }
                    LocalDate date = lesson.getStartTime().toLocalDate();
                    if (!limitReachedDays.isEmpty() && limitReachedDays.contains(date)) {
                        return false;
                    }
                    if (!effectivePreferredDays.isEmpty()
                        && !effectivePreferredDays.contains(lesson.getStartTime().getDayOfWeek())) {
                        return false;
                    }
                    return true;
                })
                .map(lesson -> {
                    Map<String, String> map = new HashMap<>();
                    LocalDateTime start = lesson.getStartTime();
                    LocalDateTime end = lesson.getEndTime();
                    if (start != null && end != null) {
                        if (effectiveBufferMinutes > 0) {
                            start = start.minusMinutes(effectiveBufferMinutes);
                            end = end.plusMinutes(effectiveBufferMinutes);
                        }
                        map.put("start", start.toString());
                        map.put("end", end.toString());
                    } else {
                        map.put("start", lesson.getStartTime() != null ? lesson.getStartTime().toString() : "");
                        map.put("end", lesson.getEndTime() != null ? lesson.getEndTime().toString() : "");
                    }
                    return map;
                })
                .collect(Collectors.toList());
            
            // Also include Google Calendar busy times from all configured calendars
            List<Calendar> calendars = calendarRepository.findByUserIdAndActiveTrue(userId);
            for (Calendar cal : calendars) {
                if (cal.getCalendarUrl() != null && !cal.getCalendarUrl().isBlank()) {
                    try {
                        List<GoogleCalendarService.BusyTime> googleBusyTimes = googleCalendarService.fetchBusyTimes(cal.getCalendarUrl());
                        List<Map<String, String>> googleBusyJson = googleBusyTimes.stream()
                            .filter(bt -> {
                                if (bt.getStart() == null) {
                                    return true;
                                }
                                LocalDate date = bt.getStart().toLocalDate();
                                if (!limitReachedDays.isEmpty() && limitReachedDays.contains(date)) {
                                    return false;
                                }
                                if (effectivePreferredDays.isEmpty()) {
                                    return true;
                                }
                                return effectivePreferredDays.contains(bt.getStart().getDayOfWeek());
                            })
                            .map(bt -> {
                                Map<String, String> map = new HashMap<>();
                                LocalDateTime start = bt.getStart();
                                LocalDateTime end = bt.getEnd();
                                if (start != null && end != null && effectiveBufferMinutes > 0) {
                                    start = start.minusMinutes(effectiveBufferMinutes);
                                    end = end.plusMinutes(effectiveBufferMinutes);
                                }
                                map.put("start", start != null ? start.toString() : "");
                                map.put("end", end != null ? end.toString() : "");
                                return map;
                            })
                            .collect(Collectors.toList());
                        busySlots.addAll(googleBusyJson);
                    } catch (Exception e) {
                        // If Google Calendar sync fails, just continue with other calendars
                    }
                }
            }
            
        // Mark non-preferred days as busy (for tutors)
        if (user instanceof Tutor tutor) {
            Set<LocalDate> fullDayBlocks = new HashSet<>();
            if (!effectivePreferredDays.isEmpty()) {
                LocalDate current = LocalDate.now();
                LocalDate end = current.plusWeeks(8); // expose unavailable days ~2 months ahead
                while (!current.isAfter(end)) {
                    if (!effectivePreferredDays.contains(current.getDayOfWeek())) {
                        Map<String, String> map = new HashMap<>();
                        map.put("start", current.atStartOfDay().toString());
                        map.put("end", current.plusDays(1).atStartOfDay().toString());
                        busySlots.add(map);
                        fullDayBlocks.add(current);
                    }
                    current = current.plusDays(1);
                }
            }

            if (!limitReachedDays.isEmpty()) {
                for (LocalDate date : limitReachedDays) {
                    if (fullDayBlocks.contains(date)) {
                        continue;
                    }
                    Map<String, String> map = new HashMap<>();
                    map.put("start", date.atStartOfDay().toString());
                    map.put("end", date.plusDays(1).atStartOfDay().toString());
                    busySlots.add(map);
                    fullDayBlocks.add(date);
                }
            }
        }

            return ResponseEntity.ok(Map.of("busyTimes", busySlots, "count", busySlots.size()));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private LessonStatus ensureLessonStatus(Lesson lesson) {
        LessonStatus desired = computeLessonStatus(lesson);
        LessonStatus current = lesson.getStatus();
        if (desired != null && desired != current) {
            lesson.setStatus(desired);
            lessonRepository.save(lesson);
            return desired;
        }
        return current;
    }

    private LessonStatus computeLessonStatus(Lesson lesson) {
        if (lesson == null) return null;
        LessonStatus status = lesson.getStatus();
        if (status == null || status == LessonStatus.CANCELLED || status == LessonStatus.COMPLETED) {
            return status;
        }
        LocalDateTime start = lesson.getStartTime();
        LocalDateTime end = lesson.getEndTime();
        if (start == null || end == null) {
            return status;
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(end) && status != LessonStatus.COMPLETED) {
            return LessonStatus.COMPLETED;
        }
        if (!now.isBefore(start) && now.isBefore(end)
                && (status == LessonStatus.SCHEDULED || status == LessonStatus.RESCHEDULED || status == LessonStatus.IN_PROGRESS)) {
            return LessonStatus.IN_PROGRESS;
        }
        return status;
    }
}


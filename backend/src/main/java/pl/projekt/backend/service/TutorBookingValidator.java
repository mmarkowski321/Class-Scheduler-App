package pl.projekt.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.Calendar;
import pl.projekt.backend.repository.CalendarRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import pl.projekt.backend.util.AvailabilityUtils;

/**
 * Validates tutor availability rules before booking is created.
 */
@Service
public class TutorBookingValidator {

    private final CalendarRepository calendarRepository;
    private final GoogleCalendarService googleCalendarService;

    @Autowired
    public TutorBookingValidator(CalendarRepository calendarRepository,
                                 GoogleCalendarService googleCalendarService) {
        this.calendarRepository = calendarRepository;
        this.googleCalendarService = googleCalendarService;
    }

    /**
     * Validate tutor availability for proposed lesson time.
     *
     * @param tutor          tutor profile
     * @param start          proposed start time
     * @param end            proposed end time
     * @param tutorLessons   existing tutor lessons
     * @return Optional error message if validation fails
     */
    public Optional<String> validate(Tutor tutor,
                                     LocalDateTime start,
                                     LocalDateTime end,
                                     List<Lesson> tutorLessons,
                                     Locale locale) {
        Locale effectiveLocale = resolveLocale(locale);
        if (tutor == null) {
            return Optional.of(message("tutorMissing", effectiveLocale));
        }

        // Preferred days validation
        Set<java.time.DayOfWeek> preferredDays = AvailabilityUtils.parsePreferredDays(tutor.getPreferredDays());
        java.time.DayOfWeek bookingDay = start.getDayOfWeek();
        if (!preferredDays.isEmpty() && !preferredDays.contains(bookingDay)) {
            String allowedDays = preferredDays.stream()
                    .sorted()
                    .map(day -> day.getDisplayName(TextStyle.SHORT, effectiveLocale))
                    .collect(Collectors.joining(", "));
            return Optional.of(message("preferredDays", effectiveLocale, allowedDays));
        }

        // Max lessons per day validation
        Integer maxLessonsPerDay = tutor.getMaxLessonsPerDay();
        if (maxLessonsPerDay != null && maxLessonsPerDay > 0) {
            LocalDate bookingDate = start.toLocalDate();
            long lessonsThatDay = tutorLessons.stream()
                    .filter(this::countsTowardsLimit)
                    .filter(lesson -> {
                        if (lesson.getStartTime() == null) return false;
                        return bookingDate.equals(lesson.getStartTime().toLocalDate());
                    })
                    .count();
            if (lessonsThatDay >= maxLessonsPerDay) {
                return Optional.of(message("maxPerDay", effectiveLocale));
            }
        }

        int bufferMinutes = Math.max(0, Optional.ofNullable(tutor.getBufferTime()).orElse(0));

        // Existing lessons + buffer
        if (hasLessonConflictWithBuffer(tutorLessons, start, end, bufferMinutes)) {
            return Optional.of(message("busyLesson", effectiveLocale));
        }

        // External calendars + buffer
        if (hasExternalConflict(tutor.getId(), start, end, bufferMinutes)) {
            return Optional.of(message("busyCalendar", effectiveLocale));
        }

        return Optional.empty();
    }

    private boolean countsTowardsLimit(Lesson lesson) {
        if (lesson == null || lesson.getStatus() == null) {
            return false;
        }
        LessonStatus status = lesson.getStatus();
        return status == LessonStatus.SCHEDULED
                || status == LessonStatus.RESCHEDULED
                || status == LessonStatus.IN_PROGRESS
                || status == LessonStatus.REQUESTED;
    }

    private boolean hasLessonConflictWithBuffer(List<Lesson> lessons,
                                                LocalDateTime start,
                                                LocalDateTime end,
                                                int bufferMinutes) {
        for (Lesson lesson : lessons) {
            if (lesson == null || lesson.getStartTime() == null || lesson.getEndTime() == null) {
                continue;
            }
            if (!countsTowardsLimit(lesson)) {
                continue; // ignore cancelled / completed lessons for conflict
            }

            LocalDateTime lessonStart = lesson.getStartTime();
            LocalDateTime lessonEnd = lesson.getEndTime();

            if (overlapsWithBuffer(start, end, lessonStart, lessonEnd, bufferMinutes)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasExternalConflict(Long tutorId,
                                        LocalDateTime start,
                                        LocalDateTime end,
                                        int bufferMinutes) {
        if (tutorId == null) {
            return false;
        }
        List<Calendar> calendars = calendarRepository.findByUserIdAndActiveTrue(tutorId);
        if (calendars.isEmpty()) {
            return false;
        }

        for (Calendar calendar : calendars) {
            if (calendar.getCalendarUrl() == null || calendar.getCalendarUrl().isBlank()) {
                continue;
            }
            try {
                List<GoogleCalendarService.BusyTime> busyTimes =
                        googleCalendarService.fetchBusyTimes(calendar.getCalendarUrl());
                for (GoogleCalendarService.BusyTime busy : busyTimes) {
                    if (busy.getStart() == null || busy.getEnd() == null) {
                        continue;
                    }
                    if (overlapsWithBuffer(start, end, busy.getStart(), busy.getEnd(), bufferMinutes)) {
                        return true;
                    }
                }
            } catch (Exception ex) {
                // ignore errors fetching calendar to avoid blocking bookings completely
                System.err.println("Failed to fetch busy times for calendar " + calendar.getId() + ": " + ex.getMessage());
            }
        }

        return false;
    }

    private boolean overlapsWithBuffer(LocalDateTime start,
                                       LocalDateTime end,
                                       LocalDateTime busyStart,
                                       LocalDateTime busyEnd,
                                       int bufferMinutes) {
        LocalDateTime paddedStart = busyStart.minusMinutes(bufferMinutes);
        LocalDateTime paddedEnd = busyEnd.plusMinutes(bufferMinutes);
        return start.isBefore(paddedEnd) && end.isAfter(paddedStart);
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.intValue() != 0;
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private Locale resolveLocale(Locale locale) {
        if (locale == null) {
            return new Locale("pl", "PL");
        }
        String language = locale.getLanguage();
        if ("pl".equalsIgnoreCase(language)) {
            return new Locale("pl", "PL");
        }
        if ("en".equalsIgnoreCase(language)) {
            return Locale.ENGLISH;
        }
        return Locale.ENGLISH;
    }

    private String message(String key, Locale locale, Object... args) {
        boolean isPolish = "pl".equalsIgnoreCase(locale.getLanguage());
        return switch (key) {
            case "tutorMissing" -> isPolish
                    ? "Profil korepetytora nie został odnaleziony."
                    : "Tutor profile was not found.";
            case "preferredDays" -> {
                String days = args.length > 0 ? String.valueOf(args[0]) : "";
                yield isPolish
                        ? "Korepetytor przyjmuje tylko w dniach: " + days + "."
                        : "The tutor accepts bookings only on: " + days + ".";
            }
            case "maxPerDay" -> isPolish
                    ? "Korepetytor osiągnął limit lekcji na ten dzień."
                    : "The tutor has reached the lesson limit for that day.";
            case "busyLesson" -> isPolish
                    ? "Korepetytor jest zajęty w wybranym czasie."
                    : "The tutor is busy at the selected time.";
            case "busyCalendar" -> isPolish
                    ? "Zewnętrzny kalendarz korepetytora blokuje ten termin."
                    : "The tutor's external calendar blocks this slot.";
            default -> isPolish ? "Wystąpił nieznany błąd." : "An unknown error occurred.";
        };
    }
}



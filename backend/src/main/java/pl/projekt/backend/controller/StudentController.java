package pl.projekt.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Review;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.GoogleCalendarService;
import pl.projekt.backend.service.TutorBookingValidator;
import pl.projekt.backend.util.JwtUtil;
import pl.projekt.backend.util.SubjectDictionary;
import pl.projekt.backend.dto.StudentLessonRescheduleRequest;
import pl.projekt.backend.dto.StudentLessonCancelRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private static final String AUTH_STUDENT_ID_ATTR = "authStudentId";
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final GoogleCalendarService googleCalendarService;
    private final ReviewRepository reviewRepository;
    private final JwtUtil jwtUtil;
    private final TutorBookingValidator tutorBookingValidator;

    @Autowired
    public StudentController(LessonRepository lessonRepository,
                             UserRepository userRepository,
                             GoogleCalendarService googleCalendarService,
                             ReviewRepository reviewRepository,
                             JwtUtil jwtUtil,
                             TutorBookingValidator tutorBookingValidator) {
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.googleCalendarService = googleCalendarService;
        this.reviewRepository = reviewRepository;
        this.jwtUtil = jwtUtil;
        this.tutorBookingValidator = tutorBookingValidator;
    }

    @GetMapping("/me/overview")
    public ResponseEntity<?> getStudentOverview(HttpServletRequest request) {
        Locale locale = request.getLocale();
        ResponseEntity<?> accessError = ensureStudentAccess(request, locale);
        if (accessError != null) {
            return accessError;
        }

        Long authId = getAuthenticatedStudentId(request);
        var user = userRepository.findById(authId);
        if (user.isEmpty() || !(user.get() instanceof Student)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", message(locale, "studentMissing")));
        }

        Student student = (Student) user.get();

        List<Lesson> lessons = lessonRepository.findByStudentId(student.getId());
        lessons.forEach(this::ensureLessonStatus);
        LocalDateTime now = LocalDateTime.now();

        Comparator<Lesson> byStartAsc = Comparator.comparing(
                Lesson::getStartTime,
                Comparator.nullsLast(Comparator.naturalOrder())
        );

        Comparator<Lesson> byStartDesc = Comparator.comparing(
                Lesson::getStartTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        );

        List<Map<String, Object>> upcoming = lessons.stream()
                .filter(lesson -> isUpcoming(lesson, now))
                .sorted(byStartAsc)
                .limit(8)
                .map(this::toStudentLessonDto)
                .collect(Collectors.toList());

        List<Map<String, Object>> history = lessons.stream()
                .filter(lesson -> isHistory(lesson, now))
                .sorted(byStartDesc)
                .limit(8)
                .map(this::toStudentLessonDto)
                .collect(Collectors.toList());

        List<Map<String, Object>> attention = lessons.stream()
                .filter(lesson -> needsAttention(lesson, now))
                .sorted(byStartAsc)
                .limit(8)
                .map(this::toStudentLessonDto)
                .collect(Collectors.toList());

        List<Map<String, Object>> newTutors = userRepository.findAll().stream()
                .filter(userItem -> userItem instanceof Tutor)
                .map(userItem -> (Tutor) userItem)
                .filter(tutor -> !Objects.equals(tutor.getId(), student.getId()))
                .filter(tutor -> !Boolean.TRUE.equals(tutor.getBanned()))
                .sorted((a, b) -> {
                    LocalDateTime bCreated = parseCreatedAt(b.getCreatedAt());
                    LocalDateTime aCreated = parseCreatedAt(a.getCreatedAt());
                    if (bCreated == null || aCreated == null) {
                        return 0;
                    }
                    return bCreated.compareTo(aCreated);
                })
                .limit(8)
                .map(this::toTutorPreview)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("upcoming", upcoming);
        response.put("history", history);
        response.put("attention", attention);
        response.put("newTutors", newTutors);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/lessons")
    public ResponseEntity<?> getStudentLessons(HttpServletRequest request) {
        Locale locale = request.getLocale();
        ResponseEntity<?> accessError = ensureStudentAccess(request, locale);
        if (accessError != null) {
            return accessError;
        }
        Long studentId = getAuthenticatedStudentId(request);
        Student student = getStudentById(studentId);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", message(locale, "studentMissing")));
        }
        List<Map<String, Object>> lessons = lessonRepository.findByStudentId(student.getId()).stream()
                .peek(this::ensureLessonStatus)
                .sorted(Comparator.comparing(
                        Lesson::getStartTime,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .map(this::toStudentLessonDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("lessons", lessons));
    }

    @PostMapping("/me/lessons/{lessonId}/cancel")
    public ResponseEntity<?> cancelStudentLesson(
            @PathVariable Long lessonId,
            @RequestBody(required = false) StudentLessonCancelRequest payload,
            HttpServletRequest request) {
        Locale locale = request.getLocale();
        ResponseEntity<?> accessError = ensureStudentAccess(request, locale);
        if (accessError != null) {
            return accessError;
        }
        Long studentId = getAuthenticatedStudentId(request);
        Student student = getStudentById(studentId);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", message(locale, "studentMissing")));
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null || lesson.getStudent() == null || !Objects.equals(lesson.getStudent().getId(), student.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", message(locale, "lessonMissing")));
        }

        if (lesson.getStatus() == LessonStatus.CANCELLED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", message(locale, "lessonAlreadyCancelled")));
        }

        lesson.setStatus(LessonStatus.CANCELLED);
        if (payload != null && StringUtils.hasText(payload.getReason())) {
            String existing = lesson.getNotes();
            List<String> notes = new ArrayList<>();
            if (StringUtils.hasText(existing)) {
                notes.add(existing);
            }
            notes.add("[Student cancel] " + payload.getReason().trim());
            lesson.setNotes(String.join("\n", notes));
        }
        googleCalendarService.deleteLessonEvent(lesson);
        lesson.setGoogleEventId(null);
        lesson.setMeetingLink(null);
        Lesson saved = lessonRepository.save(lesson);
        ensureLessonStatus(saved);
        return ResponseEntity.ok(toStudentLessonDto(saved));
    }

    @PostMapping("/me/lessons/{lessonId}/reschedule")
    public ResponseEntity<?> rescheduleStudentLesson(
            @PathVariable Long lessonId,
            @RequestBody StudentLessonRescheduleRequest payload,
            HttpServletRequest request) {
        Locale locale = request.getLocale();
        ResponseEntity<?> accessError = ensureStudentAccess(request, locale);
        if (accessError != null) {
            return accessError;
        }
        Long studentId = getAuthenticatedStudentId(request);
        Student student = getStudentById(studentId);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", message(locale, "studentMissing")));
        }

        Lesson lesson = lessonRepository.findById(lessonId).orElse(null);
        if (lesson == null || lesson.getStudent() == null || !Objects.equals(lesson.getStudent().getId(), student.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", message(locale, "lessonMissing")));
        }

        if (payload == null || !StringUtils.hasText(payload.getStart())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", message(locale, "invalidStart")));
        }

        LocalDateTime newStart;
        try {
            newStart = LocalDateTime.parse(payload.getStart().trim());
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", message(locale, "invalidStart")));
        }

        int duration = resolveDuration(payload.getDurationMinutes(), lesson);
        if (duration <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", message(locale, "invalidDuration")));
        }

        LocalDateTime newEnd = newStart.plusMinutes(duration);

        if (newStart.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", message(locale, "invalidStartPast")));
        }

        if (hasStudentConflict(student.getId(), newStart, newEnd, lesson.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", message(locale, "studentConflict")));
        }

        Tutor tutor = lesson.getTutor();
        if (tutor != null) {
            List<Lesson> tutorLessons = lessonRepository.findByTutorId(tutor.getId()).stream()
                    .filter(other -> !Objects.equals(other.getId(), lesson.getId()))
                    .collect(Collectors.toList());
            var validation = tutorBookingValidator.validate(tutor, newStart, newEnd, tutorLessons, locale);
            if (validation.isPresent()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", validation.get()));
            }
        }

        googleCalendarService.deleteLessonEvent(lesson);
        lesson.setGoogleEventId(null);
        lesson.setStartTime(newStart);
        lesson.setEndTime(newEnd);
        lesson.setStatus(LessonStatus.RESCHEDULED);
        lesson.setMeetingLink(null);
        if (payload != null && StringUtils.hasText(payload.getNote())) {
            String existing = lesson.getNotes();
            List<String> notes = new ArrayList<>();
            if (StringUtils.hasText(existing)) {
                notes.add(existing);
            }
            notes.add("[Student reschedule] " + payload.getNote().trim());
            lesson.setNotes(String.join("\n", notes));
        }
        Lesson saved = lessonRepository.save(lesson);
        ensureLessonStatus(saved);
        return ResponseEntity.ok(toStudentLessonDto(saved));
    }

    private boolean isUpcoming(Lesson lesson, LocalDateTime now) {
        if (lesson == null || lesson.getStartTime() == null) {
            return false;
        }
        LessonStatus status = lesson.getStatus();
        if (status == null) {
            return false;
        }
        return (status == LessonStatus.SCHEDULED
                || status == LessonStatus.RESCHEDULED
                || status == LessonStatus.IN_PROGRESS)
                && !lesson.getStartTime().isBefore(now.minusHours(1));
    }

    private boolean isHistory(Lesson lesson, LocalDateTime now) {
        if (lesson == null) {
            return false;
        }
        LessonStatus status = lesson.getStatus();
        if (status == null) {
            return false;
        }
        LocalDateTime end = lesson.getEndTime();
        LocalDateTime start = lesson.getStartTime();

        if (status == LessonStatus.COMPLETED) {
            return true;
        }
        if (status == LessonStatus.CANCELLED && start != null && start.isBefore(now)) {
            return true;
        }
        return (status == LessonStatus.SCHEDULED || status == LessonStatus.RESCHEDULED || status == LessonStatus.IN_PROGRESS)
                && end != null && end.isBefore(now.minusMinutes(5));
    }

    private boolean needsAttention(Lesson lesson, LocalDateTime now) {
        if (lesson == null) {
            return false;
        }
        LessonStatus status = lesson.getStatus();
        if (status == null) {
            return false;
        }
        LocalDateTime start = lesson.getStartTime();

        if (status == LessonStatus.REQUESTED) {
            return true;
        }
        if (status == LessonStatus.CANCELLED) {
            return start == null || !start.isBefore(now.minusDays(30));
        }
        if (status == LessonStatus.RESCHEDULED) {
            return true;
        }
        return false;
    }

    private Map<String, Object> toStudentLessonDto(Lesson lesson) {
        Map<String, Object> dto = new HashMap<>();
        LessonStatus status = ensureLessonStatus(lesson);
        LocalDateTime start = lesson.getStartTime();
        LocalDateTime end = lesson.getEndTime();
        dto.put("id", lesson.getId());
        dto.put("status", status != null ? status.name() : null);
        dto.put("start", start != null ? start.toString() : null);
        dto.put("end", end != null ? end.toString() : null);
        if (start != null && end != null) {
            dto.put("durationMinutes", Duration.between(start, end).toMinutes());
        }
        dto.put("meetingLink", lesson.getMeetingLink());
        dto.put("deliveryMode", lesson.getDeliveryMode() != null ? lesson.getDeliveryMode().name() : null);
        dto.put("onsiteCity", lesson.getOnsiteCity());
        dto.put("onsitePostalCode", lesson.getOnsitePostalCode());
        dto.put("onsiteStreet", lesson.getOnsiteStreet());
        dto.put("onsiteBuilding", lesson.getOnsiteBuilding());
        dto.put("onsiteApartment", lesson.getOnsiteApartment());
        dto.put("notes", lesson.getNotes());

        Review review = reviewRepository.findByLessonId(lesson.getId()).orElse(null);
        dto.put("studentReviewSubmitted", review != null && review.getStudentReviewAt() != null);
        dto.put("tutorReviewSubmitted", review != null && review.getTutorReviewAt() != null);

        Tutor tutor = lesson.getTutor();
        if (tutor != null) {
            Map<String, Object> tutorDto = new HashMap<>();
            tutorDto.put("id", tutor.getId());
            tutorDto.put("firstName", tutor.getFirstName());
            tutorDto.put("lastName", tutor.getLastName());
            tutorDto.put("email", tutor.getEmail());
            dto.put("tutor", tutorDto);
            dto.put("tutorName", tutor.getFirstName() + " " + tutor.getLastName());
        }
        return dto;
    }

    private Map<String, Object> toTutorPreview(Tutor tutor) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", tutor.getId());
        dto.put("firstName", tutor.getFirstName());
        dto.put("lastName", tutor.getLastName());
        dto.put("createdAt", tutor.getCreatedAt());
        dto.put("hourlyRate", tutor.getHourlyRate());

        Set<String> subjects = SubjectDictionary.extractSubjects(tutor.getSubjects());
        dto.put("subjects", subjects);
        return dto;
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    private LocalDateTime parseCreatedAt(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.replace("Z", ""));
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String message(Locale locale, String key) {
        boolean polish = locale != null && locale.getLanguage() != null
                && locale.getLanguage().toLowerCase(Locale.ROOT).startsWith("pl");
        return switch (key) {
            case "authRequired" -> polish ? "Wymagane jest zalogowanie." : "You need to be logged in.";
            case "invalidToken" -> polish ? "Nieprawidłowy lub wygasły token." : "Invalid or expired token.";
            case "studentOnly" -> polish ? "Ta sekcja jest dostępna tylko dla uczniów."
                    : "This section is only available for students.";
            case "studentMissing" -> polish ? "Nie odnaleziono profilu ucznia."
                    : "Student profile not found.";
            case "lessonMissing" -> polish ? "Nie znaleziono zajęć." : "Lesson not found.";
            case "lessonAlreadyCancelled" -> polish ? "Te zajęcia są już odwołane." : "Lesson already cancelled.";
            case "lessonTooLate" -> polish ? "Nie można modyfikować zajęć na mniej niż 24 godziny przed rozpoczęciem."
                    : "Cannot modify lessons less than 24 hours before start.";
            case "invalidStart" -> polish ? "Podaj prawidłową datę i godzinę." : "Provide a valid date and time.";
            case "invalidStartPast" -> polish ? "Nowy termin musi być w przyszłości." : "New time must be in the future.";
            case "invalidDuration" -> polish ? "Nieprawidłowy czas trwania zajęć." : "Invalid lesson duration.";
            case "studentConflict" -> polish ? "Masz już wtedy inne zajęcia." : "You already have another lesson then.";
            default -> polish ? "Wystąpił nieoczekiwany błąd." : "An unexpected error occurred.";
        };
    }

    private ResponseEntity<?> ensureStudentAccess(HttpServletRequest request, Locale locale) {
        String token = extractToken(request);
        if (!StringUtils.hasText(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", message(locale, "authRequired")));
        }
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", message(locale, "invalidToken")));
        }
        Long authId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);
        if (!"STUDENT".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", message(locale, "studentOnly")));
        }
        request.setAttribute(AUTH_STUDENT_ID_ATTR, authId);
        return null;
    }

    private Long getAuthenticatedStudentId(HttpServletRequest request) {
        Object attr = request.getAttribute(AUTH_STUDENT_ID_ATTR);
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return null;
    }

    private Student getStudentById(Long id) {
        if (id == null) return null;
        return userRepository.findById(id)
                .filter(user -> user instanceof Student)
                .map(user -> (Student) user)
                .orElse(null);
    }

    private boolean canModifyLesson(Lesson lesson) {
        if (lesson == null) {
            return true;
        }
        LocalDateTime start = lesson.getStartTime();
        if (start == null) {
            return true;
        }
        return !start.isBefore(LocalDateTime.now());
    }

    private int resolveDuration(Integer requested, Lesson lesson) {
        if (requested != null && requested > 0) {
            return requested;
        }
        if (lesson.getStartTime() != null && lesson.getEndTime() != null) {
            return (int) Duration.between(lesson.getStartTime(), lesson.getEndTime()).toMinutes();
        }
        if (lesson.getTutor() != null && lesson.getTutor().getLessonDuration() != null) {
            return lesson.getTutor().getLessonDuration();
        }
        return 60;
    }

    private boolean hasStudentConflict(Long studentId, LocalDateTime start, LocalDateTime end, Long excludeLessonId) {
        return lessonRepository.findByStudentId(studentId).stream()
                .filter(other -> excludeLessonId == null || !Objects.equals(other.getId(), excludeLessonId))
                .filter(other -> other.getStartTime() != null && other.getEndTime() != null)
                .anyMatch(other -> overlaps(other.getStartTime(), other.getEndTime(), start, end));
    }

    private boolean overlaps(LocalDateTime startA, LocalDateTime endA, LocalDateTime startB, LocalDateTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
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



package pl.projekt.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.projekt.backend.dto.TutorBookingRequest;
import pl.projekt.backend.dto.TutorPublicDto;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.EmailService;
import pl.projekt.backend.util.JwtUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tutors")
@CrossOrigin(origins = "http://localhost:5173")
public class TutorController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    @GetMapping
    public ResponseEntity<?> listTutors(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false, name = "days") List<String> days) {

        Set<String> dayFilter = normalizeList(days);
        String query = normalize(q);
        String subjectFilter = normalize(subject);
        String cityFilter = normalize(city);

        List<TutorPublicDto> tutors = userRepository.findAll().stream()
                .filter(user -> user instanceof Tutor)
                .map(user -> (Tutor) user)
                .filter(tutor -> !Boolean.TRUE.equals(tutor.getBanned()))
                .filter(tutor -> matchesQuery(tutor, query))
                .filter(tutor -> matchesSubject(tutor, subjectFilter))
                .filter(tutor -> matchesCity(tutor, cityFilter))
                .filter(tutor -> matchesPrice(tutor, maxPrice))
                .filter(tutor -> matchesDays(tutor, dayFilter))
                .map(TutorPublicDto::fromEntity)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("count", tutors.size());
        response.put("tutors", tutors);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTutor(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty() || !(userOpt.get() instanceof Tutor)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Tutor not found"));
        }

        Tutor tutor = (Tutor) userOpt.get();
        if (Boolean.TRUE.equals(tutor.getBanned())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Tutor not available"));
        }

        return ResponseEntity.ok(TutorPublicDto.fromEntity(tutor));
    }

    @PostMapping("/{id}/bookings")
    public ResponseEntity<?> bookTutor(
            @PathVariable Long id,
            @RequestBody TutorBookingRequest payload,
            HttpServletRequest request) {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Authorization token required"));
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }

        Long studentId = jwtUtil.getUserIdFromToken(token);

        Optional<User> tutorOpt = userRepository.findById(id);
        if (tutorOpt.isEmpty() || !(tutorOpt.get() instanceof Tutor)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Tutor not found"));
        }
        Tutor tutor = (Tutor) tutorOpt.get();
        if (Boolean.TRUE.equals(tutor.getBanned())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Tutor is not accepting bookings"));
        }

        Optional<User> studentOpt = userRepository.findById(studentId);
        if (studentOpt.isEmpty() || !(studentOpt.get() instanceof Student)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Only students can book lessons"));
        }
        Student student = (Student) studentOpt.get();

        if (payload == null || !StringUtils.hasText(payload.getStart())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Start date is required"));
        }

        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(payload.getStart());
            if (StringUtils.hasText(payload.getEnd())) {
                end = LocalDateTime.parse(payload.getEnd());
            } else {
                int duration = resolveDurationMinutes(payload.getDurationMinutes(), tutor.getLessonDuration());
                end = start.plusMinutes(duration);
            }
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid date format"));
        }

        if (!end.isAfter(start)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "End time must be after start time"));
        }

        if (start.isBefore(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cannot book past time slots"));
        }

        if (hasConflict(lessonRepository.findByTutorId(id), start, end)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Tutor is busy at that time"));
        }

        if (hasConflict(lessonRepository.findByStudentId(student.getId()), start, end)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "You already have a lesson scheduled at that time"));
        }

        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(start);
        lesson.setEndTime(end);
        lesson.setStatus(LessonStatus.REQUESTED);
        lesson.setNotes(payload.getNotes());

        Lesson saved = lessonRepository.save(lesson);
        sendTutorBookingEmail(tutor, student, saved);

        Map<String, Object> response = new HashMap<>();
        response.put("lessonId", saved.getId());
        response.put("status", saved.getStatus());
        response.put("start", saved.getStartTime().toString());
        response.put("end", saved.getEndTime().toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/bookings")
    public ResponseEntity<?> getTutorBookings(@PathVariable Long id, HttpServletRequest request) {
        ResponseEntity<?> accessError = ensureTutorAccess(request, id, true);
        if (accessError != null) {
            return accessError;
        }
        return buildTutorBookingsResponse(id);
    }

    @PostMapping("/{id}/bookings/{lessonId}/confirm")
    public ResponseEntity<?> confirmBooking(@PathVariable Long id,
                                            @PathVariable Long lessonId,
                                            HttpServletRequest request) {
        ResponseEntity<?> accessError = ensureTutorAccess(request, id, true);
        if (accessError != null) {
            return accessError;
        }
        return confirmBookingInternal(id, lessonId);
    }

    @GetMapping("/{id}/overview")
    public ResponseEntity<?> getTutorOverview(@PathVariable Long id, HttpServletRequest request) {
        Long authIdFromToken = getAuthenticatedTutorId(request);
        ResponseEntity<?> accessError = ensureTutorAccess(request, authIdFromToken != null ? authIdFromToken : id, false);
        if (accessError != null) {
            return accessError;
        }

        Long targetId = authIdFromToken != null ? authIdFromToken : id;
        return buildTutorOverviewResponse(targetId);
    }

    @PostMapping("/{id}/bookings/{lessonId}/decline")
    public ResponseEntity<?> declineBooking(@PathVariable Long id,
                                            @PathVariable Long lessonId,
                                            HttpServletRequest request) {
        ResponseEntity<?> accessError = ensureTutorAccess(request, id, true);
        if (accessError != null) {
            return accessError;
        }
        return declineBookingInternal(id, lessonId);
    }

    @GetMapping("/me/bookings")
    public ResponseEntity<?> getTutorBookingsMe(HttpServletRequest request) {
        ResponseEntity<?> accessError = ensureTutorAccess(request, null, true);
        if (accessError != null) {
            return accessError;
        }
        Long authId = getAuthenticatedTutorId(request);
        if (authId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tutor access required"));
        }
        return buildTutorBookingsResponse(authId);
    }

    @PostMapping("/me/bookings/{lessonId}/confirm")
    public ResponseEntity<?> confirmBookingMe(@PathVariable Long lessonId, HttpServletRequest request) {
        ResponseEntity<?> accessError = ensureTutorAccess(request, null, true);
        if (accessError != null) {
            return accessError;
        }
        Long authId = getAuthenticatedTutorId(request);
        if (authId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tutor access required"));
        }
        return confirmBookingInternal(authId, lessonId);
    }

    @PostMapping("/me/bookings/{lessonId}/decline")
    public ResponseEntity<?> declineBookingMe(@PathVariable Long lessonId, HttpServletRequest request) {
        ResponseEntity<?> accessError = ensureTutorAccess(request, null, true);
        if (accessError != null) {
            return accessError;
        }
        Long authId = getAuthenticatedTutorId(request);
        if (authId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tutor access required"));
        }
        return declineBookingInternal(authId, lessonId);
    }

    @GetMapping("/me/overview")
    public ResponseEntity<?> getTutorOverviewMe(HttpServletRequest request) {
        ResponseEntity<?> accessError = ensureTutorAccess(request, null, false);
        if (accessError != null) {
            return accessError;
        }
        Long authId = getAuthenticatedTutorId(request);
        if (authId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tutor access required"));
        }
        return buildTutorOverviewResponse(authId);
    }

    private ResponseEntity<?> buildTutorBookingsResponse(Long tutorId) {
        Optional<User> tutorOpt = userRepository.findById(tutorId);
        if (tutorOpt.isEmpty() || !(tutorOpt.get() instanceof Tutor)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Tutor not found"));
        }

        List<Lesson> lessons = lessonRepository.findByTutorId(tutorId);
        Comparator<Lesson> byStart = Comparator.comparing(Lesson::getStartTime);

        List<Map<String, Object>> requests = lessons.stream()
                .filter(lesson -> lesson.getStatus() == LessonStatus.REQUESTED)
                .sorted(byStart)
                .map(this::toTutorLessonDto)
                .collect(Collectors.toList());

        List<Map<String, Object>> confirmed = lessons.stream()
                .filter(lesson -> lesson.getStatus() == LessonStatus.SCHEDULED
                        || lesson.getStatus() == LessonStatus.RESCHEDULED
                        || lesson.getStatus() == LessonStatus.IN_PROGRESS)
                .sorted(byStart)
                .map(this::toTutorLessonDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "requests", requests,
                "confirmed", confirmed
        ));
    }

    private ResponseEntity<?> confirmBookingInternal(Long tutorId, Long lessonId) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Booking not found"));
        }

        Lesson lesson = lessonOpt.get();
        if (!Objects.equals(lesson.getTutor().getId(), tutorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Booking does not belong to this tutor"));
        }

        if (lesson.getStatus() != LessonStatus.REQUESTED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only requested lessons can be confirmed"));
        }

        lesson.setStatus(LessonStatus.SCHEDULED);
        lessonRepository.save(lesson);

        return ResponseEntity.ok(toTutorLessonDto(lesson));
    }

    private ResponseEntity<?> declineBookingInternal(Long tutorId, Long lessonId) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Booking not found"));
        }

        Lesson lesson = lessonOpt.get();
        if (!Objects.equals(lesson.getTutor().getId(), tutorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Booking does not belong to this tutor"));
        }

        if (lesson.getStatus() == LessonStatus.CANCELLED) {
            return ResponseEntity.badRequest().body(Map.of("error", "Booking already cancelled"));
        }

        lesson.setStatus(LessonStatus.CANCELLED);
        lessonRepository.save(lesson);

        return ResponseEntity.ok(Map.of("lessonId", lesson.getId(), "status", lesson.getStatus()));
    }

    private ResponseEntity<?> buildTutorOverviewResponse(Long tutorId) {
        Optional<User> tutorOpt = userRepository.findById(tutorId);
        if (tutorOpt.isEmpty() || !(tutorOpt.get() instanceof Tutor)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Tutor not found"));
        }

        Tutor tutor = (Tutor) tutorOpt.get();

        List<Lesson> lessons = lessonRepository.findByTutorId(tutorId);
        LocalDateTime now = LocalDateTime.now();

        Comparator<Lesson> byStart = Comparator.comparing(Lesson::getStartTime);

        List<Map<String, Object>> upcoming = lessons.stream()
                .filter(lesson -> (lesson.getStatus() == LessonStatus.SCHEDULED
                        || lesson.getStatus() == LessonStatus.RESCHEDULED
                        || lesson.getStatus() == LessonStatus.IN_PROGRESS)
                        && lesson.getStartTime().isAfter(now.minusHours(1)))
                .sorted(byStart)
                .limit(5)
                .map(this::toOverviewLessonDto)
                .collect(Collectors.toList());

        List<Map<String, Object>> requests = lessons.stream()
                .filter(lesson -> lesson.getStatus() == LessonStatus.REQUESTED)
                .sorted(byStart)
                .limit(5)
                .map(this::toOverviewLessonDto)
                .collect(Collectors.toList());

        List<Map<String, Object>> reviews = reviewRepository.findByTutorId(tutorId).stream()
                .sorted((a, b) -> {
                    LocalDateTime bTime = b.getCreatedAt();
                    LocalDateTime aTime = a.getCreatedAt();
                    if (bTime == null || aTime == null) {
                        return b.getId().compareTo(a.getId());
                    }
                    return bTime.compareTo(aTime);
                })
                .limit(5)
                .map(review -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", review.getId());
                    dto.put("rating", review.getTutorRating());
                    dto.put("comment", review.getComment());
                    dto.put("createdAt", review.getCreatedAt() != null ? review.getCreatedAt().toString() : null);
                    if (review.getStudent() != null) {
                        Map<String, Object> studentDto = new HashMap<>();
                        studentDto.put("firstName", review.getStudent().getFirstName());
                        studentDto.put("lastName", review.getStudent().getLastName());
                        dto.put("student", studentDto);
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> newTutors = userRepository.findAll().stream()
                .filter(user -> user instanceof Tutor)
                .map(user -> (Tutor) user)
                .filter(other -> !Objects.equals(other.getId(), tutor.getId()))
                .filter(other -> !Boolean.TRUE.equals(other.getBanned()))
                .sorted((a, b) -> {
                    LocalDateTime bCreated = parseCreatedAt(b.getCreatedAt());
                    LocalDateTime aCreated = parseCreatedAt(a.getCreatedAt());
                    if (bCreated == null || aCreated == null) {
                        return 0;
                    }
                    return bCreated.compareTo(aCreated);
                })
                .limit(5)
                .map(other -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("id", other.getId());
                    dto.put("firstName", other.getFirstName());
                    dto.put("lastName", other.getLastName());
                    dto.put("createdAt", other.getCreatedAt());
                    dto.put("hourlyRate", other.getHourlyRate());
                    dto.put("subjects", tutorSubjectsPreview(other.getSubjects()));
                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "upcoming", upcoming,
                "requests", requests,
                "reviews", reviews,
                "newTutors", newTutors
        ));
    }

    private boolean matchesQuery(Tutor tutor, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(tutor.getFirstName(), needle)
                || containsIgnoreCase(tutor.getLastName(), needle)
                || containsIgnoreCase(tutor.getSubjects(), needle)
                || containsIgnoreCase(tutor.getTeachingLanguages(), needle);
    }

    private boolean matchesSubject(Tutor tutor, String subject) {
        if (!StringUtils.hasText(subject)) {
            return true;
        }
        return containsIgnoreCase(tutor.getSubjects(), subject);
    }

    private boolean matchesCity(Tutor tutor, String city) {
        if (!StringUtils.hasText(city)) {
            return true;
        }
        return containsIgnoreCase(tutor.getCity(), city);
    }

    private boolean matchesPrice(Tutor tutor, Double maxPrice) {
        if (maxPrice == null) {
            return true;
        }
        Double rate = tutor.getHourlyRate();
        return rate != null && rate <= maxPrice;
    }

    private boolean matchesDays(Tutor tutor, Set<String> days) {
        if (days.isEmpty()) {
            return true;
        }
        Set<String> tutorDays = parseCsvToSet(tutor.getPreferredDays());
        if (tutorDays.isEmpty()) {
            return false;
        }
        tutorDays.retainAll(days);
        return !tutorDays.isEmpty();
    }

    private boolean hasConflict(List<Lesson> lessons, LocalDateTime start, LocalDateTime end) {
        return lessons.stream()
                .filter(lesson -> lesson.getStatus() == LessonStatus.SCHEDULED
                        || lesson.getStatus() == LessonStatus.RESCHEDULED
                        || lesson.getStatus() == LessonStatus.IN_PROGRESS
                        || lesson.getStatus() == LessonStatus.REQUESTED)
                .anyMatch(lesson -> overlaps(lesson.getStartTime(), lesson.getEndTime(), start, end));
    }

    private boolean overlaps(LocalDateTime aStart, LocalDateTime aEnd, LocalDateTime bStart, LocalDateTime bEnd) {
        return aStart.isBefore(bEnd) && bStart.isBefore(aEnd);
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        if (!StringUtils.hasText(haystack) || !StringUtils.hasText(needle)) {
            return false;
        }
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : null;
    }

    private Set<String> normalizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return new HashSet<>();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private Set<String> parseCsvToSet(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new HashSet<>();
        }
        return Arrays.stream(raw.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toSet());
    }

    private int resolveDurationMinutes(Integer requested, Integer tutorDefault) {
        if (requested != null && requested > 0) {
            return requested;
        }
        if (tutorDefault != null && tutorDefault > 0) {
            return tutorDefault;
        }
        return 60;
    }

    private void sendTutorBookingEmail(Tutor tutor, Student student, Lesson lesson) {
        try {
            emailService.sendTutorBookingRequestEmail(tutor, student, lesson);
        } catch (Exception ignored) {
        }
    }

    private static final String AUTH_TUTOR_ID_ATTR = "authTutorId";

    private ResponseEntity<?> ensureTutorAccess(HttpServletRequest request, Long tutorId, boolean strictMatch) {
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authorization token required"));
        }
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired token"));
        }
        Long authId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);
        if (!"TUTOR".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tutor access required"));
        }
        request.setAttribute(AUTH_TUTOR_ID_ATTR, authId);
        if (strictMatch) {
            Long target = tutorId != null ? tutorId : authId;
            if (!Objects.equals(authId, target)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tutor access required"));
            }
        }
        return null;
    }

    private Long getAuthenticatedTutorId(HttpServletRequest request) {
        Object attr = request.getAttribute(AUTH_TUTOR_ID_ATTR);
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return null;
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    private Map<String, Object> toTutorLessonDto(Lesson lesson) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", lesson.getId());
        dto.put("status", lesson.getStatus().toString());
        dto.put("start", lesson.getStartTime().toString());
        dto.put("end", lesson.getEndTime().toString());
        dto.put("durationMinutes", Duration.between(lesson.getStartTime(), lesson.getEndTime()).toMinutes());
        dto.put("notes", lesson.getNotes());

        Student student = lesson.getStudent();
        Map<String, Object> studentDto = new HashMap<>();
        studentDto.put("id", student.getId());
        studentDto.put("firstName", student.getFirstName());
        studentDto.put("lastName", student.getLastName());
        studentDto.put("email", student.getEmail());
        dto.put("student", studentDto);

        return dto;
    }

    private Map<String, Object> toOverviewLessonDto(Lesson lesson) {
        Map<String, Object> dto = toTutorLessonDto(lesson);
        Student student = lesson.getStudent();
        if (student != null) {
            dto.put("studentName", student.getFirstName() + " " + student.getLastName());
        }
        return dto;
    }

    private LocalDateTime parseCreatedAt(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(raw.replace("Z", ""));
        } catch (Exception ignored) {
        }
        return null;
    }

    private List<String> tutorSubjectsPreview(String subjectsRaw) {
        if (!StringUtils.hasText(subjectsRaw)) {
            return List.of();
        }
        return Arrays.stream(subjectsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(3)
                .collect(Collectors.toList());
    }
}



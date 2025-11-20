package pl.projekt.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import pl.projekt.backend.dto.TutorLessonDeclineRequest;
import pl.projekt.backend.dto.TutorLessonProposalRequest;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Review;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.EmailService;
import pl.projekt.backend.service.GoogleCalendarService;
import pl.projekt.backend.service.MeetingLinkService;
import pl.projekt.backend.service.TutorBookingValidator;
import pl.projekt.backend.util.JwtUtil;
import pl.projekt.backend.util.AvailabilityUtils;
import pl.projekt.backend.util.SubjectDictionary;

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    @Autowired
    private EmailService emailService;

    @Autowired
    private MeetingLinkService meetingLinkService;

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @Autowired
    private TutorBookingValidator tutorBookingValidator;

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
                .filter(this::hasCompleteProfile)
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

        Locale locale = request.getLocale();
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

        LessonDeliveryMode deliveryMode = LessonDeliveryMode.fromString(payload.getDeliveryMode());
        if (deliveryMode == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", tutorMessage(locale,
                            "Wybierz, czy lekcja ma być online, czy stacjonarnie.",
                            "Select whether the lesson is online or onsite.")));
        }

        if (!tutorSupportsMode(tutor, deliveryMode)) {
            boolean online = deliveryMode == LessonDeliveryMode.ONLINE;
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", tutorMessage(locale,
                            online
                                    ? "Ten korepetytor nie prowadzi lekcji online."
                                    : "Ten korepetytor nie prowadzi lekcji stacjonarnych.",
                            online
                                    ? "This tutor does not teach online."
                                    : "This tutor does not teach onsite.")));
        }

        OnsiteAddress onsiteAddress = null;
        if (deliveryMode == LessonDeliveryMode.ONSITE) {
            onsiteAddress = extractOnsiteAddress(payload);
            if (!onsiteAddress.isComplete()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", tutorMessage(locale,
                                "Podaj pełny adres (miasto, kod pocztowy, ulicę i numer).",
                                "Provide the full address (city, postal code, street and building number).")));
            }
        }

        List<Lesson> tutorLessons = lessonRepository.findByTutorId(id);
        Optional<String> availabilityError = tutorBookingValidator.validate(
                tutor, start, end, tutorLessons, locale);
        if (availabilityError.isPresent()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", availabilityError.get()));
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
        lesson.setDeliveryMode(deliveryMode);
        lesson.setNotes(payload.getNotes());
        if (deliveryMode == LessonDeliveryMode.ONSITE && onsiteAddress != null) {
            lesson.setOnsiteCity(onsiteAddress.city());
            lesson.setOnsitePostalCode(onsiteAddress.postalCode());
            lesson.setOnsiteStreet(onsiteAddress.street());
            lesson.setOnsiteBuilding(onsiteAddress.building());
            lesson.setOnsiteApartment(onsiteAddress.apartment());
            lesson.setMeetingLink(null);
        } else {
            lesson.setOnsiteCity(null);
            lesson.setOnsitePostalCode(null);
            lesson.setOnsiteStreet(null);
            lesson.setOnsiteBuilding(null);
            lesson.setOnsiteApartment(null);
            lesson.setMeetingLink(null);
        }

        Lesson saved = lessonRepository.save(lesson);
        sendTutorBookingEmail(tutor, student, saved);

        Map<String, Object> response = new HashMap<>();
        response.put("lessonId", saved.getId());
        response.put("status", saved.getStatus());
        response.put("start", saved.getStartTime().toString());
        response.put("end", saved.getEndTime().toString());
        response.put("deliveryMode", saved.getDeliveryMode() != null ? saved.getDeliveryMode().name() : null);
        if (saved.getDeliveryMode() == LessonDeliveryMode.ONSITE) {
            response.put("onsiteCity", saved.getOnsiteCity());
            response.put("onsitePostalCode", saved.getOnsitePostalCode());
            response.put("onsiteStreet", saved.getOnsiteStreet());
            response.put("onsiteBuilding", saved.getOnsiteBuilding());
            response.put("onsiteApartment", saved.getOnsiteApartment());
        }
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
        return confirmBookingInternal(id, lessonId, request.getLocale());
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
                                            @RequestBody(required = false) TutorLessonDeclineRequest payload,
                                            HttpServletRequest request) {
        ResponseEntity<?> accessError = ensureTutorAccess(request, id, true);
        if (accessError != null) {
            return accessError;
        }
        return declineBookingInternal(id, lessonId, payload, request.getLocale());
    }

    @PostMapping("/{id}/bookings/{lessonId}/propose")
    public ResponseEntity<?> proposeBooking(@PathVariable Long id,
                                            @PathVariable Long lessonId,
                                            @RequestBody TutorLessonProposalRequest payload,
                                            HttpServletRequest request) {
        ResponseEntity<?> accessError = ensureTutorAccess(request, id, true);
        if (accessError != null) {
            return accessError;
        }
        return proposeBookingInternal(id, lessonId, payload, request.getLocale());
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
        return confirmBookingInternal(authId, lessonId, request.getLocale());
    }

    @PostMapping("/me/bookings/{lessonId}/decline")
    public ResponseEntity<?> declineBookingMe(@PathVariable Long lessonId,
                                              @RequestBody(required = false) TutorLessonDeclineRequest payload,
                                              HttpServletRequest request) {
        ResponseEntity<?> accessError = ensureTutorAccess(request, null, true);
        if (accessError != null) {
            return accessError;
        }
        Long authId = getAuthenticatedTutorId(request);
        if (authId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tutor access required"));
        }
        return declineBookingInternal(authId, lessonId, payload, request.getLocale());
    }

    @PostMapping("/me/bookings/{lessonId}/propose")
    public ResponseEntity<?> proposeBookingMe(@PathVariable Long lessonId,
                                              @RequestBody TutorLessonProposalRequest payload,
                                              HttpServletRequest request) {
        ResponseEntity<?> accessError = ensureTutorAccess(request, null, true);
        if (accessError != null) {
            return accessError;
        }
        Long authId = getAuthenticatedTutorId(request);
        if (authId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Tutor access required"));
        }
        return proposeBookingInternal(authId, lessonId, payload, request.getLocale());
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
        lessons.forEach(this::ensureLessonStatus);
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

    private ResponseEntity<?> confirmBookingInternal(Long tutorId, Long lessonId, Locale locale) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", tutorMessage(locale, "Nie znaleziono rezerwacji.", "Booking not found")));
        }

        Lesson lesson = lessonOpt.get();
        if (!Objects.equals(lesson.getTutor().getId(), tutorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", tutorMessage(locale, "Rezerwacja nie należy do tego korepetytora.", "Booking does not belong to this tutor")));
        }

        if (lesson.getStatus() != LessonStatus.REQUESTED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", tutorMessage(locale, "Można potwierdzić tylko oczekujące rezerwacje.", "Only requested lessons can be confirmed")));
        }

        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setMeetingLink(null);
        lesson.setGoogleEventId(null);

        boolean onlineLesson = lesson.getDeliveryMode() == LessonDeliveryMode.ONLINE;
        if (onlineLesson && googleCalendarService.isEnabled()) {
            googleCalendarService.createLessonEvent(lesson).ifPresent(event -> {
                lesson.setGoogleEventId(event.eventId());
                if (onlineLesson && StringUtils.hasText(event.hangoutLink())) {
                    lesson.setMeetingLink(event.hangoutLink());
                }
            });
        }

        if (onlineLesson && !StringUtils.hasText(lesson.getMeetingLink())) {
            lesson.setMeetingLink(meetingLinkService.generateJitsiLink(lesson));
        }

        lessonRepository.save(lesson);
        ensureLessonStatus(lesson);

        return ResponseEntity.ok(toTutorLessonDto(lesson));
    }

    private ResponseEntity<?> declineBookingInternal(Long tutorId,
                                                     Long lessonId,
                                                     TutorLessonDeclineRequest payload,
                                                     Locale locale) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", tutorMessage(locale, "Nie znaleziono rezerwacji.", "Booking not found")));
        }

        Lesson lesson = lessonOpt.get();
        if (!Objects.equals(lesson.getTutor().getId(), tutorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", tutorMessage(locale, "Rezerwacja nie należy do tego korepetytora.", "Booking does not belong to this tutor")));
        }

        if (lesson.getStatus() == LessonStatus.CANCELLED) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", tutorMessage(locale, "Rezerwacja jest już odwołana.", "Booking already cancelled")));
        }

        lesson.setStatus(LessonStatus.CANCELLED);
        googleCalendarService.deleteLessonEvent(lesson);
        lesson.setGoogleEventId(null);
        if (payload != null && StringUtils.hasText(payload.getReason())) {
            appendNote(lesson, "[Tutor decline]", payload.getReason());
        }
        lesson.setMeetingLink(null);
        lessonRepository.save(lesson);

        return ResponseEntity.ok(Map.of("lessonId", lesson.getId(), "status", lesson.getStatus()));
    }

    private ResponseEntity<?> proposeBookingInternal(Long tutorId,
                                                     Long lessonId,
                                                     TutorLessonProposalRequest payload,
                                                     Locale locale) {
        Optional<Lesson> lessonOpt = lessonRepository.findById(lessonId);
        if (lessonOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Booking not found"));
        }

        Lesson lesson = lessonOpt.get();
        if (!Objects.equals(lesson.getTutor().getId(), tutorId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", tutorMessage(locale, "Rezerwacja nie należy do tego korepetytora.", "Booking does not belong to this tutor")));
        }

        if (lesson.getStatus() != LessonStatus.REQUESTED
                && lesson.getStatus() != LessonStatus.RESCHEDULED
                && lesson.getStatus() != LessonStatus.SCHEDULED) {
            return ResponseEntity.badRequest().body(Map.of("error", tutorMessage(locale, "Termin można zmienić tylko dla oczekujących lub zaplanowanych lekcji.", "Only pending or scheduled lessons can be rescheduled")));
        }

        if (payload == null || !StringUtils.hasText(payload.getStart())) {
            return ResponseEntity.badRequest().body(Map.of("error", tutorMessage(locale, "Podaj datę i godzinę.", "Start date is required")));
        }

        LocalDateTime newStart;
        try {
            newStart = LocalDateTime.parse(payload.getStart().trim());
        } catch (DateTimeParseException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", tutorMessage(locale, "Nieprawidłowy format daty.", "Invalid date format")));
        }

        int duration = resolveDurationMinutes(payload.getDurationMinutes(),
                lesson.getTutor() != null ? lesson.getTutor().getLessonDuration() : null);
        if (duration <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", tutorMessage(locale, "Nieprawidłowy czas trwania.", "Invalid duration")));
        }

        LocalDateTime newEnd = newStart.plusMinutes(duration);
        if (newStart.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", tutorMessage(locale, "Nowy termin musi być w przyszłości.", "Proposed time must be in the future")));
        }

        // Check tutor conflicts excluding this lesson
        List<Lesson> tutorLessons = lessonRepository.findByTutorId(tutorId).stream()
                .filter(other -> !Objects.equals(other.getId(), lesson.getId()))
                .collect(Collectors.toList());
        if (hasConflict(tutorLessons, newStart, newEnd)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", tutorMessage(locale, "Masz w tym czasie inne zajęcia.", "Tutor has another lesson at that time")));
        }

        // Check student conflicts excluding this lesson
        Student student = lesson.getStudent();
        if (student != null) {
            List<Lesson> studentLessons = lessonRepository.findByStudentId(student.getId()).stream()
                    .filter(other -> !Objects.equals(other.getId(), lesson.getId()))
                    .collect(Collectors.toList());
            if (hasConflict(studentLessons, newStart, newEnd)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", tutorMessage(locale, "Uczeń ma w tym czasie inne zajęcia.", "Student has another lesson at that time")));
            }
        }

        // Validate against tutor availability rules
        Tutor tutor = lesson.getTutor();
        if (tutor != null) {
            List<Lesson> forValidation = tutorLessons;
            Locale effectiveLocale = locale != null ? locale : Locale.getDefault();
            Optional<String> validation = tutorBookingValidator.validate(
                    tutor,
                    newStart,
                    newEnd,
                    forValidation,
                    effectiveLocale);
            if (validation.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", validation.get()));
            }
        }

        googleCalendarService.deleteLessonEvent(lesson);
        lesson.setGoogleEventId(null);
        lesson.setMeetingLink(null);
        lesson.setStartTime(newStart);
        lesson.setEndTime(newEnd);
        lesson.setStatus(LessonStatus.RESCHEDULED);
        if (payload != null && StringUtils.hasText(payload.getNote())) {
            appendNote(lesson, "[Tutor proposal]", payload.getNote());
        }
        Lesson saved = lessonRepository.save(lesson);
        ensureLessonStatus(saved);

        return ResponseEntity.ok(toTutorLessonDto(saved));
    }

    private ResponseEntity<?> buildTutorOverviewResponse(Long tutorId) {
        Optional<User> tutorOpt = userRepository.findById(tutorId);
        if (tutorOpt.isEmpty() || !(tutorOpt.get() instanceof Tutor)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Tutor not found"));
        }

        Tutor tutor = (Tutor) tutorOpt.get();

        List<Lesson> lessons = lessonRepository.findByTutorId(tutorId);
        lessons.forEach(this::ensureLessonStatus);
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
                    LocalDateTime bCreated = b.getCreatedAt();
                    LocalDateTime aCreated = a.getCreatedAt();
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
        String normalizedFilter = SubjectDictionary.normalize(subject);
        if (normalizedFilter != null) {
            Set<String> tutorSubjects = SubjectDictionary.extractSubjects(tutor.getSubjects());
            return tutorSubjects.contains(normalizedFilter);
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
        if (rate == null) {
            return true;
        }
        return rate <= maxPrice;
    }

    private boolean matchesDays(Tutor tutor, Set<String> days) {
        if (days.isEmpty()) {
            return true;
        }
        Set<String> tutorDays = AvailabilityUtils.parsePreferredDayCodes(tutor.getPreferredDays());
        if (tutorDays.isEmpty()) {
            return false;
        }
        for (String day : days) {
            if (tutorDays.contains(day)) {
                return true;
            }
        }
        return false;
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
        // If lesson has ended (now >= end), mark as COMPLETED
        if (!now.isBefore(end) && status != LessonStatus.COMPLETED) {
            return LessonStatus.COMPLETED;
        }
        // If lesson is currently happening (start <= now < end), mark as IN_PROGRESS
        if (!now.isBefore(start) && now.isBefore(end)
                && (status == LessonStatus.SCHEDULED || status == LessonStatus.RESCHEDULED || status == LessonStatus.IN_PROGRESS)) {
            return LessonStatus.IN_PROGRESS;
        }
        return status;
    }

    private boolean tutorSupportsMode(Tutor tutor, LessonDeliveryMode mode) {
        if (tutor == null || mode == null) {
            return false;
        }
        List<String> modes = parseValues(tutor.getLessonModes());
        if (modes.isEmpty()) {
            return false;
        }
        return modes.stream().anyMatch(raw -> {
            String lower = raw.toLowerCase(Locale.ROOT);
            if (mode == LessonDeliveryMode.ONLINE) {
                return lower.contains("online") || lower.contains("zdal") || lower.contains("remote")
                        || lower.contains("hybrid") || lower.contains("hybryd");
            }
            return lower.contains("onsite") || lower.contains("on-site") || lower.contains("stacjon")
                    || lower.contains("stationary") || lower.contains("hybrid") || lower.contains("hybryd");
        });
    }

    private OnsiteAddress extractOnsiteAddress(TutorBookingRequest payload) {
        if (payload == null) {
            return new OnsiteAddress(null, null, null, null, null);
        }
        return new OnsiteAddress(
                trimToNull(payload.getOnsiteCity()),
                trimToNull(payload.getOnsitePostalCode()),
                trimToNull(payload.getOnsiteStreet()),
                trimToNull(payload.getOnsiteBuilding()),
                trimToNull(payload.getOnsiteApartment())
        );
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

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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
        LessonStatus status = ensureLessonStatus(lesson);
        LocalDateTime start = lesson.getStartTime();
        LocalDateTime end = lesson.getEndTime();
        dto.put("id", lesson.getId());
        dto.put("status", status != null ? status.name() : null);
        dto.put("start", start != null ? start.toString() : null);
        dto.put("end", end != null ? end.toString() : null);
        dto.put("durationMinutes",
                (start != null && end != null) ? Duration.between(start, end).toMinutes() : null);
        dto.put("notes", lesson.getNotes());
        dto.put("meetingLink", lesson.getMeetingLink());
        dto.put("deliveryMode", lesson.getDeliveryMode() != null ? lesson.getDeliveryMode().name() : null);
        dto.put("onsiteCity", lesson.getOnsiteCity());
        dto.put("onsitePostalCode", lesson.getOnsitePostalCode());
        dto.put("onsiteStreet", lesson.getOnsiteStreet());
        dto.put("onsiteBuilding", lesson.getOnsiteBuilding());
        dto.put("onsiteApartment", lesson.getOnsiteApartment());

        Review review = reviewRepository.findByLessonId(lesson.getId()).orElse(null);
        dto.put("studentReviewSubmitted", review != null && review.getStudentReviewAt() != null);
        dto.put("tutorReviewSubmitted", review != null && review.getTutorReviewAt() != null);

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

    private void appendNote(Lesson lesson, String prefix, String message) {
        if (!StringUtils.hasText(message)) {
            return;
        }
        String entry = prefix + " " + message.trim();
        if (StringUtils.hasText(lesson.getNotes())) {
            lesson.setNotes(lesson.getNotes() + "\n" + entry);
        } else {
            lesson.setNotes(entry);
        }
    }

    private String tutorMessage(Locale locale, String pl, String en) {
        if (locale != null && locale.getLanguage() != null
                && locale.getLanguage().toLowerCase(Locale.ROOT).startsWith("pl")) {
            return pl;
        }
        return en;
    }

    private boolean hasCompleteProfile(Tutor tutor) {
        if (tutor == null) return false;
        if (!StringUtils.hasText(tutor.getEducation())) return false;
        if (tutor.getExperienceYears() == null) return false;
        if (!StringUtils.hasText(tutor.getSubjects())) return false;
        if (tutor.getHourlyRate() == null || tutor.getHourlyRate() <= 0) return false;
        if (tutor.getLessonDuration() == null || tutor.getLessonDuration() <= 0) return false;
        if (!StringUtils.hasText(tutor.getTeachingLanguages())) return false;
        if (!StringUtils.hasText(tutor.getLessonModes())) return false;
        if (!StringUtils.hasText(tutor.getTeachingMethods())) return false;
        if (!StringUtils.hasText(tutor.getBio())) return false;

        List<String> modes = parseValues(tutor.getLessonModes());
        boolean requiresOnsiteDetails = modes.stream().anyMatch(mode -> {
            String lower = mode.toLowerCase(Locale.ROOT);
            return lower.contains("onsite")
                    || lower.contains("on-site")
                    || lower.contains("stacjon")
                    || lower.contains("hybrid")
                    || lower.contains("hybryd");
        });

        if (requiresOnsiteDetails) {
            if (!StringUtils.hasText(tutor.getCity())) return false;
            if (tutor.getTravelRadius() == null || tutor.getTravelRadius() < 0) return false;
        }

        return true;
    }

    private List<String> parseValues(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String trimmed = raw.trim();
        try {
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                Map<String, Object> map = OBJECT_MAPPER.readValue(trimmed, MAP_TYPE);
                return map.entrySet().stream()
                        .filter(entry -> isTruthy(entry.getValue()))
                        .map(Map.Entry::getKey)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .collect(Collectors.toList());
            }
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                List<String> list = OBJECT_MAPPER.readValue(trimmed, LIST_TYPE);
                return list.stream()
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
        }
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0d;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private record OnsiteAddress(String city, String postalCode, String street, String building, String apartment) {
        boolean isComplete() {
            return StringUtils.hasText(city)
                    && StringUtils.hasText(postalCode)
                    && StringUtils.hasText(street)
                    && StringUtils.hasText(building);
        }
    }
}



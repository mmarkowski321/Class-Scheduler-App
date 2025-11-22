package pl.projekt.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.projekt.backend.dto.TutorBookingRequest;
import pl.projekt.backend.dto.TutorLessonDeclineRequest;
import pl.projekt.backend.dto.TutorLessonProposalRequest;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Review;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.EmailService;
import pl.projekt.backend.service.GoogleCalendarService;
import pl.projekt.backend.service.TutorBookingValidator;
import pl.projekt.backend.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import pl.projekt.backend.service.GoogleCalendarService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TutorControllerIntegrationTest {

    private static final String STUDENT_TOKEN = "student-token";
    private static final String TUTOR_TOKEN = "tutor-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private GoogleCalendarService googleCalendarService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private TutorBookingValidator tutorBookingValidator;

    private Student student;
    private Tutor tutor;
    private Tutor tutor2;
    private Tutor bannedTutor;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        lessonRepository.deleteAll();
        userRepository.deleteAll();

        student = new Student();
        student.setEmail("student@example.com");
        student.setPassword("password");
        student.setFirstName("Student");
        student.setLastName("Test");
        student.setBirthDate(java.time.LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        student = (Student) userRepository.save(student);

        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword("password");
        tutor.setFirstName("John");
        tutor.setLastName("Doe");
        tutor.setBirthDate(java.time.LocalDate.of(1990, 1, 1));
        tutor.setEmailVerified(true);
        tutor.setSubjects("math,physics");
        tutor.setCity("Warsaw");
        tutor.setHourlyRate(100.0);
        tutor.setLessonDuration(60);
        tutor.setTeachingLanguages("pl,en");
        tutor.setLessonModes("{\"online\":true,\"onsite\":true}");
        tutor.setEducation("University");
        tutor.setExperienceYears(5);
        tutor.setTeachingMethods("Interactive");
        tutor.setBio("Experienced tutor");
        tutor.setTravelRadius(10); // Required when onsite mode is enabled
        tutor = (Tutor) userRepository.save(tutor);

        tutor2 = new Tutor();
        tutor2.setEmail("tutor2@example.com");
        tutor2.setPassword("password");
        tutor2.setFirstName("Jane");
        tutor2.setLastName("Smith");
        tutor2.setBirthDate(java.time.LocalDate.of(1990, 1, 1));
        tutor2.setEmailVerified(true);
        tutor2.setSubjects("angielski");
        tutor2.setCity("Krakow");
        tutor2.setHourlyRate(80.0);
        tutor2.setLessonDuration(90);
        tutor2.setEducation("University");
        tutor2.setExperienceYears(3);
        tutor2.setTeachingLanguages("en");
        tutor2.setLessonModes("{\"online\":true}");
        tutor2.setTeachingMethods("Interactive");
        tutor2.setBio("Experienced English tutor");
        tutor2 = (Tutor) userRepository.save(tutor2);

        bannedTutor = new Tutor();
        bannedTutor.setEmail("banned@example.com");
        bannedTutor.setPassword("password");
        bannedTutor.setFirstName("Banned");
        bannedTutor.setLastName("Tutor");
        bannedTutor.setBirthDate(java.time.LocalDate.of(1990, 1, 1));
        bannedTutor.setEmailVerified(true);
        bannedTutor.setBanned(true);
        bannedTutor = (Tutor) userRepository.save(bannedTutor);

        // Mock JWT for student
        when(jwtUtil.validateToken(STUDENT_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(STUDENT_TOKEN)).thenReturn(student.getId());
        when(jwtUtil.getRoleFromToken(STUDENT_TOKEN)).thenReturn("STUDENT");

        // Mock JWT for tutor
        when(jwtUtil.validateToken(TUTOR_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(TUTOR_TOKEN)).thenReturn(tutor.getId());
        when(jwtUtil.getRoleFromToken(TUTOR_TOKEN)).thenReturn("TUTOR");

        // Mock tutor booking validator
        when(tutorBookingValidator.validate(any(Tutor.class), any(LocalDateTime.class), any(LocalDateTime.class), any(), any()))
                .thenReturn(Optional.empty());

        // Mock Google Calendar service
        GoogleCalendarService.CalendarEvent calendarEvent = new GoogleCalendarService.CalendarEvent("event-id-123", null);
        when(googleCalendarService.createLessonEvent(any(Lesson.class))).thenReturn(Optional.of(calendarEvent));
        doNothing().when(googleCalendarService).deleteLessonEvent(any(Lesson.class));
    }

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
        lessonRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void shouldListTutors() throws Exception {
        // Refresh tutors from database to ensure they're loaded
        tutor = (Tutor) userRepository.findById(tutor.getId()).orElse(tutor);
        tutor2 = (Tutor) userRepository.findById(tutor2.getId()).orElse(tutor2);
        
        mockMvc.perform(get("/api/tutors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").exists())
                .andExpect(jsonPath("$.tutors").isArray())
                .andExpect(jsonPath("$.tutors.length()").value(2)) // Should exclude banned tutor
                .andExpect(jsonPath("$.tutors[0].firstName").exists())
                .andExpect(jsonPath("$.tutors[0].lastName").exists());
    }

    @Test
    void shouldExcludeBannedTutorsFromList() throws Exception {
        mockMvc.perform(get("/api/tutors")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutors").isArray())
                .andExpect(jsonPath("$.tutors.length()").value(2)); // Only non-banned tutors
    }

    @Test
    void shouldFilterTutorsBySubject() throws Exception {
        mockMvc.perform(get("/api/tutors")
                        .param("subject", "math")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutors").isArray())
                .andExpect(jsonPath("$.tutors.length()").value(1))
                .andExpect(jsonPath("$.tutors[0].firstName").value("John"));
    }

    @Test
    void shouldFilterTutorsByEnglishSubject() throws Exception {
        mockMvc.perform(get("/api/tutors")
                        .param("subject", "english")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutors").isArray())
                .andExpect(jsonPath("$.tutors.length()").value(1))
                .andExpect(jsonPath("$.tutors[0].firstName").value("Jane"));
    }

    @Test
    void shouldFilterTutorsByCity() throws Exception {
        mockMvc.perform(get("/api/tutors")
                        .param("city", "Warsaw")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutors").isArray())
                .andExpect(jsonPath("$.tutors.length()").value(1))
                .andExpect(jsonPath("$.tutors[0].city").value("Warsaw"));
    }

    @Test
    void shouldFilterTutorsByMaxPrice() throws Exception {
        mockMvc.perform(get("/api/tutors")
                        .param("maxPrice", "90.0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutors").isArray())
                .andExpect(jsonPath("$.tutors.length()").value(1))
                .andExpect(jsonPath("$.tutors[0].hourlyRate").value(80.0));
    }

    @Test
    void shouldGetTutorById() throws Exception {
        mockMvc.perform(get("/api/tutors/{id}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tutor.getId()))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.hourlyRate").value(100.0));
    }

    @Test
    void shouldReturnNotFoundForNonExistentTutor() throws Exception {
        mockMvc.perform(get("/api/tutors/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tutor not found"));
    }

    @Test
    void shouldReturnNotFoundForBannedTutor() throws Exception {
        mockMvc.perform(get("/api/tutors/{id}", bannedTutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Tutor not available"));
    }

    @Test
    void shouldBookTutorAsStudent() throws Exception {
        TutorBookingRequest request = new TutorBookingRequest();
        request.setStart(LocalDateTime.now().plusDays(1).toString());
        request.setDurationMinutes(60);
        request.setDeliveryMode("ONLINE");
        request.setNotes("Test booking");

        mockMvc.perform(post("/api/tutors/{id}/bookings", tutor.getId())
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lessonId").exists())
                .andExpect(jsonPath("$.status").value(LessonStatus.REQUESTED.name()));

        // Verify lesson was created
        java.util.List<Lesson> lessons = lessonRepository.findByTutorId(tutor.getId());
        assertThat(lessons).hasSize(1);
        assertThat(lessons.get(0).getStatus()).isEqualTo(LessonStatus.REQUESTED);
        assertThat(lessons.get(0).getStudent().getId()).isEqualTo(student.getId());
        assertThat(lessons.get(0).getDeliveryMode()).isEqualTo(LessonDeliveryMode.ONLINE);

        // Verify email was sent
        verify(emailService).sendTutorBookingRequestEmail(eq(tutor), eq(student), any(Lesson.class));
    }

    @Test
    void shouldRejectBookingWithoutAuth() throws Exception {
        TutorBookingRequest request = new TutorBookingRequest();
        request.setStart(LocalDateTime.now().plusDays(1).toString());
        request.setDeliveryMode("ONLINE");

        mockMvc.perform(post("/api/tutors/{id}/bookings", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Authorization token required"));
    }

    @Test
    void shouldRejectBookingWithInvalidToken() throws Exception {
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        TutorBookingRequest request = new TutorBookingRequest();
        request.setStart(LocalDateTime.now().plusDays(1).toString());
        request.setDeliveryMode("ONLINE");

        mockMvc.perform(post("/api/tutors/{id}/bookings", tutor.getId())
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid or expired token"));
    }

    @Test
    void shouldRejectBookingForBannedTutor() throws Exception {
        TutorBookingRequest request = new TutorBookingRequest();
        request.setStart(LocalDateTime.now().plusDays(1).toString());
        request.setDeliveryMode("ONLINE");

        mockMvc.perform(post("/api/tutors/{id}/bookings", bannedTutor.getId())
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tutor is not accepting bookings"));
    }

    @Test
    void shouldRejectBookingWithoutStartTime() throws Exception {
        TutorBookingRequest request = new TutorBookingRequest();
        request.setDeliveryMode("ONLINE");

        mockMvc.perform(post("/api/tutors/{id}/bookings", tutor.getId())
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Start date is required"));
    }

    @Test
    void shouldRejectBookingInThePast() throws Exception {
        TutorBookingRequest request = new TutorBookingRequest();
        request.setStart(LocalDateTime.now().minusDays(1).toString());
        request.setDeliveryMode("ONLINE");

        mockMvc.perform(post("/api/tutors/{id}/bookings", tutor.getId())
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot book past time slots"));
    }

    @Test
    void shouldRejectBookingWithInvalidDeliveryMode() throws Exception {
        TutorBookingRequest request = new TutorBookingRequest();
        request.setStart(LocalDateTime.now().plusDays(1).toString());
        request.setDeliveryMode("INVALID_MODE");

        mockMvc.perform(post("/api/tutors/{id}/bookings", tutor.getId())
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldBookTutorWithOnsiteAddress() throws Exception {
        TutorBookingRequest request = new TutorBookingRequest();
        request.setStart(LocalDateTime.now().plusDays(1).toString());
        request.setDurationMinutes(60);
        request.setDeliveryMode("ONSITE");
        request.setOnsiteCity("Warsaw");
        request.setOnsitePostalCode("00-001");
        request.setOnsiteStreet("Main Street");
        request.setOnsiteBuilding("1");

        mockMvc.perform(post("/api/tutors/{id}/bookings", tutor.getId())
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(LessonStatus.REQUESTED.name()));

        // Verify lesson was created with onsite address
        java.util.List<Lesson> lessons = lessonRepository.findByTutorId(tutor.getId());
        assertThat(lessons).hasSize(1);
        Lesson lesson = lessons.get(0);
        assertThat(lesson.getDeliveryMode()).isEqualTo(LessonDeliveryMode.ONSITE);
        assertThat(lesson.getOnsiteCity()).isEqualTo("Warsaw");
        assertThat(lesson.getOnsitePostalCode()).isEqualTo("00-001");
        assertThat(lesson.getOnsiteStreet()).isEqualTo("Main Street");
        assertThat(lesson.getOnsiteBuilding()).isEqualTo("1");
    }

    @Test
    void shouldRejectBookingWhenValidatorFails() throws Exception {
        // Mock validator to return error
        when(tutorBookingValidator.validate(any(Tutor.class), any(LocalDateTime.class), any(LocalDateTime.class), any(), any()))
                .thenReturn(Optional.of("Tutor is busy at this time"));

        TutorBookingRequest request = new TutorBookingRequest();
        request.setStart(LocalDateTime.now().plusDays(1).toString());
        request.setDurationMinutes(60);
        request.setDeliveryMode("ONLINE");

        mockMvc.perform(post("/api/tutors/{id}/bookings", tutor.getId())
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tutor is busy at this time"));
    }

    @Test
    void shouldGetTutorBookings() throws Exception {
        // Create a lesson for tutor
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusDays(1));
        lesson.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.REQUESTED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson);

        mockMvc.perform(get("/api/tutors/{id}/bookings", tutor.getId())
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.confirmed").isArray())
                .andExpect(jsonPath("$.requests.length()").value(1))
                .andExpect(jsonPath("$.requests[0].id").value(lesson.getId()));
    }

    @Test
    void shouldGetTutorBookingsMe() throws Exception {
        // Create a lesson for tutor
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusDays(1));
        lesson.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.REQUESTED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson);

        mockMvc.perform(get("/api/tutors/me/bookings")
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.confirmed").isArray())
                .andExpect(jsonPath("$.requests.length()").value(1));
    }

    @Test
    void shouldGetTutorOverview() throws Exception {
        // Create lessons for tutor
        Lesson lesson1 = new Lesson();
        lesson1.setTutor(tutor);
        lesson1.setStudent(student);
        lesson1.setStartTime(LocalDateTime.now().plusDays(1));
        lesson1.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson1.setStatus(LessonStatus.SCHEDULED);
        lesson1.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson1);

        Lesson lesson2 = new Lesson();
        lesson2.setTutor(tutor);
        lesson2.setStudent(student);
        lesson2.setStartTime(LocalDateTime.now().plusDays(2));
        lesson2.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        lesson2.setStatus(LessonStatus.REQUESTED);
        lesson2.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson2);

        // Create review
        Review review = new Review();
        review.setLesson(lesson1);
        review.setStudent(student);
        review.setTutor(tutor);
        review.setTutorRating(5);
        review.setComment("Great tutor!");
        review.setCreatedAt(LocalDateTime.now());
        reviewRepository.save(review);

        mockMvc.perform(get("/api/tutors/{id}/overview", tutor.getId())
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming").isArray())
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.newTutors").isArray())
                .andExpect(jsonPath("$.upcoming.length()").value(1))
                .andExpect(jsonPath("$.requests.length()").value(1))
                .andExpect(jsonPath("$.reviews.length()").value(1));
    }

    @Test
    void shouldGetTutorOverviewMe() throws Exception {
        // Create lessons for tutor
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusDays(1));
        lesson.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson);

        mockMvc.perform(get("/api/tutors/me/overview")
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcoming").isArray())
                .andExpect(jsonPath("$.requests").isArray())
                .andExpect(jsonPath("$.reviews").isArray())
                .andExpect(jsonPath("$.newTutors").isArray());
    }

    @Test
    void shouldConfirmBookingMe() throws Exception {
        // Create a requested lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusDays(1));
        lesson.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.REQUESTED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        mockMvc.perform(post("/api/tutors/me/bookings/{lessonId}/confirm", lesson.getId())
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lesson.getId()))
                .andExpect(jsonPath("$.status").value(LessonStatus.SCHEDULED.name()));

        Lesson updated = lessonRepository.findById(lesson.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LessonStatus.SCHEDULED);
    }

    @Test
    void shouldDeclineBooking() throws Exception {
        // Create a requested lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusDays(1));
        lesson.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.REQUESTED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        TutorLessonDeclineRequest request = new TutorLessonDeclineRequest();
        request.setReason("Not available at that time");

        mockMvc.perform(post("/api/tutors/{id}/bookings/{lessonId}/decline", tutor.getId(), lesson.getId())
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(lesson.getId()))
                .andExpect(jsonPath("$.status").value(LessonStatus.CANCELLED.name()));

        Lesson updated = lessonRepository.findById(lesson.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LessonStatus.CANCELLED);
        assertThat(updated.getNotes()).contains("Not available at that time");

        verify(googleCalendarService).deleteLessonEvent(any(Lesson.class));
    }

    @Test
    void shouldProposeBooking() throws Exception {
        // Create a requested lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusDays(1));
        lesson.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.REQUESTED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        TutorLessonProposalRequest request = new TutorLessonProposalRequest();
        LocalDateTime newStart = LocalDateTime.now().plusDays(2);
        request.setStart(newStart.toString());
        request.setDurationMinutes(90);
        request.setNote("Better time for me");

        mockMvc.perform(post("/api/tutors/{id}/bookings/{lessonId}/propose", tutor.getId(), lesson.getId())
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lesson.getId()))
                .andExpect(jsonPath("$.status").value(LessonStatus.RESCHEDULED.name()));

        Lesson updated = lessonRepository.findById(lesson.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(LessonStatus.RESCHEDULED);
        // Compare with tolerance for nanoseconds precision differences (database may truncate nanoseconds)
        // Compare only up to seconds precision to avoid nanosecond differences
        assertThat(updated.getStartTime().truncatedTo(java.time.temporal.ChronoUnit.SECONDS))
                .isEqualTo(newStart.truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        assertThat(updated.getNotes()).contains("Better time for me");
    }

    @Test
    void shouldRejectConfirmBookingMeForNonExistentLesson() throws Exception {
        mockMvc.perform(post("/api/tutors/me/bookings/99999/confirm")
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDeclineBookingForNonExistentLesson() throws Exception {
        TutorLessonDeclineRequest request = new TutorLessonDeclineRequest();
        request.setReason("Not available");

        mockMvc.perform(post("/api/tutors/{id}/bookings/99999/decline", tutor.getId())
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectProposeBookingForNonExistentLesson() throws Exception {
        TutorLessonProposalRequest request = new TutorLessonProposalRequest();
        request.setStart(LocalDateTime.now().plusDays(2).toString());
        request.setDurationMinutes(60);

        mockMvc.perform(post("/api/tutors/{id}/bookings/99999/propose", tutor.getId())
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectConfirmBookingMeForWrongTutor() throws Exception {
        // Create lesson for another tutor
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor2);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusDays(1));
        lesson.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.REQUESTED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        mockMvc.perform(post("/api/tutors/me/bookings/{lessonId}/confirm", lesson.getId())
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectConfirmBookingMeForAlreadyScheduledLesson() throws Exception {
        // Create an already scheduled lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusDays(1));
        lesson.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        mockMvc.perform(post("/api/tutors/me/bookings/{lessonId}/confirm", lesson.getId())
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDeclineBookingForAlreadyCancelledLesson() throws Exception {
        // Create a cancelled lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusDays(1));
        lesson.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.CANCELLED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        TutorLessonDeclineRequest request = new TutorLessonDeclineRequest();
        request.setReason("Not available");

        mockMvc.perform(post("/api/tutors/{id}/bookings/{lessonId}/decline", tutor.getId(), lesson.getId())
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectProposeBookingForInvalidStartTime() throws Exception {
        // Create a requested lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusDays(1));
        lesson.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.REQUESTED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        TutorLessonProposalRequest request = new TutorLessonProposalRequest();
        request.setStart(LocalDateTime.now().minusDays(1).toString()); // Past time
        request.setDurationMinutes(60);

        mockMvc.perform(post("/api/tutors/{id}/bookings/{lessonId}/propose", tutor.getId(), lesson.getId())
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}


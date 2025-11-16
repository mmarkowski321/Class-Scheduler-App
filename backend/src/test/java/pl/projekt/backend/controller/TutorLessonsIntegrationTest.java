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
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.EmailService;
import pl.projekt.backend.service.GoogleCalendarService;
import pl.projekt.backend.service.MeetingLinkService;
import pl.projekt.backend.util.JwtUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class TutorLessonsIntegrationTest {

    private static final String TUTOR_TOKEN = "tutor-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private GoogleCalendarService googleCalendarService;

    @MockBean
    private MeetingLinkService meetingLinkService;

    @MockBean
    private EmailService emailService;

    private Tutor tutor;
    private Student student;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        userRepository.deleteAll();

        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword("secret");
        tutor.setFirstName("Maria");
        tutor.setLastName("Lewandowska");
        tutor.setBirthDate(LocalDate.of(1991, 3, 3));
        tutor.setLessonDuration(60);
        tutor.setLessonModes("{\"online\":true}");
        tutor = (Tutor) userRepository.save(tutor);

        student = new Student();
        student.setEmail("student@example.com");
        student.setPassword("pass");
        student.setFirstName("Piotr");
        student.setLastName("Zielinski");
        student.setBirthDate(LocalDate.of(2011, 4, 15));
        student = (Student) userRepository.save(student);

        when(jwtUtil.validateToken(TUTOR_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(TUTOR_TOKEN)).thenReturn(tutor.getId());
        when(jwtUtil.getRoleFromToken(TUTOR_TOKEN)).thenReturn("TUTOR");
        when(googleCalendarService.isEnabled()).thenReturn(false);
    }

    @AfterEach
    void clean() {
        lessonRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void tutorShouldProposeNewTimeForScheduledLesson() throws Exception {
        Lesson lesson = lessonRepository.save(buildLesson(
                LocalDateTime.now().plusDays(2).withHour(15).withMinute(0),
                LessonStatus.SCHEDULED,
                "https://meet.test/abc",
                "event-123"));

        LocalDateTime newStart = lesson.getStartTime().plusDays(1).withHour(17);
        String payload = objectMapper.createObjectNode()
                .put("start", newStart.toString())
                .put("durationMinutes", 90)
                .put("note", "Need later time")
                .toString();

        mockMvc.perform(post("/api/tutors/me/bookings/" + lesson.getId() + "/propose")
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(LessonStatus.RESCHEDULED.name()))
                .andExpect(jsonPath("$.start").value(newStart.toString()))
                .andExpect(jsonPath("$.meetingLink").isEmpty());

        Lesson stored = lessonRepository.findById(lesson.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(LessonStatus.RESCHEDULED);
        assertThat(stored.getStartTime()).isCloseTo(newStart, within(1, ChronoUnit.MICROS));
        assertThat(stored.getEndTime()).isCloseTo(newStart.plusMinutes(90), within(1, ChronoUnit.MICROS));
        assertThat(stored.getMeetingLink()).isNull();
        assertThat(stored.getGoogleEventId()).isNull();
        assertThat(stored.getNotes()).contains("[Tutor proposal] Need later time");

        verify(googleCalendarService).deleteLessonEvent(argThat(it -> it.getId().equals(lesson.getId())));
    }

    @Test
    void tutorShouldCancelScheduledLesson() throws Exception {
        Lesson lesson = lessonRepository.save(buildLesson(
                LocalDateTime.now().plusDays(1).withHour(11),
                LessonStatus.SCHEDULED,
                "https://meet.test/link",
                "gcal-1"));

        String payload = objectMapper.createObjectNode()
                .put("reason", "Unexpected trip")
                .toString();

        mockMvc.perform(post("/api/tutors/me/bookings/" + lesson.getId() + "/decline")
                        .header("Authorization", bearer(TUTOR_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(LessonStatus.CANCELLED.name()));

        Lesson stored = lessonRepository.findById(lesson.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(LessonStatus.CANCELLED);
        assertThat(stored.getMeetingLink()).isNull();
        assertThat(stored.getGoogleEventId()).isNull();
        assertThat(stored.getNotes()).contains("[Tutor decline] Unexpected trip");

        verify(googleCalendarService).deleteLessonEvent(argThat(it -> it.getId().equals(lesson.getId())));
    }

    private Lesson buildLesson(LocalDateTime start, LessonStatus status, String meetingLink, String eventId) {
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(start);
        lesson.setEndTime(start.plusHours(1));
        lesson.setStatus(status);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson.setMeetingLink(meetingLink);
        lesson.setGoogleEventId(eventId);
        lesson.setNotes("Initial note");
        return lesson;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}


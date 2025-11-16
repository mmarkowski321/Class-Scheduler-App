package pl.projekt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.MvcResult;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.EmailService;
import pl.projekt.backend.service.GoogleCalendarService;
import pl.projekt.backend.util.JwtUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class StudentLessonsIntegrationTest {

    private static final String STUDENT_TOKEN = "student-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

@Autowired
private LessonRepository lessonRepository;

@Autowired
private pl.projekt.backend.repository.ReviewRepository reviewRepository;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private GoogleCalendarService googleCalendarService;

    @MockBean
    private EmailService emailService;

    private Student student;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        lessonRepository.deleteAll();
        userRepository.deleteAll();

        student = new Student();
        student.setEmail("student@example.com");
        student.setPassword("secret");
        student.setFirstName("Adam");
        student.setLastName("Nowak");
        student.setBirthDate(LocalDate.of(2010, 2, 15));
        student.setLanguages("pl");
        student.setTimezone("Europe/Warsaw");
        student = userRepository.save(student);

        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword("pass");
        tutor.setFirstName("Ewa");
        tutor.setLastName("Kowalska");
        tutor.setBirthDate(LocalDate.of(1990, 7, 5));
        tutor.setEducation("Uni");
        tutor.setExperienceYears(4);
        tutor.setSubjects("math");
        tutor.setHourlyRate(120.0);
        tutor.setLessonDuration(60);
        tutor.setTeachingLanguages("pl");
        tutor.setLessonModes("{\"online\":true}");
        tutor.setTeachingMethods("fun");
        tutor.setBio("Bio");
        tutor = (Tutor) userRepository.save(tutor);

        when(jwtUtil.validateToken(STUDENT_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(STUDENT_TOKEN)).thenReturn(student.getId());
        when(jwtUtil.getRoleFromToken(STUDENT_TOKEN)).thenReturn("STUDENT");
    }

    @AfterEach
    void clean() {
        reviewRepository.deleteAll();
        lessonRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void cancelLessonShouldClearMeetingLinkAndAddNote() throws Exception {
        Lesson lesson = lessonRepository.save(buildLesson(
                LocalDateTime.now().plusDays(2).withHour(12).withMinute(0),
                LessonStatus.SCHEDULED,
                "https://meet.test/link",
                "event-1"));

        String payload = objectMapper.writeValueAsString(
                objectMapper.createObjectNode().put("reason", "Student sick"));

        mockMvc.perform(post("/api/students/me/lessons/" + lesson.getId() + "/cancel")
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(LessonStatus.CANCELLED.name()))
                .andExpect(jsonPath("$.meetingLink").isEmpty());

        Lesson stored = lessonRepository.findById(lesson.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(LessonStatus.CANCELLED);
        assertThat(stored.getMeetingLink()).isNull();
        assertThat(stored.getGoogleEventId()).isNull();
        assertThat(stored.getNotes()).contains("[Student cancel] Student sick");

        verify(googleCalendarService).deleteLessonEvent(any(Lesson.class));
    }

    @Test
    void rescheduleShouldReturnConflictWhenStudentBusy() throws Exception {
        Lesson target = lessonRepository.save(buildLesson(
                LocalDateTime.now().plusDays(3).withHour(10),
                LessonStatus.SCHEDULED,
                null,
                null));

        lessonRepository.save(buildLesson(
                target.getStartTime().plusHours(1),
                LessonStatus.SCHEDULED,
                null,
                null));

        String payload = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("start", target.getStartTime().plusMinutes(30).toString())
                        .put("durationMinutes", 60));

        mockMvc.perform(post("/api/students/me/lessons/" + target.getId() + "/reschedule")
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        Lesson stored = lessonRepository.findById(target.getId()).orElseThrow();
        assertThat(stored.getStartTime()).isCloseTo(target.getStartTime(), within(1, ChronoUnit.MICROS));
    }

    @Test
    void rescheduleShouldUpdateTimesAndResetMeetingLink() throws Exception {
        Lesson lesson = lessonRepository.save(buildLesson(
                LocalDateTime.now().plusDays(4).withHour(9),
                LessonStatus.SCHEDULED,
                "https://meet.test/old",
                "event-old"));

        doNothing().when(googleCalendarService).deleteLessonEvent(any(Lesson.class));

        LocalDateTime newStart = lesson.getStartTime().plusDays(1);
        String payload = objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("start", newStart.toString())
                        .put("durationMinutes", 90)
                        .put("note", "need later time"));

        MvcResult result = mockMvc.perform(post("/api/students/me/lessons/" + lesson.getId() + "/reschedule")
                        .header("Authorization", bearer(STUDENT_TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(LessonStatus.RESCHEDULED.name()))
                .andExpect(jsonPath("$.meetingLink").isEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(json.get("start").asText()).isEqualTo(newStart.toString());
        assertThat(json.get("notes").asText()).contains("Student reschedule");

        Lesson stored = lessonRepository.findById(lesson.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(LessonStatus.RESCHEDULED);
        assertThat(stored.getStartTime()).isCloseTo(newStart, within(1, ChronoUnit.MICROS));
        assertThat(stored.getEndTime()).isCloseTo(newStart.plusMinutes(90), within(1, ChronoUnit.MICROS));
        assertThat(stored.getMeetingLink()).isNull();
        assertThat(stored.getNotes()).contains("[Student reschedule] need later time");

        verify(googleCalendarService, atLeastOnce()).deleteLessonEvent(any(Lesson.class));
    }

    private Lesson buildLesson(LocalDateTime start, LessonStatus status, String meetingLink, String eventId) {
        Lesson lesson = new Lesson();
        lesson.setStudent(student);
        lesson.setTutor(tutor);
        lesson.setStartTime(start);
        lesson.setEndTime(start.plusHours(1));
        lesson.setStatus(status);
        lesson.setDeliveryMode(pl.projekt.backend.model.LessonDeliveryMode.ONLINE);
        lesson.setMeetingLink(meetingLink);
        lesson.setGoogleEventId(eventId);
        return lesson;
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}


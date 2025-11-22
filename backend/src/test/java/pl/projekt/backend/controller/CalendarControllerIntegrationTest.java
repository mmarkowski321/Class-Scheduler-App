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
import pl.projekt.backend.model.Calendar;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.model.User;
import pl.projekt.backend.repository.CalendarRepository;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.GoogleCalendarService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CalendarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private GoogleCalendarService googleCalendarService;

    private Tutor tutor;
    private Student student;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        calendarRepository.deleteAll();
        lessonRepository.deleteAll();

        // Create tutor
        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword(passwordEncoder.encode("password123"));
        tutor.setFirstName("Anna");
        tutor.setLastName("Nowak");
        tutor.setBirthDate(LocalDate.of(1990, 1, 1));
        tutor.setEmailVerified(true);
        tutor = (Tutor) userRepository.save(tutor);

        // Create student
        student = new Student();
        student.setEmail("student@example.com");
        student.setPassword(passwordEncoder.encode("password123"));
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        student = (Student) userRepository.save(student);
    }

    @AfterEach
    void clean() {
        lessonRepository.deleteAll();
        calendarRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldSyncCalendarWithUrlParameter() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        List<GoogleCalendarService.BusyTime> mockBusyTimes = List.of(
            new GoogleCalendarService.BusyTime(
                now.plusHours(1),
                now.plusHours(2),
                "Test Event",
                "Test Description"
            )
        );

        when(googleCalendarService.fetchBusyTimes(anyString()))
            .thenReturn(mockBusyTimes);

        mockMvc.perform(get("/api/calendar/sync/{userId}?calendarUrl=https://calendar.example.com/feed.ics", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busyTimes").isArray())
                .andExpect(jsonPath("$.busyTimes[0].title").value("Test Event"))
                .andExpect(jsonPath("$.busyTimes[0].description").value("Test Description"))
                .andExpect(jsonPath("$.count").value(1));

        verify(googleCalendarService).fetchBusyTimes(eq("https://calendar.example.com/feed.ics"));
    }

    @Test
    void shouldSyncCalendarFromDatabase() throws Exception {
        // Create calendar in database
        Calendar calendar = new Calendar();
        calendar.setUser(tutor);
        calendar.setCalendarUrl("https://calendar.example.com/feed.ics");
        calendar.setActive(true);
        calendarRepository.save(calendar);

        LocalDateTime now = LocalDateTime.now();
        List<GoogleCalendarService.BusyTime> mockBusyTimes = List.of(
            new GoogleCalendarService.BusyTime(
                now.plusHours(1),
                now.plusHours(2),
                "Database Event",
                null
            )
        );

        when(googleCalendarService.fetchBusyTimes(anyString()))
            .thenReturn(mockBusyTimes);

        mockMvc.perform(get("/api/calendar/sync/{userId}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busyTimes").isArray())
                .andExpect(jsonPath("$.busyTimes[0].title").value("Database Event"))
                .andExpect(jsonPath("$.count").value(1));

        verify(googleCalendarService).fetchBusyTimes(eq("https://calendar.example.com/feed.ics"));
    }

    @Test
    void shouldReturnErrorWhenUserNotFound() throws Exception {
        mockMvc.perform(get("/api/calendar/sync/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid user ID"));

        verify(googleCalendarService, never()).fetchBusyTimes(anyString());
    }

    @Test
    void shouldReturnErrorWhenNoCalendarConfigured() throws Exception {
        mockMvc.perform(get("/api/calendar/sync/{userId}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No calendar URLs configured"));

        verify(googleCalendarService, never()).fetchBusyTimes(anyString());
    }

    @Test
    void shouldReturnEmptyListWhenNoEventsFound() throws Exception {
        Calendar calendar = new Calendar();
        calendar.setUser(tutor);
        calendar.setCalendarUrl("https://calendar.example.com/feed.ics");
        calendar.setActive(true);
        calendarRepository.save(calendar);

        when(googleCalendarService.fetchBusyTimes(anyString()))
            .thenReturn(List.of());

        mockMvc.perform(get("/api/calendar/sync/{userId}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busyTimes").isArray())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.warning").exists());
    }

    @Test
    void shouldCheckBusyTimeSlot() throws Exception {
        Calendar calendar = new Calendar();
        calendar.setUser(tutor);
        calendar.setCalendarUrl("https://calendar.example.com/feed.ics");
        calendar.setActive(true);
        calendarRepository.save(calendar);

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        when(googleCalendarService.isTimeSlotBusy(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(true);

        Map<String, String> request = new HashMap<>();
        request.put("start", start.toString());
        request.put("end", end.toString());

        mockMvc.perform(post("/api/calendar/check-busy/{userId}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busy").value(true));

        verify(googleCalendarService).isTimeSlotBusy(eq("https://calendar.example.com/feed.ics"), eq(start), eq(end));
    }

    @Test
    void shouldCheckFreeTimeSlot() throws Exception {
        Calendar calendar = new Calendar();
        calendar.setUser(tutor);
        calendar.setCalendarUrl("https://calendar.example.com/feed.ics");
        calendar.setActive(true);
        calendarRepository.save(calendar);

        LocalDateTime start = LocalDateTime.now().plusHours(3);
        LocalDateTime end = LocalDateTime.now().plusHours(4);

        when(googleCalendarService.isTimeSlotBusy(anyString(), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(false);

        Map<String, String> request = new HashMap<>();
        request.put("start", start.toString());
        request.put("end", end.toString());

        mockMvc.perform(post("/api/calendar/check-busy/{userId}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busy").value(false));
    }

    @Test
    void shouldReturnNotBusyWhenNoCalendarConfigured() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = LocalDateTime.now().plusHours(2);

        Map<String, String> request = new HashMap<>();
        request.put("start", start.toString());
        request.put("end", end.toString());

        mockMvc.perform(post("/api/calendar/check-busy/{userId}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busy").value(false))
                .andExpect(jsonPath("$.message").value("No calendar configured"));

        verify(googleCalendarService, never()).isTimeSlotBusy(anyString(), any(), any());
    }

    @Test
    void shouldGetLessonsForTutor() throws Exception {
        // Create lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusHours(1));
        lesson.setEndTime(LocalDateTime.now().plusHours(2));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson);

        mockMvc.perform(get("/api/calendar/lessons/{userId}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons").isArray())
                .andExpect(jsonPath("$.lessons[0].studentId").value(student.getId()))
                .andExpect(jsonPath("$.lessons[0].status").value("SCHEDULED"));
    }

    @Test
    void shouldGetLessonsForStudent() throws Exception {
        // Create lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusHours(1));
        lesson.setEndTime(LocalDateTime.now().plusHours(2));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson);

        mockMvc.perform(get("/api/calendar/lessons/{userId}", student.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessons").isArray())
                .andExpect(jsonPath("$.lessons[0].tutorId").value(tutor.getId()));
    }

    @Test
    void shouldGetPublicBusyTimes() throws Exception {
        // Create lesson
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().plusHours(1));
        lesson.setEndTime(LocalDateTime.now().plusHours(2));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson);

        // Create calendar
        Calendar calendar = new Calendar();
        calendar.setUser(tutor);
        calendar.setCalendarUrl("https://calendar.example.com/feed.ics");
        calendar.setActive(true);
        calendarRepository.save(calendar);

        LocalDateTime now = LocalDateTime.now();
        List<GoogleCalendarService.BusyTime> mockBusyTimes = List.of(
            new GoogleCalendarService.BusyTime(
                now.plusHours(3),
                now.plusHours(4),
                "Public Event",
                null
            )
        );

        when(googleCalendarService.fetchBusyTimes(anyString()))
            .thenReturn(mockBusyTimes);

        mockMvc.perform(get("/api/calendar/public/{userId}", tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busyTimes").isArray())
                .andExpect(jsonPath("$.count").exists());
    }

    @Test
    void shouldReturnErrorForPublicBusyTimesWithInvalidUser() throws Exception {
        mockMvc.perform(get("/api/calendar/public/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid user ID"));
    }
}


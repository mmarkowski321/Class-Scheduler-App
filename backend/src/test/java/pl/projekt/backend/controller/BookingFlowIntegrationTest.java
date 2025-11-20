package pl.projekt.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pl.projekt.backend.model.Calendar;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.CalendarRepository;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.EmailService;
import pl.projekt.backend.service.GoogleCalendarService;
import pl.projekt.backend.util.JwtUtil;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class BookingFlowIntegrationTest {

    private static final String STUDENT_TOKEN = "student-token";
    private static final String TUTOR_TOKEN = "tutor-token";
    private static final String MEET_LINK = "https://meet.google.com/abc-defg-hij";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @MockBean
    private GoogleCalendarService googleCalendarService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private EmailService emailService;

    private Tutor tutor;
    private Student student;
    private Calendar calendar;
    private LocalDateTime busyStart;
    private LocalDateTime busyEnd;

    @BeforeEach
    void setUp() {
        lessonRepository.deleteAll();
        calendarRepository.deleteAll();
        userRepository.deleteAll();

        student = new Student();
        student.setEmail("student@example.com");
        student.setPassword("secret");
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
        student.setLanguages("pl");
        student.setTimezone("Europe/Warsaw");
        student = userRepository.save(student);

        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword("pass");
        tutor.setFirstName("Anna");
        tutor.setLastName("Nowak");
        tutor.setBirthDate(LocalDate.of(1990, 6, 15));
        tutor.setEducation("Uniwersytet");
        tutor.setExperienceYears(5);
        tutor.setSubjects("mathematics,physics");
        tutor.setHourlyRate(150.0);
        tutor.setLessonDuration(60);
        tutor.setTeachingLanguages("pl,en");
        tutor.setLessonModes("{\"online\":true}");
        tutor.setTeachingMethods("interactive");
        tutor.setBio("Passionate tutor");
        tutor.setPreferredDays("{\"mon\":true,\"tue\":true,\"wed\":true,\"thu\":true,\"fri\":true}");
        tutor.setBufferTime(15);
        tutor.setMaxLessonsPerDay(4);
        tutor = (Tutor) userRepository.save(tutor);

        calendar = new Calendar();
        calendar.setUser(tutor);
        calendar.setCalendarUrl("https://calendar.example/test.ics");
        calendar.setActive(true);
        calendarRepository.save(calendar);

        // Find next Monday (or next weekday if today is Monday-Friday)
        LocalDateTime candidateStart = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
        // If the day is Saturday or Sunday, move to next Monday
        DayOfWeek dayOfWeek = candidateStart.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY) {
            candidateStart = candidateStart.plusDays(2); // Move to Monday
        } else if (dayOfWeek == DayOfWeek.SUNDAY) {
            candidateStart = candidateStart.plusDays(1); // Move to Monday
        }
        busyStart = candidateStart;
        busyEnd = busyStart.plusHours(1);

        when(googleCalendarService.fetchBusyTimes(calendar.getCalendarUrl()))
                .thenReturn(List.of(new GoogleCalendarService.BusyTime(busyStart, busyEnd, "Busy", "External meeting")));
        when(googleCalendarService.isEnabled()).thenReturn(true);
        when(googleCalendarService.createLessonEvent(any(Lesson.class)))
                .thenReturn(Optional.of(new GoogleCalendarService.CalendarEvent("event-123", MEET_LINK)));

        when(jwtUtil.validateToken(STUDENT_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(STUDENT_TOKEN)).thenReturn(student.getId());

        when(jwtUtil.validateToken(TUTOR_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(TUTOR_TOKEN)).thenReturn(tutor.getId());
        when(jwtUtil.getRoleFromToken(TUTOR_TOKEN)).thenReturn("TUTOR");
    }

    @AfterEach
    void clean() {
        lessonRepository.deleteAll();
        calendarRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void bookingFlowShouldRespectCalendarConflictsAndGenerateMeetingLink() throws Exception {
        LocalDateTime conflictingStart = busyStart.plusMinutes(15);
        attemptBooking(conflictingStart)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        LocalDateTime freeSlotStart = busyEnd.plusHours(1);
        // Ensure freeSlotStart is also on a weekday (Mon-Fri)
        DayOfWeek freeSlotDay = freeSlotStart.getDayOfWeek();
        if (freeSlotDay == DayOfWeek.SATURDAY) {
            freeSlotStart = freeSlotStart.plusDays(2); // Move to Monday
        } else if (freeSlotDay == DayOfWeek.SUNDAY) {
            freeSlotStart = freeSlotStart.plusDays(1); // Move to Monday
        }
        long lessonId = createBooking(freeSlotStart);

        mockMvc.perform(post("/api/tutors/" + tutor.getId() + "/bookings/" + lessonId + "/confirm")
                        .header("Authorization", bearer(TUTOR_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(LessonStatus.SCHEDULED.name()))
                .andExpect(jsonPath("$.meetingLink").value(MEET_LINK));

        Lesson stored = lessonRepository.findById(lessonId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(LessonStatus.SCHEDULED);
        assertThat(stored.getMeetingLink()).isEqualTo(MEET_LINK);
        assertThat(stored.getGoogleEventId()).isEqualTo("event-123");

        verify(googleCalendarService, atLeastOnce()).fetchBusyTimes(calendar.getCalendarUrl());
        verify(googleCalendarService).createLessonEvent(ArgumentMatchers.argThat(lesson -> lesson.getId().equals(lessonId)));
    }

    private long createBooking(LocalDateTime start) throws Exception {
        MvcResult result = attemptBooking(start)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lessonId").exists())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.get("lessonId").asLong();
    }

    private org.springframework.test.web.servlet.ResultActions attemptBooking(LocalDateTime start) throws Exception {
        Map<String, Object> payload = Map.of(
                "start", start.toString(),
                "durationMinutes", 60,
                "deliveryMode", LessonDeliveryMode.ONLINE.name()
        );

        return mockMvc.perform(post("/api/tutors/" + tutor.getId() + "/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload))
                .header("Authorization", bearer(STUDENT_TOKEN)));
    }

    private String bearer(String rawToken) {
        return "Bearer " + rawToken;
    }
}


package pl.projekt.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.FileSystemUtils;
import pl.projekt.backend.model.Calendar;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.CalendarRepository;
import pl.projekt.backend.repository.UserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CalendarRepository calendarRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Student student;
    private Tutor tutor;

    @BeforeEach
    void setUp() throws Exception {
        calendarRepository.deleteAll();
        userRepository.deleteAll();
        cleanUploads();

        student = new Student();
        student.setEmail("student@example.com");
        student.setPassword("secret");
        student.setFirstName("Milosz");
        student.setLastName("Markowski");
        student.setBirthDate(LocalDate.of(2005, 1, 1));
        student.setTimezone("Europe/Warsaw");
        student.setLanguages("polish");
        student = (Student) userRepository.save(student);

        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword("secret");
        tutor.setFirstName("Anna");
        tutor.setLastName("Nowak");
        tutor.setBirthDate(LocalDate.of(1990, 1, 1));
        tutor.setEmailVerified(true);
        tutor = (Tutor) userRepository.save(tutor);
    }

    @Test
    void shouldUpdateStudentProfileFields() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("school", "LO nr 1");
        payload.put("grade", "2A");
        payload.put("track", "mat-fiz");
        payload.put("meetingMode", "{\"online\":true}");
        payload.put("preferredTools", "{\"meet\":true}");
        payload.put("preferredDays", "{\"mon\":true}");
        payload.put("shareProfile", true);

        mockMvc.perform(put("/api/profile/student/" + student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.school").value("LO nr 1"))
                .andExpect(jsonPath("$.grade").value("2A"))
                .andExpect(jsonPath("$.track").value("mat-fiz"));

        Student updated = (Student) userRepository.findById(student.getId()).orElseThrow();
        assertThat(updated.getSchool()).isEqualTo("LO nr 1");
        assertThat(updated.getGrade()).isEqualTo("2A");
        assertThat(updated.getTrack()).isEqualTo("mat-fiz");
    }

    @Test
    void shouldHandlePhotoUploadAndDeletion() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G'}
        );

        mockMvc.perform(multipart("/api/profile/student/" + student.getId() + "/photo")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").exists());

        Student reloaded = (Student) userRepository.findById(student.getId()).orElseThrow();
        assertThat(reloaded.getPhotoUrl()).startsWith("/uploads/student-photos/");

        Path photoPath = Path.of(".").toAbsolutePath().resolve(reloaded.getPhotoUrl().substring(1));
        assertThat(Files.exists(photoPath)).isTrue();

        mockMvc.perform(delete("/api/profile/student/" + student.getId() + "/photo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value(""));

        Student afterDelete = (Student) userRepository.findById(student.getId()).orElseThrow();
        assertThat(afterDelete.getPhotoUrl()).isNull();
        assertThat(Files.exists(photoPath)).isFalse();
    }

    @Test
    void shouldGetProfile() throws Exception {
        mockMvc.perform(get("/api/profile/" + student.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId()))
                .andExpect(jsonPath("$.email").value("student@example.com"))
                .andExpect(jsonPath("$.firstName").value("Milosz"))
                .andExpect(jsonPath("$.lastName").value("Markowski"));
    }

    @Test
    void shouldReturnNotFoundForNonExistentProfile() throws Exception {
        mockMvc.perform(get("/api/profile/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateTutorProfile() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("education", "University");
        payload.put("experienceYears", 5);
        payload.put("subjects", "math,physics");
        payload.put("hourlyRate", 100.0);
        payload.put("lessonDuration", 60);
        payload.put("bio", "Experienced tutor");

        mockMvc.perform(put("/api/profile/tutor/" + tutor.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.education").value("University"))
                .andExpect(jsonPath("$.experienceYears").value(5))
                .andExpect(jsonPath("$.subjects").value("math,physics"))
                .andExpect(jsonPath("$.hourlyRate").value(100.0))
                .andExpect(jsonPath("$.lessonDuration").value(60))
                .andExpect(jsonPath("$.bio").value("Experienced tutor"));

        Tutor updated = (Tutor) userRepository.findById(tutor.getId()).orElseThrow();
        assertThat(updated.getEducation()).isEqualTo("University");
        assertThat(updated.getExperienceYears()).isEqualTo(5);
        assertThat(updated.getSubjects()).isEqualTo("math,physics");
    }

    @Test
    void shouldRejectUpdateTutorProfileWithInvalidId() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("education", "University");

        mockMvc.perform(put("/api/profile/tutor/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid tutor ID"));
    }

    @Test
    void shouldGetUserCalendars() throws Exception {
        Calendar calendar = new Calendar();
        calendar.setUser(student);
        calendar.setCalendarUrl("https://calendar.google.com/calendar/ical/test/basic.ics");
        calendar.setName("My Calendar");
        calendar.setActive(true);
        calendarRepository.save(calendar);

        mockMvc.perform(get("/api/profile/" + student.getId() + "/calendars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calendars").isArray())
                .andExpect(jsonPath("$.calendars.length()").value(1))
                .andExpect(jsonPath("$.calendars[0].calendarUrl").value("https://calendar.google.com/calendar/ical/test/basic.ics"))
                .andExpect(jsonPath("$.calendars[0].name").value("My Calendar"))
                .andExpect(jsonPath("$.calendars[0].active").value(true));
    }

    @Test
    void shouldAddCalendar() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("calendarUrl", "https://calendar.google.com/calendar/ical/test/basic.ics");
        payload.put("name", "New Calendar");

        mockMvc.perform(post("/api/profile/" + student.getId() + "/calendars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calendarUrl").value("https://calendar.google.com/calendar/ical/test/basic.ics"))
                .andExpect(jsonPath("$.name").value("New Calendar"))
                .andExpect(jsonPath("$.active").value(true));

        java.util.List<Calendar> calendars = calendarRepository.findByUserIdAndActiveTrue(student.getId());
        assertThat(calendars).hasSize(1);
        assertThat(calendars.get(0).getCalendarUrl()).isEqualTo("https://calendar.google.com/calendar/ical/test/basic.ics");
    }

    @Test
    void shouldRejectAddCalendarWithMissingUrl() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("name", "New Calendar");

        mockMvc.perform(post("/api/profile/" + student.getId() + "/calendars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Calendar URL is required"));
    }

    @Test
    void shouldDeleteCalendar() throws Exception {
        Calendar calendar = new Calendar();
        calendar.setUser(student);
        calendar.setCalendarUrl("https://calendar.google.com/calendar/ical/test/basic.ics");
        calendar.setName("My Calendar");
        calendar.setActive(true);
        calendar = calendarRepository.save(calendar);

        mockMvc.perform(delete("/api/profile/" + student.getId() + "/calendars/" + calendar.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Calendar deleted successfully"));

        assertThat(calendarRepository.findById(calendar.getId())).isEmpty();
    }

    @Test
    void shouldRejectDeleteCalendarWithInvalidId() throws Exception {
        mockMvc.perform(delete("/api/profile/" + student.getId() + "/calendars/99999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Calendar not found"));
    }

    private void cleanUploads() throws Exception {
        Path uploads = Path.of("uploads");
        if (Files.exists(uploads)) {
            FileSystemUtils.deleteRecursively(uploads);
        }
    }
}


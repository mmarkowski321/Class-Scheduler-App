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
import pl.projekt.backend.model.Student;
import pl.projekt.backend.repository.UserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private ObjectMapper objectMapper;

    private Student student;

    @BeforeEach
    void setUp() throws Exception {
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

        student = userRepository.save(student);
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

    private void cleanUploads() throws Exception {
        Path uploads = Path.of("uploads");
        if (Files.exists(uploads)) {
            FileSystemUtils.deleteRecursively(uploads);
        }
    }
}


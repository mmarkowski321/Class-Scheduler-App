package pl.projekt.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import pl.projekt.backend.config.JwtPrincipal;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Review;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.jackson.serialization.FAIL_ON_EMPTY_BEANS=false")
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerIntegrationTest {

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

    private Student student;
    private Tutor tutor;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        lessonRepository.deleteAll();
        userRepository.deleteAll();

        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword("secret");
        tutor.setFirstName("Adam");
        tutor.setLastName("Nowak");
        tutor.setBirthDate(LocalDate.of(1990, 1, 1));
        tutor = userRepository.save(tutor);

        student = new Student();
        student.setEmail("student@example.com");
        student.setPassword("secret");
        student.setFirstName("Marta");
        student.setLastName("Kowalska");
        student.setBirthDate(LocalDate.of(2005, 5, 5));
        student.setTimezone("Europe/Warsaw");
        student = userRepository.save(student);

        lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().minusDays(1));
        lesson.setEndTime(LocalDateTime.now().minusDays(1).plusHours(1));
        lesson.setStatus(LessonStatus.COMPLETED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson = lessonRepository.save(lesson);

        // Mock authentication
        JwtPrincipal principal = new JwtPrincipal(student.getId(), "STUDENT");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, null);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void studentCanSubmitReview() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lessonId", lesson.getId(),
                                "studentId", student.getId(),
                                "tutorRating", 5,
                                "platformRating", 4,
                                "comment", "Świetna lekcja!"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorRating").value(5))
                .andExpect(jsonPath("$.platformRating").value(4))
                .andExpect(jsonPath("$.comment").value("Świetna lekcja!"));

        Review saved = reviewRepository.findByLessonId(lesson.getId()).orElseThrow();
        assertThat(saved.getStudentReviewAt()).isNotNull();
        assertThat(saved.getTutorRating()).isEqualTo(5);
        assertThat(saved.getPlatformRating()).isEqualTo(4);
    }

    @Test
    void tutorCanSubmitFeedbackAfterStudentReview() throws Exception {
        // ensure base review exists
        Review review = new Review();
        review.setLesson(lesson);
        review.setStudent(student);
        review.setTutor(tutor);
        review.setTutorRating(5);
        review.setPlatformRating(5);
        reviewRepository.save(review);

        mockMvc.perform(post("/api/reviews/lesson/" + lesson.getId() + "/tutor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tutorId", tutor.getId(),
                                "studentRating", 4,
                                "platformRating", 5,
                                "comment", "Student przygotowany."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentBehaviorRating").value(4))
                .andExpect(jsonPath("$.tutorPlatformRating").value(5))
                .andExpect(jsonPath("$.tutorComment").value("Student przygotowany."));

        Review saved = reviewRepository.findByLessonId(lesson.getId()).orElseThrow();
        assertThat(saved.getTutorReviewAt()).isNotNull();
        assertThat(saved.getTutorComment()).isEqualTo("Student przygotowany.");
    }
}


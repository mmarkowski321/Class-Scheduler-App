package pl.projekt.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.projekt.backend.config.JwtPrincipal;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Review;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewController reviewController;

    private Lesson lesson;
    private Student student;
    private Tutor tutor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(reviewController).build();

        lesson = new Lesson();
        lesson.setId(5L);

        tutor = new Tutor();
        tutor.setId(9L);

        student = new Student();
        student.setId(3L);

        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStatus(LessonStatus.COMPLETED);

        // Mock authentication
        JwtPrincipal principal = new JwtPrincipal(student.getId(), "STUDENT");
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, null);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void createReview_shouldPersistWhenPayloadValid() throws Exception {
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(userRepository.findById(tutor.getId())).thenReturn(Optional.of(tutor));
        when(reviewRepository.findByLessonId(lesson.getId())).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lessonId", lesson.getId(),
                                "studentId", student.getId(),
                                "tutorRating", 5,
                                "platformRating", 4,
                                "comment", "Super lekcja"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorRating").value(5))
                .andExpect(jsonPath("$.platformRating").value(4));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getTutorRating()).isEqualTo(5);
        assertThat(captor.getValue().getStudentReviewAt()).isNotNull();
    }

    @Test
    void createReview_shouldRejectOutOfRangeRating() throws Exception {
        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "lessonId", lesson.getId(),
                                "studentId", student.getId(),
                                "tutorRating", 6,
                                "platformRating", 1,
                                "comment", "bad"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void tutorReview_shouldValidateOwnership() throws Exception {
        Review existing = new Review();
        existing.setLesson(lesson);
        existing.setStudent(student);
        existing.setTutor(tutor);

        when(lessonRepository.findById(lesson.getId())).thenReturn(Optional.of(lesson));
        when(reviewRepository.findByLessonId(lesson.getId())).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post("/api/reviews/lesson/" + lesson.getId() + "/tutor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "tutorId", tutor.getId(),
                                "studentRating", 5,
                                "platformRating", 4,
                                "comment", "dobrze"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentBehaviorRating").value(5));
    }
}


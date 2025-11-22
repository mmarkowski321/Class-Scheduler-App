package pl.projekt.backend.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Review;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class StatsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private Tutor tutor;
    private Student student;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        lessonRepository.deleteAll();
        userRepository.deleteAll();

        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setPassword("password");
        tutor.setFirstName("Tutor");
        tutor.setLastName("Test");
        tutor.setBirthDate(java.time.LocalDate.of(1990, 1, 1));
        tutor.setEmailVerified(true);
        tutor.setSubjects("math");
        tutor.setEducation("University");
        tutor.setExperienceYears(5);
        tutor.setHourlyRate(100.0);
        tutor.setLessonDuration(60);
        tutor.setTeachingLanguages("pl");
        tutor.setLessonModes("{\"online\":true}");
        tutor.setTeachingMethods("Interactive");
        tutor.setBio("Test tutor");
        tutor = (Tutor) userRepository.save(tutor);

        student = new Student();
        student.setEmail("student@example.com");
        student.setPassword("password");
        student.setFirstName("Student");
        student.setLastName("Test");
        student.setBirthDate(java.time.LocalDate.of(2010, 1, 1));
        student.setEmailVerified(true);
        student = (Student) userRepository.save(student);
    }

    @AfterEach
    void tearDown() {
        reviewRepository.deleteAll();
        lessonRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldGetHomepageStats() throws Exception {
        // Create completed lesson from last month
        Lesson lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.now().minusDays(15));
        lesson.setEndTime(LocalDateTime.now().minusDays(15).plusHours(1));
        lesson.setStatus(LessonStatus.COMPLETED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(lesson);

        // Create review with platform rating
        Review review = new Review();
        review.setLesson(lesson);
        review.setTutor(tutor);
        review.setStudent(student);
        review.setTutorRating(5);
        review.setPlatformRating(5);
        reviewRepository.save(review);

        mockMvc.perform(get("/api/stats/homepage")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutors").value(1))
                .andExpect(jsonPath("$.monthlyLessons").value(1))
                .andExpect(jsonPath("$.satisfiedStudents").value(100.0));
    }

    @Test
    void shouldGetHomepageStatsWithNoLessons() throws Exception {
        mockMvc.perform(get("/api/stats/homepage")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutors").value(1))
                .andExpect(jsonPath("$.monthlyLessons").value(0))
                .andExpect(jsonPath("$.satisfiedStudents").isEmpty());
    }

    @Test
    void shouldGetHomepageStatsWithMultipleTutors() throws Exception {
        Tutor tutor2 = new Tutor();
        tutor2.setEmail("tutor2@example.com");
        tutor2.setPassword("password");
        tutor2.setFirstName("Tutor2");
        tutor2.setLastName("Test");
        tutor2.setBirthDate(java.time.LocalDate.of(1990, 1, 1));
        tutor2.setEmailVerified(true);
        tutor2.setSubjects("english");
        tutor2.setEducation("University");
        tutor2.setExperienceYears(3);
        tutor2.setHourlyRate(80.0);
        tutor2.setLessonDuration(90);
        tutor2.setTeachingLanguages("en");
        tutor2.setLessonModes("{\"online\":true}");
        tutor2.setTeachingMethods("Interactive");
        tutor2.setBio("Test tutor 2");
        userRepository.save(tutor2);

        Tutor tutor3 = new Tutor();
        tutor3.setEmail("tutor3@example.com");
        tutor3.setPassword("password");
        tutor3.setFirstName("Tutor3");
        tutor3.setLastName("Test");
        tutor3.setBirthDate(java.time.LocalDate.of(1990, 1, 1));
        tutor3.setEmailVerified(true);
        tutor3.setSubjects("physics");
        tutor3.setEducation("University");
        tutor3.setExperienceYears(4);
        tutor3.setHourlyRate(120.0);
        tutor3.setLessonDuration(60);
        tutor3.setTeachingLanguages("pl");
        tutor3.setLessonModes("{\"online\":true}");
        tutor3.setTeachingMethods("Interactive");
        tutor3.setBio("Test tutor 3");
        userRepository.save(tutor3);

        mockMvc.perform(get("/api/stats/homepage")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutors").value(3));
    }

    @Test
    void shouldExcludeOldCompletedLessons() throws Exception {
        // Create completed lesson older than 1 month
        Lesson oldLesson = new Lesson();
        oldLesson.setTutor(tutor);
        oldLesson.setStudent(student);
        oldLesson.setStartTime(LocalDateTime.now().minusMonths(2));
        oldLesson.setEndTime(LocalDateTime.now().minusMonths(2).plusHours(1));
        oldLesson.setStatus(LessonStatus.COMPLETED);
        oldLesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(oldLesson);

        // Create completed lesson from last month
        Lesson recentLesson = new Lesson();
        recentLesson.setTutor(tutor);
        recentLesson.setStudent(student);
        recentLesson.setStartTime(LocalDateTime.now().minusDays(15));
        recentLesson.setEndTime(LocalDateTime.now().minusDays(15).plusHours(1));
        recentLesson.setStatus(LessonStatus.COMPLETED);
        recentLesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lessonRepository.save(recentLesson);

        mockMvc.perform(get("/api/stats/homepage")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyLessons").value(1));
    }

    @Test
    void shouldCalculateAverageRatingCorrectly() throws Exception {
        Lesson lesson1 = new Lesson();
        lesson1.setTutor(tutor);
        lesson1.setStudent(student);
        lesson1.setStartTime(LocalDateTime.now().minusDays(15));
        lesson1.setEndTime(LocalDateTime.now().minusDays(15).plusHours(1));
        lesson1.setStatus(LessonStatus.COMPLETED);
        lesson1.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson1 = lessonRepository.save(lesson1);

        // Create reviews with different ratings
        Review review1 = new Review();
        review1.setLesson(lesson1);
        review1.setTutor(tutor);
        review1.setStudent(student);
        review1.setTutorRating(5);
        review1.setPlatformRating(5);
        reviewRepository.save(review1);

        Student student2 = new Student();
        student2.setEmail("student2@example.com");
        student2.setPassword("password");
        student2.setFirstName("Student2");
        student2.setLastName("Test");
        student2.setBirthDate(java.time.LocalDate.of(2010, 1, 1));
        student2.setEmailVerified(true);
        student2 = (Student) userRepository.save(student2);

        Lesson lesson2 = new Lesson();
        lesson2.setTutor(tutor);
        lesson2.setStudent(student2);
        lesson2.setStartTime(LocalDateTime.now().minusDays(10));
        lesson2.setEndTime(LocalDateTime.now().minusDays(10).plusHours(1));
        lesson2.setStatus(LessonStatus.COMPLETED);
        lesson2.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson2 = lessonRepository.save(lesson2);

        Review review2 = new Review();
        review2.setLesson(lesson2);
        review2.setTutor(tutor);
        review2.setStudent(student2);
        review2.setTutorRating(4);
        review2.setPlatformRating(4);
        reviewRepository.save(review2);

        mockMvc.perform(get("/api/stats/homepage")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.satisfiedStudents").value(90.0)); // (5+4)/2 = 4.5, 4.5/5 * 100 = 90%
    }
}


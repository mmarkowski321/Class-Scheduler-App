package pl.projekt.backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.LessonDeliveryMode;
import pl.projekt.backend.model.LessonStatus;
import pl.projekt.backend.model.Student;
import pl.projekt.backend.model.Tutor;
import pl.projekt.backend.repository.LessonRepository;
import pl.projekt.backend.repository.ReviewRepository;
import pl.projekt.backend.repository.UserRepository;
import pl.projekt.backend.service.EmailService;
import pl.projekt.backend.service.GoogleCalendarService;
import pl.projekt.backend.service.MeetingLinkService;
import pl.projekt.backend.service.TutorBookingValidator;
import pl.projekt.backend.util.JwtUtil;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TutorControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @Mock
    private MeetingLinkService meetingLinkService;

    @Mock
    private GoogleCalendarService googleCalendarService;

    @Mock
    private TutorBookingValidator tutorBookingValidator;

    @InjectMocks
    private TutorController tutorController;

    private Tutor tutor;
    private Student student;

    @BeforeEach
    void setUp() {
        tutor = new Tutor();
        tutor.setId(1L);
        tutor.setFirstName("John");
        tutor.setLastName("Doe");
        tutor.setSubjects("mathematics,physics");
        tutor.setTeachingLanguages("Polish,English");
        tutor.setCity("Warsaw");
        tutor.setHourlyRate(100.0);
        tutor.setLessonDuration(60);
        tutor.setEducation("University");
        tutor.setExperienceYears(5);
        tutor.setTeachingMethods("Interactive");
        tutor.setBio("Experienced tutor");
        tutor.setLessonModes("{\"online\":true,\"onsite\":true}");
        tutor.setTravelRadius(10);

        student = new Student();
        student.setId(2L);
        student.setFirstName("Jane");
        student.setLastName("Smith");
        student.setBirthDate(LocalDate.of(2010, 1, 1));
    }

    // Tests matchesQuery - returns true when query is empty
    @Test
    void shouldMatchQueryWhenEmpty() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("matchesQuery", Tutor.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(tutorController, tutor, null);
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "   ");
        assertThat(result).isTrue();
    }

    // Tests matchesQuery - matches by first name
    @Test
    void shouldMatchQueryByFirstName() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("matchesQuery", Tutor.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(tutorController, tutor, "john");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "JOHN");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "Jo");
        assertThat(result).isTrue();
    }

    // Tests matchesQuery - matches by last name
    @Test
    void shouldMatchQueryByLastName() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("matchesQuery", Tutor.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(tutorController, tutor, "doe");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "DOE");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "Do");
        assertThat(result).isTrue();
    }

    // Tests matchesQuery - matches by subjects
    @Test
    void shouldMatchQueryBySubjects() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("matchesQuery", Tutor.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(tutorController, tutor, "mathematics");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "physics");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "math");
        assertThat(result).isTrue();
    }

    // Tests matchesQuery - matches by teaching languages
    @Test
    void shouldMatchQueryByTeachingLanguages() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("matchesQuery", Tutor.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(tutorController, tutor, "polish");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "english");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "pol");
        assertThat(result).isTrue();
    }

    // Tests matchesQuery - returns false when no match
    @Test
    void shouldNotMatchQueryWhenNoMatch() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("matchesQuery", Tutor.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(tutorController, tutor, "nonexistent");
        assertThat(result).isFalse();

        result = (boolean) method.invoke(tutorController, tutor, "xyz123");
        assertThat(result).isFalse();
    }

    // Tests matchesQuery - handles null tutor fields
    @Test
    void shouldHandleNullTutorFields() throws Exception {
        Tutor nullTutor = new Tutor();
        nullTutor.setFirstName(null);
        nullTutor.setLastName(null);
        nullTutor.setSubjects(null);
        nullTutor.setTeachingLanguages(null);

        Method method = TutorController.class.getDeclaredMethod("matchesQuery", Tutor.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(tutorController, nullTutor, "test");
        assertThat(result).isFalse();
    }

    // Tests parseValues - parses JSON object format
    @Test
    void shouldParseValuesFromJsonObject() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "{\"online\":true,\"onsite\":false,\"hybrid\":true}");

        assertThat(result).containsExactlyInAnyOrder("online", "hybrid");
        assertThat(result).doesNotContain("onsite");
    }

    // Tests parseValues - parses JSON array format
    @Test
    void shouldParseValuesFromJsonArray() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "[\"online\",\"onsite\",\"hybrid\"]");

        assertThat(result).containsExactlyInAnyOrder("online", "onsite", "hybrid");
    }

    // Tests parseValues - parses comma-separated format
    @Test
    void shouldParseValuesFromCommaSeparated() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "online,onsite,hybrid");

        assertThat(result).containsExactlyInAnyOrder("online", "onsite", "hybrid");
    }

    // Tests parseValues - handles empty string
    @Test
    void shouldReturnEmptyListForEmptyString() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "");

        assertThat(result).isEmpty();
    }

    // Tests parseValues - handles null
    @Test
    void shouldReturnEmptyListForNull() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, (String) null);

        assertThat(result).isEmpty();
    }

    // Tests parseValues - handles whitespace
    @Test
    void shouldTrimWhitespace() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, " online , onsite , hybrid ");

        assertThat(result).containsExactlyInAnyOrder("online", "onsite", "hybrid");
    }

    // Tests parseValues - handles invalid JSON (falls back to comma-separated)
    @Test
    void shouldFallbackToCommaSeparatedForInvalidJson() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "{invalid json}");

        assertThat(result).containsExactly("{invalid json}");
    }

    // Tests parseValues - handles JSON object with false values
    @Test
    void shouldFilterOutFalseValuesFromJsonObject() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "{\"online\":true,\"onsite\":false,\"hybrid\":0}");

        assertThat(result).containsExactly("online");
    }

    // Tests parseValues - handles JSON object with numeric values
    @Test
    void shouldHandleNumericValuesInJsonObject() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "{\"online\":1,\"onsite\":0,\"hybrid\":-1}");

        assertThat(result).containsExactlyInAnyOrder("online", "hybrid");
    }

    // Tests hasConflict - returns true when lessons overlap
    @Test
    void shouldDetectConflictWhenLessonsOverlap() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson existing = new Lesson();
        existing.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        existing.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        existing.setStatus(LessonStatus.SCHEDULED);
        lessons.add(existing);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 30);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 15, 30);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isTrue();
    }

    // Tests hasConflict - returns false when no overlap
    @Test
    void shouldNotDetectConflictWhenNoOverlap() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson existing = new Lesson();
        existing.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        existing.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        existing.setStatus(LessonStatus.SCHEDULED);
        lessons.add(existing);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 16, 0);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 17, 0);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isFalse();
    }

    // Tests hasConflict - ignores cancelled lessons
    @Test
    void shouldIgnoreCancelledLessons() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson cancelled = new Lesson();
        cancelled.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        cancelled.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        cancelled.setStatus(LessonStatus.CANCELLED);
        lessons.add(cancelled);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 30);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 15, 30);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isFalse();
    }

    // Tests hasConflict - ignores completed lessons
    @Test
    void shouldIgnoreCompletedLessons() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson completed = new Lesson();
        completed.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        completed.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        completed.setStatus(LessonStatus.COMPLETED);
        lessons.add(completed);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 30);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 15, 30);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isFalse();
    }

    // Tests hasConflict - checks REQUESTED status
    @Test
    void shouldCheckRequestedStatus() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson requested = new Lesson();
        requested.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        requested.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        requested.setStatus(LessonStatus.REQUESTED);
        lessons.add(requested);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 30);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 15, 30);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isTrue();
    }

    // Tests hasConflict - checks RESCHEDULED status
    @Test
    void shouldCheckRescheduledStatus() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson rescheduled = new Lesson();
        rescheduled.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        rescheduled.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        rescheduled.setStatus(LessonStatus.RESCHEDULED);
        lessons.add(rescheduled);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 30);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 15, 30);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isTrue();
    }

    // Tests hasConflict - checks IN_PROGRESS status
    @Test
    void shouldCheckInProgressStatus() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson inProgress = new Lesson();
        inProgress.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        inProgress.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        inProgress.setStatus(LessonStatus.IN_PROGRESS);
        lessons.add(inProgress);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 30);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 15, 30);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isTrue();
    }

    // Tests hasConflict - handles edge case where new lesson starts exactly when old ends
    @Test
    void shouldNotConflictWhenNewLessonStartsWhenOldEnds() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson existing = new Lesson();
        existing.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        existing.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        existing.setStatus(LessonStatus.SCHEDULED);
        lessons.add(existing);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 15, 0);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 16, 0);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isFalse();
    }

    // Tests hasConflict - handles edge case where new lesson ends exactly when old starts
    @Test
    void shouldNotConflictWhenNewLessonEndsWhenOldStarts() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson existing = new Lesson();
        existing.setStartTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        existing.setEndTime(LocalDateTime.of(2024, 12, 15, 16, 0));
        existing.setStatus(LessonStatus.SCHEDULED);
        lessons.add(existing);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 0);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 15, 0);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isFalse();
    }

    // Tests hasConflict - returns false for empty list
    @Test
    void shouldReturnFalseForEmptyLessonList() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 0);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 15, 0);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isFalse();
    }

    // Tests hasConflict - handles multiple overlapping lessons
    @Test
    void shouldDetectConflictWithMultipleLessons() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        
        Lesson lesson1 = new Lesson();
        lesson1.setStartTime(LocalDateTime.of(2024, 12, 15, 10, 0));
        lesson1.setEndTime(LocalDateTime.of(2024, 12, 15, 11, 0));
        lesson1.setStatus(LessonStatus.SCHEDULED);
        lessons.add(lesson1);

        Lesson lesson2 = new Lesson();
        lesson2.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        lesson2.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        lesson2.setStatus(LessonStatus.SCHEDULED);
        lessons.add(lesson2);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 30);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 15, 30);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isTrue();
    }

    // Tests hasConflict - handles null start/end times
    @Test
    void shouldHandleNullStartEndTimes() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson lesson = new Lesson();
        lesson.setStartTime(null);
        lesson.setEndTime(null);
        lesson.setStatus(LessonStatus.SCHEDULED);
        lessons.add(lesson);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 0);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 15, 0);

        // hasConflict should handle null start/end times gracefully
        // When lesson has null times, it cannot overlap with anything
        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isFalse();
    }

    // Tests hasConflict - handles lesson that completely contains new lesson
    @Test
    void shouldDetectConflictWhenExistingLessonContainsNew() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson existing = new Lesson();
        existing.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        existing.setEndTime(LocalDateTime.of(2024, 12, 15, 16, 0));
        existing.setStatus(LessonStatus.SCHEDULED);
        lessons.add(existing);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 30);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 15, 30);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isTrue();
    }

    // Tests hasConflict - handles new lesson that completely contains existing
    @Test
    void shouldDetectConflictWhenNewLessonContainsExisting() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("hasConflict", List.class, LocalDateTime.class, LocalDateTime.class);
        method.setAccessible(true);

        List<Lesson> lessons = new ArrayList<>();
        Lesson existing = new Lesson();
        existing.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 30));
        existing.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 30));
        existing.setStatus(LessonStatus.SCHEDULED);
        lessons.add(existing);

        LocalDateTime newStart = LocalDateTime.of(2024, 12, 15, 14, 0);
        LocalDateTime newEnd = LocalDateTime.of(2024, 12, 15, 16, 0);

        boolean result = (boolean) method.invoke(tutorController, lessons, newStart, newEnd);
        assertThat(result).isTrue();
    }

    // Tests matchesQuery - handles empty tutor fields
    @Test
    void shouldMatchQueryWithEmptyFields() throws Exception {
        Tutor emptyTutor = new Tutor();
        emptyTutor.setFirstName("");
        emptyTutor.setLastName("");
        emptyTutor.setSubjects("");
        emptyTutor.setTeachingLanguages("");

        Method method = TutorController.class.getDeclaredMethod("matchesQuery", Tutor.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(tutorController, emptyTutor, "test");
        assertThat(result).isFalse();
    }

    // Tests matchesQuery - case insensitive matching
    @Test
    void shouldMatchQueryCaseInsensitive() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("matchesQuery", Tutor.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(tutorController, tutor, "JOHN");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "john");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "John");
        assertThat(result).isTrue();
    }

    // Tests matchesQuery - partial matching
    @Test
    void shouldMatchQueryPartially() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("matchesQuery", Tutor.class, String.class);
        method.setAccessible(true);

        boolean result = (boolean) method.invoke(tutorController, tutor, "mat");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "phys");
        assertThat(result).isTrue();

        result = (boolean) method.invoke(tutorController, tutor, "pol");
        assertThat(result).isTrue();
    }

    // Tests parseValues - handles JSON object with string values
    @Test
    void shouldParseJsonObjectWithStringValues() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "{\"online\":\"true\",\"onsite\":\"false\"}");

        assertThat(result).contains("online");
        assertThat(result).doesNotContain("onsite");
    }

    // Tests parseValues - handles JSON object with empty keys
    @Test
    void shouldFilterEmptyKeysFromJsonObject() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "{\"online\":true,\"\":true,\"  \":true}");

        assertThat(result).contains("online");
        assertThat(result).doesNotContain("");
        assertThat(result).doesNotContain("  ");
    }

    // Tests parseValues - handles JSON array with empty strings
    @Test
    void shouldFilterEmptyStringsFromJsonArray() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "[\"online\",\"\",\"  \",\"onsite\"]");

        assertThat(result).containsExactlyInAnyOrder("online", "onsite");
        assertThat(result).doesNotContain("");
        assertThat(result).doesNotContain("  ");
    }

    // Tests parseValues - handles comma-separated with empty values
    @Test
    void shouldFilterEmptyValuesFromCommaSeparated() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "online,,onsite,  ,hybrid");

        assertThat(result).containsExactlyInAnyOrder("online", "onsite", "hybrid");
    }

    // Tests parseValues - handles JSON object with null values
    @Test
    void shouldFilterNullValuesFromJsonObject() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "{\"online\":true,\"onsite\":null}");

        assertThat(result).contains("online");
        assertThat(result).doesNotContain("onsite");
    }

    // Tests parseValues - handles malformed JSON array (falls back to comma-separated)
    @Test
    void shouldFallbackToCommaSeparatedForMalformedJsonArray() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "[invalid json array");

        assertThat(result).contains("[invalid json array");
    }

    // Tests parseValues - handles JSON object with boolean string values
    @Test
    void shouldParseJsonObjectWithBooleanStringValues() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "{\"online\":\"true\",\"onsite\":\"false\"}");

        assertThat(result).contains("online");
        assertThat(result).doesNotContain("onsite");
    }

    // Tests parseValues - handles JSON object with mixed value types
    @Test
    void shouldHandleMixedValueTypesInJsonObject() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, "{\"online\":true,\"onsite\":1,\"hybrid\":\"yes\",\"remote\":false}");

        assertThat(result).containsExactlyInAnyOrder("online", "onsite");
        assertThat(result).doesNotContain("remote", "hybrid");
    }

    // Tests parseValues - handles whitespace in JSON
    @Test
    void shouldHandleWhitespaceInJson() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, " { \"online\" : true , \"onsite\" : false } ");

        assertThat(result).contains("online");
        assertThat(result).doesNotContain("onsite");
    }

    // Tests parseValues - handles JSON array with whitespace
    @Test
    void shouldHandleWhitespaceInJsonArray() throws Exception {
        Method method = TutorController.class.getDeclaredMethod("parseValues", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) method.invoke(tutorController, " [ \"online\" , \"onsite\" , \"hybrid\" ] ");

        assertThat(result).containsExactlyInAnyOrder("online", "onsite", "hybrid");
    }
}


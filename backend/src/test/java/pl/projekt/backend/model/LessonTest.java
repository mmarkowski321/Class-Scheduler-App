package pl.projekt.backend.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class LessonTest {

    private Lesson lesson;
    private Tutor tutor;
    private Student student;

    @BeforeEach
    void setUp() {
        tutor = new Tutor();
        tutor.setEmail("tutor@example.com");
        tutor.setFirstName("Anna");
        tutor.setLastName("Nowak");
        tutor.setBirthDate(LocalDate.of(1990, 1, 1));

        student = new Student();
        student.setEmail("student@example.com");
        student.setFirstName("Jan");
        student.setLastName("Kowalski");
        student.setBirthDate(LocalDate.of(2010, 1, 1));

        lesson = new Lesson();
        lesson.setTutor(tutor);
        lesson.setStudent(student);
        lesson.setStartTime(LocalDateTime.of(2024, 12, 15, 14, 0));
        lesson.setEndTime(LocalDateTime.of(2024, 12, 15, 15, 0));
        lesson.setStatus(LessonStatus.REQUESTED);
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
    }

    // New lessons should default to REQUESTED status
    @Test
    void shouldSetDefaultStatus() {
        Lesson newLesson = new Lesson();
        assertThat(newLesson.getStatus()).isEqualTo(LessonStatus.REQUESTED);
    }

    // onCreate should initialize createdAt and updatedAt
    @Test
    void shouldSetCreatedAtOnPersist() {
        assertThat(lesson.getCreatedAt()).isNull();

        lesson.onCreate();

        assertThat(lesson.getCreatedAt()).isNotNull();
        assertThat(lesson.getUpdatedAt()).isNotNull();
    }

    // onUpdate should advance updatedAt while keeping createdAt intact
    @Test
    void shouldSetUpdatedAtOnUpdate() {
        lesson.onCreate();
        LocalDateTime originalCreatedAt = lesson.getCreatedAt();
        LocalDateTime originalUpdatedAt = lesson.getUpdatedAt();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        lesson.onUpdate();

        assertThat(lesson.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(lesson.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    // Supports full address details for onsite lessons
    @Test
    void shouldHandleOnSiteLesson() {
        lesson.setDeliveryMode(LessonDeliveryMode.ONSITE);
        lesson.setOnsiteCity("Warszawa");
        lesson.setOnsitePostalCode("00-001");
        lesson.setOnsiteStreet("ul. Przykładowa");
        lesson.setOnsiteBuilding("1");
        lesson.setOnsiteApartment("10");

        assertThat(lesson.getDeliveryMode()).isEqualTo(LessonDeliveryMode.ONSITE);
        assertThat(lesson.getOnsiteCity()).isEqualTo("Warszawa");
        assertThat(lesson.getOnsitePostalCode()).isEqualTo("00-001");
        assertThat(lesson.getOnsiteStreet()).isEqualTo("ul. Przykładowa");
        assertThat(lesson.getOnsiteBuilding()).isEqualTo("1");
        assertThat(lesson.getOnsiteApartment()).isEqualTo("10");
    }

    // Stores meeting link for online lessons
    @Test
    void shouldHandleOnlineLesson() {
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson.setMeetingLink("https://meet.jit.si/test");

        assertThat(lesson.getDeliveryMode()).isEqualTo(LessonDeliveryMode.ONLINE);
        assertThat(lesson.getMeetingLink()).isEqualTo("https://meet.jit.si/test");
    }

    // Can switch between online and onsite modes
    @Test
    void shouldHandleOnSiteAndOnlineLesson() {
        lesson.setDeliveryMode(LessonDeliveryMode.ONLINE);
        lesson.setMeetingLink("https://meet.jit.si/test");
        
        assertThat(lesson.getDeliveryMode()).isEqualTo(LessonDeliveryMode.ONLINE);
        assertThat(lesson.getMeetingLink()).isNotNull();
        
        lesson.setDeliveryMode(LessonDeliveryMode.ONSITE);
        lesson.setOnsiteCity("Warszawa");
        assertThat(lesson.getDeliveryMode()).isEqualTo(LessonDeliveryMode.ONSITE);
        assertThat(lesson.getOnsiteCity()).isNotNull();
    }

    // Stores Google Calendar event id if present
    @Test
    void shouldSetGoogleEventId() {
        lesson.setGoogleEventId("event-123");

        assertThat(lesson.getGoogleEventId()).isEqualTo("event-123");
    }

    // Stores free-form notes with the lesson
    @Test
    void shouldSetNotes() {
        lesson.setNotes("Please prepare homework for next lesson");

        assertThat(lesson.getNotes()).isEqualTo("Please prepare homework for next lesson");
    }

    // Allows typical status transitions throughout lesson lifecycle
    @Test
    void shouldHandleStatusTransitions() {
        assertThat(lesson.getStatus()).isEqualTo(LessonStatus.REQUESTED);

        lesson.setStatus(LessonStatus.SCHEDULED);
        assertThat(lesson.getStatus()).isEqualTo(LessonStatus.SCHEDULED);

        lesson.setStatus(LessonStatus.IN_PROGRESS);
        assertThat(lesson.getStatus()).isEqualTo(LessonStatus.IN_PROGRESS);

        lesson.setStatus(LessonStatus.COMPLETED);
        assertThat(lesson.getStatus()).isEqualTo(LessonStatus.COMPLETED);
    }

    // Sets start and end times accurately
    @Test
    void shouldSetStartAndEndTime() {
        LocalDateTime start = LocalDateTime.of(2024, 12, 20, 10, 0);
        LocalDateTime end = LocalDateTime.of(2024, 12, 20, 11, 0);

        lesson.setStartTime(start);
        lesson.setEndTime(end);

        assertThat(lesson.getStartTime()).isEqualTo(start);
        assertThat(lesson.getEndTime()).isEqualTo(end);
    }

    // Supports clearing meeting link
    @Test
    void shouldClearMeetingLink() {
        lesson.setMeetingLink("https://meet.jit.si/test");
        assertThat(lesson.getMeetingLink()).isNotNull();

        lesson.setMeetingLink(null);

        assertThat(lesson.getMeetingLink()).isNull();
    }

    // Supports clearing Google event id
    @Test
    void shouldClearGoogleEventId() {
        lesson.setGoogleEventId("event-123");
        assertThat(lesson.getGoogleEventId()).isNotNull();

        lesson.setGoogleEventId(null);

        assertThat(lesson.getGoogleEventId()).isNull();
    }
}


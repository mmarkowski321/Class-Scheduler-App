package pl.projekt.backend.service;

import org.junit.jupiter.api.Test;
import pl.projekt.backend.model.Lesson;
import pl.projekt.backend.model.Tutor;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class MeetingLinkServiceTest {

    private final MeetingLinkService service = new MeetingLinkService();

    @Test
    void generateJitsiLink_shouldIncludeLessonMetadataAndRandomSuffix() {
        Lesson lesson = new Lesson();
        lesson.setId(42L);
        lesson.setStartTime(LocalDateTime.of(2025, 11, 12, 14, 30));

        Tutor tutor = new Tutor();
        tutor.setLastName("Markowski");
        lesson.setTutor(tutor);

        String link = service.generateJitsiLink(lesson);

        assertThat(link).startsWith("https://meet.jit.si/eduscheduler-l42-202511121430-markowski-");

        String slug = link.substring("https://meet.jit.si/".length());
        assertThat(slug).containsPattern(Pattern.compile("-[a-z0-9]{6}$"));
    }

    @Test
    void generateJitsiLink_shouldFallbackGracefullyWhenLessonDataMissing() {
        String link = service.generateJitsiLink(null);

        assertThat(link).startsWith("https://meet.jit.si/eduscheduler-");
        assertThat(link).matches("https://meet\\.jit\\.si/eduscheduler-[a-z0-9]{6}");
    }
}


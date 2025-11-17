package pl.projekt.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import pl.projekt.backend.model.Lesson;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class MeetingLinkService {

    private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /**
     * Generates a deterministic-but-unique meeting slug for Jitsi.
     * The format is: eduscheduler-<lessonId>-<yyyyMMddHHmm>-<random6>
     * to avoid collisions while making it easy to trace the origin.
     */
    public String generateJitsiLink(Lesson lesson) {
        StringBuilder slug = new StringBuilder("eduscheduler");

        if (lesson != null) {
            if (lesson.getId() != null) {
                slug.append("-l").append(lesson.getId());
            }
            if (lesson.getStartTime() != null) {
                slug.append("-").append(lesson.getStartTime().format(ISO_DATE));
            }
            if (lesson.getTutor() != null && StringUtils.hasText(lesson.getTutor().getLastName())) {
                slug.append("-")
                        .append(normalize(lesson.getTutor().getLastName()));
            }
        }

        slug.append("-").append(randomSegment(6));
        return "https://meet.jit.si/" + slug;
    }

    private String normalize(String input) {
        return input.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }

    private String randomSegment(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return builder.toString();
    }
}





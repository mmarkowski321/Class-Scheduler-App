package pl.projekt.backend.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import pl.projekt.backend.model.Tutor;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class TutorPublicDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String city;
    private String photoUrl;
    private String bio;
    private Double hourlyRate;
    private Integer lessonDuration;
    private Integer experienceYears;
    private List<String> subjects;
    private List<String> teachingLanguages;
    private List<String> lessonModes;
    private List<String> preferredDays;
    private List<String> examResults;
    private String education;
    private Integer travelRadius;
    private String teachingMethods;
    private String website;
    private String linkedIn;
    private List<String> certificates;

    public static TutorPublicDto fromEntity(Tutor tutor) {
        TutorPublicDto dto = new TutorPublicDto();
        dto.id = tutor.getId();
        dto.firstName = tutor.getFirstName();
        dto.lastName = tutor.getLastName();
        dto.city = tutor.getCity();
        dto.photoUrl = tutor.getPhotoUrl();
        dto.bio = tutor.getBio();
        dto.hourlyRate = tutor.getHourlyRate();
        dto.lessonDuration = tutor.getLessonDuration();
        dto.experienceYears = tutor.getExperienceYears();
        dto.subjects = split(tutor.getSubjects());
        dto.teachingLanguages = split(tutor.getTeachingLanguages());
        dto.lessonModes = split(tutor.getLessonModes());
        dto.preferredDays = split(tutor.getPreferredDays());
        dto.examResults = split(tutor.getExamResults());
        dto.education = tutor.getEducation();
        dto.travelRadius = tutor.getTravelRadius();
        dto.teachingMethods = tutor.getTeachingMethods();
        dto.website = tutor.getWebsite();
        dto.linkedIn = tutor.getLinkedIn();
        dto.certificates = split(tutor.getCertificates());
        return dto;
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    private static List<String> split(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();

        // Try JSON object where true values mean enabled flags (e.g. {"online":true})
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                Map<String, Object> data = OBJECT_MAPPER.readValue(trimmed, MAP_TYPE);
                return data.entrySet().stream()
                        .filter(entry -> isTruthy(entry.getValue()))
                        .map(Map.Entry::getKey)
                        .map(String::trim)
                        .filter(value -> !value.isEmpty())
                        .collect(Collectors.toList());
            } catch (Exception ignored) {
                // fall back to CSV parsing
            }
        }

        // Try JSON array string (e.g. ["Math","Physics"])
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                List<String> data = OBJECT_MAPPER.readValue(trimmed, LIST_TYPE);
                return data.stream()
                        .map(String::trim)
                        .filter(part -> !part.isEmpty())
                        .collect(Collectors.toList());
            } catch (Exception ignored) {
                // fall back
            }
        }

        // Default CSV split
        return Arrays.stream(trimmed.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .collect(Collectors.toList());
    }

    private static boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() != 0d;
        return Boolean.parseBoolean(String.valueOf(value));
    }
}



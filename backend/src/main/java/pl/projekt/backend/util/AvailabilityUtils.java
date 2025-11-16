package pl.projekt.backend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper methods for parsing availability-related data structures.
 */
public final class AvailabilityUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {};

    private AvailabilityUtils() {
    }

    /**
     * Parse preferred day payload stored as String into a set of DayOfWeek values.
     * Supports JSON objects with boolean flags, JSON arrays, and CSV strings.
     */
    public static Set<DayOfWeek> parsePreferredDays(String raw) {
        LinkedHashSet<DayOfWeek> result = new LinkedHashSet<>();
        if (!StringUtils.hasText(raw)) {
            return result;
        }

        String trimmed = raw.trim();

        // JSON object: {"mon":true,"tue":false,...}
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                Map<String, Object> map = OBJECT_MAPPER.readValue(trimmed, MAP_TYPE);
                map.forEach((key, value) -> addIfTruthy(result, key, value));
                return result;
            } catch (Exception ignored) {
                // fall through to other strategies
            }
        }

        // JSON array: ["mon","tue"]
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                List<String> list = OBJECT_MAPPER.readValue(trimmed, LIST_TYPE);
                list.forEach(item -> addIfTruthy(result, item, true));
                return result;
            } catch (Exception ignored) {
                // fall through
            }
        }

        // CSV fallback: "mon,tue"
        Arrays.stream(trimmed.split("[,;\\s]+"))
                .forEach(token -> addIfTruthy(result, token, true));

        return result;
    }

    private static void addIfTruthy(Set<DayOfWeek> target, String key, Object value) {
        if (!StringUtils.hasText(key)) {
            return;
        }
        if (!isTruthy(value)) {
            return;
        }
        DayOfWeek day = toDayOfWeek(key);
        if (day != null) {
            target.add(day);
        }
    }

    private static DayOfWeek toDayOfWeek(String key) {
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "mon", "monday", "pon", "poniedziałek" -> DayOfWeek.MONDAY;
            case "tue", "tuesday", "wt", "wtorek" -> DayOfWeek.TUESDAY;
            case "wed", "wednesday", "śr", "sr", "środa", "sroda" -> DayOfWeek.WEDNESDAY;
            case "thu", "thursday", "czw", "czwartek" -> DayOfWeek.THURSDAY;
            case "fri", "friday", "pt", "piątek", "piatek" -> DayOfWeek.FRIDAY;
            case "sat", "saturday", "sob", "sobota" -> DayOfWeek.SATURDAY;
            case "sun", "sunday", "nd", "niedz", "niedziela" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    private static boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean bool) return bool;
        if (value instanceof Number number) return number.doubleValue() != 0d;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * Convert DayOfWeek to canonical short code (mon, tue, ...).
     */
    public static String toDayCode(DayOfWeek dayOfWeek) {
        if (dayOfWeek == null) {
            return null;
        }
        return switch (dayOfWeek) {
            case MONDAY -> "mon";
            case TUESDAY -> "tue";
            case WEDNESDAY -> "wed";
            case THURSDAY -> "thu";
            case FRIDAY -> "fri";
            case SATURDAY -> "sat";
            case SUNDAY -> "sun";
        };
    }

    /**
     * Helper returning set of day codes based on stored raw string.
     */
    public static Set<String> parsePreferredDayCodes(String raw) {
        return parsePreferredDays(raw).stream()
                .map(AvailabilityUtils::toDayCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}



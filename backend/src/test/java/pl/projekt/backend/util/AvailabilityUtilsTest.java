package pl.projekt.backend.util;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityUtilsTest {

    @Test
    void parsePreferredDays_shouldHandleJsonObject() {
        String raw = "{\"mon\":true,\"tue\":false,\"wed\":1}";

        Set<DayOfWeek> result = AvailabilityUtils.parsePreferredDays(raw);

        assertThat(result).containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);
    }

    @Test
    void parsePreferredDays_shouldHandleJsonArray() {
        String raw = "[\"fri\",\"Sunday\"]";

        Set<DayOfWeek> result = AvailabilityUtils.parsePreferredDays(raw);

        assertThat(result).containsExactly(DayOfWeek.FRIDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void parsePreferredDays_shouldHandleCsvBackups() {
        String raw = "mon, wt, Śr";

        Set<DayOfWeek> result = AvailabilityUtils.parsePreferredDays(raw);

        assertThat(result).containsExactly(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY
        );
    }

    @Test
    void toDayCodeShouldMapProperly() {
        assertThat(AvailabilityUtils.toDayCode(DayOfWeek.THURSDAY)).isEqualTo("thu");
        assertThat(AvailabilityUtils.toDayCode(null)).isNull();
    }

    @Test
    void parsePreferredDayCodesShouldReturnCanonicalCodes() {
        String raw = "{\"mon\":true,\"fri\":true}";

        assertThat(AvailabilityUtils.parsePreferredDayCodes(raw)).containsExactly("mon", "fri");
    }
}


package pl.projekt.backend.util;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class SubjectDictionaryTest {

    // Normalizes various math aliases into canonical "matematyka"
    @Test
    void shouldNormalizeMatematyka() {
        assertThat(SubjectDictionary.normalize("matematyka")).isEqualTo("matematyka");
        assertThat(SubjectDictionary.normalize("mat")).isEqualTo("matematyka");
        assertThat(SubjectDictionary.normalize("math")).isEqualTo("matematyka");
        assertThat(SubjectDictionary.normalize("mathematics")).isEqualTo("matematyka");
        assertThat(SubjectDictionary.normalize("Matematyka")).isEqualTo("matematyka");
        assertThat(SubjectDictionary.normalize("MAT")).isEqualTo("matematyka");
    }

    // Normalizes Polish language aliases into "jezyk-polski"
    @Test
    void shouldNormalizePolishLanguage() {
        assertThat(SubjectDictionary.normalize("jezyk-polski")).isEqualTo("jezyk-polski");
        assertThat(SubjectDictionary.normalize("polski")).isEqualTo("jezyk-polski");
        assertThat(SubjectDictionary.normalize("jpol")).isEqualTo("jezyk-polski");
        assertThat(SubjectDictionary.normalize("polish")).isEqualTo("jezyk-polski");
        assertThat(SubjectDictionary.normalize("język polski")).isEqualTo("jezyk-polski");
    }

    // Normalizes English language aliases into "jezyk-angielski"
    @Test
    void shouldNormalizeEnglishLanguage() {
        assertThat(SubjectDictionary.normalize("angielski")).isEqualTo("jezyk-angielski");
        assertThat(SubjectDictionary.normalize("ang")).isEqualTo("jezyk-angielski");
        assertThat(SubjectDictionary.normalize("english")).isEqualTo("jezyk-angielski");
    }

    // Normalizes biology aliases into "biologia"
    @Test
    void shouldNormalizeBiology() {
        assertThat(SubjectDictionary.normalize("biologia")).isEqualTo("biologia");
        assertThat(SubjectDictionary.normalize("bio")).isEqualTo("biologia");
        assertThat(SubjectDictionary.normalize("biology")).isEqualTo("biologia");
    }

    // Normalizes chemistry aliases into "chemia"
    @Test
    void shouldNormalizeChemistry() {
        assertThat(SubjectDictionary.normalize("chemia")).isEqualTo("chemia");
        assertThat(SubjectDictionary.normalize("chem")).isEqualTo("chemia");
        assertThat(SubjectDictionary.normalize("chemistry")).isEqualTo("chemia");
    }

    // Normalizes programming terms to canonical representations (programming, informatics)
    @Test
    void shouldNormalizeProgramming() {
        assertThat(SubjectDictionary.normalize("programowanie")).isEqualTo("informatyka");
        assertThat(SubjectDictionary.normalize("coding")).isEqualTo("programowanie");
        assertThat(SubjectDictionary.normalize("software development")).isEqualTo("programowanie");
    }
    
    // Normalizes informatics aliases into "informatyka"
    @Test
    void shouldNormalizeInformatics() {
        assertThat(SubjectDictionary.normalize("informatyka")).isEqualTo("informatyka");
        assertThat(SubjectDictionary.normalize("cs")).isEqualTo("informatyka");
        assertThat(SubjectDictionary.normalize("computer science")).isEqualTo("informatyka");
        assertThat(SubjectDictionary.normalize("programowanie")).isEqualTo("informatyka");
    }

    // Normalizes python keywords into "programowanie-python"
    @Test
    void shouldNormalizePython() {
        assertThat(SubjectDictionary.normalize("programowanie-python")).isEqualTo("programowanie-python");
        assertThat(SubjectDictionary.normalize("python")).isEqualTo("programowanie-python");
        assertThat(SubjectDictionary.normalize("python programming")).isEqualTo("programowanie-python");
    }

    // Returns null when no matching subject is found
    @Test
    void shouldReturnNullForUnknownSubject() {
        assertThat(SubjectDictionary.normalize("unknown-subject")).isNull();
        assertThat(SubjectDictionary.normalize("xyz")).isNull();
        assertThat(SubjectDictionary.normalize("")).isNull();
    }

    // Returns null for null input
    @Test
    void shouldReturnNullForNullInput() {
        assertThat(SubjectDictionary.normalize(null)).isNull();
    }

    // Extracts canonical subjects from comma-separated list
    @Test
    void shouldExtractSubjectsFromCommaSeparatedString() {
        Set<String> subjects = SubjectDictionary.extractSubjects("matematyka, biologia, chemia");

        assertThat(subjects).containsExactlyInAnyOrder("matematyka", "biologia", "chemia");
    }

    // Extracts canonical subjects from alias tokens
    @Test
    void shouldExtractSubjectsWithAliases() {
        Set<String> subjects = SubjectDictionary.extractSubjects("mat, bio, chem");

        assertThat(subjects).containsExactlyInAnyOrder("matematyka", "biologia", "chemia");
    }

    // Handles mixed case tokens and aliases
    @Test
    void shouldExtractSubjectsMixedCaseAndAliases() {
        Set<String> subjects = SubjectDictionary.extractSubjects("MAT, biology, Chemistry, python");

        assertThat(subjects).containsExactlyInAnyOrder("matematyka", "biologia", "chemia", "programowanie-python");
    }

    // Supports Polish language names for subjects
    @Test
    void shouldExtractSubjectsWithPolishNames() {
        Set<String> subjects = SubjectDictionary.extractSubjects("język polski, angielski, niemiecki");

        assertThat(subjects).containsExactlyInAnyOrder("jezyk-polski", "jezyk-angielski", "jezyk-niemiecki");
    }

    // Ignores extra whitespace around tokens
    @Test
    void shouldHandleWhitespace() {
        Set<String> subjects = SubjectDictionary.extractSubjects("  matematyka  ,  biologia  ,  chemia  ");

        assertThat(subjects).containsExactlyInAnyOrder("matematyka", "biologia", "chemia");
    }

    // Returns empty set for empty input
    @Test
    void shouldReturnEmptySetForEmptyString() {
        Set<String> subjects = SubjectDictionary.extractSubjects("");

        assertThat(subjects).isEmpty();
    }

    // Returns empty set for null input
    @Test
    void shouldReturnEmptySetForNullString() {
        Set<String> subjects = SubjectDictionary.extractSubjects(null);

        assertThat(subjects).isEmpty();
    }

    // Filters out unknown or unmapped tokens
    @Test
    void shouldFilterOutUnknownSubjects() {
        Set<String> subjects = SubjectDictionary.extractSubjects("matematyka, unknown-subject, biologia, xyz");

        assertThat(subjects).containsExactlyInAnyOrder("matematyka", "biologia");
    }

    // Removes duplicates while preserving unique canonical values
    @Test
    void shouldRemoveDuplicates() {
        Set<String> subjects = SubjectDictionary.extractSubjects("matematyka, matematyka, biologia, matematyka");

        assertThat(subjects).containsExactlyInAnyOrder("matematyka", "biologia");
    }

    // Extracts a single subject correctly
    @Test
    void shouldHandleSingleSubject() {
        Set<String> subjects = SubjectDictionary.extractSubjects("matematyka");

        assertThat(subjects).containsExactly("matematyka");
    }

    // Preserves insertion order for extracted subjects
    @Test
    void shouldPreserveOrderInLinkedHashSet() {
        Set<String> subjects = SubjectDictionary.extractSubjects("matematyka, biologia, chemia, fizyka");

        assertThat(subjects).containsExactly("matematyka", "biologia", "chemia", "fizyka");
    }

    // Normalizes variants with and without hyphens
    @Test
    void shouldNormalizeWithHyphens() {
        assertThat(SubjectDictionary.normalize("jezyk polski")).isEqualTo("jezyk-polski");
        assertThat(SubjectDictionary.normalize("jezyk-polski")).isEqualTo("jezyk-polski");
    }

    // Normalizes various programming-related subjects correctly
    @Test
    void shouldHandleProgrammingSubjects() {
        assertThat(SubjectDictionary.normalize("programowanie-web")).isEqualTo("programowanie-web");
        assertThat(SubjectDictionary.normalize("web development")).isEqualTo("programowanie-web");
        assertThat(SubjectDictionary.normalize("frontend")).isEqualTo("programowanie-web");
    }

    // Normalizes exam-related subjects properly
    @Test
    void shouldHandleExamSubjects() {
        assertThat(SubjectDictionary.normalize("matura polski")).isEqualTo("matura-polski");
        assertThat(SubjectDictionary.normalize("matura z polskiego")).isEqualTo("matura-polski");
        assertThat(SubjectDictionary.normalize("egzamin osmoklasisty")).isEqualTo("egzamin-osmoklasisty");
        assertThat(SubjectDictionary.normalize("e8")).isEqualTo("egzamin-osmoklasisty");
    }
}


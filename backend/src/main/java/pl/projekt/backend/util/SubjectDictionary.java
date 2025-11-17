package pl.projekt.backend.util;

import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Słownik przedmiotów z aliasami – ułatwia standaryzację nazw i wyszukiwanie.
 */
public final class SubjectDictionary {

    private static final List<SubjectEntry> SUBJECTS = List.of(
            new SubjectEntry("matematyka", "Matematyka", "Mathematics", List.of("mat", "math", "mathematics", "matematyka rozszerzona")),
            new SubjectEntry("jezyk-polski", "Język polski", "Polish language", List.of("polski", "jpol", "polish", "polish language", "język polski")),
            new SubjectEntry("jezyk-angielski", "Język angielski", "English language", List.of("angielski", "ang", "english", "english language", "język angielski")),
            new SubjectEntry("jezyk-niemiecki", "Język niemiecki", "German language", List.of("niemiecki", "niem", "german", "german language", "język niemiecki")),
            new SubjectEntry("jezyk-hiszpanski", "Język hiszpański", "Spanish language", List.of("hiszpanski", "hiszp", "spanish", "spanish language", "język hiszpański")),
            new SubjectEntry("jezyk-francuski", "Język francuski", "French language", List.of("francuski", "fr", "french", "french language", "język francuski")),
            new SubjectEntry("jezyk-wloski", "Język włoski", "Italian language", List.of("wloski", "italian", "italian language", "język włoski")),
            new SubjectEntry("jezyk-rosyjski", "Język rosyjski", "Russian language", List.of("rosyjski", "ros", "russian", "russian language", "język rosyjski")),
            new SubjectEntry("biologia", "Biologia", "Biology", List.of("bio", "biology")),
            new SubjectEntry("chemia", "Chemia", "Chemistry", List.of("chem", "chemistry")),
            new SubjectEntry("fizyka", "Fizyka", "Physics", List.of("physics", "fiz")),
            new SubjectEntry("geografia", "Geografia", "Geography", List.of("geo", "geography")),
            new SubjectEntry("historia", "Historia", "History", List.of("history")),
            new SubjectEntry("wos", "Wiedza o społeczeństwie", "Civics / Social studies", List.of("wiedza o społeczeństwie", "wiedza o spoleczenstwie", "civics", "social studies")),
            new SubjectEntry("informatyka", "Informatyka", "Computer science", List.of("cs", "computer science", "programowanie", "programming")),
            new SubjectEntry("programowanie", "Programowanie", "Programming", List.of("coding", "software development")),
            new SubjectEntry("statystyka", "Statystyka", "Statistics", List.of("statistics", "probability", "probability theory")),
            new SubjectEntry("ekonomia", "Ekonomia", "Economics", List.of("economics", "microeconomics", "macroeconomics")),
            new SubjectEntry("rachunkowosc", "Rachunkowość", "Accounting", List.of("accounting", "księgowość", "ksiegowosc")),
            new SubjectEntry("biznes", "Biznes i przedsiębiorczość", "Business & entrepreneurship", List.of("business", "entrepreneurship", "przedsiębiorczość", "przedsiebiorczosc")),
            new SubjectEntry("prawo", "Prawo", "Law", List.of("law", "legal studies")),
            new SubjectEntry("psychologia", "Psychologia", "Psychology", List.of("psychology")),
            new SubjectEntry("filozofia", "Filozofia", "Philosophy", List.of("philosophy")),
            new SubjectEntry("muzyka", "Muzyka", "Music", List.of("music", "nauka gry", "instrumenty")),
            new SubjectEntry("plastyka", "Plastyka / Sztuka", "Art", List.of("sztuka", "art", "fine arts")),
            new SubjectEntry("edukacja-wczesnoszkolna", "Edukacja wczesnoszkolna", "Primary education", List.of("nauczanie początkowe", "primary education")),
            new SubjectEntry("przyroda", "Przyroda", "Science / Nature studies", List.of("science", "nature studies")),
            new SubjectEntry("matura-polski", "Matura – język polski", "Polish – high school exit exam", List.of("matura z polskiego", "polish matura", "matura język polski")),
            new SubjectEntry("matura-matematyka", "Matura – matematyka", "Mathematics – high school exit exam", List.of("matura z matematyki", "math matura")),
            new SubjectEntry("matura-angielski", "Matura – język angielski", "English – high school exit exam", List.of("matura z angielskiego", "english matura")),
            new SubjectEntry("egzamin-osmoklasisty", "Egzamin ósmoklasisty", "Primary school exam", List.of("e8", "egzamin osmoklasisty", "primary exam")),
            new SubjectEntry("programowanie-web", "Programowanie webowe", "Web development", List.of("web development", "frontend", "backend", "javascript", "html", "css")),
            new SubjectEntry("programowanie-mobile", "Programowanie mobilne", "Mobile development", List.of("mobile development", "android", "ios", "kotlin", "swift", "flutter", "react native")),
            new SubjectEntry("programowanie-python", "Python", "Python", List.of("python", "python programming", "python basics", "python advanced")),
            new SubjectEntry("programowanie-java", "Java", "Java", List.of("java", "java programming", "java basics", "java advanced")),
            new SubjectEntry("programowanie-cpp", "C/C++", "C / C++", List.of("c++", "cpp", "c language", "c programming")),
            new SubjectEntry("programowanie-sql", "Bazy danych / SQL", "Databases / SQL", List.of("sql", "databases", "database design", "bazy danych")),
            new SubjectEntry("logika", "Logika", "Logic", List.of("logic", "formal logic")),
            new SubjectEntry("etyka", "Etyka", "Ethics", List.of("ethics")),
            new SubjectEntry("socjologia", "Socjologia", "Sociology", List.of("sociology")),
            new SubjectEntry("marketing", "Marketing", "Marketing", List.of("digital marketing", "marketing internetowy")),
            new SubjectEntry("zarzadzanie-projektami", "Zarządzanie projektami", "Project management", List.of("project management", "pm", "scrum", "agile")),
            new SubjectEntry("grafika-komputerowa", "Grafika komputerowa", "Graphic design", List.of("graphic design", "digital art", "grafika komputerowa")),
            new SubjectEntry("fotografia", "Fotografia", "Photography", List.of("photography")),
            new SubjectEntry("robotyka", "Robotyka", "Robotics", List.of("robotics")),
            new SubjectEntry("elektronika", "Elektronika", "Electronics", List.of("electronics", "elektrotechnika")),
            new SubjectEntry("mechanika", "Mechanika", "Mechanics", List.of("mechanics"))
    );

    private static final Map<String, String> ALIAS_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        for (SubjectEntry entry : SUBJECTS) {
            Set<String> variants = new HashSet<>();
            variants.add(entry.value());
            variants.add(entry.value().replace("-", " "));
            variants.add(entry.labelPl().toLowerCase(Locale.ROOT));
            variants.add(entry.labelEn().toLowerCase(Locale.ROOT));
            entry.aliases().stream()
                    .map(alias -> alias.toLowerCase(Locale.ROOT))
                    .forEach(variants::add);

            for (String variant : variants) {
                String key = normalizeKey(variant);
                if (!map.containsKey(key)) {
                    map.put(key, entry.value());
                }
            }
        }
        ALIAS_MAP = Collections.unmodifiableMap(map);
    }

    private SubjectDictionary() {}

    private static String normalizeKey(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Zwraca kanoniczną nazwę przedmiotu (value) na podstawie dowolnego aliasu.
     */
    public static String normalize(String value) {
        String key = normalizeKey(value);
        if (key == null) return null;
        return ALIAS_MAP.get(key);
    }

    /**
     * Parsuje surowy tekst (np. wpisany przez tutora) i zwraca zbiór kanonicznych nazw.
     */
    public static Set<String> extractSubjects(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptySet();
        }
        return Arrays.stream(raw.split(","))
                .map(token -> normalizeKey(token))
                .filter(Objects::nonNull)
                .map(ALIAS_MAP::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private record SubjectEntry(String value, String labelPl, String labelEn, List<String> aliases) {}
}



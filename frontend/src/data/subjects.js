/**
 * Lista wspieranych przedmiotów wraz z aliasami (synonimami i skrótami),
 * wykorzystywana zarówno przy wprowadzaniu danych przez tutorów,
 * jak i przy filtrowaniu oraz wyszukiwaniu po stronie studentów.
 *
 * Każdy wpis zawiera:
 * - value  -> ustandaryzowana nazwa (klucz w systemie)
 * - label  -> nazwa wyświetlana w UI (język polski)
 * - aliases -> tablica dopuszczalnych wariantów (w tym angielskich)
 */

export const SUBJECTS = [
  { value: "matematyka", label: "Matematyka", labelEn: "Mathematics", aliases: ["mat", "math", "mathematics", "matematyka rozszerzona"] },
  { value: "jezyk-polski", label: "Język polski", labelEn: "Polish language", aliases: ["polski", "jpol", "polish language", "polish"] },
  { value: "jezyk-angielski", label: "Język angielski", labelEn: "English language", aliases: ["angielski", "ang", "english", "english language"] },
  { value: "jezyk-niemiecki", label: "Język niemiecki", labelEn: "German language", aliases: ["niemiecki", "niem", "german", "german language"] },
  { value: "jezyk-hiszpanski", label: "Język hiszpański", labelEn: "Spanish language", aliases: ["hiszpanski", "hiszp", "spanish", "spanish language"] },
  { value: "jezyk-francuski", label: "Język francuski", labelEn: "French language", aliases: ["francuski", "fr", "french", "french language"] },
  { value: "jezyk-wloski", label: "Język włoski", labelEn: "Italian language", aliases: ["wloski", "italian", "italian language"] },
  { value: "jezyk-rosyjski", label: "Język rosyjski", labelEn: "Russian language", aliases: ["rosyjski", "ros", "russian", "russian language"] },
  { value: "biologia", label: "Biologia", labelEn: "Biology", aliases: ["bio", "biology"] },
  { value: "chemia", label: "Chemia", labelEn: "Chemistry", aliases: ["chem", "chemistry"] },
  { value: "fizyka", label: "Fizyka", labelEn: "Physics", aliases: ["physics", "fiz"] },
  { value: "geografia", label: "Geografia", labelEn: "Geography", aliases: ["geo", "geography"] },
  { value: "historia", label: "Historia", labelEn: "History", aliases: ["history"] },
  { value: "wos", label: "Wiedza o społeczeństwie", labelEn: "Civics / Social studies", aliases: ["wiedza o spoleczenstwie", "wos", "civics", "social studies"] },
  { value: "informatyka", label: "Informatyka", labelEn: "Computer science", aliases: ["cs", "computer science", "programowanie", "programming"] },
  { value: "programowanie", label: "Programowanie", labelEn: "Programming", aliases: ["coding", "software development", "programming basics"] },
  { value: "matematyka-dyskretna", label: "Matematyka dyskretna", labelEn: "Discrete mathematics", aliases: ["discrete math", "discrete mathematics"] },
  { value: "statystyka", label: "Statystyka", labelEn: "Statistics", aliases: ["statistics", "probability", "probability theory"] },
  { value: "rachunkowosc", label: "Rachunkowość", labelEn: "Accounting", aliases: ["rachunkowosc", "accounting", "ksiegowosc"] },
  { value: "ekonomia", label: "Ekonomia", labelEn: "Economics", aliases: ["economics", "microeconomics", "macroeconomics"] },
  { value: "biznes", label: "Biznes i przedsiębiorczość", labelEn: "Business & entrepreneurship", aliases: ["business", "entrepreneurship", "przedsiebiorczosc"] },
  { value: "prawo", label: "Prawo", labelEn: "Law", aliases: ["law", "legal studies"] },
  { value: "psychologia", label: "Psychologia", labelEn: "Psychology", aliases: ["psychology"] },
  { value: "filozofia", label: "Filozofia", labelEn: "Philosophy", aliases: ["philosophy"] },
  { value: "muzyka", label: "Muzyka", labelEn: "Music", aliases: ["music", "nauka gry", "instrumenty"] },
  { value: "plastyka", label: "Plastyka / Sztuka", labelEn: "Art", aliases: ["sztuka", "art", "fine arts"] },
  { value: "edukacja-wczesnoszkolna", label: "Edukacja wczesnoszkolna", labelEn: "Primary education", aliases: ["nauczanie poczatkowe", "primary education", "elementary tutoring"] },
  { value: "przyroda", label: "Przyroda", labelEn: "Science / Nature studies", aliases: ["science", "nature studies"] },
  { value: "matematyka-olimpiady", label: "Matematyka (olimpiady)", labelEn: "Mathematics (contests)", aliases: ["math contests", "olimpiada matematyczna", "competition math"] },
  { value: "fizyka-olimpiady", label: "Fizyka (olimpiady)", labelEn: "Physics (contests)", aliases: ["physics contests", "olimpiada fizyczna"] },
  { value: "jezyk-polski-olimpiady", label: "Język polski (olimpiady)", labelEn: "Polish (contests)", aliases: ["polish contests", "olimpiada polonistyczna"] },
  { value: "matura-polski", label: "Matura – język polski", labelEn: "Polish – high school exit exam", aliases: ["matura polski", "polish matura", "matura język polski"] },
  { value: "matura-matematyka", label: "Matura – matematyka", labelEn: "Mathematics – high school exit exam", aliases: ["matura matematyka", "math matura"] },
  { value: "matura-angielski", label: "Matura – język angielski", labelEn: "English – high school exit exam", aliases: ["matura angielski", "english matura"] },
  { value: "egzamin-osmoklasisty", label: "Egzamin ósmoklasisty", labelEn: "Primary school exam", aliases: ["egzamin osmoklasisty", "e8", "primary exam"] },
  { value: "programowanie-web", label: "Programowanie webowe", labelEn: "Web development", aliases: ["frontend", "backend", "web development", "javascript", "html", "css"] },
  { value: "programowanie-mobile", label: "Programowanie mobilne", labelEn: "Mobile development", aliases: ["mobile development", "android", "ios", "kotlin", "swift", "flutter", "react native"] },
  { value: "programowanie-python", label: "Python", labelEn: "Python", aliases: ["python programming", "python basics", "python advanced"] },
  { value: "programowanie-java", label: "Java", labelEn: "Java", aliases: ["java programming", "java basics", "java advanced"] },
  { value: "programowanie-cpp", label: "C/C++", labelEn: "C / C++", aliases: ["cpp", "c++", "c language", "c programming"] },
  { value: "programowanie-sql", label: "Bazy danych / SQL", labelEn: "Databases / SQL", aliases: ["sql", "databases", "database design"] },
  { value: "logika", label: "Logika", labelEn: "Logic", aliases: ["logic", "formal logic"] },
  { value: "etyka", label: "Etyka", labelEn: "Ethics", aliases: ["ethics"] },
  { value: "socjologia", label: "Socjologia", labelEn: "Sociology", aliases: ["sociology"] },
  { value: "marketing", label: "Marketing", labelEn: "Marketing", aliases: ["digital marketing", "marketing internetowy"] },
  { value: "zarzadzanie-projektami", label: "Zarządzanie projektami", labelEn: "Project management", aliases: ["project management", "pm", "scrum", "agile"] },
  { value: "grafika-komputerowa", label: "Grafika komputerowa", labelEn: "Graphic design", aliases: ["graphic design", "digital art", "grafika"] },
  { value: "fotografia", label: "Fotografia", labelEn: "Photography", aliases: ["photography"] },
  { value: "robotyka", label: "Robotyka", labelEn: "Robotics", aliases: ["robotics"] },
  { value: "elektronika", label: "Elektronika", labelEn: "Electronics", aliases: ["electronics", "elektrotechnika"] },
  { value: "mechanika", label: "Mechanika", labelEn: "Mechanics", aliases: ["mechanics"] }
];

/**
 * Mapa alias -> wartość ustandaryzowana, przygotowana do szybkiego wyszukiwania.
 * Wszystkie klucze są znormalizowane (trim + lower-case).
 */
export const SUBJECT_ALIAS_MAP = SUBJECTS.reduce((acc, subject) => {
  const variants = new Set([subject.value, subject.label, subject.labelEn, ...subject.aliases]);
  variants.forEach((variant) => {
    const key = variant.toLowerCase().trim();
    if (!key) return;
    if (!acc[key]) {
      acc[key] = subject.value;
    }
  });
  return acc;
}, {});

/**
 * Zwraca ustandaryzowaną nazwę przedmiotu (value) na podstawie dowolnego aliasu.
 * Gdy nie znajdzie dopasowania, zwraca null.
 */
export function normalizeSubject(value) {
  if (!value) return null;
  const key = String(value).toLowerCase().trim();
  return SUBJECT_ALIAS_MAP[key] || null;
}



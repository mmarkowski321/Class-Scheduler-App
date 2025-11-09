## TECHNICZNA WYKONALNOŚĆ SYSTEMU

### Architektura warstwowa

System został zaprojektowany zgodnie z architekturą trójwarstwową, która zapewnia separację odpowiedzialności, skalowalność oraz łatwość utrzymania:

#### 1. Warstwa prezentacji (Frontend)
- **Technologia:** React.js 19.1.1
- **Framework budowania:** Vite 7.1.7
- **Język programowania:** JavaScript (ES6+)
- **Biblioteki kluczowe:**
  - React Router DOM 7.9.4 – routing i nawigacja
  - React i18next 16.0.0 – internacjonalizacja (PL/EN)
  - FullCalendar 6.1.19 – komponent kalendarza
  - Material-UI 7.3.4 – komponenty interfejsu użytkownika
- **Licencja:** MIT License (wszystkie biblioteki open-source)

#### 2. Warstwa logiki biznesowej (Backend)
- **Framework:** Spring Boot 3.5.6
- **Język programowania:** Java 21 (LTS)
- **Moduły Spring:**
  - Spring Data JPA – warstwa dostępu do danych
  - Spring Security – autoryzacja i uwierzytelnianie
  - Spring Web – REST API
  - Spring Mail – powiadomienia e-mail
- **Biblioteki dodatkowe:**
  - JJWT 0.12.3 – JSON Web Tokens dla autoryzacji
  - Lombok – redukcja boilerplate code
  - PostgreSQL Driver – sterownik bazy danych
- **Licencja:** Apache License 2.0 (Spring Framework), Apache License 2.0 (JJWT), MIT License (Lombok)

#### 3. Warstwa persystencji danych (Database)
- **System zarządzania bazą danych:** PostgreSQL (najnowsza wersja LTS)
- **ORM:** Hibernate (wbudowany w Spring Data JPA)
- **Baza danych deweloperska:** H2 Database (in-memory, do testów)
- **Licencja:** PostgreSQL License (open-source, podobna do MIT/BSD)

### Technologie infrastrukturalne

#### Konteneryzacja i orkiestracja
- **Docker** – konteneryzacja aplikacji
- **Kubernetes (K8s)** – orkiestracja kontenerów, skalowanie poziome przez HPA
- **Helm** – zarządzanie pakietami Kubernetes

#### CI/CD i automatyzacja
- **Jenkins** – ciągła integracja i wdrożenia
- **Git** – kontrola wersji (GitFlow workflow)
- **Maven** – zarządzanie zależnościami i budowanie (backend)
- **npm** – zarządzanie pakietami (frontend)

#### Powiadomienia
- **SMTP (Gmail/Resend.com)** – wysyłka e-maili
- **JavaMailSender** – integracja z serwerami SMTP

### Integracje zewnętrzne

- **Google Calendar API** – synchronizacja kalendarzy (iCal feed)
- **USOS API** – integracja z systemem uczelni (iCal feed)
- **REST API** – komunikacja między warstwami

### Wykonalność techniczna

Wszystkie zastosowane technologie są:
- **Sprawdzone i dojrzałe** – szeroko używane w przemyśle
- **Dokumentowane** – bogata dokumentacja i społeczność
- **Open-source** – brak kosztów licencyjnych
- **Kompatybilne** – Java 21 LTS zapewnia długoterminowe wsparcie
- **Skalowalne** – architektura mikrousługowa gotowa do skalowania poziomego

### Wymagania systemowe

**Backend:**
- Java 21 lub nowsza
- Minimum 512 MB RAM (zalecane 1 GB)
- Maven 3.6+

**Frontend:**
- Node.js 18+ lub nowsza
- npm 9+ lub yarn

**Baza danych:**
- PostgreSQL 12+ (produkcja)
- H2 Database (rozwój/testy)

**Infrastruktura:**
- Kubernetes 1.24+
- Docker 20.10+
- Helm 3.0+



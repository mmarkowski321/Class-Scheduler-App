## KOSZTORYS PROJEKTU

### 1. Koszty rozwoju (Development Costs)

#### 1.1. Koszty pracy zespołu deweloperskiego

**Założenia:**
- Liczba deweloperów: 3 osoby
- Czas pracy: 4 godziny dziennie
- Dni robocze: 5 dni w tygodniu
- Czas trwania projektu: 2 miesiące (8 tygodni)
- Stawka godzinowa: 50 zł netto/godzinę

**Obliczenia:**
- Godziny tygodniowo na osobę: 4 godziny × 5 dni = 20 godzin/tydzień
- Łączne godziny tygodniowo (zespół): 3 osoby × 20 godzin = 60 godzin/tydzień
- Łączne godziny w projekcie: 60 godzin × 8 tygodni = **480 godzin**
- Koszt pracy: 480 godzin × 50 zł = **24 000 zł netto**

**Podział kosztów pracy:**
- Backend development (Spring Boot, API): ~160 godzin × 50 zł = 8 000 zł
- Frontend development (React, UI/UX): ~160 godzin × 50 zł = 8 000 zł
- DevOps i infrastruktura (Docker, K8s, CI/CD): ~160 godzin × 50 zł = 8 000 zł

#### 1.2. Koszty narzędzi i licencji

**Narzędzia deweloperskie (koszt: 0 zł):**
- **IDE:** IntelliJ IDEA Community Edition / Visual Studio Code – darmowe
- **Kontrola wersji:** GitHub / GitLab – darmowe dla projektów open-source
- **CI/CD:** Jenkins – open-source, darmowe
- **Konteneryzacja:** Docker Desktop – darmowe dla użytkowników indywidualnych
- **Orkiestracja:** Kubernetes – open-source, darmowe
- **Narzędzia testowe:** JUnit, Jest – open-source, darmowe
- **Narzędzia do dokumentacji:** Markdown, Doxygen – darmowe

**Licencje oprogramowania:**
- Wszystkie wykorzystane technologie są open-source (MIT, Apache 2.0, PostgreSQL License)
- **Łączny koszt licencji: 0 zł**

### 2. Koszty infrastruktury i hostingu

#### 2.1. Środowisko deweloperskie (Development)
- **Koszt:** 0 zł (lokalne maszyny deweloperskie)

#### 2.2. Środowisko produkcyjne (Production)

**Opcja 1: Chmura publiczna (AWS/GCP/Azure)**
- **Kubernetes cluster (managed):** ~200-400 zł/miesiąc
- **PostgreSQL (managed database):** ~150-300 zł/miesiąc
- **Load balancer:** ~50-100 zł/miesiąc
- **Storage (dane, backupy):** ~50-100 zł/miesiąc
- **Monitoring i logi:** ~50-100 zł/miesiąc
- **Łączny koszt miesięczny:** ~500-1 000 zł/miesiąc

**Opcja 2: Hosting własny/VPS**
- **Serwer VPS (4 CPU, 8 GB RAM):** ~100-200 zł/miesiąc
- **Domena (.pl):** ~30-50 zł/rok (~3-5 zł/miesiąc)
- **Certyfikat SSL (Let's Encrypt):** 0 zł
- **Łączny koszt miesięczny:** ~105-205 zł/miesiąc

**Rekomendacja:** Dla projektu akademickiego/mvp zalecana jest opcja 2 (VPS), co daje **~150 zł/miesiąc** średnio.

#### 2.3. Usługi zewnętrzne

**E-mail (powiadomienia):**
- **Gmail SMTP:** 0 zł (dla małej liczby e-maili)
- **Resend.com:** 0 zł (tier darmowy do 3 000 e-maili/miesiąc)
- **Alternatywa płatna:** ~50-100 zł/miesiąc (przy większym wolumenie)

**Monitoring i analityka:**
- **Google Analytics:** 0 zł
- **Sentry (error tracking):** 0 zł (tier darmowy)
- **Uptime monitoring:** 0 zł (UptimeRobot – tier darmowy)

### 3. Koszty utrzymania systemu (Maintenance)

#### 3.1. Koszty miesięczne (po wdrożeniu)

**Infrastruktura:**
- Hosting/VPS: ~150 zł/miesiąc
- Domena: ~5 zł/miesiąc
- Backup storage: ~20 zł/miesiąc
- **Razem:** ~175 zł/miesiąc

**Utrzymanie i wsparcie techniczne:**
- Aktualizacje bezpieczeństwa: ~10 godzin/miesiąc × 50 zł = 500 zł/miesiąc
- Naprawa błędów i optymalizacja: ~5 godzin/miesiąc × 50 zł = 250 zł/miesiąc
- **Razem:** ~750 zł/miesiąc

**Łączny koszt utrzymania:** ~925 zł/miesiąc

#### 3.2. Koszty roczne

- Infrastruktura: 175 zł × 12 = 2 100 zł/rok
- Utrzymanie: 750 zł × 12 = 9 000 zł/rok
- **Razem:** ~11 100 zł/rok

### 4. Podsumowanie kosztorysu

#### Koszty jednorazowe (Development)
| Pozycja | Koszt |
|---------|-------|
| Praca zespołu (480 godzin) | 24 000 zł |
| Narzędzia deweloperskie | 0 zł |
| Licencje oprogramowania | 0 zł |
| **RAZEM** | **24 000 zł** |

#### Koszty miesięczne (Production)
| Pozycja | Koszt |
|---------|-------|
| Hosting i infrastruktura | 175 zł |
| Utrzymanie techniczne | 750 zł |
| **RAZEM** | **925 zł/miesiąc** |

#### Koszty roczne (Production)
| Pozycja | Koszt |
|---------|-------|
| Infrastruktura | 2 100 zł |
| Utrzymanie | 9 000 zł |
| **RAZEM** | **11 100 zł/rok** |

### 5. Uwagi i założenia

- Stawki godzinowe mogą się różnić w zależności od doświadczenia deweloperów
- Koszty hostingu mogą być niższe przy wykorzystaniu akademickich creditów chmurowych (AWS Educate, Google Cloud for Education)
- W przypadku skalowania systemu koszty infrastruktury mogą wzrosnąć proporcjonalnie
- Koszty utrzymania mogą być redukowane poprzez automatyzację (CI/CD, monitoring)
- Wszystkie kwoty podane w złotych polskich (PLN), netto



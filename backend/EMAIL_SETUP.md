# Email Setup (env-only)

Backend ma domyślnie ustawionego dostawcę SMTP na Resend. W pliku `application.properties` skonfigurowane są:

```properties
spring.mail.host=smtp.resend.com
spring.mail.port=587
spring.mail.username=resend
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
app.base-url=http://localhost:5173
```

Jedyne czego potrzebujesz, to ustawić hasło (API key) oraz opcjonalnie nadawcę jako zmienne środowiskowe PRZED uruchomieniem backendu.

## Szybki start (Windows PowerShell)

```powershell
$env:SPRING_MAIL_PASSWORD="TWÓJ_RESEND_API_KEY"
$env:MAIL_FROM="onboarding@resend.dev"  # opcjonalnie własny adres po weryfikacji domeny

cd "C:\Users\Acer\Desktop\Class-Scheduler-App\backend"
.\mvnw.cmd spring-boot:run
```

Uwagi:
- `MAIL_FROM` można pominąć – backend ustawi domyślnie `onboarding@resend.dev`.
- Jeśli zweryfikujesz swoją domenę w Resend, ustaw np. `MAIL_FROM=noreply@twojadomena.com`.

## Test

Zarejestruj użytkownika w aplikacji. Powinieneś dostać e‑mail weryfikacyjny. Jeśli nie:
- sprawdź, czy PowerShell był tym samym oknem, w którym odpalasz backend,
- upewnij się, że nie ma spacji/cudzysłowów w zmiennych (poza koniecznymi),
- sprawdź logi backendu pod kątem błędów SMTP.


# Konfiguracja zmiennych środowiskowych (bez pliku .env)

Backend korzysta z domyślnej konfiguracji SMTP (Resend). Przed uruchomieniem ustaw TYLKO hasło i opcjonalnie nadawcę jako zmienne środowiskowe.

## Windows (PowerShell)
```powershell
$env:SPRING_MAIL_PASSWORD="re_twoj_klucz_api"
$env:MAIL_FROM="onboarding@resend.dev"  # opcjonalnie własny adres po weryfikacji domeny

cd "C:\Users\Acer\Desktop\Class-Scheduler-App\backend"
.\mvnw.cmd spring-boot:run
```

## Linux/Mac
```bash
export SPRING_MAIL_PASSWORD="re_twoj_klucz_api"
export MAIL_FROM="onboarding@resend.dev" # opcjonalnie
./mvnw spring-boot:run
```

Nie używamy pliku `.env`.


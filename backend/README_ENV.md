# Konfiguracja zmiennych środowiskowych

## Opcja 1: Plik .env (Najłatwiejsze)

1. **Utwórz plik `.env` w folderze `backend/`**

2. **Dodaj zmienne:**
```env
RESEND_API_KEY=re_twoj_klucz_api
APP_BASE_URL=http://localhost:5173
```

3. **Uruchom aplikację** - Spring Boot automatycznie odczyta plik `.env`

## Opcja 2: Zmienne środowiskowe systemu

### Windows (PowerShell):
```powershell
$env:RESEND_API_KEY="re_twoj_klucz_api"
$env:APP_BASE_URL="http://localhost:5173"
```

### Linux/Mac:
```bash
export RESEND_API_KEY="re_twoj_klucz_api"
export APP_BASE_URL="http://localhost:5173"
```

## Opcja 3: Przekazanie przy uruchomieniu

```bash
RESEND_API_KEY=re_twoj_klucz_api ./mvnw spring-boot:run
```

---

**Ważne:** Plik `.env` jest w `.gitignore` - NIE BĘDZIE SIĘ COMMITOWAĆ do Git!


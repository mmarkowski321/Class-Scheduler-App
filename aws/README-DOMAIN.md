# Konfiguracja domeny eduscheduler.eu

## 1. Konfiguracja DNS w OVH Cloud

1. Zaloguj się do [OVH Cloud Control Panel](https://www.ovh.com/manager/)
2. Przejdź do: **Domeny** > **eduscheduler.eu** > **Strefa DNS** (DNS Zone)
3. Dodaj/Edytuj rekordy A:

   **Rekord A dla domeny głównej:**
   - **Nazwa (Name):** `@` (lub puste)
   - **Typ (Type):** `A`
   - **Wartość (Target):** `13.62.198.13` (Twój publiczny IP EC2)
   - **TTL:** `3600`

   **Rekord A dla www:**
   - **Nazwa (Name):** `www`
   - **Typ (Type):** `A`
   - **Wartość (Target):** `13.62.198.13` (Twój publiczny IP EC2)
   - **TTL:** `3600`

4. Zapisz zmiany
5. Poczekaj 5-30 minut na propagację DNS

## 2. Weryfikacja DNS

Sprawdź, czy DNS działa poprawnie:

```bash
dig eduscheduler.eu
dig www.eduscheduler.eu
nslookup eduscheduler.eu
```

Oba powinny wskazywać na Twój publiczny IP EC2.

## 3. Wdrożenie Ingress z domeną

Na VM wykonaj:

```bash
cd ~/Class-Scheduler-App/aws
git pull
./setup-https.sh
```

To wdroży Ingress z obsługą domeny `eduscheduler.eu` i `www.eduscheduler.eu`.

## 4. Certyfikat SSL

### Opcja A: Self-signed (szybkie, ale będzie ostrzeżenie w przeglądarce)

```bash
./setup-https.sh
```

### Opcja B: Let's Encrypt (darmowy, zaufany certyfikat) - **ZALECANE**

1. Poczekaj na propagację DNS (sprawdź z `dig`)
2. Uruchom:

```bash
./setup-letsencrypt.sh
```

Skrypt poprosi o Twój email (dla powiadomień Let's Encrypt).

## 5. Aktualizacja backendu

Po skonfigurowaniu domeny, zaktualizuj backend deployment:

```bash
cd ~/Class-Scheduler-App/aws
git pull
kubectl apply -f ../k8s/backend/deployment.yaml
kubectl rollout restart deployment eduscheduler-backend
```

To zaktualizuje `APP_BASE_URL` i `CORS_ALLOWED_ORIGINS` do użycia domeny.

## 6. Dostęp do aplikacji

Po skonfigurowaniu DNS i wdrożeniu:

- **HTTPS:** `https://eduscheduler.eu`
- **HTTPS (www):** `https://www.eduscheduler.eu`

## Troubleshooting

### DNS nie działa

- Sprawdź konfigurację w OVH
- Poczekaj dłużej na propagację (do 24h w skrajnych przypadkach)
- Sprawdź z różnych lokalizacji: `dig @8.8.8.8 eduscheduler.eu`

### Certyfikat Let's Encrypt się nie wydaje

- Upewnij się, że DNS działa (`dig eduscheduler.eu`)
- Sprawdź logi cert-manager: `kubectl logs -n cert-manager -l app=cert-manager`
- Sprawdź status certyfikatu: `kubectl describe certificate eduscheduler-tls-secret`

### Aplikacja niedostępna przez HTTPS

- Sprawdź, czy Security Group ma otwarty port 443
- Sprawdź status Ingress: `kubectl get ingress eduscheduler-ingress`
- Sprawdź logi nginx-ingress: `kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller`


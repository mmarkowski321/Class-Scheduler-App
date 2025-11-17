# 🚀 Kompletny Przewodnik - Od Key Pair do Kubernetes Deployment

**Pełna instrukcja krok po kroku** - od utworzenia key pair do wdrożenia wszystkich podów i serwisów na Kubernetes (minikube).

---

## 📋 Spis Treści

1. [Wymagania](#-wymagania)
2. [Krok 1: Utwórz Key Pair](#-krok-1-utwórz-key-pair)
3. [Krok 2: Utwórz VM z Minikube](#-krok-2-utwórz-vm-z-minikube)
4. [Krok 3: Skonfiguruj ECR Authentication](#-krok-3-skonfiguruj-ecr-authentication)
5. [Krok 4: Wdróż Wszystko na Kubernetes](#-krok-4-wdróż-wszystko-na-kubernetes)
6. [Krok 5: Sprawdź Działanie](#-krok-5-sprawdź-działanie)
7. [Troubleshooting](#-troubleshooting)

---

## ✅ Wymagania

Przed rozpoczęciem upewnij się że masz:

- ✅ **AWS CLI** skonfigurowany (`aws configure`)
- ✅ **AWS Account** z dostępem do EC2
- ✅ **Docker** (opcjonalnie - do lokalnego build)
- ✅ **Git Bash** lub **WSL** (dla skryptów bash)
- ✅ **Obrazy wypushowane do ECR** (backend i frontend)

**Sprawdź AWS CLI:**
```powershell
aws --version
aws configure list
```

**Sprawdź dostęp do ECR:**
```powershell
aws ecr describe-repositories --region eu-north-1
```

---

## 🔑 Krok 1: Utwórz Key Pair

**Cel:** Utworzenie klucza SSH do połączenia z VM.

### **Metoda A: Automatyczny Skrypt (Rekomendowane)**

```powershell
# W PowerShell
cd aws
.\create-key-pair.ps1

# W Git Bash - ustaw właściwe uprawnienia dla klucza
chmod 400 ~/.ssh/eduscheduler-key
```

**Co robi skrypt:**
- ✅ Generuje klucz SSH (RSA 4096 bit)
- ✅ Zapisuje w `C:\Users\<TwojaNazwa>\.ssh\eduscheduler-key`
- ✅ Importuje klucz publiczny do AWS

### **Metoda B: Ręcznie (PowerShell)**

```powershell
# 1. Wygeneruj klucz SSH
ssh-keygen -t rsa -b 4096 -f $env:USERPROFILE\.ssh\eduscheduler-key -N ""

# 2. Sprawdź czy klucze zostały utworzone
ls $env:USERPROFILE\.ssh\eduscheduler-key*

# 3. Zaimportuj klucz publiczny do AWS
$pubKey = Get-Content $env:USERPROFILE\.ssh\eduscheduler-key.pub -Raw | ForEach-Object { $_.Trim() }
$pubKey | Out-File -Encoding ASCII $env:TEMP\eduscheduler-key-clean.pub
aws ec2 import-key-pair `
  --key-name eduscheduler-key `
  --public-key-material fileb://$env:TEMP\eduscheduler-key-clean.pub `
  --region eu-north-1

# 4. Sprawdź czy klucz został zaimportowany
aws ec2 describe-key-pairs --key-names eduscheduler-key --region eu-north-1
```

### **✅ Weryfikacja:**

```powershell
# Sprawdź klucz w AWS
aws ec2 describe-key-pairs --region eu-north-1 --query 'KeyPairs[*].KeyName' --output table

# Powinno pokazać: eduscheduler-key
```

**✅ Jeśli widzisz `eduscheduler-key` - przejdź do następnego kroku!**

---

## 🖥️ Krok 2: Utwórz VM z Minikube

**Cel:** Utworzenie EC2 instance z automatyczną instalacją Minikube (Kubernetes).

### **Metoda A: Automatyczny Skrypt (Rekomendowane)**

```bash
# Użyj Git Bash lub WSL
cd aws
chmod +x create-vm-simple.sh
./create-vm-simple.sh eduscheduler-key
```

**Co robi skrypt:**
- ✅ Automatycznie znajduje najnowszą Debian/Ubuntu AMI
- ✅ Tworzy Security Group z otwartymi portami (22, 80, 8080)
- ✅ Tworzy EC2 instance (t3.medium)
- ✅ Instaluje Docker, kubectl, minikube (przez user data)
- ✅ Zwraca **Public IP** i **Instance ID**

**⏱️ Czas:** ~3-5 minut na automatyczną instalację po utworzeniu VM.

### **Metoda B: Ręcznie (AWS Console)**

1. **Wejdź do AWS Console:**
   - https://console.aws.amazon.com/ec2/
   - Region: **eu-north-1** (Stockholm)

2. **Launch Instance:**
   - Kliknij **"Launch Instance"**
   - Name: `eduscheduler-minikube`

3. **Wybierz AMI:**
   - Search: `ubuntu 22.04` lub `debian 12`
   - Wybierz najnowszą wersję

4. **Instance Type:**
   - Wybierz: **t3.medium** (2 vCPU, 4GB RAM) - rekomendowane
   - LUB **t3.small** (2 vCPU, 2GB RAM) - tańsze (~$12/miesiąc)

5. **Key Pair:**
   - Wybierz: **eduscheduler-key** (utworzony w Kroku 1)

6. **Network Settings:**
   - **Security Group:** Create new security group
   - **Inbound Rules:**
     - SSH (22) - Anywhere (0.0.0.0/0)
     - HTTP (80) - Anywhere (0.0.0.0/0)
     - Custom TCP (8080) - Anywhere (0.0.0.0/0)

7. **Advanced Details → User Data:**
   - Kliknij **"Advanced Details"** (u dołu strony, przed "Summary")
   - Przewiń w dół do sekcji **"User data"**
   - Kliknij w pole tekstowe "User data" (duże pole tekstowe)
   - Otwórz plik `aws/ec2-userdata.sh` w edytorze tekstu
   - Skopiuj **całą zawartość** pliku (Ctrl+A, Ctrl+C)
   - Wklej do pola "User data" (Ctrl+V)
   
   **⚠️ WAŻNE:** To pole automatycznie instaluje Docker, kubectl, minikube na VM!

8. **Launch Instance:**
   - Kliknij **"Launch Instance"**
   - Skopiuj **Instance ID** i **Public IP**

### **✅ Weryfikacja:**

```bash
# 1. Ustaw właściwe uprawnienia dla klucza prywatnego (SSH wymaga 400)
chmod 400 ~/.ssh/eduscheduler-key

# 2. Poczekaj 3-5 minut, potem połącz się z VM
# Dla Debian (domyślny użytkownik: admin) - spróbuj najpierw:
ssh -i ~/.ssh/eduscheduler-key admin@PUBLIC_IP
# LUB dla Ubuntu (domyślny użytkownik: ubuntu):
ssh -i ~/.ssh/eduscheduler-key ubuntu@PUBLIC_IP

# 3. Na VM, sprawdź logi instalacji (jeśli coś nie działa):
sudo cat /var/log/user-data-install.log

# 4. Sprawdź czy minikube działa:
minikube status
# Powinno pokazać: running

# 5. Sprawdź Kubernetes nodes
kubectl get nodes
# Powinien być 1 node: minikube

# 6. Sprawdź cluster info
kubectl cluster-info
```

**✅ Jeśli `kubectl get nodes` pokazuje node - przejdź do następnego kroku!**

---

## 🔐 Krok 3: Skonfiguruj ECR Authentication

**Cel:** Konfiguracja Kubernetes secret do pullowania obrazów z ECR.

### **Na VM (SSH):**

```bash
# 1. Połącz się z VM
ssh -i ~/.ssh/eduscheduler-key ubuntu@PUBLIC_IP

# 2. Skonfiguruj AWS credentials (użyj tych samych co lokalnie)
aws configure
# Wpisz:
# - AWS Access Key ID: [twoj-access-key]
# - AWS Secret Access Key: [twoj-secret-key]
# - Default region: eu-north-1
# - Default output format: json

# 3. Sprawdź czy AWS działa
aws sts get-caller-identity

# 4. Sklonuj repozytorium (wszystkie branche)
git clone --all <twoje-repo-url>
cd Class-Scheduler-App

# LUB sklonuj tylko main (jeśli wystarczy):
# git clone <twoje-repo-url>
# cd Class-Scheduler-App

# LUB skopiuj pliki przez scp z lokalnego komputera:
# (z lokalnego komputera, w PowerShell/Git Bash):
# cd C:\Users\Acer\Desktop\Class-Scheduler-App
# scp -i ~/.ssh/eduscheduler-key -r aws/ admin@VM_PUBLIC_IP:~/Class-Scheduler-App/
# scp -i ~/.ssh/eduscheduler-key -r k8s/ admin@VM_PUBLIC_IP:~/Class-Scheduler-App/

# 5. Utwórz Kubernetes secret dla ECR
chmod +x aws/create-ecr-secret.sh
./aws/create-ecr-secret.sh
```

**Co robi skrypt:**
- ✅ Pobiera ECR login token
- ✅ Tworzy Kubernetes secret `ecr-registry-secret`
- ✅ Secret jest używany przez pody do pullowania obrazów z ECR

### **✅ Weryfikacja:**

```bash
# Sprawdź czy secret został utworzony
kubectl get secret ecr-registry-secret

# Powinno pokazać:
# NAME                  TYPE                             DATA   AGE
# ecr-registry-secret   kubernetes.io/dockerconfigjson   1      ...
```

**⚠️ UWAGA:** Secret wygasa po 12 godzinach. Jeśli pody nie mogą pullować obrazów, uruchom `./aws/create-ecr-secret.sh` ponownie.

**✅ Jeśli secret istnieje - przejdź do następnego kroku!**

---

## 🚀 Krok 4: Wdróż Wszystko na Kubernetes

**Cel:** Wdrożenie wszystkich podów, serwisów, network policies i HPA.

### **Przygotowanie (na VM):**

```bash
# Jeśli jeszcze nie sklonowałeś repo (wszystkie branche):
git clone --all <twoje-repo-url>
cd Class-Scheduler-App

# LUB tylko main branch:
# git clone <twoje-repo-url>
# cd Class-Scheduler-App

# LUB jeśli masz tylko k8s/ folder:
# Upewnij się że masz folder k8s/ z wszystkimi manifestami
```

### **Deployment (Automatyczny - Rekomendowane):**

```bash
# Na VM
cd Class-Scheduler-App
chmod +x aws/deploy-all.sh
./aws/deploy-all.sh
```

**Co robi skrypt:**
1. ✅ **Database:** StatefulSet (PostgreSQL) + Service + PVC
2. ✅ **Database Schema:** Init Job (tworzy schemat bazy)
3. ✅ **Network Policies:** Izolacja sieci (default deny, allow rules)
4. ✅ **Backend:** Deployment (Spring Boot) + Service + HPA
5. ✅ **Frontend:** Deployment (React) + Service + HPA

**⏱️ Czas:** ~2-5 minut na deployment wszystkich komponentów.

### **Deployment (Ręcznie - Krok po Kroku):**

Jeśli chcesz wdrożyć ręcznie:

```bash
# 1. Database Secret (credentials)
kubectl apply -f k8s/database/secret.yaml

# 2. Database Service
kubectl apply -f k8s/database/service.yaml

# 3. Database Schema ConfigMap
kubectl apply -f k8s/database/schema-configmap.yaml

# 4. Database StatefulSet
kubectl apply -f k8s/database/statefulset.yaml

# 5. Poczekaj aż DB będzie ready
kubectl wait --for=condition=ready pod -l app=postgres --timeout=120s

# 6. Database Init Job (tworzy schemat)
kubectl apply -f k8s/database/init-job.yaml
kubectl wait --for=condition=complete job/eduscheduler-db-init --timeout=180s

# 7. Network Policies
kubectl apply -f k8s/network/default-deny-all.yaml
kubectl apply -f k8s/network/allow-frontend-to-backend.yaml
kubectl apply -f k8s/network/allow-backend-to-postgres.yaml
kubectl apply -f k8s/network/allow-backend-egress-dns.yaml

# 8. Backend Service
kubectl apply -f k8s/backend/service.yaml

# 9. Backend Deployment
kubectl apply -f k8s/backend/deployment.yaml

# 10. Backend HPA
kubectl apply -f k8s/backend/hpa.yaml

# 11. Frontend Service
kubectl apply -f k8s/frontend/service.yaml

# 12. Frontend Deployment
kubectl apply -f k8s/frontend/deployment.yaml

# 13. Frontend HPA
kubectl apply -f k8s/frontend/hpa.yaml
```

### **✅ Weryfikacja:**

```bash
# Sprawdź wszystkie pody
kubectl get pods
# Powinieneś zobaczyć:
# NAME                                   READY   STATUS    RESTARTS   AGE
# postgres-0                             1/1     Running   0          ...
# eduscheduler-backend-xxxxx             1/1     Running   0          ...
# eduscheduler-frontend-xxxxx            1/1     Running   0          ...

# Sprawdź wszystkie serwisy
kubectl get services
# Powinieneś zobaczyć:
# NAME                      TYPE        CLUSTER-IP      EXTERNAL-IP   PORT(S)
# postgres                  ClusterIP   10.96.x.x       <none>        5432/TCP
# eduscheduler-backend      ClusterIP   10.96.x.x       <none>        8080/TCP
# eduscheduler-frontend     NodePort    10.96.x.x       <none>        80:30080/TCP

# Sprawdź wszystkie deploymenty
kubectl get deployments

# Sprawdź StatefulSet
kubectl get statefulset

# Sprawdź HPA
kubectl get hpa
```

**✅ Jeśli wszystkie pody są `Running` - przejdź do następnego kroku!**

---

## ✅ Krok 5: Sprawdź Działanie

**Cel:** Weryfikacja że aplikacja działa poprawnie.

### **Sprawdź Logi:**

```bash
# Backend logs
kubectl logs -f deployment/eduscheduler-backend

# Frontend logs
kubectl logs -f deployment/eduscheduler-frontend

# Database logs
kubectl logs -f statefulset/postgres
```

### **Sprawdź URL:**

```bash
# Frontend URL (NodePort)
minikube service eduscheduler-frontend --url
# Zwróci coś jak: http://VM_PUBLIC_IP:30080

# Backend (port-forward - opcjonalnie)
kubectl port-forward service/eduscheduler-backend 8080:8080
# Wtedy backend będzie dostępny na: http://localhost:8080
```

### **Przetestuj Aplikację:**

1. **Otwórz przeglądarkę:**
   - URL: `http://VM_PUBLIC_IP:30080`
   - Powinna załadować się strona główna aplikacji

2. **Sprawdź Backend API:**
   ```bash
   # Na VM
   curl http://localhost:8080/actuator/health
   # Powinno zwrócić: {"status":"UP"}
   ```

### **Przydatne Komendy:**

```bash
# Zobacz wszystkie resources
kubectl get all

# Szczegóły poda
kubectl describe pod <pod-name>

# Szczegóły serwisu
kubectl describe service <service-name>

# Events
kubectl get events --sort-by='.lastTimestamp'

# Shell do poda
kubectl exec -it <pod-name> -- /bin/bash

# Restart deployment
kubectl rollout restart deployment/eduscheduler-backend

# Historia rollout
kubectl rollout history deployment/eduscheduler-backend
```

---

## 🐛 Troubleshooting

### **Problem: Key Pair nie działa**

```powershell
# Sprawdź czy klucz istnieje lokalnie
Test-Path $env:USERPROFILE\.ssh\eduscheduler-key

# Sprawdź czy klucz istnieje w AWS
aws ec2 describe-key-pairs --key-names eduscheduler-key --region eu-north-1

# Ustaw właściwe uprawnienia (w Git Bash/WSL)
chmod 400 ~/.ssh/eduscheduler-key
```

### **Problem: Nie mogę połączyć się z VM (SSH)**

```bash
# Sprawdź czy VM jest running
aws ec2 describe-instances --instance-ids i-xxxxx --region eu-north-1 --query 'Reservations[0].Instances[0].State.Name'

# Sprawdź Security Group
aws ec2 describe-instances --instance-ids i-xxxxx --region eu-north-1 --query 'Reservations[0].Instances[0].SecurityGroups'

# Sprawdź czy port 22 jest otwarty
aws ec2 describe-security-groups --group-ids sg-xxxxx --region eu-north-1 --query 'SecurityGroups[0].IpPermissions'
```

### **Problem: Minikube nie działa**

```bash
# Na VM - sprawdź status
minikube status

# Jeśli nie działa, uruchom:
minikube start --driver=docker
minikube addons enable metrics-server

# Sprawdź logi
minikube logs
```

### **Problem: Pody nie startują (ImagePullBackOff)**

```bash
# Sprawdź czy ECR secret istnieje
kubectl get secret ecr-registry-secret

# Jeśli nie istnieje, utwórz ponownie
./aws/create-ecr-secret.sh

# Sprawdź szczegóły poda
kubectl describe pod <pod-name>

# Sprawdź czy obrazy istnieją w ECR
aws ecr list-images --repository-name eduscheduler-backend --region eu-north-1
aws ecr list-images --repository-name eduscheduler-frontend --region eu-north-1
```

### **Problem: Database nie startuje**

```bash
# Sprawdź logi
kubectl logs statefulset/postgres

# Sprawdź PVC (Persistent Volume Claim)
kubectl get pvc

# Sprawdź szczegóły StatefulSet
kubectl describe statefulset postgres

# Sprawdź secret
kubectl get secret postgres-secret
```

### **Problem: Backend nie może połączyć się z Database**

```bash
# Sprawdź czy database jest ready
kubectl get pod postgres-0

# Sprawdź czy Service istnieje
kubectl get service postgres

# Sprawdź backend logs
kubectl logs deployment/eduscheduler-backend

# Test connection z poda backend
kubectl exec -it deployment/eduscheduler-backend -- curl postgres:5432
```

### **Problem: Network Policies blokują ruch**

```bash
# Tymczasowo wyłącz network policies (dla debugowania)
kubectl delete networkpolicy --all

# Potem włącz ponownie
kubectl apply -f k8s/network/
```

### **Problem: Aplikacja nie działa (500 errors)**

```bash
# Sprawdź wszystkie logi
kubectl logs deployment/eduscheduler-backend --tail=100
kubectl logs deployment/eduscheduler-frontend --tail=100

# Sprawdź events
kubectl get events --sort-by='.lastTimestamp' | tail -20

# Sprawdź czy wszystkie pody są ready
kubectl get pods -o wide
```

---

## 📊 Podsumowanie

Po wykonaniu wszystkich kroków masz:

- ✅ **VM na AWS EC2** z Minikube (Kubernetes)
- ✅ **Database:** PostgreSQL jako StatefulSet (z PVC)
- ✅ **Backend:** Spring Boot jako Deployment (z HPA)
- ✅ **Frontend:** React jako Deployment (z HPA)
- ✅ **Services:** 3 serwisy (postgres, backend, frontend)
- ✅ **Network Policies:** Izolacja sieci
- ✅ **Persistent Storage:** Dane przetrwają restart
- ✅ **Auto-scaling:** HPA dla frontend i backend

## 🌐 Dostęp do Aplikacji

- **Frontend:** `http://VM_PUBLIC_IP:30080`
- **Backend:** `http://VM_PUBLIC_IP:8080` (lub przez port-forward)

## 💰 Koszt

- **t3.medium:** ~$24/miesiąc (~$0.034/h)
- **t3.small:** ~$12/miesiąc (~$0.017/h) - minimum

**Pamiętaj:** Zatrzymaj VM gdy nie używasz (`aws ec2 stop-instances --instance-ids i-xxxxx`)

---

## 🎉 Gotowe!

Masz pełny Kubernetes cluster z wszystkimi podami, serwisami i konfiguracją!

**Potrzebujesz pomocy?** Sprawdź:
- `aws/DEPLOY-K8S.md` - szczegółowy przewodnik Kubernetes
- `aws/CREATE-KEY-PAIR.md` - szczegóły key pair
- `aws/NAJLATWIEJSZA-METODA.md` - alternatywne metody


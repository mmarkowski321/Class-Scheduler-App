Minikube quick start (frontend, backend, postgres)

Prereqs:
- kubectl, minikube
- container images available (replace ghcr.io/your-org/* in manifests)

Steps:
1) Start minikube:
   minikube start

2) Create DB secret (adjust password before applying):
   kubectl apply -f k8s/database/secret.example.yaml

3) Deploy Postgres (Service + StatefulSet):
   kubectl apply -f k8s/database/service.yaml
   kubectl apply -f k8s/database/statefulset.yaml

4) Load DB schema:
   - Put the current SQL into ConfigMap (optional editing of schema-configmap.yaml to include full file)
   kubectl apply -f k8s/database/schema-configmap.yaml
   kubectl apply -f k8s/database/init-job.yaml
   kubectl wait --for=condition=complete job/eduscheduler-db-init --timeout=180s

5) Deploy backend:
   kubectl apply -f k8s/backend/service.yaml
   kubectl apply -f k8s/backend/deployment.yaml

6) Deploy frontend:
   kubectl apply -f k8s/frontend/service.yaml
   kubectl apply -f k8s/frontend/deployment.yaml

7) Open app:
   minikube service eduscheduler-frontend --url

Notes:
- Backend reads DB creds from Secret `postgres-secret` and connects to `postgres:5432/eduscheduler`.
- For production, switch frontend Service to LoadBalancer/Ingress and add TLS.
- Replace images in deployments with your registry (e.g., GCR/Artifact Registry). Use imagePullSecrets if private.



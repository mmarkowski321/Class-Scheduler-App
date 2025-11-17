#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "$SCRIPT_DIR/../k8s" ]; then
  K8S_DIR="$SCRIPT_DIR/../k8s"
else
  K8S_DIR="./k8s"
fi

echo "Deploying EduScheduler to Kubernetes"
echo ""

echo "Deploying database..."
kubectl apply -f $K8S_DIR/database/secret.yaml
kubectl apply -f $K8S_DIR/database/service.yaml

# Create ConfigMap from schema SQL file
SCHEMA_FILE="$SCRIPT_DIR/../database/backend/schema-postgres.sql"
if [ ! -f "$SCHEMA_FILE" ]; then
  SCHEMA_FILE="./database/backend/schema-postgres.sql"
fi
if [ -f "$SCHEMA_FILE" ]; then
  echo "Creating ConfigMap from $SCHEMA_FILE..."
  kubectl create configmap eduscheduler-schema \
    --from-file=schema.sql="$SCHEMA_FILE" \
    --dry-run=client -o yaml | kubectl apply -f -
else
  echo "WARNING: Schema file not found at $SCHEMA_FILE"
  echo "Using existing ConfigMap if available..."
fi

echo "Checking storage classes..."
kubectl get storageclass

echo "Applying StatefulSet..."
kubectl apply -f $K8S_DIR/database/statefulset.yaml

echo "Waiting for postgres to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres --timeout=120s || true

echo "Initializing database schema..."
kubectl apply -f $K8S_DIR/database/init-job.yaml
kubectl wait --for=condition=complete job/eduscheduler-db-init --timeout=180s || true

echo "Applying network policies..."
kubectl apply -f $K8S_DIR/network/default-deny-all.yaml
kubectl apply -f $K8S_DIR/network/allow-frontend-to-backend.yaml
kubectl apply -f $K8S_DIR/network/allow-backend-to-postgres.yaml
kubectl apply -f $K8S_DIR/network/allow-backend-egress-dns.yaml

echo "Deploying backend..."
if ! kubectl get secret backend-secrets > /dev/null 2>&1; then
  echo "WARNING: backend-secrets secret not found!"
  echo "Create it manually using:"
  echo "  kubectl create secret generic backend-secrets \\"
  echo "    --from-literal=SPRING_MAIL_PASSWORD=\"your-email-password\""
  echo ""
fi
kubectl apply -f $K8S_DIR/backend/service.yaml
kubectl apply -f $K8S_DIR/backend/deployment.yaml
kubectl apply -f $K8S_DIR/backend/hpa.yaml

echo "Deploying frontend..."
kubectl apply -f $K8S_DIR/frontend/service.yaml
kubectl apply -f $K8S_DIR/frontend/deployment.yaml
kubectl apply -f $K8S_DIR/frontend/hpa.yaml

echo ""
echo "Deployment Complete!"
echo ""
echo "Check status:"
echo "  kubectl get pods"
echo "  kubectl get services"
echo ""
echo "To access the application:"
echo "  1. Set up external access using kubectl port-forward:"
echo "     cd aws && ./setup-external-access.sh"
echo ""
echo "     This sets up port forwarding from EC2 port 30080 to the service"
echo ""
echo "  2. Get your EC2 public IP:"
echo "     curl -s http://169.254.169.254/latest/meta-data/public-ipv4"
echo ""
echo "  3. Access frontend via:"
echo "     http://<EC2_PUBLIC_IP>:30080"
echo "     OR from EC2 VM: http://localhost:30080"
echo ""
echo "  4. Make sure Security Group allows inbound traffic on port 30080"
echo ""
echo "Note: kubectl port-forward must remain running for external access."
echo "     To stop: ./stop-port-forward.sh"
echo ""


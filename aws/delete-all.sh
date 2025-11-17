#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "$SCRIPT_DIR/../k8s" ]; then
  K8S_DIR="$SCRIPT_DIR/../k8s"
else
  K8S_DIR="./k8s"
fi

echo "Deleting EduScheduler from Kubernetes"
echo ""

echo "Deleting deployments..."
kubectl delete deployment eduscheduler-backend --ignore-not-found=true
kubectl delete deployment eduscheduler-frontend --ignore-not-found=true

echo "Deleting services..."
kubectl delete service eduscheduler-backend --ignore-not-found=true
kubectl delete service eduscheduler-frontend --ignore-not-found=true
kubectl delete service postgres --ignore-not-found=true

echo "Deleting HPA..."
kubectl delete hpa eduscheduler-backend --ignore-not-found=true
kubectl delete hpa eduscheduler-frontend --ignore-not-found=true

echo "Deleting network policies..."
kubectl delete networkpolicy default-deny-all --ignore-not-found=true
kubectl delete networkpolicy allow-frontend-to-backend --ignore-not-found=true
kubectl delete networkpolicy allow-backend-to-postgres --ignore-not-found=true
kubectl delete networkpolicy allow-backend-egress-dns --ignore-not-found=true

echo "Deleting database resources..."
kubectl delete job eduscheduler-db-init --ignore-not-found=true
kubectl delete statefulset postgres --ignore-not-found=true

echo "Waiting for pods to terminate..."
sleep 5

echo "Deleting PVCs..."
kubectl delete pvc pgdata-postgres-0 --ignore-not-found=true

echo "Deleting configmaps..."
kubectl delete configmap eduscheduler-schema --ignore-not-found=true

echo "Deleting secrets (keeping postgres-secret and ecr-registry-secret)..."
echo "Note: postgres-secret and ecr-registry-secret are kept for reuse"

echo ""
echo "Cleanup Complete!"
echo ""
echo "To redeploy, run:"
echo "  cd ~/Class-Scheduler-App/aws"
echo "  ./deploy-all.sh"
echo ""


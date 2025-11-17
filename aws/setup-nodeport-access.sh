#!/bin/bash
set -e

echo "Setting up NodePort access for Minikube..."
echo ""

# Get Minikube IP
MINIKUBE_IP=$(minikube ip)
if [ -z "$MINIKUBE_IP" ]; then
    echo "Error: Could not get Minikube IP. Is Minikube running?"
    exit 1
fi

echo "Minikube IP: $MINIKUBE_IP"
echo ""

# Stop minikube tunnel (not needed for NodePort)
echo "Stopping minikube tunnel (not needed for NodePort)..."
pkill -f "minikube tunnel" || echo "No tunnel process found"
echo ""

# Apply NodePort service
echo "Applying NodePort service..."
kubectl apply -f ../k8s/frontend/service.yaml

# Wait for service
sleep 2

echo ""
echo "Service status:"
kubectl get svc eduscheduler-frontend

echo ""
echo "=== Access Information ==="
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
echo "Public IP: $PUBLIC_IP"
echo ""
echo "Access options:"
echo "1. From EC2 VM (localhost):"
echo "   curl http://$MINIKUBE_IP:30080"
echo ""
echo "2. From anywhere (via EC2 public IP):"
echo "   http://$PUBLIC_IP:30080"
echo ""
echo "3. Using kubectl port-forward (temporary, for testing):"
echo "   kubectl port-forward svc/eduscheduler-frontend 8080:80"
echo "   Then access: http://localhost:8080"
echo ""
echo "Note: Make sure Security Group allows port 30080"
echo ""


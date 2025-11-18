#!/bin/bash
set -e

echo "Setting up Let's Encrypt certificate for eduscheduler.eu..."
echo ""

echo "1. Installing cert-manager..."
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

echo ""
echo "2. Waiting for cert-manager to be ready..."
kubectl wait --for=condition=ready pod \
  -l app.kubernetes.io/instance=cert-manager \
  -n cert-manager \
  --timeout=300s || echo "cert-manager may still be starting..."

echo ""
read -p "Enter your email for Let's Encrypt notifications: " EMAIL
if [ -z "$EMAIL" ]; then
  echo "ERROR: Email is required"
  exit 1
fi

echo ""
echo "3. Creating ClusterIssuer..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "$SCRIPT_DIR/../k8s" ]; then
  K8S_DIR="$SCRIPT_DIR/../k8s"
else
  K8S_DIR="./k8s"
fi

# Create ClusterIssuer with user's email
cat > /tmp/cluster-issuer.yaml <<EOF
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: $EMAIL
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
      - http01:
          ingress:
            class: nginx
EOF

kubectl apply -f /tmp/cluster-issuer.yaml
rm -f /tmp/cluster-issuer.yaml

echo ""
echo "4. Setting up external access for Ingress..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/setup-ingress-access.sh" ]; then
  bash $SCRIPT_DIR/setup-ingress-access.sh
else
  echo "WARNING: setup-ingress-access.sh not found. Setting up manually..."
  
  # Kill existing port-forward
  sudo pkill -f "kubectl port-forward.*ingress-nginx-controller" || pkill -f "kubectl port-forward.*ingress-nginx-controller" || true
  sleep 2
  
  # Get Ingress service
  INGRESS_SVC=$(kubectl get svc -n ingress-nginx -l app.kubernetes.io/component=controller -o name | head -1)
  HTTP_PORT=$(kubectl get $INGRESS_SVC -n ingress-nginx -o jsonpath='{.spec.ports[?(@.name=="http")].port}')
  HTTPS_PORT=$(kubectl get $INGRESS_SVC -n ingress-nginx -o jsonpath='{.spec.ports[?(@.name=="https")].port}')
  
  # Start port-forward with sudo (required for ports < 1024)
  nohup sudo kubectl port-forward --address 0.0.0.0 $INGRESS_SVC -n ingress-nginx 80:$HTTP_PORT 443:$HTTPS_PORT > /tmp/ingress-port-forward.log 2>&1 &
  sleep 5
fi

echo ""
echo "5. Updating Ingress to use Let's Encrypt..."
kubectl apply -f $K8S_DIR/ingress/ingress-letsencrypt.yaml

echo ""
echo "6. Waiting for certificate to be issued..."
echo "This may take 1-5 minutes..."
kubectl wait --for=condition=ready certificate eduscheduler-tls-secret \
  --timeout=600s || echo "Certificate may still be issuing..."

echo ""
echo "=== Let's Encrypt Setup Complete ==="
echo ""
echo "Check certificate status:"
echo "  kubectl describe certificate eduscheduler-tls-secret"
echo ""
echo "Access the application:"
echo "  HTTPS: https://eduscheduler.eu"
echo "  HTTPS: https://www.eduscheduler.eu"
echo ""
echo "IMPORTANT: Make sure DNS is configured correctly:"
echo "  dig eduscheduler.eu"
echo "  dig www.eduscheduler.eu"
echo "Both should resolve to your EC2 public IP"
echo ""


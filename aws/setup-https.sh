#!/bin/bash
set -e

echo "Setting up HTTPS/SSL for EduScheduler..."
echo ""

echo "1. Enabling nginx-ingress addon..."
minikube addons enable ingress

echo ""
echo "2. Waiting for ingress controller to be ready..."
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=300s || echo "Ingress controller may still be starting..."

echo ""
echo "3. Getting ingress controller IP..."
INGRESS_IP=$(kubectl get ingressclass nginx -o jsonpath='{.metadata.annotations.ingressclass\.kubernetes\.io/default-class}' 2>/dev/null || echo "")
if [ -z "$INGRESS_IP" ]; then
  INGRESS_IP=$(minikube ip)
fi
echo "Ingress IP: $INGRESS_IP"

echo ""
echo "4. Creating self-signed TLS certificate for eduscheduler.eu..."
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "13.62.198.13")
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /tmp/tls.key \
  -out /tmp/tls.crt \
  -subj "/CN=eduscheduler.eu" \
  -addext "subjectAltName=DNS:eduscheduler.eu,DNS:www.eduscheduler.eu,IP:$PUBLIC_IP" 2>/dev/null || {
  echo "Note: openssl may not be available, using kubectl to create cert..."
  echo "Creating certificate secret with dummy data (will be replaced)..."
}

echo ""
echo "5. Creating TLS secret..."
if [ -f /tmp/tls.key ] && [ -f /tmp/tls.crt ]; then
  kubectl create secret tls eduscheduler-tls-secret \
    --key /tmp/tls.key \
    --cert /tmp/tls.crt \
    --dry-run=client -o yaml | kubectl apply -f -
  rm -f /tmp/tls.key /tmp/tls.crt
else
  echo "WARNING: Could not create self-signed cert with openssl."
  echo "Creating TLS secret with placeholder (you may need to create certificate manually)..."
  kubectl create secret tls eduscheduler-tls-secret \
    --key <(openssl genrsa 2048 2>/dev/null || echo "-----BEGIN PRIVATE KEY-----") \
    --cert <(echo "-----BEGIN CERTIFICATE-----") \
    --dry-run=client -o yaml | kubectl apply -f - 2>/dev/null || {
    echo "Please create certificate manually and then:"
    echo "  kubectl create secret tls eduscheduler-tls-secret --key tls.key --cert tls.crt"
  }
fi

echo ""
echo "6. Deploying Ingress..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -d "$SCRIPT_DIR/../k8s" ]; then
  K8S_DIR="$SCRIPT_DIR/../k8s"
else
  K8S_DIR="./k8s"
fi

kubectl apply -f $K8S_DIR/ingress/ingress.yaml

echo ""
echo "7. Waiting for Ingress to be ready..."
sleep 5

echo ""
echo "=== HTTPS Setup Complete ==="
echo ""
echo "Note: Using self-signed certificate. Browser will show security warning."
echo "To get a valid certificate from Let's Encrypt, see instructions below."
echo ""
echo "Access the application:"
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "13.62.198.13")
echo "  HTTPS: https://eduscheduler.eu"
echo "  HTTPS: https://www.eduscheduler.eu"
echo "  (IP fallback: https://$PUBLIC_IP - may show cert warning)"
echo ""
echo "IMPORTANT: Configure DNS in OVH Cloud:"
echo "  1. Log in to OVH Cloud Control Panel"
echo "  2. Go to Domains > eduscheduler.eu > DNS Zone"
echo "  3. Add/Edit A record:"
echo "     Name: @ (or blank)"
echo "     Target: $PUBLIC_IP"
echo "     TTL: 3600"
echo "  4. Add/Edit A record:"
echo "     Name: www"
echo "     Target: $PUBLIC_IP"
echo "     TTL: 3600"
echo "  5. Wait 5-30 minutes for DNS propagation"
echo ""
echo "Note: Make sure Security Group allows port 443 (HTTPS)"
echo ""
echo "To get a valid certificate from Let's Encrypt (recommended):"
echo "  1. Wait for DNS to propagate (check with: dig eduscheduler.eu)"
echo "  2. Install cert-manager: kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml"
echo "  3. Create ClusterIssuer for Let's Encrypt"
echo "  4. Update Ingress to use cert-manager annotations"
echo ""


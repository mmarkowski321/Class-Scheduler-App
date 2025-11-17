#!/bin/bash
set -e

IP_ADDRESS=${1:-"13.62.198.13"}

echo "Generating self-signed TLS certificate for IP: $IP_ADDRESS"
echo ""

# Install openssl if not available
if ! command -v openssl &> /dev/null; then
  echo "Installing openssl..."
  sudo apt-get update -y
  sudo apt-get install -y openssl
fi

echo "Creating certificate..."
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /tmp/tls.key \
  -out /tmp/tls.crt \
  -subj "/CN=$IP_ADDRESS" \
  -addext "subjectAltName=IP:$IP_ADDRESS,DNS:$IP_ADDRESS"

echo ""
echo "Creating Kubernetes secret..."
kubectl create secret tls eduscheduler-tls-secret \
  --key /tmp/tls.key \
  --cert /tmp/tls.crt \
  --dry-run=client -o yaml | kubectl apply -f -

echo ""
echo "Cleaning up temporary files..."
rm -f /tmp/tls.key /tmp/tls.crt

echo ""
echo "Certificate created successfully!"
echo "Secret: eduscheduler-tls-secret"
echo ""


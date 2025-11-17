#!/bin/bash
set -e

DOMAIN=${1:-"eduscheduler.eu"}
IP_ADDRESS=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "")

echo "Generating self-signed TLS certificate for domain: $DOMAIN"
if [ -n "$IP_ADDRESS" ]; then
  echo "Including IP address: $IP_ADDRESS"
fi
echo ""

# Install openssl if not available
if ! command -v openssl &> /dev/null; then
  echo "Installing openssl..."
  sudo apt-get update -y
  sudo apt-get install -y openssl
fi

echo "Creating certificate..."
if [ -n "$IP_ADDRESS" ]; then
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /tmp/tls.key \
    -out /tmp/tls.crt \
    -subj "/CN=$DOMAIN" \
    -addext "subjectAltName=DNS:$DOMAIN,DNS:www.$DOMAIN,IP:$IP_ADDRESS"
else
  openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /tmp/tls.key \
    -out /tmp/tls.crt \
    -subj "/CN=$DOMAIN" \
    -addext "subjectAltName=DNS:$DOMAIN,DNS:www.$DOMAIN"
fi

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


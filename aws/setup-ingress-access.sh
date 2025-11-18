#!/bin/bash
set -e

echo "Setting up external access for Ingress (port 80/443)..."
echo ""

# Kill existing port-forward processes
echo "1. Stopping existing port-forward processes..."
pkill -f "kubectl port-forward.*ingress-nginx-controller" || echo "No existing port-forward found"
sleep 2

# Get Ingress service port
INGRESS_SVC=$(kubectl get svc -n ingress-nginx -l app.kubernetes.io/component=controller -o name | head -1)
if [ -z "$INGRESS_SVC" ]; then
  echo "ERROR: Ingress controller service not found!"
  echo "Make sure nginx-ingress addon is enabled: minikube addons enable ingress"
  exit 1
fi

HTTP_PORT=$(kubectl get $INGRESS_SVC -n ingress-nginx -o jsonpath='{.spec.ports[?(@.name=="http")].port}')
HTTPS_PORT=$(kubectl get $INGRESS_SVC -n ingress-nginx -o jsonpath='{.spec.ports[?(@.name=="https")].port}')

echo "2. Starting port-forward for Ingress..."
echo "   HTTP port: $HTTP_PORT -> 80"
echo "   HTTPS port: $HTTPS_PORT -> 443"
echo ""

# Start port-forward in background, binding to all interfaces (0.0.0.0)
nohup kubectl port-forward --address 0.0.0.0 $INGRESS_SVC -n ingress-nginx 80:$HTTP_PORT 443:$HTTPS_PORT > /tmp/ingress-port-forward.log 2>&1 &
PORT_FORWARD_PID=$!

echo "Port-forward started (PID: $PORT_FORWARD_PID)"
echo "Logs: /tmp/ingress-port-forward.log"
echo ""

# Wait a moment for port-forward to establish
sleep 3

# Check if port-forward is running
if ps -p $PORT_FORWARD_PID > /dev/null; then
  echo "✓ Port-forward is running"
else
  echo "✗ Port-forward failed to start. Check logs:"
  tail -20 /tmp/ingress-port-forward.log
  exit 1
fi

echo ""
echo "3. Testing local access..."
if curl -I http://localhost/.well-known/acme-challenge/test > /dev/null 2>&1; then
  echo "✓ Local access works"
else
  echo "⚠ Local access test failed (may be normal if Ingress not ready)"
fi

echo ""
echo "4. Getting public IP..."
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4 2>/dev/null || echo "unknown")
echo "Public IP: $PUBLIC_IP"

echo ""
echo "=== External Access Setup Complete ==="
echo ""
echo "Ingress is now accessible at:"
echo "  HTTP:  http://$PUBLIC_IP"
echo "  HTTPS: https://$PUBLIC_IP"
echo "  Domain: http://eduscheduler.eu"
echo "  Domain: https://eduscheduler.eu"
echo ""
echo "IMPORTANT: Make sure Security Group allows ports 80 and 443"
echo ""
echo "To stop port-forward:"
echo "  pkill -f 'kubectl port-forward.*ingress-nginx-controller'"
echo ""
echo "To check status:"
echo "  ps aux | grep 'kubectl port-forward.*ingress-nginx-controller'"
echo "  tail -f /tmp/ingress-port-forward.log"
echo ""


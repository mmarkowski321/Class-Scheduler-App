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

# Start port-forward in background with sudo -E (preserves environment, including KUBECONFIG)
echo "Starting port-forward with sudo -E (required for ports < 1024)..."
echo "Note: This requires sudo access and kubeconfig for root user"
echo ""

# Try with sudo -E first (preserves environment)
nohup sudo -E kubectl port-forward --address 0.0.0.0 $INGRESS_SVC -n ingress-nginx 80:$HTTP_PORT 443:$HTTPS_PORT > /tmp/ingress-port-forward.log 2>&1 &
PORT_FORWARD_PID=$!

echo "Port-forward started (PID: $PORT_FORWARD_PID)"
echo "Logs: /tmp/ingress-port-forward.log"
echo ""

# Wait a moment for port-forward to establish
sleep 3

# Check if port-forward is running (need to check with sudo ps or check process name)
sleep 2
if pgrep -f "kubectl port-forward.*ingress-nginx-controller" > /dev/null || sudo pgrep -f "kubectl port-forward.*ingress-nginx-controller" > /dev/null; then
  echo "✓ Port-forward is running"
else
  echo "✗ Port-forward failed to start. Check logs:"
  sudo tail -20 /tmp/ingress-port-forward.log
  echo ""
  echo "Note: Ports 80 and 443 require root privileges."
  echo "Trying alternative method with iptables..."
  
  # Get LoadBalancer IP or use NodePort
  LB_IP=$(kubectl get $INGRESS_SVC -n ingress-nginx -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
  if [ -z "$LB_IP" ]; then
    # Use ClusterIP if LoadBalancer doesn't have IP yet
    LB_IP=$(kubectl get $INGRESS_SVC -n ingress-nginx -o jsonpath='{.spec.clusterIP}')
  fi
  
  echo "Using Ingress service IP: $LB_IP"
  
  # Alternative: Use iptables to forward ports to ClusterIP
  # This requires sudo privileges but doesn't need kubectl from root
  sudo iptables -t nat -C PREROUTING -p tcp --dport 80 -j DNAT --to-destination $LB_IP:$HTTP_PORT 2>/dev/null || \
    sudo iptables -t nat -A PREROUTING -p tcp --dport 80 -j DNAT --to-destination $LB_IP:$HTTP_PORT
  sudo iptables -t nat -C PREROUTING -p tcp --dport 443 -j DNAT --to-destination $LB_IP:$HTTPS_PORT 2>/dev/null || \
    sudo iptables -t nat -A PREROUTING -p tcp --dport 443 -j DNAT --to-destination $LB_IP:$HTTPS_PORT
  
  echo "✓ iptables rules added for port forwarding"
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
echo "  sudo pkill -f 'kubectl port-forward.*ingress-nginx-controller'"
echo "  Or: pkill -f 'kubectl port-forward.*ingress-nginx-controller'"
echo ""
echo "To check status:"
echo "  sudo ps aux | grep 'kubectl port-forward.*ingress-nginx-controller'"
echo "  sudo tail -f /tmp/ingress-port-forward.log"
echo ""
echo "To remove iptables rules (if used):"
echo "  sudo iptables -t nat -D PREROUTING -p tcp --dport 80 -j DNAT --to-destination 127.0.0.1:$HTTP_PORT"
echo "  sudo iptables -t nat -D PREROUTING -p tcp --dport 443 -j DNAT --to-destination 127.0.0.1:$HTTPS_PORT"
echo ""


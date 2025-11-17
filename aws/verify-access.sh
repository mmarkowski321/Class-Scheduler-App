#!/bin/bash
set -e

echo "=== Verifying Application Access ==="
echo ""

# Get public IP
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
echo "EC2 Public IP: $PUBLIC_IP"
echo ""

# Check minikube tunnel
echo "1. Checking minikube tunnel..."
if pgrep -f "minikube tunnel" > /dev/null; then
    echo "   ✓ minikube tunnel is running"
    ps aux | grep "[m]inikube tunnel"
else
    echo "   ✗ minikube tunnel is NOT running"
    echo "   Start it with: ./start-minikube-tunnel.sh"
fi
echo ""

# Check service
echo "2. Checking frontend service..."
kubectl get svc eduscheduler-frontend
echo ""

# Check pods
echo "3. Checking frontend pod..."
kubectl get pods -l app=eduscheduler-frontend
echo ""

# Test localhost
echo "4. Testing localhost access..."
if curl -s -o /dev/null -w "%{http_code}" http://localhost:80 | grep -q "200\|301\|302"; then
    echo "   ✓ Service is accessible on localhost:80"
else
    echo "   ✗ Service is NOT accessible on localhost:80"
fi
echo ""

# Test via cluster IP
CLUSTER_IP=$(kubectl get svc eduscheduler-frontend -o jsonpath='{.spec.clusterIP}')
echo "5. Testing cluster IP ($CLUSTER_IP)..."
if kubectl run -it --rm curl-test --image=curlimages/curl:latest --restart=Never -- curl -s -o /dev/null -w "%{http_code}" http://${CLUSTER_IP}:80 | grep -q "200\|301\|302"; then
    echo "   ✓ Service is accessible via cluster IP"
else
    echo "   ✗ Service is NOT accessible via cluster IP"
fi
echo ""

# Check routes
echo "6. Checking routing table..."
ip route show | grep -E "docker|minikube" || echo "   (No specific routes found)"
echo ""

echo "=== Access Information ==="
echo "Local access:  http://localhost:80"
echo "Public access: http://${PUBLIC_IP}:80"
echo ""
echo "If localhost works but public IP doesn't:"
echo "  1. Check Security Group allows port 80 (TCP, 0.0.0.0/0)"
echo "  2. Verify minikube tunnel is running: ps aux | grep 'minikube tunnel'"
echo "  3. Check tunnel logs: tail -f /tmp/minikube-tunnel.log"
echo ""


#!/bin/bash
set -e

echo "Setting up external access using kubectl port-forward..."
echo ""

# Get public IP
PUBLIC_IP=$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4)
echo "EC2 Public IP: $PUBLIC_IP"
echo ""

# Check if port-forward is already running
if pgrep -f "kubectl port-forward.*eduscheduler-frontend.*30080" > /dev/null; then
    echo "Port-forward is already running. Stopping existing process..."
    pkill -f "kubectl port-forward.*eduscheduler-frontend.*30080" || true
    sleep 2
fi

echo "Starting kubectl port-forward on port 30080..."
echo "This will bind to 0.0.0.0 to allow external access"
echo ""

# Start port-forward in background, binding to all interfaces
nohup kubectl port-forward --address 0.0.0.0 svc/eduscheduler-frontend 30080:80 > /tmp/kubectl-port-forward.log 2>&1 &
PORT_FORWARD_PID=$!

echo "Port-forward started (PID: $PORT_FORWARD_PID)"
echo "Logs: /tmp/kubectl-port-forward.log"
echo ""

# Wait a moment for port-forward to start
sleep 3

# Test local access
echo "Testing local access..."
if curl -s -o /dev/null -w "%{http_code}" http://localhost:30080 | grep -q "200\|301\|302"; then
    echo "✓ Service is accessible on localhost:30080"
else
    echo "✗ Service is NOT accessible on localhost:30080"
    echo "Check logs: tail -f /tmp/kubectl-port-forward.log"
fi

echo ""
echo "=== Access Information ==="
echo "From EC2 VM:"
echo "  http://localhost:30080"
echo ""
echo "From anywhere (via EC2 public IP):"
echo "  http://$PUBLIC_IP:30080"
echo ""
echo "To stop port-forward:"
echo "  pkill -f 'kubectl port-forward.*eduscheduler-frontend.*30080'"
echo ""
echo "To check status:"
echo "  ps aux | grep 'kubectl port-forward'"
echo "  tail -f /tmp/kubectl-port-forward.log"
echo ""
echo "Make sure Security Group allows port 30080 (TCP, 0.0.0.0/0)"
echo ""


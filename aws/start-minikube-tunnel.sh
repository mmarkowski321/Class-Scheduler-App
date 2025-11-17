#!/bin/bash
set -e

echo "Starting minikube tunnel for LoadBalancer services..."
echo "This will run in the background and expose services on the host's IP"
echo ""

# Kill existing tunnel if running
pkill -f "minikube tunnel" || true
sleep 2

# Start tunnel in background
nohup minikube tunnel > /tmp/minikube-tunnel.log 2>&1 &
TUNNEL_PID=$!

echo "Minikube tunnel started (PID: $TUNNEL_PID)"
echo "Logs: /tmp/minikube-tunnel.log"
echo ""
echo "To stop the tunnel:"
echo "  pkill -f 'minikube tunnel'"
echo ""
echo "To check status:"
echo "  ps aux | grep 'minikube tunnel'"
echo "  tail -f /tmp/minikube-tunnel.log"
echo ""
echo "Waiting for services to get external IPs..."
sleep 5

kubectl get svc

echo ""
echo "Access the frontend at: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):80"
echo "Or check 'kubectl get svc' for the EXTERNAL-IP"


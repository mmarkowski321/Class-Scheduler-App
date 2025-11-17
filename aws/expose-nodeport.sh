#!/bin/bash
set -e

echo "Setting up port forwarding for Minikube NodePort services..."

# Get Minikube IP
MINIKUBE_IP=$(minikube ip)
if [ -z "$MINIKUBE_IP" ]; then
  echo "Error: Could not get Minikube IP. Is Minikube running?"
  exit 1
fi

echo "Minikube IP: $MINIKUBE_IP"
echo ""

# Forward port 30080 (frontend NodePort) from host to Minikube
echo "Setting up iptables rule for port 30080 (frontend)..."
sudo iptables -t nat -C PREROUTING -p tcp --dport 30080 -j DNAT --to-destination "$MINIKUBE_IP:30080" 2>/dev/null || \
sudo iptables -t nat -A PREROUTING -p tcp --dport 30080 -j DNAT --to-destination "$MINIKUBE_IP:30080"

sudo iptables -t nat -C OUTPUT -p tcp --dport 30080 -d 127.0.0.1 -j DNAT --to-destination "$MINIKUBE_IP:30080" 2>/dev/null || \
sudo iptables -t nat -A OUTPUT -p tcp --dport 30080 -d 127.0.0.1 -j DNAT --to-destination "$MINIKUBE_IP:30080"

# Allow forwarding
sudo iptables -C FORWARD -p tcp -d "$MINIKUBE_IP" --dport 30080 -j ACCEPT 2>/dev/null || \
sudo iptables -I FORWARD 1 -p tcp -d "$MINIKUBE_IP" --dport 30080 -j ACCEPT

echo "Port forwarding configured!"
echo ""
echo "Test locally:"
echo "  curl http://localhost:30080"
echo ""
echo "Get your public IP:"
echo "  curl -s http://169.254.169.254/latest/meta-data/public-ipv4"
echo ""
echo "Access from anywhere:"
echo "  http://<EC2_PUBLIC_IP>:30080"


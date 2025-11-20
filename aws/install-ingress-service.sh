#!/bin/bash
set -e

echo "Installing systemd service for Ingress port-forward..."
echo ""

# Get current user (admin)
CURRENT_USER=$(whoami)
HOME_DIR=$(eval echo ~$CURRENT_USER)

# Get Ingress service name
INGRESS_SVC=$(kubectl get svc -n ingress-nginx -l app.kubernetes.io/component=controller -o name | head -1 | sed 's|service/||')
if [ -z "$INGRESS_SVC" ]; then
  echo "ERROR: Ingress controller service not found!"
  echo "Make sure nginx-ingress addon is enabled: minikube addons enable ingress"
  exit 1
fi

echo "Ingress service: $INGRESS_SVC"
echo ""

# Get kubectl and minikube paths
KUBECTL_PATH=$(which kubectl)
MINIKUBE_PATH=$(which minikube)
KUBECONFIG_PATH="$HOME_DIR/.kube/config"

# Create systemd service file
SERVICE_FILE="/etc/systemd/system/ingress-port-forward.service"

echo "Creating systemd service file..."
sudo tee $SERVICE_FILE > /dev/null <<EOF
[Unit]
Description=Kubernetes Ingress Port Forward
After=network.target minikube.service
Requires=minikube.service

[Service]
Type=simple
User=root
Restart=always
RestartSec=10
StandardOutput=append:/var/log/ingress-port-forward.log
StandardError=append:/var/log/ingress-port-forward.log

# Set environment for kubectl
Environment="KUBECONFIG=$KUBECONFIG_PATH"

# Wait for minikube to be ready
ExecStartPre=/bin/sleep 30
ExecStartPre=$KUBECTL_PATH get svc $INGRESS_SVC -n ingress-nginx

# Start port-forward
ExecStart=$KUBECTL_PATH port-forward --address 0.0.0.0 service/$INGRESS_SVC -n ingress-nginx 80:80 443:443

# Kill port-forward on stop
ExecStop=/bin/pkill -f "kubectl port-forward.*$INGRESS_SVC"

[Install]
WantedBy=multi-user.target
EOF

echo "✓ Service file created: $SERVICE_FILE"
echo ""

# Copy kubeconfig to root (required for root user)
if [ ! -f /root/.kube/config ]; then
  echo "Copying kubeconfig to /root/.kube/..."
  sudo mkdir -p /root/.kube
  sudo cp $KUBECONFIG_PATH /root/.kube/config
  sudo chown root:root /root/.kube/config
  sudo chmod 600 /root/.kube/config
  echo "✓ Kubeconfig copied"
else
  echo "✓ Kubeconfig already exists for root"
fi

# Stop existing port-forward processes
echo "Stopping existing port-forward processes..."
sudo pkill -f "kubectl port-forward.*ingress-nginx-controller" || echo "No existing port-forward found"
sleep 2

# Reload systemd
echo "Reloading systemd daemon..."
sudo systemctl daemon-reload

# Enable service (start on boot)
echo "Enabling service (auto-start on boot)..."
sudo systemctl enable ingress-port-forward.service

# Start service
echo "Starting service..."
sudo systemctl start ingress-port-forward.service

# Wait a moment
sleep 3

# Check status
echo ""
echo "Checking service status..."
sudo systemctl status ingress-port-forward.service --no-pager -l | head -20

echo ""
echo "=== Service Installation Complete ==="
echo ""
echo "Service is now installed and running!"
echo ""
echo "To manage the service:"
echo "  Start:    sudo systemctl start ingress-port-forward"
echo "  Stop:     sudo systemctl stop ingress-port-forward"
echo "  Restart:  sudo systemctl restart ingress-port-forward"
echo "  Status:   sudo systemctl status ingress-port-forward"
echo "  Logs:     sudo journalctl -u ingress-port-forward -f"
echo ""
echo "The service will automatically start on system boot."
echo ""



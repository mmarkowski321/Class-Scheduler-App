#!/bin/bash
set -e

exec > >(tee -a /var/log/user-data-install.log) 2>&1

echo "Starting user data script..."
echo "Timestamp: $(date)"
echo ""

echo "Updating system packages..."
apt-get update -y
apt-get install -y curl wget apt-transport-https ca-certificates gnupg lsb-release

echo "Installing Docker..."
curl -fsSL https://download.docker.com/linux/debian/gpg | gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/debian $(lsb_release -cs) stable" | tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

DEFAULT_USER="admin"
if ! id -u "$DEFAULT_USER" &>/dev/null; then
    DEFAULT_USER="ubuntu"
fi

echo "Adding $DEFAULT_USER to docker group..."
usermod -aG docker "$DEFAULT_USER" || true

echo "Starting Docker service..."
systemctl enable docker
systemctl start docker
systemctl status docker --no-pager || true

echo "Installing kubectl..."
KUBECTL_VERSION=$(curl -L -s https://dl.k8s.io/release/stable.txt)
curl -LO "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/amd64/kubectl"
chmod +x kubectl
mv kubectl /usr/local/bin/
kubectl version --client 2>/dev/null || kubectl version --client --short 2>/dev/null || echo "kubectl installed (version check skipped)"

echo "Installing minikube..."
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
chmod +x minikube-linux-amd64
mv minikube-linux-amd64 /usr/local/bin/minikube
minikube version || echo "minikube installed (version check skipped)"

echo "Installing AWS CLI v2..."
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip
apt-get install -y unzip || true
cd /tmp
unzip -q awscliv2.zip
./aws/install
rm -rf aws awscliv2.zip
aws --version || echo "AWS CLI installed (version check skipped)"

echo "Starting minikube as user $DEFAULT_USER..."
MEMORY_SIZE="3000mb"
TOTAL_MEM=$(free -m | awk '/^Mem:/{print $2}')
if [ "$TOTAL_MEM" -lt 3500 ]; then
    MEMORY_SIZE="2400mb"
fi
if [ "$TOTAL_MEM" -lt 2500 ]; then
    MEMORY_SIZE="2000mb"
fi
echo "Starting Minikube with ${MEMORY_SIZE} memory..."
sudo -u "$DEFAULT_USER" -E env HOME=/home/$DEFAULT_USER minikube start --driver=docker --memory="$MEMORY_SIZE" || {
    echo "Minikube start as $DEFAULT_USER failed, trying as root..."
    minikube start --driver=docker --memory="$MEMORY_SIZE"
}

echo "Enabling metrics-server addon..."
sudo -u "$DEFAULT_USER" -E env HOME=/home/$DEFAULT_USER minikube addons enable metrics-server || {
    echo "Metrics-server enable as $DEFAULT_USER failed, trying as root..."
    minikube addons enable metrics-server
}

echo ""
echo "Verifying installation..."
echo "Docker version:"
docker --version
echo ""
echo "kubectl version:"
kubectl version --client 2>/dev/null || kubectl version --client --short 2>/dev/null || echo "kubectl installed"
echo ""
echo "minikube version:"
minikube version
echo ""
echo "minikube status:"
sudo -u "$DEFAULT_USER" -E env HOME=/home/$DEFAULT_USER minikube status || minikube status
echo ""

echo "User data script completed."
echo "Timestamp: $(date)"

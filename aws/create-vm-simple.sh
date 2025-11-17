#!/bin/bash
set -e

KEY_NAME=${1:-""}
INSTANCE_TYPE=${2:-"c7i-flex.large"}
REGION=${AWS_REGION:-"eu-north-1"}

if [ -z "$KEY_NAME" ]; then
  echo "Usage: $0 <key-pair-name> [instance-type]"
  echo "Example: $0 my-key-pair"
  echo "Example: $0 my-key-pair c7i-flex.large"
  echo ""
  echo "Recommended instance types:"
  echo "  c7i-flex.large - 2 vCPU, 4GB RAM (default, recommended for Minikube)"
  echo "  m7i-flex.large - 2 vCPU, 8GB RAM (for heavier workloads)"
  echo ""
  echo "Other options (may have insufficient RAM for Minikube):"
  echo "  t3.small   - 2 vCPU, 2GB RAM (WARNING: insufficient for Minikube - needs 1800MB+)"
  echo "  t3.medium  - 2 vCPU, 4GB RAM (may work but not recommended)"
  echo ""
  echo "To list available key pairs:"
  echo "  aws ec2 describe-key-pairs --region $REGION --query 'KeyPairs[*].KeyName' --output table"
  exit 1
fi

echo "Creating EC2 instance..."
echo "Region: $REGION"
echo "Key pair: $KEY_NAME"
echo "Instance type: $INSTANCE_TYPE"
echo ""

echo "Finding latest Debian 12 AMI..."
AMI_ID=$(aws ec2 describe-images \
  --owners 136693071363 \
  --filters "Name=name,Values=debian-12-amd64*" "Name=state,Values=available" \
  --region $REGION \
  --query 'Images | sort_by(@, &CreationDate) | [-1].ImageId' \
  --output text)

if [ -z "$AMI_ID" ] || [ "$AMI_ID" == "None" ]; then
  echo "ERROR: Could not find Debian 12 AMI in region $REGION"
  echo "Trying Ubuntu 22.04 instead..."
  AMI_ID=$(aws ec2 describe-images \
    --owners 099720109477 \
    --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" "Name=state,Values=available" \
    --region $REGION \
    --query 'Images | sort_by(@, &CreationDate) | [-1].ImageId' \
    --output text)
fi

if [ -z "$AMI_ID" ] || [ "$AMI_ID" == "None" ]; then
  echo "ERROR: Could not find a suitable AMI. Please check your region."
  exit 1
fi

echo "Using AMI: $AMI_ID"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/ec2-userdata.sh" ]; then
  USER_DATA_FILE="$SCRIPT_DIR/ec2-userdata.sh"
elif [ -f "aws/ec2-userdata.sh" ]; then
  USER_DATA_FILE="aws/ec2-userdata.sh"
else
  echo "ERROR: User data script not found: ec2-userdata.sh"
  echo "Expected location: $SCRIPT_DIR/ec2-userdata.sh or aws/ec2-userdata.sh"
  exit 1
fi

if [[ "$OSTYPE" == "darwin"* ]]; then
  USER_DATA=$(base64 -i "$USER_DATA_FILE")
else
  USER_DATA=$(base64 -w 0 "$USER_DATA_FILE" 2>/dev/null || base64 "$USER_DATA_FILE" | tr -d '\n')
fi
if [ -z "$USER_DATA" ]; then
  echo "ERROR: Failed to encode user data"
  exit 1
fi

echo "User data encoded: $(echo -n "$USER_DATA" | wc -c) characters"
SG_NAME="eduscheduler-minikube-sg"
echo "Setting up security group..."
SG_ID=$(aws ec2 describe-security-groups \
  --filters "Name=group-name,Values=$SG_NAME" \
  --region $REGION \
  --query 'SecurityGroups[0].GroupId' \
  --output text 2>/dev/null || echo "")

if [ -z "$SG_ID" ] || [ "$SG_ID" == "None" ]; then
  echo "Creating security group..."
  SG_ID=$(aws ec2 create-security-group \
    --group-name "$SG_NAME" \
    --description "Security group for EduScheduler Minikube VM" \
    --region $REGION \
    --query 'GroupId' \
    --output text)
  
  echo "Opening ports 22 (SSH), 80 (HTTP), 443 (HTTPS), 30080 (Frontend NodePort), 8080 (Backend)..."
  aws ec2 authorize-security-group-ingress \
    --group-id "$SG_ID" \
    --protocol tcp \
    --port 22 \
    --cidr 0.0.0.0/0 \
    --region $REGION 2>/dev/null || true
  
  aws ec2 authorize-security-group-ingress \
    --group-id "$SG_ID" \
    --protocol tcp \
    --port 80 \
    --cidr 0.0.0.0/0 \
    --region $REGION 2>/dev/null || true
  
  aws ec2 authorize-security-group-ingress \
    --group-id "$SG_ID" \
    --protocol tcp \
    --port 443 \
    --cidr 0.0.0.0/0 \
    --region $REGION 2>/dev/null || true
  
  aws ec2 authorize-security-group-ingress \
    --group-id "$SG_ID" \
    --protocol tcp \
    --port 30080 \
    --cidr 0.0.0.0/0 \
    --region $REGION 2>/dev/null || true
  
  aws ec2 authorize-security-group-ingress \
    --group-id "$SG_ID" \
    --protocol tcp \
    --port 8080 \
    --cidr 0.0.0.0/0 \
    --region $REGION 2>/dev/null || true
else
  echo "Using existing security group: $SG_ID"
fi

echo ""

echo "Creating EC2 instance ($INSTANCE_TYPE)..."

if [[ "$INSTANCE_TYPE" =~ ^t3\.(micro|small)$ ]] || [[ "$INSTANCE_TYPE" =~ ^t4g\.(micro|small)$ ]]; then
  echo "WARNING: $INSTANCE_TYPE has insufficient RAM for Minikube!"
  echo "Minikube requires minimum 1800MB RAM (recommended 3000MB+)."
  echo "Use c7i-flex.large (4GB RAM) or m7i-flex.large (8GB RAM) instead:"
  echo "$0 $KEY_NAME c7i-flex.large"
  echo ""
  read -p "Continue anyway? (y/N): " -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 1
  fi
  echo ""
fi

INSTANCE_ID=$(aws ec2 run-instances \
  --image-id "$AMI_ID" \
  --instance-type "$INSTANCE_TYPE" \
  --key-name "$KEY_NAME" \
  --security-group-ids "$SG_ID" \
  --user-data "$USER_DATA" \
  --associate-public-ip-address \
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=eduscheduler-minikube}]" \
  --region $REGION \
  --query 'Instances[0].InstanceId' \
  --output text)

if [ -z "$INSTANCE_ID" ] || [ "$INSTANCE_ID" == "None" ]; then
  echo "ERROR: Failed to create instance"
  exit 1
fi

echo "Instance created: $INSTANCE_ID"
echo "Waiting for instance to be running..."

aws ec2 wait instance-running --instance-ids "$INSTANCE_ID" --region $REGION

echo "Allocating Elastic IP for static public IP..."
ALLOCATION_ID=$(aws ec2 allocate-address \
  --domain vpc \
  --region $REGION \
  --tag-specifications "ResourceType=elastic-ip,Tags=[{Key=Name,Value=eduscheduler-minikube-eip}]" \
  --query 'AllocationId' \
  --output text)

if [ -z "$ALLOCATION_ID" ] || [ "$ALLOCATION_ID" == "None" ]; then
  echo "WARNING: Failed to allocate Elastic IP. Using dynamic IP (will change after restart)."
  PUBLIC_IP=$(aws ec2 describe-instances \
    --instance-ids "$INSTANCE_ID" \
    --region $REGION \
    --query 'Reservations[0].Instances[0].PublicIpAddress' \
    --output text)
  ELASTIC_IP=""
else
  echo "Associating Elastic IP with instance..."
  aws ec2 associate-address \
    --instance-id "$INSTANCE_ID" \
    --allocation-id "$ALLOCATION_ID" \
    --region $REGION > /dev/null 2>&1 || echo "WARNING: Failed to associate Elastic IP"
  
  sleep 2
  PUBLIC_IP=$(aws ec2 describe-addresses \
    --allocation-ids "$ALLOCATION_ID" \
    --region $REGION \
    --query 'Addresses[0].PublicIp' \
    --output text)
  ELASTIC_IP="$PUBLIC_IP"
fi

echo ""
echo "VM Ready!"
echo "Instance ID: $INSTANCE_ID"
if [ -n "$ELASTIC_IP" ]; then
  echo "Public IP:   $PUBLIC_IP (Elastic IP - STATIC, won't change after restart)"
  echo "Allocation ID: $ALLOCATION_ID"
else
  echo "Public IP:   $PUBLIC_IP (Dynamic - WILL CHANGE after stop/start)"
fi
echo ""
echo "SSH:"
echo "  chmod 400 ~/.ssh/${KEY_NAME}"
echo "  ssh -i ~/.ssh/${KEY_NAME} admin@${PUBLIC_IP}"
echo ""
echo "Wait 3-5 minutes for setup, then verify:"
echo "  minikube status"
echo "  kubectl get nodes"
echo ""
echo "Check logs: sudo cat /var/log/user-data-install.log"
echo ""
if [ -n "$ELASTIC_IP" ]; then
  echo "Note: This Elastic IP will persist even if you stop/start the instance."
  echo "To release it later: aws ec2 release-address --allocation-id $ALLOCATION_ID --region $REGION"
fi
echo ""




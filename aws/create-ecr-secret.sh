#!/bin/bash
set -e

AWS_ACCOUNT_ID=${AWS_ACCOUNT_ID:-"619613970416"}
REGION=${AWS_REGION:-"eu-north-1"}

echo "Getting ECR login token..."
ECR_PASSWORD=$(aws ecr get-login-password --region $REGION)

echo "Creating Kubernetes secret..."
kubectl create secret docker-registry ecr-registry-secret \
  --docker-server=${AWS_ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com \
  --docker-username=AWS \
  --docker-password="${ECR_PASSWORD}" \
  --docker-email=none@example.com \
  --dry-run=client -o yaml | kubectl apply -f -

echo "Secret 'ecr-registry-secret' created/updated!"
echo "Token expires after 12 hours. Run this script again to refresh."


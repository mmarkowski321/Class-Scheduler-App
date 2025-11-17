#!/bin/bash
set -e

echo "Stopping minikube tunnel..."
pkill -f "minikube tunnel" || echo "No tunnel process found"
echo "Done."


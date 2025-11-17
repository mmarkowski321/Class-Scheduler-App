#!/bin/bash
set -e

echo "Stopping kubectl port-forward..."
pkill -f "kubectl port-forward.*eduscheduler-frontend.*30080" || echo "No port-forward process found"
echo "Done."


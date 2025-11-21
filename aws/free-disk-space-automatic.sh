#!/bin/bash

echo "=== Automatic Disk Space Cleanup ==="
echo ""

# Check disk usage before
BEFORE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
echo "Disk usage before cleanup: ${BEFORE}%"
echo ""

# 1. Clean old system logs (keep last 12 hours)
echo "1. Cleaning system logs (keeping last 12 hours)..."
sudo journalctl --vacuum-time=12h
echo ""

# 2. Clean unused Docker images
echo "2. Cleaning unused Docker images..."
docker image prune -a -f
echo ""

# 3. Clean unused containers and networks
echo "3. Cleaning unused Docker containers and networks..."
docker container prune -f
docker network prune -f
echo ""

# 4. Clean old Docker logs (keep empty logs)
echo "4. Cleaning Docker logs..."
if [ -d /var/lib/docker/containers ]; then
    sudo find /var/lib/docker/containers -name "*-json.log" -exec truncate -s 0 {} \;
    echo "   Docker logs cleaned"
fi
echo ""

# 5. Clean apt cache
echo "5. Cleaning apt cache..."
sudo apt-get clean -y
sudo apt-get autoremove -y
echo ""

# 6. Clean all unused Docker resources (CAREFUL - removes everything unused)
echo "6. Cleaning all unused Docker resources (without volumes)..."
docker system prune -a -f
echo ""

# Check disk usage after
AFTER=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
FREED=$((BEFORE - AFTER))
echo ""
echo "=== Summary ==="
echo "Disk usage before: ${BEFORE}%"
echo "Disk usage after: ${AFTER}%"
echo "Freed: ${FREED}%"
echo ""
df -h /

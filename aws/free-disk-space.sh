#!/bin/bash

echo "=== Disk Usage Analysis ==="
echo ""
df -h
echo ""

echo "=== Top 10 Largest Directories ==="
sudo du -sh /* 2>/dev/null | sort -hr | head -10
echo ""

echo "=== Docker Usage ==="
docker system df
echo ""

echo "=== System Logs ==="
sudo journalctl --disk-usage
echo ""

echo "=== Checking Docker Images/Containers ==="
echo "Images:"
docker images --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}" | head -20
echo ""
echo "Containers (all):"
docker ps -a --format "table {{.ID}}\t{{.Names}}\t{{.Status}}" | head -20
echo ""

echo "=== Analyzing /var/lib/docker ==="
sudo du -sh /var/lib/docker/* 2>/dev/null | sort -hr | head -10
echo ""

echo "=== Analyzing /var/log ==="
sudo du -sh /var/log/* 2>/dev/null | sort -hr | head -10
echo ""

echo "=== Analyzing /tmp ==="
sudo du -sh /tmp/* 2>/dev/null | sort -hr | head -10
echo ""

echo ""
echo "=== Commands to Free Up Space ==="
echo ""
echo "1. Clean unused Docker images:"
echo "   docker image prune -a -f"
echo ""
echo "2. Clean all unused Docker resources:"
echo "   docker system prune -a --volumes -f"
echo ""
echo "3. Clean system logs (keep last 12 hours):"
echo "   sudo journalctl --vacuum-time=12h"
echo ""
echo "4. Clean Docker logs:"
echo "   sudo truncate -s 0 /var/lib/docker/containers/*/*-json.log"
echo ""
echo "5. Clean apt cache:"
echo "   sudo apt-get clean"
echo "   sudo apt-get autoremove -y"
echo ""

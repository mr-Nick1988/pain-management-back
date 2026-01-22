#!/bin/bash

# ============================================================================
# Quick Diagnostics Script - Pain Management Platform
# ============================================================================
# Purpose: Quickly check the health status of all system components
# Usage: ./scripts/quick-diagnostics.sh
# ============================================================================

echo "============================================"
echo "   PAIN MANAGEMENT PLATFORM - DIAGNOSTICS"
echo "============================================"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

ERRORS=0
WARNINGS=0

check_port() {
  local name=$1
  local port=$2
  
  if command -v nc &> /dev/null; then
    if nc -z localhost $port 2>/dev/null; then
      echo -e "${GREEN}✅${NC} $name (port $port): UP"
      return 0
    else
      echo -e "${RED}❌${NC} $name (port $port): DOWN"
      ((ERRORS++))
      return 1
    fi
  else
    # Fallback to curl
    if curl -s -o /dev/null http://localhost:$port 2>/dev/null; then
      echo -e "${GREEN}✅${NC} $name (port $port): UP"
      return 0
    else
      echo -e "${RED}❌${NC} $name (port $port): DOWN"
      ((ERRORS++))
      return 1
    fi
  fi
}

check_docker_container() {
  local name=$1
  
  if docker ps --format '{{.Names}}' | grep -q "^${name}$"; then
    local status=$(docker inspect --format='{{.State.Health.Status}}' $name 2>/dev/null)
    if [ "$status" == "healthy" ] || [ "$status" == "" ]; then
      echo -e "${GREEN}✅${NC} Container $name: RUNNING"
      return 0
    else
      echo -e "${YELLOW}⚠️${NC} Container $name: UNHEALTHY ($status)"
      ((WARNINGS++))
      return 1
    fi
  else
    echo -e "${RED}❌${NC} Container $name: NOT RUNNING"
    ((ERRORS++))
    return 1
  fi
}

echo -e "\n${BLUE}=== Docker Containers ===${NC}"
check_docker_container "dev_kafka"
check_docker_container "dev_postgres"
check_docker_container "dev_postgres_analytics"
check_docker_container "dev_auth"
check_docker_container "dev_emr"
check_docker_container "dev_notification"
check_docker_container "dev_pain_escalation"
check_docker_container "dev_external_vas"
check_docker_container "dev_reporting"
check_docker_container "dev_backup"

echo -e "\n${BLUE}=== Network Ports ===${NC}"
echo "Infrastructure:"
check_port "  Kafka" 9092
check_port "  PostgreSQL (Main)" 5432
check_port "  PostgreSQL (Analytics)" 5433

echo ""
echo "Application Services:"
check_port "  Monolith" 8080
check_port "  Auth Service" 8082
check_port "  Backup Service" 8085
check_port "  EMR Service" 8086
check_port "  Notification Service" 8087
check_port "  Pain Escalation" 8088
check_port "  External VAS" 8089
check_port "  Reporting Service" 8091

echo ""
echo "Tools (Optional):"
check_port "  Kafdrop" 9000 || true
check_port "  Prometheus" 9090 || true
check_port "  Grafana" 3000 || true

echo -e "\n${BLUE}=== Disk Space ===${NC}"
if command -v df &> /dev/null; then
  df -h 2>/dev/null | grep -E 'Filesystem|/dev/sd|C:' || df -h
else
  echo "df command not available"
fi

echo -e "\n${BLUE}=== Memory Usage ===${NC}"
if command -v free &> /dev/null; then
  free -h
elif command -v systeminfo &> /dev/null; then
  systeminfo | findstr /C:"Available Physical Memory"
else
  echo "Memory info not available"
fi

echo -e "\n${BLUE}=== Docker Stats ===${NC}"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" 2>/dev/null || echo "Docker stats not available"

echo -e "\n${BLUE}=== Summary ===${NC}"
echo -e "Errors:   ${RED}${ERRORS}${NC}"
echo -e "Warnings: ${YELLOW}${WARNINGS}${NC}"

echo -e "\n============================================"
if [ $ERRORS -eq 0 ]; then
  echo -e "   ${GREEN}✅ SYSTEM HEALTHY${NC}"
  echo "============================================"
  exit 0
else
  echo -e "   ${RED}❌ ISSUES DETECTED${NC}"
  echo "============================================"
  echo ""
  echo "See TROUBLESHOOTING.md for help:"
  echo "  docs/TROUBLESHOOTING.md"
  exit 1
fi

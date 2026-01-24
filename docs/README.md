# 📚 Pain Management Platform - Complete Documentation

**Version:** 3.3  
**Last Updated:** January 24, 2026  
**Status:** ✅ All 8 Services Running (API Gateway + 7 Microservices) + Distributed Tracing (Jaeger)

---

## 🎯 Overview

This documentation suite provides comprehensive guides for developing, deploying, testing, and maintaining the Pain Management Platform - a microservices-based healthcare application.

### System Architecture

- **API Gateway** (Port 8000): Single entry point, routing, JWT validation, Circuit Breaker
- **Monolith** (Port 8080): Core business logic
- **7 Microservices** (Ports 8082-8091): Specialized functions
- **Infrastructure**: Kafka, PostgreSQL, Consul (Service Discovery), Jaeger (Distributed Tracing)
- **Observability**: Prometheus, Grafana, Jaeger UI (port 16686)
- **Tools**: Kafdrop, Docker Compose

---

## 📖 Documentation Index

### 🚀 Getting Started

| Document | Description | Audience |
|----------|-------------|----------|
| **[01-guides/MICROSERVICES_QUICKSTART.md](01-guides/MICROSERVICES_QUICKSTART.md)** | Quick start guide | Developers |
| **[01-guides/HOW_TO_RUN_AND_TEST.md](01-guides/HOW_TO_RUN_AND_TEST.md)** | How to run and test (Russian) | All |
| **[03-operations/DEVOPS_COMPLETE_GUIDE.md](03-operations/DEVOPS_COMPLETE_GUIDE.md)** | Complete DevOps operations guide | DevOps, Developers, QA |

### 🐋 Docker & Infrastructure

| Document | Description | Audience |
|----------|-------------|----------|
| **[03-operations/DOCKER_COMPOSE_REFERENCE.md](03-operations/DOCKER_COMPOSE_REFERENCE.md)** | Complete Docker Compose reference | DevOps, Developers |
| **[03-operations/DATABASE_SETUP_PLAN.md](03-operations/DATABASE_SETUP_PLAN.md)** | Database setup and structure | DevOps, Developers |
| **[docker-compose.dev.yml](../docker-compose.dev.yml)** | Docker Compose configuration | DevOps |
| **[.env.example](../.env.example)** | Environment variables template | All |

### 🧪 Testing & Quality

| Document | Description | Audience |
|----------|-------------|----------|
| **[04-testing/TESTING_GUIDE.md](04-testing/TESTING_GUIDE.md)** | Comprehensive testing guide | QA, Developers |
| **[03-operations/TROUBLESHOOTING.md](03-operations/TROUBLESHOOTING.md)** | Problem resolution guide | All |

### 🏗️ Architecture & Design

| Document | Description | Audience |
|----------|-------------|----------|
| **[02-architecture/BACKEND_ARCHITECTURE_OVERVIEW.md](02-architecture/BACKEND_ARCHITECTURE_OVERVIEW.md)** | 🎯 **Complete backend architecture** | **All** |
| **[MICROSERVICES_PATTERNS_ROADMAP.md](MICROSERVICES_PATTERNS_ROADMAP.md)** | 🚀 **Microservices patterns roadmap** | **Architects, Developers** |
| **[SERVICE_DISCOVERY_IMPLEMENTATION.md](SERVICE_DISCOVERY_IMPLEMENTATION.md)** | 🔍 **Service Discovery with Consul** | **Developers, DevOps** |
| **[DISTRIBUTED_TRACING_IMPLEMENTATION.md](DISTRIBUTED_TRACING_IMPLEMENTATION.md)** | 🔭 **Distributed Tracing with Jaeger** | **Developers, DevOps** |
| **[CIRCUIT_BREAKER_IMPLEMENTATION.md](CIRCUIT_BREAKER_IMPLEMENTATION.md)** | 🔥 **Circuit Breaker implementation guide** | **Developers** |
| **[API_GATEWAY_INTEGRATION_GUIDE.md](API_GATEWAY_INTEGRATION_GUIDE.md)** | API Gateway integration | Developers |
| **[02-architecture/MICROSERVICES_MIGRATION_STRATEGY.md](02-architecture/MICROSERVICES_MIGRATION_STRATEGY.md)** | Migration strategy | Architects, Lead Developers |
| **[02-architecture/MIGRATION_ROADMAP.md](02-architecture/MIGRATION_ROADMAP.md)** | Migration roadmap | Project Managers, Developers |
| **[06-api/event-schemas/KAFKA_EVENT_SCHEMAS.md](06-api/event-schemas/KAFKA_EVENT_SCHEMAS.md)** | Kafka event schemas | Developers |

### 📋 Microservices Documentation

| Document | Service | Port |
|----------|---------|------|
| **[05-microservices/api-gateway-service.md](05-microservices/api-gateway-service.md)** | 🚪 **API Gateway - Single Entry Point** | **8000** |
| **[05-microservices/authentication-service-docs.md](05-microservices/authentication-service-docs.md)** | Authentication & JWT | 8082 |
| **[05-microservices/emr-integration-service-docs.md](05-microservices/emr-integration-service-docs.md)** | EMR & FHIR integration | 8086 |
| **[05-microservices/NOTIFICATION_SERVICE.md](05-microservices/NOTIFICATION_SERVICE.md)** | Email, WebSocket, Push | 8087 |
| **[05-microservices/PAIN_ESCALATION_SERVICE.md](05-microservices/PAIN_ESCALATION_SERVICE.md)** | Pain tracking & alerts | 8088 |
| **[05-microservices/EXTERNAL_VAS_INTEGRATION_SERVICE.md](05-microservices/EXTERNAL_VAS_INTEGRATION_SERVICE.md)** | VAS device integration | 8089 |
| **[05-microservices/reporting-service-docs.md](05-microservices/reporting-service-docs.md)** | Analytics & reports | 8091 |
| **[05-microservices/backup-restore-documentation.md](05-microservices/backup-restore-documentation.md)** | Database backup | 8085 |

---

## 🚀 Quick Start Guide

### For Developers

1. **Read first**: [03-operations/DEVOPS_COMPLETE_GUIDE.md](03-operations/DEVOPS_COMPLETE_GUIDE.md) - Section "Quick Start (5 Minutes)"
2. **Setup environment**: Copy `.env.example` to `.env`
3. **Start infrastructure**:
   ```bash
   docker-compose up -d
   ```
4. **Run monolith**:
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
5. **Verify**: `curl http://localhost:8080/actuator/health`

### For DevOps Engineers

1. **Full system setup**: [03-operations/DEVOPS_COMPLETE_GUIDE.md](03-operations/DEVOPS_COMPLETE_GUIDE.md)
2. **Docker Compose reference**: [03-operations/DOCKER_COMPOSE_REFERENCE.md](03-operations/DOCKER_COMPOSE_REFERENCE.md)
3. **Start all services**:
   ```bash
   docker-compose --profile all up -d
   ```
4. **Monitor**: Access Kafdrop (http://localhost:9000), Prometheus (http://localhost:9090), Grafana (http://localhost:3000)

### For QA Engineers

1. **Testing guide**: [04-testing/TESTING_GUIDE.md](04-testing/TESTING_GUIDE.md)
2. **Start test environment**:
   ```bash
   docker-compose --profile core up -d
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
3. **Run tests**: See [TESTING_GUIDE.md](TESTING_GUIDE.md) for API tests, integration tests, E2E tests

---

## 🔧 Common Scenarios

### Scenario 1: Start Working on a Specific Microservice

```bash
# 1. Start infrastructure + dependencies
docker-compose --profile core up -d

# 2. Stop the service you want to develop
docker-compose stop dev_emr

# 3. Run locally from IDE
cd C:\backend_projects\microservices\emr-integration-service
mvn spring-boot:run

# Now you can debug with breakpoints
```

**See**: [DEVOPS_COMPLETE_GUIDE.md - Development Workflows](DEVOPS_COMPLETE_GUIDE.md#development-workflows)

---

### Scenario 2: Test Full System Integration

```bash
# Start everything
docker-compose --profile all --profile tools up -d

# Run monolith
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Access Kafdrop to monitor events
http://localhost:9000
```

**See**: [TESTING_GUIDE.md - Integration Testing](TESTING_GUIDE.md#integration-testing)

---

### Scenario 3: Troubleshoot a Problem

1. **Check Quick Diagnostics**:
   ```bash
   ./scripts/quick-diagnostics.sh
   ```

2. **View specific service logs**:
   ```bash
   docker-compose logs -f auth-service
   ```

3. **Find solution**: [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

---

## 📊 System Ports Reference

### Infrastructure
- **5432** - PostgreSQL (Main)
- **5433** - PostgreSQL (Analytics)
- **9092** - Kafka
- **9000** - Kafdrop (Kafka UI)
- **9090** - Prometheus
- **3000** - Grafana

### Application Services
- **8080** - Monolith
- **8082** - Authentication Service
- **8085** - Backup & Restore Service
- **8086** - EMR Integration Service
- **8087** - Notification Service
- **8088** - Pain Escalation Service
- **8089** - External VAS Service
- **8091** - Reporting Service

---

## 🎓 Learning Path

### For New Team Members

1. **Day 1**: Read [DEVOPS_COMPLETE_GUIDE.md](DEVOPS_COMPLETE_GUIDE.md) - Overview & Quick Start
2. **Day 2**: Follow [HOW_TO_RUN_AND_TEST.md](HOW_TO_RUN_AND_TEST.md) - Run full system
3. **Day 3**: Study [DOCKER_COMPOSE_REFERENCE.md](DOCKER_COMPOSE_REFERENCE.md) - Understand infrastructure
4. **Day 4**: Practice [TESTING_GUIDE.md](TESTING_GUIDE.md) - Run tests
5. **Day 5**: Review [architecture/MICROSERVICES_MIGRATION_STRATEGY.md](architecture/MICROSERVICES_MIGRATION_STRATEGY.md) - Understand architecture

### For Experienced Developers

- **Architecture**: [architecture/](architecture/) folder
- **API Contracts**: [api/event-schemas/](api/event-schemas/) folder
- **Service Details**: [microservices/](microservices/) folder

---

## 🆘 Getting Help

### Documentation Not Clear?

1. Search this README for your topic
2. Check [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common issues
3. Review specific guide (DevOps, Testing, Docker Compose)

### System Not Working?

1. Run diagnostics: `./scripts/quick-diagnostics.sh`
2. Check logs: `docker-compose logs -f`
3. See [TROUBLESHOOTING.md](TROUBLESHOOTING.md)

### Need to Understand Architecture?

1. [architecture/MICROSERVICES_MIGRATION_STRATEGY.md](architecture/MICROSERVICES_MIGRATION_STRATEGY.md)
2. [architecture/diagrams/](architecture/diagrams/)
3. Individual service docs in [microservices/](microservices/)

---

## 📝 Documentation Standards

### Language Policy

- **Technical Documentation** (DevOps, Docker, Testing): **English**
- **Architecture & Migration**: **Russian** (legacy)
- **Quick References**: **Both languages**

### File Naming Conventions

- `UPPERCASE_WITH_UNDERSCORES.md` - Major documentation files
- `lowercase-with-dashes.md` - Service-specific documentation
- `README.md` - Index/overview files

### Documentation Structure

```
docs/
├── README.md                          # This file - main index
├── DEVOPS_COMPLETE_GUIDE.md          # Complete DevOps guide
├── DOCKER_COMPOSE_REFERENCE.md       # Docker Compose reference
├── TESTING_GUIDE.md                  # Testing guide
├── TROUBLESHOOTING.md                # Troubleshooting guide
├── HOW_TO_RUN_AND_TEST.md           # Quick reference (Russian)
├── MICROSERVICES_QUICKSTART.md      # Migration quick start
├── architecture/                     # Architecture documentation
│   ├── MICROSERVICES_MIGRATION_STRATEGY.md
│   ├── MIGRATION_ROADMAP.md
│   └── diagrams/
├── api/                              # API documentation
│   └── event-schemas/
│       └── KAFKA_EVENT_SCHEMAS.md
└── microservices/                    # Individual service docs
    ├── AUTHENTICATION_SERVICE.md
    ├── EMR_INTEGRATION_SERVICE.md
    └── ...
```

---

## 🔄 Regular Maintenance Tasks

### Daily
- Check service health: `docker-compose ps`
- Review error logs: `docker-compose logs | grep -i error`

### Weekly
- Review [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for new issues
- Update test data: See [TESTING_GUIDE.md - Test Data Management](TESTING_GUIDE.md#test-data-management)

### Monthly
- Review and update documentation
- Check disk space: `df -h`
- Clean Docker resources: `docker system prune -a`

---

## 📈 Monitoring & Observability

### Built-in Tools

| Tool | URL | Purpose |
|------|-----|---------|
| Kafdrop | http://localhost:9000 | Kafka UI - topics, messages, consumer groups |
| Prometheus | http://localhost:9090 | Metrics collection & queries |
| Grafana | http://localhost:3000 | Metrics visualization (admin/admin) |

### Health Checks

All services expose Spring Boot Actuator:
```bash
# Monolith
curl http://localhost:8080/actuator/health

# Microservices
curl http://localhost:8082/actuator/health  # Auth
curl http://localhost:8086/actuator/health  # EMR
curl http://localhost:8087/actuator/health  # Notification
curl http://localhost:8088/actuator/health  # Pain Escalation
curl http://localhost:8089/actuator/health  # External VAS
curl http://localhost:8091/actuator/health  # Reporting
```

**See**: [DEVOPS_COMPLETE_GUIDE.md - Health Checks](DEVOPS_COMPLETE_GUIDE.md#health-checks-and-monitoring)

---

## 🔐 Security Considerations

### Secrets Management

- **Never commit** `.env` to git (already in `.gitignore`)
- Use **strong JWT secrets** (min 256 bits)
- Rotate secrets regularly
- Use environment variables for all sensitive data

**See**: [DEVOPS_COMPLETE_GUIDE.md - Security Best Practices](DEVOPS_COMPLETE_GUIDE.md#security-best-practices)

### Network Security

- Services communicate via internal Docker network
- Only necessary ports exposed to host
- Use `host.docker.internal` for host access from containers

---

## 🎯 Project Status

### Current Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         MONOLITH (8080)                          │
│  Core Business Logic + Treatment Protocols + User Workflows      │
└──────────────────┬──────────────────────────────────────────────┘
                   │
        ┌──────────┴──────────┐
        │                     │
        ▼                     ▼
┌──────────────┐      ┌──────────────┐
│ KAFKA (9092) │      │ POSTGRES     │
│ Event Bus    │      │ (5432)       │
└──────┬───────┘      └──────────────┘
       │
       ├─────────────┬─────────────┬─────────────┬─────────────┐
       │             │             │             │             │
       ▼             ▼             ▼             ▼             ▼
┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐
│   Auth   │  │   EMR    │  │  Notify  │  │   Pain   │  │   VAS    │
│  (8082)  │  │  (8086)  │  │  (8087)  │  │  (8088)  │  │  (8089)  │
└──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘
       │             │             │             │             │
       └─────────────┴─────────────┴─────────────┴─────────────┘
                                   │
                                   ▼
                          ┌──────────────┐
                          │  Reporting   │
                          │   (8091)     │
                          └──────────────┘
```

### Migration Progress

- ✅ **Stage 0**: Infrastructure setup complete
- ✅ **Stage 1**: Core microservices deployed
- 🔄 **Stage 2**: Additional services in progress
- ⏳ **Stage 3**: Monolith cleanup planned

**See**: [architecture/MIGRATION_ROADMAP.md](architecture/MIGRATION_ROADMAP.md)

---

## 📞 Contact & Support

### Documentation Feedback

Found an issue or have a suggestion? Update the relevant document and create a pull request.

### Code Issues

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) first, then:
1. Collect debug info: `./scripts/collect-debug-info.sh`
2. Check existing issues
3. Create new issue with debug information

---

## 📜 Version History

### Version 3.0 (January 12, 2026)
- ✅ Complete DevOps documentation suite
- ✅ Comprehensive Docker Compose reference
- ✅ Full testing guide with scripts
- ✅ Detailed troubleshooting guide
- ✅ Updated all existing documentation

### Version 2.0 (January 9, 2026)
- Microservices architecture implemented
- Docker Compose profiles added
- Monitoring with Prometheus/Grafana

### Version 1.0 (September 21, 2025)
- Initial monolith implementation
- Basic Docker setup

---

## 🎉 Quick Win Checklist

New to the project? Complete these tasks:

- [ ] Read [DEVOPS_COMPLETE_GUIDE.md](DEVOPS_COMPLETE_GUIDE.md) - Quick Start section
- [ ] Copy `.env.example` to `.env`
- [ ] Run `docker-compose up -d`
- [ ] Run `mvn spring-boot:run`
- [ ] Access http://localhost:8080/actuator/health
- [ ] Access http://localhost:9000 (Kafdrop)
- [ ] Run `./scripts/quick-diagnostics.sh`
- [ ] Test one API endpoint from [TESTING_GUIDE.md](TESTING_GUIDE.md)
- [ ] Review [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- [ ] Explore one microservice in [microservices/](microservices/)

---

**Welcome to the Pain Management Platform! 🚀**

For questions, start with this README, then dive into specific guides.

---

**Last Updated:** January 12, 2026  
**Version:** 3.0  
**Maintained By:** Development Team

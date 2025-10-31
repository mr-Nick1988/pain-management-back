# 05 - Eureka Server (Service Discovery)

**Предыдущий:** [04_DOCKER_SETUP.md](04_DOCKER_SETUP.md)  
**Следующий:** [06_CONFIG_SERVER.md](06_CONFIG_SERVER.md)

---

## 🎯 Назначение

**Eureka Server** - это Service Discovery сервер, который позволяет микросервисам:
- Регистрироваться при запуске
- Находить друг друга по имени
- Автоматически обнаруживать недоступные сервисы

---

## 📦 Создание проекта

### Структура

```
eureka-server/
├── src/
│   └── main/
│       ├── java/
│       │   └── pain/management/eureka/
│       │       └── EurekaServerApplication.java
│       └── resources/
│           ├── application.yml
│           └── application-docker.yml
├── pom.xml
└── Dockerfile
```

### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.5</version>
    </parent>
    
    <groupId>pain.management</groupId>
    <artifactId>eureka-server</artifactId>
    <version>1.0.0</version>
    <name>Eureka Server</name>
    
    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 💻 Код приложения

### EurekaServerApplication.java

```java
package pain.management.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

---

## ⚙️ Конфигурация

### application.yml

```yaml
server:
  port: 8761

spring:
  application:
    name: eureka-server

eureka:
  instance:
    hostname: localhost
  client:
    register-with-eureka: false
    fetch-registry: false
    service-url:
      defaultZone: http://${eureka.instance.hostname}:${server.port}/eureka/
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 10000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always

logging:
  level:
    com.netflix.eureka: INFO
    com.netflix.discovery: INFO
```

### application-docker.yml

```yaml
spring:
  config:
    activate:
      on-profile: docker

eureka:
  instance:
    hostname: eureka-server
    prefer-ip-address: true
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/
```

---

## 🐳 Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/eureka-server-1.0.0.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 🚀 Сборка и запуск

### Локальный запуск

```bash
# Сборка
mvn clean package

# Запуск
java -jar target/eureka-server-1.0.0.jar

# Или через Maven
mvn spring-boot:run
```

### Docker запуск

```bash
# Сборка образа
docker build -t eureka-server:1.0.0 .

# Запуск контейнера
docker run -d \
  --name eureka-server \
  -p 8761:8761 \
  -e SPRING_PROFILES_ACTIVE=docker \
  eureka-server:1.0.0
```

### Docker Compose

Добавьте в `docker-compose.yml`:

```yaml
eureka-server:
  build: ./infrastructure/eureka-server
  container_name: eureka-server
  ports:
    - "8761:8761"
  environment:
    - SPRING_PROFILES_ACTIVE=docker
  networks:
    - microservices-network
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8761/actuator/health"]
    interval: 30s
    timeout: 10s
    retries: 5
```

Запуск:

```bash
docker-compose up -d eureka-server
```

---

## 🔍 Проверка работоспособности

### Web UI

Откройте браузер: http://localhost:8761

Вы должны увидеть Eureka Dashboard с:
- Статусом сервера
- Списком зарегистрированных сервисов (пока пустой)
- Общей информацией

### Health Check

```bash
curl http://localhost:8761/actuator/health
```

Ожидаемый ответ:

```json
{
  "status": "UP"
}
```

### Eureka API

```bash
# Список всех зарегистрированных сервисов
curl http://localhost:8761/eureka/apps

# Информация о конкретном сервисе
curl http://localhost:8761/eureka/apps/LOGGING-SERVICE
```

---

## 🔧 Регистрация клиента

### Добавить зависимость в микросервис

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

### Конфигурация клиента

```yaml
spring:
  application:
    name: logging-service

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 30
```

### Включить Eureka Client

```java
@SpringBootApplication
@EnableDiscoveryClient  // Или @EnableEurekaClient
public class LoggingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoggingServiceApplication.class, args);
    }
}
```

---

## 📊 Мониторинг

### Метрики

```bash
# Prometheus метрики
curl http://localhost:8761/actuator/prometheus
```

### Важные метрики

- `eureka_server_registry_size` - Количество зарегистрированных сервисов
- `eureka_server_renewals` - Количество обновлений регистрации
- `eureka_server_evictions` - Количество удалений сервисов

---

## ⚙️ Настройки производительности

### Для Production

```yaml
eureka:
  server:
    # Включить self-preservation mode
    enable-self-preservation: true
    
    # Интервал проверки (по умолчанию 60 секунд)
    eviction-interval-timer-in-ms: 60000
    
    # Renewal threshold
    renewal-percent-threshold: 0.85
    
    # Response cache
    response-cache-update-interval-ms: 30000
    
  instance:
    # Heartbeat interval
    lease-renewal-interval-in-seconds: 30
    
    # Lease expiration
    lease-expiration-duration-in-seconds: 90
```

---

## 🔐 Безопасность

### Добавить Spring Security (опционально)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

```yaml
spring:
  security:
    user:
      name: admin
      password: ${EUREKA_PASSWORD:admin}

eureka:
  client:
    service-url:
      defaultZone: http://admin:${EUREKA_PASSWORD:admin}@localhost:8761/eureka/
```

---

## ✅ Чеклист

- [ ] Eureka Server запущен
- [ ] Web UI доступен (http://localhost:8761)
- [ ] Health check проходит
- [ ] Готов к регистрации клиентов

---

**Следующий шаг:** [06_CONFIG_SERVER.md](06_CONFIG_SERVER.md)

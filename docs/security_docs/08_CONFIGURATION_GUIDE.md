# Руководство по конфигурации системы безопасности

## Конфигурация Auth Service (Микросервис)

### application.yml

```yaml
server:
  port: 8082

spring:
  application:
    name: auth-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/auth_db
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

# JWT Configuration
jwt:
  secret: "my-super-secret-key-for-jwt-signing-must-be-at-least-256-bits-long-for-hs256-algorithm"
  access-token-expiration: 86400000   # 24 часа в миллисекундах
  refresh-token-expiration: 604800000 # 7 дней в миллисекундах

# Logging
logging:
  level:
    root: INFO
    com.auth: DEBUG
    org.springframework.security: DEBUG
```

### Переменные окружения (для production)

```bash
# .env файл для Auth Service
JWT_SECRET=your-production-secret-key-min-256-bits
DB_URL=jdbc:postgresql://prod-db:5432/auth_db
DB_USERNAME=auth_user
DB_PASSWORD=secure_password
```

---

## Конфигурация Monolith API

### application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: pain-management-monolith
  
  datasource:
    url: jdbc:postgresql://localhost:5432/pain_management_db
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

# JWT Configuration (ДОЛЖЕН СОВПАДАТЬ С AUTH SERVICE!)
jwt:
  secret: "my-super-secret-key-for-jwt-signing-must-be-at-least-256-bits-long-for-hs256-algorithm"

# Security Configuration
spring:
  security:
    filter:
      order: -100  # JWT фильтр выполняется первым

# CORS Configuration
cors:
  allowed-origins: http://localhost:5173,http://localhost:3000
  allowed-methods: GET,POST,PUT,PATCH,DELETE,OPTIONS
  allowed-headers: "*"
  allow-credentials: true

# Logging
logging:
  level:
    root: INFO
    pain_helper_back: DEBUG
    pain_helper_back.security: DEBUG
    org.springframework.security: DEBUG
```

### Переменные окружения (для production)

```bash
# .env файл для Monolith
JWT_SECRET=your-production-secret-key-min-256-bits
DB_URL=jdbc:postgresql://prod-db:5432/pain_management_db
DB_USERNAME=pain_user
DB_PASSWORD=secure_password
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

---

## Конфигурация Frontend

### .env файл

```env
# Development
REACT_APP_AUTH_SERVICE_URL=http://localhost:8082
REACT_APP_API_BASE_URL=http://localhost:8080

# Production
# REACT_APP_AUTH_SERVICE_URL=https://auth.your-domain.com
# REACT_APP_API_BASE_URL=https://api.your-domain.com
```

### package.json (proxy для development)

```json
{
  "name": "pain-management-frontend",
  "version": "1.0.0",
  "proxy": "http://localhost:8080",
  "scripts": {
    "start": "react-scripts start",
    "build": "react-scripts build"
  }
}
```

---

## Генерация секретного ключа

### Требования к секретному ключу:
- Минимум 256 бит (32 символа) для HS256
- Случайная строка
- Одинаковая в Auth Service и Monolith

### Генерация в Java:

```java
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;

public class JwtSecretGenerator {
    public static void main(String[] args) {
        // Генерируем случайный ключ для HS256
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        
        // Конвертируем в Base64 строку
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
        
        System.out.println("JWT Secret Key:");
        System.out.println(base64Key);
    }
}
```

### Генерация в командной строке:

```bash
# Linux/Mac
openssl rand -base64 64

# Windows (PowerShell)
$bytes = New-Object byte[] 64
(New-Object Security.Cryptography.RNGCryptoServiceProvider).GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

---

## Docker Compose конфигурация

### docker-compose.yml

```yaml
version: '3.8'

services:
  # PostgreSQL для Auth Service
  auth-db:
    image: postgres:15
    container_name: auth-db
    environment:
      POSTGRES_DB: auth_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5433:5432"
    volumes:
      - auth-db-data:/var/lib/postgresql/data

  # PostgreSQL для Monolith
  pain-db:
    image: postgres:15
    container_name: pain-db
    environment:
      POSTGRES_DB: pain_management_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - pain-db-data:/var/lib/postgresql/data

  # Auth Service
  auth-service:
    build: ./auth-service
    container_name: auth-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://auth-db:5432/auth_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8082:8082"
    depends_on:
      - auth-db

  # Monolith API
  monolith-api:
    build: ./pain-management-back
    container_name: monolith-api
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://pain-db:5432/pain_management_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"
    depends_on:
      - pain-db

  # Frontend
  frontend:
    build: ./pain-management-frontend
    container_name: frontend
    environment:
      REACT_APP_AUTH_SERVICE_URL: http://localhost:8082
      REACT_APP_API_BASE_URL: http://localhost:8080
    ports:
      - "5173:5173"
    depends_on:
      - auth-service
      - monolith-api

volumes:
  auth-db-data:
  pain-db-data:
```

### .env файл для Docker Compose

```env
JWT_SECRET=my-super-secret-key-for-jwt-signing-must-be-at-least-256-bits-long-for-hs256-algorithm
```

---

## Настройка CORS

### В Monolith (SecurityConfig.java)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            // ... остальная конфигурация
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Разрешенные origins
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3000",
            "https://your-production-domain.com"
        ));
        
        // Разрешенные методы
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // Разрешенные заголовки
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Разрешить credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Максимальное время кеширования preflight запроса
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
```

### В Auth Service (application.yml)

```yaml
spring:
  web:
    cors:
      allowed-origins: 
        - http://localhost:5173
        - http://localhost:3000
        - https://your-production-domain.com
      allowed-methods: 
        - GET
        - POST
        - PUT
        - PATCH
        - DELETE
        - OPTIONS
      allowed-headers: "*"
      allow-credentials: true
      max-age: 3600
```

---

## Настройка логирования

### logback-spring.xml (для обоих сервисов)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Console Appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File Appender для Security логов -->
    <appender name="SECURITY_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/security.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/security.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Security логи -->
    <logger name="pain_helper_back.security" level="DEBUG" additivity="false">
        <appender-ref ref="SECURITY_FILE"/>
        <appender-ref ref="CONSOLE"/>
    </logger>

    <!-- Spring Security логи -->
    <logger name="org.springframework.security" level="DEBUG" additivity="false">
        <appender-ref ref="SECURITY_FILE"/>
        <appender-ref ref="CONSOLE"/>
    </logger>

    <!-- Root logger -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

---

## Настройка для разных окружений

### application-dev.yml (Development)

```yaml
jwt:
  secret: "dev-secret-key-for-development-only-not-for-production"
  access-token-expiration: 86400000   # 24 часа

logging:
  level:
    root: DEBUG
    pain_helper_back: DEBUG

spring:
  jpa:
    show-sql: true
    properties:
      hibernate:
        format_sql: true
```

### application-prod.yml (Production)

```yaml
jwt:
  secret: ${JWT_SECRET}  # Из переменных окружения
  access-token-expiration: 900000  # 15 минут (более безопасно)

logging:
  level:
    root: WARN
    pain_helper_back: INFO
    pain_helper_back.security: INFO

spring:
  jpa:
    show-sql: false
```

### Запуск с профилем:

```bash
# Development
java -jar -Dspring.profiles.active=dev monolith.jar

# Production
java -jar -Dspring.profiles.active=prod monolith.jar
```

---

## Настройка SSL/TLS (для Production)

### application.yml

```yaml
server:
  port: 8443
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${KEYSTORE_PASSWORD}
    key-store-type: PKCS12
    key-alias: tomcat
```

### Генерация самоподписанного сертификата (для тестирования):

```bash
keytool -genkeypair -alias tomcat -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650 \
  -storepass changeit
```

---

## Мониторинг и метрики

### pom.xml (добавить зависимости)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

### application.yml

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
```

### Доступные endpoints:

- `http://localhost:8080/actuator/health` - Здоровье приложения
- `http://localhost:8080/actuator/metrics` - Метрики
- `http://localhost:8080/actuator/prometheus` - Метрики для Prometheus

---

## Чек-лист конфигурации

### Auth Service
- [ ] `jwt.secret` установлен (минимум 256 бит)
- [ ] `jwt.access-token-expiration` настроен
- [ ] `jwt.refresh-token-expiration` настроен
- [ ] База данных настроена
- [ ] CORS настроен для Frontend
- [ ] Логирование настроено

### Monolith
- [ ] `jwt.secret` совпадает с Auth Service
- [ ] База данных настроена (отдельная от Auth Service)
- [ ] CORS настроен для Frontend
- [ ] SecurityConfig правильно настроен
- [ ] JwtAuthenticationFilter добавлен в цепочку фильтров
- [ ] RoleCheckAspect активирован (@EnableAspectJAutoProxy)
- [ ] Логирование настроено

### Frontend
- [ ] `REACT_APP_AUTH_SERVICE_URL` указывает на Auth Service
- [ ] `REACT_APP_API_BASE_URL` указывает на Monolith
- [ ] axios interceptors настроены
- [ ] authService реализован
- [ ] ProtectedRoute реализован

### Production
- [ ] JWT secret в переменных окружения (не в коде!)
- [ ] SSL/TLS настроен
- [ ] CORS ограничен только production доменами
- [ ] Логирование в файлы
- [ ] Мониторинг настроен
- [ ] Короткий срок жизни access token (15 минут)

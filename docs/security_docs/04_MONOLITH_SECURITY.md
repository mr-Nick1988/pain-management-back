# Компоненты безопасности в Monolith

## Обзор компонентов

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Request                              │
│            Authorization: Bearer <token>                     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  1. JwtAuthenticationFilter (OncePerRequestFilter)          │
│     • Извлекает токен из заголовка                          │
│     • Валидирует токен                                      │
│     • Создает Authentication объект                         │
│     • Сохраняет в SecurityContext                           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  2. SecurityConfig (Spring Security Configuration)          │
│     • Настраивает цепочку фильтров                          │
│     • Определяет публичные endpoints                        │
│     • Отключает CSRF, Session                               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  3. RoleCheckAspect (AOP)                                   │
│     • Перехватывает методы с @RequireRole                   │
│     • Проверяет роль пользователя                           │
│     • Бросает AccessDeniedException если нет прав           │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│  4. Controller                                              │
│     • Получает Authentication из параметра                  │
│     • Логирует действия пользователя                        │
│     • Выполняет бизнес-логику                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 1. JwtAuthenticationFilter

### Назначение
Фильтр, который перехватывает каждый HTTP запрос и валидирует JWT токен.

### Расположение
```
src/main/java/pain_helper_back/security/JwtAuthenticationFilter.java
```

### Код

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Извлекаем токен из заголовка Authorization
            String token = extractTokenFromRequest(request);
            
            if (token != null && validateToken(token)) {
                // 2. Извлекаем claims из токена
                Claims claims = extractAllClaims(token);
                
                String personId = claims.get("personId", String.class);
                String role = claims.get("role", String.class);
                String firstName = claims.get("firstName", String.class);
                
                // 3. Создаем UserDetails
                UserDetails userDetails = new UserDetails();
                userDetails.setPersonId(personId);
                userDetails.setRole(role);
                userDetails.setFirstName(firstName);
                
                // 4. Создаем Authentication объект
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                personId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                authentication.setDetails(userDetails);
                
                // 5. Сохраняем в SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("JWT authenticated: personId={}, role={}", personId, role);
            }
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
        }
        
        // 6. Передаем запрос дальше
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Data
    public static class UserDetails {
        private String personId;
        private String role;
        private String firstName;
    }
}
```

### Что делает:
1. **Извлекает токен** из заголовка `Authorization: Bearer <token>`
2. **Валидирует токен** (подпись + срок действия)
3. **Извлекает claims** (personId, role, firstName)
4. **Создает Authentication** объект с ролью пользователя
5. **Сохраняет в SecurityContext** для использования в контроллерах

### Важно:
- Наследуется от `OncePerRequestFilter` → выполняется один раз на запрос
- Использует JJWT 0.12.3 API (`Jwts.parser()`, `.verifyWith()`, `.parseSignedClaims()`)
- Секретный ключ берется из `application.yml` (`jwt.secret`)
- Если токен невалидный, запрос все равно продолжается (но без Authentication)

---

## 2. SecurityConfig

### Назначение
Конфигурация Spring Security для настройки цепочки фильтров и правил доступа.

### Расположение
```
src/main/java/pain_helper_back/security/SecurityConfig.java
```

### Код

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Отключаем CSRF (не нужен для stateless API)
            .csrf(csrf -> csrf.disable())
            
            // Отключаем сессии (используем JWT)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Настраиваем правила доступа
            .authorizeHttpRequests(auth -> auth
                // Публичные endpoints (без токена)
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Все остальные требуют аутентификации
                .anyRequest().authenticated()
            )
            
            // Добавляем JWT фильтр перед UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, 
                UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Что делает:
1. **Отключает CSRF** - не нужен для stateless API с JWT
2. **Отключает сессии** - используем JWT вместо сессий
3. **Определяет публичные endpoints** - `/api/auth/**` доступны без токена
4. **Требует аутентификацию** для всех остальных endpoints
5. **Добавляет JWT фильтр** в цепочку фильтров Spring Security

### Публичные endpoints:
- `/api/auth/**` - аутентификация (login, refresh, change-password)
- `/actuator/**` - мониторинг (health, metrics)
- `/swagger-ui/**` - документация API

### Защищенные endpoints:
- `/api/doctor/**` - требует токен + роль DOCTOR
- `/api/nurse/**` - требует токен + роль NURSE
- `/api/anesthesiologist/**` - требует токен + роль ANESTHESIOLOGIST
- `/api/admin/**` - требует токен + роль ADMIN

---

## 3. RoleCheckAspect

### Назначение
AOP аспект для проверки ролей пользователя на основе аннотации `@RequireRole`.

### Расположение
```
src/main/java/pain_helper_back/security/RoleCheckAspect.java
```

### Код

```java
@Aspect
@Component
@Slf4j
public class RoleCheckAspect {

    @Around("@annotation(pain_helper_back.security.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. Получаем аннотацию @RequireRole
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireRole requireRole = signature.getMethod().getAnnotation(RequireRole.class);
        String[] requiredRoles = requireRole.value();
        
        // 2. Получаем Authentication из SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Access denied: User is not authenticated");
            throw new AccessDeniedException("User is not authenticated");
        }
        
        // 3. Получаем роль пользователя
        JwtAuthenticationFilter.UserDetails userDetails = 
            (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        String userRole = userDetails.getRole();
        
        // 4. Проверяем, есть ли роль пользователя в списке разрешенных
        boolean hasRole = Arrays.asList(requiredRoles).contains(userRole);
        
        if (!hasRole) {
            log.warn("Access denied for user {} with role {}. Required roles: {}", 
                userDetails.getPersonId(), userRole, Arrays.toString(requiredRoles));
            throw new AccessDeniedException(
                "Access denied. Required roles: " + Arrays.toString(requiredRoles)
            );
        }
        
        // 5. Роль подходит → выполняем метод
        log.debug("Access granted for user {} with role {}", 
            userDetails.getPersonId(), userRole);
        return joinPoint.proceed();
    }
}
```

### Что делает:
1. **Перехватывает методы** с аннотацией `@RequireRole`
2. **Получает список разрешенных ролей** из аннотации
3. **Получает роль пользователя** из SecurityContext
4. **Проверяет соответствие** роли пользователя разрешенным ролям
5. **Бросает AccessDeniedException** если роль не подходит
6. **Выполняет метод** если роль подходит

### Пример использования:

```java
@GetMapping("/patients")
@RequireRole({"DOCTOR", "ADMIN"})  // Разрешено DOCTOR и ADMIN
public List<PatientDTO> getAllPatients() {
    ...
}

@PostMapping
@RequireRole("ADMIN")  // Разрешено только ADMIN
public PersonDTO createPerson(@RequestBody PersonDTO dto) {
    ...
}
```

---

## 4. RequireRole аннотация

### Назначение
Аннотация для указания требуемых ролей на методах контроллера.

### Расположение
```
src/main/java/pain_helper_back/security/RequireRole.java
```

### Код

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();
}
```

### Использование:

```java
// Одна роль
@RequireRole("DOCTOR")
public void doctorOnlyMethod() { }

// Несколько ролей
@RequireRole({"DOCTOR", "ADMIN"})
public void doctorOrAdminMethod() { }

// Массив ролей
@RequireRole({"NURSE", "DOCTOR", "ADMIN"})
public void multipleRolesMethod() { }
```

---

## 5. GlobalExceptionHandler

### Назначение
Централизованная обработка исключений безопасности.

### Расположение
```
src/main/java/pain_helper_back/exception/GlobalExceptionHandler.java
```

### Код

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(
                "Access Denied",
                ex.getMessage(),
                HttpStatus.FORBIDDEN.value()
            ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(
                "Unauthorized",
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value()
            ));
    }
}
```

### Обрабатывает:
- **AccessDeniedException** → 403 Forbidden (нет прав)
- **AuthenticationException** → 401 Unauthorized (не аутентифицирован)

---

## 6. Пример использования в Controller

```java
@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping("/patients")
    @RequireRole({"DOCTOR", "ADMIN"})
    public List<PatientDTO> getAllPatients(Authentication authentication) {
        // 1. Получаем данные пользователя из Authentication
        JwtAuthenticationFilter.UserDetails userDetails =
            (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        
        // 2. Логируем действие
        log.info("GET /api/doctor/patients - requestedBy={}, role={}", 
            userDetails.getPersonId(), userDetails.getRole());
        
        // 3. Выполняем бизнес-логику
        return doctorService.getAllPatients();
    }

    @PostMapping("/patients")
    @RequireRole("DOCTOR")
    public PatientDTO createPatient(
            @RequestBody @Valid PatientDTO patientDto,
            Authentication authentication) {
        
        JwtAuthenticationFilter.UserDetails userDetails =
            (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        
        log.info("POST /api/doctor/patients - createdBy={}", userDetails.getPersonId());
        
        return doctorService.createPatient(patientDto);
    }
}
```

---

## 7. Конфигурация (application.yml)

```yaml
jwt:
  secret: "my-super-secret-key-for-jwt-signing-must-be-at-least-256-bits"

spring:
  security:
    filter:
      order: -100  # JWT фильтр выполняется первым

logging:
  level:
    pain_helper_back.security: DEBUG  # Логи безопасности
```

**ВАЖНО**: `jwt.secret` должен быть одинаковым в Auth Service и Monolith!

---

## Поток обработки запроса

```
1. HTTP Request → JwtAuthenticationFilter
   ↓
2. Извлекает токен из заголовка Authorization
   ↓
3. Валидирует токен (подпись + срок)
   ↓
4. Извлекает claims (personId, role, firstName)
   ↓
5. Создает Authentication объект
   ↓
6. Сохраняет в SecurityContext
   ↓
7. SecurityConfig проверяет, требуется ли аутентификация
   ↓
8. RoleCheckAspect проверяет @RequireRole
   ↓
9. Controller получает Authentication
   ↓
10. Controller логирует действие
   ↓
11. Controller вызывает Service
   ↓
12. Response возвращается клиенту
```

---

## Преимущества архитектуры

1. **Stateless**: Не используем сессии, все данные в токене
2. **Масштабируемость**: Можно добавлять инстансы Monolith без проблем
3. **Разделение ответственностей**: Auth Service генерирует токены, Monolith валидирует
4. **Гибкость**: Легко добавлять новые роли и endpoints
5. **Логирование**: Все действия пользователей логируются с personId
6. **Безопасность**: Централизованная проверка ролей через AOP

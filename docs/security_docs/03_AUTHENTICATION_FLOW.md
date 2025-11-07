# Детальный поток аутентификации

## Сценарий 1: Первый вход в систему (Login)

### Шаг 1: Пользователь открывает приложение

```
User → Browser → http://localhost:5173
```

Frontend проверяет localStorage:
```javascript
const token = localStorage.getItem('accessToken');
if (!token) {
  // Токена нет → редирект на /login
  navigate('/login');
}
```

### Шаг 2: Пользователь вводит credentials

```
┌─────────────────────────┐
│   Login Form            │
│  ┌──────────────────┐   │
│  │ Login: doctor1   │   │
│  ├──────────────────┤   │
│  │ Password: ••••••│   │
│  ├──────────────────┤   │
│  │   [Login]        │   │
│  └──────────────────┘   │
└─────────────────────────┘
```

### Шаг 3: Frontend отправляет запрос на Auth Service

```http
POST http://localhost:8082/api/auth/login
Content-Type: application/json

{
  "login": "doctor1",
  "password": "pass123"
}
```

### Шаг 4: Auth Service обрабатывает запрос

```java
// AuthController.java
@PostMapping("/login")
public ResponseEntity<AuthResponseDTO> login(@RequestBody LoginRequestDTO request) {
    // 1. Ищем пользователя в БД
    Person person = personRepository.findByLogin(request.getLogin())
        .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    
    // 2. Проверяем пароль (BCrypt)
    if (!passwordEncoder.matches(request.getPassword(), person.getPassword())) {
        throw new BadCredentialsException("Invalid credentials");
    }
    
    // 3. Генерируем токены
    String accessToken = jwtService.generateAccessToken(person);
    String refreshToken = jwtService.generateRefreshToken(person);
    
    // 4. Возвращаем ответ
    return ResponseEntity.ok(AuthResponseDTO.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(86400)
        .personId(person.getPersonId())
        .firstName(person.getFirstName())
        .role(person.getRole())
        .temporaryCredentials(person.isTemporaryCredentials())
        .build());
}
```

### Шаг 5: Auth Service возвращает токены

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "personId": "DOC001",
  "firstName": "John",
  "role": "DOCTOR",
  "temporaryCredentials": false
}
```

### Шаг 6: Frontend сохраняет токены

```javascript
// authService.js
export const login = async (credentials) => {
  const response = await axios.post(
    'http://localhost:8082/api/auth/login',
    credentials
  );
  
  // Сохраняем в localStorage
  localStorage.setItem('accessToken', response.data.accessToken);
  localStorage.setItem('refreshToken', response.data.refreshToken);
  localStorage.setItem('personId', response.data.personId);
  localStorage.setItem('role', response.data.role);
  localStorage.setItem('firstName', response.data.firstName);
  
  return response.data;
};
```

### Шаг 7: Frontend редиректит по роли

```javascript
// Login.jsx
const handleLogin = async (credentials) => {
  try {
    const data = await authService.login(credentials);
    
    // Редирект по роли
    switch (data.role) {
      case 'ADMIN':
        navigate('/admin/dashboard');
        break;
      case 'DOCTOR':
        navigate('/doctor/patients');
        break;
      case 'NURSE':
        navigate('/nurse/patients');
        break;
      case 'ANESTHESIOLOGIST':
        navigate('/anesthesiologist/escalations');
        break;
    }
  } catch (error) {
    setError('Invalid credentials');
  }
};
```

---

## Сценарий 2: Запрос к защищенному API

### Шаг 1: Пользователь нажимает "Получить пациентов"

```javascript
// PatientsPage.jsx
useEffect(() => {
  const fetchPatients = async () => {
    const patients = await patientService.getAllPatients();
    setPatients(patients);
  };
  fetchPatients();
}, []);
```

### Шаг 2: Frontend добавляет токен в запрос

```javascript
// api/axiosConfig.js
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### Шаг 3: Запрос отправляется на Monolith

```http
GET http://localhost:8080/api/doctor/patients
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Шаг 4: JwtAuthenticationFilter перехватывает запрос

```java
// JwtAuthenticationFilter.java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) {
    try {
        // 1. Извлекаем токен из заголовка
        String token = extractTokenFromRequest(request);
        // "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        
        if (token != null && validateToken(token)) {
            // 2. Валидируем токен (подпись + срок)
            Claims claims = extractAllClaims(token);
            
            // 3. Извлекаем данные пользователя
            String personId = claims.get("personId", String.class);  // "DOC001"
            String role = claims.get("role", String.class);          // "DOCTOR"
            String firstName = claims.get("firstName", String.class); // "John"
            
            // 4. Создаем UserDetails
            UserDetails userDetails = new UserDetails();
            userDetails.setPersonId(personId);
            userDetails.setRole(role);
            userDetails.setFirstName(firstName);
            
            // 5. Создаем Authentication объект
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    personId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );
            authentication.setDetails(userDetails);
            
            // 6. Сохраняем в SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    } catch (Exception e) {
        log.error("JWT authentication failed: {}", e.getMessage());
    }
    
    // 7. Передаем запрос дальше по цепочке фильтров
    filterChain.doFilter(request, response);
}
```

### Шаг 5: RoleCheckAspect проверяет роль

```java
// RoleCheckAspect.java
@Around("@annotation(pain_helper_back.security.RequireRole)")
public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
    // 1. Получаем аннотацию @RequireRole
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    RequireRole requireRole = signature.getMethod().getAnnotation(RequireRole.class);
    String[] requiredRoles = requireRole.value();  // ["DOCTOR", "ADMIN"]
    
    // 2. Получаем Authentication из SecurityContext
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    
    if (authentication == null || !authentication.isAuthenticated()) {
        throw new AccessDeniedException("User is not authenticated");
    }
    
    // 3. Получаем роль пользователя
    UserDetails userDetails = (UserDetails) authentication.getDetails();
    String userRole = userDetails.getRole();  // "DOCTOR"
    
    // 4. Проверяем, есть ли роль пользователя в списке разрешенных
    boolean hasRole = Arrays.asList(requiredRoles).contains(userRole);
    
    if (!hasRole) {
        throw new AccessDeniedException(
            "Access denied. Required roles: " + Arrays.toString(requiredRoles)
        );
    }
    
    // 5. Роль подходит → выполняем метод
    return joinPoint.proceed();
}
```

### Шаг 6: Controller обрабатывает запрос

```java
// DoctorController.java
@GetMapping("/patients")
@RequireRole({"DOCTOR", "ADMIN"})
public List<PatientDTO> searchPatients(
        @RequestParam(required = false) String firstName,
        @RequestParam(required = false) String lastName,
        Authentication authentication) {
    
    // 1. Получаем данные пользователя из Authentication
    JwtAuthenticationFilter.UserDetails userDetails =
        (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
    
    // 2. Логируем действие
    log.info("GET /api/doctor/patients - requestedBy={}", userDetails.getPersonId());
    // "GET /api/doctor/patients - requestedBy=DOC001"
    
    // 3. Вызываем Service
    return doctorService.searchPatients(firstName, lastName, ...);
}
```

### Шаг 7: Monolith возвращает данные

```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "mrn": "MRN001",
    "firstName": "Alice",
    "lastName": "Smith",
    ...
  },
  {
    "mrn": "MRN002",
    "firstName": "Bob",
    "lastName": "Johnson",
    ...
  }
]
```

### Шаг 8: Frontend отображает данные

```javascript
// PatientsPage.jsx
const [patients, setPatients] = useState([]);

useEffect(() => {
  const fetchPatients = async () => {
    const data = await patientService.getAllPatients();
    setPatients(data);  // Обновляем state
  };
  fetchPatients();
}, []);

return (
  <div>
    {patients.map(patient => (
      <PatientCard key={patient.mrn} patient={patient} />
    ))}
  </div>
);
```

---

## Сценарий 3: Токен истек (401 Unauthorized)

### Шаг 1: Access Token истек

```
Время создания токена: 2025-11-06 10:00:00
Срок действия: 24 часа
Текущее время: 2025-11-07 10:00:01
→ Токен истек!
```

### Шаг 2: Monolith возвращает 401

```java
// JwtAuthenticationFilter.java
private boolean validateToken(String token) {
    try {
        extractAllClaims(token);  // Проверяет exp claim
        return true;
    } catch (ExpiredJwtException e) {
        log.error("JWT token expired");
        return false;  // Токен истек
    }
}
```

```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "error": "Unauthorized",
  "message": "JWT token expired"
}
```

### Шаг 3: Frontend Interceptor перехватывает 401

```javascript
// api/axiosConfig.js
axios.interceptors.response.use(
  response => response,
  async error => {
    const originalRequest = error.config;
    
    // Проверяем, что это 401 и мы еще не пытались обновить токен
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        // Пытаемся обновить токен
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await axios.post(
          'http://localhost:8082/api/auth/refresh',
          { refreshToken }
        );
        
        // Сохраняем новый accessToken
        const newAccessToken = response.data.accessToken;
        localStorage.setItem('accessToken', newAccessToken);
        
        // Обновляем заголовок в оригинальном запросе
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;
        
        // Повторяем оригинальный запрос с новым токеном
        return axios(originalRequest);
      } catch (refreshError) {
        // Refresh token тоже истек → выходим из системы
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      }
    }
    
    return Promise.reject(error);
  }
);
```

### Шаг 4: Auth Service обновляет токен

```java
// AuthController.java
@PostMapping("/refresh")
public ResponseEntity<AuthResponseDTO> refresh(@RequestBody RefreshRequestDTO request) {
    // 1. Валидируем refresh token
    if (!jwtService.validateToken(request.getRefreshToken())) {
        throw new BadCredentialsException("Invalid refresh token");
    }
    
    // 2. Извлекаем personId из refresh token
    String personId = jwtService.extractPersonId(request.getRefreshToken());
    
    // 3. Ищем пользователя в БД
    Person person = personRepository.findByPersonId(personId)
        .orElseThrow(() -> new BadCredentialsException("User not found"));
    
    // 4. Генерируем новый access token
    String newAccessToken = jwtService.generateAccessToken(person);
    
    // 5. Возвращаем новый токен
    return ResponseEntity.ok(AuthResponseDTO.builder()
        .accessToken(newAccessToken)
        .refreshToken(request.getRefreshToken())  // Старый refresh token
        .tokenType("Bearer")
        .expiresIn(86400)
        .personId(person.getPersonId())
        .firstName(person.getFirstName())
        .role(person.getRole())
        .build());
}
```

### Шаг 5: Frontend повторяет запрос

```http
GET http://localhost:8080/api/doctor/patients
Authorization: Bearer <NEW_ACCESS_TOKEN>
```

Теперь запрос успешно выполняется с новым токеном.

---

## Сценарий 4: Нет прав доступа (403 Forbidden)

### Шаг 1: Медсестра пытается создать пользователя

```javascript
// Frontend (Nurse пытается создать пользователя)
const createUser = async (userData) => {
  await axios.post('http://localhost:8080/api/admin/persons', userData);
};
```

### Шаг 2: RoleCheckAspect блокирует запрос

```java
// AdminController.java
@PostMapping
@RequireRole("ADMIN")  // Требуется роль ADMIN
public PersonDTO createPerson(@RequestBody PersonRegisterRequestDTO dto) {
    ...
}
```

```java
// RoleCheckAspect.java
String userRole = userDetails.getRole();  // "NURSE"
String[] requiredRoles = {"ADMIN"};

boolean hasRole = Arrays.asList(requiredRoles).contains(userRole);
// false → медсестра не является админом

if (!hasRole) {
    throw new AccessDeniedException(
        "Access denied. Required roles: [ADMIN]"
    );
}
```

### Шаг 3: GlobalExceptionHandler обрабатывает ошибку

```java
// GlobalExceptionHandler.java
@ExceptionHandler(AccessDeniedException.class)
public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(new ErrorResponse("Access denied", ex.getMessage()));
}
```

### Шаг 4: Frontend получает 403

```http
HTTP/1.1 403 Forbidden
Content-Type: application/json

{
  "error": "Access denied",
  "message": "Access denied. Required roles: [ADMIN]"
}
```

### Шаг 5: Frontend показывает ошибку

```javascript
try {
  await createUser(userData);
} catch (error) {
  if (error.response?.status === 403) {
    toast.error('У вас нет прав для выполнения этого действия');
  }
}
```

---

## Сценарий 5: Logout (Выход из системы)

### Шаг 1: Пользователь нажимает "Выйти"

```javascript
// Header.jsx
const handleLogout = () => {
  authService.logout();
  navigate('/login');
};
```

### Шаг 2: Frontend очищает localStorage

```javascript
// authService.js
export const logout = () => {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('personId');
  localStorage.removeItem('role');
  localStorage.removeItem('firstName');
  
  // Или просто:
  localStorage.clear();
};
```

### Шаг 3: Редирект на страницу логина

```javascript
navigate('/login');
```

**Примечание**: В текущей реализации logout происходит только на клиенте. Токен остается валидным до истечения срока действия. Для более безопасного logout можно реализовать:
- **Token blacklist** на сервере
- **Token revocation endpoint** в Auth Service
- **Короткий срок жизни** access token (15 минут)

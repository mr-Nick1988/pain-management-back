# Архитектура безопасности системы Pain Management

## Общая схема взаимодействия

```
┌─────────────────────────────────────────────────────────────────────┐
│                          FRONTEND (React)                            │
│                     http://localhost:5173                            │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             │ 1. POST /api/auth/login
                             │    {login, password}
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│              AUTHENTICATION SERVICE (Микросервис)                    │
│                     http://localhost:8082                            │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  • Проверяет credentials в БД                                 │  │
│  │  • Генерирует JWT токены (access + refresh)                   │  │
│  │  • Возвращает: accessToken, refreshToken, personId, role      │  │
│  └───────────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             │ 2. Токены сохраняются в localStorage
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          FRONTEND (React)                            │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  localStorage:                                                 │  │
│  │    - accessToken: "eyJhbGc..."                                │  │
│  │    - refreshToken: "eyJhbGc..."                               │  │
│  │    - personId: "DOC001"                                       │  │
│  │    - role: "DOCTOR"                                           │  │
│  └───────────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             │ 3. Запрос к API с токеном
                             │    Authorization: Bearer eyJhbGc...
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    MONOLITH API (Spring Boot)                        │
│                     http://localhost:8080                            │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  JwtAuthenticationFilter (OncePerRequestFilter)               │  │
│  │  ┌─────────────────────────────────────────────────────────┐  │  │
│  │  │ 1. Извлекает токен из заголовка Authorization          │  │  │
│  │  │ 2. Валидирует токен (подпись, срок действия)           │  │  │
│  │  │ 3. Извлекает claims: personId, role, firstName         │  │  │
│  │  │ 4. Создает Authentication объект                       │  │  │
│  │  │ 5. Сохраняет в SecurityContext                         │  │  │
│  │  └─────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                             │                                        │
│                             ▼                                        │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  RoleCheckAspect (@Aspect)                                    │  │
│  │  ┌─────────────────────────────────────────────────────────┐  │  │
│  │  │ 1. Перехватывает методы с @RequireRole                 │  │  │
│  │  │ 2. Проверяет роль из Authentication                    │  │  │
│  │  │ 3. Если роль не подходит → AccessDeniedException       │  │  │
│  │  │ 4. Если роль подходит → выполняет метод                │  │  │
│  │  └─────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                             │                                        │
│                             ▼                                        │
│  ┌───────────────────────────────────────────────────────────────┐  │
│  │  Controller (DoctorController, NurseController, etc.)         │  │
│  │  ┌─────────────────────────────────────────────────────────┐  │  │
│  │  │ • Получает Authentication из параметра                 │  │  │
│  │  │ • Извлекает UserDetails из authentication.getDetails() │  │  │
│  │  │ • Логирует действие пользователя                       │  │  │
│  │  │ • Выполняет бизнес-логику                              │  │  │
│  │  └─────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

## Ключевые компоненты

### 1. Authentication Service (Микросервис)
- **Порт**: 8082
- **Ответственность**: Аутентификация пользователей, генерация JWT токенов
- **База данных**: Своя БД с таблицей Person (credentials)
- **Не знает** о бизнес-логике Pain Management

### 2. Monolith API (Spring Boot)
- **Порт**: 8080
- **Ответственность**: Бизнес-логика, управление пациентами, рекомендациями
- **База данных**: Своя БД с Patient, Recommendation, VAS, EMR
- **Не хранит** credentials, только валидирует JWT токены

### 3. Frontend (React)
- **Порт**: 5173
- **Ответственность**: UI, хранение токенов, отправка запросов с токенами
- **Взаимодействует** с обоими сервисами

## Разделение ответственности

| Компонент | Что делает | Что НЕ делает |
|-----------|------------|---------------|
| **Auth Service** | • Проверяет login/password<br>• Генерирует JWT<br>• Обновляет токены<br>• Меняет пароль | • Не знает о пациентах<br>• Не знает о рекомендациях<br>• Не валидирует бизнес-запросы |
| **Monolith** | • Валидирует JWT<br>• Проверяет роли<br>• Выполняет бизнес-логику<br>• Управляет данными | • Не проверяет пароли<br>• Не генерирует токены<br>• Не хранит credentials |
| **Frontend** | • Хранит токены<br>• Добавляет токен в запросы<br>• Обрабатывает 401 ошибки<br>• Обновляет токены | • Не проверяет роли на клиенте<br>• Не валидирует токены |

## Поток данных при логине

```
1. User вводит login/password
   ↓
2. Frontend → POST http://localhost:8082/api/auth/login
   Body: {login: "doctor1", password: "pass123"}
   ↓
3. Auth Service проверяет credentials в БД
   ↓
4. Auth Service генерирует JWT токены
   ↓
5. Auth Service → Frontend
   Response: {
     accessToken: "eyJhbGc...",
     refreshToken: "eyJhbGc...",
     personId: "DOC001",
     role: "DOCTOR",
     firstName: "John"
   }
   ↓
6. Frontend сохраняет в localStorage
   ↓
7. Frontend редиректит на /doctor/patients
```

## Поток данных при API запросе

```
1. User нажимает "Получить список пациентов"
   ↓
2. Frontend → GET http://localhost:8080/api/doctor/patients
   Headers: {Authorization: "Bearer eyJhbGc..."}
   ↓
3. Monolith: JwtAuthenticationFilter перехватывает запрос
   ↓
4. JwtAuthenticationFilter валидирует токен (подпись + срок)
   ↓
5. JwtAuthenticationFilter извлекает claims (personId, role)
   ↓
6. JwtAuthenticationFilter создает Authentication объект
   ↓
7. RoleCheckAspect проверяет @RequireRole("DOCTOR")
   ↓
8. DoctorController получает Authentication
   ↓
9. DoctorController логирует: "GET /api/doctor/patients - requestedBy=DOC001"
   ↓
10. DoctorController вызывает Service
   ↓
11. Service возвращает данные
   ↓
12. Monolith → Frontend
   Response: [{patient1}, {patient2}, ...]
```

## Обработка ошибок

### 401 Unauthorized (токен истек)
```
1. Monolith возвращает 401
   ↓
2. Frontend Axios Interceptor перехватывает
   ↓
3. Frontend → POST http://localhost:8082/api/auth/refresh
   Body: {refreshToken: "eyJhbGc..."}
   ↓
4. Auth Service генерирует новый accessToken
   ↓
5. Frontend сохраняет новый accessToken
   ↓
6. Frontend повторяет оригинальный запрос
```

### 403 Forbidden (нет прав)
```
1. RoleCheckAspect бросает AccessDeniedException
   ↓
2. GlobalExceptionHandler перехватывает
   ↓
3. Monolith → Frontend
   Response: 403 {message: "Access denied. Required role: ADMIN"}
   ↓
4. Frontend показывает ошибку пользователю
```

## Безопасность

### Что защищает систему:
1. **JWT подпись**: Токены подписаны секретным ключом, подделка невозможна
2. **Срок действия**: accessToken живет 24 часа, refreshToken - 7 дней
3. **Проверка ролей**: Каждый endpoint защищен @RequireRole
4. **HTTPS** (в production): Токены передаются по защищенному каналу
5. **CORS**: Только разрешенные домены могут делать запросы

### Что НЕ защищает (и это нормально):
1. **XSS**: Токены в localStorage уязвимы к XSS (решение: httpOnly cookies)
2. **CSRF**: Не защищает от CSRF (решение: CSRF токены или SameSite cookies)
3. **Replay attacks**: Старый токен можно переиспользовать до истечения срока

## Следующие шаги

Читайте документацию в следующем порядке:
1. `02_JWT_TOKEN_STRUCTURE.md` - Структура JWT токена
2. `03_AUTHENTICATION_FLOW.md` - Детальный поток аутентификации
3. `04_MONOLITH_SECURITY.md` - Компоненты безопасности в Monolith
4. `05_FRONTEND_INTEGRATION.md` - Интеграция с фронтендом
5. `06_TESTING_GUIDE.md` - Как тестировать систему

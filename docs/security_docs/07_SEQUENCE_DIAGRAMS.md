# Диаграммы последовательности (Sequence Diagrams)

## 1. Полный поток логина и запроса к API

```
┌─────────┐         ┌──────────┐         ┌──────────────┐         ┌──────────┐
│ Browser │         │ Frontend │         │ Auth Service │         │ Monolith │
└────┬────┘         └────┬─────┘         └──────┬───────┘         └────┬─────┘
     │                   │                       │                      │
     │ 1. Открыть /login │                       │                      │
     ├──────────────────>│                       │                      │
     │                   │                       │                      │
     │ 2. Показать форму │                       │                      │
     │<──────────────────┤                       │                      │
     │                   │                       │                      │
     │ 3. Ввести login/pass                      │                      │
     ├──────────────────>│                       │                      │
     │                   │                       │                      │
     │                   │ 4. POST /api/auth/login                      │
     │                   │      {login, password}│                      │
     │                   ├──────────────────────>│                      │
     │                   │                       │                      │
     │                   │                       │ 5. Проверить credentials
     │                   │                       │    в БД              │
     │                   │                       │                      │
     │                   │                       │ 6. Генерировать JWT  │
     │                   │                       │    (access + refresh)│
     │                   │                       │                      │
     │                   │ 7. 200 OK             │                      │
     │                   │    {accessToken,      │                      │
     │                   │     refreshToken,     │                      │
     │                   │     personId, role}   │                      │
     │                   │<──────────────────────┤                      │
     │                   │                       │                      │
     │                   │ 8. Сохранить токены   │                      │
     │                   │    в localStorage     │                      │
     │                   │                       │                      │
     │ 9. Редирект /doctor/patients              │                      │
     │<──────────────────┤                       │                      │
     │                   │                       │                      │
     │ 10. Загрузить страницу                    │                      │
     ├──────────────────>│                       │                      │
     │                   │                       │                      │
     │                   │ 11. GET /api/doctor/patients                 │
     │                   │     Authorization: Bearer <token>            │
     │                   ├─────────────────────────────────────────────>│
     │                   │                       │                      │
     │                   │                       │ 12. JwtAuthenticationFilter
     │                   │                       │     • Извлечь токен  │
     │                   │                       │     • Валидировать   │
     │                   │                       │     • Создать Auth   │
     │                   │                       │                      │
     │                   │                       │ 13. RoleCheckAspect  │
     │                   │                       │     • Проверить роль │
     │                   │                       │                      │
     │                   │                       │ 14. DoctorController │
     │                   │                       │     • Логировать     │
     │                   │                       │     • Вызвать Service│
     │                   │                       │                      │
     │                   │ 15. 200 OK            │                      │
     │                   │     [{patient1}, ...]  │                      │
     │                   │<─────────────────────────────────────────────┤
     │                   │                       │                      │
     │ 16. Отобразить пациентов                  │                      │
     │<──────────────────┤                       │                      │
     │                   │                       │                      │
```

---

## 2. Обновление токена при 401 Unauthorized

```
┌──────────┐         ┌──────────────┐         ┌──────────┐
│ Frontend │         │ Auth Service │         │ Monolith │
└────┬─────┘         └──────┬───────┘         └────┬─────┘
     │                      │                      │
     │ 1. GET /api/doctor/patients                 │
     │    Authorization: Bearer <EXPIRED_TOKEN>    │
     ├────────────────────────────────────────────>│
     │                      │                      │
     │                      │ 2. JwtAuthenticationFilter
     │                      │    • Токен истек     │
     │                      │    • exp < now       │
     │                      │                      │
     │ 3. 401 Unauthorized  │                      │
     │<────────────────────────────────────────────┤
     │                      │                      │
     │ 4. Axios Interceptor │                      │
     │    перехватывает 401 │                      │
     │                      │                      │
     │ 5. POST /api/auth/refresh                   │
     │    {refreshToken}    │                      │
     ├─────────────────────>│                      │
     │                      │                      │
     │                      │ 6. Валидировать      │
     │                      │    refreshToken      │
     │                      │                      │
     │                      │ 7. Генерировать      │
     │                      │    новый accessToken │
     │                      │                      │
     │ 8. 200 OK            │                      │
     │    {accessToken}     │                      │
     │<─────────────────────┤                      │
     │                      │                      │
     │ 9. Сохранить новый   │                      │
     │    accessToken в     │                      │
     │    localStorage      │                      │
     │                      │                      │
     │ 10. Повторить оригинальный запрос           │
     │     Authorization: Bearer <NEW_TOKEN>       │
     ├────────────────────────────────────────────>│
     │                      │                      │
     │                      │ 11. Токен валидный   │
     │                      │                      │
     │ 12. 200 OK           │                      │
     │     [{patient1}, ...] │                      │
     │<────────────────────────────────────────────┤
     │                      │                      │
```

---

## 3. Отказ в доступе (403 Forbidden)

```
┌──────────┐         ┌──────────┐
│ Frontend │         │ Monolith │
│ (NURSE)  │         │          │
└────┬─────┘         └────┬─────┘
     │                    │
     │ 1. POST /api/admin/persons (создать пользователя)
     │    Authorization: Bearer <NURSE_TOKEN>
     ├───────────────────>│
     │                    │
     │                    │ 2. JwtAuthenticationFilter
     │                    │    • Токен валидный
     │                    │    • role = "NURSE"
     │                    │    • Создать Authentication
     │                    │
     │                    │ 3. RoleCheckAspect
     │                    │    @RequireRole("ADMIN")
     │                    │    • userRole = "NURSE"
     │                    │    • requiredRoles = ["ADMIN"]
     │                    │    • "NURSE" ∉ ["ADMIN"]
     │                    │    • throw AccessDeniedException
     │                    │
     │                    │ 4. GlobalExceptionHandler
     │                    │    • Перехватить исключение
     │                    │    • Вернуть 403
     │                    │
     │ 5. 403 Forbidden   │
     │    {error: "Access denied. Required roles: [ADMIN]"}
     │<───────────────────┤
     │                    │
     │ 6. Показать ошибку │
     │    "У вас нет прав"│
     │                    │
```

---

## 4. Детальный поток JwtAuthenticationFilter

```
┌─────────────┐         ┌──────────────────────┐         ┌─────────────────┐
│ HTTP Request│         │ JwtAuthenticationFilter│         │ SecurityContext │
└──────┬──────┘         └──────────┬───────────┘         └────────┬────────┘
       │                           │                              │
       │ 1. GET /api/doctor/patients                              │
       │    Authorization: Bearer eyJhbGc...                      │
       ├──────────────────────────>│                              │
       │                           │                              │
       │                           │ 2. extractTokenFromRequest() │
       │                           │    • Получить заголовок      │
       │                           │      "Authorization"         │
       │                           │    • Извлечь токен после     │
       │                           │      "Bearer "               │
       │                           │    → "eyJhbGc..."            │
       │                           │                              │
       │                           │ 3. validateToken(token)      │
       │                           │    • extractAllClaims()      │
       │                           │    • Проверить подпись       │
       │                           │    • Проверить exp claim     │
       │                           │    → true/false              │
       │                           │                              │
       │                           │ 4. extractAllClaims(token)   │
       │                           │    • Jwts.parser()           │
       │                           │    • .verifyWith(key)        │
       │                           │    • .parseSignedClaims()    │
       │                           │    → Claims object           │
       │                           │                              │
       │                           │ 5. Извлечь данные из claims  │
       │                           │    • personId = "DOC001"     │
       │                           │    • role = "DOCTOR"         │
       │                           │    • firstName = "John"      │
       │                           │                              │
       │                           │ 6. Создать UserDetails       │
       │                           │    userDetails.setPersonId() │
       │                           │    userDetails.setRole()     │
       │                           │    userDetails.setFirstName()│
       │                           │                              │
       │                           │ 7. Создать Authentication    │
       │                           │    new UsernamePasswordAuthenticationToken(
       │                           │      personId,               │
       │                           │      null,                   │
       │                           │      [ROLE_DOCTOR]           │
       │                           │    )                         │
       │                           │    authentication.setDetails(userDetails)
       │                           │                              │
       │                           │ 8. Сохранить в SecurityContext
       │                           ├─────────────────────────────>│
       │                           │                              │
       │                           │ 9. filterChain.doFilter()    │
       │                           │    → Передать запрос дальше  │
       │                           │                              │
```

---

## 5. Детальный поток RoleCheckAspect

```
┌────────────┐         ┌───────────────┐         ┌─────────────────┐
│ Controller │         │ RoleCheckAspect│         │ SecurityContext │
└─────┬──────┘         └───────┬───────┘         └────────┬────────┘
      │                        │                          │
      │ @RequireRole({"DOCTOR", "ADMIN"})                 │
      │ public List<PatientDTO> getAllPatients() {...}    │
      │                        │                          │
      │ 1. Вызов метода        │                          │
      ├───────────────────────>│                          │
      │                        │                          │
      │                        │ 2. @Around перехватывает │
      │                        │    вызов метода          │
      │                        │                          │
      │                        │ 3. Получить аннотацию    │
      │                        │    @RequireRole          │
      │                        │    requiredRoles =       │
      │                        │    ["DOCTOR", "ADMIN"]   │
      │                        │                          │
      │                        │ 4. Получить Authentication
      │                        ├─────────────────────────>│
      │                        │                          │
      │                        │ 5. Authentication object │
      │                        │<─────────────────────────┤
      │                        │                          │
      │                        │ 6. Проверить isAuthenticated()
      │                        │    → true                │
      │                        │                          │
      │                        │ 7. Получить UserDetails  │
      │                        │    authentication.getDetails()
      │                        │    → userDetails         │
      │                        │                          │
      │                        │ 8. Получить роль         │
      │                        │    userRole = userDetails.getRole()
      │                        │    → "DOCTOR"            │
      │                        │                          │
      │                        │ 9. Проверить роль        │
      │                        │    "DOCTOR" ∈ ["DOCTOR", "ADMIN"]
      │                        │    → true                │
      │                        │                          │
      │                        │ 10. joinPoint.proceed()  │
      │                        │     → Выполнить метод    │
      ├───────────────────────>│                          │
      │                        │                          │
      │ 11. Выполнить бизнес-логику                       │
      │     return doctorService.getAllPatients()         │
      │                        │                          │
      │ 12. Вернуть результат  │                          │
      │<───────────────────────┤                          │
      │                        │                          │
```

---

## 6. Поток смены пароля

```
┌──────────┐         ┌──────────────┐         ┌──────────┐
│ Frontend │         │ Auth Service │         │ Database │
└────┬─────┘         └──────┬───────┘         └────┬─────┘
     │                      │                      │
     │ 1. POST /api/auth/change-password           │
     │    Authorization: Bearer <token>            │
     │    {oldPassword, newPassword}               │
     ├─────────────────────>│                      │
     │                      │                      │
     │                      │ 2. Валидировать токен│
     │                      │    • Извлечь personId│
     │                      │                      │
     │                      │ 3. Найти пользователя│
     │                      ├─────────────────────>│
     │                      │                      │
     │                      │ 4. Person object     │
     │                      │<─────────────────────┤
     │                      │                      │
     │                      │ 5. Проверить старый  │
     │                      │    пароль (BCrypt)   │
     │                      │    passwordEncoder   │
     │                      │    .matches(old, hash)
     │                      │                      │
     │                      │ 6. Хешировать новый  │
     │                      │    пароль (BCrypt)   │
     │                      │    newHash = encoder │
     │                      │    .encode(newPass)  │
     │                      │                      │
     │                      │ 7. Обновить в БД     │
     │                      ├─────────────────────>│
     │                      │                      │
     │                      │ 8. Success           │
     │                      │<─────────────────────┤
     │                      │                      │
     │ 9. 200 OK            │                      │
     │    {message: "Password changed"}            │
     │<─────────────────────┤                      │
     │                      │                      │
```

---

## 7. Взаимодействие двух сервисов

```
┌──────────┐         ┌──────────────┐         ┌──────────┐
│ Frontend │         │ Auth Service │         │ Monolith │
│          │         │ :8082        │         │ :8080    │
└────┬─────┘         └──────┬───────┘         └────┬─────┘
     │                      │                      │
     │ ФАЗА 1: АУТЕНТИФИКАЦИЯ                      │
     │ ════════════════════════════════════════    │
     │                      │                      │
     │ 1. POST /api/auth/login                     │
     ├─────────────────────>│                      │
     │                      │                      │
     │                      │ • Проверить credentials
     │                      │ • Генерировать JWT   │
     │                      │ • Вернуть токены     │
     │                      │                      │
     │ 2. {accessToken, ...}│                      │
     │<─────────────────────┤                      │
     │                      │                      │
     │ ФАЗА 2: АВТОРИЗАЦИЯ                         │
     │ ════════════════════════════════════════    │
     │                      │                      │
     │ 3. GET /api/doctor/patients                 │
     │    Authorization: Bearer <token>            │
     ├────────────────────────────────────────────>│
     │                      │                      │
     │                      │ • Валидировать токен │
     │                      │   (тот же secret key)│
     │                      │ • Проверить роль     │
     │                      │ • Выполнить запрос   │
     │                      │                      │
     │ 4. [{patients}]      │                      │
     │<────────────────────────────────────────────┤
     │                      │                      │
     │ ФАЗА 3: ОБНОВЛЕНИЕ ТОКЕНА                   │
     │ ════════════════════════════════════════    │
     │                      │                      │
     │ 5. Токен истек (401) │                      │
     │<────────────────────────────────────────────┤
     │                      │                      │
     │ 6. POST /api/auth/refresh                   │
     ├─────────────────────>│                      │
     │                      │                      │
     │ 7. {newAccessToken}  │                      │
     │<─────────────────────┤                      │
     │                      │                      │
     │ 8. Повторить запрос с новым токеном         │
     ├────────────────────────────────────────────>│
     │                      │                      │
     │ 9. [{patients}]      │                      │
     │<────────────────────────────────────────────┤
     │                      │                      │
```

---

## Ключевые моменты

### 1. Разделение ответственности
- **Auth Service**: Генерирует и обновляет токены
- **Monolith**: Валидирует токены и проверяет роли
- **Frontend**: Хранит токены и добавляет их в запросы

### 2. Stateless архитектура
- Нет сессий на сервере
- Вся информация о пользователе в JWT токене
- Можно масштабировать Monolith горизонтально

### 3. Безопасность
- Токены подписаны секретным ключом
- Подпись проверяется при каждом запросе
- Роли проверяются через AOP аспект

### 4. Автоматическое обновление
- Frontend автоматически обновляет токен при 401
- Пользователь не замечает истечения токена
- Плавный UX без логаутов

# Руководство по тестированию системы безопасности

## Тестирование с Postman

### 1. Логин (получение токенов)

**Request:**
```http
POST http://localhost:8082/api/auth/login
Content-Type: application/json

{
  "login": "doctor1",
  "password": "pass123"
}
```

**Expected Response (200 OK):**
```json
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

**Что делать дальше:**
1. Скопируйте `accessToken`
2. Используйте его в заголовке `Authorization: Bearer <accessToken>` для всех последующих запросов

---

### 2. Запрос к защищенному endpoint (с токеном)

**Request:**
```http
GET http://localhost:8080/api/doctor/patients
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Expected Response (200 OK):**
```json
[
  {
    "mrn": "MRN001",
    "firstName": "Alice",
    "lastName": "Smith",
    ...
  }
]
```

---

### 3. Запрос без токена (401 Unauthorized)

**Request:**
```http
GET http://localhost:8080/api/doctor/patients
```

**Expected Response (401 Unauthorized):**
```json
{
  "error": "Unauthorized",
  "message": "JWT token is missing or invalid"
}
```

---

### 4. Запрос с неправильной ролью (403 Forbidden)

**Scenario:** NURSE пытается создать пользователя (требуется ADMIN)

**Request:**
```http
POST http://localhost:8080/api/admin/persons
Authorization: Bearer <NURSE_TOKEN>
Content-Type: application/json

{
  "personId": "DOC999",
  "firstName": "Test",
  "lastName": "User",
  "role": "DOCTOR",
  "login": "test",
  "password": "pass123"
}
```

**Expected Response (403 Forbidden):**
```json
{
  "error": "Access Denied",
  "message": "Access denied. Required roles: [ADMIN]",
  "status": 403
}
```

---

### 5. Обновление токена (refresh)

**Request:**
```http
POST http://localhost:8082/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Expected Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  // Новый токен
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  // Старый токен
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "personId": "DOC001",
  "firstName": "John",
  "role": "DOCTOR"
}
```

---

### 6. Смена пароля

**Request:**
```http
POST http://localhost:8082/api/auth/change-password
Authorization: Bearer <ACCESS_TOKEN>
Content-Type: application/json

{
  "oldPassword": "pass123",
  "newPassword": "newpass456"
}
```

**Expected Response (200 OK):**
```json
{
  "message": "Password changed successfully"
}
```

---

## Postman Collection

### Создание Postman Collection

1. **Создайте Environment:**
   - `auth_base_url`: `http://localhost:8082`
   - `api_base_url`: `http://localhost:8080`
   - `access_token`: (будет заполнен автоматически)
   - `refresh_token`: (будет заполнен автоматически)

2. **Создайте Collection "Pain Management API"**

3. **Добавьте Pre-request Script в Collection:**
```javascript
// Автоматически добавляет токен в заголовок
if (pm.environment.get("access_token")) {
    pm.request.headers.add({
        key: 'Authorization',
        value: 'Bearer ' + pm.environment.get("access_token")
    });
}
```

4. **Добавьте Test Script для Login endpoint:**
```javascript
// Сохраняем токены в environment после логина
if (pm.response.code === 200) {
    const response = pm.response.json();
    pm.environment.set("access_token", response.accessToken);
    pm.environment.set("refresh_token", response.refreshToken);
    pm.environment.set("person_id", response.personId);
    pm.environment.set("role", response.role);
}
```

---

## Тестирование ролей

### ADMIN роль

**Что может:**
- ✅ Создавать/удалять пользователей (`POST /api/admin/persons`)
- ✅ Просматривать всех пациентов (`GET /api/admin/persons/patients`)
- ✅ Доступ ко всем endpoints других ролей

**Что не может:**
- ❌ Ничего (ADMIN имеет полный доступ)

**Тестовые запросы:**
```http
# Создать пользователя (только ADMIN)
POST http://localhost:8080/api/admin/persons
Authorization: Bearer <ADMIN_TOKEN>

# Получить всех пользователей (только ADMIN)
GET http://localhost:8080/api/admin/persons
Authorization: Bearer <ADMIN_TOKEN>
```

---

### DOCTOR роль

**Что может:**
- ✅ Управлять пациентами (`/api/doctor/patients`)
- ✅ Одобрять/отклонять рекомендации (`POST /api/doctor/recommendations/{id}/approve`)
- ✅ Просматривать историю пациентов (`GET /api/doctor/patients/{mrn}/history`)

**Что не может:**
- ❌ Создавать пользователей (`POST /api/admin/persons`)
- ❌ Вводить VAS (`POST /api/nurse/patients/{mrn}/vas`)
- ❌ Одобрять эскалации (`POST /api/anesthesiologist/recommendations/{id}/approve`)

**Тестовые запросы:**
```http
# Получить пациентов (DOCTOR или ADMIN)
GET http://localhost:8080/api/doctor/patients
Authorization: Bearer <DOCTOR_TOKEN>

# Одобрить рекомендацию (только DOCTOR)
POST http://localhost:8080/api/doctor/recommendations/1/approve
Authorization: Bearer <DOCTOR_TOKEN>
Content-Type: application/json

{
  "comment": "Approved",
  "approvedBy": "DOC001"
}
```

---

### NURSE роль

**Что может:**
- ✅ Управлять пациентами (`/api/nurse/patients`)
- ✅ Вводить VAS (`POST /api/nurse/patients/{mrn}/vas`)
- ✅ Создавать рекомендации (`POST /api/nurse/patients/{mrn}/recommendation`)
- ✅ Выполнять рекомендации (`POST /api/nurse/patients/{mrn}/recommendation/execute`)

**Что не может:**
- ❌ Одобрять рекомендации (`POST /api/doctor/recommendations/{id}/approve`)
- ❌ Создавать пользователей (`POST /api/admin/persons`)

**Тестовые запросы:**
```http
# Создать VAS (только NURSE)
POST http://localhost:8080/api/nurse/patients/MRN001/vas
Authorization: Bearer <NURSE_TOKEN>
Content-Type: application/json

{
  "painLevel": 7,
  "location": "Lower back",
  "recordedBy": "NURSE001"
}

# Создать рекомендацию (только NURSE)
POST http://localhost:8080/api/nurse/patients/MRN001/recommendation
Authorization: Bearer <NURSE_TOKEN>
```

---

### ANESTHESIOLOGIST роль

**Что может:**
- ✅ Просматривать эскалации (`GET /api/anesthesiologist/escalations`)
- ✅ Одобрять/отклонять эскалации (`POST /api/anesthesiologist/recommendations/{id}/approve`)
- ✅ Создавать протоколы (`POST /api/anesthesiologist/recommendations`)
- ✅ Обновлять рекомендации (`PUT /api/anesthesiologist/recommendations/{id}/update`)

**Что не может:**
- ❌ Вводить VAS (`POST /api/nurse/patients/{mrn}/vas`)
- ❌ Создавать пользователей (`POST /api/admin/persons`)

**Тестовые запросы:**
```http
# Получить эскалации (только ANESTHESIOLOGIST)
GET http://localhost:8080/api/anesthesiologist/escalations
Authorization: Bearer <ANESTHESIOLOGIST_TOKEN>

# Одобрить эскалацию (только ANESTHESIOLOGIST)
POST http://localhost:8080/api/anesthesiologist/recommendations/1/approve
Authorization: Bearer <ANESTHESIOLOGIST_TOKEN>
Content-Type: application/json

{
  "comment": "Escalation approved",
  "approvedBy": "ANEST001"
}
```

---

## Тестирование с curl

### 1. Логин
```bash
curl -X POST http://localhost:8082/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"login":"doctor1","password":"pass123"}'
```

### 2. Запрос с токеном
```bash
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

curl -X GET http://localhost:8080/api/doctor/patients \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Создание пациента
```bash
curl -X POST http://localhost:8080/api/doctor/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Alice",
    "lastName": "Smith",
    "birthDate": "1990-01-01",
    "gender": "FEMALE"
  }'
```

---

## Тестирование Frontend

### 1. Проверка редиректа при отсутствии токена

**Шаги:**
1. Откройте браузер в режиме инкогнито
2. Перейдите на `http://localhost:5173/doctor/patients`
3. **Ожидается:** Редирект на `/login`

### 2. Проверка логина

**Шаги:**
1. Откройте `http://localhost:5173/login`
2. Введите credentials: `doctor1` / `pass123`
3. Нажмите "Login"
4. **Ожидается:** Редирект на `/doctor/patients`
5. Откройте DevTools → Application → Local Storage
6. **Ожидается:** Видны `accessToken`, `refreshToken`, `personId`, `role`

### 3. Проверка автоматического обновления токена

**Шаги:**
1. Залогиньтесь как `doctor1`
2. Откройте DevTools → Application → Local Storage
3. Измените `accessToken` на невалидное значение (например, `"invalid"`)
4. Обновите страницу или сделайте любой запрос
5. **Ожидается:** 
   - Axios interceptor перехватит 401
   - Обновит токен через `/api/auth/refresh`
   - Повторит запрос с новым токеном

### 4. Проверка защиты роутов по ролям

**Шаги:**
1. Залогиньтесь как `nurse1` (роль NURSE)
2. Попробуйте перейти на `/admin/dashboard`
3. **Ожидается:** Редирект на `/forbidden`

### 5. Проверка logout

**Шаги:**
1. Залогиньтесь как любой пользователь
2. Нажмите кнопку "Logout"
3. **Ожидается:**
   - localStorage очищен
   - Редирект на `/login`
   - Попытка перейти на защищенный роут → редирект на `/login`

---

## Автоматизированные тесты (Jest)

### Тест authService

```javascript
// src/services/__tests__/authService.test.js
import authService from '../authService';
import axios from 'axios';

jest.mock('axios');

describe('AuthService', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test('login should save tokens to localStorage', async () => {
    const mockResponse = {
      data: {
        accessToken: 'access123',
        refreshToken: 'refresh123',
        personId: 'DOC001',
        role: 'DOCTOR',
        firstName: 'John'
      }
    };
    axios.post.mockResolvedValue(mockResponse);

    await authService.login({ login: 'doctor1', password: 'pass123' });

    expect(localStorage.getItem('accessToken')).toBe('access123');
    expect(localStorage.getItem('refreshToken')).toBe('refresh123');
    expect(localStorage.getItem('personId')).toBe('DOC001');
    expect(localStorage.getItem('role')).toBe('DOCTOR');
  });

  test('logout should clear localStorage', () => {
    localStorage.setItem('accessToken', 'token');
    localStorage.setItem('refreshToken', 'refresh');

    authService.logout();

    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  test('isAuthenticated should return true if token exists', () => {
    localStorage.setItem('accessToken', 'token');
    expect(authService.isAuthenticated()).toBe(true);
  });

  test('isAuthenticated should return false if token does not exist', () => {
    expect(authService.isAuthenticated()).toBe(false);
  });
});
```

---

## Проверка логов

### Backend логи (Monolith)

После запроса вы должны видеть логи:

```
2025-11-06 19:00:00 DEBUG JwtAuthenticationFilter - JWT authenticated: personId=DOC001, role=DOCTOR
2025-11-06 19:00:00 DEBUG RoleCheckAspect - Access granted for user DOC001 with role DOCTOR
2025-11-06 19:00:00 INFO  DoctorController - GET /api/doctor/patients - requestedBy=DOC001
```

### Если токен невалидный:

```
2025-11-06 19:00:00 ERROR JwtAuthenticationFilter - Invalid JWT token: JWT signature does not match
2025-11-06 19:00:00 WARN  RoleCheckAspect - Access denied: User is not authenticated
```

### Если роль не подходит:

```
2025-11-06 19:00:00 WARN  RoleCheckAspect - Access denied for user NURSE001 with role NURSE. Required roles: [ADMIN]
```

---

## Чек-лист тестирования

### Аутентификация
- [ ] Логин с правильными credentials → 200 OK + токены
- [ ] Логин с неправильными credentials → 401 Unauthorized
- [ ] Refresh token → 200 OK + новый accessToken
- [ ] Refresh с невалидным токеном → 401 Unauthorized

### Авторизация
- [ ] Запрос с валидным токеном → 200 OK
- [ ] Запрос без токена → 401 Unauthorized
- [ ] Запрос с истекшим токеном → 401 Unauthorized
- [ ] Запрос с неправильной ролью → 403 Forbidden

### Роли
- [ ] ADMIN может создавать пользователей
- [ ] DOCTOR может одобрять рекомендации
- [ ] NURSE может вводить VAS
- [ ] ANESTHESIOLOGIST может одобрять эскалации
- [ ] NURSE не может создавать пользователей (403)
- [ ] DOCTOR не может одобрять эскалации (403)

### Frontend
- [ ] Редирект на /login при отсутствии токена
- [ ] Редирект по роли после логина
- [ ] Автоматическое обновление токена при 401
- [ ] Logout очищает localStorage
- [ ] ProtectedRoute блокирует доступ по ролям

---

## Troubleshooting

### Проблема: 401 Unauthorized при валидном токене

**Причины:**
1. Разные секретные ключи в Auth Service и Monolith
2. Токен истек
3. Токен поврежден при копировании

**Решение:**
1. Проверьте `jwt.secret` в `application.yml` обоих сервисов
2. Сгенерируйте новый токен через `/api/auth/login`
3. Убедитесь, что токен не содержит пробелов или переносов строк

### Проблема: 403 Forbidden при правильной роли

**Причины:**
1. Аннотация `@RequireRole` указывает другую роль
2. Роль в токене не совпадает с ожидаемой

**Решение:**
1. Проверьте аннотацию `@RequireRole` на методе контроллера
2. Декодируйте токен на jwt.io и проверьте claim `role`
3. Проверьте логи `RoleCheckAspect`

### Проблема: Cannot resolve method 'parserBuilder' in 'Jwts'

**Причина:** Используется старый API JJWT

**Решение:** Используйте новый API для JJWT 0.12.3:
```java
// Старый API (не работает)
Jwts.parserBuilder().setSigningKey(key).build()

// Новый API (работает)
Jwts.parser().verifyWith(key).build()
```

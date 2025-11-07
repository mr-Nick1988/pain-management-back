# Структура JWT токена

## Что такое JWT?

JWT (JSON Web Token) - это стандарт для безопасной передачи информации между сторонами в виде JSON объекта.

## Структура JWT

JWT состоит из 3 частей, разделенных точками:

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwZXJzb25JZCI6IkRPQzAwMSIsInJvbGUiOiJET0NUT1IiLCJmaXJzdE5hbWUiOiJKb2huIiwiaWF0IjoxNzMwOTE2NjI3LCJleHAiOjE3MzEwMDMwMjd9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
│                                      │                                                                                                                                                    │                                          │
│          HEADER                      │                                                      PAYLOAD                                                                                        │              SIGNATURE                   │
```

### 1. HEADER (Заголовок)

```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

- **alg**: Алгоритм подписи (HMAC SHA-256)
- **typ**: Тип токена (JWT)

### 2. PAYLOAD (Полезная нагрузка)

```json
{
  "personId": "DOC001",
  "role": "DOCTOR",
  "firstName": "John",
  "iat": 1730916627,
  "exp": 1731003027
}
```

#### Наши custom claims:
- **personId**: Уникальный ID пользователя (например, "DOC001", "NURSE002")
- **role**: Роль пользователя ("ADMIN", "DOCTOR", "NURSE", "ANESTHESIOLOGIST")
- **firstName**: Имя пользователя для отображения

#### Стандартные claims:
- **iat** (Issued At): Время создания токена (Unix timestamp)
- **exp** (Expiration): Время истечения токена (Unix timestamp)

### 3. SIGNATURE (Подпись)

```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_key
)
```

Подпись гарантирует, что токен не был изменен после создания.

## Два типа токенов

### Access Token (Токен доступа)

```json
{
  "personId": "DOC001",
  "role": "DOCTOR",
  "firstName": "John",
  "iat": 1730916627,
  "exp": 1731003027
}
```

- **Срок жизни**: 24 часа (86400 секунд)
- **Назначение**: Используется для доступа к API
- **Где хранится**: localStorage (Frontend)
- **Как используется**: Отправляется в заголовке `Authorization: Bearer <token>`

### Refresh Token (Токен обновления)

```json
{
  "personId": "DOC001",
  "type": "refresh",
  "iat": 1730916627,
  "exp": 1731521427
}
```

- **Срок жизни**: 7 дней (604800 секунд)
- **Назначение**: Используется для получения нового Access Token
- **Где хранится**: localStorage (Frontend)
- **Как используется**: Отправляется в теле запроса `/api/auth/refresh`

## Как создается токен (Auth Service)

```java
// В Authentication Service (микросервис)
public String generateAccessToken(Person person) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + 86400000); // 24 часа
    
    return Jwts.builder()
        .setSubject(person.getPersonId())
        .claim("personId", person.getPersonId())
        .claim("role", person.getRole())
        .claim("firstName", person.getFirstName())
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
}
```

## Как валидируется токен (Monolith)

```java
// В Monolith API
private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())  // Проверяет подпись
        .build()
        .parseSignedClaims(token)     // Парсит токен
        .getPayload();                // Извлекает claims
}

private SecretKey getSigningKey() {
    byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
}
```

### Что проверяется при валидации:

1. **Подпись**: Токен подписан правильным секретным ключом?
2. **Срок действия**: Токен еще не истек (exp > now)?
3. **Формат**: Токен имеет правильную структуру?

Если хотя бы одна проверка не пройдена → токен невалидный → 401 Unauthorized

## Секретный ключ

### В Auth Service (application.yml)
```yaml
jwt:
  secret: "my-super-secret-key-for-jwt-signing-must-be-at-least-256-bits"
  access-token-expiration: 86400000   # 24 часа в миллисекундах
  refresh-token-expiration: 604800000 # 7 дней в миллисекундах
```

### В Monolith (application.yml)
```yaml
jwt:
  secret: "my-super-secret-key-for-jwt-signing-must-be-at-least-256-bits"
```

**ВАЖНО**: Секретный ключ ДОЛЖЕН быть одинаковым в обоих сервисах!

## Пример полного ответа при логине

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwZXJzb25JZCI6IkRPQzAwMSIsInJvbGUiOiJET0NUT1IiLCJmaXJzdE5hbWUiOiJKb2huIiwiaWF0IjoxNzMwOTE2NjI3LCJleHAiOjE3MzEwMDMwMjd9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJwZXJzb25JZCI6IkRPQzAwMSIsInR5cGUiOiJyZWZyZXNoIiwiaWF0IjoxNzMwOTE2NjI3LCJleHAiOjE3MzE1MjE0Mjd9.abc123xyz789",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "personId": "DOC001",
  "firstName": "John",
  "role": "DOCTOR",
  "temporaryCredentials": false
}
```

## Как Frontend использует токены

### 1. Сохранение после логина
```javascript
// После успешного логина
const response = await axios.post('http://localhost:8082/api/auth/login', {
  login: 'doctor1',
  password: 'pass123'
});

// Сохраняем в localStorage
localStorage.setItem('accessToken', response.data.accessToken);
localStorage.setItem('refreshToken', response.data.refreshToken);
localStorage.setItem('personId', response.data.personId);
localStorage.setItem('role', response.data.role);
```

### 2. Добавление в запросы
```javascript
// Axios interceptor автоматически добавляет токен
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### 3. Обновление при истечении
```javascript
// Axios interceptor обрабатывает 401
axios.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      // Токен истек, обновляем
      const refreshToken = localStorage.getItem('refreshToken');
      const response = await axios.post('http://localhost:8082/api/auth/refresh', {
        refreshToken
      });
      
      // Сохраняем новый accessToken
      localStorage.setItem('accessToken', response.data.accessToken);
      
      // Повторяем оригинальный запрос
      error.config.headers.Authorization = `Bearer ${response.data.accessToken}`;
      return axios.request(error.config);
    }
    return Promise.reject(error);
  }
);
```

## Декодирование токена (для отладки)

Вы можете декодировать токен на сайте [jwt.io](https://jwt.io) или в коде:

```javascript
// Frontend (JavaScript)
function parseJwt(token) {
  const base64Url = token.split('.')[1];
  const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
  const jsonPayload = decodeURIComponent(
    atob(base64).split('').map(c => 
      '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)
    ).join('')
  );
  return JSON.parse(jsonPayload);
}

const token = localStorage.getItem('accessToken');
const payload = parseJwt(token);
console.log(payload);
// {personId: "DOC001", role: "DOCTOR", firstName: "John", iat: 1730916627, exp: 1731003027}
```

## Безопасность токенов

### ✅ Что защищает:
- **Подделка**: Невозможно изменить payload без знания секретного ключа
- **Целостность**: Любое изменение токена делает подпись невалидной
- **Срок действия**: Токены автоматически становятся невалидными после истечения

### ⚠️ Уязвимости:
- **XSS**: Если злоумышленник внедрит JS код, он может украсть токен из localStorage
- **Перехват**: Если токен перехвачен, его можно использовать до истечения срока
- **Хранение**: localStorage доступен всем скриптам на странице

### 🛡️ Рекомендации для production:
1. Использовать **httpOnly cookies** вместо localStorage
2. Включить **HTTPS** для всех запросов
3. Использовать **короткий срок жизни** для accessToken (15 минут)
4. Реализовать **token rotation** для refreshToken
5. Добавить **device fingerprinting** для дополнительной проверки

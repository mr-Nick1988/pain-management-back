# Authentication Service (Аутентификация)

Сервис аутентификации для микросервисной системы. Отвечает за регистрацию, вход, выдачу JWT-токенов через HttpOnly куки, их валидацию/обновление, выход и получение информации о текущем пользователе.

- **Порт:** 8082
- **База:** PostgreSQL
- **Стек:** Java 21, Spring Boot 3, Spring Security, JPA/Hibernate, JJWT, Lombok, Docker

## Архитектура и поток аутентификации
- **Статлес**: Сессий на сервере нет (SessionCreationPolicy.STATELESS).
- **JWT в HttpOnly куки**:
  - `accessToken` (15 минут по умолчанию) приходит/обновляется в куки с `Path=/`.
  - `refreshToken` (7 дней по умолчанию) приходит в куки с `Path=/api/auth/refresh`.
  - Оба куки — HttpOnly; флаг `Secure` управляется свойством `cookie.secure`.
- **Авторизация**: Все эндпоинты сервиса открыты (`/api/auth/**`) — сервис выдает и обслуживает токены. Доступ к другим ресурсам системы защищается на стороне этих сервисов/шлюза, которые читают и валидируют `accessToken`.

## JWT
- **Типы токенов**: `ACCESS` и `REFRESH` (reclaim `type`).
- **Подпись**: симметричный ключ HMAC (секрет в `jwt.secret`).
- **Сроки**:
  - `jwt.expiration` (мс) — access (по умолчанию 900000 = 15 мин)
  - `jwt.refresh-expiration` (мс) — refresh (по умолчанию 604800000 = 7 дн)
- **Клеймы**:
  - subject: `personId`
  - `role`, `login`, `type` (ACCESS или REFRESH)

## Куки
- **`accessToken`**: HttpOnly, `Path=/`, `Max-Age` из `cookie.max-age.access-token` (сек)
- **`refreshToken`**: HttpOnly, `Path=/api/auth/refresh`, `Max-Age` из `cookie.max-age.refresh-token` (сек)
- **`Secure`**: включать в проде (`cookie.secure=true`), иначе браузер не пришлет куки по HTTPS.
- SameSite явно не задается (используйте настройки реверс-прокси/шлюза при необходимости).

## CORS
- Конфигурируется через `CorsConfig` и свойства `cors.*`.
- Включена поддержка cookies/credentials (`allow-credentials: true`).
- Заголовок `Set-Cookie` добавлен в `exposedHeaders` для корректной установки куки браузером.
- По умолчанию разрешен origin: `http://localhost:5173` (переопределяется переменной окружения).

## Роли
- `ADMIN`, `DOCTOR`, `NURSE`, `ANESTHESIOLOGIST` (см. `enums/Role.java`).

## Сущность User (кратко)
- `id` (Long, PK, auto)
- `personId` (unique), `firstName`, `lastName`, `login` (unique), `password` (bcrypt), `role`
- `temporaryCredentials` (boolean, по умолчанию true), `active` (boolean, по умолчанию true)
- `createdAt`, `lastLoginAt`

## Эндпоинты API (base: `/api/auth`)

- **POST `/register`** — регистрация пользователя (для админ-сценариев)
  - Тело (JSON):
    ```json
    {"personId":"string","firstName":"string","lastName":"string","login":"string","password":"string","role":"ADMIN|DOCTOR|NURSE|ANESTHESIOLOGIST"}
    ```
  - Ответ 201 (JSON): созданный пользователь (в текущей реализации сериализуется вся сущность, включая хэш пароля)
  - Ошибки: 400 (валидация), 409 (пользователь уже существует)

- **POST `/login`** — вход пользователя
  - Тело (JSON):
    ```json
    {"login":"string","password":"string"}
    ```
  - Ответ 200 (JSON):
    ```json
    {"personId":"string","firstName":"string","lastName":"string","role":"string","temporaryCredentials":true}
    ```
  - Сайд-эффект: устанавливает куки `accessToken` (Path=/) и `refreshToken` (Path=/api/auth/refresh), оба HttpOnly
  - Ошибки: 401 (неверные креды/неактивен)

- **POST `/validate`** — валидация access-токена из куки
  - Источник токена: куки `accessToken`
  - Ответ 200 (JSON):
    ```json
    {"valid":true,"personId":"string","role":"ADMIN","message":"Token is valid"}
    ```
  - Ошибки: 401 при отсутствии куки (тело с сообщением), 200 c `valid=false` при невалидности

- **POST `/refresh`** — обновление access-токена
  - Источник токена: куки `refreshToken`
  - Ответ 200 (JSON): `{ "success": true, "message": "Token refreshed successfully" }`
  - Сайд-эффект: переустанавливает куки `accessToken`
  - Ошибки: 401 при отсутствии/некорректном refresh-токене

- **POST `/logout`** — выход
  - Ответ 200 (JSON): `{ "success": true, "message": "Logged out successfully" }`
  - Сайд-эффект: очищает оба куки (`Max-Age=0`)

- **GET `/me`** — текущий пользователь по access-токену
  - Источник токена: куки `accessToken`
  - Ответ 200 (JSON):
    ```json
    {"personId":"string","firstName":"string","lastName":"string","login":"string","role":"string","temporaryCredentials":true}
    ```
  - Ошибки: 401 (отсутствует/некорректный токен)

- **POST `/change-password`** — смена логина/пароля
  - Тело (JSON):
    ```json
    {"currentLogin":"string","oldPassword":"string","newLogin":"string","newPassword":"string"}
    ```
  - Ответ 200 (text): `"Password changed successfully"`
  - Ошибки: 400 (валидация), 401 (неверный старый пароль), 409 (новый логин занят)

### Форматы ошибок
- 401/409/500 — объект:
  ```json
  {"status":401,"message":"...","timestamp":"2025-01-01T00:00:00"}
  ```
- 400 (валидация) — map полей:
  ```json
  {"field":"error message"}
  ```

## Примеры cURL
- **Login (записать куки):**
  ```bash
  curl -i -c cookies.txt \
    -H "Content-Type: application/json" \
    -d '{"login":"user","password":"pass"}' \
    http://localhost:8082/api/auth/login
  ```
- **Validate (читать куки):**
  ```bash
  curl -b cookies.txt -X POST http://localhost:8082/api/auth/validate
  ```
- **Me:**
  ```bash
  curl -b cookies.txt http://localhost:8082/api/auth/me
  ```
- **Refresh (обновить access и перезаписать cookie jar):**
  ```bash
  curl -b cookies.txt -c cookies.txt -X POST http://localhost:8082/api/auth/refresh
  ```
- **Logout:**
  ```bash
  curl -b cookies.txt -X POST http://localhost:8082/api/auth/logout
  ```

## Конфигурация
Используется `application-local.yml` (профиль `local`).

- **Активировать профиль local**:
  - Maven: `mvn spring-boot:run -Dspring-boot.run.profiles=local`
  - Jar: `java -jar app.jar --spring.profiles.active=local`

- **Основные свойства**:
  - `spring.datasource.url` (env: `SPRING_DATASOURCE_URL`, дефолт `jdbc:postgresql://localhost:5432/auth_db`)
  - `spring.datasource.username` (env: `SPRING_DATASOURCE_USERNAME`, дефолт `postgres`)
  - `spring.datasource.password` (env: `SPRING_DATASOURCE_PASSWORD`, дефолт `postgres`)
  - `spring.jpa.hibernate.ddl-auto=update`, `show-sql=true`
  - `jwt.secret` (env: `JWT_SECRET`), `jwt.expiration`, `jwt.refresh-expiration`
  - `cookie.secure` (false по умолчанию локально)
  - `cookie.max-age.access-token` (сек), `cookie.max-age.refresh-token` (сек)
  - `cors.allowed-origins` (env: `CORS_ALLOWED_ORIGINS`, дефолт `http://localhost:5173`)
  - `cors.allowed-methods`, `cors.allowed-headers`, `cors.allow-credentials=true`
  - Actuator: `management.endpoints.web.exposure.include=health,info,metrics`

## Запуск локально
- **Зависимости**: Java 21, Maven, PostgreSQL.
- Поднимите Postgres и создайте БД (`auth_db`), либо укажите свою `SPRING_DATASOURCE_URL`.
- Экспортируйте `JWT_SECRET` (достаточно длинный случайный секрет).
- Запустите:
  ```bash
  mvn spring-boot:run -Dspring-boot.run.profiles=local
  ```

## Запуск в Docker
- Сборка образа:
  ```bash
  docker build -t auth-service:latest .
  ```
- Запуск контейнера:
  ```bash
  docker run --rm -p 8082:8082 \
    -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/auth_db \
    -e SPRING_DATASOURCE_USERNAME=postgres \
    -e SPRING_DATASOURCE_PASSWORD=postgres \
    -e JWT_SECRET="change-me-to-strong-secret" \
    -e CORS_ALLOWED_ORIGINS=http://localhost:5173 \
    -e SPRING_PROFILES_ACTIVE=local \
    auth-service:latest
  ```

## Интеграция фронтенда
- Отправляйте запросы с `credentials: 'include'` (fetch/axios), чтобы браузер присылал/получал HttpOnly куки.
- Origin фронтенда должен быть явно указан в `cors.allowed-origins`.
- `refreshToken` ограничен по `Path` и отправится браузером только на `/api/auth/refresh`.

## Мониторинг
- Открыты `/actuator/health`, `/actuator/info`, `/actuator/metrics`.

## Известные особенности и TODO
- На `POST /register` в ответе сериализуется вся сущность пользователя, включая хэш пароля. Для безопасности рекомендуется:
  - Возвращать DTO без пароля, либо
  - Пометить поле `password` как `@JsonIgnore`.
- В проде обязательно `cookie.secure=true` и HTTPS.
- `jwt.secret` должен быть длинным и случайным.
- Механизм отзыва (ревокации) токенов не реализован; при необходимости хранить refresh токены/их состояние в БД.

## Связанные документы
- `API_ENDPOINTS.md` — краткий список эндпоинтов
- `HTTPONLY_COOKIES_MIGRATION.md` — детали миграции на HttpOnly куки

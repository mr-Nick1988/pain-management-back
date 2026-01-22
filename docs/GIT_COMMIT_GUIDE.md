# 📝 Git Commit Best Practices Guide

**Last Updated:** January 22, 2026

---

## 🎯 Основные Принципы

### 1. Структура Коммита

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Пример:**
```
feat(microservices): complete microservices infrastructure setup

- All 7 microservices successfully running
- PostgreSQL databases created (8 databases total)
- Kafka topics configured
- Docker Compose configuration finalized
- Environment variables documented

Closes #123
```

---

## 📋 Типы Коммитов (Conventional Commits)

| Тип | Когда использовать | Примеры |
|-----|-------------------|---------|
| **feat** | Новая функциональность | `feat(auth): add JWT refresh token support` |
| **fix** | Исправление бага | `fix(emr): resolve database connection issue` |
| **docs** | Только документация | `docs: restructure documentation folders` |
| **style** | Форматирование кода | `style(auth): format code with prettier` |
| **refactor** | Рефакторинг без изменения функциональности | `refactor(monolith): extract common utilities` |
| **perf** | Улучшение производительности | `perf(api): optimize database queries` |
| **test** | Добавление тестов | `test(notification): add unit tests` |
| **build** | Изменения в сборке | `build: upgrade Spring Boot to 3.5.5` |
| **ci** | CI/CD изменения | `ci: add GitHub Actions workflow` |
| **chore** | Рутинные задачи | `chore: update dependencies` |
| **revert** | Откат коммита | `revert: feat(auth): add JWT refresh` |

---

## ✍️ Правила Написания

### Subject (Заголовок)

**DO ✅**
```
feat(microservices): add authentication service
fix(docker): resolve PostgreSQL connection timeout
docs: update architecture overview
```

**DON'T ❌**
```
Fixed bug                          # Нет контекста
added new feature.                 # С точкой в конце
FEAT: BIG CHANGES                  # Заглавные буквы
Update stuff                       # Неинформативно
```

### Правила для Subject:
1. **Длина:** до 50 символов
2. **Начало:** строчная буква (кроме type)
3. **Без точки** в конце
4. **Императив:** "add" не "added" или "adds"
5. **На английском** (общепринятая практика)

### Body (Тело)

**Когда нужно:**
- Изменения затрагивают несколько файлов
- Нужно объяснить "почему", а не "что"
- Есть breaking changes

**Формат:**
- Отступ одной пустой строкой от subject
- Каждая строка до 72 символов
- Можно использовать bullet points (-)

**Пример:**
```
feat(microservices): implement pain escalation service

This service monitors VAS readings and triggers alerts when:
- VAS increases by 2+ points within 4 hours
- VAS reaches critical level (8+)
- Patient hasn't received medication for 6+ hours

The service uses Kafka to consume VAS events and publishes
pain.escalated events to notify the medical staff.
```

### Footer (Подвал)

**Для чего:**
- Ссылки на issues: `Closes #123`, `Fixes #456`
- Breaking changes: `BREAKING CHANGE: ...`
- Co-authors: `Co-authored-by: Name <email>`

---

## 🎨 Scopes (Области)

**Для микросервисов:**
- `auth` - Authentication Service
- `emr` - EMR Integration Service
- `notification` - Notification Service
- `pain-escalation` - Pain Escalation Service
- `external-vas` - External VAS Service
- `reporting` - Reporting Service
- `backup` - Backup & Restore Service

**Для инфраструктуры:**
- `docker` - Docker/Docker Compose
- `kafka` - Kafka configuration
- `postgres` - PostgreSQL
- `ci` - CI/CD
- `infra` - Общая инфраструктура

**Для кода:**
- `api` - API endpoints
- `db` - Database migrations
- `config` - Configuration
- `security` - Security & authentication

**Для документации:**
- `docs` - Documentation

---

## 📦 Примеры Коммитов для Разных Ситуаций

### 1. Микросервисы Подняты (Твой Случай)

```bash
# Вариант 1: Кратко
git commit -m "feat(microservices): complete infrastructure setup - all 7 services running"

# Вариант 2: Детально
git commit -m "feat(microservices): complete microservices infrastructure setup

All 7 microservices successfully deployed and running:
- Authentication Service (8082)
- EMR Integration Service (8086) 
- Notification Service (8087)
- Pain Escalation Service (8088)
- External VAS Service (8089)
- Reporting Service (8091)
- Backup & Restore Service (8085)

Infrastructure components:
- PostgreSQL Main (8 databases created)
- PostgreSQL Analytics (1 database)
- Kafka with 8 topics
- Prometheus, Grafana, Kafdrop

Configuration:
- Docker Compose profiles configured
- Environment variables documented
- Database per service pattern implemented
- Health checks verified for all services

This marks completion of the microservices split milestone."
```

### 2. Структурирование Документации (Тоже Твой Случай)

```bash
# Вариант 1: Кратко
git commit -m "docs: restructure documentation into organized folders"

# Вариант 2: Детально
git commit -m "docs: restructure documentation into organized folders

Reorganized documentation from flat structure to categorized folders:

Created new structure:
- 01-guides/ - Quick start guides
- 02-architecture/ - Architecture documentation
- 03-operations/ - DevOps and operations
- 04-testing/ - Testing guides
- 05-microservices/ - Microservices docs
- 06-api/ - API documentation
- archive/ - Historical migration documents

Benefits:
- Easier navigation
- Logical grouping by purpose
- Numbered folders for priority
- Archived outdated migration docs

Updated all internal links in README.md to reflect new structure."
```

### 3. Исправление Бага

```bash
git commit -m "fix(auth): resolve JWT token expiration validation

The JWT validation was incorrectly checking refresh token expiration
instead of access token expiration, causing premature session expiry.

Added JWT_EXPIRATION and JWT_REFRESH_EXPIRATION environment variables
to docker-compose.dev.yml.

Fixes #245"
```

### 4. Добавление Новой Функции

```bash
git commit -m "feat(notification): implement email notification templates

Added template system for notification emails:
- Welcome email template
- Pain escalation alert template  
- Medication reminder template

Templates support variable substitution and HTML formatting.
Uses Thymeleaf for template rendering.

Closes #189"
```

### 5. Рефакторинг

```bash
git commit -m "refactor(monolith): extract common patient validation logic

Extracted patient validation from multiple controllers into
PatientValidationService to reduce code duplication.

No functional changes - pure refactoring."
```

### 6. Обновление Зависимостей

```bash
git commit -m "build: upgrade Spring Boot from 3.5.5 to 3.5.7

- Security patches included
- Minor bug fixes
- All tests passing

No breaking changes."
```

---

## 🔄 Работа с Несколькими Изменениями

### Если изменений много - делай несколько коммитов!

**Плохо ❌**
```bash
git add .
git commit -m "fix: various updates"
```

**Хорошо ✅**
```bash
# Коммит 1: Инфраструктура
git add docker-compose.dev.yml init-databases.sql
git commit -m "feat(microservices): complete infrastructure setup"

# Коммит 2: Документация
git add docs/
git commit -m "docs: restructure documentation folders"

# Коммит 3: Environment
git add .env.development
git commit -m "chore: update environment variables template"
```

---

## 🎯 Коммиты Для Твоей Текущей Работы

### Рекомендуемая Последовательность:

```bash
# 1. Коммит с микросервисами (главное достижение)
git add docker-compose.dev.yml init-databases.sql .env.development
git add microservices/emr-integration-service/src/main/java/com/painmanagement/emr/service/EmrIntegrationServiceImpl.java
git commit -m "feat(microservices): complete microservices infrastructure - all 7 services running

Successfully deployed and verified all microservices:
- Authentication Service (8082)
- EMR Integration Service (8086)
- Notification Service (8087)
- Pain Escalation Service (8088)
- External VAS Service (8089)
- Reporting Service (8091)
- Backup & Restore Service (8085)

Infrastructure:
- Created 8 PostgreSQL databases (Database per Service pattern)
- Configured Kafka with 8 topics
- Added Prometheus, Grafana, Kafdrop monitoring
- Resolved environment variable issues
- Created EmrIntegrationServiceImpl for EMR service

Configuration changes:
- Added missing CORS, JWT, Mail, Kafka env variables
- Updated docker-compose.dev.yml with all microservices
- Created init-databases.sql for automatic DB setup

All services health checks passing. System ready for development."

# 2. Коммит с документацией (организация)
git add docs/
git commit -m "docs: restructure documentation into organized folders

Reorganized flat documentation structure into categorized folders:

New structure:
- 01-guides/ - Quick start and how-to guides
- 02-architecture/ - Architecture and design docs
- 03-operations/ - DevOps, Docker, troubleshooting
- 04-testing/ - Testing guides and strategies
- 05-microservices/ - Individual service documentation
- 06-api/ - API and event schemas
- archive/ - Historical migration documents

Changes:
- Moved 15 markdown files to appropriate folders
- Renamed existing folders with numeric prefixes for sorting
- Updated all internal links in README.md
- Archived outdated migration progress documents
- Created BACKEND_ARCHITECTURE_OVERVIEW.md with complete system architecture

Benefits: easier navigation, logical grouping, cleaner root directory."
```

---

## 📊 Частота Коммитов

### Когда коммитить:

✅ **Коммить нужно:**
- После завершения логической единицы работы
- После успешного прохождения тестов
- Перед переключением на другую задачу
- В конце рабочего дня (если есть работающий код)

❌ **Не коммитить:**
- Сломанный код
- Закомментированный код (dead code)
- Временные файлы
- Credentials или секреты

---

## 🔍 Проверка Перед Коммитом

```bash
# 1. Проверь статус
git status

# 2. Посмотри diff
git diff

# 3. Добавь файлы выборочно (не git add .)
git add <конкретные-файлы>

# 4. Проверь что добавилось
git status

# 5. Коммит
git commit -m "type(scope): message"

# 6. Проверь историю
git log --oneline -5
```

---

## 🚀 Push Best Practices

```bash
# 1. Проверь ветку
git branch

# 2. Pull перед push (если работаешь в команде)
git pull origin <branch-name>

# 3. Push
git push origin <branch-name>

# 4. Если первый push ветки
git push -u origin <branch-name>
```

---

## 🎓 Дополнительные Ресурсы

- [Conventional Commits](https://www.conventionalcommits.org/)
- [How to Write a Git Commit Message](https://chris.beams.io/posts/git-commit/)
- [Angular Commit Guidelines](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#commit)

---

## 📌 Шпаргалка

```bash
# Типы коммитов (в порядке частоты):
feat     - новая функция
fix      - исправление бага
docs     - документация
refactor - рефакторинг
test     - тесты
chore    - рутина (зависимости, конфиг)
style    - форматирование
perf     - производительность
build    - сборка
ci       - CI/CD

# Формат:
type(scope): subject до 50 символов

# Примеры:
git commit -m "feat(auth): add JWT support"
git commit -m "fix(docker): resolve port conflict"
git commit -m "docs: update README with setup instructions"
git commit -m "refactor: extract validation logic"
```

---

**Главное правило:** Коммит должен быть понятен тебе через 6 месяцев. 📅

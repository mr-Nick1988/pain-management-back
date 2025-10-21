# 📊 ТЕСТИРОВАНИЕ МОДУЛЯ АНАЛИТИКИ - ЧАСТЬ 1: ПОДГОТОВКА И СОБЫТИЯ

## 🎯 ЦЕЛЬ ЭТОГО ДОКУМЕНТА
Пошаговое тестирование генерации событий аналитики через действия в системе. Вы будете создавать моковые данные и проверять, что события корректно сохраняются в MongoDB.

---

## 📋 ПРЕДВАРИТЕЛЬНЫЕ ТРЕБОВАНИЯ

### 1. Проверка MongoDB подключения
**ЧТО ДЕЛАТЬ:**
```bash
# Проверьте переменную окружения
echo $MONGODB_URI
```

**ЧТО ДОЛЖНО БЫТЬ:**
- URI должен быть в формате: `mongodb://localhost:27017/pain_management` или MongoDB Atlas URI
- База данных должна быть доступна

**ГАЛОЧКА ✓:** MongoDB подключена и доступна

---

### 2. Проверка коллекций MongoDB
**ЧТО ДЕЛАТЬ:**
Откройте MongoDB Compass или используйте mongo shell:
```javascript
// В MongoDB Compass подключитесь к вашей БД
// Или в mongo shell:
use pain_management
show collections
```

**ЧТО ДОЛЖНО БЫТЬ:**
Две коллекции должны быть созданы автоматически:
- `analytics_events` - бизнес-события
- `log_entries` - технические логи

**ГАЛОЧКА ✓:** Коллекции существуют (или будут созданы при первом событии)

---

### 3. Запуск приложения
**ЧТО ДЕЛАТЬ:**
```bash
# Убедитесь что MONGODB_URI установлена
# Запустите приложение
mvn spring-boot:run
```

**ЧТО ДОЛЖНО БЫТЬ В ЛОГАХ:**
```
Started PainHelperBackApplication in X seconds
MongoDB connected successfully
```

**ГАЛОЧКА ✓:** Приложение запущено без ошибок подключения к MongoDB

---

## 🔥 ТЕСТИРОВАНИЕ СОБЫТИЙ: ПОШАГОВЫЕ СЦЕНАРИИ

---

## СЦЕНАРИЙ 1: USER_LOGIN - Вход пользователя

### Шаг 1.1: Успешный вход
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
POST http://localhost:8080/api/persons/login
Content-Type: application/json

{
  "login": "admin001",
  "password": "admin123"
}
```

**ЧТО ДОЛЖНО ВЕРНУТЬСЯ:**
```json
{
  "personId": "admin001",
  "firstName": "Admin",
  "lastName": "User",
  "role": "ADMIN",
  "token": "..."
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
Откройте коллекцию `analytics_events` и найдите последнюю запись:
```javascript
db.analytics_events.find().sort({timestamp: -1}).limit(1)
```

**ЧТО ДОЛЖНО БЫТЬ В ДОКУМЕНТЕ:**
```json
{
  "_id": "...",
  "timestamp": "2025-10-13T12:37:31",
  "eventType": "USER_LOGIN_SUCCESS",
  "userId": "admin001",
  "userRole": "ADMIN",
  "status": "SUCCESS",
  "metadata": {
    "loginAt": "2025-10-13T12:37:31",
    "success": true,
    "ipAddress": "..."
  }
}
```

**ГАЛОЧКА ✓:** Событие USER_LOGIN_SUCCESS создано с правильными полями

---

### Шаг 1.2: Неуспешный вход
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
POST http://localhost:8080/api/persons/login
Content-Type: application/json

{
  "login": "admin001",
  "password": "wrongpassword"
}
```

**ЧТО ДОЛЖНО ВЕРНУТЬСЯ:**
```json
{
  "error": "Invalid credentials"
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({eventType: "USER_LOGIN_FAILED"}).sort({timestamp: -1}).limit(1)
```

**ЧТО ДОЛЖНО БЫТЬ:**
```json
{
  "eventType": "USER_LOGIN_FAILED",
  "userId": "admin001",
  "status": "FAILED",
  "metadata": {
    "success": false
  }
}
```

**ГАЛОЧКА ✓:** Событие USER_LOGIN_FAILED создано при неверном пароле

---

## СЦЕНАРИЙ 2: PERSON_CREATED - Создание сотрудника (ADMIN)

### Шаг 2.1: Создание нового доктора
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
POST http://localhost:8080/api/admin/persons
Content-Type: application/json

{
  "personId": "DOC001",
  "firstName": "John",
  "lastName": "Smith",
  "login": "jsmith",
  "password": "password123",
  "role": "DOCTOR"
}
```

**ЧТО ДОЛЖНО ВЕРНУТЬСЯ:**
```json
{
  "personId": "DOC001",
  "firstName": "John",
  "lastName": "Smith",
  "role": "DOCTOR",
  "active": true
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({eventType: "PERSON_CREATED"}).sort({timestamp: -1}).limit(1)
```

**ЧТО ДОЛЖНО БЫТЬ В ДОКУМЕНТЕ:**
```json
{
  "eventType": "PERSON_CREATED",
  "userId": "admin001",
  "userRole": "ADMIN",
  "metadata": {
    "firstName": "John",
    "lastName": "Smith",
    "createdAt": "...",
    "newPersonId": "DOC001",
    "newPersonRole": "DOCTOR"
  }
}
```

**ГАЛОЧКА ✓:** Событие PERSON_CREATED содержит данные о новом сотруднике

---

### Шаг 2.2: Создание медсестры
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
POST http://localhost:8080/api/admin/persons
Content-Type: application/json

{
  "personId": "NURSE001",
  "firstName": "Mary",
  "lastName": "Johnson",
  "login": "mjohnson",
  "password": "password123",
  "role": "NURSE"
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({
  eventType: "PERSON_CREATED",
  "metadata.newPersonRole": "NURSE"
}).sort({timestamp: -1}).limit(1)
```

**ГАЛОЧКА ✓:** Событие создано для медсестры

---

## СЦЕНАРИЙ 3: PATIENT_REGISTERED - Регистрация пациента

### Шаг 3.1: Регистрация пациента через DOCTOR
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
POST http://localhost:8080/api/doctor/patients?doctorId=DOC001
Content-Type: application/json

{
  "firstName": "Alice",
  "lastName": "Brown",
  "dateOfBirth": "1985-05-15",
  "gender": "FEMALE",
  "emrNumber": "MRN001",
  "additionalInfo": "Test patient"
}
```

**ЧТО ДОЛЖНО ВЕРНУТЬСЯ:**
```json
{
  "id": 1,
  "firstName": "Alice",
  "lastName": "Brown",
  "emrNumber": "MRN001",
  "gender": "FEMALE",
  "active": true
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({eventType: "PATIENT_REGISTERED"}).sort({timestamp: -1}).limit(1)
```

**ЧТО ДОЛЖНО БЫТЬ В ДОКУМЕНТЕ:**
```json
{
  "eventType": "PATIENT_REGISTERED",
  "patientId": 1,
  "patientMrn": "MRN001",
  "userId": "DOC001",
  "userRole": "DOCTOR",
  "metadata": {
    "age": 40,
    "gender": "FEMALE",
    "registeredAt": "..."
  }
}
```

**ПРОВЕРКИ:**
- ✓ patientId соответствует ID из ответа
- ✓ patientMrn = "MRN001"
- ✓ userId = "DOC001"
- ✓ userRole = "DOCTOR"
- ✓ metadata.age рассчитан правильно (текущий год - год рождения)
- ✓ metadata.gender = "FEMALE"

**ГАЛОЧКА ✓:** Событие PATIENT_REGISTERED создано с полными данными

---

### Шаг 3.2: Регистрация второго пациента (мужчина, другой возраст)
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
POST http://localhost:8080/api/doctor/patients?doctorId=DOC001
Content-Type: application/json

{
  "firstName": "Bob",
  "lastName": "Wilson",
  "dateOfBirth": "1995-08-20",
  "gender": "MALE",
  "emrNumber": "MRN002",
  "additionalInfo": "Test patient 2"
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({
  eventType: "PATIENT_REGISTERED",
  patientMrn: "MRN002"
})
```

**ПРОВЕРКИ:**
- ✓ metadata.gender = "MALE"
- ✓ metadata.age около 30 лет

**ГАЛОЧКА ✓:** Второй пациент зарегистрирован, данные корректны

---

## СЦЕНАРИЙ 4: VAS_RECORDED - Запись уровня боли (NURSE)

### Шаг 4.1: Запись нормального уровня боли (VAS = 4)
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
POST http://localhost:8080/api/nurse/vas?nurseId=NURSE001
Content-Type: application/json

{
  "patientMrn": "MRN001",
  "vasLevel": 4,
  "painLocation": "Lower back"
}
```

**ЧТО ДОЛЖНО ВЕРНУТЬСЯ:**
```json
{
  "id": 1,
  "patientMrn": "MRN001",
  "vasLevel": 4,
  "painLocation": "Lower back",
  "recordedAt": "...",
  "recordedBy": "NURSE001"
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({eventType: "VAS_RECORDED"}).sort({timestamp: -1}).limit(1)
```

**ЧТО ДОЛЖНО БЫТЬ В ДОКУМЕНТЕ:**
```json
{
  "eventType": "VAS_RECORDED",
  "patientMrn": "MRN001",
  "userId": "NURSE001",
  "userRole": "NURSE",
  "vasLevel": 4,
  "priority": "NORMAL",
  "metadata": {
    "painLocation": "Lower back",
    "recordedAt": "...",
    "isCritical": false
  }
}
```

**ПРОВЕРКИ:**
- ✓ vasLevel = 4
- ✓ priority = "NORMAL" (т.к. VAS < 8)
- ✓ metadata.isCritical = false
- ✓ userId = "NURSE001"

**ГАЛОЧКА ✓:** VAS запись с нормальным уровнем боли создана

---

### Шаг 4.2: Запись критического уровня боли (VAS = 9)
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
POST http://localhost:8080/api/nurse/vas?nurseId=NURSE001
Content-Type: application/json

{
  "patientMrn": "MRN001",
  "vasLevel": 9,
  "painLocation": "Chest"
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({
  eventType: "VAS_RECORDED",
  vasLevel: 9
}).sort({timestamp: -1}).limit(1)
```

**ЧТО ДОЛЖНО БЫТЬ:**
```json
{
  "vasLevel": 9,
  "priority": "HIGH",
  "metadata": {
    "isCritical": true,
    "painLocation": "Chest"
  }
}
```

**ПРОВЕРКИ:**
- ✓ vasLevel = 9
- ✓ priority = "HIGH" (т.к. VAS >= 8)
- ✓ metadata.isCritical = true

**ГАЛОЧКА ✓:** Критический VAS помечен как HIGH priority

---

### Шаг 4.3: Несколько записей для разных пациентов
**ЧТО ДЕЛАТЬ:**
Создайте 3-5 записей VAS для обоих пациентов с разными уровнями (2, 5, 7, 8, 10)

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({eventType: "VAS_RECORDED"}).count()
```

**ГАЛОЧКА ✓:** Все VAS записи созданы (должно быть минимум 7 записей)

---

## СЦЕНАРИЙ 5: RECOMMENDATION_APPROVED - Одобрение рекомендации

### Шаг 5.1: Создание рекомендации
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
POST http://localhost:8080/api/doctor/recommendations?doctorId=DOC001
Content-Type: application/json

{
  "patientMrn": "MRN001",
  "description": "Increase morphine dosage to 10mg",
  "justification": "Patient VAS level is 9"
}
```

**ЗАПОМНИТЕ:** `recommendationId` из ответа (например, 1)

---

### Шаг 5.2: Одобрение рекомендации
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
PUT http://localhost:8080/api/doctor/recommendations/1/approve?doctorId=DOC001
Content-Type: application/json

{
  "comment": "Approved after review"
}
```

**ЧТО ДОЛЖНО ВЕРНУТЬСЯ:**
```json
{
  "id": 1,
  "status": "APPROVED",
  "approvedBy": "DOC001",
  "approvalComment": "Approved after review"
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({eventType: "RECOMMENDATION_APPROVED"}).sort({timestamp: -1}).limit(1)
```

**ЧТО ДОЛЖНО БЫТЬ В ДОКУМЕНТЕ:**
```json
{
  "eventType": "RECOMMENDATION_APPROVED",
  "recommendationId": 1,
  "patientMrn": "MRN001",
  "userId": "DOC001",
  "userRole": "DOCTOR",
  "status": "APPROVED",
  "processingTimeMs": 1234,
  "metadata": {
    "comment": "Approved after review",
    "approvedAt": "..."
  }
}
```

**ПРОВЕРКИ:**
- ✓ recommendationId = 1
- ✓ status = "APPROVED"
- ✓ processingTimeMs > 0 (время обработки)
- ✓ metadata.comment присутствует

**ГАЛОЧКА ✓:** Событие одобрения рекомендации создано

---

## СЦЕНАРИЙ 6: RECOMMENDATION_REJECTED + ESCALATION_CREATED

### Шаг 6.1: Создание новой рекомендации
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
POST http://localhost:8080/api/doctor/recommendations?doctorId=DOC001
Content-Type: application/json

{
  "patientMrn": "MRN002",
  "description": "Prescribe fentanyl patch",
  "justification": "Chronic pain management"
}
```

**ЗАПОМНИТЕ:** `recommendationId` (например, 2)

---

### Шаг 6.2: Отклонение рекомендации (создаст эскалацию)
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
PUT http://localhost:8080/api/doctor/recommendations/2/reject?doctorId=DOC001
Content-Type: application/json

{
  "rejectedReason": "Requires anesthesiologist approval",
  "comment": "High risk medication"
}
```

**ЧТО ДОЛЖНО ВЕРНУТЬСЯ:**
```json
{
  "id": 2,
  "status": "REJECTED",
  "rejectedBy": "DOC001",
  "rejectionReason": "Requires anesthesiologist approval"
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB - Событие 1:**
```javascript
db.analytics_events.find({
  eventType: "RECOMMENDATION_REJECTED",
  recommendationId: 2
})
```

**ЧТО ДОЛЖНО БЫТЬ:**
```json
{
  "eventType": "RECOMMENDATION_REJECTED",
  "recommendationId": 2,
  "patientMrn": "MRN002",
  "userId": "DOC001",
  "userRole": "DOCTOR",
  "status": "REJECTED",
  "rejectionReason": "Requires anesthesiologist approval",
  "metadata": {
    "rejectionReason": "Requires anesthesiologist approval",
    "comment": "High risk medication"
  }
}
```

**ГАЛОЧКА ✓:** Событие RECOMMENDATION_REJECTED создано

---

**ЧТО ПРОВЕРИТЬ В MONGODB - Событие 2:**
```javascript
db.analytics_events.find({
  eventType: "ESCALATION_CREATED",
  recommendationId: 2
})
```

**ЧТО ДОЛЖНО БЫТЬ:**
```json
{
  "eventType": "ESCALATION_CREATED",
  "escalationId": 1,
  "recommendationId": 2,
  "patientMrn": "MRN002",
  "userId": "DOC001",
  "userRole": "DOCTOR",
  "priority": "HIGH",
  "vasLevel": 9,
  "metadata": {
    "escalationReason": "Requires anesthesiologist approval",
    "escalatedAt": "..."
  }
}
```

**ПРОВЕРКИ:**
- ✓ escalationId присутствует
- ✓ recommendationId = 2
- ✓ priority установлен (HIGH/MEDIUM/LOW)
- ✓ Оба события созданы (REJECTED и CREATED)

**ГАЛОЧКА ✓:** Отклонение рекомендации автоматически создало эскалацию

---

## СЦЕНАРИЙ 7: ESCALATION_RESOLVED - Разрешение эскалации

### Шаг 7.1: Разрешение эскалации анестезиологом
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
PUT http://localhost:8080/api/anesthesiologist/escalations/1/resolve?anesthesiologistId=ANESTH001
Content-Type: application/json

{
  "resolution": "Approved with dosage adjustment",
  "approved": true
}
```

**ЧТО ДОЛЖНО ВЕРНУТЬСЯ:**
```json
{
  "id": 1,
  "status": "RESOLVED",
  "resolvedBy": "ANESTH001",
  "resolution": "Approved with dosage adjustment"
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({
  eventType: "ESCALATION_RESOLVED",
  escalationId: 1
})
```

**ЧТО ДОЛЖНО БЫТЬ В ДОКУМЕНТЕ:**
```json
{
  "eventType": "ESCALATION_RESOLVED",
  "escalationId": 1,
  "recommendationId": 2,
  "patientMrn": "MRN002",
  "userId": "ANESTH001",
  "userRole": "ANESTHESIOLOGIST",
  "status": "RESOLVED",
  "processingTimeMs": 5678,
  "metadata": {
    "resolution": "Approved with dosage adjustment",
    "resolvedAt": "...",
    "approved": true
  }
}
```

**ПРОВЕРКИ:**
- ✓ escalationId = 1
- ✓ userId = "ANESTH001"
- ✓ userRole = "ANESTHESIOLOGIST"
- ✓ status = "RESOLVED"
- ✓ processingTimeMs > 0 (время от создания до разрешения)
- ✓ metadata.approved = true

**ГАЛОЧКА ✓:** Эскалация разрешена, событие создано с временем обработки

---

## СЦЕНАРИЙ 8: PERSON_UPDATED - Обновление данных сотрудника

### Шаг 8.1: Обновление данных доктора
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
PUT http://localhost:8080/api/admin/persons/DOC001?updatedBy=admin001
Content-Type: application/json

{
  "firstName": "John",
  "lastName": "Smith-Updated",
  "login": "jsmith",
  "role": "DOCTOR"
}
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({
  eventType: "PERSON_UPDATED",
  "metadata.updatedPersonId": "DOC001"
})
```

**ЧТО ДОЛЖНО БЫТЬ:**
```json
{
  "eventType": "PERSON_UPDATED",
  "userId": "admin001",
  "userRole": "ADMIN",
  "metadata": {
    "updatedPersonId": "DOC001",
    "updatedAt": "...",
    "changedFields": {
      "lastName": "Smith-Updated"
    }
  }
}
```

**ГАЛОЧКА ✓:** Обновление сотрудника отслеживается с измененными полями

---

## СЦЕНАРИЙ 9: PERSON_DELETED - Удаление сотрудника

### Шаг 9.1: Удаление сотрудника
**ЧТО ДЕЛАТЬ В POSTMAN:**
```http
DELETE http://localhost:8080/api/admin/persons/DOC001?deletedBy=admin001&reason=Left organization
```

**ЧТО ПРОВЕРИТЬ В MONGODB:**
```javascript
db.analytics_events.find({
  eventType: "PERSON_DELETED",
  "metadata.deletedPersonId": "DOC001"
})
```

**ЧТО ДОЛЖНО БЫТЬ:**
```json
{
  "eventType": "PERSON_DELETED",
  "userId": "admin001",
  "userRole": "ADMIN",
  "metadata": {
    "deletedPersonId": "DOC001",
    "deletedPersonRole": "DOCTOR",
    "firstName": "John",
    "lastName": "Smith-Updated",
    "deletedAt": "...",
    "reason": "Left organization"
  }
}
```

**ГАЛОЧКА ✓:** Удаление сотрудника зафиксировано с причиной

---

## 📊 ФИНАЛЬНАЯ ПРОВЕРКА КОЛЛЕКЦИИ analytics_events

### Подсчет всех событий по типам
**ЧТО ДЕЛАТЬ В MONGODB:**
```javascript
db.analytics_events.aggregate([
  {
    $group: {
      _id: "$eventType",
      count: { $sum: 1 }
    }
  },
  {
    $sort: { count: -1 }
  }
])
```

**ЧТО ДОЛЖНО БЫТЬ (минимум):**
```json
[
  { "_id": "VAS_RECORDED", "count": 7 },
  { "_id": "USER_LOGIN_SUCCESS", "count": 3 },
  { "_id": "PATIENT_REGISTERED", "count": 2 },
  { "_id": "PERSON_CREATED", "count": 2 },
  { "_id": "RECOMMENDATION_APPROVED", "count": 1 },
  { "_id": "RECOMMENDATION_REJECTED", "count": 1 },
  { "_id": "ESCALATION_CREATED", "count": 1 },
  { "_id": "ESCALATION_RESOLVED", "count": 1 },
  { "_id": "PERSON_UPDATED", "count": 1 },
  { "_id": "PERSON_DELETED", "count": 1 },
  { "_id": "USER_LOGIN_FAILED", "count": 1 }
]
```

**ГАЛОЧКА ✓:** Все 10 типов событий присутствуют в базе

---

## ✅ ЧЕКЛИСТ ЧАСТЬ 1: СОБЫТИЯ

- [ ] MongoDB подключена и работает
- [ ] Коллекции analytics_events и log_entries созданы
- [ ] USER_LOGIN_SUCCESS создается при успешном входе
- [ ] USER_LOGIN_FAILED создается при неверном пароле
- [ ] PERSON_CREATED фиксирует создание сотрудников
- [ ] PATIENT_REGISTERED содержит возраст и пол пациента
- [ ] VAS_RECORDED различает NORMAL и HIGH priority
- [ ] RECOMMENDATION_APPROVED содержит processingTimeMs
- [ ] RECOMMENDATION_REJECTED создает эскалацию автоматически
- [ ] ESCALATION_CREATED содержит priority и vasLevel
- [ ] ESCALATION_RESOLVED содержит время разрешения
- [ ] PERSON_UPDATED отслеживает измененные поля
- [ ] PERSON_DELETED содержит причину удаления
- [ ] Все события имеют timestamp, userId, userRole
- [ ] Metadata содержит дополнительную информацию

**ЕСЛИ ВСЕ ГАЛОЧКИ ПРОСТАВЛЕНЫ** ✅ - переходите к PART 2: Тестирование API эндпоинтов аналитики

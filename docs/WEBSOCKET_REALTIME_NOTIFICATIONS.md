# 🔔 WebSocket Real-Time Уведомления - Полная Реализация

**Дата реализации:** 22.10.2025  
**Статус:** ✅ Полностью реализовано  
**Версия:** 1.0.0

---

## 📋 ОБЗОР

Реализована **полная end-to-end система real-time уведомлений** через WebSocket для всех критических событий в системе управления болью.

### ✅ Что реализовано:

1. **Унифицированная система уведомлений**
   - Единый формат для всех типов уведомлений
   - Автоматическая маршрутизация по топикам
   - Поддержка приоритетов и ролей

2. **WebSocket инфраструктура**
   - Настроенные endpoints и топики
   - Поддержка broadcast и персональных сообщений
   - Обратная совместимость с legacy топиками

3. **Интеграция со всеми модулями**
   - Pain Escalation уведомления
   - EMR критические алерты
   - Обновления рекомендаций
   - Системные сообщения

4. **Тестовый контроллер**
   - REST API для отправки тестовых уведомлений
   - Проверка работоспособности WebSocket
   - Демонстрация всех типов уведомлений

---

## 🏗️ АРХИТЕКТУРА

```
websocket/
├── dto/
│   └── UnifiedNotificationDTO.java          # Унифицированный DTO для всех уведомлений
├── service/
│   └── UnifiedNotificationService.java      # Сервис отправки уведомлений
└── controller/
    └── WebSocketTestController.java         # REST контроллер для тестирования

external_emr_integration_service/config/
└── WebSocketConfig.java                     # Конфигурация WebSocket (обновлена)

pain_escalation_tracking/service/
└── PainEscalationNotificationService.java   # Интегрирован с унифицированным сервисом
```

---

## 🔌 WEBSOCKET ENDPOINTS

### Основные endpoints:

1. **`ws://localhost:8080/ws-notifications`** - основной endpoint для всех уведомлений
2. **`ws://localhost:8080/ws-emr-alerts`** - legacy endpoint (обратная совместимость)

---

## 📡 ДОСТУПНЫЕ ТОПИКИ

### 1. Общие топики

| Топик | Описание | Кто подписывается |
|-------|----------|-------------------|
| `/topic/notifications/all` | Все уведомления | Dashboard, администраторы |
| `/topic/notifications/critical` | Только критические | Все роли |

### 2. Топики по ролям

| Топик | Описание | Кто подписывается |
|-------|----------|-------------------|
| `/topic/notifications/doctors` | Уведомления для врачей | Врачи |
| `/topic/notifications/anesthesiologists` | Уведомления для анестезиологов | Анестезиологи |
| `/topic/notifications/nurses` | Уведомления для медсестер | Медсестры |
| `/topic/notifications/admins` | Уведомления для администраторов | Администраторы |

### 3. Топики по типам событий

| Топик | Описание | Тип события |
|-------|----------|-------------|
| `/topic/notifications/emr-alerts` | EMR критические алерты | EMR_ALERT |
| `/topic/notifications/pain-escalations` | Эскалации боли | PAIN_ESCALATION |
| `/topic/notifications/recommendations` | Обновления рекомендаций | RECOMMENDATION_UPDATE |
| `/topic/notifications/dose-reminders` | Напоминания о дозах | DOSE_REMINDER |
| `/topic/notifications/protocol-approvals` | Одобрения протоколов | PROTOCOL_APPROVAL |
| `/topic/notifications/critical-vas` | Критический VAS | CRITICAL_VAS |

### 4. Персональные топики

| Топик | Описание |
|-------|----------|
| `/queue/notifications/{userId}` | Персональные уведомления конкретному пользователю |

### 5. Legacy топики (обратная совместимость)

| Топик | Описание |
|-------|----------|
| `/topic/escalations/doctors` | Эскалации для врачей (старый формат) |
| `/topic/escalations/anesthesiologists` | Эскалации для анестезиологов (старый формат) |
| `/topic/escalations/dashboard` | Dashboard эскалации (старый формат) |
| `/topic/escalations/critical` | Критические эскалации (старый формат) |
| `/topic/escalations/status-updates` | Обновления статусов (старый формат) |
| `/topic/emr-alerts` | EMR алерты (старый формат) |

---

## 📦 ФОРМАТ УВЕДОМЛЕНИЯ

### UnifiedNotificationDTO

```json
{
  "type": "PAIN_ESCALATION",
  "priority": "CRITICAL",
  "patientMrn": "EMR-12345",
  "patientName": "Иван Иванов",
  "title": "Эскалация боли: CRITICAL",
  "message": "VAS изменился с 5 на 9 (+4). Critical pain level: VAS 9",
  "details": "Значительный рост боли слишком рано после последней дозы",
  "recommendations": "URGENT: Immediate intervention required",
  "timestamp": "2025-10-22T20:00:00",
  "relatedEntityId": 123,
  "relatedEntityType": "ESCALATION",
  "additionalData": null,
  "requiresAction": true,
  "actionUrl": "/escalations/123",
  "diagnoses": ["M54.5 - Low back pain"],
  "targetRole": "DOCTOR",
  "targetUserId": null
}
```

### Типы уведомлений (NotificationType)

- **EMR_ALERT** - Критические изменения в EMR
- **PAIN_ESCALATION** - Эскалация боли
- **RECOMMENDATION_UPDATE** - Обновление рекомендации
- **DOSE_REMINDER** - Напоминание о дозе
- **SYSTEM_MESSAGE** - Системное сообщение
- **PROTOCOL_APPROVAL** - Требуется одобрение протокола
- **PATIENT_ADMISSION** - Поступление нового пациента
- **CRITICAL_VAS** - Критический уровень боли

### Приоритеты (NotificationPriority)

- **LOW** - Низкий (информационное)
- **MEDIUM** - Средний (требует внимания)
- **HIGH** - Высокий (требует скорого действия)
- **CRITICAL** - Критический (требует немедленного действия)

---

## 💻 ПОДКЛЮЧЕНИЕ КЛИЕНТА (JavaScript)

### Базовое подключение

```javascript
// Подключение к WebSocket
const socket = new SockJS('http://localhost:8080/ws-notifications');
const stompClient = Stomp.over(socket);

// Подключение
stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Подписка на все уведомления
    stompClient.subscribe('/topic/notifications/all', function(message) {
        const notification = JSON.parse(message.body);
        handleNotification(notification);
    });
});

// Обработка уведомления
function handleNotification(notification) {
    console.log('Received notification:', notification);
    
    // Показать уведомление в UI
    showToast(notification.title, notification.message, notification.priority);
    
    // Если требуется действие - показать кнопку
    if (notification.requiresAction) {
        showActionButton(notification.actionUrl);
    }
}
```

### Подписка на несколько топиков

```javascript
stompClient.connect({}, function(frame) {
    // Подписка на уведомления для врачей
    stompClient.subscribe('/topic/notifications/doctors', function(message) {
        const notification = JSON.parse(message.body);
        handleDoctorNotification(notification);
    });
    
    // Подписка на критические уведомления
    stompClient.subscribe('/topic/notifications/critical', function(message) {
        const notification = JSON.parse(message.body);
        handleCriticalNotification(notification);
    });
    
    // Подписка на эскалации боли
    stompClient.subscribe('/topic/notifications/pain-escalations', function(message) {
        const notification = JSON.parse(message.body);
        handlePainEscalation(notification);
    });
    
    // Подписка на EMR алерты
    stompClient.subscribe('/topic/notifications/emr-alerts', function(message) {
        const notification = JSON.parse(message.body);
        handleEmrAlert(notification);
    });
});
```

### Персональные уведомления

```javascript
// Подписка на персональные уведомления
const userId = getCurrentUserId(); // Получить ID текущего пользователя

stompClient.subscribe('/queue/notifications/' + userId, function(message) {
    const notification = JSON.parse(message.body);
    handlePersonalNotification(notification);
});
```

### Фильтрация по приоритету

```javascript
stompClient.subscribe('/topic/notifications/all', function(message) {
    const notification = JSON.parse(message.body);
    
    // Обработка в зависимости от приоритета
    switch(notification.priority) {
        case 'CRITICAL':
            showCriticalAlert(notification);
            playAlertSound();
            break;
        case 'HIGH':
            showHighPriorityNotification(notification);
            break;
        case 'MEDIUM':
            showMediumPriorityNotification(notification);
            break;
        case 'LOW':
            showLowPriorityNotification(notification);
            break;
    }
});
```

---

## 🧪 ТЕСТИРОВАНИЕ

### REST API для тестирования

#### 1. Отправить простое тестовое уведомление

```bash
curl -X POST http://localhost:8080/api/websocket/test
```

**Ответ:**
```json
{
  "status": "success",
  "message": "Test notification sent to all subscribers",
  "timestamp": "2025-10-22T20:00:00"
}
```

#### 2. Отправить тестовый EMR алерт

```bash
curl -X POST http://localhost:8080/api/websocket/test/emr-alert
```

**Результат:** Отправляется критический EMR алерт на все топики

#### 3. Отправить тестовую эскалацию боли

```bash
curl -X POST http://localhost:8080/api/websocket/test/pain-escalation
```

**Результат:** Отправляется эскалация боли с приоритетом HIGH

#### 4. Отправить критическое уведомление

```bash
curl -X POST http://localhost:8080/api/websocket/test/critical
```

**Результат:** Отправляется на все критические топики

#### 5. Отправить персональное уведомление

```bash
curl -X POST http://localhost:8080/api/websocket/test/personal/doctor_123
```

**Результат:** Отправляется только пользователю `doctor_123`

#### 6. Отправить уведомление для роли

```bash
curl -X POST http://localhost:8080/api/websocket/test/role/DOCTOR
```

**Результат:** Отправляется всем врачам

#### 7. Проверить статус WebSocket

```bash
curl http://localhost:8080/api/websocket/status
```

**Ответ:**
```json
{
  "status": "active",
  "endpoints": {
    "main": "ws://localhost:8080/ws-notifications",
    "legacy": "ws://localhost:8080/ws-emr-alerts"
  },
  "topics": {
    "all": "/topic/notifications/all",
    "doctors": "/topic/notifications/doctors",
    "anesthesiologists": "/topic/notifications/anesthesiologists",
    "nurses": "/topic/notifications/nurses",
    "critical": "/topic/notifications/critical",
    "emr_alerts": "/topic/notifications/emr-alerts",
    "pain_escalations": "/topic/notifications/pain-escalations"
  },
  "timestamp": "2025-10-22T20:00:00"
}
```

---

## 🔗 ИНТЕГРАЦИЯ С МОДУЛЯМИ

### 1. Pain Escalation Module

При создании эскалации боли автоматически отправляются уведомления:

```java
// В PainEscalationServiceImpl.handleNewVasRecord()
Escalation savedEscalation = escalationRepository.save(escalation);

// Отправляем WebSocket уведомление
notificationService.sendEscalationNotification(
    savedEscalation,
    patient,
    newVasLevel,
    previousVasLevel,
    checkResult.getRecommendations()
);
```

**Результат:**
- Отправляется на `/topic/notifications/pain-escalations`
- Отправляется на `/topic/notifications/doctors`
- Если CRITICAL - отправляется на `/topic/notifications/critical`
- Legacy формат отправляется на старые топики

### 2. EMR Change Detection

При критических изменениях EMR автоматически отправляются уведомления:

```java
// В EmrRecalculationService.handleCriticalEmrChanges()
UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
    .type(UnifiedNotificationDTO.NotificationType.EMR_ALERT)
    .priority(UnifiedNotificationDTO.NotificationPriority.CRITICAL)
    .patientMrn(patient.getMrn())
    .title("Критическое изменение EMR: " + alert.getParameterName())
    .message(String.format("%s изменился с %s на %s",
            alert.getParameterName(),
            alert.getOldValue(),
            alert.getNewValue()))
    .build();

unifiedNotificationService.sendCriticalNotification(notification);
```

**Результат:**
- Отправляется на `/topic/notifications/emr-alerts`
- Отправляется на `/topic/notifications/critical`
- Отправляется на `/topic/notifications/doctors`

### 3. Recommendation Updates

При пересчете рекомендаций отправляются уведомления:

```java
// В EmrRecalculationService.sendNewRecommendationNotification()
UnifiedNotificationDTO notification = UnifiedNotificationDTO.builder()
    .type(UnifiedNotificationDTO.NotificationType.RECOMMENDATION_UPDATE)
    .priority(UnifiedNotificationDTO.NotificationPriority.HIGH)
    .title("Новая рекомендация после изменения EMR")
    .message("Сгенерирована новая рекомендация из-за критических изменений EMR")
    .build();

unifiedNotificationService.sendNotification(notification);
```

---

## 📊 ПРИМЕРЫ УВЕДОМЛЕНИЙ

### Пример 1: Эскалация боли

```json
{
  "type": "PAIN_ESCALATION",
  "priority": "HIGH",
  "patientMrn": "EMR-12345",
  "patientName": "Иван Иванов",
  "title": "Эскалация боли: HIGH",
  "message": "VAS изменился с 5 на 8 (+3). Pain increased significantly",
  "details": "Значительный рост боли через 2 часа после введения дозы",
  "recommendations": "Рассмотреть увеличение дозировки или смену препарата",
  "timestamp": "2025-10-22T15:30:00",
  "relatedEntityId": 456,
  "relatedEntityType": "ESCALATION",
  "requiresAction": true,
  "actionUrl": "/escalations/456",
  "diagnoses": ["M54.5 - Low back pain"],
  "targetRole": "DOCTOR"
}
```

### Пример 2: Критический EMR алерт

```json
{
  "type": "EMR_ALERT",
  "priority": "CRITICAL",
  "patientMrn": "EMR-12345",
  "patientName": "Иван Иванов",
  "title": "Критическое изменение EMR: GFR",
  "message": "GFR изменился с 45 на 25",
  "details": "Тяжелая почечная недостаточность. GFR < 30",
  "recommendations": "Требуется пересмотр всех активных назначений",
  "timestamp": "2025-10-22T16:00:00",
  "requiresAction": true,
  "targetRole": "DOCTOR"
}
```

### Пример 3: Новая рекомендация

```json
{
  "type": "RECOMMENDATION_UPDATE",
  "priority": "HIGH",
  "patientMrn": "EMR-12345",
  "patientName": "Иван Иванов",
  "title": "Новая рекомендация после изменения EMR",
  "message": "Сгенерирована новая рекомендация #789 из-за критических изменений EMR",
  "details": "Изменения EMR: GFR: 45 → 25",
  "recommendations": "Tramadol 50mg PO q6h PRN (снижено из-за GFR < 30)",
  "timestamp": "2025-10-22T16:05:00",
  "relatedEntityId": 789,
  "relatedEntityType": "RECOMMENDATION",
  "requiresAction": true,
  "actionUrl": "/recommendations/789",
  "targetRole": "DOCTOR"
}
```

---

## ✅ ЧЕКЛИСТ РЕАЛИЗАЦИИ

- [x] UnifiedNotificationDTO создан
- [x] UnifiedNotificationService реализован
- [x] WebSocketConfig обновлен с новыми топиками
- [x] WebSocketTestController создан для тестирования
- [x] PainEscalationNotificationService интегрирован
- [x] EmrRecalculationService отправляет уведомления
- [x] Поддержка всех типов уведомлений
- [x] Поддержка приоритетов
- [x] Поддержка ролей
- [x] Персональные уведомления
- [x] Broadcast уведомления
- [x] Legacy топики (обратная совместимость)
- [x] REST API для тестирования
- [x] Документация

---

## 🚀 ЗАПУСК И ПРОВЕРКА

### 1. Запустить приложение

```bash
mvn spring-boot:run
```

### 2. Проверить статус WebSocket

```bash
curl http://localhost:8080/api/websocket/status
```

### 3. Отправить тестовое уведомление

```bash
curl -X POST http://localhost:8080/api/websocket/test
```

### 4. Подключиться из браузера

Открыть консоль браузера и выполнить:

```javascript
const socket = new SockJS('http://localhost:8080/ws-notifications');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected!');
    
    stompClient.subscribe('/topic/notifications/all', function(message) {
        console.log('Received:', JSON.parse(message.body));
    });
});
```

### 5. Отправить тестовый алерт

```bash
curl -X POST http://localhost:8080/api/websocket/test/emr-alert
```

Вы должны увидеть уведомление в консоли браузера!

---

## 📚 СВЯЗАННАЯ ДОКУМЕНТАЦИЯ

- [Pain Escalation Module](PAIN_ESCALATION_MODULE.md)
- [EMR Recalculation](EMR_RECALCULATION.md)
- [WebSocket Configuration](../src/main/java/pain_helper_back/external_emr_integration_service/config/WebSocketConfig.java)

---

**Автор:** Pain Management Team  
**Дата:** 22.10.2025  
**Статус:** ✅ Полностью реализовано

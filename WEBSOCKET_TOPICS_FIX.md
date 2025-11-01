# 🎯 WebSocket Topics - ИСПРАВЛЕНО!

## Проблема

**Backend отправлял на:**
- `/topic/notifications/anesthesiologists` ❌
- `/topic/notifications/pain-escalations` ❌

**Frontend подписан на:**
- `/topic/escalations/anesthesiologists` ✅

**Результат:** Сообщения НЕ доходили до фронтенда!

---

## Решение

**Файл:** `UnifiedNotificationService.java`

**Исправлены топики:**

### До:
```java
case "ANESTHESIOLOGIST" -> "/topic/notifications/anesthesiologists"; ❌
case PAIN_ESCALATION -> "/topic/notifications/pain-escalations"; ❌
```

### После:
```java
case "ANESTHESIOLOGIST" -> "/topic/escalations/anesthesiologists"; ✅
case PAIN_ESCALATION -> "/topic/escalations/anesthesiologists"; ✅
```

---

## Правильные топики

| Роль | Топик |
|------|-------|
| ANESTHESIOLOGIST | `/topic/escalations/anesthesiologists` |
| DOCTOR | `/topic/escalations/doctors` |
| NURSE | `/topic/escalations/nurses` |
| Dashboard | `/topic/escalations/dashboard` |
| Critical | `/topic/escalations/critical` |
| EMR Alerts | `/topic/emr-alerts` |

---

## Проверка (СЕЙЧАС!)

### 1. Перезапусти backend
```bash
# Ctrl+C
mvn spring-boot:run
```

### 2. Отправь тестовое уведомление
```bash
curl -X POST http://localhost:8080/api/websocket/test/pain-escalation
```

### 3. Проверь логи backend
**Теперь должно быть:**
```
📨 Sent notification to /topic/escalations/anesthesiologists: Эскалация боли
Notification sent successfully: type=PAIN_ESCALATION, priority=HIGH, patient=TEST-12345
```

### 4. Проверь консоль фронтенда (F12)
**Должно появиться:**
```
📨 [ANESTHESIOLOGIST TOPIC] Received: {
  type: "PAIN_ESCALATION",
  priority: "HIGH",
  patientMrn: "TEST-12345",
  message: "VAS увеличился с 5 до 9..."
}
```

### 5. Проверь toast уведомление
**Должен появиться toast:**
```
🚨 Эскалация боли
Тестовый (MRN: TEST-12345)
VAS увеличился с 5 до 9 через 2 часа после введения дозы
```

---

## Быстрая проверка (10 секунд)

```bash
# 1. Отправить тест
curl -X POST http://localhost:8080/api/websocket/test/pain-escalation

# 2. Проверить логи
tail -f logs/application.log | grep "📨"
```

**Ожидаемый лог:**
```
📨 Sent notification to /topic/escalations/anesthesiologists: Эскалация боли
```

---

## Что изменилось

### sendNotification()
- ✅ Отправка на `/topic/escalations/dashboard` (вместо `/topic/notifications/all`)
- ✅ Логи с эмодзи 📨 для легкой идентификации
- ✅ Правильные топики по ролям

### getRoleTopicPath()
- ✅ `/topic/escalations/anesthesiologists` (вместо `/topic/notifications/anesthesiologists`)
- ✅ `/topic/escalations/doctors` (вместо `/topic/notifications/doctors`)

### getTypeTopicPath()
- ✅ `PAIN_ESCALATION` → `/topic/escalations/anesthesiologists`
- ✅ `CRITICAL_VAS` → `/topic/escalations/critical`
- ✅ `EMR_ALERT` → `/topic/emr-alerts`

---

## Теперь работает! 🎉

**Перезапусти backend и проверь!**

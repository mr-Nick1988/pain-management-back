# WebSocket Fix - Исправление 404 ошибки

## 🔴 Проблема

**Ошибка:**
```
Request URL: http://localhost:8080/api/ws/info?t=1762012037813
Status Code: 404 Not Found
```

**Причина:** Конфликт двух WebSocket конфигураций с разными endpoints:
1. `WebSocketConfig.java` → `/ws`
2. `WebSocketNotificationsConfig.java` → `/ws-notifications`

Spring не знал, какой использовать, и оба не работали.

---

## ✅ Решение

### 1. Объединил конфигурации в одну

**Файл:** `src/main/java/pain_helper_back/config/WebSocketConfig.java`

**Единый endpoint:** `/ws`

**Удален дублирующий конфиг:**
`src/main/java/pain_helper_back/external_emr_integration_service/config/WebSocketNotificationsConfig.java`

---

## 🧪 Как проверить WebSocket (ПОШАГОВО)

### Шаг 1: Перезапустить бэкенд

**ВАЖНО:** После изменения конфигурации нужно перезапустить приложение!

```bash
# Остановить текущий процесс (Ctrl+C)
# Запустить заново
mvn spring-boot:run
```

---

### Шаг 2: Проверить статус WebSocket

```bash
curl http://localhost:8080/api/websocket/status
```

**Ожидаемый ответ:**
```json
{
  "status": "active",
  "endpoint": "ws://localhost:8080/ws",
  "sockjs_endpoint": "http://localhost:8080/ws",
  "topics": {
    "anesthesiologists": "/topic/escalations/anesthesiologists",
    "doctors": "/topic/escalations/doctors",
    "critical": "/topic/escalations/critical",
    "dashboard": "/topic/escalations/dashboard",
    "emr_alerts": "/topic/emr-alerts"
  },
  "frontend_example": "const socket = new SockJS('http://localhost:8080/ws'); const stompClient = Stomp.over(socket);",
  "timestamp": "2025-11-01T17:55:00"
}
```

---

### Шаг 3: Отправить тестовое уведомление

```bash
curl -X POST http://localhost:8080/api/websocket/test/pain-escalation
```

**Ожидаемый ответ:**
```json
{
  "status": "success",
  "message": "Test pain escalation sent",
  "type": "PAIN_ESCALATION",
  "priority": "HIGH"
}
```

**Проверить логи бэкенда:**
```
INFO  p.w.s.UnifiedNotificationService : Sending PAIN_ESCALATION notification to /topic/escalations/anesthesiologists
```

---

### Шаг 4: Проверить подключение на фронтенде

#### Открыть консоль браузера (F12)

**Должно быть:**
```
WebSocket connection to 'ws://localhost:8080/ws/...' succeeded
```

**НЕ должно быть:**
```
Failed to load resource: the server responded with a status of 404 (Not Found)
```

---

## 🎯 Тестирование с фронтенда

### Вариант 1: Через браузерную консоль (F12)

```javascript
// 1. Подключиться к WebSocket
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

// 2. Подключиться и подписаться
stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Подписка на эскалации для анестезиологов
    stompClient.subscribe('/topic/escalations/anesthesiologists', function(message) {
        console.log('Received:', JSON.parse(message.body));
    });
    
    // Подписка на критические уведомления
    stompClient.subscribe('/topic/escalations/critical', function(message) {
        console.log('CRITICAL:', JSON.parse(message.body));
    });
});

// 3. Отправить тестовое уведомление с бэкенда
// curl -X POST http://localhost:8080/api/websocket/test/pain-escalation

// 4. В консоли должно появиться:
// Received: {type: "PAIN_ESCALATION", priority: "HIGH", ...}
```

---

### Вариант 2: Через React компонент

**Проверить файл:** `src/hooks/useWebSocket.ts` (на фронтенде)

**Правильное подключение:**
```typescript
const socket = new SockJS('http://localhost:8080/ws'); // ✅ БЕЗ /api/
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
    stompClient.subscribe('/topic/escalations/anesthesiologists', (message) => {
        const notification = JSON.parse(message.body);
        console.log('Escalation received:', notification);
    });
});
```

**НЕПРАВИЛЬНО:**
```typescript
const socket = new SockJS('http://localhost:8080/api/ws'); // ❌ НЕТ /api/
```

---

## 🔍 Диагностика проблем

### Проблема: 404 Not Found на /ws/info

**Причина:** Фронт пытается подключиться к `/api/ws` вместо `/ws`

**Решение:** Убрать `/api/` из URL подключения

**Было:**
```typescript
const socket = new SockJS('http://localhost:8080/api/ws');
```

**Стало:**
```typescript
const socket = new SockJS('http://localhost:8080/ws');
```

---

### Проблема: WebSocket подключается, но уведомления не приходят

**Проверить:**

1. **Правильный топик?**
   ```typescript
   // ✅ Правильно
   stompClient.subscribe('/topic/escalations/anesthesiologists', callback);
   
   // ❌ Неправильно
   stompClient.subscribe('/topic/notifications/anesthesiologists', callback);
   ```

2. **Бэкенд отправляет уведомления?**
   ```bash
   # Проверить логи
   grep "Sending.*notification" logs/application.log
   ```

3. **Подписка выполнена ДО отправки уведомления?**
   ```typescript
   stompClient.connect({}, () => {
       // Подписка ВНУТРИ callback connect
       stompClient.subscribe('/topic/escalations/anesthesiologists', callback);
   });
   ```

---

### Проблема: Уведомления приходят, но не отображаются

**Проверить:**

1. **Callback вызывается?**
   ```typescript
   stompClient.subscribe('/topic/escalations/anesthesiologists', (message) => {
       console.log('Message received!', message); // Добавить лог
       const data = JSON.parse(message.body);
   });
   ```

2. **Формат данных правильный?**
   ```typescript
   console.log('Raw message:', message.body);
   const data = JSON.parse(message.body);
   console.log('Parsed data:', data);
   ```

---

## 📊 Полный тест (End-to-End)

### 1. Запустить бэкенд
```bash
mvn spring-boot:run
```

### 2. Проверить endpoint
```bash
curl http://localhost:8080/api/websocket/status
# Должен вернуть 200 OK
```

### 3. Открыть фронтенд
```bash
cd frontend
npm run dev
```

### 4. Войти как анестезиолог
- Login: `anesthesiologist1`
- Password: `password`

### 5. Открыть консоль браузера (F12)
**Должно быть:**
```
WebSocket connected
Subscribed to /topic/escalations/anesthesiologists
```

### 6. Отправить тестовое уведомление
```bash
curl -X POST http://localhost:8080/api/websocket/test/pain-escalation
```

### 7. Проверить консоль браузера
**Должно появиться:**
```
Escalation received: {
  type: "PAIN_ESCALATION",
  priority: "HIGH",
  patientMrn: "TEST-12345",
  message: "VAS увеличился с 5 до 9..."
}
```

### 8. Проверить toast уведомление
**Должен появиться toast в правом верхнем углу:**
```
🚨 Эскалация боли
Тестовый (MRN: TEST-12345)
VAS увеличился с 5 до 9 через 2 часа после введения дозы
```

---

## 🎯 Быстрая проверка (30 секунд)

```bash
# 1. Проверить статус
curl http://localhost:8080/api/websocket/status

# 2. Отправить тест
curl -X POST http://localhost:8080/api/websocket/test/pain-escalation

# 3. Проверить логи
tail -f logs/application.log | grep "WebSocket\|notification"
```

**Ожидаемые логи:**
```
INFO  p.w.s.UnifiedNotificationService : Sending PAIN_ESCALATION notification
INFO  p.w.s.UnifiedNotificationService : Notification sent to /topic/escalations/anesthesiologists
```

---

## 📝 Checklist

- [ ] Бэкенд перезапущен после изменения конфигурации
- [ ] `/api/websocket/status` возвращает 200 OK
- [ ] Фронт подключается к `http://localhost:8080/ws` (БЕЗ /api/)
- [ ] В консоли браузера: "WebSocket connected"
- [ ] Тестовое уведомление отправлено: `curl -X POST .../test/pain-escalation`
- [ ] В консоли браузера появилось уведомление
- [ ] Toast уведомление отобразилось на фронте

---

## 🚀 Следующие шаги

1. **Проверить реальные эскалации:**
   - Создать VAS с уровнем 6
   - Доктор одобряет рекомендацию
   - Создать VAS с уровнем 9
   - Проверить, что WebSocket уведомление пришло

2. **Проверить критические уведомления:**
   - Создать VAS с уровнем 10
   - Проверить, что пришло на `/topic/escalations/critical`

3. **Проверить подписку на несколько топиков:**
   ```typescript
   stompClient.subscribe('/topic/escalations/anesthesiologists', callback1);
   stompClient.subscribe('/topic/escalations/critical', callback2);
   ```

---

## 🐛 Известные проблемы

### 1. "global is not defined" (Vite)

**Решение:** Добавить в `vite.config.ts`:
```typescript
export default defineConfig({
  define: {
    global: 'globalThis',
  },
})
```

### 2. Дублирующиеся уведомления

**Причина:** Подписка выполняется несколько раз

**Решение:** Отписаться перед повторной подпиской:
```typescript
let subscription = null;

if (subscription) {
    subscription.unsubscribe();
}

subscription = stompClient.subscribe('/topic/...', callback);
```

---

## 📞 Контакты

**Backend:**
- `WebSocketConfig.java` - конфигурация WebSocket
- `WebSocketTestController.java` - тестовые endpoints
- `UnifiedNotificationService.java` - отправка уведомлений
- `PainEscalationNotificationService.java` - уведомления об эскалациях

**Frontend:**
- `useWebSocket.ts` - хук для подключения
- Проверить URL подключения: должен быть `http://localhost:8080/ws`

---

**Статус:** Готово к тестированию! 🎉

**Endpoint:** `http://localhost:8080/ws` (БЕЗ /api/)

**Топики:**
- `/topic/escalations/anesthesiologists`
- `/topic/escalations/doctors`
- `/topic/escalations/critical`
- `/topic/emr-alerts`

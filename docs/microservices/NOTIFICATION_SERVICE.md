# Notification Service Documentation

**Version:** 1.0.0  
**Port:** 8087  
**Status:** ✅ Implemented  
**Location:** `C:\backend_projects\microservices\notification-service\`

---

## Overview

Microservice for real-time notifications via WebSocket and Email with user preferences and notification history.

### Key Features

- ✅ Real-time WebSocket notifications (STOMP protocol)
- ✅ HTML Email notifications with Thymeleaf templates
- ✅ User notification preferences management
- ✅ Complete notification history and audit trail
- ✅ Automatic retry mechanism for failed notifications
- ✅ Kafka consumer for notification requests
- ✅ Scheduled retry job (every 15 minutes)

---

## Architecture

### Tech Stack

- **Java 21**
- **Spring Boot 3.5.5**
- **Spring WebSocket** (STOMP over SockJS)
- **Spring Mail** + **Thymeleaf**
- **Apache Kafka** (consumer)
- **PostgreSQL** (preferences + history)
- **Liquibase** (migrations)

### Database Schema

**Tables:**
1. `user_notification_preferences` - User settings (email/websocket toggles, per-type preferences)
2. `notification_history` - Complete audit trail with retry tracking
3. `notification_templates` - HTML email templates with variable placeholders

**Default Templates:**
- `RECOMMENDATION_CREATED`
- `RECOMMENDATION_APPROVED`
- `ESCALATION_CREATED`
- `EMR_CRITICAL_ALERT`

---

## Kafka Integration

### Consumes

**Topic:** `notification.requests`

**Message Schema:**
```json
{
  "notificationId": "uuid",
  "userId": "user123",
  "notificationType": "RECOMMENDATION_CREATED",
  "title": "New Recommendation",
  "message": "Recommendation created for patient",
  "metadata": {
    "patientName": "John Doe",
    "patientId": 123,
    "recommendationId": 456,
    "painLevel": 7
  },
  "priority": "HIGH",
  "timestamp": "2026-01-09T15:30:00",
  "emailEnabled": true,
  "websocketEnabled": true
}
```

---

## REST API

### User Preferences

```http
GET    /api/notifications/preferences/{userId}
POST   /api/notifications/preferences
PUT    /api/notifications/preferences/{userId}/email?email=new@example.com
PUT    /api/notifications/preferences/{userId}/toggle/{channel}?enabled=true
```

### Notification History

```http
GET    /api/notifications/history/{userId}
GET    /api/notifications/history/{userId}/recent?hours=24
GET    /api/notifications/stats/{userId}
```

---

## WebSocket Connection

### Endpoint

```
ws://localhost:8087/ws
```

### Subscribe to User Notifications

```javascript
stompClient.subscribe('/user/queue/notifications', (message) => {
  const notification = JSON.parse(message.body);
  console.log('Received:', notification);
});
```

### Message Format

```json
{
  "notificationId": "uuid",
  "type": "RECOMMENDATION_CREATED",
  "title": "New Recommendation",
  "message": "Details...",
  "priority": "HIGH",
  "timestamp": "2026-01-09T15:30:00",
  "metadata": {}
}
```

---

## Email Configuration

### SMTP Setup (Gmail Example)

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: your_email@gmail.com
    password: your_app_password  # Use App Password, not regular password
```

### Template Variables

Templates support `{{variableName}}` placeholders:

```html
<h2>Hello {{userName}}</h2>
<p>Patient {{patientName}} requires attention.</p>
<p>Pain Level: {{painLevel}}/10</p>
```

---

## User Preferences

### Granular Control

Users can configure:
- **Channels:** Email, WebSocket (toggle each independently)
- **Notification Types:**
  - `recommendation_created`
  - `recommendation_approved`
  - `recommendation_rejected`
  - `escalation_created`
  - `escalation_resolved`
  - `emr_critical_alert`
  - `pain_level_high`
  - `dose_administered`

### Default Preferences

All channels and types enabled by default for new users.

---

## Retry Mechanism

### Automatic Retry

- **Trigger:** Scheduled job runs every 15 minutes
- **Target:** Notifications with status `FAILED`
- **Max Attempts:** 3 (configurable)
- **Tracking:** Retry count stored in `notification_history`

### Configuration

```yaml
notification:
  retry:
    max-attempts: 3
    delay-ms: 5000
```

---

## Running the Service

### Prerequisites

```bash
# PostgreSQL
createdb notification_db

# Kafka
docker-compose up -d kafka
```

### Local Development

```bash
cd C:\backend_projects\microservices\notification-service
mvn spring-boot:run
```

### Environment Variables

```bash
DB_URL=jdbc:postgresql://localhost:5432/notification_db
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
SMTP_HOST=smtp.gmail.com
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_app_password
SERVER_PORT=8087
```

---

## Testing

### Test WebSocket

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const socket = new SockJS('http://localhost:8087/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  stompClient.subscribe('/user/queue/notifications', (message) => {
    console.log('Notification:', JSON.parse(message.body));
  });
});
```

### Send Test Notification (Kafka)

```bash
docker exec -it kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic notification.requests

# Paste JSON:
{"notificationId":"test-1","userId":"user123","notificationType":"RECOMMENDATION_CREATED","title":"Test","message":"Test notification"}
```

### Test Email

1. Configure SMTP in `application.yml`
2. Create user preference: `POST /api/notifications/preferences`
3. Send notification via Kafka
4. Check inbox

---

## Monitoring

### Endpoints

```http
GET /actuator/health
GET /actuator/metrics
GET /api/notifications/stats/{userId}
```

### Stats Response

```json
{
  "totalNotifications": 150,
  "successfulNotifications": 145,
  "failedNotifications": 5
}
```

---

## Integration with Monolith

### When to Send Notifications

The monolith should publish to `notification.requests` topic for:

1. **Recommendation Created** - Nurse creates recommendation
2. **Recommendation Approved** - Doctor approves recommendation
3. **Recommendation Rejected** - Doctor rejects recommendation
4. **Escalation Created** - Doctor creates escalation
5. **Escalation Resolved** - Anesthesiologist resolves escalation
6. **EMR Critical Alert** - EMR Integration Service detects critical values
7. **Pain Level High** - Patient records high pain level (VAS ≥ 7)
8. **Dose Administered** - Nurse administers pain medication

### Example: Monolith Publishing

```java
@Autowired
private KafkaTemplate<String, NotificationRequest> kafkaTemplate;

public void sendNotification(String userId, String type, String title, String message) {
    NotificationRequest request = NotificationRequest.builder()
        .notificationId(UUID.randomUUID().toString())
        .userId(userId)
        .notificationType(type)
        .title(title)
        .message(message)
        .timestamp(LocalDateTime.now())
        .priority("NORMAL")
        .build();
    
    kafkaTemplate.send("notification.requests", userId, request);
}
```

---

## Files Created

**Total:** 28 files, 1,979 lines

**Key Components:**
- `NotificationServiceApplication.java` - Main application
- `NotificationServiceImpl.java` - Core notification logic
- `EmailNotificationService.java` - Email sending
- `WebSocketNotificationService.java` - WebSocket broadcasting
- `NotificationRequestConsumer.java` - Kafka consumer
- `NotificationRetryScheduler.java` - Retry job
- `WebSocketConfig.java` - STOMP configuration
- `KafkaConsumerConfig.java` - Kafka setup

**Controllers:**
- `NotificationPreferenceController.java`
- `NotificationHistoryController.java`

**Entities:**
- `UserNotificationPreference.java`
- `NotificationHistory.java`
- `NotificationTemplate.java`

---

## Security Considerations

### Email

- Use app-specific passwords (not main account)
- Enable TLS/STARTTLS
- Validate email addresses

### WebSocket

- Configure `allowed-origins` for CORS
- Use `wss://` in production
- Implement authentication (future)

---

## Future Enhancements

- [ ] Push notifications (mobile)
- [ ] SMS via Twilio
- [ ] In-app notification center
- [ ] Read/unread tracking
- [ ] Notification grouping
- [ ] Template management UI
- [ ] Analytics dashboard

---

## Troubleshooting

### Emails Not Sending

1. Check SMTP credentials in logs
2. Query `notification_history` for error messages
3. Verify firewall allows port 587
4. Confirm Gmail app password (not regular password)

### WebSocket Not Connecting

1. Verify `allowed-origins` includes frontend URL
2. Check endpoint: `http://localhost:8087/ws`
3. Review browser console for CORS errors

### Notifications Skipped

1. Check user preferences via REST API
2. Verify notification type is enabled
3. Confirm email address is set

---

**Created:** 2026-01-09  
**Committed:** ✅  
**Ready for Integration:** ✅

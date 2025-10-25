package pain_helper_back.external_emr_integration_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/*
 * Конфигурация WebSocket для real-time уведомлений.
 * 
 * ЗАЧЕМ НУЖЕН WEBSOCKET:
 * - Мгновенное уведомление врачей о критических изменениях без перезагрузки страницы
 * - Real-time обновление дашборда с алертами
 * - Уведомления об эскалациях боли
 * - Снижение нагрузки на сервер (вместо постоянного polling)
 * 
 * ДОСТУПНЫЕ ТОПИКИ:
 * 1. /topic/emr-alerts - критические изменения в EMR
 * 2. /topic/escalations/doctors - эскалации боли для врачей
 * 3. /topic/escalations/anesthesiologists - эскалации для анестезиологов
 * 4. /topic/escalations/dashboard - мониторинг эскалаций
 * 5. /topic/escalations/critical - критические эскалации
 * 6. /topic/escalations/status-updates - обновления статусов эскалаций
 * 7. /queue/escalations - персональные уведомления врачу
 * 
 * КАК РАБОТАЕТ:
 * 1. Frontend подключается к WebSocket: ws://localhost:8080/ws-notifications
 * 2. Frontend подписывается на нужные топики
 * 3. Backend отправляет уведомления в топики при событиях
 * 4. Все подписанные клиенты мгновенно получают уведомления
 * 
 * ПРИМЕР ПОДКЛЮЧЕНИЯ (JavaScript):
 * const socket = new SockJS('http://localhost:8080/ws-notifications');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect({}, () => {
 *     // Подписка на эскалации боли
 *     stompClient.subscribe('/topic/escalations/doctors', (message) => {
 *         const escalation = JSON.parse(message.body);
 *         showEscalationNotification(escalation);
 *     });
 *     // Подписка на критические эскалации
 *     stompClient.subscribe('/topic/escalations/critical', (message) => {
 *         const critical = JSON.parse(message.body);
 *         showCriticalAlert(critical);
 *     });
 * });
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /*
     * Настройка message broker для маршрутизации сообщений.
     * 
     * /topic - для broadcast сообщений (все подписанные клиенты получат)
     * /queue - для персональных сообщений конкретному пользователю
     * /app - префикс для сообщений от клиента к серверу
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Включаем simple broker для топиков и очередей
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Префикс для сообщений от клиента
        registry.setApplicationDestinationPrefixes("/app");
        
        // Префикс для персональных сообщений
        registry.setUserDestinationPrefix("/user");
    }

    /*
     * Регистрация WebSocket endpoints.
     * 
     * Клиенты подключаются к: ws://localhost:8080/ws-notifications
     * С поддержкой SockJS fallback для старых браузеров
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Основной endpoint для всех уведомлений
        registry.addEndpoint("/ws-notifications")
                .setAllowedOrigins("*")  // В продакшене указать конкретные домены
                .withSockJS();  // Fallback для браузеров без WebSocket
        
        // Legacy endpoint для EMR alerts (обратная совместимость)
        registry.addEndpoint("/ws-emr-alerts")
                .setAllowedOrigins("*")
                .withSockJS();
    }
}

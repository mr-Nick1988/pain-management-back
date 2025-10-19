package pain_helper_back.external_emr_integration_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/*
 * Конфигурация WebSocket для real-time уведомлений о критических изменениях в EMR.
 * 
 * ЗАЧЕМ НУЖЕН WEBSOCKET:
 * - Мгновенное уведомление врачей о критических изменениях без перезагрузки страницы
 * - Real-time обновление дашборда с алертами
 * - Снижение нагрузки на сервер (вместо постоянного polling)
 * 
 * КАК РАБОТАЕТ:
 * 1. Frontend подключается к WebSocket: ws://localhost:8080/ws-emr-alerts
 * 2. Frontend подписывается на топик: /topic/emr-alerts
 * 3. Backend отправляет алерты в топик при обнаружении критических изменений
 * 4. Все подписанные клиенты мгновенно получают уведомления
 * 
 * ПРИМЕР ПОДКЛЮЧЕНИЯ (JavaScript):
 * const socket = new SockJS('http://localhost:8080/ws-emr-alerts');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect({}, () => {
 *     stompClient.subscribe('/topic/emr-alerts', (message) => {
 *         const alert = JSON.parse(message.body);
 *         showNotification(alert);
 *     });
 * });
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Настройка message broker для маршрутизации сообщений.
     * 
     * /topic - для broadcast сообщений (все подписанные клиенты получат)
     * /app - префикс для сообщений от клиента к серверу
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Включаем simple broker для топиков
        registry.enableSimpleBroker("/topic");
        
        // Префикс для сообщений от клиента
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Регистрация WebSocket endpoint.
     * 
     * Клиенты подключаются к: ws://localhost:8080/ws-emr-alerts
     * С поддержкой SockJS fallback для старых браузеров
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-emr-alerts")
                .setAllowedOrigins("*")  // В продакшене указать конкретные домены
                .withSockJS();  // Fallback для браузеров без WebSocket
    }
}

/*
 * UNIFIED WebSocket Configuration
 * 
 * ENDPOINTS:
 * - /ws - основной endpoint для всех WebSocket подключений
 * 
 * TOPICS:
 * - /topic/escalations/anesthesiologists - эскалации для анестезиологов
 * - /topic/escalations/doctors - эскалации для врачей
 * - /topic/escalations/critical - критические эскалации
 * - /topic/escalations/dashboard - мониторинг эскалаций
 * - /topic/emr-alerts - критические изменения в EMR
 * 
 * FRONTEND CONNECTION:
 * const socket = new SockJS('http://localhost:8080/ws');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect({}, () => {
 *     stompClient.subscribe('/topic/escalations/anesthesiologists', callback);
 * });
 */
package pain_helper_back.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Настройка брокера сообщений (куда фронт будет подписываться)
        config.enableSimpleBroker("/topic", "/queue");
        // Префикс, через который фронт шлёт запросы на бэк (например, если ты делаешь send из React)
        config.setApplicationDestinationPrefixes("/app");
        // Префикс для персональных сообщений
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Основной endpoint для всех WebSocket подключений
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
/*
 * UNIFIED WebSocket Configuration
 * 
 * ENDPOINTS:
 * - /ws - основной endpoint for allх WebSocket подключений
 * 
 * TOPICS:
 * - /topic/escalations/anesthesiologists - эскалации for anesthesiologistов
 * - /topic/escalations/doctors - эскалации for doctorей
 * - /topic/escalations/critical - критические эскалации
 * - /topic/escalations/dashboard - мониторинг эскалаций
 * - /topic/emr-alerts - критические fromменения в EMR
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
        // Префикс, via который фронт шлёт requestы на бэк (наExample, if ты делаешь send from React)
        config.setApplicationDestinationPrefixes("/app");
        // Префикс for персональных сообщений
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Основной endpoint for allх WebSocket подключений
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
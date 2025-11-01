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
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Точка подключения WebSocket-клиента (React)
        registry.addEndpoint("/ws")        // например ws://localhost:8080/ws
                .setAllowedOriginPatterns("*")
                .withSockJS();              // позволяет работать через fallback (для старых браузеров)
    }
}
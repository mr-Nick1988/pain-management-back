package pain_helper_back.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * Конфигурация для асинхронных операций и планировщика задач
 *
 * НАvalue:
 * - Включение @Async для асинхронной отправки email
 * - Включение @Scheduled для автоматической агрегации данных
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // Конфигурация by default
    // Spring Boot автоматически создаст ThreadPoolTaskExecutor
}


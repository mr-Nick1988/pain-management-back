package pain_helper_back.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * Конфигурация для асинхронных операций и планировщика задач
 *
 * НАЗНАЧЕНИЕ:
 * - Включение @Async для асинхронной отправки email
 * - Включение @Scheduled для автоматической агрегации данных
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    // Конфигурация по умолчанию
    // Spring Boot автоматически создаст ThreadPoolTaskExecutor
}


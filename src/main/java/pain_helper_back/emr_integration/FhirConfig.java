package pain_helper_back.emr_integration;

/**
 * Конфигурация HAPI FHIR клиента для интеграции с внешними EMR системами.
 *
 * FHIR (Fast Healthcare Interoperability Resources) - стандарт обмена медицинскими данными.
 * Используем FHIR R4 (4-я версия стандарта).
 *
 * Этот класс создает два Spring бина:
 * 1. FhirContext - контекст для работы с FHIR ресурсами (thread-safe, создается один раз)
 * 2. IGenericClient - клиент для выполнения FHIR запросов (read, search, create и т.д.)
 */

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirConfig {

    /**
     * URL FHIR сервера из application.properties.
     * По умолчанию используем публичный тестовый сервер HAPI FHIR.
     *
     * Можно переопределить в application.properties:
     * fhir.server.url=http://your-fhir-server.com/fhir
     */
    @Value("${fhir.server.url:http://hapi.fhir.org/baseR4}")
    private String fhirServerUrl;

    /**
     * Таймаут подключения к FHIR серверу (в миллисекундах).
     * По умолчанию 10 секунд.
     *
     * Connection timeout - время ожидания установки соединения с сервером.
     */
    @Value("${fhir.connection.timeout:10000}")
    private Integer connectionTimeout;

    /**
     * Таймаут сокета (в миллисекундах).
     * По умолчанию 10 секунд.
     *
     * Socket timeout - время ожидания ответа от сервера после установки соединения.
     */
    @Value("${fhir.socket.timeout:10000}")
    private Integer socketTimeout;

    /**
     * Создает FhirContext - основной объект HAPI FHIR для работы с FHIR ресурсами.
     *
     * FhirContext:
     * - Thread-safe (можно использовать из разных потоков)
     * - Создается один раз и переиспользуется (тяжелый объект)
     * - Содержит парсеры, валидаторы, настройки для FHIR R4
     *
     * ИСПРАВЛЕНО:
     * - Убрали 'new' перед FhirContext.forR4() - это статический метод, не конструктор
     * - Настраиваем таймауты через getRestfulClientFactory() (правильный API в HAPI FHIR 6.x)
     * - Возвращаем созданный объект fhirContext, а не создаем новый
     *
     * @return контекст для FHIR R4 с настроенными таймаутами
     */
    @Bean
    public FhirContext fhirContext() {
        // FhirContext.forR4() - СТАТИЧЕСКИЙ метод, создает контекст для FHIR R4 стандарта

        FhirContext fhirContext = FhirContext.forR4();
        // Настраиваем таймауты через IRestfulClientFactory
        // В HAPI FHIR 6.x методы setConnectionTimeout/setSocketTimeout удалены из IGenericClient
        // Правильный способ - настроить через factory в контексте
        fhirContext.getRestfulClientFactory().setConnectTimeout(connectionTimeout);
        fhirContext.getRestfulClientFactory().setSocketTimeout(socketTimeout);

        return fhirContext;
    }

    /**
     * Создает IGenericClient - клиент для выполнения FHIR запросов.
     *
     * IGenericClient позволяет:
     * - read() - получить ресурс по ID
     * - search() - искать ресурсы по критериям
     * - create() - создать новый ресурс
     * - update() - обновить существующий ресурс
     * - delete() - удалить ресурс
     *
     * Таймауты уже настроены в fhirContext, поэтому здесь их устанавливать не нужно.
     *
     * ИСПРАВЛЕНО:
     * - Добавили @Bean - без этого Spring не создаст бин, метод будет "мертвым кодом"
     * - Убрали client.setConnectionTimeout/setSocketTimeout - эти методы не существуют в HAPI FHIR 6.x
     *
     * @param fhirContext контекст FHIR (внедряется Spring)
     * @return настроенный FHIR клиент для указанного сервера
     */
    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        // Создаем REST клиент для указанного FHIR сервера
        // Таймауты уже настроены в fhirContext.getRestfulClientFactory()
        return fhirContext.newRestfulGenericClient(fhirServerUrl);
    }
}
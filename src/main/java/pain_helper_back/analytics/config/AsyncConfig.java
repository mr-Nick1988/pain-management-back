//package pain_helper_back.analytics.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//import java.util.concurrent.Executor;
//
///*
// * Конфигурация для асинхронной обработки событий
// * Используется для неблокирующей записи логов и событий в MongoDB
// */
//
//@Configuration
//@EnableAsync
//public class AsyncConfig {
//    @Bean(name = "analyticsTaskExecutor")
//    public Executor analyticsTaskExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(5);
//        executor.setMaxPoolSize(10);
//        executor.setQueueCapacity(100);
//        executor.setThreadNamePrefix("Analytics-");
//        executor.initialize();
//        return executor;
//    }
//}

package pain_helper_back.mongo_config;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import pain_helper_back.performance_SLA_monitoring.entity.PerformanceMetric;

@Configuration
@EnableMongoRepositories(
        basePackages = "pain_helper_back.performance_SLA_monitoring.repository",
        mongoTemplateRef = "performanceMongoTemplate"
)
public class PerformanceMongoConfig {

    @Bean(name = "performanceMongoClient")
    public MongoClient performanceMongoClient(@Value("${app.mongodb.performance.uri}") String uri) {
        return MongoClients.create(uri);
    }

    @Bean(name = "performanceMongoDbFactory")
    public MongoDatabaseFactory performanceMongoDbFactory(
            @Qualifier("performanceMongoClient") MongoClient client,
            @Value("${app.mongodb.performance.database}") String database
    ) {
        return new SimpleMongoClientDatabaseFactory(client, database);
    }

    @Bean(name = "performanceMongoTemplate")
    public MongoTemplate performanceMongoTemplate(
            @Qualifier("performanceMongoDbFactory") MongoDatabaseFactory factory
    ) {
        return new MongoTemplate(factory);
    }

    @Bean
    public ApplicationRunner performanceIndexes(
            @Qualifier("performanceMongoTemplate") MongoTemplate template
    ) {
        return args -> {
            template.indexOps(PerformanceMetric.class).createIndex(new Index().on("operationName", Sort.Direction.ASC));
            template.indexOps(PerformanceMetric.class).createIndex(new Index().on("slaViolated", Sort.Direction.ASC));
            template.indexOps(PerformanceMetric.class).createIndex(new Index().on("status", Sort.Direction.ASC));
            template.indexOps(PerformanceMetric.class).createIndex(new Index().on("timestamp", Sort.Direction.ASC));
        };
    }
}
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
import pain_helper_back.analytics.entity.AnalyticsEvent;

@Configuration
@EnableMongoRepositories(
        basePackages = "pain_helper_back.analytics.repository",
        mongoTemplateRef = "analyticsMongoTemplate"
)
public class AnalyticsMongoConfig {

    @Bean(name = "analyticsMongoClient")
    public MongoClient analyticsMongoClient(@Value("${app.mongodb.analytics.uri}") String uri) {
        return MongoClients.create(uri);
    }

    @Bean(name = "analyticsMongoDbFactory")
    public MongoDatabaseFactory analyticsMongoDbFactory(
            @Qualifier("analyticsMongoClient") MongoClient client,
            @Value("${app.mongodb.analytics.database}") String database
    ) {
        return new SimpleMongoClientDatabaseFactory(client, database);
    }

    @Bean(name = "analyticsMongoTemplate")
    public MongoTemplate analyticsMongoTemplate(
            @Qualifier("analyticsMongoDbFactory") MongoDatabaseFactory factory
    ) {
        return new MongoTemplate(factory);
    }

    // Гарантируем индексы (дополнительно к @Indexed)
    @Bean
    public ApplicationRunner analyticsIndexes(
            @Qualifier("analyticsMongoTemplate") MongoTemplate template
    ) {
        return args -> {
            template.indexOps(AnalyticsEvent.class).createIndex(new Index().on("timestamp", Sort.Direction.ASC));
            template.indexOps(AnalyticsEvent.class).createIndex(new Index().on("eventType", Sort.Direction.ASC));
            template.indexOps(AnalyticsEvent.class).createIndex(new Index().on("userId", Sort.Direction.ASC));
            template.indexOps(AnalyticsEvent.class).createIndex(new Index().on("diagnosisCodes", Sort.Direction.ASC));
        };
    }
}
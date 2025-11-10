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
import pain_helper_back.backup_restore.entity.BackupHistory;

@Configuration
@EnableMongoRepositories(
        basePackages = "pain_helper_back.backup_restore.repository",
        mongoTemplateRef = "backupMongoTemplate"
)
public class BackupMongoConfig {

    @Bean(name = "backupMongoClient")
    public MongoClient backupMongoClient(@Value("${app.mongodb.backup.uri}") String uri) {
        return MongoClients.create(uri);
    }

    @Bean(name = "backupMongoDbFactory")
    public MongoDatabaseFactory backupMongoDbFactory(
            @Qualifier("backupMongoClient") MongoClient client,
            @Value("${app.mongodb.backup.database}") String database
    ) {
        return new SimpleMongoClientDatabaseFactory(client, database);
    }

    @Bean(name = "backupMongoTemplate")
    public MongoTemplate backupMongoTemplate(
            @Qualifier("backupMongoDbFactory") MongoDatabaseFactory factory
    ) {
        return new MongoTemplate(factory);
    }

    @Bean
    public ApplicationRunner backupIndexes(
            @Qualifier("backupMongoTemplate") MongoTemplate template
    ) {
        return args -> {
            template.indexOps(BackupHistory.class).createIndex(new Index().on("backupType", Sort.Direction.ASC));
            template.indexOps(BackupHistory.class).createIndex(new Index().on("status", Sort.Direction.ASC));
            template.indexOps(BackupHistory.class).createIndex(new Index().on("startTime", Sort.Direction.DESC));
            template.indexOps(BackupHistory.class).createIndex(new Index().on("expirationDate", Sort.Direction.ASC));
        };
    }
}
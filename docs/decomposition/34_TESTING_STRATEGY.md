# 34 - Testing Strategy

**Предыдущий:** [33_EVENT_DRIVEN_PATTERNS.md](33_EVENT_DRIVEN_PATTERNS.md)  
**Следующий:** [35_DEPLOYMENT_STRATEGY.md](35_DEPLOYMENT_STRATEGY.md)

---

## 🎯 Пирамида тестирования

```
        ┌─────────────┐
        │   E2E Tests │  ← 10% (медленные, хрупкие)
        └─────────────┘
      ┌───────────────────┐
      │ Integration Tests │  ← 30% (средние)
      └───────────────────┘
    ┌───────────────────────┐
    │     Unit Tests        │  ← 60% (быстрые, надежные)
    └───────────────────────┘
```

---

## 🧪 Unit Tests

### Тестирование Service Layer

```java
@ExtendWith(MockitoExtension.class)
class LoggingServiceImplTest {
    
    @Mock
    private LogEntryRepository logEntryRepository;
    
    @InjectMocks
    private LoggingServiceImpl loggingService;
    
    @Test
    void testProcessLogEvent() {
        // Arrange
        LogEventDTO event = LogEventDTO.builder()
            .operation("testOperation")
            .serviceName("test-service")
            .build();
        
        when(logEntryRepository.save(any())).thenReturn(new LogEntry());
        
        // Act
        loggingService.processLogEvent(event);
        
        // Assert
        verify(logEntryRepository, times(1)).save(any(LogEntry.class));
    }
    
    @Test
    void testGetLogsByUserId() {
        // Arrange
        String userId = "user-123";
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now();
        
        List<LogEntry> expectedLogs = Arrays.asList(new LogEntry(), new LogEntry());
        when(logEntryRepository.findByUserIdAndTimestampBetween(userId, from, to))
            .thenReturn(expectedLogs);
        
        // Act
        List<LogEntry> result = loggingService.getLogsByUserId(userId, from, to);
        
        // Assert
        assertEquals(2, result.size());
        verify(logEntryRepository).findByUserIdAndTimestampBetween(userId, from, to);
    }
}
```

---

## 🔗 Integration Tests

### Тестирование с Kafka

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"logging-events"})
@TestPropertySource(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
class LogEventConsumerIntegrationTest {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private LogEntryRepository logEntryRepository;
    
    @Test
    void testConsumeLogEvent() throws Exception {
        // Arrange
        LogEventDTO event = LogEventDTO.builder()
            .id(UUID.randomUUID().toString())
            .timestamp(LocalDateTime.now())
            .serviceName("test-service")
            .operation("testOperation")
            .logLevel("INFO")
            .build();
        
        // Act
        kafkaTemplate.send("logging-events", event.getId(), event).get();
        
        // Wait for async processing
        Thread.sleep(2000);
        
        // Assert
        List<LogEntry> logs = logEntryRepository.findByServiceName("test-service");
        assertFalse(logs.isEmpty());
        assertEquals("testOperation", logs.get(0).getOperation());
    }
}
```

### Тестирование с MongoDB

```java
@SpringBootTest
@DataMongoTest
class LogEntryRepositoryIntegrationTest {
    
    @Autowired
    private LogEntryRepository repository;
    
    @Test
    void testSaveAndFind() {
        // Arrange
        LogEntry entry = LogEntry.builder()
            .timestamp(LocalDateTime.now())
            .serviceName("test-service")
            .operation("testOp")
            .userId("user-123")
            .logLevel("INFO")
            .build();
        
        // Act
        LogEntry saved = repository.save(entry);
        
        // Assert
        assertNotNull(saved.getId());
        
        List<LogEntry> found = repository.findByUserId("user-123");
        assertEquals(1, found.size());
        assertEquals("testOp", found.get(0).getOperation());
    }
}
```

---

## 🌐 End-to-End Tests

### REST API Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LoggingControllerE2ETest {
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private LogEntryRepository repository;
    
    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }
    
    @Test
    void testSearchLogsEndpoint() {
        // Arrange - создать тестовые данные
        LogEntry entry1 = createLogEntry("user-123", "operation1");
        LogEntry entry2 = createLogEntry("user-123", "operation2");
        repository.saveAll(Arrays.asList(entry1, entry2));
        
        // Act
        LocalDateTime from = LocalDateTime.now().minusDays(1);
        LocalDateTime to = LocalDateTime.now().plusDays(1);
        
        ResponseEntity<LogEntry[]> response = restTemplate.getForEntity(
            "/api/logs/user/user-123?from=" + from + "&to=" + to,
            LogEntry[].class
        );
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().length);
    }
    
    private LogEntry createLogEntry(String userId, String operation) {
        return LogEntry.builder()
            .timestamp(LocalDateTime.now())
            .serviceName("test-service")
            .operation(operation)
            .userId(userId)
            .logLevel("INFO")
            .build();
    }
}
```

---

## 🚀 Load Testing

### Gatling Script

```scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class LoggingServiceLoadTest extends Simulation {
  
  val httpProtocol = http
    .baseUrl("http://localhost:8081")
    .acceptHeader("application/json")
  
  val scn = scenario("Search Logs")
    .exec(http("search_logs")
      .get("/api/logs/search")
      .queryParam("from", "2025-10-31T00:00:00")
      .queryParam("to", "2025-10-31T23:59:59")
      .check(status.is(200)))
  
  setUp(
    scn.inject(
      rampUsersPerSec(10) to 100 during (2 minutes),
      constantUsersPerSec(100) during (5 minutes)
    )
  ).protocols(httpProtocol)
}
```

### JMeter Test Plan

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="Logging Service Load Test">
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="Users">
        <stringProp name="ThreadGroup.num_threads">100</stringProp>
        <stringProp name="ThreadGroup.ramp_time">60</stringProp>
        <stringProp name="ThreadGroup.duration">300</stringProp>
        
        <HTTPSamplerProxy>
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8081</stringProp>
          <stringProp name="HTTPSampler.path">/api/logs/search</stringProp>
          <stringProp name="HTTPSampler.method">GET</stringProp>
        </HTTPSamplerProxy>
      </ThreadGroup>
    </TestPlan>
  </hashTree>
</jmeterTestPlan>
```

---

## 🔍 Contract Testing

### Pact Consumer Test

```java
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "logging-service", port = "8081")
class LoggingServiceConsumerTest {
    
    @Pact(consumer = "analytics-service")
    public RequestResponsePact createPact(PactDslWithProvider builder) {
        return builder
            .given("logs exist for user")
            .uponReceiving("a request for user logs")
            .path("/api/logs/user/user-123")
            .method("GET")
            .willRespondWith()
            .status(200)
            .body(new PactDslJsonArray()
                .object()
                    .stringType("id")
                    .stringType("operation")
                    .stringType("userId", "user-123")
                .closeObject())
            .toPact();
    }
    
    @Test
    @PactTestFor(pactMethod = "createPact")
    void testGetUserLogs(MockServer mockServer) {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<LogEntry[]> response = restTemplate.getForEntity(
            mockServer.getUrl() + "/api/logs/user/user-123",
            LogEntry[].class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}
```

---

## 🐛 Chaos Testing

### Chaos Monkey

```yaml
# application.yml
chaos:
  monkey:
    enabled: true
    watcher:
      service: true
      repository: true
    assaults:
      level: 5
      latency-active: true
      latency-range-start: 1000
      latency-range-end: 5000
      exception-active: true
      exception-rate: 0.1
```

---

## 📊 Test Coverage

### Jacoco Configuration

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.10</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Целевые показатели

| Тип | Минимум | Целевой |
|-----|---------|---------|
| **Line Coverage** | 70% | 80%+ |
| **Branch Coverage** | 60% | 75%+ |
| **Method Coverage** | 80% | 90%+ |

---

## ✅ Test Checklist

### Перед каждым релизом

- [ ] Все unit тесты проходят
- [ ] Все integration тесты проходят
- [ ] E2E тесты проходят
- [ ] Load тесты показывают приемлемую производительность
- [ ] Test coverage > 80%
- [ ] Нет критических багов
- [ ] Contract тесты проходят
- [ ] Chaos тесты пройдены

---

**Следующий шаг:** [35_DEPLOYMENT_STRATEGY.md](35_DEPLOYMENT_STRATEGY.md)

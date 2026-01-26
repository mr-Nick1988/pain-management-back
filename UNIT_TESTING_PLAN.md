# 🧪 Unit Testing Plan - All Microservices

**Date:** January 25, 2026  
**Objective:** Cover all 8 microservices with unit tests  
**Stack:** JUnit 5 + Mockito + AssertJ + Spring Boot Test

---

## 📋 Testing Stack

### Already Included (via spring-boot-starter-test)
- ✅ **JUnit 5** - Testing framework
- ✅ **Mockito** - Mocking framework
- ✅ **AssertJ** - Fluent assertions
- ✅ **Spring Boot Test** - Spring context testing
- ✅ **Hamcrest** - Matchers
- ✅ **JSONassert** - JSON assertions

### Additional Dependencies Needed
- **Testcontainers** - For integration tests with PostgreSQL (optional)
- **WireMock** - For external service mocking (optional)

---

## 🎯 Microservices to Test (8 total)

### 1. API Gateway Service (port 8000)
**Priority:** HIGH - Entry point for all requests  
**Components to test:**
- [ ] JWT validation filters
- [ ] Route configuration
- [ ] Circuit breaker integration
- [ ] Service discovery integration

### 2. Authentication Service (port 8082)
**Priority:** CRITICAL - Security foundation  
**Components to test:**
- [ ] `AuthService` - registration, login, password change
- [ ] `JwtService` - token generation and validation
- [ ] `AuthController` - REST endpoints
- [ ] Password encoding
- [ ] Login event tracking

### 3. EMR Integration Service (port 8086)
**Priority:** HIGH - External system integration  
**Components to test:**
- [ ] EMR client service
- [ ] Data transformation
- [ ] Circuit breaker fallbacks
- [ ] Error handling

### 4. Notification Service (port 8087)
**Priority:** HIGH - Communication  
**Components to test:**
- [ ] Email notification service
- [ ] SMS notification service
- [ ] Notification templates
- [ ] Kafka event consumers

### 5. Pain Escalation Service (port 8088)
**Priority:** MEDIUM - Business logic  
**Components to test:**
- [ ] Escalation logic
- [ ] Alert generation
- [ ] Business rules

### 6. External VAS Integration Service (port 8089)
**Priority:** MEDIUM - External API  
**Components to test:**
- [ ] VAS client
- [ ] API request/response handling
- [ ] Circuit breaker

### 7. Reporting Service (port 8091)
**Priority:** MEDIUM - Analytics  
**Components to test:**
- [ ] Report generation
- [ ] Data aggregation
- [ ] Query builders

### 8. Backup & Restore Service (port 8085)
**Priority:** MEDIUM - Data management  
**Components to test:**
- [ ] Backup logic
- [ ] Restore logic
- [ ] File handling

---

## 📐 Test Structure Template

```
src/
├── main/java/
│   └── com/painmanagement/{service}/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       └── dto/
└── test/java/
    └── com/painmanagement/{service}/
        ├── controller/      # Controller tests (MockMvc)
        ├── service/         # Service tests (Mockito)
        └── integration/     # Integration tests (optional)
```

---

## 🧪 Test Types

### 1. Unit Tests (Priority: HIGH)
**Target:** Service layer, utility classes  
**Approach:** Mock all dependencies with Mockito  
**Coverage goal:** 70-80% for services

### 2. Controller Tests (Priority: MEDIUM)
**Target:** REST controllers  
**Approach:** MockMvc + mocked services  
**Coverage goal:** 60-70%

### 3. Integration Tests (Priority: LOW)
**Target:** Full flow with real DB  
**Approach:** Testcontainers + PostgreSQL  
**Coverage goal:** 40-50%

---

## 📝 Test Naming Convention

```java
// Pattern: methodName_scenario_expectedResult
@Test
void login_validCredentials_returnsLoginResponse()

@Test
void login_invalidPassword_throwsInvalidCredentialsException()

@Test
void generateAccessToken_validUser_returnsValidToken()
```

---

## 🔧 Mockito Best Practices

### 1. Use `@ExtendWith(MockitoExtension.class)`
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private AuthService authService;
}
```

### 2. Mock dependencies, test behavior
```java
when(userRepository.findByLogin("john")).thenReturn(Optional.of(user));
verify(userRepository, times(1)).save(any(User.class));
```

### 3. Use ArgumentCaptor for complex assertions
```java
ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
verify(userRepository).save(userCaptor.capture());
assertEquals("john", userCaptor.getValue().getLogin());
```

---

## 📊 Implementation Plan

### Phase 1: Setup & Template (Day 1)
- [x] Define testing stack
- [x] Create testing plan document
- [ ] Create test template for Authentication Service
- [ ] Write 5-10 example tests
- [ ] Document testing patterns

### Phase 2: Authentication Service (Day 1-2)
- [ ] Test `JwtService` (8-10 tests)
- [ ] Test `AuthService` (15-20 tests)
- [ ] Test `AuthController` (10-12 tests)
- [ ] Coverage: 70%+

### Phase 3: EMR Integration Service (Day 2-3)
- [ ] Test EMR client service
- [ ] Test data transformations
- [ ] Test circuit breakers
- [ ] Coverage: 60%+

### Phase 4: Notification Service (Day 3-4)
- [ ] Test email service
- [ ] Test SMS service
- [ ] Test Kafka consumers
- [ ] Coverage: 60%+

### Phase 5: Remaining Services (Day 4-7)
- [ ] Pain Escalation Service
- [ ] External VAS Service
- [ ] Reporting Service
- [ ] Backup & Restore Service
- [ ] Coverage: 50%+ each

### Phase 6: API Gateway (Day 7-8)
- [ ] Test filters
- [ ] Test route configuration
- [ ] Test circuit breakers
- [ ] Coverage: 50%+

### Phase 7: Coverage Report & Cleanup (Day 8-9)
- [ ] Generate JaCoCo coverage report
- [ ] Fix coverage gaps
- [ ] Code review
- [ ] Documentation update

---

## 🎯 Success Criteria

✅ **Overall code coverage:** 60%+ for all services  
✅ **Service layer coverage:** 70%+ for critical services  
✅ **All business logic covered** with unit tests  
✅ **No flaky tests** - all tests pass consistently  
✅ **Fast execution** - full test suite < 5 minutes  

---

## 🚀 Quick Start - Example Test

```java
package pain.management.authentication.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pain.management.authentication.repository.UserRepository;
import pain.management.authentication.entity.User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private JwtService jwtService;
    
    @Test
    void generateAccessToken_validUser_returnsToken() {
        // Given
        String personId = "12345";
        String role = "DOCTOR";
        String login = "john.doe";
        
        // When
        String token = jwtService.generateAccessToken(personId, role, login);
        
        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtService.extractPersonId(token)).isEqualTo(personId);
        assertThat(jwtService.extractRole(token)).isEqualTo(role);
    }
}
```

---

**Next Action:** Start with Authentication Service - create comprehensive test suite

**Estimated Time:** 1-2 weeks for all 8 microservices

**Last Updated:** January 25, 2026

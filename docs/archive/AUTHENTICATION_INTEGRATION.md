н# Authentication Service Integration Guide

**Version:** 1.0  
**Date:** 2026-01-08  
**Status:** Completed (Stage 1.1)

---

## Overview

The monolith has been integrated with the **Authentication Service** (port 8082) for JWT-based authentication. All login/password management functionality has been removed from the monolith and delegated to the external microservice.

---

## Architecture Changes

### Before (Monolith Authentication)
```
User → Monolith → PersonService.login()
                → Person Entity (with password field)
                → Plain-text password comparison
                → No JWT tokens
```

### After (Authentication Service)
```
User → Authentication Service → JWT tokens in HttpOnly cookies
     ↓
Monolith → JwtAuthenticationFilter → Validates JWT
         → SecurityContext populated with personId + role
         → Controllers use @AuthenticationPrincipal
```

---

## Components

### 1. JWT Validation Infrastructure

**JwtUtil** (`config/security/JwtUtil.java`)
- Parses and validates JWT tokens
- Extracts claims: personId, role, login, tokenType
- Uses HMAC secret key (must match Auth Service)

**JwtAuthenticationFilter** (`config/security/JwtAuthenticationFilter.java`)
- Intercepts all requests
- Extracts `accessToken` from cookies
- Validates token type (ACCESS, not REFRESH)
- Populates SecurityContext with authentication

**SecurityConfig** (`config/security/SecurityConfig.java`)
- Disables CSRF (stateless JWT)
- Enables CORS for frontend
- Temporarily permits all endpoints (for migration)
- Adds JWT filter before UsernamePasswordAuthenticationFilter

### 2. REST Client

**AuthenticationServiceClient** (`client/AuthenticationServiceClient.java`)
- `validateToken(String token)` - validate JWT with Auth Service
- `getUserInfo(String token)` - get user details
- Circuit Breaker pattern with Resilience4j
- Fallback methods for service unavailability

### 3. Utility Classes

**SecurityUtils** (`config/security/SecurityUtils.java`)
- `getCurrentPersonId()` - get personId from SecurityContext
- `getCurrentPersonIdOrThrow()` - get personId or throw exception
- `getCurrentRole()` - get user role
- `hasRole(String role)` - check if user has specific role
- `isAuthenticated()` - check authentication status

### 4. Database Migration

**Liquibase Changelog** (`db/changelog/changes/001-remove-password-from-person.xml`)
- Drops `password` column from `person` table
- Rollback support (re-adds column if needed)
- Precondition checks before execution

---

## Configuration

### application.yml

```yaml
# JWT Configuration (must match Authentication Service)
jwt:
  secret: ${JWT_SECRET:pain-management-secret-key-change-in-production...}

# Authentication Service Integration
auth:
  service:
    url: ${AUTH_SERVICE_URL:http://localhost:8082}
    timeout: ${AUTH_SERVICE_TIMEOUT:5000}

# Circuit Breaker for Auth Service
resilience4j:
  circuitbreaker:
    instances:
      authService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        permittedNumberOfCallsInHalfOpenState: 3
        automaticTransitionFromOpenToHalfOpenEnabled: true
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
        eventConsumerBufferSize: 10
```

### Environment Variables

**Required:**
- `JWT_SECRET` - shared secret with Authentication Service (min 256 bits for HS256)

**Optional:**
- `AUTH_SERVICE_URL` - Authentication Service URL (default: http://localhost:8082)
- `AUTH_SERVICE_TIMEOUT` - Request timeout in ms (default: 5000)

---

## Usage in Controllers

### Example: Get Current User

```java
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@GetMapping("/me")
public UserProfileDTO getCurrentUser(@AuthenticationPrincipal String personId) {
    return userService.getUserProfile(personId);
}
```

### Example: Role-Based Access Control

```java
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public void deleteUser(@PathVariable String id) {
    adminService.deleteUser(id);
}
```

### Example: Using SecurityUtils

```java
import pain_helper_back.config.security.SecurityUtils;

public void someBusinessLogic() {
    String currentUser = SecurityUtils.getCurrentPersonIdOrThrow();
    String role = SecurityUtils.getCurrentRole().orElse("UNKNOWN");
    
    if (SecurityUtils.hasRole("DOCTOR")) {
        // Doctor-specific logic
    }
}
```

---

## API Endpoints

### Removed from Monolith

❌ `POST /api/person/login` → Use `POST http://localhost:8082/api/auth/login`  
❌ `POST /api/person/change-credentials` → Use `POST http://localhost:8082/api/auth/change-password`

### Added to Monolith

✅ `GET /api/person/me` - Get current user profile (requires JWT)

### Authentication Service Endpoints

- `POST /api/auth/register` - Register new user (admin only)
- `POST /api/auth/login` - Login and receive JWT cookies
- `POST /api/auth/validate` - Validate access token
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/logout` - Logout and clear cookies
- `GET /api/auth/me` - Get current user info
- `POST /api/auth/change-password` - Change credentials

---

## Authentication Flow

### 1. Login (Frontend → Auth Service)

```javascript
// Frontend calls Authentication Service directly
const response = await fetch('http://localhost:8082/api/auth/login', {
  method: 'POST',
  credentials: 'include', // Important for cookies
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ login: 'doctor1', password: 'password123' })
});

// Auth Service sets HttpOnly cookies:
// - accessToken (15 min, Path=/)
// - refreshToken (7 days, Path=/api/auth/refresh)
```

### 2. API Request (Frontend → Monolith)

```javascript
// Frontend calls Monolith with cookies
const response = await fetch('http://localhost:8080/api/doctor/patients', {
  credentials: 'include' // Browser automatically sends cookies
});

// Monolith:
// 1. JwtAuthenticationFilter extracts accessToken from cookies
// 2. JwtUtil validates token and extracts personId, role
// 3. SecurityContext populated with authentication
// 4. Controller receives personId via @AuthenticationPrincipal
```

### 3. Token Refresh (Frontend → Auth Service)

```javascript
// When accessToken expires (15 min), refresh it
const response = await fetch('http://localhost:8082/api/auth/refresh', {
  method: 'POST',
  credentials: 'include' // Sends refreshToken cookie
});

// Auth Service validates refreshToken and issues new accessToken
```

### 4. Logout (Frontend → Auth Service)

```javascript
const response = await fetch('http://localhost:8082/api/auth/logout', {
  method: 'POST',
  credentials: 'include'
});

// Auth Service clears both cookies (Max-Age=0)
```

---

## Security Considerations

### JWT Secret

⚠️ **Critical:** The `jwt.secret` must be:
- **Identical** between Monolith and Authentication Service
- At least **256 bits** (32 characters) for HS256 algorithm
- **Stored securely** (environment variable, not in git)
- **Rotated regularly** in production

### Cookie Security

- **HttpOnly:** Cookies cannot be accessed by JavaScript (XSS protection)
- **Secure:** Set `cookie.secure=true` in production (HTTPS only)
- **SameSite:** Configure in reverse proxy if needed
- **Path restrictions:** refreshToken only sent to `/api/auth/refresh`

### CORS Configuration

```java
// SecurityConfig allows credentials from frontend
configuration.setAllowedOrigins(List.of("http://localhost:5173"));
configuration.setAllowCredentials(true);
```

### Session Management

- **Stateless:** No server-side sessions (`SessionCreationPolicy.STATELESS`)
- **JWT in cookies:** All state in signed token
- **No CSRF tokens needed:** CSRF disabled for stateless APIs

---

## Testing

### Manual Testing

1. **Start Authentication Service:**
   ```bash
   cd C:\backend_projects\microservices\authentication-service
   mvn spring-boot:run
   ```

2. **Start Monolith:**
   ```bash
   cd C:\backend_projects\pain_managment_back
   mvn spring-boot:run
   ```

3. **Login via Auth Service:**
   ```bash
   curl -X POST http://localhost:8082/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"login":"admin","password":"admin123"}' \
     -c cookies.txt
   ```

4. **Call Monolith with JWT:**
   ```bash
   curl http://localhost:8080/api/person/me \
     -b cookies.txt
   ```

### Integration Tests

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class JwtAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenValidJwt_thenSuccess() throws Exception {
        String validToken = generateTestToken();
        
        mockMvc.perform(get("/api/person/me")
                .cookie(new Cookie("accessToken", validToken)))
                .andExpect(status().isOk());
    }

    @Test
    void whenNoJwt_thenUnauthorized() throws Exception {
        mockMvc.perform(get("/api/person/me"))
                .andExpect(status().isUnauthorized());
    }
}
```

---

## Migration Checklist

- [x] Add JWT dependencies (jjwt, spring-security)
- [x] Create JwtUtil for token parsing
- [x] Create JwtAuthenticationFilter
- [x] Configure SecurityConfig with permitAll (temporary)
- [x] Create AuthenticationServiceClient with Circuit Breaker
- [x] Create Liquibase migration to drop password column
- [x] Remove password field from Person entity
- [x] Remove login/changeCredentials from PersonService
- [x] Update PersonController (remove auth endpoints)
- [x] Create SecurityUtils for easy JWT access
- [x] Update application.yml with JWT config
- [ ] Write unit tests for JwtUtil
- [ ] Write integration tests for JwtAuthenticationFilter
- [ ] Update SecurityConfig to require authentication (remove permitAll)
- [ ] Synchronize users between Monolith and Auth Service
- [ ] Test E2E flow with frontend

---

## Troubleshooting

### Issue: "No authenticated user found in SecurityContext"

**Cause:** JWT token missing or invalid

**Solution:**
1. Check if `accessToken` cookie is present in request
2. Verify JWT secret matches between services
3. Check token expiration (default 15 min)
4. Review JwtAuthenticationFilter logs

### Issue: "Authentication Service unavailable"

**Cause:** Auth Service not running or unreachable

**Solution:**
1. Check if Auth Service is running on port 8082
2. Verify network connectivity
3. Circuit Breaker will return fallback response
4. Check Resilience4j metrics

### Issue: "CORS error in browser"

**Cause:** Frontend origin not allowed

**Solution:**
1. Add frontend URL to SecurityConfig CORS config
2. Ensure `allowCredentials` is true
3. Check browser console for specific CORS error

### Issue: "Token validation failed"

**Cause:** Token signature mismatch

**Solution:**
1. Verify JWT_SECRET is identical in both services
2. Check token type (must be ACCESS, not REFRESH)
3. Ensure token hasn't expired
4. Review JwtUtil.parseToken() logs

---

## Future Improvements

### Short-term (STAGE 1.1G)
- [ ] Add `@PreAuthorize` annotations to controllers
- [ ] Write comprehensive test suite
- [ ] Enable full authentication (remove permitAll)

### Medium-term (STAGE 1.2-1.3)
- [ ] Add distributed tracing (correlation IDs)
- [ ] Implement token refresh logic in frontend
- [ ] Add audit logging for authentication events

### Long-term (STAGE 2+)
- [ ] Consider OAuth2/OpenID Connect
- [ ] Add multi-factor authentication
- [ ] Implement token revocation list
- [ ] Add session management dashboard

---

## References

- [Authentication Service Documentation](microservices/authentication-service-docs.md)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)

---

**Document Version:** 1.0  
**Last Updated:** 2026-01-08  
**Maintained by:** Cascade AI Assistant

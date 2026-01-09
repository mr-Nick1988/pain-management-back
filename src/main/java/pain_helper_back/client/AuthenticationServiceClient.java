package pain_helper_back.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pain_helper_back.client.dto.AuthValidationResponse;
import pain_helper_back.client.dto.UserInfoResponse;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${auth.service.url:http://localhost:8082}")
    private String authServiceUrl;

    @Value("${auth.service.timeout:5000}")
    private int timeoutMs;

    /**
     * Validate access token via Authentication Service
     */
    @CircuitBreaker(name = "authService", fallbackMethod = "validateTokenFallback")
    public AuthValidationResponse validateToken(String token) {
        log.debug("Validating token via Authentication Service");
        
        WebClient webClient = webClientBuilder
                .baseUrl(authServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            return webClient.post()
                    .uri("/api/auth/validate")
                    .cookie("accessToken", token)
                    .retrieve()
                    .bodyToMono(AuthValidationResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
        } catch (Exception e) {
            log.error("Failed to validate token via Authentication Service: {}", e.getMessage());
            throw new RuntimeException("Authentication Service unavailable", e);
        }
    }

    /**
     * Get user information by token
     */
    @CircuitBreaker(name = "authService", fallbackMethod = "getUserInfoFallback")
    public UserInfoResponse getUserInfo(String token) {
        log.debug("Getting user info via Authentication Service");
        
        WebClient webClient = webClientBuilder
                .baseUrl(authServiceUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        try {
            return webClient.get()
                    .uri("/api/auth/me")
                    .cookie("accessToken", token)
                    .retrieve()
                    .bodyToMono(UserInfoResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .block();
        } catch (Exception e) {
            log.error("Failed to get user info via Authentication Service: {}", e.getMessage());
            throw new RuntimeException("Authentication Service unavailable", e);
        }
    }

    /**
     * Fallback for validateToken - return invalid response
     */
    public AuthValidationResponse validateTokenFallback(String token, Exception e) {
        log.error("Circuit breaker activated for validateToken: {}", e.getMessage());
        AuthValidationResponse response = new AuthValidationResponse();
        response.setValid(false);
        response.setMessage("Authentication Service is temporarily unavailable");
        return response;
    }

    /**
     * Fallback for getUserInfo
     */
    public UserInfoResponse getUserInfoFallback(String token, Exception e) {
        log.error("Circuit breaker activated for getUserInfo: {}", e.getMessage());
        throw new RuntimeException("Authentication Service is temporarily unavailable");
    }
}

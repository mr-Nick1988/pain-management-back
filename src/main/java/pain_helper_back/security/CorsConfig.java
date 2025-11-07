package pain_helper_back.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for allowing credentials (HttpOnly cookies)
 * IMPORTANT: allowCredentials requires exact origin (NOT "*")
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // IMPORTANT: Specify exact origin (NOT "*") when using allowCredentials
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",  // Vite dev server
            "http://localhost:3000"   // Alternative React dev server
        ));
        
        // CRITICAL: Allow credentials (cookies) to be sent with requests
        configuration.setAllowCredentials(true);
        
        // Allowed HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // Allowed headers
        configuration.setAllowedHeaders(List.of("*"));
        
        // Expose Set-Cookie header to frontend
        configuration.setExposedHeaders(Arrays.asList("Set-Cookie"));
        
        // Max age for preflight requests (1 hour)
        configuration.setMaxAge(3600L);
        
        // Apply CORS configuration to all endpoints
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

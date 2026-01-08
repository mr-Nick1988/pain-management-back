package pain_helper_back.config.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utility class for working with Spring Security Context and JWT authentication
 */
@Slf4j
public class SecurityUtils {

    private SecurityUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Get current authenticated user's personId from JWT token
     * 
     * @return personId if authenticated, empty Optional otherwise
     */
    public static Optional<String> getCurrentPersonId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof String) {
                return Optional.of((String) authentication.getPrincipal());
            }
        } catch (Exception e) {
            log.error("Error getting current personId from SecurityContext: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Get current authenticated user's personId or throw exception
     * 
     * @return personId
     * @throws IllegalStateException if user is not authenticated
     */
    public static String getCurrentPersonIdOrThrow() {
        return getCurrentPersonId()
                .orElseThrow(() -> new IllegalStateException("No authenticated user found in SecurityContext"));
    }

    /**
     * Get current user's role from JWT token
     * 
     * @return role name (e.g., "ADMIN", "DOCTOR", "NURSE", "ANESTHESIOLOGIST")
     */
    public static Optional<String> getCurrentRole() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getAuthorities() != null) {
                return authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(auth -> auth.startsWith("ROLE_"))
                        .map(auth -> auth.substring(5)) // Remove "ROLE_" prefix
                        .findFirst();
            }
        } catch (Exception e) {
            log.error("Error getting current role from SecurityContext: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Check if current user has specific role
     * 
     * @param role role name (without "ROLE_" prefix)
     * @return true if user has the role
     */
    public static boolean hasRole(String role) {
        return getCurrentRole()
                .map(currentRole -> currentRole.equals(role))
                .orElse(false);
    }

    /**
     * Check if user is authenticated
     * 
     * @return true if authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && !(authentication.getPrincipal() instanceof String && "anonymousUser".equals(authentication.getPrincipal()));
    }
}

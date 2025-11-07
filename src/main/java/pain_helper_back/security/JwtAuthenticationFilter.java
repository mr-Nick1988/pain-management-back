package pain_helper_back.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Extract JWT token from HttpOnly cookie
            String token = extractTokenFromCookie(request);
            
            if (token != null && validateToken(token)) {
                // 2. Extract claims from token
                Claims claims = extractAllClaims(token);
                
                String personId = claims.get("personId", String.class);
                String role = claims.get("role", String.class);
                String firstName = claims.get("firstName", String.class);
                
                // 3. Create UserDetails
                UserDetails userDetails = new UserDetails();
                userDetails.setPersonId(personId);
                userDetails.setRole(role);
                userDetails.setFirstName(firstName);
                
                // 4. Create Authentication object
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                personId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                authentication.setDetails(userDetails);
                
                // 5. Save in SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("JWT authenticated from cookie: personId={}, role={}", personId, role);
            }
        } catch (Exception e) {
            log.error("JWT authentication failed: {}", e.getMessage());
        }
        
        // 6. Pass request to next filter
        filterChain.doFilter(request, response);
    }

    /**
     * Extract JWT token from HttpOnly cookie
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Validate JWT token
     */
    private boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Data
    public static class UserDetails {
        private String personId;
        private String role;
        private String firstName;
    }
}

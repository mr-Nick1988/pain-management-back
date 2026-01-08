package pain_helper_back.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        String token = extractTokenFromCookies(request);

        if (token != null && jwtUtil.validateToken(token)) {
            String tokenType = jwtUtil.getTokenTypeFromToken(token);
            
            // Проверяем, что это ACCESS токен (не REFRESH)
            if ("ACCESS".equals(tokenType)) {
                String personId = jwtUtil.getPersonIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                String login = jwtUtil.getLoginFromToken(token);

                if (personId != null && role != null) {
                    // Создаем Authentication объект
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                            personId, // principal
                            null, // credentials
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                    
                    // Добавляем детали запроса
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Устанавливаем в SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("JWT authentication successful for personId: {}, role: {}", personId, role);
                }
            } else {
                log.warn("Invalid token type: {} (expected ACCESS)", tokenType);
            }
        } else if (token != null) {
            log.warn("Invalid or expired JWT token");
        }

        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

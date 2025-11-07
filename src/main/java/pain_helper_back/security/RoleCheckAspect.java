package pain_helper_back.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * AOP Aspect for checking user roles based on @RequireRole annotation
 */
@Aspect
@Component
@Slf4j
public class RoleCheckAspect {

    @Around("@annotation(pain_helper_back.security.RequireRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. Get @RequireRole annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireRole requireRole = signature.getMethod().getAnnotation(RequireRole.class);
        String[] requiredRoles = requireRole.value();
        
        // 2. Get Authentication from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("Access denied: User is not authenticated");
            throw new AccessDeniedException("User is not authenticated");
        }
        
        // 3. Get user role
        JwtAuthenticationFilter.UserDetails userDetails = 
            (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        String userRole = userDetails.getRole();
        
        // 4. Check if user role is in the list of allowed roles
        boolean hasRole = Arrays.asList(requiredRoles).contains(userRole);
        
        if (!hasRole) {
            log.warn("Access denied for user {} with role {}. Required roles: {}", 
                userDetails.getPersonId(), userRole, Arrays.toString(requiredRoles));
            throw new AccessDeniedException(
                "Access denied. Required roles: " + Arrays.toString(requiredRoles)
            );
        }
        
        // 5. Role matches → execute method
        log.debug("Access granted for user {} with role {}", 
            userDetails.getPersonId(), userRole);
        return joinPoint.proceed();
    }
}

package pain_helper_back.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static String getUserIdOrSystem() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getName() != null) {
            return auth.getName();
        }
        return "system";
        
    }

    public static String getRoleOrUnknown() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getAuthorities() != null) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                String a = ga.getAuthority(); // e.g. ROLE_NURSE
                if (a != null && a.startsWith("ROLE_")) {
                    return a.substring(5);
                }
            }
        }
        return "UNKNOWN";
    }

    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated();
    }
}

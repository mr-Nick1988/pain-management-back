package pain_helper_back.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * Annotation for specifying required roles on controller methods
 * 
 * Usage:
 * @RequireRole("DOCTOR") - single role
 * @RequireRole({"DOCTOR", "ADMIN"}) - multiple roles
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();
}

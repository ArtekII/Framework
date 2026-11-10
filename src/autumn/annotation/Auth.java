package autumn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auth {
    String USER_ATTRIBUTE = "auth.user";
    String ROLE_ATTRIBUTE = "auth.role";

    /** Roles autorises : un seul role correspondant suffit. Vide = connexion uniquement. */
    String[] roles() default {};
}

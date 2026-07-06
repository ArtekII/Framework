package autumn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD) // S'applique uniquement sur les méthodes

public @interface UrlMapping {
    String value() default "";
    String method() default "GET"; // Méthode HTTP par défaut
}

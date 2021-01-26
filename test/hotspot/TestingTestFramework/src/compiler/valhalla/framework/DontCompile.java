package compiler.valhalla.framework;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Prevent method compilation
@Retention(RetentionPolicy.RUNTIME)
public @interface DontCompile {
    CompLevel[] value() default {};
}

package compiler.valhalla.framework;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Force method compilation
@Retention(RetentionPolicy.RUNTIME)
public @interface ForceCompile {
    CompLevel value() default CompLevel.ANY;
}

package compiler.valhalla.framework;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Run {
    String test() default "";
    RunMode mode() default RunMode.NORMAL;
}

package compiler.valhalla.framework;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Check {
    String test() default "";
    CheckAt when() default CheckAt.EACH_INVOCATION;
}

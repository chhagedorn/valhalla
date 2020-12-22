package compiler.valhalla.framework;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Number of warmup iterations
@Retention(RetentionPolicy.RUNTIME)
public @interface Warmup {
    int value();
}

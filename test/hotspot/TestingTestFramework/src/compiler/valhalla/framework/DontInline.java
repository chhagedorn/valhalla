package compiler.valhalla.framework;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Prevent method inlining during compilation
@Retention(RetentionPolicy.RUNTIME)
public @interface DontInline {
}

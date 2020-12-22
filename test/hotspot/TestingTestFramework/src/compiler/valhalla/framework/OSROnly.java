package compiler.valhalla.framework;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Do not enqueue the test method for compilation immediately after warmup loops have finished. Instead
// let the test method be compiled with on-stack-replacement.
@Retention(RetentionPolicy.RUNTIME)
public @interface OSROnly {}
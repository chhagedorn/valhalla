package compiler.valhalla.framework;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Test {
    // Regular expression used to match forbidden IR nodes
    // in the C2 IR emitted for this test.
    String failOn() default "";
    // Regular expressions used to match and count IR nodes.
    String[] match() default {};
    int[] matchCount() default {};
    CompLevel compLevel() default CompLevel.ANY;
    Skip[] skip() default {};
    int valid() default 0;
}

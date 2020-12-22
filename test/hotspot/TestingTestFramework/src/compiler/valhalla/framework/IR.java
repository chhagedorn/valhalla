package compiler.valhalla.framework;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Repeatable(IRs.class)
public @interface IR {
    // Regular expression used to match forbidden IR nodes
    // in the C2 IR emitted for this test.
    String[] failOn() default {};

    // Regular expressions used to match and count IR nodes.
    String[] counts() default {};
    String[] applyIf() default {};
    String[] applyIfNot() default {};
    String[] applyIfAnd() default {};
    String[] applyIfOr() default {};
}

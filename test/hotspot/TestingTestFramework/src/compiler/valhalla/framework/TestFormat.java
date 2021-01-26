package compiler.valhalla.framework;

import java.util.ArrayList;
import java.util.List;

public class TestFormat {
    private static final List<String> FAILURES = new ArrayList<>();

    public static void check(boolean test, String failureMessage) {
        if (!test) {
            FAILURES.add(failureMessage);
            throw new TestFormatException(failureMessage);
        }
    }

    public static void fail(String failureMessage) {
        FAILURES.add(failureMessage);
        throw new TestFormatException(failureMessage);
    }

    public static void reportIfAnyFailures() {
        if (FAILURES.isEmpty()) {
            // No format violation detected.
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("One or more format violations have been detected:\n\n");
        builder.append("Violations (").append(FAILURES.size()).append(")\n");
        builder.append("--------------\n");
        for (String failure : FAILURES) {
            builder.append(" - ").append(failure).append("\n");
        }
        FAILURES.clear();
        throw new TestFormatException(builder.toString());
    }
}

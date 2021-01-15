package compiler.valhalla.framework;

public class TestFormat {
    public static void check(boolean test, String failureMessage) {
        if (!test) {
            throw new TestFormatException(failureMessage);
        }
    }
    public static void fail(String failureMessage) {
        throw new TestFormatException(failureMessage);
    }

    public static void fail(String failureMessage, Exception e) {
        throw new TestFormatException(failureMessage, e);
    }
}

class TestFormatException extends RuntimeException {
    public TestFormatException(String message) {
        super(message);
    }

    public TestFormatException(String message, Exception e) {
        super(message, e);
    }
}

package compiler.valhalla.framework;

public class TestRunException extends RuntimeException {
    public TestRunException(String message) {
        super(message);
    }

    public TestRunException(String message, Exception e) {
        super(message, e);
    }
}

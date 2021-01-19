package compiler.valhalla.framework;

public class TestFormatException extends RuntimeException {
    public TestFormatException(String message) {
        super(message);
    }

    public TestFormatException(String message, Exception e) {
        super(message, e);
    }
}

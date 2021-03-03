package testframework;

// Errors in the framework
class TestFrameworkException extends RuntimeException {
    public TestFrameworkException(String message) {
        super(message);
    }
    public TestFrameworkException(String message, Exception e) {
        super(message, e);
    }
}

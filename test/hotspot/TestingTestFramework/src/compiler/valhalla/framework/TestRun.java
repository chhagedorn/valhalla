package compiler.valhalla.framework;

public class TestRun {
    public static void check(boolean test, String failureMessage) {
        if (!test) {
            throw new TestRunException(failureMessage);
        }
    }
    public static void fail(String failureMessage) {
        throw new TestRunException(failureMessage);
    }

    public static void fail(String failureMessage, Exception e) {
        throw new TestRunException(failureMessage, e);
    }
}

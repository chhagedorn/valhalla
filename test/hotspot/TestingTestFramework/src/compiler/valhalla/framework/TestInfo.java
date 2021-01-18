package compiler.valhalla.framework;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.Random;

public class TestInfo {
    private static final Random random = new Random();
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    private boolean toggleBool = false;
    private boolean onWarmUp = true;
    private final Method testMethod;

    public TestInfo(Method testMethod) {
        this.testMethod = testMethod;
    }

    public boolean toggleBoolean() {
        toggleBool = !toggleBool;
        return toggleBool;
    }

    public static int getRandomInt() {
        return random.nextInt() % 1000;
    }

    public static long getRandomLong() {
        return random.nextLong() % 1000;
    }

    public static double getRandomDouble() {
        return random.nextDouble() % 1000;
    }

    public boolean isWarmUp() {
        return onWarmUp;
    }

    void setWarmUpFinished() {
        onWarmUp = false;
    }

    public boolean isC2Compiled() {
        return TestFramework.isC2Compiled(testMethod);
    }

    public void assertDeoptimizedByC2() {
        TestFramework.assertDeoptimizedByC2(testMethod);
    }

    public void assertCompiledByC2(Method m) {
        TestFramework.assertCompiledByC2(testMethod);
    }
}

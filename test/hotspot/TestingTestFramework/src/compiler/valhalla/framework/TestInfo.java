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


    enum TriState {
        Maybe,
        Yes,
        No
    }

    public static boolean isC2Compiled(Method m) {
        return compiledByC2(m) == TriState.Yes;
    }

    public static void assertDeoptimizedByC2(Method m) {
        if (compiledByC2(m) == TriState.Yes) {
            throw new TestRunException(m + " should have been deoptimized");
        }
    }

    public static void assertCompiledByC2(Method m) {
        if (compiledByC2(m) == TriState.No) {
            throw new TestRunException(m + " should have been compiled");
        }
    }

    private static TriState compiledByC2(Method m) {
        if (!TestFramework.USE_COMPILER || TestFramework.XCOMP || TestFramework.TEST_C1 ||
                (TestFramework.STRESS_CC && !WHITE_BOX.isMethodCompilable(m, CompLevel.C2_FULL_OPTIMIZATION.getValue(), false))) {
            return TriState.Maybe;
        }
        if (WHITE_BOX.isMethodCompiled(m, false) &&
                WHITE_BOX.getMethodCompilationLevel(m, false) >= CompLevel.C2_FULL_OPTIMIZATION.getValue()) {
            return TriState.Yes;
        }
        return TriState.No;
    }
}

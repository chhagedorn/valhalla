package compiler.valhalla.framework;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.Random;

public class TestInfo {
    private static final Random random = new Random();
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    protected static final long PerMethodTrapLimit = (Long)WHITE_BOX.getVMFlag("PerMethodTrapLimit");
    protected static final boolean ProfileInterpreter = (Boolean)WHITE_BOX.getVMFlag("ProfileInterpreter");

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

    public Method getTest() {
        return testMethod;
    }

//    public boolean isC2Compiled(Method m) {
//        return WHITE_BOX.isMethodCompiled(m, false) && WHITE_BOX.getMethodCompilationLevel(m, false) == CompLevel.C2.getValue();
////        return compiledByC2(m) == TriState.Yes;
//    }
//
//    public boolean isCompiledAtLevel(Method m, CompLevel level) {
//        return WHITE_BOX.isMethodCompiled(m, false) && WHITE_BOX.getMethodCompilationLevel(m, false) == level.getValue();
////        return compiledByC2(m) == TriState.Yes;
//    }
//
//    public void assertDeoptimizedByC2(Method m) {
//        TestRun.check(!isC2Compiled(m) || PerMethodTrapLimit == 0 || !ProfileInterpreter, m + " should have been deoptimized");
//    }
//
//    public void assertCompiledByC2(Method m) {
//        TestRun.check(isC2Compiled(m), m + " should have been compiled");
//    }
//
//    public void assertCompiledAtLevel(Method m, CompLevel level) {
//        TestRun.check(isCompiledAtLevel(m, level), m + " should have been compiled at level " + level.name());
//    }
}

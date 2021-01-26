package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;

public class TestControls {
    static int[] executed = new int[13];
    static boolean wasExecuted = false;
    static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    public static void main(String[] args) throws Exception {
        Method runTestsOnSameVM = TestFramework.class.getDeclaredMethod("runTestsOnSameVM", Class.class);
        runTestsOnSameVM.setAccessible(true);
        runTestsOnSameVM.invoke(null, new Object[]{ null });
        final int defaultIterations = TestFramework.WARMUP_ITERATIONS + 1;
        Asserts.assertEQ(executed[0], 1001);
        Asserts.assertEQ(executed[1], 101);
        Asserts.assertEQ(executed[2], 10000);
        Asserts.assertEQ(executed[3], 10000);
        Asserts.assertEQ(executed[4], defaultIterations);
        Asserts.assertEQ(executed[5], defaultIterations);
        Asserts.assertEQ(executed[6], 5001);
        Asserts.assertEQ(executed[7], 5001);
        Asserts.assertEQ(executed[8], 1);
        Asserts.assertEQ(executed[9], 5000);
        Asserts.assertEQ(executed[10], 1);
        Asserts.assertEQ(executed[11], 2);
        Asserts.assertEQ(executed[12], 1);
        Asserts.assertFalse(wasExecuted);
    }

    @Test
    @Warmup(1000)
    public void test1() {
        executed[0]++;
    }

    @Check(test = "test1")
    public void check1(TestInfo info) {
        if (executed[0] <= 1000) {
            Asserts.assertTrue(info.isWarmUp());
        } else {
            Asserts.assertTrue(!info.isWarmUp() && executed[0] == 1001);
            TestFramework.assertCompiledByC2(info.getTest());
        }
    }

    @Test
    @Warmup(100)
    public void test2() {
        executed[1]++;
    }

    @Check(test = "test2", when = CheckAt.COMPILED)
    public void check2(TestInfo info) throws NoSuchMethodException{
        Asserts.assertTrue(!info.isWarmUp() && executed[1] == 101);
        TestFramework.assertCompiledByC2(info.getTest());
        TestFramework.assertCompiledByC2(TestControls.class.getDeclaredMethod("overload", int.class));
        TestFramework.assertCompiledAtLevel(TestControls.class.getDeclaredMethod("overload", double.class), CompLevel.C1_LIMITED_PROFILE);
    }

    @Test
    public void overload() {
        executed[4]++;
    }

    @ForceCompile
    @DontInline
    public static void overload(int i) {
        wasExecuted = true;
    }

    @ForceCompile(CompLevel.C1_LIMITED_PROFILE)
    @ForceInline
    public static void overload(double i) {
        wasExecuted = true;
    }

    @Check(test = "overload")
    public void checkOverload()  {
        executed[5]++;
    }

    @Test
    public void testDontCompile() {
        executed[2]++;
    }
    
    @DontCompile
    public static void dontCompile() {
        executed[3]++;
    }
    
    @Run(test = "testDontCompile", mode = RunMode.ONCE)
    public void runTestDontCompile() throws NoSuchMethodException {
        for (int i = 0; i < 10000; i++) {
            dontCompile(); // Should not compile this method
            testDontCompile();
        }
        TestFramework.assertNotCompiled(TestControls.class.getDeclaredMethod("dontCompile"));
    }

    @Test
    public void testCompileAtLevel1() {
        executed[6]++;
    }

    @DontCompile({CompLevel.C1_FULL_PROFILE, CompLevel.C1_LIMITED_PROFILE, CompLevel.C2})
    public static void dontCompile2() {
        executed[7]++;
    }

    @Run(test = "testCompileAtLevel1")
    @Warmup(5000)
    public void runTestDontCompile2(TestInfo info) throws NoSuchMethodException {
        dontCompile2();
        testCompileAtLevel1();
        if (!info.isWarmUp()) {
            executed[8]++;
            int compLevel = WHITE_BOX.getMethodCompilationLevel(TestControls.class.getDeclaredMethod("dontCompile2"), false);
            Asserts.assertLessThan(compLevel, CompLevel.C1_LIMITED_PROFILE.getValue());
        } else {
            executed[9]++;
        }
    }

    @Test
    @Warmup(0)
    public void noWarmup() {
        executed[10]++;
    }

    @Test
    public void noWarmup2() {
        executed[11]++;
    }

    @Run(test = "noWarmup2")
    @Warmup(0)
    public void runNoWarmup2(TestInfo info) {
        noWarmup2();
        noWarmup2();
        Asserts.assertTrue(!info.isWarmUp());
        executed[12]++;
    }
}

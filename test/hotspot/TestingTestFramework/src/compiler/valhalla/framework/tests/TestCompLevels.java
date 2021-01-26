package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;
import jdk.test.lib.Asserts;

import java.lang.reflect.Method;

// Requires C1 and C2 enabled
public class TestCompLevels {
    static int[] testExecuted = new int[4];

    public static void main(String[] args) throws Exception {
        Method runTestsOnSameVM = TestFramework.class.getDeclaredMethod("runTestsOnSameVM");
        runTestsOnSameVM.setAccessible(true);
        runTestsOnSameVM.invoke(null);
        for (int i = 0; i < testExecuted.length; i++) {
            int value = testExecuted[i];
            if (value != TestFramework.WARMUP_ITERATIONS + 1) {
                // Warmups + 1 compiled invocation
                throw new RuntimeException("Test " + i + "  was executed " + value + " times stead of "
                                                   + TestFramework.WARMUP_ITERATIONS + 1 + " times." );
            }
        }
        Scenario s = new Scenario(1, "-XX:-TieredCompilation");
        TestFramework.runWithScenarios(TestNoTiered.class, s);
        s = new Scenario(1, "-XX:TieredStopAtLevel=1");
        TestFramework.runWithScenarios(TestStopAtLevel1.class, s);
        Asserts.assertTrue(TestFramework.getLastVmOutput().contains("TestStopAtLevel1=34"));
    }

    @Test(compLevel = CompLevel.C1)
    public void testC1() {
        testExecuted[0]++;
    }

    @Check(test = "testC1", when = CheckAt.COMPILED)
    public void checkTestC1(TestInfo info) {
        TestFramework.assertCompiledAtLevel(info.getTest(), CompLevel.C1);
    }

    @Test(compLevel = CompLevel.C1_LIMITED_PROFILE)
    public void testC1Limited() {
        testExecuted[1]++;
    }

    @Check(test = "testC1Limited", when = CheckAt.COMPILED)
    public void checkTestLimited(TestInfo info) {
        TestFramework.assertCompiledAtLevel(info.getTest(), CompLevel.C1_LIMITED_PROFILE);
    }

    @Test(compLevel = CompLevel.C1_FULL_PROFILE)
    public void testC1Full() {
        testExecuted[2]++;
    }

    @Check(test = "testC1Full", when = CheckAt.COMPILED)
    public void checkTestC1Full(TestInfo info) {
        TestFramework.assertCompiledAtLevel(info.getTest(), CompLevel.C1_FULL_PROFILE);
    }

    @Test(compLevel = CompLevel.C2)
    public void testC2() {
        testExecuted[3]++;
    }

    @Check(test = "testC2", when = CheckAt.COMPILED)
    public void checkTestC2(TestInfo info) {
        TestFramework.assertCompiledAtLevel(info.getTest(), CompLevel.C2);
    }
}

class TestNoTiered {
    @Test(compLevel = CompLevel.C1)
    public void notExecuted() {
        throw new RuntimeException("Should not be executed");
    }

    @Test(compLevel = CompLevel.C1_LIMITED_PROFILE)
    public void notExecuted1() {
        throw new RuntimeException("Should not be executed");
    }

    @Test(compLevel = CompLevel.C1_FULL_PROFILE)
    public void notExecuted2() {
        throw new RuntimeException("Should not be executed");
    }

    @Test(compLevel = CompLevel.SKIP)
    public void notExecuted3() {
        throw new RuntimeException("Should not be executed");
    }
}

class TestStopAtLevel1 {
    @Test(compLevel = CompLevel.C2)
    public void notExecuted() {
        throw new RuntimeException("Should not be executed");
    }

    @Test(compLevel = CompLevel.C1_LIMITED_PROFILE)
    public void notExecuted1() {
        throw new RuntimeException("Should not be executed");
    }

    @Test(compLevel = CompLevel.C1_FULL_PROFILE)
    public void notExecuted2() {
        throw new RuntimeException("Should not be executed");
    }

    @Test(compLevel = CompLevel.C1)
    public int executed() {
        return 34;
    }

    @Check(test = "executed", when = CheckAt.COMPILED)
    public void checkExecuted(int result) {
        System.out.println("TestStopAtLevel1=" + result);
    }
}

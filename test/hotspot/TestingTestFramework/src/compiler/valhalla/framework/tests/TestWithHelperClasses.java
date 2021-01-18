package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;
import jdk.test.lib.Asserts;

public class TestWithHelperClasses {

    public static void main(String[] args) throws NoSuchMethodException {
        TestFramework.runWithHelperClasses(TestWithHelperClasses.class, Helper1.class, Helper2.class);
        try {
            TestFramework.runWithHelperClasses(TestWithHelperClasses.class, Helper1.class);
        } catch (TestRunException e) {
            Asserts.assertTrue(e.getMessage().contains("public static void compiler.valhalla.framework.tests.Helper2.foo() should have been compiled"));
        }
    }

    @Test
    public void test() throws NoSuchMethodException {
        TestFramework.assertCompiledByC2(Helper1.class.getMethod("foo"));
        TestFramework.assertCompiledByC2(Helper2.class.getMethod("foo"));
    }
}

class Helper1 {

    @ForceCompile(CompLevel.C2)
    public static void foo() {
        throw new RuntimeException("Should not be executed");
    }
}

class Helper2 {

    @ForceCompile(CompLevel.C2)
    public static void foo() {
        throw new RuntimeException("Should not be executed");
    }
}

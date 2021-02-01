package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;
import jdk.test.lib.Asserts;

import java.lang.reflect.Method;

public class TestBadFormat {

    private static Method runTestsOnSameVM;
    public static void main(String[] args) throws NoSuchMethodException {
        runTestsOnSameVM = TestFramework.class.getDeclaredMethod("runTestsOnSameVM", Class.class);
        runTestsOnSameVM.setAccessible(true);
        expectTestFormatException(BadArguments.class);
        expectTestFormatException(BadOverloadedMethod.class);
        expectTestFormatException(BadCompilerControl.class);
        expectTestFormatException(BadWarmup.class);
    }

    private static void expectTestFormatException(Class<?> clazz) {
        try {
            runTestsOnSameVM.invoke(null, clazz);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                System.out.println(cause.getMessage());
            }
            Asserts.assertTrue(cause instanceof TestFormatException, "Unexpected exception: " + cause);
            String msg = cause.getMessage();
            Asserts.assertTrue(msg.contains("Violations"));
            return;
        }
        throw new RuntimeException("Should catch an exception");
    }
}

class BadArguments {

    @Test
    public void noArgAnnotation(int a) {}

    @Test
    @Arguments(Argument.DEFAULT)
    public void argNumberMismatch(int a, int b) {}

    @Test
    @Arguments(Argument.DEFAULT)
    public void argNumberMismatch2() {}

    @Test
    @Arguments(Argument.NUMBER_42)
    public void notBoolean(boolean a) {}

    @Test
    @Arguments(Argument.NUMBER_MINUS_42)
    public void notBoolean2(boolean a) {}

    @Test
    @Arguments(Argument.TRUE)
    public void notNumber(int a) {}

    @Test
    @Arguments(Argument.FALSE)
    public void notNumber2(int a) {}

    @Test
    @Arguments(Argument.BOOLEAN_TOGGLE_FIRST_TRUE)
    public void notNumber3(int a) {}

    @Test
    @Arguments(Argument.BOOLEAN_TOGGLE_FIRST_FALSE)
    public void notNumber4(int a) {}

    @Test
    @Arguments({Argument.BOOLEAN_TOGGLE_FIRST_FALSE, Argument.TRUE})
    public void notNumber5(boolean a, int b) {}


    @Test
    @Arguments({Argument.BOOLEAN_TOGGLE_FIRST_FALSE, Argument.NUMBER_42})
    public void notNumber6(int a, boolean b) {}
}

class BadOverloadedMethod {

    @Test
    public void sameName() {}

    @Test
    @Arguments(Argument.DEFAULT)
    public void sameName(boolean a) {}

    @Test
    @Arguments(Argument.DEFAULT)
    public void sameName(double a) {}
}

class BadCompilerControl {

    @Test
    @DontCompile
    public void test1() {}

    @Test
    @ForceCompile
    public void test2() {}

    @Test
    @DontInline
    public void test3() {}

    @Test
    @ForceInline
    public void test4() {}

    @Test
    @ForceInline
    @ForceCompile
    @DontInline
    @DontCompile
    public void test5() {}

    @DontInline
    @ForceInline
    public void mix1() {}

    @DontCompile
    @ForceCompile
    public void mix2() {}
}

class BadWarmup {

    @Warmup(10000)
    public void warmUpNonTest() {}

    @Test
    @Warmup(-1)
    public void negativeWarmup() {}

    @Run(test = "negativeWarmup")
    @Warmup(-1)
    public void negativeWarmup2() {}
}

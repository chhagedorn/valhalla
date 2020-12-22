package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Stream;

public class TestPackagePrivate {
    public static void main(String[] args) {
        TestFramework.run(PackagePrivate.class);
    }
}

//    @Test
//    @Arguments({ArgumentValue.DEFAULT, ArgumentValue.FALSE, ArgumentValue.NUMBER_42, ArgumentValue.RANDOM_ALL, ArgumentValue.RANDOM_ONCE, ArgumentValue.BOOLEAN_TOGGLE})
//    public int test(int arg1, boolean arg2, double arg3, double arg4, int arg5, boolean arg6) {
//        return 0;
//    }
//
//    // Useful for quick checking, nothing fancy going on. This method is optional.
//    @Check(test="test", when=CheckAt.C2_COMPILED)
//    // Must match method 'test' when removing '_check'. Could also think about matching in annotation, e.g. @Check(test="test2").
//    public void test_check(int result /* must match return argument of 'test', possible to check? */) {
//        // This method runs in interpreter, DontCompile.
//        // Check that there is a method 'test' with @Test, no method 'test_check' or when present no @Run at it (bad style though, better use check in annotation?),
//        // 'test' has non-void return (if void, what do you need the check for then?)
//        // If 'test' has arguments but no @Arguments annotation, framework takes default arguments (bad style though).
//        // Framework executes 'test' with warmup (specified or default).
//        // This method is then called from framework once after 'test' compiled or once when 'test' is called the first time by framework and after 'test' is compiled
////        Asserts.assertEQ(result, 0);
//    }
//
//    @Test
//    public void test2(int arg1, boolean arg2, int arg3, int arg4) {
//    }
//
//    // Optional method.
//    // Useful when more complex/changing arguments are required. Framework calls this method in interpreter and let it handle how to call the method
//    // 'test'. Framework could verify that this method has at least one call to 'test2'?
//    @Run(test="test2")
//    // Must match method 'test2' when removing '_run'. Could also think about matching in annotation, e.g. @Run(test="test2").
//    public void test2_run(TestInfo info) {
//        // This method runs in interpreter, DontCompile
//        // Check that there is a method 'test2' with @Test.
//        // Check that no @Arguments present in 'test2' (not useful when specifying here how to call the method)
//        // Called each time by framework, specifies how to run test
//        if (info.isWarmUp()) {
//            test2(34, info.toggleBoolean(), info.getRandomInt(), 0);
//        } else {
//            test2(12, true, info.getRandomInt(), -555);
//        }
//    }

 class PackagePrivate {
    @Test
    public void test() {
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void test2(int x) {
    }
}
package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

public class TestBasics {
    public static void main(String[] args) throws Exception {
        // Run on same VM to make this test easier as we are not interested in any output processing.
        Method runTestsOnSameVM = TestFramework.class.getDeclaredMethod("runTestsOnSameVM", Class.class);
        runTestsOnSameVM.setAccessible(true);
        runTestsOnSameVM.invoke(null, new Object[]{ null });

        if (wasExecuted) {
            throw new RuntimeException("Executed non @Test method or a method that was not intended to be run");
        }
        for (int i = 0; i < executed.length; i++) {
            int value = executed[i];
            if (value != TestFramework.WARMUP_ITERATIONS + 1) {
                // Warmups + 1 C2 compiled invocation
                throw new RuntimeException("Test " + i + "  was executed " + value + " times stead of "
                        + TestFramework.WARMUP_ITERATIONS + 1 + " times." );
            }
        }

        for (int value : executedOnce) {
            if (value != 1) {
                throw new RuntimeException("Check function should have been executed exactly once");
            }
        }
    }

//    @Test
//    @Arguments({ArgumentValue.DEFAULT, ArgumentValue.FALSE, ArgumentValue.NUMBER_42, ArgumentValue.RANDOM_ALL, ArgumentValue.RANDOM_ONCE, ArgumentValue.BOOLEAN_TOGGLE})
//    public int test(int arg1, boolean arg2, double arg3, double arg4, int arg5, boolean arg6) {
//        return 0;
//    }
//
//    // Useful for quick checking, nothing fancy going on. This method is optional.
//    @Check(test = "test", when=CheckAt.C2_COMPILED)
//    // Must match method 'test' when removing '_check'. Could also think about matching in annotation, e.g. @Check(test = "test2").
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
//    @Run(test = "test2")
//    // Must match method 'test2' when removing '_run'. Could also think about matching in annotation, e.g. @Run(test = "test2").
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

    static boolean wasExecuted = false;
    static int[] executed = new int[78];
    static int[] executedOnce = new int[5];
    boolean lastToggleBoolean = true;
    long[] nonFloatingRandomNumbers = new long[10];
    double[] floatingRandomNumbers = new double[10];
    Boolean[] randomBooleans = new Boolean[64];

    private void clearNonFloatingRandomNumbers() {
        nonFloatingRandomNumbers = new long[10];
    }

    private void clearFloatingRandomNumbers() {
        floatingRandomNumbers = new double[10];
    }

    private void clearRandomBooleans() {
        randomBooleans = new Boolean[64];
    }

    // Base test, no arguments, directly invoked.
    @Test
    public void test() {
        executed[0]++;
    }

    // Not a test
    public void noTest() {
        wasExecuted = true;
    }

    // Not a test
    public void test2() {
        wasExecuted = true;
    }

    // Can overload non- @Test
    public static void test2(int i) {
        wasExecuted = true;
    }

    // Can overload a @Test if it is not a @Test itself.
    public static void test(double i) {
        wasExecuted = true;
    }

    @Test
    public static void staticTest() {
        executed[1]++;
    }

    @Test
    public final void finalTest() {
        executed[2]++;
    }

    @Test
    public int returnValueTest() {
        executed[3]++;
        return 4;
    }

    // Base test, with arguments, directly invoked.
    // Specify the argument values with @Arguments
    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void byteDefaultArgument(byte x) {
        executed[4]++;
        if (x != 0) {
            throw new RuntimeException("Must be 0");
        }
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void shortDefaultArgument(short x) {
        executed[5]++;
        if (x != 0) {
            throw new RuntimeException("Must be 0");
        }
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void intDefaultArgument(int x) {
        executed[6]++;
        if (x != 0) {
            throw new RuntimeException("Must be 0");
        }
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void longDefaultArgument(long x) {
        executed[7]++;
        if (x != 0L) {
            throw new RuntimeException("Must be 0");
        }
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void floatDefaultArgument(float x) {
        executed[8]++;
        if (x != 0.0f) {
            throw new RuntimeException("Must be 0.0");
        }
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void doubleDefaultArgument(double x) {
        executed[9]++;
        if (x != 0.0f) {
            throw new RuntimeException("Must be 0.0");
        }
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void charDefaultArgument(char x) {
        executed[10]++;
        if (x != '\u0000') {
            throw new RuntimeException("Must be \u0000");
        }
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void booleanDefaultArgument(boolean x) {
        executed[11]++;
        if (x) {
            throw new RuntimeException("Must be false");
        }
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void stringObjectDefaultArgument(String x) {
        executed[12]++;
        if (x == null || x.length() != 0) {
            throw new RuntimeException("Default string object must be non-null and having a length of zero");
        }
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void defaultObjectDefaultArgument(DefaultObject x) {
        executed[13]++;
        if (x == null || x.i != 4) {
            throw new RuntimeException("Default object must not be null and its i field must be 4");
        }
    }

    @Test
    @Arguments(ArgumentValue.NUMBER_42)
    public void byte42(byte x) {
        executed[14]++;
        if (x != 42) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(ArgumentValue.NUMBER_42)
    public void short42(short x) {
        executed[15]++;
        if (x != 42) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(ArgumentValue.NUMBER_42)
    public void int42(int x) {
        executed[16]++;
        if (x != 42) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(ArgumentValue.NUMBER_42)
    public void long42(long x) {
        executed[17]++;
        if (x != 42) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(ArgumentValue.NUMBER_42)
    public void float42(float x) {
        executed[18]++;
        if (x != 42.0) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(ArgumentValue.NUMBER_42)
    public void double42(double x) {
        executed[19]++;
        if (x != 42.0) {
            throw new RuntimeException("Must be 42");
        }
    }

    @Test
    @Arguments(ArgumentValue.FALSE)
    public void booleanFalse(boolean x) {
        executed[20]++;
        if (x) {
            throw new RuntimeException("Must be false");
        }
    }

    @Test
    @Arguments(ArgumentValue.TRUE)
    public void booleanTrue(boolean x) {
        executed[21]++;
        if (!x) {
            throw new RuntimeException("Must be true");
        }
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_ONCE)
    public void randomByte(byte x) {
        executed[22]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_ONCE)
    public void randomShort(short x) {
        executed[23]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_ONCE)
    public void randomInt(int x) {
        executed[24]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_ONCE)
    public void randomLong(long x) {
        executed[25]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_ONCE)
    public void randomFloat(float x) {
        executed[26]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_ONCE)
    public void randomDouble(double x) {
        executed[27]++;
    }

    // Not executed
    public void randomNotExecutedTest(double x) {
        wasExecuted = true;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_ONCE)
    public void randomBoolean(boolean x) {
        executed[28]++;
    }

    @Test
    @Arguments(ArgumentValue.BOOLEAN_TOGGLE_FIRST_FALSE)
    public void booleanToggleFirstFalse(boolean x) {
        if (executed[29] == 0) {
            // First invocation
            if (x) {
                throw new RuntimeException("BOOLEAN_TOGGLE_FIRST_FALSE must be false on first invocation");
            }
        } else if (x == lastToggleBoolean) {
            throw new RuntimeException("BOOLEAN_TOGGLE_FIRST_FALSE did not toggle");
        }
        lastToggleBoolean = x;
        executed[29]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_EACH)
    public void randomEachByte(byte x) {
        checkNonFloatingRandomNumber(x, executed[30]);
        executed[30]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_EACH)
    public void randomEachShort(short x) {
        checkNonFloatingRandomNumber(x, executed[31]);
        executed[31]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_EACH)
    public void randomEachInt(int x) {
        checkNonFloatingRandomNumber(x, executed[32]);
        executed[32]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_EACH)
    public void randomEachLong(long x) {
        checkNonFloatingRandomNumber(x, executed[33]);
        executed[33]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_EACH)
    public void randomEachChar(char x) {
        checkNonFloatingRandomNumber(x, executed[34]);
        executed[34]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_EACH)
    public void randomEachFloat(float x) {
        checkFloatingRandomNumber(x, executed[35]);
        executed[35]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_EACH)
    public void randomEachDouble(double x) {
        checkFloatingRandomNumber(x, executed[36]);
        executed[36]++;
    }

    @Test
    @Arguments(ArgumentValue.RANDOM_EACH)
    public void randomEachBoolean(boolean x) {
        checkRandomBoolean(x, executed[37]);
        executed[37]++;
    }

    private void checkNonFloatingRandomNumber(long x, int invocationCount) {
        int mod10 = invocationCount % 10;
        if (invocationCount > 0 && mod10 == 0) {
            // Not first invocation
            // Check the last 10 numbers and ensure that there are at least 2 different ones.
            // All numbers are equal? Very unlikely nd we should really consider to play the lottery...
            long first = nonFloatingRandomNumbers[0];
            if (Arrays.stream(nonFloatingRandomNumbers).allMatch(n -> n == first)) {
                throw new RuntimeException("RANDOM_EACH does not generate random integer numbers");
            }
            clearNonFloatingRandomNumbers();
        }
        nonFloatingRandomNumbers[mod10] = x;
    }

    private void checkFloatingRandomNumber(double x, int invocationCount) {
        int mod10 = invocationCount % 10;
        if (invocationCount > 0 && mod10 == 0) {
            // Not first invocation
            // Check the last 10 numbers and ensure that there are at least 2 different ones.
            // All numbers are equal? Very unlikely nd we should really consider to play the lottery...
            double first = floatingRandomNumbers[0];
            if (Arrays.stream(floatingRandomNumbers).allMatch(n -> n == first)) {
                throw new RuntimeException("RANDOM_EACH does not generate random floating point numbers");
            }
            clearFloatingRandomNumbers();
        }
        floatingRandomNumbers[mod10] = x;
    }

    private void checkRandomBoolean(boolean x, int invocationCount) {
        int mod64 = invocationCount % 64;
        if (invocationCount > 0 && mod64 == 0) {
            // Not first invocation
            // Check the last 64 booleans and ensure that there are at least one true and one false.
            // All booleans are equal? Very unlikely (chance of 2^64) and we should really consider
            // to play the lottery...
            if (Arrays.stream(randomBooleans).allMatch(b -> b == randomBooleans[0])) {
                throw new RuntimeException("RANDOM_EACH does not generate random booleans");
            }
            clearRandomBooleans();
        }
        randomBooleans[mod64] = x;
    }


    @Test
    @Arguments(ArgumentValue.NUMBER_MINUS_42)
    public void byteMinus42(byte x) {
        executed[38]++;
        if (x != -42) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments(ArgumentValue.NUMBER_MINUS_42)
    public void shortMinus42(short x) {
        executed[39]++;
        if (x != -42) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments(ArgumentValue.NUMBER_MINUS_42)
    public void intMinus42(int x) {
        executed[40]++;
        if (x != -42) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments(ArgumentValue.NUMBER_MINUS_42)
    public void longMinus42(long x) {
        executed[41]++;
        if (x != -42) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments(ArgumentValue.NUMBER_MINUS_42)
    public void floatMinus42(float x) {
        executed[42]++;
        if (x != -42.0) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments(ArgumentValue.NUMBER_MINUS_42)
    public void doubleMinus42(double x) {
        executed[43]++;
        if (x != -42.0) {
            throw new RuntimeException("Must be -42");
        }
    }

    @Test
    @Arguments({ArgumentValue.DEFAULT, ArgumentValue.DEFAULT})
    public void twoArgsDefault1(byte x, short y) {
        executed[44]++;
        if (x != 0 || y != 0) {
            throw new RuntimeException("Both must be 0");
        }
    }

    @Test
    @Arguments({ArgumentValue.DEFAULT, ArgumentValue.DEFAULT})
    public void twoArgsDefault2(int x, short y) {
        executed[45]++;
        if (x != 0 || y != 0) {
            throw new RuntimeException("Both must be 0");
        }
    }

    @Test
    @Arguments({ArgumentValue.DEFAULT, ArgumentValue.DEFAULT})
    public void twoArgsDefault3(short x, long y) {
        executed[46]++;
        if (x != 0 || y != 0) {
            throw new RuntimeException("Both must be 0");
        }
    }

    @Test
    @Arguments({ArgumentValue.DEFAULT, ArgumentValue.DEFAULT})
    public void twoArgsDefault4(float x, boolean y) {
        executed[47]++;
        if (x != 0.0 || y) {
            throw new RuntimeException("Must be 0 and false");
        }
    }

    @Test
    @Arguments({ArgumentValue.DEFAULT, ArgumentValue.DEFAULT})
    public void twoArgsDefault5(boolean x, char y) {
        executed[48]++;
        if (x || y != '\u0000') {
            throw new RuntimeException("Must be false and \u0000");
        }
    }

    @Test
    @Arguments({ArgumentValue.DEFAULT, ArgumentValue.DEFAULT})
    public void twoArgsDefault6(char x, byte y) {
        executed[49]++;
        if (x != '\u0000' || y != 0) {
            throw new RuntimeException("Must be\u0000 and 0");
        }
    }

    @Test
    @Arguments({ArgumentValue.RANDOM_ONCE, ArgumentValue.RANDOM_ONCE})
    public void twoArgsRandomOnce(char x, byte y) {
        executed[50]++;
    }

    @Test
    @Arguments({ArgumentValue.RANDOM_ONCE, ArgumentValue.RANDOM_ONCE,
                ArgumentValue.RANDOM_ONCE, ArgumentValue.RANDOM_ONCE,
                ArgumentValue.RANDOM_ONCE, ArgumentValue.RANDOM_ONCE,
                ArgumentValue.RANDOM_ONCE, ArgumentValue.RANDOM_ONCE})
    public void checkRandomOnceDifferentArgs(int a, int b, int c, int d, int e, int f, int g, int h) {
        if (Stream.of(a, b, c, d, e, f, g, h).allMatch(i -> i == a)) {
            throw new RuntimeException("RANDOM_ONCE does not produce random values for different arguments");
        }
        executed[51]++;
    }

    @Test
    @Arguments({ArgumentValue.RANDOM_ONCE, ArgumentValue.RANDOM_ONCE,
                ArgumentValue.RANDOM_ONCE, ArgumentValue.RANDOM_ONCE,
                ArgumentValue.RANDOM_ONCE, ArgumentValue.RANDOM_ONCE,
                ArgumentValue.RANDOM_ONCE, ArgumentValue.RANDOM_ONCE})
    public void checkMixedRandoms1(byte a, short b, int c, long d, char e, boolean f, float g, double h) {
        executed[52]++;
    }

    @Test
    @Arguments({ArgumentValue.RANDOM_EACH, ArgumentValue.RANDOM_EACH,
                ArgumentValue.RANDOM_EACH, ArgumentValue.RANDOM_EACH,
                ArgumentValue.RANDOM_EACH, ArgumentValue.RANDOM_EACH,
                ArgumentValue.RANDOM_EACH, ArgumentValue.RANDOM_EACH})
    public void checkMixedRandoms2(byte a, short b, int c, long d, char e, boolean f, float g, double h) {
        executed[53]++;
    }

    @Test
    @Arguments({ArgumentValue.RANDOM_ONCE, ArgumentValue.RANDOM_ONCE,
                ArgumentValue.RANDOM_EACH, ArgumentValue.RANDOM_EACH,
                ArgumentValue.RANDOM_ONCE, ArgumentValue.RANDOM_EACH,
                ArgumentValue.RANDOM_EACH, ArgumentValue.RANDOM_ONCE})
    public void checkMixedRandoms3(byte a, short b, int c, long d, char e, boolean f, float g, double h) {
        executed[54]++;
    }

    @Test
    @Arguments({ArgumentValue.NUMBER_42, ArgumentValue.NUMBER_42,
                ArgumentValue.NUMBER_42, ArgumentValue.NUMBER_42,
                ArgumentValue.NUMBER_42, ArgumentValue.NUMBER_42})
    public void check42Mix1(byte a, short b, int c, long d, float e, double f) {
        if (a != 42 || b != 42 || c != 42 || d != 42 || e != 42.0 || f != 42.0) {
            throw new RuntimeException("Must all be 42");
        }
        executed[55]++;
    }

    @Test
    @Arguments({ArgumentValue.NUMBER_MINUS_42, ArgumentValue.NUMBER_MINUS_42,
                ArgumentValue.NUMBER_MINUS_42, ArgumentValue.NUMBER_MINUS_42,
                ArgumentValue.NUMBER_MINUS_42, ArgumentValue.NUMBER_MINUS_42})
    public void check42Mix2(byte a, short b, int c, long d, float e, double f) {
        if (a != -42 || b != -42 || c != -42 || d != -42 || e != -42.0 || f != -42.0) {
            throw new RuntimeException("Must all be -42");
        }
        executed[56]++;
    }

    @Test
    @Arguments({ArgumentValue.NUMBER_MINUS_42, ArgumentValue.NUMBER_42,
                ArgumentValue.NUMBER_MINUS_42, ArgumentValue.NUMBER_MINUS_42,
                ArgumentValue.NUMBER_42, ArgumentValue.NUMBER_MINUS_42})
    public void check42Mix3(byte a, short b, int c, long d, float e, double f) {
        if (a != -42 || b != 42 || c != -42 || d != -42 || e != 42.0 || f != -42.0) {
            throw new RuntimeException("Do not match the right 42 version");
        }
        executed[57]++;
    }


    @Test
    @Arguments(ArgumentValue.BOOLEAN_TOGGLE_FIRST_TRUE)
    public void booleanToggleFirstTrue(boolean x) {
        if (executed[58] == 0) {
            // First invocation
            if (!x) {
                throw new RuntimeException("BOOLEAN_TOGGLE_FIRST_FALSE must be false on first invocation");
            }
        } else if (x == lastToggleBoolean) {
            throw new RuntimeException("BOOLEAN_TOGGLE_FIRST_FALSE did not toggle");
        }
        lastToggleBoolean = x;
        executed[58]++;
    }

    @Test
    @Arguments({ArgumentValue.BOOLEAN_TOGGLE_FIRST_FALSE, ArgumentValue.BOOLEAN_TOGGLE_FIRST_TRUE})
    public void checkTwoToggles(boolean b1, boolean b2) {
        if (executed[59] == 0) {
            // First invocation
            if (b1 || !b2) {
                throw new RuntimeException("BOOLEAN_TOGGLES have wrong initial value");
            }
        } else if (b1 == b2) {
            throw new RuntimeException("Boolean values must be different");
        } else if (b1 == lastToggleBoolean) {
            throw new RuntimeException("Booleans did not toggle");
        }
        lastToggleBoolean = b1;
        executed[59]++;
    }

    @Test
    @Arguments({ArgumentValue.BOOLEAN_TOGGLE_FIRST_FALSE, ArgumentValue.FALSE,
                ArgumentValue.TRUE, ArgumentValue.BOOLEAN_TOGGLE_FIRST_TRUE})
    public void booleanMix(boolean b1, boolean b2, boolean b3, boolean b4) {
        if (executed[60] == 0) {
            // First invocation
            if (b1 || b2 || !b3 || !b4) {
                throw new RuntimeException("BOOLEAN_TOGGLES have wrong initial value");
            }
        } else if (b1 == b4) {
            throw new RuntimeException("Boolean values must be different");
        } else if (b1 == lastToggleBoolean) {
            throw new RuntimeException("Booleans did not toggle");
        }
        lastToggleBoolean = b1;
        executed[60]++;
    }

    /*
     * Checked tests.
     */

    @Test
    public int testCheck() {
        executed[63]++;
        return 1;
    }

    // Checked test. Check invoked after invoking "testCheck". Perform some more things after invocation.
    @Check(test = "testCheck")
    public void checkTestCheck() {
        executed[64]++; // Executed on each invocation
    }

    @Test
    public int testCheckReturn() {
        executed[65]++;
        return 2;
    }

    // Checked test with return value. Perform checks on it.
    @Check(test = "testCheckReturn")
    public void checkTestCheckReturn(int returnValue) {
        if (returnValue != 2) {
            throw new RuntimeException("Must be 2");
        }
        executed[66]++; // Executed on each invocation
    }

    @Test
    public int testCheckTestInfo() {
        executed[67]++;
        return 3;
    }

    // Checked test with info object about test.
    @Check(test = "testCheckTestInfo")
    public void checkTestCheckTestInfo(TestInfo testInfo) {
        executed[68]++; // Executed on each invocation
    }


    @Test
    public int testCheckBoth() {
        executed[69]++;
        return 4;
    }

    // Checked test with return value and info object about test.
    @Check(test = "testCheckBoth")
    public void checkTestCheckTestInfo(int returnValue, TestInfo testInfo) {
        if (returnValue != 4) {
            throw new RuntimeException("Must be 4");
        }
        executed[70]++; // Executed on each invocation
    }

    @Test
    public int testCheckOnce() {
        executed[71]++;
        return 1;
    }

    // Check method only invoked once after method is compiled after warm up.
    @Check(test = "testCheckOnce", when = CheckAt.COMPILED)
    public void checkTestCheckOnce() {
        executedOnce[0]++; // Executed once
    }

    @Test
    public int testCheckReturnOnce() {
        executed[72]++;
        return 2;
    }

    @Check(test = "testCheckReturnOnce", when = CheckAt.COMPILED)
    public void checkTestCheckReturnOnce(int returnValue) {
        if (returnValue != 2) {
            throw new RuntimeException("Must be 2");
        }
        executedOnce[1]++; // Executed once
    }

    @Test
    public int testCheckTestInfoOnce() {
        executed[73]++;
        return 3;
    }

    @Check(test = "testCheckTestInfoOnce", when = CheckAt.COMPILED)
    public void checkTestCheckTestInfoOnce(TestInfo testInfo) {
        executedOnce[2]++; // Executed once
    }

    @Test
    public int testCheckBothOnce() {
        executed[74]++;
        return 4;
    }

    @Check(test = "testCheckBothOnce", when = CheckAt.COMPILED)
    public void checkTestCheckBothOnce(int returnValue, TestInfo testInfo) {
        if (returnValue != 4) {
            throw new RuntimeException("Must be 4");
        }
        executedOnce[3]++; // Executed once
    }

    @Test
    public void sameName() {
        executed[76]++;
    }

    // Allowed to overload test method if not test method itself
    public void sameName(boolean a) {
        wasExecuted = true;
    }

    @Check(test = "sameName")
    public void checkSameName() {
        executed[77]++;
    }

    /*
     * Custom run tests.
     */

    @Test
    public void testRun() {
        executed[61]++;
    }

    // Custom run test. This method is invoked each time instead of @Test method. This method responsible for calling
    // the @Test method. @Test method is compiled after warm up. This is similar to the verifiers in the old Valhalla framework.
    @Run(test = "testRun")
    public void runTestRun(TestInfo info) {
        testRun();
    }

    @Test
    public void testRunNoTestInfo(int i) { // Argument allowed when run by @Run
        executed[62]++;
    }

    @Run(test = "testRunNoTestInfo")
    public void runTestRunNoTestInfo() {
        testRunNoTestInfo(3);
    }

    @Test
    public void testNotRun() {
        wasExecuted = true;
    }

    @Run(test = "testNotRun")
    public void runTestNotRun() {
        // Do not execute the test. Pointless but need to test that as well.
    }

    @Test
    public void testRunOnce() {
        executedOnce[4]++;
    }

    // Custom run test that is only invoked once. There is no warm up and no compilation. This method is responsible
    // for triggering compilation.
    @Run(test = "testRunOnce", mode = RunMode.INVOKE_ONCE)
    public void runTestRunOnce(TestInfo info) {
        testRunOnce();
    }

    @Test
    public void testRunOnce2() {
        executed[75]++;
    }

    @Run(test = "testRunOnce2", mode = RunMode.INVOKE_ONCE)
    public void runTestRunOnce2(TestInfo info) {
        for (int i = 0; i < TestFramework.WARMUP_ITERATIONS + 1; i++) {
            testRunOnce2();
        }
    }
}

class DefaultObject {
    int i = 4;
}

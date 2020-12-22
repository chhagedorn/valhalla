package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;
import jdk.test.lib.Asserts;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class TestIRMatching {
    private static final String SHOULD_NOT_REACH_MESSAGE = "should not reach";
    static int[] testExecuted = new int[61];

    public static void main(String[] args) {
        // Run with -DPrintValidIRRules=true to simulate TestVM
        runFailOnTests(AndOr1.class, "test1(int)", "CallStaticJava", "-XX:SuspendRetryCount=50", "-XX:+UsePerfData", "-XX:+UseTLAB");
        runFailOnTests(AndOr1.class, "test1(int)", "CallStaticJava", "-XX:SuspendRetryCount=50", "-XX:+UsePerfData", "-XX:+UseTLAB");
        runFailOnTests(AndOr1.class, "test2()", "CallStaticJava", "-XX:SuspendRetryCount=50", "-XX:-UsePerfData", "-XX:+UseTLAB");

        TestFramework.runWithArguments(AndOr1.class, "-XX:SuspendRetryCount=52", "-XX:+UsePerfData", "-XX:+UseTLAB");

        TestFramework.runWithArguments(Comparisons.class, "-XX:SuspendRetryCount=50");

        findIrIds(TestFramework.getLastVmOutput(), "testMatchAllIf50", 0, 21);
        findIrIds(TestFramework.getLastVmOutput(), "testMatchNoneIf50", -1, -1);
        TestFramework.runWithArguments(Comparisons.class, "-XX:SuspendRetryCount=49");
        findIrIds(TestFramework.getLastVmOutput(), "testMatchAllIf50", 4, 6, 13, 18);
        findIrIds(TestFramework.getLastVmOutput(), "testMatchNoneIf50", 0, 3, 8, 10, 17, 22);
        TestFramework.runWithArguments(Comparisons.class, "-XX:SuspendRetryCount=51");
        findIrIds(TestFramework.getLastVmOutput(), "testMatchAllIf50", 7, 12, 19, 21);
        findIrIds(TestFramework.getLastVmOutput(), "testMatchNoneIf50", 4, 7, 11, 16, 20, 22);

        TestFramework.runWithArguments(MultipleFailOnGood.class, "-XX:SuspendRetryCount=50");

        runFailOnTests(MultipleFailOnBad.class, "fail1()", "Store", "-XX:SuspendRetryCount=50");
        runFailOnTests(MultipleFailOnBad.class, "fail2()", "CallStaticJava", "-XX:SuspendRetryCount=51");
        runFailOnTests(MultipleFailOnBad.class, "fail3()", "Store", "-XX:SuspendRetryCount=52");
        runFailOnTests(MultipleFailOnBad.class, "fail4()", "Store", "-XX:SuspendRetryCount=53");
        runFailOnTests(MultipleFailOnBad.class, "fail5()", "Store", "-XX:SuspendRetryCount=54");
        runFailOnTests(MultipleFailOnBad.class, "fail6()", new String[] {"MyClass", "call,static  wrapper for: _new_instance_Java"}, "-XX:SuspendRetryCount=55");
        runFailOnTests(MultipleFailOnBad.class, "fail7()", new String[] {"MyClass", "call,static  wrapper for: _new_instance_Java"}, "-XX:SuspendRetryCount=56");
        runFailOnTests(MultipleFailOnBad.class, "fail8()", new String[] {"MyClassSub", "call,static  wrapper for: _new_instance_Java"}, "-XX:SuspendRetryCount=57");
        runFailOnTests(MultipleFailOnBad.class, "fail9()", new String[] {"Store", "CallStaticJava"}, "-XX:SuspendRetryCount=58");
        TestFramework.runWithArguments(GoodCount.class, "-XX:SuspendRetryCount=50");
    }

    private static void runFailOnTests(Class<?> c, String methodName, String node, String... args) {
        try {
            TestFramework.runWithArguments(c, args);
            shouldNotReach();
        } catch (RuntimeException e) {
            shouldContain(e, c.getSimpleName() + "." + methodName, "forbidden", node);
        }
    }


    private static void runFailOnTests(Class<?> c, String methodName, String[] matches, String... args) {
        try {
            TestFramework.runWithArguments(c, args);
            shouldNotReach();
        } catch (RuntimeException e) {
            String[] allMatches = Arrays.copyOf(matches, matches.length + 2);
            allMatches[matches.length] = c.getSimpleName() + "." + methodName;
            allMatches[matches.length + 1] = "forbidden";
            shouldContain(e, allMatches);
        }
    }

    private static void shouldNotReach() {
        Asserts.fail(SHOULD_NOT_REACH_MESSAGE);
    }

    private static void shouldContain(RuntimeException e, String... strings) {
        String message = e.getMessage();
        if (e.getMessage().equals(SHOULD_NOT_REACH_MESSAGE)) {
            throw e;
        }
        Arrays.stream(strings).forEach(s -> Asserts.assertTrue(message.contains(s), "Did not find \"" + s + "\" in:\n" + message));
    }


    public static void findIrIds(String output, String method, int... numbers) {
        StringBuilder builder = new StringBuilder();
        builder.append(method);
        for (int i = 0; i < numbers.length; i+=2) {
            int start = numbers[i];
            int endIncluded = numbers[i + 1];
            for (int j = start; j <= endIncluded; j++) {
                builder.append(",");
                builder.append(j);
            }
        }
        Asserts.assertTrue(output.contains(builder.toString()), "Could not find encoding: \"" + builder.toString() + "\n");
    }
}

class AndOr1 {
    @Test
    @Arguments(ArgumentValue.DEFAULT)
    @IR(applyIfAnd={"UsePerfData", "true", "SuspendRetryCount", "50", "UseTLAB", "true"}, failOn={IRNode.CALL})
    public void test1(int i) {
        dontInline();
    }

    @Test
    @IR(applyIfOr={"UsePerfData", "false", "SuspendRetryCount", "51", "UseTLAB", "false"}, failOn={IRNode.CALL})
    public void test2() {
        dontInline();
    }

    @DontInline
    private void dontInline() {
    }
}

class MultipleFailOnGood {
    private int iFld;
    private MyClassSub myClassSub = new MyClassSub();

    @Test
    @IR(applyIf={"SuspendRetryCount", "50"}, failOn={IRNode.STORE, IRNode.CALL})
    @IR(failOn={IRNode.STORE, IRNode.CALL})
    @IR(applyIfOr={"SuspendRetryCount", "99", "SuspendRetryCount", "100"}, failOn={IRNode.RETURN, IRNode.CALL}) // Not applied
    public void good1() {
        forceInline();
    }

    @Test
    @IR(failOn={IRNode.STORE, IRNode.CALL})
    @IR(applyIfNot={"SuspendRetryCount", "20"}, failOn={IRNode.ALLOC})
    @IR(applyIfNot={"SuspendRetryCount", "< 100"}, failOn={IRNode.ALLOC_OF, "Test"})
    public void good2() {
        forceInline();
    }

    @Test
    @IR(failOn={IRNode.STORE_OF_CLASS, "Test", IRNode.CALL})
    @IR(applyIfNot={"SuspendRetryCount", "20"}, failOn={IRNode.ALLOC})
    @IR(applyIfNot={"SuspendRetryCount", "< 100"}, failOn={IRNode.ALLOC_OF, "Test"})
    public void good3() {
        forceInline();
    }

    @Test
    @IR(failOn={IRNode.CALL, IRNode.STORE_OF_CLASS, "UnknownClass"})
    public void good4() {
        iFld = 42;
    }

    @Test
    @IR(failOn={IRNode.STORE_OF_FIELD, "xFld", IRNode.CALL})
    public void good5() {
        iFld = 42;
    }

    @Test
    @IR(failOn={IRNode.STORE_OF_CLASS, "MyClass"}) // Needs exact match to fail
    public void good6() {
        myClassSub.iFld = 42;
    }

    @Test
    @IR(failOn={IRNode.STORE_OF_CLASS, "MyClassSub"}) // Static write is with Class and not MySubClass
    public void good7() {
        MyClassSub.iFldStatic = 42;
    }

    @ForceInline
    private void forceInline() {
    }
}

class MultipleFailOnBad {
    private int iFld;
    private MyClass myClass;
    @Test
    @IR(applyIf={"SuspendRetryCount", "50"}, failOn={IRNode.STORE, IRNode.CALL})
    public void fail1() {
        iFld = 42;
    }

    @Test
    @IR(applyIf={"SuspendRetryCount", "51"}, failOn={IRNode.STORE, IRNode.CALL})
    public void fail2() {
        dontInline();
    }

    @Test
    @IR(applyIf={"SuspendRetryCount", "52"}, failOn={IRNode.CALL, IRNode.STORE_OF_CLASS, "MultipleFailOnBad", IRNode.ALLOC})
    public void fail3() {
        iFld = 42;
    }

    @Test
    @IR(applyIf={"SuspendRetryCount", "53"}, failOn={IRNode.STORE_OF_CLASS, "compiler/valhalla/framework/tests/MultipleFailOnBad", IRNode.CALL, IRNode.ALLOC})
    public void fail4() {
        iFld = 42;
    }

    @Test
    @IR(applyIf={"SuspendRetryCount", "54"}, failOn={IRNode.STORE_OF_FIELD, "iFld", IRNode.CALL, IRNode.ALLOC})
    public void fail5() {
        iFld = 42;
    }

    @Test
    @IR(applyIf={"SuspendRetryCount", "55"}, failOn={IRNode.STORE_OF_CLASS, "MyClass", IRNode.ALLOC})
    public void fail6() {
        myClass = new MyClass();
    }

    @Test
    @IR(applyIf={"SuspendRetryCount", "56"}, failOn={IRNode.STORE_OF_CLASS, "UnknownClass", IRNode.ALLOC_OF, "MyClass"})
    public void fail7() {
        myClass = new MyClass();
    }

    @Test
    @IR(applyIf={"SuspendRetryCount", "57"}, failOn={IRNode.STORE_OF_CLASS, "UnknownClass", IRNode.ALLOC_OF, "compiler/valhalla/framework/tests/MyClassSub"})
    public void fail8() {
        myClass = new MyClassSub();
    }

    @Test
    @IR(applyIf={"SuspendRetryCount", "58"}, failOn={IRNode.STORE, IRNode.CALL})
    public void fail9() {
        iFld = 42;
        dontInline();
    }

    @DontInline
    private void dontInline() {
    }
}

// Called with -XX:SuspendRetryCount=X.
class Comparisons {
    // Applies all IR rules if SuspendRetryCount=50
    @Test
    @IR(applyIf={"SuspendRetryCount", "50"}) // Index 0
    @IR(applyIf={"SuspendRetryCount", "=50"})
    @IR(applyIf={"SuspendRetryCount", "= 50"})
    @IR(applyIf={"SuspendRetryCount", " =  50"})
    @IR(applyIf={"SuspendRetryCount", "<=50"}) // Index 4
    @IR(applyIf={"SuspendRetryCount", "<= 50"})
    @IR(applyIf={"SuspendRetryCount", " <=  50"})
    @IR(applyIf={"SuspendRetryCount", ">=50"}) // Index 7
    @IR(applyIf={"SuspendRetryCount", ">= 50"})
    @IR(applyIf={"SuspendRetryCount", " >=  50"})
    @IR(applyIf={"SuspendRetryCount", ">49"})
    @IR(applyIf={"SuspendRetryCount", "> 49"})
    @IR(applyIf={"SuspendRetryCount", " >  49"})
    @IR(applyIf={"SuspendRetryCount", "<51"}) // Index 13
    @IR(applyIf={"SuspendRetryCount", "< 51"})
    @IR(applyIf={"SuspendRetryCount", " <  51"})
    @IR(applyIf={"SuspendRetryCount", "!=51"})
    @IR(applyIf={"SuspendRetryCount", "!= 51"})
    @IR(applyIf={"SuspendRetryCount", " !=  51"})
    @IR(applyIf={"SuspendRetryCount", "!=49"})
    @IR(applyIf={"SuspendRetryCount", "!= 49"})
    @IR(applyIf={"SuspendRetryCount", " !=  49"}) // Index 21
    public void testMatchAllIf50() {
    }

    // Applies no IR rules if SuspendRetryCount=50
    @Test
    @IR(applyIf={"SuspendRetryCount", "49"}) // Index 0
    @IR(applyIf={"SuspendRetryCount", "=49"})
    @IR(applyIf={"SuspendRetryCount", "= 49"})
    @IR(applyIf={"SuspendRetryCount", " =  49"})
    @IR(applyIf={"SuspendRetryCount", "51"}) // Index 4
    @IR(applyIf={"SuspendRetryCount", "=51"})
    @IR(applyIf={"SuspendRetryCount", "= 51"})
    @IR(applyIf={"SuspendRetryCount", " =  51"})
    @IR(applyIf={"SuspendRetryCount", "<=49"}) // Index 8
    @IR(applyIf={"SuspendRetryCount", "<= 49"})
    @IR(applyIf={"SuspendRetryCount", " <=  49"})
    @IR(applyIf={"SuspendRetryCount", ">=51"}) // Index 11
    @IR(applyIf={"SuspendRetryCount", ">= 51"})
    @IR(applyIf={"SuspendRetryCount", " >=  51"})
    @IR(applyIf={"SuspendRetryCount", ">50"})
    @IR(applyIf={"SuspendRetryCount", "> 50"})
    @IR(applyIf={"SuspendRetryCount", " >  50"})
    @IR(applyIf={"SuspendRetryCount", "<50"}) // Index 17
    @IR(applyIf={"SuspendRetryCount", "< 50"})
    @IR(applyIf={"SuspendRetryCount", " <  50"})
    @IR(applyIf={"SuspendRetryCount", "!=50"})
    @IR(applyIf={"SuspendRetryCount", "!= 50"})
    @IR(applyIf={"SuspendRetryCount", " !=  50"}) // Index 22
    public void testMatchNoneIf50() {
    }
}


class GoodCount {
    int iFld;
    int iFld2;
    MyClass myClass = new MyClass();

    @Test
    @IR(applyIf={"SuspendRetryCount", "50"}, counts={IRNode.STORE, "1"})
    public void good1() {
        iFld = 3;
    }

    @Test
    @IR(counts={IRNode.STORE, "2"})
    public void good2() {
        iFld = 3;
        iFld2 = 4;
    }

    @Test
    @IR(counts={IRNode.STORE, "2", IRNode.STORE_OF_CLASS, "GoodCount", "2"})
    public void good3() {
        iFld = 3;
        iFld2 = 4;
    }

    @Test
    @IR(counts={IRNode.STORE_OF_FIELD, "iFld", "1", IRNode.STORE, "2", IRNode.STORE_OF_CLASS, "GoodCount", "2"})
    public void good4() {
        iFld = 3;
        iFld2 = 4;
    }
}

class MyClass {
    int iFld;
}
class MyClassSub extends MyClass {
    int iFld;
    static int iFldStatic;
}
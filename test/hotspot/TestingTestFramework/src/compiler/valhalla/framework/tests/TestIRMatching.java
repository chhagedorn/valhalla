/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;
import jdk.test.lib.Asserts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestIRMatching {

    public static void main(String[] args) {
//        runFailOnTests(Constraint.failOnMatches(AndOr1.class, "test1(int)", 1, true,"CallStaticJava"), "-XX:SuspendRetryCount=50", "-XX:+UsePerfData", "-XX:+UseTLAB");
//        runFailOnTests(Constraint.failOnMatches(AndOr1.class, "test2()", 1, true,"CallStaticJava"), "-XX:SuspendRetryCount=50", "-XX:-UsePerfData", "-XX:+UseTLAB");
//
//        runWithArguments(AndOr1.class, "-XX:SuspendRetryCount=52", "-XX:+UsePerfData", "-XX:+UseTLAB");
//
//        runWithArguments(FlagComparisons.class, "-XX:SuspendRetryCount=50");
//        findIrIds(TestFramework.getLastVmOutput(), "testMatchAllIf50", 0, 21);
//        findIrIds(TestFramework.getLastVmOutput(), "testMatchNoneIf50", -1, -1);
//
//        runWithArguments(FlagComparisons.class, "-XX:SuspendRetryCount=49");
//        findIrIds(TestFramework.getLastVmOutput(), "testMatchAllIf50", 4, 6, 13, 18);
//        findIrIds(TestFramework.getLastVmOutput(), "testMatchNoneIf50", 0, 3, 8, 10, 17, 22);
//
//        runWithArguments(FlagComparisons.class, "-XX:SuspendRetryCount=51");
//        findIrIds(TestFramework.getLastVmOutput(), "testMatchAllIf50", 7, 12, 19, 21);
//        findIrIds(TestFramework.getLastVmOutput(), "testMatchNoneIf50", 4, 7, 11, 16, 20, 22);
//
//        runWithArguments(MultipleFailOnGood.class, "-XX:SuspendRetryCount=50");
//
//        runFailOnTests(Constraint.failOnMatches(MultipleFailOnBad.class, "fail1()", 1,true, "Store"),
//                       Constraint.failOnMatches(MultipleFailOnBad.class, "fail2()", 1,true, "CallStaticJava"),
//                       Constraint.failOnMatches(MultipleFailOnBad.class, "fail3()", 1,true, "Store"),
//                       Constraint.failOnMatches(MultipleFailOnBad.class, "fail4()", 1,true, "Store"),
//                       Constraint.failOnMatches(MultipleFailOnBad.class, "fail5()", 1,true, "Store", "iFld"),
//                       Constraint.failOnAlloc(MultipleFailOnBad.class, "fail6()", 1,true, "MyClass"),
//                       Constraint.failOnAlloc(MultipleFailOnBad.class, "fail7()", 1,true, "MyClass"),
//                       Constraint.failOnAlloc(MultipleFailOnBad.class, "fail8()", 1,true, "MyClass"),
//                       Constraint.failOnMatches(MultipleFailOnBad.class, "fail9()", 1,true, "Store", "CallStaticJava"),
//                       Constraint.failOnMatches(MultipleFailOnBad.class, "fail10()", 1,true, "Store", "iFld"));
//
//        runWithArguments(CountComparisons.class, "-XX:SuspendRetryCount=50");
//        runWithArguments(GoodCount.class, "-XX:SuspendRetryCount=50");
//        runFailOnTests(Constraint.countsMatches(BadCount.class, "bad1()", 1,true),
//                       Constraint.countsMatches(BadCount.class, "bad1()", 2,false),
//                       Constraint.countsMatches(BadCount.class, "bad2()", 1,false),
//                       Constraint.countsMatches(BadCount.class, "bad2()", 2,true),
//                       Constraint.countsMatches(BadCount.class, "bad3()", 1,true),
//                       Constraint.countsMatches(BadCount.class, "bad3()", 2,true));
//
//        runFailOnTests(Constraint.failOnArrayAlloc(AllocArray.class, "allocArray()", 1, true, "MyClass"),
//                       Constraint.failOnArrayAlloc(AllocArray.class, "allocArray()", 2, true, "MyClass"),
//                       Constraint.failOnArrayAlloc(AllocArray.class, "allocArray()", 3, false, "MyClass"),
//                       Constraint.failOnArrayAlloc(AllocArray.class, "allocArray()", 4, false, "MyClass"),
//                       Constraint.failOnArrayAlloc(AllocArray.class, "allocArray()", 5, true, "MyClass"));
//
//        runFailOnTests(new String[] {"-XX:-UseCompressedClassPointers"},
//                       Constraint.failOnMatches(Loads.class, "load()", 1, true, "Load"),
//                       Constraint.failOnMatches(Loads.class, "load()", 2, true, "Loads"),
//                       Constraint.failOnMatches(Loads.class, "load()", 3, true, "Loads"),
//                       Constraint.failOnMatches(Loads.class, "load()", 4, true, "Load", "iFld"),
//                       Constraint.failOnMatches(Loads.class, "load()", 5, false, "Load"),
//                       Constraint.failOnMatches(Loads.class, "load()", 6, false, "LoadKlass"),
//                       Constraint.failOnMatches(Loads.class, "loadKlass()", 1, false, "LoadKlass"));
//
//        runFailOnTests(Constraint.failOnMatches(Loops.class, "loop()", 1, true, "Loop"),
//                       Constraint.failOnMatches(Loops.class, "loop()", 2, false, "CountedLoop"),
//                       Constraint.failOnMatches(Loops.class, "loop()", 3, false, "CountedLoop", "main"),
//                       Constraint.failOnMatches(Loops.class, "countedLoop()", 1, false, "Loop"),
//                       Constraint.failOnMatches(Loops.class, "countedLoop()", 2, true, "CountedLoop"),
//                       Constraint.failOnMatches(Loops.class, "countedLoop()", 3, false, "CountedLoop", "main"),
//                       Constraint.failOnMatches(Loops.class, "loopAndCountedLoop()", 1, true, "Loop"),
//                       Constraint.failOnMatches(Loops.class, "loopAndCountedLoop()", 2, true, "CountedLoop"),
//                       Constraint.failOnMatches(Loops.class, "loopAndCountedLoop()", 3, false, "CountedLoop", "main"),
//                       Constraint.failOnMatches(Loops.class, "countedLoopMain()", 1, false, "Loop"),
//                       Constraint.failOnMatches(Loops.class, "countedLoopMain()", 2, true, "CountedLoop"),
//                       Constraint.failOnMatches(Loops.class, "countedLoopMain()", 3, true, "CountedLoop", "main"));
    runFailOnTests(Constraint.failOnMatches(Traps.class, "traps()", 1, true, "CallStaticJava", "uncommon_trap"),
                   Constraint.failOnMatches(Traps.class, "traps()", 2, true, "CallStaticJava", "uncommon_trap", "predicate"),
                   Constraint.failOnMatches(Traps.class, "traps()", 3, false, "Store", "iFld"),
                   Constraint.failOnMatches(Traps.class, "noTraps()", 1, false, "CallStaticJava", "uncommon_trap"),
                   Constraint.failOnMatches(Traps.class, "noTraps()", 2, true, "Store", "iFld"),
                   Constraint.failOnMatches(Traps.class, "nullCheck()", 1, true, "CallStaticJava", "uncommon_trap"),
                   Constraint.failOnMatches(Traps.class, "nullCheck()", 2, true, "CallStaticJava", "uncommon_trap", "null_check")
    );

    }

    private static void runWithArguments(Class<?> clazz, String... args) {
        TestFramework.runWithScenarios(clazz, new Scenario(0, args));
    }

    private static void runFailOnTests(String[] args , Constraint... constraints) {
        try {
            Scenario s = new Scenario(0, args);
            TestFramework.runWithScenarios(constraints[0].getKlass(), s); // All constraints have the same class.
            shouldNotReach();
        } catch (ShouldNotReachException e) {
            throw e;
        } catch (RuntimeException e) {
            checkConstraints(e, constraints);
        }
    }

    private static void runFailOnTests(Constraint... constraints) {
        try {
            TestFramework.run(constraints[0].getKlass()); // All constraints have the same class.
            shouldNotReach();
        } catch (ShouldNotReachException e) {
            throw e;
        } catch (RuntimeException e) {
            checkConstraints(e, constraints);
        }
    }

    private static void checkConstraints(RuntimeException e, Constraint[] constraints) {
        String message = e.getMessage();
        try {
            long expectedFailures = Arrays.stream(constraints).filter(Constraint::shouldMatch).count();
            Pattern pattern = Pattern.compile("Failures \\((\\d+)\\)");
            Matcher matcher = pattern.matcher(message);
            if (expectedFailures > 0) {
                Asserts.assertTrue(matcher.find(), "Could not find failures");
                long foundFailuresCount = Long.parseLong(matcher.group(1));
                Asserts.assertEQ(foundFailuresCount, expectedFailures);
            } else {
                Asserts.assertFalse(matcher.find(), "Should not have found failures");
            }

            for (Constraint constraint : constraints) {
                constraint.checkConstraint(e);
            }
        } catch (Exception e1) {
            System.out.println(TestFramework.getLastVmOutput());
            System.out.println(message);
            throw e1;
        }
    }

    // Single constraint
    private static void runFailOnTests(Constraint constraint, String... args) {
        try {
            Scenario scenario = new Scenario(0, args);
            TestFramework.runWithScenarios(constraint.getKlass(), scenario); // All constraints have the same class.
            shouldNotReach();
        } catch (TestRunException e) {
            System.out.println(e.getMessage());
            constraint.checkConstraint(e);
        }
    }

    private static void shouldNotReach() {
        throw new ShouldNotReachException("Framework did not fail but it should have!");
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
    @Arguments(Argument.DEFAULT)
    @IR(applyIfAnd = {"UsePerfData", "true", "SuspendRetryCount", "50", "UseTLAB", "true"}, failOn = {IRNode.CALL})
    public void test1(int i) {
        dontInline();
    }

    @Test
    @IR(applyIfOr = {"UsePerfData", "false", "SuspendRetryCount", "51", "UseTLAB", "false"}, failOn = {IRNode.CALL})
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
    @IR(applyIf = {"SuspendRetryCount", "50"}, failOn = {IRNode.STORE, IRNode.CALL})
    @IR(failOn = {IRNode.STORE, IRNode.CALL})
    @IR(applyIfOr = {"SuspendRetryCount", "99", "SuspendRetryCount", "100"}, failOn = {IRNode.LOOP, IRNode.CALL}) // Not applied
    public void good1() {
        forceInline();
    }

    @Test
    @IR(failOn = {IRNode.STORE, IRNode.CALL})
    @IR(applyIfNot = {"SuspendRetryCount", "20"}, failOn = {IRNode.ALLOC})
    @IR(applyIfNot = {"SuspendRetryCount", "< 100"}, failOn = {IRNode.ALLOC_OF, "Test"})
    public void good2() {
        forceInline();
    }

    @Test
    @IR(failOn = {IRNode.STORE_OF_CLASS, "Test", IRNode.CALL})
    @IR(applyIfNot = {"SuspendRetryCount", "20"}, failOn = {IRNode.ALLOC})
    @IR(applyIfNot = {"SuspendRetryCount", "< 100"}, failOn = {IRNode.ALLOC_OF, "Test"})
    public void good3() {
        forceInline();
    }

    @Test
    @IR(failOn = {IRNode.CALL, IRNode.STORE_OF_CLASS, "UnknownClass"})
    public void good4() {
        iFld = 42;
    }

    @Test
    @IR(failOn = {IRNode.STORE_OF_FIELD, "xFld", IRNode.CALL})
    public void good5() {
        iFld = 42;
    }

    @Test
    @IR(failOn = {IRNode.STORE_OF_CLASS, "MyClass"}) // Needs exact match to fail
    public void good6() {
        myClassSub.iFld = 42;
    }

    @Test
    @IR(failOn = {IRNode.STORE_OF_CLASS, "MyClassSub"}) // Static write is with Class and not MySubClass
    public void good7() {
        MyClassSub.iFldStatic = 42;
    }

    @ForceInline
    private void forceInline() {
    }
}

class MultipleFailOnBad {
    private int iFld;
    private int myInt;
    private MyClass myClass;
    @Test
    @IR(failOn = {IRNode.STORE, IRNode.CALL})
    public void fail1() {
        iFld = 42;
    }

    @Test
    @IR(failOn = {IRNode.STORE, IRNode.CALL})
    public void fail2() {
        dontInline();
    }

    @Test
    @IR(failOn = {IRNode.CALL, IRNode.STORE_OF_CLASS, "MultipleFailOnBad", IRNode.ALLOC})
    public void fail3() {
        iFld = 42;
    }

    @Test
    @IR(failOn = {IRNode.STORE_OF_CLASS, "compiler/valhalla/framework/tests/MultipleFailOnBad", IRNode.CALL, IRNode.ALLOC})
    public void fail4() {
        iFld = 42;
    }

    @Test
    @IR(failOn = {IRNode.STORE_OF_FIELD, "iFld", IRNode.CALL, IRNode.ALLOC})
    public void fail5() {
        iFld = 42;
    }

    @Test
    @IR(failOn = {IRNode.STORE_OF_CLASS, "MyClass", IRNode.ALLOC})
    public void fail6() {
        myClass = new MyClass();
    }

    @Test
    @IR(failOn = {IRNode.STORE_OF_CLASS, "UnknownClass", IRNode.ALLOC_OF, "MyClass"})
    public void fail7() {
        myClass = new MyClass();
    }

    @Test
    @IR(failOn = {IRNode.STORE_OF_CLASS, "UnknownClass", IRNode.ALLOC_OF, "compiler/valhalla/framework/tests/MyClassSub"})
    public void fail8() {
        myClass = new MyClassSub();
    }

    @Test
    @IR(failOn = {IRNode.STORE, IRNode.CALL})
    public void fail9() {
        iFld = 42;
        dontInline();
    }

    @Test
    @IR(failOn = {IRNode.STORE_OF_FIELD, "iFld", IRNode.CALL, IRNode.ALLOC})
    public void fail10() {
        myInt = 34;
        iFld = 42;
    }

    @DontInline
    private void dontInline() {
    }
}

// Called with -XX:SuspendRetryCount=X.
class FlagComparisons {
    // Applies all IR rules if SuspendRetryCount=50
    @Test
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "50"}) // Index 0
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "=50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "= 50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " =   50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "<=50"}) // Index 4
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "<= 50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " <=  50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", ">=50"}) // Index 7
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", ">= 50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " >=  50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", ">49"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "> 49"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " >  49"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "<51"}) // Index 13
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "< 51"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " <  51"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "!=51"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "!= 51"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " !=  51"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "!=49"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "!= 49"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " !=  49"}) // Index 21
    public void testMatchAllIf50() {
    }

    // Applies no IR rules if SuspendRetryCount=50
    @Test
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "49"}) // Index 0
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "=49"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "= 49"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " =  49"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "51"}) // Index 4
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "=51"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "= 51"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " =  51"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "<=49"}) // Index 8
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "<= 49"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " <=  49"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", ">=51"}) // Index 11
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", ">= 51"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " >=  51"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", ">50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "> 50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " >  50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "<50"}) // Index 17
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "< 50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " <  50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "!=50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", "!= 50"})
    @IR(failOn = IRNode.CALL, applyIf = {"SuspendRetryCount", " !=  50"}) // Index 22
    public void testMatchNoneIf50() {
    }
}

class CountComparisons {
    int iFld;

    @Test
    @IR(counts = {IRNode.STORE, "= 1",
                  IRNode.STORE, "=1",
                  IRNode.STORE, " = 1",
                  IRNode.STORE, "  =  1",
                  IRNode.STORE, ">= 1",
                  IRNode.STORE, ">=1",
                  IRNode.STORE, " >= 1",
                  IRNode.STORE, "  >=  1",
                  IRNode.STORE, "<= 1",
                  IRNode.STORE, "<=1",
                  IRNode.STORE, " <= 1",
                  IRNode.STORE, "  <=  1",
                  IRNode.STORE, "!= 0",
                  IRNode.STORE, "!=0",
                  IRNode.STORE, " != 0",
                  IRNode.STORE, "  !=  0",
                  IRNode.STORE, "> 0",
                  IRNode.STORE, ">0",
                  IRNode.STORE, " > 0",
                  IRNode.STORE, "  >  0",
                  IRNode.STORE, "< 2",
                  IRNode.STORE, "<2",
                  IRNode.STORE, " < 2",
                  IRNode.STORE, "  <  2",
    })
    public void countComparison() {
        iFld = 3;
    }
}

class GoodCount {
    int iFld;
    int iFld2;
    long result;
    MyClass myClass = new MyClass();
    MyClass myClassSubPoly = new MyClassSub();
    MyClassSub myClassSub = new MyClassSub();

    @Test
    @IR(counts = {IRNode.STORE, "1"})
    public void good1() {
        iFld = 3;
    }

    @Test
    @IR(counts = {IRNode.STORE, "2"})
    public void good2() {
        iFld = 3;
        iFld2 = 4;
    }

    @Test
    @IR(counts = {IRNode.STORE, "2", IRNode.STORE_OF_CLASS, "GoodCount", "2"})
    public void good3() {
        iFld = 3;
        iFld2 = 4;
    }

    @Test
    @IR(counts = {IRNode.STORE_OF_FIELD, "iFld", "1", IRNode.STORE, "2", IRNode.STORE_OF_CLASS, "GoodCount", "2"})
    public void good4() {
        iFld = 3;
        iFld2 = 4;
    }

    @Test
    @IR(counts = {IRNode.STORE_OF_FIELD, "iFld", "2", IRNode.STORE, "2", IRNode.STORE_OF_CLASS, "GoodCount", "1",
                  IRNode.STORE_OF_CLASS, "compiler/valhalla/framework/tests/MyClass", "1",
                  IRNode.STORE_OF_CLASS, "framework/tests/GoodCount", "1"})
    public void good5() {
        iFld = 3;
        myClass.iFld = 4;
    }

    @Test
    @IR(counts = {IRNode.STORE_OF_FIELD, "myClass", "1", IRNode.STORE_OF_CLASS, "GoodCount", "1",
                  IRNode.STORE_OF_CLASS, "/GoodCount", "1", IRNode.STORE_OF_CLASS, "MyClass", "0"},
        failOn = {IRNode.STORE_OF_CLASS, "MyClass"})
    public void good6() {
        myClass = new MyClass();
    }

    @Test
    @IR(counts = {IRNode.STORE_OF_FIELD, "iFld", "3", IRNode.STORE_OF_CLASS, "GoodCount", "0",
                  IRNode.STORE_OF_CLASS, "MyClass", "2", IRNode.STORE_OF_CLASS, "MyClassSub", "1",
                  IRNode.STORE, "3"},
        failOn = {IRNode.STORE_OF_CLASS, "GoodCount"})
    public void good7() {
        myClass.iFld = 1;
        myClassSubPoly.iFld = 2;
        myClassSub.iFld = 3;
    }

    @Test
    @IR(counts = {IRNode.LOAD, "1", IRNode.STORE, "1"})
    public void good8() {
        result = iFld;
    }


    @Test
    @IR(counts = {IRNode.LOAD, "4", IRNode.STORE, "1", IRNode.LOAD_OF_FIELD, "iFld", "2", IRNode.LOAD_OF_FIELD, "iFld2", "0",
                  IRNode.LOAD_OF_FIELD, "lFldStatic", "1", IRNode.LOAD_OF_CLASS, "GoodCount", "2", IRNode.LOAD_OF_CLASS, "MyClass", "1",
                  IRNode.STORE_OF_CLASS, "GoodCount", "1", IRNode.STORE_OF_FIELD, "result", "1",
                  IRNode.LOAD_OF_FIELD, "myClass", "1"})
    public void good9() {
        result = iFld + MyClass.lFldStatic + myClass.iFld; // 1 + 1 + 2 loads (myClass is LoadN of GoodCount and myClass.iFld a LoadI of MyClass)
    }
}

class BadCount {
    int iFld;
    int result;
    @Test
    @IR(counts = {IRNode.LOAD, "!= 1"})
    @IR(counts = {IRNode.STORE, "> 0"})
    public void bad1() {
        result = iFld;
    }

    @Test
    @IR(counts = {IRNode.LOAD, "1"})
    @IR(counts = {IRNode.STORE, "< 1"})
    public void bad2() {
        result = iFld;
    }


    @Test
    @IR(counts = {IRNode.LOAD, "0"})
    @IR(counts = {IRNode.STORE, " <= 0"})
    public void bad3() {
        result = iFld;
    }
}

class Loads {
    int iFld = 34;
    int result = 0;
    Object[] myClassArr = new MyClass[3];
    Object myClass = new MyClass();

    @Test
    @IR(failOn = {IRNode.LOAD})
    @IR(failOn = {IRNode.LOAD_OF_CLASS, "compiler/valhalla/framework/tests/Loads"})
    @IR(failOn = {IRNode.LOAD_OF_CLASS, "Loads"})
    @IR(failOn = {IRNode.LOAD_OF_FIELD, "iFld"})
    @IR(failOn = {IRNode.LOAD_OF_FIELD, "iFld2", IRNode.LOAD_OF_CLASS, "Load"}) // Does not fail
    @IR(failOn = {IRNode.LOAD_KLASS}) // Does not fail
    public void load() {
        result = iFld;
    }

    @Test
    @IR(failOn = {IRNode.LOAD_KLASS})
    public void loadKlass() {
        if (myClass instanceof MyClass) {
            result = 3;
        }
    }
}

class AllocArray {
    MyClass[] myClassArray;

    @Test
    @IR(failOn = {IRNode.ALLOC_ARRAY})
    @IR(failOn = {IRNode.ALLOC_ARRAY_OF, "MyClass"})
    @IR(failOn = {IRNode.ALLOC_ARRAY_OF, "MyClasss"}) // Does not fail
    @IR(failOn = {IRNode.ALLOC_ARRAY_OF, "compiler/valhalla/framework/tests/MySubClass"}) // Does not fail
    @IR(failOn = {IRNode.ALLOC_ARRAY_OF, "compiler/valhalla/framework/tests/MyClass"})
    public void allocArray() {
        myClassArray = new MyClass[2];
    }
}

class Loops {
    int limit = 1024;
    int[] iArr = new int[100];

    @DontInline
    public void dontInline() {}

    @Test
    @IR(failOn = IRNode.LOOP) // fails
    @IR(failOn = IRNode.COUNTEDLOOP)
    @IR(failOn = IRNode.COUNTEDLOOP_MAIN)
    public void loop() {
        for (int i = 0; i < limit; i++) {
            dontInline();
        }
    }

    @Test
    @IR(failOn = IRNode.LOOP)
    @IR(failOn = IRNode.COUNTEDLOOP) // fails
    @IR(failOn = IRNode.COUNTEDLOOP_MAIN)
    public void countedLoop() {
        for (int i = 0; i < 2000; i++) {
            dontInline();
        }
    }

    @Test
    @IR(failOn = IRNode.LOOP) // fails
    @IR(failOn = IRNode.COUNTEDLOOP) // fails
    @IR(failOn = IRNode.COUNTEDLOOP_MAIN)
    public void loopAndCountedLoop() {
        for (int i = 0; i < 2000; i++) {
            for (int j = 0; j < limit; j++) {
                dontInline();
            }
        }
    }

    @Test
    @IR(failOn = IRNode.LOOP) // fails
    @IR(failOn = IRNode.COUNTEDLOOP)
    @IR(failOn = IRNode.COUNTEDLOOP_MAIN) // fails
    public void countedLoopMain() {
        // Cannot unroll completely -> create pre/main/post
        for (int i = 0; i < 100; i++) {
            iArr[i] = i;
        }
    }

    @Test
    @IR(failOn = IRNode.LOOP) // fails
    @IR(failOn = IRNode.COUNTEDLOOP)
    @IR(failOn = IRNode.COUNTEDLOOP_MAIN)
    public void countedLoopUnrolled() {
        // Completely unrolled -> no pre/main/post
        for (int i = 0; i < 8; i++) {
            iArr[i] = i;
        }
    }
}

class Traps {
    int number42 = 42;
    int iFld = 42;
    MyClass myClass = new MyClass();

    @Test
    @IR(failOn = IRNode.TRAP) // fails
    @IR(failOn = IRNode.PREDICATE_TRAP) // fails
    @IR(failOn = {IRNode.STORE_OF_FIELD, "iFld"})
    public void traps() {
        for (int i = 0; i < 100; i++) {
            if (number42 != 42) {
                // Never reached
                iFld = i;
            }
        }
    }

    @Test
    @IR(failOn = IRNode.TRAP)
    @IR(failOn = {IRNode.STORE_OF_FIELD, "iFld"}) // fails
    public void noTraps() {
        for (int i = 0; i < 100; i++) {
            if (i < 42) {
                // Reached, no uncommon trap
                iFld = i;
            }
        }
    }

    @Test
    @IR(failOn = IRNode.TRAP) // fails
    @IR(failOn = IRNode.NULL_CHECK_TRAP) // fails
    public void nullCheck() {
        if (myClass instanceof MyClassSub) {
            iFld = 4;
        }
    }
}

class MyClass {
    int iFld;
    static long lFldStatic;
}
class MyClassSub extends MyClass {
    int iFld;
    static int iFldStatic;
}

enum FailType {
    FAIL_ON, COUNTS
}

class Constraint {
    private final Class<?> klass;
    private final int ruleIdx;
    private final Pattern irPattern;
    private final List<String> matches;
    private final Pattern methodPattern;
    private final String classAndMethod;
    private final FailType failType;
    private final boolean shouldMatch;

    private Constraint(Class<?> klass, String methodName, int ruleIdx, FailType failType, List<String> matches, boolean shouldMatch) {
        this.klass = klass;
        classAndMethod = klass.getSimpleName() + "." + methodName;
        this.ruleIdx = ruleIdx;
        this.failType = failType;
        this.methodPattern = Pattern.compile(Pattern.quote(classAndMethod));
        if (failType == FailType.FAIL_ON) {
            irPattern = Pattern.compile("rule " + ruleIdx + ":.*\\R.*Failing Regex.*\\R.*Failure:.*contains forbidden node\\(s\\):");
        } else {
            irPattern = Pattern.compile("rule " + ruleIdx + ":.*\\R.*Failing Regex.*\\R.*Failure:.*contains wrong number of nodes. Failed constraint:.*");
        }
        this.shouldMatch = shouldMatch;
        this.matches = matches;
    }

    public Class<?> getKlass() {
        return klass;
    }

    public boolean shouldMatch() {
        return shouldMatch;
    }

    public static Constraint failOnAlloc(Class<?> klass, String methodName, int ruleIdx, boolean shouldMatch, String allocKlass) {
        List<String> list = new ArrayList<>();
        list.add(allocKlass);
        list.add("call,static  wrapper for: _new_instance_Java");
        return new Constraint(klass, methodName, ruleIdx, FailType.FAIL_ON, list, shouldMatch);
    }

    public static Constraint failOnArrayAlloc(Class<?> klass, String methodName, int ruleIdx, boolean shouldMatch, String allocKlass) {
        List<String> list = new ArrayList<>();
        list.add(allocKlass);
        list.add("call,static  wrapper for: _new_array_Java");
        return new Constraint(klass, methodName, ruleIdx, FailType.FAIL_ON, list, shouldMatch);
    }

    public static Constraint failOnMatches(Class<?> klass, String methodName, int ruleIdx, boolean shouldMatch, String... matches) {
        return new Constraint(klass, methodName, ruleIdx, FailType.FAIL_ON, new ArrayList<>(Arrays.asList(matches)), shouldMatch);
    }

    public static Constraint countsMatches(Class<?> klass, String methodName, int ruleIdx, boolean shouldMatch) {
        return new Constraint(klass, methodName, ruleIdx, FailType.COUNTS, new ArrayList<>(), shouldMatch);
    }

    public void checkConstraint(RuntimeException e) {
        String message = e.getMessage();
        String[] splitMethods = message.split("Method");
        for (String method : splitMethods) {
            if (methodPattern.matcher(method).find()) {
                String[] splitIrRules = method.split("@IR");
                for (String irRule : splitIrRules) {
                    if (irPattern.matcher(irRule).find()) {
                        boolean allMatch = matches.stream().allMatch(irRule::contains);
                        if (shouldMatch) {
                            Asserts.assertTrue(allMatch, "Constraint for method " + classAndMethod + ", rule " + ruleIdx + " could not be matched:\n" + message);
                        } else {
                            Asserts.assertFalse(allMatch, "Constraint for method " + classAndMethod  + ", rule " + ruleIdx + " should not have been matched:\n" + message);
                        }
                        return;
                    }
                }
                Predicate<String> irPredicate = s -> irPattern.matcher(s).find();
                if (shouldMatch) {
                    Asserts.assertTrue(Arrays.stream(splitIrRules).anyMatch(irPredicate), "Constraint for method " + classAndMethod + ", rule "
                                       + ruleIdx + " could not be matched:\n" + message);
                } else {
                    Asserts.assertTrue(Arrays.stream(splitIrRules).noneMatch(irPredicate), "Constraint for method " + classAndMethod + ", rule "
                                       + ruleIdx + " should not have been matched:\n" + message);
                }
                return;
            }
        }
        if (shouldMatch) {
            Asserts.fail("Constraint for method " + classAndMethod + ", rule " + ruleIdx + " could not be matched:\n" + message);
        }
    }
}

class ShouldNotReachException extends RuntimeException {
    ShouldNotReachException(String s) {
        super(s);
    }
}

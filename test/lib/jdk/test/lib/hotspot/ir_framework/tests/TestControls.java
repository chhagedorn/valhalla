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

package jdk.test.lib.hotspot.ir_framework.tests;

import jdk.test.lib.hotspot.ir_framework.*;
import jdk.test.lib.Asserts;
import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Run with -Xbatch
public class TestControls {
    static int[] executed = new int[15];
    static boolean wasExecuted = false;
    static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    public int iFld;

    public static void main(String[] args) throws Exception {
        Method runTestsOnSameVM = TestFrameworkExecution.class.getDeclaredMethod("runTestsOnSameVM", Class.class);
        runTestsOnSameVM.setAccessible(true);
        runTestsOnSameVM.invoke(null, new Object[]{ null });
        final int defaultIterations = TestFrameworkExecution.WARMUP_ITERATIONS + 1;
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
        Asserts.assertEQ(executed[13], 1);
        Asserts.assertFalse(wasExecuted);
        final long started = System.currentTimeMillis();
        long elapsed = 0;
        Method overloadDouble = TestControls.class.getDeclaredMethod("overload", double.class);
        Method overloadInt = TestControls.class.getDeclaredMethod("overload", int.class);
        while (!(TestFramework.isC2Compiled(overloadInt) && TestFramework.isCompiledAtLevel(overloadDouble, CompLevel.C1_LIMITED_PROFILE)) && elapsed < 5000) {
            elapsed = System.currentTimeMillis() - started;
        }
        TestFramework.assertCompiledAtLevel(TestControls.class.getDeclaredMethod("overload", double.class), CompLevel.C1_LIMITED_PROFILE);
        TestFramework.assertCompiledByC2(TestControls.class.getDeclaredMethod("overload", int.class));

        TestFramework framework = new TestFramework(ClassInitializerTest.class);
        framework.addFlags("-XX:+PrintCompilation").addHelperClasses(ClassInitializerHelper.class).start();
        String output = TestFramework.getLastTestVMOutput();
        Pattern p = Pattern.compile("4.*ClassInitializerTest::<clinit>");
        Matcher m = p.matcher(output);
        Asserts.assertTrue(m.find());
        p = Pattern.compile("2.*ClassInitializerHelper::<clinit>");
        m = p.matcher(output);
        Asserts.assertTrue(m.find());
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
    public void check2(TestInfo info) {
        Asserts.assertTrue(!info.isWarmUp() && executed[1] == 101);
        TestFramework.assertCompiledByC2(info.getTest());
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
    
    @Run(test = "testDontCompile", mode = RunMode.STANDALONE)
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

    @DontCompile(CompLevel.ANY)
    public static void dontCompile2() {
        executed[7]++;
    }

    @Run(test = "testCompileAtLevel1")
    @Warmup(5000)
    public void runTestDontCompile2(RunInfo info) throws NoSuchMethodException {
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
    public void runNoWarmup2(RunInfo info) {
        noWarmup2();
        noWarmup2();
        Asserts.assertTrue(!info.isWarmUp());
        executed[12]++;
    }

    @Test
    public void testCompilation() {
        wasExecuted = true;
    }

    @DontCompile(CompLevel.ANY)
    public void dontCompileAny() {
        for (int i = 0; i < 10; i++) {
            iFld = i;
        }
    }

    @DontCompile(CompLevel.C1)
    public void dontCompileC1() {
        for (int i = 0; i < 10; i++) {
            iFld = 3;
        }
    }

    @DontCompile(CompLevel.C2)
    public void dontCompileC2(int x, boolean b) {
        for (int i = 0; i < 10; i++) {
            iFld = x;
        }
    }

    // Default is C2.
    @ForceCompile
    public void forceCompileDefault() {
        wasExecuted = true;
    }

    // ANY maps to C2.
    @ForceCompile
    public void forceCompileAny() {
        wasExecuted = true;
    }

    @ForceCompile(CompLevel.C1)
    public void forceCompileC1() {
        wasExecuted = true;
    }

    @ForceCompile(CompLevel.C1_LIMITED_PROFILE)
    public void forceCompileC1Limited() {
        wasExecuted = true;
    }

    @ForceCompile(CompLevel.C1_FULL_PROFILE)
    public void forceCompileC1Full() {
        wasExecuted = true;
    }

    @ForceCompile(CompLevel.C2)
    public void forceCompileC2() {
        wasExecuted = true;
    }

    @ForceCompile(CompLevel.C1)
    @DontCompile(CompLevel.C2)
    public void forceC1DontC2() {
        wasExecuted = true;
    }

    @ForceCompile(CompLevel.C2)
    @DontCompile(CompLevel.C1)
    public void forceC2DontC1() {
        wasExecuted = true;
    }

    @Run(test = "testCompilation")
    @Warmup(0)
    public void runTestCompilation(RunInfo info) {
        for (int i = 0; i < 10000; i++) {
            dontCompileAny();
            dontCompileC1();
            dontCompileC2(i, i % 2 == 0);
        }
        TestFramework.assertCompiledByC2(info.getTest());
        TestFramework.assertNotCompiled(info.getTestClassMethod("dontCompileAny"));
        TestFramework.assertCompiledByC2(info.getTestClassMethod("dontCompileC1"));
        TestFramework.assertCompiledByC1(info.getTestClassMethod("dontCompileC2", int.class, boolean.class));

        TestFramework.assertCompiledAtLevel(info.getTestClassMethod("forceCompileDefault"), CompLevel.C2);
        TestFramework.assertCompiledAtLevel(info.getTestClassMethod("forceCompileAny"), CompLevel.C2);
        TestFramework.assertCompiledAtLevel(info.getTestClassMethod("forceCompileC2"), CompLevel.C2);
        TestFramework.assertCompiledAtLevel(info.getTestClassMethod("forceCompileC1"), CompLevel.C1);
        TestFramework.assertCompiledAtLevel(info.getTestClassMethod("forceCompileC1Limited"), CompLevel.C1_LIMITED_PROFILE);
        TestFramework.assertCompiledAtLevel(info.getTestClassMethod("forceCompileC1Full"), CompLevel.C1_FULL_PROFILE);

        TestFramework.assertCompiledAtLevel(info.getTestClassMethod("forceC1DontC2"), CompLevel.C1);
        TestFramework.assertCompiledAtLevel(info.getTestClassMethod("forceC2DontC1"), CompLevel.C2);
        executed[13]++;
    }
}

@ForceCompileClassInitializer
class ClassInitializerTest {

    static int i;
    static Object o;
    static {
        i = 3;
        o = new Object();
    }
    @Test
    public void test() {}
}

@ForceCompileClassInitializer(CompLevel.C1_LIMITED_PROFILE)
class ClassInitializerHelper {
    static int i;
    static {
        i = 3;
    }
}

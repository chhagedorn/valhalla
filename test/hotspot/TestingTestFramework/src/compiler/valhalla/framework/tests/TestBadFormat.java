/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;

public class TestBadFormat {

    private static Method runTestsOnSameVM;
    public static void main(String[] args) throws NoSuchMethodException {
        runTestsOnSameVM = TestFramework.class.getDeclaredMethod("runTestsOnSameVM", Class.class);
        runTestsOnSameVM.setAccessible(true);
//        expectTestFormatException(BadArguments.class);
//        expectTestFormatException(BadOverloadedMethod.class);
        expectTestFormatException(BadCompilerControl.class, -1);
//        expectTestFormatException(BadWarmup.class, 5);
//        expectTestFormatException(BadRunTests.class, -1);
    }

    private static void expectTestFormatException(Class<?> clazz, int count) {
        try {
            runTestsOnSameVM.invoke(null, clazz);
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                System.out.println(cause.getMessage());
            }
            if (!(cause instanceof TestFormatException)) {
                e.printStackTrace();
                Asserts.fail("Unexpected exception: " + cause);
            }
            String msg = cause.getMessage();
            // TODO:
            if (count != -1) {
                Asserts.assertTrue(msg.contains("Violations (" + count + ")"));
            }
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

    @Test
    @Arguments({Argument.MIN, Argument.MAX})
    public void notNumber7(boolean a, boolean b) {}

    @Test
    @Arguments({Argument.DEFAULT})
    public void missingDefaultConstructor(ClassNoDefaultConstructor a) {}
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

    @Test
    public void test6() {}

    @Run(test = "test6")
    @DontCompile
    public void notAtRun() {}

    @Test
    public void test7() {}

    @Run(test = "test7")
    @ForceCompile
    public void notAtRun2() {}

    @Test
    public void test8() {}

    @Run(test = "test8")
    @DontInline
    public void notAtRun3() {}

    @Test
    public void test9() {}

    @Run(test = "test9")
    @ForceInline
    public void notAtRun4() {}

    @Test
    public void test10() {}

    @Run(test = "test10")
    @ForceInline
    @ForceCompile
    @DontInline
    @DontCompile
    public void notAtRun5() {}

    @Test
    public void test11() {}

    @Check(test = "test11")
    @DontCompile
    public void notAtCheck() {}

    @Test
    public void test12() {}

    @Check(test = "test12")
    @ForceCompile
    public void notAtCheck2() {}

    @Test
    public void test13() {}

    @Check(test = "test13")
    @DontInline
    public void notAtCheck3() {}

    @Test
    public void test14() {}

    @Check(test = "test14")
    @ForceInline
    public void notAtCheck4() {}

    @Test
    public void test15() {}

    @Check(test = "test15")
    @ForceInline
    @ForceCompile
    @DontInline
    @DontCompile
    public void notAtCheck5() {}
}

class BadWarmup {

    @Warmup(10000)
    public void warmUpNonTest() {}

    @Test
    @Warmup(1)
    public void someTest() {}

    @Run(test = "someTest")
    @Warmup(1)
    public void twoWarmups() {}

    @Test
    @Warmup(-1)
    public void negativeWarmup() {}

    @Test
    public void someTest2() {}

    @Run(test = "someTest2")
    @Warmup(-1)
    public void negativeWarmup2() {}

    @Test
    public void someTest3() {}

    @Run(test = "someTest2", mode = RunMode.INVOKE_ONCE)
    @Warmup(-1)
    public void noWarmupAtInvokeOnce() {}

}

class BadRunTests {
    @Test
    public void sharedByTwo() {}

    @Run(test = "sharedByTwo")
    public void share1() {}

    @Run(test = "sharedByTwo")
    public void share2() {}

    @Run(test = "doesNotExist")
    public void noTestExists() {}

    @Test
    @Arguments({Argument.DEFAULT})
    public void argTest(int x) {}

    @Run(test = "argTest")
    public void noArgumentAnnotationForRun() {}

    @Test
    public void test1() {}

    @Run(test = "test1")
    public void wrongParameters1(int x) {}

    @Test
    public void test2() {}

    @Run(test = "test2")
    public void wrongParameters(TestInfo info, int x) {}

    @Test
    public void invalidShare() {}

    @Run(test = "invalidShare")
    public void shareSameTestTwice1() {}

    @Run(test = "invalidShare")
    public void shareSameTestTwice2() {}


}

class ClassNoDefaultConstructor {
    private ClassNoDefaultConstructor(int i) {
    }
}

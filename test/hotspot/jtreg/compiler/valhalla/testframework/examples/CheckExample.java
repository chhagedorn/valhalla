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

/*
 * @test
 * @summary Example test to use the new test framework.
 * @library /test/lib
 * @run driver compiler.valhalla.testframework.examples.CheckExample
 */
 
package compiler.valhalla.testframework.examples;

import jdk.test.lib.hotspot.ir_framework.*;

public class CheckExample {

    public static void main(String[] args) {
        TestFramework.run(); // equivalent to TestFramework.run(TestSimpleTest.class)
    }

    /*
     * If there is no warm up specified the Test Framework will do the following:
     * - Invoke @Test method TestFrameworkExecution.WARMUP_ITERATIONS many times.
     * - By default, after each invocation, the @Check method of the @Test method is invoked. This can be disabled by
     *   using CheckAt.COMPILED.
     * - After the warmup, the @Test method.
     * - Invoke @Test method once again and then always the @Check method once.
     */

    /*
     * Configurable things for checked tests:
     * - At @Test method:
     *   - @Warmup: Change warm-up iterations of test (defined by default by TestFrameworkExecution.WARMUP_ITERATIONS)
     *   - @Arguments: If a @Test method specifies arguments, you need to provide arguments by using @Arguments such
     *                 that the framework knows how to call the method. If you need more complex values, use @Run.
     *   - @IR: Arbitrary number of @IR rules.
     * - At @Check method:
     *   - when: When should the @Check method be invoked.
     *   - No @IR annotations.
     */

    @Test
    @Arguments(Argument.DEFAULT) // As with normal tests, you need to tell the framework what the argument is.
    @Warmup(100) // As with normal tests, you can specify the warmup iterations.
    public int test(int x) {
        return 42;
    }

    // Check method for test(). Invoked directly after test() by the Test Framework.
    @Check(test = "test") // Specify the @Test method for which this method is a check.
    public void basicCheck() {
        // Do some checks after an invocation.
    }

    @Test
    public int test2() {
        return 42;
    }

    // This version of @Check passes the return value from test2() as an argument.
    // The return type and the parameter type must match.
    @Check(test = "test2")
    public void checkWithReturn(int returnValue) {
        // Do some checks after an invocation.
        if (returnValue != 42) {
            throw new RuntimeException("Must match");
        }
    }

    @Test
    public int test3() {
        return 42;
    }

    // This version of @Check passes a TestInfo object to the check which contains some additional information about the test.
    @Check(test = "test3")
    public void checkWithTestInfo(TestInfo info) {
        // Do some checks after an invocation. Additional queries with TestInfo.
    }

    @Test
    public int test4() {
        return 42;
    }

    // This version of @Check passes a TestInfo object to the check which contains some additional information about the test
    // and additionally the return value. The order of the arguments is important. The return value must come first and then
    // the TestInfo parameter. Any other combination of different arguments are forbidden to specify for @Check methods.
    @Check(test = "test4")
    public void checkWithReturnAndTestInfo(int returnValue, TestInfo info) {
        // Do some checks after an invocation. Additional queries with TestInfo.
        if (returnValue != 42) {
            throw new RuntimeException("Must match");
        }
    }


    @Test
    public int test5() {
        return 42;
    }

    // Check method for test5() is only invoked once warmup is finished and test() has been compiled by the Test Framework.
    @Check(test = "test5", when = CheckAt.COMPILED) // Specify the @Test method for which this method is a check.
    public void checkAfterCompiled() {
        // Do some checks after compilation.
    }

}

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
 * @run driver compiler.valhalla.testframework.examples.TestExample
 */
 
package compiler.valhalla.testframework.examples;

import jdk.test.lib.hotspot.ir_framework.*;

public class TestExample {

    int iFld;

    public static void main(String[] args) {
        TestFramework.run(); // equivalent to TestFramework.run(TestSimpleTest.class)
    }

    /*
     * If there is no warm up specified the Test Framework will do the following:
     * - Invoke @Test method TestFrameworkExecution.WARMUP_ITERATIONS many times.
     * - Then do compilation of @Test method.
     * - Invoke @Test method once again
     */

    /*
     * Configurable things for simple tests (no @Run or @Check) at @Test method:
     * - @Warmup: Change warm-up iterations of test (defined by default by TestFrameworkExecution.WARMUP_ITERATIONS)
     * - @Arguments: If a @Test method specifies arguments, you need to provide arguments by using @Arguments such
     *               that the framework knows how to call the method. If you need more complex values, use @Run.
     * - @IR: Arbitrary number of @IR rules.
     */

    // Test without arguments.
    @Test
    public void mostBasicTest() {
        iFld = 42;
    }

    // Test with arguments. Use Argument class to choose a value.
    // Object arguments need to have an associated default constructor in its class.
    @Test
    @Arguments({Argument.DEFAULT, Argument.MAX})
    public void basicTestWithArguments(int x, long y) {
        iFld = x;
    }

    // @Warmup needs to be positive or zero. In case of zero, the method is directly compiled (simulated -Xcomp).
    @Test
    @Arguments({Argument.DEFAULT, Argument.MAX})
    @Warmup(100)
    public void basicTestWithDifferentWarmup(int x, long y) {
        iFld = x;
    }
}

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
 * @run driver jdk.test.lib.hotspot.ir_framework.examples.RunExample
 */

package jdk.test.lib.hotspot.ir_framework.examples;

import jdk.test.lib.hotspot.ir_framework.*;

public class RunExample {

    public static void main(String[] args) {
        TestFramework.run(); // equivalent to TestFramework.run(TestSimpleTest.class)
    }

    /*
     * If there is no warm up specified the Test Framework will do the following:
     * - Invoke @Run method TestFrameworkExecution.WARMUP_ITERATIONS many times. Note that the @Run method is responsible
     *   to invoke the @Test methods. This is not done by the framework. The @Run method can do any arbitrary argument setup
     *   and return value verification.
     * - After the warm-up, the @Test methods are compiled (there can be multiple @Test methods).
     * - Invoke @Run method once again.
     */

    /*
     * Configurable things for custom run tests:
     * - At @Test method:
     *   - @IR: Arbitrary number of @IR rules.
     *   - No @Warmup, this must be set at @Run method.
     *   - No @Arguments, these are set by @Run method.
     * - At @Run method:
     *   - @Warmup: Change warm-up iterations of @Run method (defined by default by TestFrameworkExecution.WARMUP_ITERATIONS)
     *   - test: Specify any number of @Test methods. They cannot be shared with other @Check or @Run methods.
     *   - mode: Choose between normal invocation as described above or STANDALONE. STANDALONE only invokes the @Run
     *           method once without warmup or a compilation by the Test Framework.
     *   - No @IR annotations
     */

    @Test
    public int test(int x) {
        return x;
    }

    // Run method for test(). Invoked directly by Test Framework instead of test().
    // Can do anything you like. It's also possible to skip or do multiple invocations of test()
    @Run(test = "test") // Specify the @Test method for which this method is a runner.
    public void basicRun() {
        int returnValue = test(34);
        if (returnValue != 34) {
            throw new RuntimeException("Must match");
        }
    }

    @Test
    public int test2(int x) {
        return x;
    }

    // This version of @Run passes the RunInfo object as an argument. No other argument combinations are allowed.
    @Run(test = "test2")
    public void runWithRunInfo(RunInfo info) {
        int returnValue = test(34);
        if (returnValue != 34) {
            throw new RuntimeException("Must match");
        }
    }

    @Test
    public int test3(int x) {
        return x;
    }

    // This version of @Run uses a user defined @Warmup.
    @Run(test = "test3")
    @Warmup(100)
    public void runWithWarmUp() {
        int returnValue = test3(34);
        if (returnValue != 34) {
            throw new RuntimeException("Must match");
        }
    }

    @Test
    public int test4(int x) {
        return x;
    }

    // This version of @Run is only invoked once by the Test Framework. There is no warm up and no compilation done
    // by the Test Framework
    @Run(test = "test4", mode = RunMode.STANDALONE)
    public void runOnlyOnce() {
        int returnValue = test4(34);
        if (returnValue != 34) {
            throw new RuntimeException("Must match");
        }
    }

    @Test
    public int test5(int x) {
        return x;
    }

    @Test
    public int test6(int x) {
        return x;
    }

    // This version of @Run can run multiple test methods and get them IR checked as part of this custom run test.
    @Run(test = {"test5", "test6"})
    public void runMultipleTests() {
        int returnValue = test5(34);
        if (returnValue != 34) {
            throw new RuntimeException("Must match");
        }
        returnValue = test6(42);
        if (returnValue != 42) {
            throw new RuntimeException("Must match");
        }
    }
}

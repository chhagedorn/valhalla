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

import jdk.test.lib.Asserts;
import jdk.test.lib.hotspot.ir_framework.*;

import java.util.Arrays;

/*
 * @test
 * @summary Test different custom run tests.
 * @library /test/lib
 * @run driver jdk.test.lib.hotspot.ir_framework.tests.TestRunTests
 */

public class TestRunTests {

    public static void main(String[] args) {
        TestFramework.run();
        try {
            TestFramework.run(BadStandalone.class);
            throw new RuntimeException("Should not reach");
        } catch (IRViolationException e) {
            String[] matches = { "test(int)", "test2(int)", "Failed IR Rules (2)"};
            Arrays.stream(matches).forEach(m -> Asserts.assertTrue(e.getMessage().contains(m)));
            Asserts.assertEQ(e.getMessage().split("STANDALONE mode", -1).length - 1, 2);
        }
    }
    public int iFld;

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test1(int x) {
        iFld = x;
        return x;
    }

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test2(int y) {
        iFld = y;
        return y;
    }

    @Run(test = {"test1", "test2"})
    public void run(RunInfo info) {
        test1(23);
        test2(42);
        if (!info.isWarmUp()) {
            TestFramework.assertCompiledByC2(info.getTest("test1"));
            TestFramework.assertCompiledByC2(info.getTest("test2"));
        }
    }

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test3(int x) {
        iFld = x;
        return x;
    }

    @Run(test = "test3")
    public void run2(RunInfo info) {
        test3(42);
        if (!info.isWarmUp()) {
            TestFramework.assertCompiledByC2(info.getTest());
        }
    }


    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test4(int x) {
        iFld = x;
        return x;
    }

    @Run(test = "test4", mode = RunMode.STANDALONE)
    public void run3(RunInfo info) {
        for (int i = 0; i < 2000; i++) {
            test4(i);
        }
    }

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test5(int x) {
        iFld = x;
        return x;
    }

    @Test(compLevel = CompLevel.WAIT_FOR_COMPILATION)
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test6(int y) {
        iFld = y;
        return y;
    }

    @Run(test = {"test5", "test6"})
    public void run4(RunInfo info) {
        test5(23);
        test6(42);
        if (!info.isWarmUp()) {
            TestFramework.assertCompiledByC2(info.getTest("test5"));
            TestFramework.assertCompiledByC2(info.getTest("test6"));
        }
    }


    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test7(int x) {
        for (int i = 0; i < 100; i++);
        iFld = x;
        return x;
    }


    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test8(int x) {
        for (int i = 0; i < 100; i++);
        iFld = x;
        return x;
    }

    @Run(test = {"test7", "test8"}, mode = RunMode.STANDALONE)
    public void run5() {
        for (int i = 0; i < 10000; i++) {
            test7(23);
            test8(42);
        }
    }

    @Test(compLevel = CompLevel.WAIT_FOR_COMPILATION)
    @Warmup(0)
    public void test9() {
        TestClass tmp = new TestClass();
        for (int i = 0; i < 100; ++i) {
            tmp.test();
        }
    }

    static class TestClass {
        public int test() {
            int res = 0;
            for (int i = 1; i < 20_000; ++i) {
                res -= i;
            }
            return res;
        }
    }
}

class BadStandalone {
    int iFld;

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test(int x) {
        iFld = x;
        return x;
    }

    @Run(test = "test", mode = RunMode.STANDALONE)
    public void run(RunInfo info) {
        test(42);
    }

    @Test
    @IR(counts = {IRNode.STORE_I, "1"})
    public int test2(int x) {
        iFld = x;
        return x;
    }

    @Run(test = "test2", mode = RunMode.STANDALONE)
    public void run2(RunInfo info) {
    }
}

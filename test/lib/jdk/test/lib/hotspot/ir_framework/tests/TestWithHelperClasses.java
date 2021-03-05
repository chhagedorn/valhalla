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

import jdk.test.lib.hotspot.ir_framework.CompLevel;
import jdk.test.lib.hotspot.ir_framework.ForceCompile;
import jdk.test.lib.hotspot.ir_framework.Test;
import jdk.test.lib.hotspot.ir_framework.TestFramework;
import jdk.test.lib.Asserts;

public class TestWithHelperClasses {

    public static void main(String[] args) {
        int exceptionsCaught = 0;
        TestFramework.runWithHelperClasses(TestWithHelperClasses.class, Helper1.class, Helper2.class);
        try {
            TestFramework.runWithHelperClasses(TestWithHelperClasses.class, Helper1.class);
        } catch (Exception e) {
            Asserts.assertFalse(e.getMessage().contains("public static void jdk.test.lib.hotspot.ir_framework.tests.Helper1.foo() should have been C2 compiled"));
            Asserts.assertFalse(TestFramework.getLastVMOutput().contains("public static void jdk.test.lib.hotspot.ir_framework.tests.Helper1.foo() should have been C2 compiled"));
            Asserts.assertTrue(TestFramework.getLastVMOutput().contains("public static void jdk.test.lib.hotspot.ir_framework.tests.Helper2.foo() should have been C2 compiled"));
            Asserts.assertTrue(e.getMessage().contains("public static void jdk.test.lib.hotspot.ir_framework.tests.Helper2.foo() should have been C2 compiled"));
            Asserts.assertFalse(TestFramework.getLastVMOutput().contains("Should not be executed"));
            Asserts.assertFalse(e.getMessage().contains("Should not be executed"));
            exceptionsCaught++;
        }
        try {
            TestFramework.runWithHelperClasses(BadHelperClasses.class, BadHelper.class);
        } catch (Exception e) {
            Asserts.assertTrue(e.getMessage().contains("Cannot use @Test annotation in helper class:"));
            Asserts.assertTrue(e.getMessage().contains("noTestInHelper"));
            exceptionsCaught++;
        }
        try {
            TestFramework.runWithHelperClasses(BadHelperClasses.class, BadHelper.class);
        } catch (Exception e) {
            Asserts.assertTrue(e.getMessage().contains("Cannot use @Test annotation in helper class:"));
            Asserts.assertTrue(e.getMessage().contains("noTestInHelper"));
            exceptionsCaught++;
        }
        if (exceptionsCaught != 2) {
            throw new RuntimeException("Did not catch " + exceptionsCaught + " exceptions!");
        }
    }

    @Test
    public void test() throws NoSuchMethodException {
        TestFramework.assertCompiledByC2(Helper1.class.getMethod("foo"));
        TestFramework.assertCompiledByC2(Helper2.class.getMethod("foo"));
        TestFramework.assertCompiledByC2(NestedHelper.class.getMethod("foo"));
        TestFramework.assertCompiledByC2(StaticNestedHelper.class.getMethod("foo"));
    }

    class NestedHelper {
        @ForceCompile(CompLevel.C2)
        public void foo() {
            throw new RuntimeException("Should not be executed");
        }
    }


    static class StaticNestedHelper {
        @ForceCompile(CompLevel.C2)
        public void foo() {
            throw new RuntimeException("Should not be executed");
        }
    }
}

class Helper1 {

    @ForceCompile(CompLevel.C2)
    public static void foo() {
        throw new RuntimeException("Should not be executed");
    }
}

class Helper2 {

    @ForceCompile(CompLevel.C2)
    public static void foo() {
        throw new RuntimeException("Should not be executed");
    }
}

class BadHelperClasses {
    @Test
    public void test() {}
}

class BadHelper {
    @Test
    public void noTestInHelper() {}
}

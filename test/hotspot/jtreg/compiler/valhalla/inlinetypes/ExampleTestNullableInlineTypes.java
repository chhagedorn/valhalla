/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @summary Test correct handling of nullable inline types.
 * @library /test/lib
 * @compile InlineTypes.java
 * @run driver compiler.valhalla.inlinetypes.ExampleTestNullableInlineTypes
 */

package compiler.valhalla.inlinetypes;

import jdk.test.lib.Asserts;
import jdk.test.lib.hotspot.ir_framework.*;

import static compiler.valhalla.inlinetypes.InlineTypes.IRNode.*;

public class ExampleTestNullableInlineTypes {
    private static final MyValue1 testValue1 = MyValue1.createWithFieldsInline(InlineTypes.rI, InlineTypes.rL);

    public static void main(String[] args) {
        Scenario[] scenarios = InlineTypes.DEFAULT_SCENARIOS;
        scenarios[3] = new Scenario(3, "-XX:-MonomorphicArrayCheck", "-XX:FlatArrayElementMaxSize=-1");
        scenarios[4] = new Scenario(4, "-XX:-MonomorphicArrayCheck");
        TestFramework testFramework = new TestFramework(TestNullableInlineTypes.class);
        testFramework.addScenarios(scenarios)
                     .addHelperClasses(MyValue1.class, MyValue2.class, MyValue2Inline.class)
                     .start();
    }

    // Test writing null to a flattenable/non-flattenable inline type field in an inline type
    final primitive class Test21Value {
        final MyValue1.ref valueField1;
        final MyValue1 valueField2;
        final MyValue1.ref alwaysNull = null;

        @ForceInline
        public Test21Value(MyValue1.ref valueField1, MyValue1 valueField2) {
            this.valueField1 = testValue1;
            this.valueField2 = testValue1;
        }

        @ForceInline
        public Test21Value test1() {
            return new Test21Value(alwaysNull, this.valueField2); // Should not throw NPE
        }

        @ForceInline
        public Test21Value test2() {
            return new Test21Value(this.valueField1, (MyValue1) alwaysNull); // Should throw NPE
        }
    }

    @Test
    public Test21Value test21(Test21Value vt) {
        vt = vt.test1();
        try {
            vt = vt.test2();
            throw new RuntimeException("NullPointerException expected");
        } catch (NullPointerException e) {
            // Expected
        }
        return vt;
    }

    @Run(test = "test21")
    public void test21_verifier() {
        test21(Test21Value.default);
    }


    @DontInline
    public void test25_callee(MyValue1 val) { }

    // Test that when checkcasting from null-ok to null-free and back to null-ok we
    // keep track of the information that the inline type can never be null.
    @Test
    @IR(failOn = {ALLOC, STORE})
    public int test25(boolean b, MyValue1.ref vt1, MyValue1 vt2) {
        vt1 = (MyValue1)vt1;
        Object obj = b ? vt1 : vt2; // We should not allocate here
        test25_callee((MyValue1) vt1);
        return ((MyValue1)obj).x;
    }

    @Run(test = "test25")
    public void test25_verifier() {
        int res = test25(true, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
        res = test25(false, testValue1, testValue1);
        Asserts.assertEquals(res, testValue1.x);
    }
}


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
 * @run driver compiler.valhalla.testframework.TestNullableInlineTypes
 */

package compiler.valhalla.testframework;

import jdk.test.lib.hotspot.ir_framework.*;

public abstract class TestNullableInlineTypes {
    private static final MyValue1 testValue1 = MyValue1.createWithFieldsInline(2, 3);

    public static void main(String[] args) {
        Scenario[] scenarios = DefaultScenarios.SCENARIOS;
        scenarios[3] = new Scenario(3, "-XX:-MonomorphicArrayCheck", "-XX:FlatArrayElementMaxSize=-1");
        scenarios[4] = new Scenario(4, "-XX:-MonomorphicArrayCheck");
        TestFramework testFramework = new TestFramework(TestNullableInlineTypes.class);
        testFramework.addScenarios(scenarios)
                     .addHelperClasses(MyValue1.class, MyValue2.class, MyValue2Inline.class,
                                       TestNullableInlineTypes.Test17Value.class, TestNullableInlineTypes.Test21Value.class)
                     .start();
    }

    // Test scalarization of default inline type with non-flattenable field
    final primitive class Test17Value {
        public final MyValue1.ref valueField;

        @ForceInline
        public Test17Value(MyValue1.ref valueField) {
            this.valueField = valueField;
        }
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
        public TestNullableInlineTypes.Test21Value test1() {
            return new TestNullableInlineTypes.Test21Value(alwaysNull, this.valueField2); // Should not throw NPE
        }

        @ForceInline
        public TestNullableInlineTypes.Test21Value test2() {
            return new TestNullableInlineTypes.Test21Value(this.valueField1, (MyValue1) alwaysNull); // Should throw NPE
        }
    }
}


/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test the handling of fields of unloaded inline classes.
 * @library /test/lib
 * @compile MyConstants.java
 * @run driver compiler.valhalla.testframework.TestUnloadedInlineTypeField
 */
package compiler.valhalla.testframework;

import jdk.test.lib.Asserts;
import jdk.test.lib.hotspot.ir_framework.*;

public class TestUnloadedInlineTypeField {

    public static void main(String[] args) {
        Scenario s0 = new Scenario(0);
        Scenario s1 = new Scenario(1, "-XX:InlineFieldMaxFlatSize=0");
        Scenario s2 = new Scenario(2, "-XX:+PatchALot");
        Scenario s3 = new Scenario(3, "-XX:InlineFieldMaxFlatSize=0", "-XX:+PatchALot");
        TestFramework.runWithScenarios(s0, s1, s2, s3);
    }

    // Test case 1:
    // The inline type field class has been loaded, but the holder class has not been loaded.
    //
    //     aload_0
    //     getfield  MyValue1Holder.v:QMyValue1;
    //               ^ not loaded      ^ already loaded
    //
    // MyValue1 has already been loaded, because it's in the InlineType attribute of
    // TestUnloadedInlineTypeField, due to TestUnloadedInlineTypeField.test1_precondition().
    static final primitive class MyValue1 {
        final int foo;

        MyValue1() {
            foo = MyConstants.rI;
        }
    }

    static class MyValue1Holder {
        MyValue1 v;

        public MyValue1Holder() {
            v = new MyValue1();
        }
    }

    static MyValue1 test1_precondition() {
        return new MyValue1();
    }

    @Test
    public int test1(Object holder) {
        if (holder != null) {
            // Don't use MyValue1Holder in the signature, it might trigger class loading
            return ((MyValue1Holder)holder).v.foo;
        } else {
            return 0;
        }
    }

    @Run(test = "test1")
    public void test1_verifier(TestInfo info) {
        if (info.isWarmUp() && info.isC1Test()) {
            test1(null);
        } else {
            MyValue1Holder holder = new MyValue1Holder();
            Asserts.assertEQ(test1(holder), MyConstants.rI);
        }
    }
}

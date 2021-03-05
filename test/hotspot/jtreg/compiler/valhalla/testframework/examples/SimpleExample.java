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
 * @run driver compiler.valhalla.testframework.examples.SimpleExample
 */
 
package compiler.valhalla.testframework.examples;

import jdk.test.lib.hotspot.ir_framework.*;

public class SimpleExample {

    int iFld;

    public static void main(String[] args) {
        TestFramework.run();
    }

    // TestFramework will verify that this @IR rule works if it is called with a debug build.
    // With a product build, it just executes this method without IR verification (Print flags
    // for verification are only available in debug builds).
    @Test
    @IR(failOn = IRNode.LOOP, counts = {IRNode.STORE_I, "1"})
    public void test() {
        iFld = 42;
    }
}

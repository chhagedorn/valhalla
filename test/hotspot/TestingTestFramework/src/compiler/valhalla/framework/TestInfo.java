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

package compiler.valhalla.framework;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.Random;

public class TestInfo {
    private static final Random random = new Random();
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    protected static final long PerMethodTrapLimit = (Long)WHITE_BOX.getVMFlag("PerMethodTrapLimit");
    protected static final boolean ProfileInterpreter = (Boolean)WHITE_BOX.getVMFlag("ProfileInterpreter");

    private boolean toggleBool = false;
    private boolean onWarmUp = true;
    private final Method testMethod;

    public TestInfo(Method testMethod) {
        this.testMethod = testMethod;
    }

    public boolean toggleBoolean() {
        toggleBool = !toggleBool;
        return toggleBool;
    }

    public static int getRandomInt() {
        return random.nextInt() % 1000;
    }

    public static long getRandomLong() {
        return random.nextLong() % 1000;
    }

    public static double getRandomDouble() {
        return random.nextDouble() % 1000;
    }

    public boolean isWarmUp() {
        return onWarmUp;
    }

    void setWarmUpFinished() {
        onWarmUp = false;
    }

    public Method getTest() {
        return testMethod;
    }

//    public boolean isC2Compiled(Method m) {
//        return WHITE_BOX.isMethodCompiled(m, false) && WHITE_BOX.getMethodCompilationLevel(m, false) == CompLevel.C2.getValue();
////        return compiledByC2(m) == TriState.Yes;
//    }
//
//    public boolean isCompiledAtLevel(Method m, CompLevel level) {
//        return WHITE_BOX.isMethodCompiled(m, false) && WHITE_BOX.getMethodCompilationLevel(m, false) == level.getValue();
////        return compiledByC2(m) == TriState.Yes;
//    }
//
//    public void assertDeoptimizedByC2(Method m) {
//        TestRun.check(!isC2Compiled(m) || PerMethodTrapLimit == 0 || !ProfileInterpreter, m + " should have been deoptimized");
//    }
//
//    public void assertCompiledByC2(Method m) {
//        TestRun.check(isC2Compiled(m), m + " should have been compiled");
//    }
//
//    public void assertCompiledAtLevel(Method m, CompLevel level) {
//        TestRun.check(isCompiledAtLevel(m, level), m + " should have been compiled at level " + level.name());
//    }
}

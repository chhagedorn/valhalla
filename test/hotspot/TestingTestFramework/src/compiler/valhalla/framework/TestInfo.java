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
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

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

    public Method getMethod(Class<?> c, String name, Class<?>... args) {
        try {
            return c.getMethod(name, args);
        } catch (NoSuchMethodException e) {
            String parameters = args == null || args.length == 0 ? "" :
                    " with arguments [" + Arrays.stream(args).map(Class::getName).collect(Collectors.joining(",")) + "]";
            throw new TestRunException("Could not find method " + name + " in " + c + parameters);
        }
    }

    public Method getTestClassMethod(String name, Class<?>... args) {
        return getMethod(testMethod.getDeclaringClass(), name, args);
    }

    public boolean isC1Compiled(Method m) {
        return TestFramework.isC1Compiled(testMethod);
    }

    public boolean isC2Compiled(Method m) {
        return TestFramework.isC2Compiled(testMethod);
    }

    public boolean isCompiledAtLevel(CompLevel compLevel) {
        return TestFramework.isCompiledAtLevel(testMethod, compLevel);
    }

    public void assertDeoptimizedByC1() {
        TestFramework.assertDeoptimizedByC1(testMethod);
    }

    public void assertCompiledByC1() {
        TestFramework.assertCompiledByC1(testMethod);
    }

    public void assertDeoptimizedByC2() {
        TestFramework.assertDeoptimizedByC2(testMethod);
    }

    public void assertCompiledByC2() {
        TestFramework.assertCompiledByC2(testMethod);
    }

    public void assertCompiledAtLevel(CompLevel level) {
        TestFramework.assertCompiledAtLevel(testMethod, level);
    }
}

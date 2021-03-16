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

package jdk.test.lib.hotspot.ir_framework;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunInfo extends AbstractInfo {

    private final Method testMethod;
    private final Map<String, Method> testMethods;

    RunInfo(Method testMethod) {
        super(testMethod.getDeclaringClass());
        this.testMethod = testMethod;
        this.testMethods = null;
    }

    RunInfo(List<Method> testMethods) {
        super(testMethods.get(0).getDeclaringClass());
        this.testMethods = new HashMap<>();
        for (Method m : testMethods) {
            this.testMethods.put(m.getName(), m);
        }
        this.testMethod = testMethods.get(0);
    }

    public Method getTest() {
        checkSingleTest("getTest");
        return testMethod;
    }

    public Method getTest(String testName) {
        return getMethod(testName);
    }

    public boolean isTestC1Compiled() {
        checkSingleTest("isTestC1Compiled");
        return TestFrameworkExecution.isC1Compiled(testMethod);
    }

    public boolean isTestC1Compiled(String testName) {
        return TestFrameworkExecution.isC1Compiled(getMethod(testName));
    }

    public boolean isTestC2Compiled() {
        checkSingleTest("isTestC2Compiled");
        return TestFrameworkExecution.isC2Compiled(testMethod);
    }

    public boolean isTestC2Compiled(String testName) {
        return TestFrameworkExecution.isC2Compiled(getMethod(testName));
    }

    public boolean isTestCompiledAtLevel(CompLevel compLevel) {
        checkSingleTest("isTestCompiledAtLevel");
        return TestFrameworkExecution.isCompiledAtLevel(testMethod, compLevel);
    }

    public boolean isTestCompiledAtLevel(String testName, CompLevel compLevel) {
        return TestFrameworkExecution.isCompiledAtLevel(getMethod(testName), compLevel);
    }

    public void assertTestDeoptimizedByC1() {
        checkSingleTest("assertTestDeoptimizedByC1");
        TestFrameworkExecution.assertDeoptimizedByC1(testMethod);
    }

    public void assertTestDeoptimizedByC1(String testName) {
        TestFrameworkExecution.assertDeoptimizedByC1(getMethod(testName));
    }

    public void assertTestCompiledByC1() {
        checkSingleTest("assertTestCompiledByC1");
        TestFrameworkExecution.assertCompiledByC1(testMethod);
    }

    public void assertTestCompiledByC1(String testName) {
        TestFrameworkExecution.assertCompiledByC1(getMethod(testName));
    }

    public void assertTestDeoptimizedByC2() {
        checkSingleTest("assertTestDeoptimizedByC2");
        TestFrameworkExecution.assertDeoptimizedByC2(testMethod);
    }

    public void assertTestDeoptimizedByC2(String testName) {
        TestFrameworkExecution.assertDeoptimizedByC2(getMethod(testName));
    }

    public void assertTestCompiledByC2() {
        checkSingleTest("assertTestCompiledByC2");
        TestFrameworkExecution.assertCompiledByC2(testMethod);
    }

    public void assertTestCompiledByC2(String testName) {
        TestFrameworkExecution.assertCompiledByC2(getMethod(testName));
    }

    public void assertTestCompiledAtLevel(CompLevel level) {
        checkSingleTest("assertTestCompiledAtLevel");
        TestFrameworkExecution.assertCompiledAtLevel(testMethod, level);
    }

    public void assertTestCompiledAtLevel(String testName, CompLevel level) {
        TestFrameworkExecution.assertCompiledAtLevel(getMethod(testName), level);
    }

    private void checkSingleTest(String calledMethod) {
        if (testMethod == null) {
            throw new TestRunException("Use " + calledMethod + " with testName String argument in @Run method when running " +
                                       "more than one @Test method.");
        }
    }

    private Method getMethod(String testName) {
        Method m = testMethods.get(testName);
        if (m == null) {
            throw new RuntimeException("Could not find @Test \"" + testName + "\" in " + testClass + " being associated with" +
                                       " corresponding @Run method.");
        }
        return m;
    }
}

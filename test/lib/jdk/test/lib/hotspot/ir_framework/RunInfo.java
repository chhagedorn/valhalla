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

/**
 * Test info class which provides some useful utility methods and information about a <b>custom run test</b>.
 * 
 * @see Run
 */
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

    /**
     * Get the associated test method object of this custom run test. This method can only be called if one test method
     * is specified in the custom run test ({@link Run#test()}). Otherwise, use {@link #getTest(String)}.
     *
     * @return the associated test method object.
     * @throws TestRunException if called for a custom run test that specifies multiple test methods in {@link Run#test()}.
     */
    public Method getTest() {
        checkSingleTest("getTest");
        return testMethod;
    }


    /**
     * Get the associated method object of the test method with the name {@code testName}. If the custom run test only
     * specifies one test method ({@link Run#test()}), consider using {@link #getTest()}.
     *
     * @param testName the test method for which the method object should be returned.
     * @return the associated test method object with the name {@code testName}.
     * @throws TestRunException if there is no test method with the name {@code testName}.
     */
    public Method getTest(String testName) {
        return getMethod(testName);
    }

    /**
     * Returns a boolean indicating if the associated test method is C1 compiled. This method can only be called if one
     * test method is specified in the custom run test ({@link Run#test()}). Otherwise, use {@link #isTestC1Compiled(String)}.
     *
     * @return {@code true} if the associated test method is C1 compiled;
     *         {@code false} otherwise.
     * @throws TestRunException if called for a custom run test that specifies multiple test methods in {@link Run#test()}.
     */
    public boolean isTestC1Compiled() {
        checkSingleTest("isTestC1Compiled");
        return TestFrameworkExecution.isC1Compiled(testMethod);
    }

    /**
     * Returns a boolean indicating if the associated test method with the name {@code testName} is C1 compiled. If the
     * custom run test only specifies one test method ({@link Run#test()}), consider using {@link #isTestC1Compiled()}.
     *
     * @param testName the name of the test method.
     * @return {@code true} if the test method with the name {@code testName} is C2 compiled;
     *         {@code false} otherwise.
     * @throws TestRunException if there is no test method with the name {@code testName}.
     */
    public boolean isTestC1Compiled(String testName) {
        return TestFrameworkExecution.isC1Compiled(getMethod(testName));
    }

    /**
     * Returns a boolean indicating if the associated test method is C2 compiled. This method can only be called if one
     * test method is specified in the custom run test ({@link Run#test()}). Otherwise, use {@link #isTestC2Compiled(String)}.
     *
     * @return {@code true} if the associated test method is C2 compiled;
     *         {@code false} otherwise.
     * @throws TestRunException if called for a custom run test that specifies multiple test methods in {@link Run#test()}.
     */
    public boolean isTestC2Compiled() {
        checkSingleTest("isTestC2Compiled");
        return TestFrameworkExecution.isC2Compiled(testMethod);
    }

    /**
     * Returns a boolean indicating if the associated test method with the name {@code testName} is C2 compiled. If the
     * custom run test only specifies one test method ({@link Run#test()}), consider using {@link #isTestC2Compiled()}.
     *
     * @param testName the name of the test method.
     * @return {@code true} if the test method with the name {@code testName} is C2 compiled;
     *         {@code false} otherwise.
     * @throws TestRunException if there is no test method with the name {@code testName}.
     */
    public boolean isTestC2Compiled(String testName) {
        return TestFrameworkExecution.isC2Compiled(getMethod(testName));
    }

    /**
     * Returns a boolean indicating if the associated test method is compiled at {@code compLevel}. This method can only
     * be called if one test method is specified in the custom run test ({@link Run#test()}).
     * Otherwise, use {@link #isTestCompiledAtLevel(String, CompLevel)}.
     *
     * @param compLevel the compilation level
     * @return {@code true} if the associated test method is compiled at {@code compLevel};
     *         {@code false} otherwise.
     * @throws TestRunException if called for a custom run test that specifies multiple test methods in {@link Run#test()}.
     */
    public boolean isTestCompiledAtLevel(CompLevel compLevel) {
        checkSingleTest("isTestCompiledAtLevel");
        return TestFrameworkExecution.isCompiledAtLevel(testMethod, compLevel);
    }

    /**
     * Returns a boolean indicating if the associated test method with the name {@code testName} is compiled at
     * {@code compLevel}. If thec ustom run test only specifies one test method ({@link Run#test()}),
     * consider using {@link #isTestCompiledAtLevel(CompLevel)} )}.
     *
     * @param testName the name of the test method.
     * @param compLevel the compilation level.
     * @return {@code true} if the test method with the name {@code testName} is compiled at {@code compLevel};
     *         {@code false} otherwise.
     * @throws TestRunException if there is no test method with the name {@code testName}.
     */
    public boolean isTestCompiledAtLevel(String testName, CompLevel compLevel) {
        return TestFrameworkExecution.isCompiledAtLevel(getMethod(testName), compLevel);
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
            throw new TestRunException("Could not find @Test \"" + testName + "\" in " + testClass + " being associated with" +
                                       " corresponding @Run method.");
        }
        return m;
    }
}

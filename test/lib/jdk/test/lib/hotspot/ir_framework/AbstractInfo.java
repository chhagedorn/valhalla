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
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Base info class which provides some useful utility methods and information about a test.
 * <p>
 * <b>Base tests</b> and <b>checked tests</b> use {@link TestInfo} while <b>custom run tests</b> use {@link RunInfo}.
 * 
 * @see Test
 * @see Check
 */
abstract public class AbstractInfo {
    private static final Random random = new Random();

    protected final Class<?> testClass;
    private boolean toggleBool = false;
    private boolean onWarmUp = true;

    AbstractInfo(Class<?> testClass) {
        this.testClass = testClass;
    }
    
    /**
     * Returns a different boolean each time this method is invoked (switching between {@code false} and {@code true}.
     * The first invocation returns {@code false}.
     *
     * @return an inverted boolean of the result of the last invocation of this method.
     */
    public boolean toggleBoolean() {
        toggleBool = !toggleBool;
        return toggleBool;
    }

    /**
     * Get a random integer.
     *
     * @return a random integer
     */
    public static int getRandomInt() {
        return random.nextInt();
    }

    /**
     * Get a random long value.
     *
     * @return a random long value.
     */
    public static long getRandomLong() {
        return random.nextLong();
    }

    /**
     * Get a random double value.
     *
     * @return a random double value.
     */
    public static double getRandomDouble() {
        return random.nextDouble();
    }

    /**
     * Returns a boolean indicating if the framework is currently warming up the associated test.
     *
     * @return the warm-up status of the associated test.
     *
     * @see Warmup
     */
    public boolean isWarmUp() {
        return onWarmUp;
    }

    /**
     * Get the method object of the method {@code name} of class {@code c} with arguments {@code args}.
     *
     * @param c    the class containing the method.
     * @param name the name of the method.
     * @param args the arguments of the method, leave empty if no arguments.
     *
     * @return the method object of the requested method.
     */
    public Method getMethod(Class<?> c, String name, Class<?>... args) {
        try {
            return c.getMethod(name, args);
        } catch (NoSuchMethodException e) {
            String parameters = args == null || args.length == 0 ? "" :
                    " with arguments [" + Arrays.stream(args).map(Class::getName).collect(Collectors.joining(",")) + "]";
            throw new TestRunException("Could not find method " + name + " in " + c + parameters);
        }
    }

    /**
     * Get the method object of the method {@code name} of the test class with arguments {@code args}.
     *
     * @param name the name of the method in the test class.
     * @param args the arguments of the method, leave empty if no arguments.
     *
     * @return the method object of the requested method in the test class.
     */
    public Method getTestClassMethod(String name, Class<?>... args) {
        return getMethod(testClass, name, args);
    }

    /**
     * Returns a boolean indicating if the test VM runs with flags that only allow C1 compilations:
     * {@code -XX:+TieredCompilation -XX:TieredStopAtLevel={1,2,3}}
     *
     * @return {@code true} if only C1 compilations are allowed;
     *         {@code false} otherwise.
     */
    public boolean isC1Test() {
        return TestFrameworkExecution.TEST_C1;
    }

    /**
     * Called by framework when the warm-up is finished. Not exposed to users.
     */
    void setWarmUpFinished() {
        onWarmUp = false;
    }
}

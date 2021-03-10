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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotate all methods in your test class which the framework should test with {@code @Test}.
 * <p>
 * Let {@code m} be a test method specifying the {@code @Test} annotation. If {@code m} is not part of a custom run test
 * (an additional method specifying {@link Run} with (@code @Run(test = "m"))), then the framework invokes {@code m} in
 * the following way:
 * <ol>
 *     <li><p>The framework warms {@code m} up for a predefined number of iterations (default is 2000) or any number
 *     specified by an additional {@link Warmup} iteration (could also be 0 which skips the warm-up completely). More
 *     information about the warm-up can be found in {@link Warmup}</li>
 *     <li><p>After the warm-up, the framework compiles {@code m} at the specified compilation level set by
 *     {@link Test#compLevel()} (default {@link CompLevel#C2}).</li>
 *     <li><p>The framework invokes {@code m} one more time to ensure that the compilation works.</li>
 *     <li><p>The framework checks any specified {@link IR} constraints. More information about IR matching can be
 *     found in {@link IR}.</li>
 * </ol>
 *
 * <p>
 * {@code m} has the following properties:
 * <ul>
 *     <li><p>If {@code m} specifies no parameters, the framework can directly invoke it.</li>
 *     <li><p>If {@code m} specifies parameters, the framework needs to know how to call {@code m}. Use {@link Arguments}
 *     with {@link Argument} properties for each parameter to use some well-defined parameters. If the method requires
 *     a more specific argument value, use a custom run test (see {@link Run}).</li>
 *     <li><p>{@code m} is not inlined by the framework.</li>
 *     <li><p>Verification of the return value of {@code m} can only TODO </li>
 * </ul>
 *
 * <p>
 * The following constraints must be met for a test method {@code m} specifying {@code @Test}:
 * <ul>
 *     <li><p>{@code m} must be part of the test class. Using {@code @Test} in other classes is not allowed.</li>
 *     <li><p>{@code m} cannot have the same name as another {@code @Test} method. Method overloading is only allowed
 *     with other non-{@code @Test} methods.</li>
 *     <li><p>{@code m} cannot specify any compile command annotations ({@link ForceCompile}, {@link DontCompile}, {@link ForceInline}, {@link DontInline}). </li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Test {
    /**
     * Specify at which compilation level the framework should eventually compile the test method after an optional
     * warmup period.
     *
     * <p>
     * Default if not specified in annotation: {@link CompLevel#C2}.
     */
    CompLevel compLevel() default CompLevel.C2;
}

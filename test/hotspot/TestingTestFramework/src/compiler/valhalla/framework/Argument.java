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

package compiler.valhalla.framework;

public enum Argument {
    /**
     * Provides the default value for any kind of primitive type and objects type if the class provides a default constructor.
     */
    DEFAULT,
    /**
     * Provides the number 42 for any primitive number type.
     */
    NUMBER_42,
    /**
     * Provides the number -42 for any primitive number type.
     */
    NUMBER_MINUS_42,
    /**
     * Provides the minimum value of the specified primitive number type.
     */
    MIN,
    /**
     * Provides the maximum value of the specified primitive number type.
     */
    MAX,
    /**
     * Provides the boolean value false.
     */
    FALSE,
    /**
     * Provides the boolean value true.
     */
    TRUE,
    /**
     * Provides a different boolean value on each test invocation, starting with false.
     */
    BOOLEAN_TOGGLE_FIRST_FALSE,
    /**
     * Provides a different boolean value on each test invocation, starting with true.
     */
    BOOLEAN_TOGGLE_FIRST_TRUE,
    /**
     * Provides a random primitive value on the first test invocation and reuses the same value for all invocation of the test.
     */
    RANDOM_ONCE,
    /**
     * Provides a different random primitive value on each test invocation.
     */
    RANDOM_EACH
}

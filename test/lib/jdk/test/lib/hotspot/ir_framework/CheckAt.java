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

/**
 * Enum used at in the {@link Check} annotation of a checked test. It specifies when the framework will invoke the
 * check method after invoking the associated {@link Test} method.
 *
 * @see Check
 * @see Test
 */
public enum CheckAt {

    /**
     * Invoke the {@link Check} method each time after invoking the associated {@link Test} method.
     */
    EACH_INVOCATION,
    /**
     * Invoke the {@link Check} method only once after the warmup of the associated {@link Test} method completed has
     * completed and test framework has compiled the test method.
     */
    COMPILED
}

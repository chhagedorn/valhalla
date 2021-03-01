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

package testframework;

import java.util.HashMap;
import java.util.Map;

public enum CompLevel {
    /**
     * Skip a {@link Test @Test} when set as {@link Test#compLevel()}.
     */
    SKIP(-3),
    /**
     *  Use any compilation level depending on the usage:
     *  <ul>
     *      <li><p>{@link Test @Test}, {@link ForceCompile @ForceCompile}: Use the highest available compilation level
     *      which is usually C2.</li>
     *      <li><p>{@link DontCompile @DontCompile}: Prevents any compilation of the associated helper method.</li>
     *  </ul>
     */
    ANY(-2),
    /**
     *  Compilation level 1: C1 compilation without any profile information.
     */
    C1(1),
    /**
     *  Compilation level 2: C1 compilation with limited profile information: Includes Invocation and backedge counters.
     */
    C1_LIMITED_PROFILE(2),
    /**
     *  Compilation level 3: C1 compilation with full profile information: Includes Invocation and backedge counters with MDO.
     */
    C1_FULL_PROFILE(3),
    /**
     * Compilation level 4: C2 compilation with full optimizations.
     */
    C2(4);

    private static final Map<Integer, CompLevel> typesByValue = new HashMap<>();
    private final int value;

    static {
        for (CompLevel level : CompLevel.values()) {
            typesByValue.put(level.value, level);
        }
    }

    CompLevel(int level) {
        this.value = level;
    }

    public int getValue() {
        return value;
    }

    public static CompLevel forValue(int value) {
        return typesByValue.get(value);
    }

    public static boolean overlapping(CompLevel l1, CompLevel l2) {
        return l1.isC1() == l2.isC1() || (l1 == C2 && l2 == C2);
    }

    private boolean isC1() {
        return this == C1 || this == C1_LIMITED_PROFILE || this == C1_FULL_PROFILE;
    }
}

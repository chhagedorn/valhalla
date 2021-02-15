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

import java.util.HashMap;
import java.util.Map;

public enum CompLevel {
    SKIP(-3), // Skip a @Test having this value
    ANY(-2),
    C1(1), // C1
    C1_LIMITED_PROFILE(2), // C1, invocation & backedge counters
    C1_FULL_PROFILE(3), // C1, invocation & backedge counters + mdo
    C2(4); // C2 or JVMCI


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
}

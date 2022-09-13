/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package compiler.valhalla.inlinetypes;

public class InlineTypeIRNode {
    private static final String PREFIX = "_#";
    private static final String POSTFIX = "#I_";
    public static final String ALLOC_G = PREFIX + "ALLOC_G" + POSTFIX;
    public static final String ALLOCA_G = PREFIX + "ALLOCA_G" + POSTFIX;
    public static final String MYVALUE_ARRAY_KLASS = PREFIX + "MYVALUE_ARRAY_KLASS" + POSTFIX;
    public static final String ALLOC = PREFIX + "ALLOC" + POSTFIX;
    public static final String ALLOCA = PREFIX + "ALLOCA" + POSTFIX;
    public static final String LOAD = PREFIX + "LOAD" + POSTFIX;
    public static final String LOADK = PREFIX + "LOADK" + POSTFIX;
    public static final String STORE = PREFIX + "STORE" + POSTFIX;
    public static final String LOOP = PREFIX + "LOOP" + POSTFIX;
    public static final String COUNTEDLOOP = PREFIX + "COUNTEDLOOP" + POSTFIX;
    public static final String COUNTEDLOOP_MAIN = PREFIX + "COUNTEDLOOP_MAIN" + POSTFIX;
    public static final String TRAP = PREFIX + "TRAP" + POSTFIX;
    public static final String LINKTOSTATIC = PREFIX + "LINKTOSTATIC" + POSTFIX;
    public static final String NPE = PREFIX + "NPE" + POSTFIX;
    public static final String CALL = PREFIX + "CALL" + POSTFIX;
    public static final String CALL_LEAF = PREFIX + "CALL_LEAF" + POSTFIX;
    public static final String CALL_LEAF_NOFP = PREFIX + "CALL_LEAF_NOFP" + POSTFIX;
    public static final String CALL_UNSAFE = PREFIX + "CALL_UNSAFE" + POSTFIX;
    public static final String STORE_INLINE_FIELDS = PREFIX + "STORE_INLINE_FIELDS" + POSTFIX;
    public static final String SCOBJ = PREFIX + "SCOBJ" + POSTFIX;
    public static final String LOAD_UNKNOWN_INLINE = PREFIX + "LOAD_UNKNOWN_INLINE" + POSTFIX;
    public static final String STORE_UNKNOWN_INLINE = PREFIX + "STORE_UNKNOWN_INLINE" + POSTFIX;
    public static final String INLINE_ARRAY_NULL_GUARD = PREFIX + "INLINE_ARRAY_NULL_GUARD" + POSTFIX;
    public static final String INTRINSIC_SLOW_PATH = PREFIX + "INTRINSIC_SLOW_PATH" + POSTFIX;
    public static final String CLONE_INTRINSIC_SLOW_PATH = PREFIX + "CLONE_INTRINSIC_SLOW_PATH" + POSTFIX;
    public static final String CLASS_CHECK_TRAP = PREFIX + "CLASS_CHECK_TRAP" + POSTFIX;
    public static final String NULL_CHECK_TRAP = PREFIX + "NULL_CHECK_TRAP" + POSTFIX;
    public static final String NULL_ASSERT_TRAP = PREFIX + "NULL_ASSERT_TRAP" + POSTFIX;
    public static final String RANGE_CHECK_TRAP = PREFIX + "RANGE_CHECK_TRAP" + POSTFIX;
    public static final String UNHANDLED_TRAP = PREFIX + "UNHANDLED_TRAP" + POSTFIX;
    public static final String PREDICATE_TRAP = PREFIX + "PREDICATE_TRAP" + POSTFIX;
    public static final String MEMBAR = PREFIX + "MEMBAR" + POSTFIX;
    public static final String CHECKCAST_ARRAY = PREFIX + "CHECKCAST_ARRAY" + POSTFIX;
    public static final String CHECKCAST_ARRAYCOPY = PREFIX + "CHECKCAST_ARRAYCOPY" + POSTFIX;
    public static final String JLONG_ARRAYCOPY = PREFIX + "JLONG_ARRAYCOPY" + POSTFIX;
    public static final String FIELD_ACCESS = PREFIX + "FIELD_ACCESS" + POSTFIX;
    public static final String SUBSTITUTABILITY_TEST = PREFIX + "SUBSTITUTABILITY_TEST" + POSTFIX;
    public static final String CMPP = PREFIX + "CMPP" + POSTFIX;
}

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

package valhallatests;

public class ValhallaIRNode {
    private static final String START = "(\\d+(\\s){2}(";
    private static final String MID = ".*)+(\\s){2}===.*";
    private static final String END = ")";

    public static final String CHECKCAST_ARRAY_INLINE = "(cmp.*precise klass \\[(L|Q)compiler/valhalla/inlinetypes/MyValue.*" + END;
    public static final String ALLOCA = "(.*precise klass \\[(L|Q)compiler/valhalla/inlinetypes/MyValue.*\\R(.*(movl|xorl|nop|spill).*\\R)*.*_new_array_Java" + END;
    public static final String STORE_INLINE_FIELDS = START + "CallStaticJava" + MID + "store_inline_type_fields" + END;
    public static final String LOAD_UNKNOWN_INLINE = "(.*call_leaf,runtime  load_unknown_inline.*" + END;
    public static final String STORE_UNKNOWN_INLINE = "(.*call_leaf,runtime  store_unknown_inline.*" + END;
    public static final String INLINE_ARRAY_NULL_GUARD = "(.*call,static  wrapper for: uncommon_trap.*reason='null_check' action='none'.*" + END;
    public static final String INTRINSIC_SLOW_PATH = "(.*call,static  wrapper for: uncommon_trap.*reason='intrinsic_or_type_checked_inlining'.*" + END;
    public static final String CLONE_INTRINSIC_SLOW_PATH = "(.*call,static.*java.lang.Object::clone.*" + END;
    public static final String JLONG_ARRAYCOPY = "(.*call_leaf_nofp,runtime  jlong_disjoint_arraycopy.*" + END;
    public static final String SUBSTITUTABILITY_TEST = START + "CallStaticJava" + MID + "java.lang.invoke.ValueBootstrapMethods::isSubstitutable" + END;
}

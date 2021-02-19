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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class IRNode {
    private static final String START = "(\\d+(\\s){2}(";
    private static final String MID = ".*)+(\\s){2}===.*";
    private static final String END = ")";

    public static final String ALLOC = "(.*precise klass .*\\R(.*(movl|xorl|nop|spill).*\\R)*.*call,static  wrapper for: _new_instance_Java" + END;
    public static final String ALLOC_OF = "(.*precise klass .*";
    private static final String ALLOC_OF_POSTFIX =  ":.*\\R(.*(movl|xorl|nop|spill).*\\R)*.*call,static  wrapper for: _new_instance_Java" + END;

    public static final String ALLOC_ARRAY = "(.*precise klass \\[L.*\\R(.*(movl|xorl|nop|spill).*\\R)*.*call,static  wrapper for: _new_array_Java" + END;
    public static final String ALLOC_ARRAY_OF = "(.*precise klass \\[L.*";
    private static final String ALLOC_ARRAY_OF_POSTFIX = ";:.*\\R(.*(movl|xorl|nop|spill).*\\R)*.*call,static  wrapper for: _new_array_Java" + END;

    public static final String STORE = START + "Store(B|C|S|I|L|F|D|P|N)" + MID + END;
    public static final String STORE_OF_CLASS = START + "Store(B|C|S|I|L|F|D|P|N)" + MID + "@\\S*";
    private static final String STORE_OF_CLASS_POSTFIX = "(:|\\+)\\S* \\*" + END;
    public static final String STORE_OF_FIELD = START + "Store(B|C|S|I|L|F|D|P|N)" + MID + "@.*name=";
    private static final String STORE_OF_FIELD_POSTFIX = ",.*" + END;

    public static final String LOAD = START + "Load(B|S|I|L|F|D|P|N)" + MID + END;
    public static final String LOAD_OF_CLASS = START + "Load(B|C|S|I|L|F|D|P|N)" + MID + "@\\S*";
    private static final String LOAD_OF_CLASS_POSTFIX = "(:|\\+)\\S* \\*" + END;
    public static final String LOAD_OF_FIELD = START + "Load(B|C|S|I|L|F|D|P|N)" + MID + "@.*name=";
    private static final String LOAD_OF_FIELD_POSTFIX = ",.*" + END;
    public static final String LOAD_KLASS  = START + "LoadK" + MID + END;



    public static final String LOOP   = START + "Loop" + MID + "" + END;
    public static final String COUNTEDLOOP = START + "CountedLoop\\b" + MID + "" + END;
    public static final String COUNTEDLOOP_MAIN = START + "CountedLoop\\b" + MID + "main" + END;

    public static final String CALL = START + "CallStaticJava" + MID + END;
    public static final String TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*reason" + END;
    public static final String PREDICATE_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*predicate" + END;
    public static final String UNSTABLE_IF_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*unstable_if" + END;
    public static final String CLASS_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*class_check" + END;
    public static final String NULL_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_check" + END;
    public static final String NULL_ASSERT_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_assert" + END;
    public static final String RANGE_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*range_check" + END;
    public static final String UNHANDLED_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*unhandled" + END;


    // Inline type allocation
    public static final String ALLOCA = "(.*precise klass \\[(L|Q)compiler/valhalla/inlinetypes/MyValue.*\\R(.*(movl|xorl|nop|spill).*\\R)*.*_new_array_Java" + END;
    public static final String LINKTOSTATIC = START + "CallStaticJava" + MID + "linkToStatic" + END;
    public static final String NPE = START + "CallStaticJava" + MID + "null_check" + END;
    public static final String STORE_INLINE_FIELDS = START + "CallStaticJava" + MID + "store_inline_type_fields" + END;
    public static final String SCOBJ = "(.*# ScObj.*" + END;
    public static final String LOAD_UNKNOWN_INLINE = "(.*call_leaf,runtime  load_unknown_inline.*" + END;
    public static final String STORE_UNKNOWN_INLINE = "(.*call_leaf,runtime  store_unknown_inline.*" + END;
    public static final String INLINE_ARRAY_NULL_GUARD = "(.*call,static  wrapper for: uncommon_trap.*reason='null_check' action='none'.*" + END;
    public static final String INTRINSIC_SLOW_PATH = "(.*call,static  wrapper for: uncommon_trap.*reason='intrinsic_or_type_checked_inlining'.*" + END;
    public static final String CLONE_INTRINSIC_SLOW_PATH = "(.*call,static.*java.lang.Object::clone.*" + END;

    public static final String MEMBAR = START + "MemBar" + MID + END;
    public static final String CHECKCAST_ARRAY = "(cmp.*precise klass \\[(L|Q)compiler/valhalla/inlinetypes/MyValue.*" + END;
    public static final String CHECKCAST_ARRAYCOPY = "(.*call_leaf_nofp,runtime  checkcast_arraycopy.*" + END;
    public static final String JLONG_ARRAYCOPY = "(.*call_leaf_nofp,runtime  jlong_disjoint_arraycopy.*" + END;
    public static final String FIELD_ACCESS = "(.*Field: *" + END;
    public static final String SUBSTITUTABILITY_TEST = START + "CallStaticJava" + MID + "java.lang.invoke.ValueBootstrapMethods::isSubstitutable" + END;

    static List<String> mergeNodes(String[] nodes) {
        final List<String> mergedNodes = new ArrayList<>();
        for (int i = 0; i < nodes.length; i += 2) {
            String node = nodes[i];
            switch (node) {
                case ALLOC_OF -> mergeCompositeNodes(nodes, mergedNodes, i, node, ALLOC_OF_POSTFIX, "ALLOC_OF");
                case ALLOC_ARRAY_OF -> mergeCompositeNodes(nodes, mergedNodes, i, node, ALLOC_ARRAY_OF_POSTFIX, "ALLOC_ARRAY_OF");
                case STORE_OF_CLASS -> mergeCompositeNodes(nodes, mergedNodes, i, node, STORE_OF_CLASS_POSTFIX, "STORE_OF_CLASS");
                case STORE_OF_FIELD -> mergeCompositeNodes(nodes, mergedNodes, i, node, STORE_OF_FIELD_POSTFIX, "STORE_OF_FIELD");
                case LOAD_OF_CLASS -> mergeCompositeNodes(nodes, mergedNodes, i, node, LOAD_OF_CLASS_POSTFIX, "LOAD_OF_CLASS");
                case LOAD_OF_FIELD -> mergeCompositeNodes(nodes, mergedNodes, i, node, LOAD_OF_FIELD_POSTFIX, "LOAD_OF_FIELD");
                default -> {
                    i--; // No composite node, do not increment by 2.
                    mergedNodes.add(node);
                }
            }
        }
        return mergedNodes;
    }

    private static void mergeCompositeNodes(String[] nodes, List<String> mergedNodes, int i, String node, String postFix, String varName) {
        TestFormat.check(i + 1 < nodes.length, "Must provide class name at index " + (i + 1) + " right after " + varName);
        mergedNodes.add(node + Pattern.quote(nodes[i + 1]) + postFix);
    }
}

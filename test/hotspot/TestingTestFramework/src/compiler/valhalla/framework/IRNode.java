package compiler.valhalla.framework;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class IRNode {
    private static final String START = "(\\d+(\\s|\\t)(";
    private static final String MID = ".*)+(\\s|\\t)===.*";
    private static final String END = ")";

    // Generic allocation
    public static final String ALLOC_G  = "(.*call,static  wrapper for: _new_instance_Java" + END;
    public static final String ALLOCA_G = "(.*call,static  wrapper for: _new_array_Java" + END;

    public static final String ALLOC  = "(.*precise klass .*\\R(.*(movl|xorl|nop|spill).*\\R)*.*_new_instance_Java" + END;
    public static final String ALLOC_OF = "(.*precise klass .*";
    private static final String ALLOC_OF_POSTFIX =  ":.*\\R(.*(movl|xorl|nop|spill).*\\R)*.*_new_instance_Java" + END;

    public static final String STORE  = START + "Store(B|C|S|I|L|F|D|P|N)" + MID + END;
    public static final String STORE_OF_CLASS  = START + "Store(B|C|S|I|L|F|D|P|N)" + MID + "@.*";
    private static final String STORE_OF_CLASS_POSTFIX = "(\\+|:).*" + END;
    public static final String STORE_OF_FIELD  = START + "Store(B|C|S|I|L|F|D|P|N)" + MID + "@.*name=";
    private static final String STORE_OF_FIELD_POSTFIX = ",.*" + END;


    // Inline type allocation
    public static final String ALLOCA = "(.*precise klass \\[(L|Q)compiler/valhalla/inlinetypes/MyValue.*\\R(.*(movl|xorl|nop|spill).*\\R)*.*_new_array_Java" + END;
    public static final String LOAD   = START + "Load(B|S|I|L|F|D|P|N)" + MID + "@compiler/valhalla/inlinetypes/MyValue.*" + END;
    public static final String LOADK  = START + "LoadK" + MID + END;
    public static final String LOOP   = START + "Loop" + MID + "" + END;
    public static final String COUNTEDLOOP = START + "CountedLoop\\b" + MID + "" + END;
    public static final String COUNTEDLOOP_MAIN = START + "CountedLoop\\b" + MID + "main" + END;
    public static final String TRAP   = START + "CallStaticJava" + MID + "uncommon_trap.*(unstable_if|predicate)" + END;
    public static final String RETURN = START + "Return" + MID + "returns" + END;
    public static final String LINKTOSTATIC = START + "CallStaticJava" + MID + "linkToStatic" + END;
    public static final String NPE = START + "CallStaticJava" + MID + "null_check" + END;
    public static final String CALL = START + "CallStaticJava" + MID + END;
    public static final String STORE_INLINE_FIELDS = START + "CallStaticJava" + MID + "store_inline_type_fields" + END;
    public static final String SCOBJ = "(.*# ScObj.*" + END;
    public static final String LOAD_UNKNOWN_INLINE = "(.*call_leaf,runtime  load_unknown_inline.*" + END;
    public static final String STORE_UNKNOWN_INLINE = "(.*call_leaf,runtime  store_unknown_inline.*" + END;
    public static final String INLINE_ARRAY_NULL_GUARD = "(.*call,static  wrapper for: uncommon_trap.*reason='null_check' action='none'.*" + END;
    public static final String INTRINSIC_SLOW_PATH = "(.*call,static  wrapper for: uncommon_trap.*reason='intrinsic_or_type_checked_inlining'.*" + END;
    public static final String CLONE_INTRINSIC_SLOW_PATH = "(.*call,static.*java.lang.Object::clone.*" + END;
    public static final String CLASS_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*class_check" + END;
    public static final String NULL_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_check" + END;
    public static final String NULL_ASSERT_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_assert" + END;
    public static final String RANGE_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*range_check" + END;
    public static final String UNHANDLED_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*unhandled" + END;
    public static final String PREDICATE_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*predicate" + END;
    public static final String MEMBAR = START + "MemBar" + MID + END;
    public static final String CHECKCAST_ARRAY = "(cmp.*precise klass \\[(L|Q)compiler/valhalla/inlinetypes/MyValue.*" + END;
    public static final String CHECKCAST_ARRAYCOPY = "(.*call_leaf_nofp,runtime  checkcast_arraycopy.*" + END;
    public static final String JLONG_ARRAYCOPY = "(.*call_leaf_nofp,runtime  jlong_disjoint_arraycopy.*" + END;
    public static final String FIELD_ACCESS = "(.*Field: *" + END;
    public static final String SUBSTITUTABILITY_TEST = START + "CallStaticJava" + MID + "java.lang.invoke.ValueBootstrapMethods::isSubstitutable" + END;

    static List<String> mergeCompositeNodes(String[] nodes) {
        final List<String> mergedNodes = new ArrayList<>();
        for (int i = 0; i < nodes.length; i++) {
            String node = nodes[i];
            switch (node) {
                case ALLOC_OF -> {
                    if (i + 1 == nodes.length) {
                        throw new TestFormatException("Must provide class name at index " + (i + 1) + " right after ALLOC_OF");
                    }
                    mergedNodes.add(node + Pattern.quote(nodes[i + 1]) + ALLOC_OF_POSTFIX);
                    i++;
                }
                case STORE_OF_CLASS -> {
                    if (i + 1 == nodes.length) {
                        throw new TestFormatException("Must provide class name at index " + (i + 1) + " right after STORE_OF_CLASS");
                    }
                    mergedNodes.add(node + Pattern.quote(nodes[i + 1]) + STORE_OF_CLASS_POSTFIX);
                    i++;
                }
                case STORE_OF_FIELD -> {
                    if (i + 1 == nodes.length) {
                        throw new TestFormatException("Must provide field name at index " + (i + 1) + " right after STORE_OF_FIELD");
                    }
                    mergedNodes.add(node + Pattern.quote(nodes[i + 1]) + STORE_OF_FIELD_POSTFIX);
                    i++;
                }
                default -> {
                    mergedNodes.add(node);
                }
            }
        }
        return mergedNodes;
    }
}

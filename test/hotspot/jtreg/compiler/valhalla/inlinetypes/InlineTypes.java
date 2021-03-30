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

package compiler.valhalla.inlinetypes;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.hotspot.ir_framework.*;

// This class should not be moved.
// TestGetfieldChains has hard codded line numbers for NamedRectangle methods
class NamedRectangle {
    Rectangle rect = new Rectangle();
    String name = "";

    static int getP1X(NamedRectangle nr) {
        return nr.rect
                .p1
                .x;
    }

    static Point getP1(NamedRectangle nr) {
        return nr.rect
                .p1;
    }
}

public class InlineTypes {
    public static final int  rI = Utils.getRandomInstance().nextInt() % 1000;
    public static final long rL = Utils.getRandomInstance().nextLong() % 1000;
    public static final double rD = Utils.getRandomInstance().nextDouble() % 1000;

    public static final Scenario[] DEFAULT_SCENARIOS = {
            new Scenario(0,
                         "-XX:-UseACmpProfile",
                         "-XX:+AlwaysIncrementalInline",
                         "-XX:FlatArrayElementMaxOops=5",
                         "-XX:FlatArrayElementMaxSize=-1",
                         "-XX:-UseArrayLoadStoreProfile",
                         "-XX:InlineFieldMaxFlatSize=-1",
                         "-XX:+InlineTypePassFieldsAsArgs",
                         "-XX:+InlineTypeReturnedAsFields"
            ),
            new Scenario(1,
                         "-XX:-UseACmpProfile",
                         "-XX:-UseCompressedOops",
                         "-XX:FlatArrayElementMaxOops=5",
                         "-XX:FlatArrayElementMaxSize=-1",
                         "-XX:-UseArrayLoadStoreProfile",
                         "-XX:InlineFieldMaxFlatSize=-1",
                         "-XX:-InlineTypePassFieldsAsArgs",
                         "-XX:-InlineTypeReturnedAsFields"
            ),
            new Scenario(2,
                         "-XX:-UseACmpProfile",
                         "-XX:-UseCompressedOops",
                         "-XX:FlatArrayElementMaxOops=0",
                         "-XX:FlatArrayElementMaxSize=0",
                         "-XX:-UseArrayLoadStoreProfile",
                         "-XX:InlineFieldMaxFlatSize=-1",
                         "-XX:+InlineTypePassFieldsAsArgs",
                         "-XX:+InlineTypeReturnedAsFields",
                         "-XX:+StressInlineTypeReturnedAsFields"
            ),
            new Scenario(3,
                         "-DVerifyIR=false",
                         "-XX:+AlwaysIncrementalInline",
                         "-XX:FlatArrayElementMaxOops=0",
                         "-XX:FlatArrayElementMaxSize=0",
                         "-XX:InlineFieldMaxFlatSize=0",
                         "-XX:+InlineTypePassFieldsAsArgs",
                         "-XX:+InlineTypeReturnedAsFields"
            ),
            new Scenario(4,
                         "-DVerifyIR=false",
                         "-XX:FlatArrayElementMaxOops=-1",
                         "-XX:FlatArrayElementMaxSize=-1",
                         "-XX:InlineFieldMaxFlatSize=0",
                         "-XX:+InlineTypePassFieldsAsArgs",
                         "-XX:-InlineTypeReturnedAsFields",
                         "-XX:-ReduceInitialCardMarks"
            ),
            new Scenario(5,
                         "-XX:-UseACmpProfile",
                         "-XX:+AlwaysIncrementalInline",
                         "-XX:FlatArrayElementMaxOops=5",
                         "-XX:FlatArrayElementMaxSize=-1",
                         "-XX:-UseArrayLoadStoreProfile",
                         "-XX:InlineFieldMaxFlatSize=-1",
                         "-XX:-InlineTypePassFieldsAsArgs",
                         "-XX:-InlineTypeReturnedAsFields"
            )
    };

    public static TestFramework getFramework() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        return new TestFramework(walker.getCallerClass()).setDefaultWarmup(251);
    }

    static class IRNode {
        // Regular expressions used to match nodes in the PrintIdeal output
        protected static final String START = "(\\d+ (.*";
        protected static final String MID = ".*)+ ===.*";
        protected static final String END = ")";
        // Generic allocation
        protected static final String ALLOC_G  = "(.*call,static  wrapper for: _new_instance_Java" + END;
        protected static final String ALLOCA_G = "(.*call,static  wrapper for: _new_array_Java" + END;
        // Inline type allocation
        protected static final String ALLOC  = "(.*precise klass compiler/valhalla/inlinetypes/MyValue.*\\R(.*(movl|xorl|nop|spill).*\\R)*.*_new_instance_Java" + END;
        protected static final String ALLOCA = "(.*precise klass \\[(L|Q)compiler/valhalla/inlinetypes/MyValue.*\\R(.*(movl|xorl|nop|spill).*\\R)*.*_new_array_Java" + END;
        protected static final String LOAD   = START + "Load(B|S|I|L|F|D|P|N)" + MID + "@compiler/valhalla/inlinetypes/MyValue.*" + END;
        protected static final String LOADK  = START + "LoadK" + MID + END;
        protected static final String STORE  = START + "Store(B|C|S|I|L|F|D|P|N)" + MID + "@compiler/valhalla/inlinetypes/MyValue.*" + END;
        protected static final String LOOP   = START + "Loop" + MID + "" + END;
        protected static final String COUNTEDLOOP = START + "CountedLoop\\b" + MID + "" + END;
        protected static final String COUNTEDLOOP_MAIN = START + "CountedLoop\\b" + MID + "main" + END;
        protected static final String TRAP   = START + "CallStaticJava" + MID + "uncommon_trap.*(unstable_if|predicate)" + END;
        protected static final String LINKTOSTATIC = START + "CallStaticJava" + MID + "linkToStatic" + END;
        protected static final String NPE = START + "CallStaticJava" + MID + "null_check" + END;
        protected static final String CALL = START + "CallStaticJava" + MID + END;
        protected static final String STORE_INLINE_FIELDS = START + "CallStaticJava" + MID + "store_inline_type_fields" + END;
        protected static final String SCOBJ = "(.*# ScObj.*" + END;
        protected static final String LOAD_UNKNOWN_INLINE = "(.*call_leaf,runtime  load_unknown_inline.*" + END;
        protected static final String STORE_UNKNOWN_INLINE = "(.*call_leaf,runtime  store_unknown_inline.*" + END;
        protected static final String INLINE_ARRAY_NULL_GUARD = "(.*call,static  wrapper for: uncommon_trap.*reason='null_check' action='none'.*" + END;
        protected static final String INTRINSIC_SLOW_PATH = "(.*call,static  wrapper for: uncommon_trap.*reason='intrinsic_or_type_checked_inlining'.*" + END;
        protected static final String CLONE_INTRINSIC_SLOW_PATH = "(.*call,static.*java.lang.Object::clone.*" + END;
        protected static final String CLASS_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*class_check" + END;
        protected static final String NULL_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_check" + END;
        protected static final String NULL_ASSERT_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*null_assert" + END;
        protected static final String RANGE_CHECK_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*range_check" + END;
        protected static final String UNHANDLED_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*unhandled" + END;
        protected static final String PREDICATE_TRAP = START + "CallStaticJava" + MID + "uncommon_trap.*predicate" + END;
        protected static final String MEMBAR = START + "MemBar" + MID + END;
        protected static final String CHECKCAST_ARRAY = "(cmp.*precise klass \\[(L|Q)compiler/valhalla/inlinetypes/MyValue.*" + END;
        protected static final String CHECKCAST_ARRAYCOPY = "(.*call_leaf_nofp,runtime  checkcast_arraycopy.*" + END;
        protected static final String JLONG_ARRAYCOPY = "(.*call_leaf_nofp,runtime  jlong_disjoint_arraycopy.*" + END;
        protected static final String FIELD_ACCESS = "(.*Field: *" + END;
        protected static final String SUBSTITUTABILITY_TEST = START + "CallStaticJava" + MID + "java.lang.invoke.ValueBootstrapMethods::isSubstitutable" + END;
    }

}

interface MyInterface {
    public long hash();
}

abstract class MyAbstract implements MyInterface {

}

final primitive class MyValueEmpty extends MyAbstract {
    public long hash() { return 0; }

    public MyValueEmpty copy(MyValueEmpty other) { return other; }
}

primitive class Point {
    int x = 4;
    int y = 7;
}

primitive class Rectangle {
    Point p0 = new Point();
    Point p1 = new Point();
}

primitive class SimpleInlineType {
    final int x;

    private SimpleInlineType() {
        x = 0;
    }

    static SimpleInlineType create() {
        return SimpleInlineType.default;
    }
}

@ForceCompileClassInitializer
final primitive class MyValue1 extends MyAbstract {
    static int s;
    static final long sf = InlineTypes.rL;
    final int x;
    final long y;
    final short z;
    final Integer o;
    final int[] oa;
    final MyValue2 v1;
    final MyValue2 v2;
    static final MyValue2 v3 = MyValue2.createWithFieldsInline(InlineTypes.rI, InlineTypes.rD);
    final int c;

    @ForceInline
    public MyValue1(int x, long y, short z, Integer o, int[] oa, MyValue2 v1, MyValue2 v2, int c) {
        s = 0;
        this.x = x;
        this.y = y;
        this.z = z;
        this.o = o;
        this.oa = oa;
        this.v1 = v1;
        this.v2 = v2;
        this.c = c;
    }

    @DontInline
    static MyValue1 createDefaultDontInline() {
        return createDefaultInline();
    }

    @ForceInline
    static MyValue1 createDefaultInline() {
        return MyValue1.default;
    }

    @DontInline
    static MyValue1 createWithFieldsDontInline(int x, long y) {
        return createWithFieldsInline(x, y);
    }

    @ForceInline
    static MyValue1 createWithFieldsInline(int x, long y) {
        MyValue1 v = createDefaultInline();
        v = setX(v, x);
        v = setY(v, y);
        v = setZ(v, (short)x);
        // Don't use Integer.valueOf here to avoid control flow added by Integer cache check
        v = setO(v, new Integer(x));
        int[] oa = {x};
        v = setOA(v, oa);
        v = setV1(v, MyValue2.createWithFieldsInline(x, y, InlineTypes.rD));
        v = setV2(v, MyValue2.createWithFieldsInline(x, y, InlineTypes.rD + x));
        v = setC(v, (int)(x+y));
        return v;
    }

    // Hash only primitive and inline type fields to avoid NullPointerException
    @ForceInline
    public long hashPrimitive() {
        return s + sf + x + y + z + c + v1.hash() + v2.hash() + v3.hash();
    }

    @ForceInline
    public long hash() {
        long res = hashPrimitive();
        try {
            res += o;
        } catch(NullPointerException npe) {}
        try {
            res += oa[0];
        } catch(NullPointerException npe) {}
        return res;
    }

    @DontCompile
    public long hashInterpreted() {
        return s + sf + x + y + z + o + oa[0] + c + v1.hashInterpreted() + v2.hashInterpreted() + v3.hashInterpreted();
    }

    @ForceInline
    public void print() {
        System.out.print("s=" + s + ", sf=" + sf + ", x=" + x + ", y=" + y + ", z=" + z + ", o=" + (o != null ? (Integer)o : "NULL") + ", oa=" + (oa != null ? oa[0] : "NULL") + ", v1[");
        v1.print();
        System.out.print("], v2[");
        v2.print();
        System.out.print("], v3[");
        v3.print();
        System.out.print("], c=" + c);
    }

    @ForceInline
    static MyValue1 setX(MyValue1 v, int x) {
        return new MyValue1(x, v.y, v.z, v.o, v.oa, v.v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setY(MyValue1 v, long y) {
        return new MyValue1(v.x, y, v.z, v.o, v.oa, v.v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setZ(MyValue1 v, short z) {
        return new MyValue1(v.x, v.y, z, v.o, v.oa, v.v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setO(MyValue1 v, Integer o) {
        return new MyValue1(v.x, v.y, v.z, o, v.oa, v.v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setOA(MyValue1 v, int[] oa) {
        return new MyValue1(v.x, v.y, v.z, v.o, oa, v.v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setC(MyValue1 v, int c) {
        return new MyValue1(v.x, v.y, v.z, v.o, v.oa, v.v1, v.v2, c);
    }

    @ForceInline
    static MyValue1 setV1(MyValue1 v, MyValue2 v1) {
        return new MyValue1(v.x, v.y, v.z, v.o, v.oa, v1, v.v2, v.c);
    }

    @ForceInline
    static MyValue1 setV2(MyValue1 v, MyValue2 v2) {
        return new MyValue1(v.x, v.y, v.z, v.o, v.oa, v.v1, v2, v.c);
    }
}

final primitive class MyValue2Inline {
    final double d;
    final long l;

    @ForceInline
    public MyValue2Inline(double d, long l) {
        this.d = d;
        this.l = l;
    }

    @ForceInline
    static MyValue2Inline setD(MyValue2Inline v, double d) {
        return new MyValue2Inline(d, v.l);
    }

    @ForceInline
    static MyValue2Inline setL(MyValue2Inline v, long l) {
        return new MyValue2Inline(v.d, l);
    }

    @ForceInline
    public static MyValue2Inline createDefault() {
        return MyValue2Inline.default;
    }

    @ForceInline
    public static MyValue2Inline createWithFieldsInline(double d, long l) {
        MyValue2Inline v = MyValue2Inline.createDefault();
        v = MyValue2Inline.setD(v, d);
        v = MyValue2Inline.setL(v, l);
        return v;
    }
}

final primitive class MyValue2 extends MyAbstract {
    final int x;
    final byte y;
    final MyValue2Inline v;

    @ForceInline
    public MyValue2(int x, byte y, MyValue2Inline v) {
        this.x = x;
        this.y = y;
        this.v = v;
    }

    @ForceInline
    public static MyValue2 createDefaultInline() {
        return MyValue2.default;
    }

    @ForceInline
    public static MyValue2 createWithFieldsInline(int x, long y, double d) {
        MyValue2 v = createDefaultInline();
        v = setX(v, x);
        v = setY(v, (byte)x);
        v = setV(v, MyValue2Inline.createWithFieldsInline(d, y));
        return v;
    }

    @ForceInline
    public static MyValue2 createWithFieldsInline(int x, double d) {
        MyValue2 v = createDefaultInline();
        v = setX(v, x);
        v = setY(v, (byte)x);
        v = setV(v, MyValue2Inline.createWithFieldsInline(d, InlineTypes.rL));
        return v;
    }

    @DontInline
    public static MyValue2 createWithFieldsDontInline(int x, double d) {
        MyValue2 v = createDefaultInline();
        v = setX(v, x);
        v = setY(v, (byte)x);
        v = setV(v, MyValue2Inline.createWithFieldsInline(d, InlineTypes.rL));
        return v;
    }

    @ForceInline
    public long hash() {
        return x + y + (long)v.d + v.l;
    }

    @DontInline
    public long hashInterpreted() {
        return x + y + (long)v.d + v.l;
    }

    @ForceInline
    public void print() {
        System.out.print("x=" + x + ", y=" + y + ", d=" + v.d + ", l=" + v.l);
    }

    @ForceInline
    static MyValue2 setX(MyValue2 v, int x) {
        return new MyValue2(x, v.y, v.v);
    }

    @ForceInline
    static MyValue2 setY(MyValue2 v, byte y) {
        return new MyValue2(v.x, y, v.v);
    }

    @ForceInline
    static MyValue2 setV(MyValue2 v, MyValue2Inline vi) {
        return new MyValue2(v.x, v.y, vi);
    }
}

final primitive class MyValue3Inline {
    final float f7;
    final double f8;

    @ForceInline
    public MyValue3Inline(float f7, double f8) {
        this.f7 = f7;
        this.f8 = f8;
    }

    @ForceInline
    static MyValue3Inline setF7(MyValue3Inline v, float f7) {
        return new MyValue3Inline(f7, v.f8);
    }

    @ForceInline
    static MyValue3Inline setF8(MyValue3Inline v, double f8) {
        return new MyValue3Inline(v.f7, f8);
    }

    @ForceInline
    public static MyValue3Inline createDefault() {
        return MyValue3Inline.default;
    }

    @ForceInline
    public static MyValue3Inline createWithFieldsInline(float f7, double f8) {
        MyValue3Inline v = createDefault();
        v = setF7(v, f7);
        v = setF8(v, f8);
        return v;
    }
}

// Inline type definition to stress test return of an inline type in registers
// (uses all registers of calling convention on x86_64)
final primitive class MyValue3 extends MyAbstract {
    final char c;
    final byte bb;
    final short s;
    final int i;
    final long l;
    final Object o;
    final float f1;
    final double f2;
    final float f3;
    final double f4;
    final float f5;
    final double f6;
    final MyValue3Inline v1;

    @ForceInline
    public MyValue3(char c, byte bb, short s, int i, long l, Object o,
                    float f1, double f2, float f3, double f4, float f5, double f6,
                    MyValue3Inline v1) {
        this.c = c;
        this.bb = bb;
        this.s = s;
        this.i = i;
        this.l = l;
        this.o = o;
        this.f1 = f1;
        this.f2 = f2;
        this.f3 = f3;
        this.f4 = f4;
        this.f5 = f5;
        this.f6 = f6;
        this.v1 = v1;
    }

    @ForceInline
    static MyValue3 setC(MyValue3 v, char c) {
        return new MyValue3(c, v.bb, v.s, v.i, v.l, v.o, v.f1, v.f2, v.f3, v.f4, v.f5, v.f6, v.v1);
    }

    @ForceInline
    static MyValue3 setBB(MyValue3 v, byte bb) {
        return new MyValue3(v.c, bb, v.s, v.i, v.l, v.o, v.f1, v.f2, v.f3, v.f4, v.f5, v.f6, v.v1);
    }

    @ForceInline
    static MyValue3 setS(MyValue3 v, short s) {
        return new MyValue3(v.c, v.bb, s, v.i, v.l, v.o, v.f1, v.f2, v.f3, v.f4, v.f5, v.f6, v.v1);
    }

    @ForceInline
    static MyValue3 setI(MyValue3 v, int i) {
        return new MyValue3(v.c, v.bb, v.s, i, v.l, v.o, v.f1, v.f2, v.f3, v.f4, v.f5, v.f6, v.v1);
    }

    @ForceInline
    static MyValue3 setL(MyValue3 v, long l) {
        return new MyValue3(v.c, v.bb, v.s, v.i, l, v.o, v.f1, v.f2, v.f3, v.f4, v.f5, v.f6, v.v1);
    }

    @ForceInline
    static MyValue3 setO(MyValue3 v, Object o) {
        return new MyValue3(v.c, v.bb, v.s, v.i, v.l, o, v.f1, v.f2, v.f3, v.f4, v.f5, v.f6, v.v1);
    }

    @ForceInline
    static MyValue3 setF1(MyValue3 v, float f1) {
        return new MyValue3(v.c, v.bb, v.s, v.i, v.l, v.o, f1, v.f2, v.f3, v.f4, v.f5, v.f6, v.v1);
    }

    @ForceInline
    static MyValue3 setF2(MyValue3 v, double f2) {
        return new MyValue3(v.c, v.bb, v.s, v.i, v.l, v.o, v.f1, f2, v.f3, v.f4, v.f5, v.f6, v.v1);
    }

    @ForceInline
    static MyValue3 setF3(MyValue3 v, float f3) {
        return new MyValue3(v.c, v.bb, v.s, v.i, v.l, v.o, v.f1, v.f2, f3, v.f4, v.f5, v.f6, v.v1);
    }

    @ForceInline
    static MyValue3 setF4(MyValue3 v, double f4) {
        return new MyValue3(v.c, v.bb, v.s, v.i, v.l, v.o, v.f1, v.f2, v.f3, f4, v.f5, v.f6, v.v1);
    }

    @ForceInline
    static MyValue3 setF5(MyValue3 v, float f5) {
        return new MyValue3(v.c, v.bb, v.s, v.i, v.l, v.o, v.f1, v.f2, v.f3, v.f4, f5, v.f6, v.v1);
    }

    @ForceInline
    static MyValue3 setF6(MyValue3 v, double f6) {
        return new MyValue3(v.c, v.bb, v.s, v.i, v.l, v.o, v.f1, v.f2, v.f3, v.f4, v.f5, f6, v.v1);
    }

    @ForceInline
    static MyValue3 setV1(MyValue3 v, MyValue3Inline v1) {
        return new MyValue3(v.c, v.bb, v.s, v.i, v.l, v.o, v.f1, v.f2, v.f3, v.f4, v.f5, v.f6, v1);
    }

    @ForceInline
    public static MyValue3 createDefault() {
        return MyValue3.default;
    }

    @ForceInline
    public static MyValue3 create() {
        java.util.Random r = Utils.getRandomInstance();
        MyValue3 v = createDefault();
        v = setC(v, (char)r.nextInt());
        v = setBB(v, (byte)r.nextInt());
        v = setS(v, (short)r.nextInt());
        v = setI(v, r.nextInt());
        v = setL(v, r.nextLong());
        v = setO(v, new Object());
        v = setF1(v, r.nextFloat());
        v = setF2(v, r.nextDouble());
        v = setF3(v, r.nextFloat());
        v = setF4(v, r.nextDouble());
        v = setF5(v, r.nextFloat());
        v = setF6(v, r.nextDouble());
        v = setV1(v, MyValue3Inline.createWithFieldsInline(r.nextFloat(), r.nextDouble()));
        return v;
    }

    @DontInline
    public static MyValue3 createDontInline() {
        return create();
    }

    @ForceInline
    public static MyValue3 copy(MyValue3 other) {
        MyValue3 v = createDefault();
        v = setC(v, other.c);
        v = setBB(v, other.bb);
        v = setS(v, other.s);
        v = setI(v, other.i);
        v = setL(v, other.l);
        v = setO(v, other.o);
        v = setF1(v, other.f1);
        v = setF2(v, other.f2);
        v = setF3(v, other.f3);
        v = setF4(v, other.f4);
        v = setF5(v, other.f5);
        v = setF6(v, other.f6);
        v = setV1(v, other.v1);
        return v;
    }

    @DontInline
    public void verify(MyValue3 other) {
        Asserts.assertEQ(c, other.c);
        Asserts.assertEQ(bb, other.bb);
        Asserts.assertEQ(s, other.s);
        Asserts.assertEQ(i, other.i);
        Asserts.assertEQ(l, other.l);
        Asserts.assertEQ(o, other.o);
        Asserts.assertEQ(f1, other.f1);
        Asserts.assertEQ(f2, other.f2);
        Asserts.assertEQ(f3, other.f3);
        Asserts.assertEQ(f4, other.f4);
        Asserts.assertEQ(f5, other.f5);
        Asserts.assertEQ(f6, other.f6);
        Asserts.assertEQ(v1.f7, other.v1.f7);
        Asserts.assertEQ(v1.f8, other.v1.f8);
    }

    @ForceInline
    public long hash() {
        return c +
               bb +
               s +
               i +
               l +
               o.hashCode() +
               Float.hashCode(f1) +
               Double.hashCode(f2) +
               Float.hashCode(f3) +
               Double.hashCode(f4) +
               Float.hashCode(f5) +
               Double.hashCode(f6) +
               Float.hashCode(v1.f7) +
               Double.hashCode(v1.f8);
    }
}

// Inline type definition with too many fields to return in registers
final primitive class MyValue4 extends MyAbstract {
    final MyValue3 v1;
    final MyValue3 v2;

    @ForceInline
    public MyValue4(MyValue3 v1, MyValue3 v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    @ForceInline
    static MyValue4 setV1(MyValue4 v, MyValue3 v1) {
        return new MyValue4(v1, v.v2);
    }

    @ForceInline
    static MyValue4 setV2(MyValue4 v, MyValue3 v2) {
        return new MyValue4(v.v1, v2);
    }

    @ForceInline
    public static MyValue4 createDefault() {
        return MyValue4.default;
    }

    @ForceInline
    public static MyValue4 create() {
        MyValue4 v = createDefault();
        MyValue3 v1 = MyValue3.create();
        v = setV1(v, v1);
        MyValue3 v2 = MyValue3.create();
        v = setV2(v, v2);
        return v;
    }

    public void verify(MyValue4 other) {
        v1.verify(other.v1);
        v2.verify(other.v2);
    }

    @ForceInline
    public long hash() {
        return v1.hash() + v2.hash();
    }
}



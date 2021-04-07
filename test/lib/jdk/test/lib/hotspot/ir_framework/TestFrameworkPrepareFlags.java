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

import jdk.test.lib.Platform;
import jdk.test.lib.management.InputArguments;
import sun.hotspot.WhiteBox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Predicate;

/**
 * This class' main method is called from {@link TestFramework} and represents the so-called "flag VM". It uses the
 * Whitebox API to determine the necessary additional flags to run the test VM (e.g. to do IR matching). It returns
 * the flags over the dedicated TestFramework socket.
 */
class TestFrameworkPrepareFlags {
    private static final WhiteBox WHITE_BOX;

    static {
        try {
            WHITE_BOX = WhiteBox.getWhiteBox();
        } catch (UnsatisfiedLinkError e) {
            TestFramework.fail("Could not load WhiteBox", e);
            throw e; // Not reached
        }
    }

    private static final boolean TIERED_COMPILATION = (Boolean)WHITE_BOX.getVMFlag("TieredCompilation");
    private static final CompLevel TIERED_COMPILATION_STOP_AT_LEVEL = CompLevel.forValue(((Long)WHITE_BOX.getVMFlag("TieredStopAtLevel")).intValue());
    static final boolean TEST_C1 = TIERED_COMPILATION && TIERED_COMPILATION_STOP_AT_LEVEL.getValue() < CompLevel.C2.getValue();

    // User defined settings
    static final boolean XCOMP = Platform.isComp();
    static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("Verbose", "false"));

    static final boolean USE_COMPILER = WHITE_BOX.getBooleanVMFlag("UseCompiler");
    static final boolean STRESS_CC = Boolean.parseBoolean(System.getProperty("StressCC", "false"));
    private static final boolean REQUESTED_VERIFY_IR = Boolean.parseBoolean(System.getProperty("VerifyIR", "true"));
    private static boolean VERIFY_IR = REQUESTED_VERIFY_IR && USE_COMPILER && !XCOMP && !STRESS_CC && !TEST_C1
                                       && Platform.isDebugBuild() && !Platform.isInt();
    private static final boolean VERIFY_VM = Boolean.parseBoolean(System.getProperty("VerifyVM", "false")) && Platform.isDebugBuild();

    private static String[] getDefaultFlags() {
        return new String[] {"-XX:-BackgroundCompilation", "-XX:CompileCommand=quiet"};
    }

    private static String[] getPrintFlags() {
        return new String[] {"-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions"};
    }

    private static String[] getVerifyFlags() {
        return new String[] {
                "-XX:+UnlockDiagnosticVMOptions", "-XX:+VerifyOops", "-XX:+VerifyStack", "-XX:+VerifyLastFrame", "-XX:+VerifyBeforeGC",
                "-XX:+VerifyAfterGC", "-XX:+VerifyDuringGC", "-XX:+VerifyAdapterSharing"};
    }

    public static void main(String[] args) {
        String testClassName = args[0];
        if (VERBOSE) {
            System.out.println("TestFrameworkPrepareFlags main() called. Prepare test VM flags to run class " + testClassName);
        }
        Class<?> testClass;
        try {
            testClass = Class.forName(testClassName);
        } catch (Exception e) {
            throw new TestRunException("Could not find test class " + testClassName, e);
        }
        emitTestVMFlags(prepareTestVmFlags(testClass));
    }

    /**
     * Emit test VM flags to standard output to parse them from the TestFramework "driver" VM again which adds them to the test VM.
     */
    private static void emitTestVMFlags(ArrayList<String> flags) {
        String encoding = TestFramework.TEST_VM_FLAGS_START + "\n" + String.join(TestFramework.TEST_VM_FLAGS_DELIMITER, flags)
                      + "\n" + TestFramework.TEST_VM_FLAGS_END;
        TestFrameworkSocket.write(encoding, "flag encoding");
    }

    private static ArrayList<String> prepareTestVmFlags(Class<?> testClass) {
        ArrayList<String> cmds = new ArrayList<>();

        if (VERIFY_VM) {
            cmds.addAll(Arrays.asList(getVerifyFlags()));
        }

        cmds.addAll(Arrays.asList(getDefaultFlags()));
        setupIrVerificationFlags(testClass, cmds);

//        // TODO: Only for debugging
//        if (cmds.get(0).startsWith("-agentlib")) {
//            cmds.set(0, "-agentlib:jdwp=transport=dt_socket,address=127.0.0.1:44444,suspend=y,server=y");
//        }


        return cmds;
    }

    private static void setupIrVerificationFlags(Class<?> testClass, ArrayList<String> cmds) {
        Predicate<String> matchCompileThreshold = flag -> flag.startsWith("-XX:CompileThreshold");
        if (VERIFY_IR && (cmds.stream().anyMatch(matchCompileThreshold)
                          || Arrays.stream(InputArguments.getVmInputArgs()).anyMatch(matchCompileThreshold))) {
            // Disable IR verification if non-default CompileThreshold is set
            if (VERBOSE) {
                System.out.println("Disabled IR verification due to CompileThreshold flag");
            }
            VERIFY_IR = false;
        } else if (!VERIFY_IR && REQUESTED_VERIFY_IR) {
            System.out.println("IR Verification disabled either due to not running a debug build, running with -Xint, or other " +
                               "VM flags that make the verification inaccurate or impossible (e.g. running with C1 only).");
        }

        if (VERIFY_IR) {
            // Add print flags for IR verification
            cmds.addAll(Arrays.asList(getPrintFlags()));
            cmds.add("-XX:+LogCompilation");
            cmds.add("-XX:CompileCommand=log," + testClass.getCanonicalName() + "::*");
            addBoolOptionForClass(cmds, testClass, "PrintIdeal");
            addBoolOptionForClass(cmds, testClass, "PrintOptoAssembly");
            // Always trap for exception throwing to not confuse IR verification
            cmds.add("-XX:-OmitStackTraceInFastThrow");
            cmds.add("-DShouldDoIRVerification=true");
        } else {
            cmds.add("-DShouldDoIRVerification=false");
        }
    }

    private static void addBoolOptionForClass(ArrayList<String> cmds, Class<?> testClass, String option) {
        cmds.add("-XX:CompileCommand=option," + testClass.getCanonicalName() + "::*,bool," + option + ",true");
    }
}

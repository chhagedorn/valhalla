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

import jdk.test.lib.Platform;
import jdk.test.lib.management.InputArguments;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class TestFrameworkRunner {
    private static final WhiteBox WHITE_BOX;

    static {
        try {
            WHITE_BOX = WhiteBox.getWhiteBox();
        } catch (UnsatisfiedLinkError e) {
            throw new TestFrameworkException("Could not load WhiteBox");
        }
    }

    private static final boolean TIERED_COMPILATION = (Boolean)WHITE_BOX.getVMFlag("TieredCompilation");
    private static final CompLevel TIERED_COMPILATION_STOP_AT_LEVEL = CompLevel.forValue(((Long)WHITE_BOX.getVMFlag("TieredStopAtLevel")).intValue());
    static final boolean TEST_C1 = TIERED_COMPILATION && TIERED_COMPILATION_STOP_AT_LEVEL.getValue() < CompLevel.C2.getValue();

    // User defined settings
    static final boolean XCOMP = Platform.isComp();
//    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("Verbose", "false"));
    static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("Verbose", "true"));

    private static final boolean COMPILE_COMMANDS = Boolean.parseBoolean(System.getProperty("CompileCommands", "true")) && !XCOMP;
    static final boolean USE_COMPILER = WHITE_BOX.getBooleanVMFlag("UseCompiler");
    static final boolean STRESS_CC = Boolean.parseBoolean(System.getProperty("StressCC", "false"));
    private static final boolean REQUESTED_VERIFY_IR = Boolean.parseBoolean(System.getProperty("VerifyIR", "true"));
    private static boolean VERIFY_IR = REQUESTED_VERIFY_IR && USE_COMPILER && !XCOMP && !STRESS_CC && !TEST_C1 && COMPILE_COMMANDS
                                       && Platform.isDebugBuild() && !Platform.isInt();
    private static final boolean VERIFY_VM = Boolean.parseBoolean(System.getProperty("VerifyVM", "false")) && Platform.isDebugBuild();
    private static final boolean PREFER_COMMAND_LINE_FLAGS = Boolean.parseBoolean(System.getProperty("PreferCommandLineFlags", "false"));

    private static String[] getDefaultFlags() {
        return new String[] {"-XX:-BackgroundCompilation"};
    }

    private static String[] getCompileCommandFlags() {
        return new String[] {"-XX:CompileCommand=quiet"};
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
        System.out.println("Framework main(), about to run tests in class " + testClassName);
        Class<?> testClass;
        try {
            testClass = Class.forName(testClassName);
        } catch (Exception e) {
            throw new TestRunException("Could not find test class " + testClassName, e);
        }
        runTestVM(testClass, args);
    }

    private static void runTestVM(Class<?> testClass, String[] args) {
        ArrayList<String> cmds = prepareTestVmFlags(testClass, args);
        OutputAnalyzer oa;
        try {
            // Calls 'main' of this class to run all specified tests with commands 'cmds'.
            oa = ProcessTools.executeTestJvm(cmds);
        } catch (Exception e) {
            throw new TestFrameworkException("Error while executing Test VM", e);
        }
        String output = oa.getOutput();
        final int exitCode = oa.getExitValue();
        if (VERBOSE || exitCode != 0) {
            System.out.println(" ----- OUTPUT -----");
            System.out.println(output);

        }
        if (exitCode != 0) {
            throw new RuntimeException("\nTestFramework runner VM exited with " + exitCode);
        }

        if (VERIFY_IR) {
            IRMatcher irMatcher = new IRMatcher(output, testClass);
            irMatcher.applyRules();
        }
    }

    private static ArrayList<String> prepareTestVmFlags(Class<?> testClass, String[] args) {
        String[] vmInputArguments = InputArguments.getVmInputArgs();
        ArrayList<String> cmds = new ArrayList<>();
        if (!PREFER_COMMAND_LINE_FLAGS) {
            cmds.addAll(Arrays.asList(vmInputArguments));
        }

        setupIrVerificationFlags(testClass, cmds);

        if (VERIFY_VM) {
            cmds.addAll(Arrays.asList(getVerifyFlags()));
        }

        cmds.addAll(Arrays.asList(getDefaultFlags()));
        if (COMPILE_COMMANDS) {
            cmds.addAll(Arrays.asList(getCompileCommandFlags()));
        }

        // TODO: Only for debugging
        if (cmds.get(0).startsWith("-agentlib")) {
            cmds.set(0, "-agentlib:jdwp=transport=dt_socket,address=127.0.0.1:44444,suspend=n,server=y");
        }

        if (PREFER_COMMAND_LINE_FLAGS) {
            // Prefer flags set via the command line over the ones set by scenarios.
            cmds.addAll(Arrays.asList(vmInputArguments));
        }

        cmds.add(TestFrameworkExecution.class.getCanonicalName());
        cmds.addAll(Arrays.asList(args)); // add test class and helpers last
        return cmds;
    }

    private static void setupIrVerificationFlags(Class<?> testClass, ArrayList<String> cmds) {
        if (VERIFY_IR && cmds.stream().anyMatch(flag -> flag.startsWith("-XX:CompileThreshold"))) {
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
            addBoolOptionForClass(cmds, testClass, "PrintIdeal");
            addBoolOptionForClass(cmds, testClass, "PrintOptoAssembly");
            // Always trap for exception throwing to not confuse IR verification
            cmds.add("-XX:-OmitStackTraceInFastThrow");
            cmds.add("-DPrintValidIRRules=true");
        } else {
            cmds.add("-DPrintValidIRRules=false");
        }
    }

    private static void addBoolOptionForClass(ArrayList<String> cmds, Class<?> testClass, String option) {
        cmds.add("-XX:CompileCommand=option," + testClass.getCanonicalName() + "::*,bool," + option + ",true");
    }
}

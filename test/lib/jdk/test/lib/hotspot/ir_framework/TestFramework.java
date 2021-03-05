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

import jdk.test.lib.Utils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.ClassFileInstaller;
import sun.hotspot.WhiteBox;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Call the driver with jtreg:
 * @library /test/lib
 * @run driver TestFrameworkDriver some.package.Test
 *
 * package some.package;
 *
 * public class Test { ... }
 */
public class TestFramework {
    public static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("Verbose", "false"));

    static final String TEST_VM_FLAGS_START = "##### TestFrameworkPrepareFlags - used by TestFramework #####";
    static final String TEST_VM_FLAGS_DELIMITER = " ";
    static final String TEST_VM_FLAGS_END = "----- END -----";
    private static boolean VERIFY_IR = true; // Should we perform IR matching?
    private static final boolean PREFER_COMMAND_LINE_FLAGS = Boolean.parseBoolean(System.getProperty("PreferCommandLineFlags", "false"));

    private List<Class<?>> helperClasses = null;
    private List<Scenario> scenarios = null;
    private final Class<?> testClass;
    private static String lastVMOutput;

    public TestFramework(Class<?> testClass) {
        TestRun.check(testClass != null, "Test class cannot be null");
        this.testClass = testClass;
    }

    /*
     * Public interface methods
     */

    public static void run() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        run(walker.getCallerClass());
    }

    public static void run(Class<?> testClass) {
        TestFramework framework = new TestFramework(testClass);
        framework.start();
    }

    public static void runWithHelperClasses(Class<?> testClass, Class<?>... helperClasses) {
        TestFramework framework = new TestFramework(testClass);
        framework.addHelperClasses(helperClasses);
        framework.start();
    }

    public static void runWithScenarios(Scenario... scenarios) {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        runWithScenarios(walker.getCallerClass(), scenarios);
    }

    public static void runWithScenarios(Class<?> testClass, Scenario... scenarios) {
        TestFramework framework = new TestFramework(testClass);
        framework.addScenarios(scenarios);
        framework.start();
    }

    public TestFramework addHelperClasses(Class<?>... helperClasses) {
        TestRun.check(helperClasses != null && Arrays.stream(helperClasses).noneMatch(Objects::isNull), "A Helper class cannot be null");
        if (this.helperClasses == null) {
            this.helperClasses = new ArrayList<>();
        }

        for (Class<?> helperClass : helperClasses) {
            TestRun.check(!this.helperClasses.contains(helperClass), "Cannot add the same class twice: " + helperClass);
            this.helperClasses.add(helperClass);
        }
        return this;
    }

    public TestFramework addScenarios(Scenario... scenarios) {
        TestRun.check(scenarios != null && Arrays.stream(scenarios).noneMatch(Objects::isNull), "A scenario cannot be null");
        if (this.scenarios == null) {
            this.scenarios = new ArrayList<>(Arrays.asList(scenarios));
        } else {
            this.scenarios.addAll(Arrays.asList(scenarios));
        }
        return this;
    }

    public void clear() {
        clearHelperClasses();
        clearScenarios();
    }

    public void clearHelperClasses() {
        this.helperClasses = null;
    }

    public void clearScenarios() {
        this.scenarios = null;
    }

    public void start() {
        installWhiteBox();
        if (scenarios == null) {
            start(null);
        } else {
            startWithScenarios();
        }
    }

    public static String getLastVMOutput() {
        return lastVMOutput;
    }

    /**
     * The following methods can only be called from actual tests and not from the main() method of a test.
     * Calling these methods from main() results in a linking exception (Whitebox not yet loaded and enabled).
     */

    // Can be called from tests for non-@Test methods
    public static void compile(Method m, CompLevel compLevel) {
        TestFrameworkExecution.compile(m, compLevel);
    }

    public static boolean isC1Compiled(Method m) {
        return TestFrameworkExecution.isC1Compiled(m);
    }

    public static boolean isC2Compiled(Method m) {
        return TestFrameworkExecution.isC2Compiled(m);
    }

    public static boolean isCompiledAtLevel(Method m, CompLevel compLevel) {
        return TestFrameworkExecution.isCompiledAtLevel(m, compLevel);
    }

    public static void assertDeoptimizedByC1(Method m) {
        TestFrameworkExecution.assertDeoptimizedByC1(m);
    }

    public static void assertDeoptimizedByC2(Method m) {
        TestFrameworkExecution.assertDeoptimizedByC2(m);
    }

    public static void assertCompiledByC1(Method m) {
        TestFrameworkExecution.assertCompiledByC1(m);
    }

    public static void assertCompiledByC2(Method m) {
        TestFrameworkExecution.assertCompiledByC2(m);
    }

    public static void assertCompiledAtLevel(Method m, CompLevel compLevel) {
        TestFrameworkExecution.assertCompiledAtLevel(m, compLevel);
    }

    public static void assertNotCompiled(Method m) {
        TestFrameworkExecution.assertNotCompiled(m);
    }

    public static void assertCompiled(Method m) {
        TestFrameworkExecution.assertCompiled(m);
    }

    /*
     * End of public interface methods
     */

    private void installWhiteBox() {
        try {
            ClassFileInstaller.main(WhiteBox.class.getName());
        } catch (Exception e) {
            throw new Error("failed to install whitebox classes", e);
        }
    }

    private void startWithScenarios() {
        Map<String, Exception> exceptionMap = new HashMap<>();
        Set<Integer> scenarioIndecies = new HashSet<>();
        for (Scenario scenario : scenarios) {
            int scenarioIndex = scenario.getIndex();
            TestFormat.check(!scenarioIndecies.contains(scenarioIndex),
                             "Cannot define two scenarios with the same index " + scenarioIndex);
            scenarioIndecies.add(scenarioIndex);
            try {
                start(scenario);
            } catch (TestFormatException e) {
                // Test format violation is wrong for all the scenarios. Only report once.
                throw new TestFormatException(e.getMessage());
            } catch (Exception e) {
                exceptionMap.put(String.valueOf(scenarioIndex), e);
            }
        }
        if (!exceptionMap.isEmpty()) {
            StringBuilder builder = new StringBuilder("The following scenarios have failed: #");
            builder.append(String.join(", #", exceptionMap.keySet())).append("\n\n");
            for (Map.Entry<String, Exception> entry: exceptionMap.entrySet()) {
                String title = "Stacktrace for Scenario #" + entry.getKey();
                builder.append(title).append("\n").append("=".repeat(title.length())).append("\n");
                Exception e = entry.getValue();
                if (e instanceof IRViolationException) {
                    // For IR violations, only show the actual message and not the (uninteresting) stack trace.
                    builder.append(e.getMessage());
                } else {
                    // Print stack trace if it was not a format violation or test run exception
                    StringWriter errors = new StringWriter();
                    entry.getValue().printStackTrace(new PrintWriter(errors));
                    builder.append(errors.toString());
                }
                builder.append("\n");
            }
            TestRun.fail(builder.toString());
        }
    }

    /**
     * Execute a separate "flag" VM with White Box access to determine all test VM flags. The flag VM emits an encoding
     * of all required flags for the test VM to the standard output. Once the flag VM exits, this driver VM parses the
     * test VM flags, which also determine if IR matching should be done, and then starts the test VM to execute all tests.
     */
    private void start(Scenario scenario) {
        if (scenario != null && !scenario.isEnabled()) {
            System.out.println("Disabled scenario #" + scenario.getIndex() + "! This scenario is not present in set flag -DScenarios " +
                               "and is therefore not executed.");
            return;
        }

        String flagVMOutput = runFlagVM();
        List<String> testVMFlags = getTestVMFlags(flagVMOutput);
        runTestVM(scenario, testVMFlags);
    }

    private String runFlagVM() {
        ArrayList<String> cmds = prepareFlagVMFlags();
        OutputAnalyzer oa;
        try {
            // Run "flag" VM with White Box access to determine the test VM flags and if IR verification should be done.
            oa = ProcessTools.executeTestJvm(cmds);
        } catch (Exception e) {
            throw new TestRunException("Failed to execute TestFramework flag VM", e);
        }
        checkFlagVMExitCode(oa);
        return oa.getOutput();
    }

    /**
     * The "flag" VM needs White Box access to prepare all test VM flags. It emits these as encoding to the standard output.
     * This driver VM then parses the flags and adds them to the test VM.
     */
    private ArrayList<String> prepareFlagVMFlags() {
        ArrayList<String> cmds = new ArrayList<>();
        cmds.add("-Dtest.jdk=" + Utils.TEST_JDK);
        cmds.add("-cp");
        cmds.add(Utils.TEST_CLASS_PATH);
        cmds.add("-Xbootclasspath/a:.");
        cmds.add("-XX:+UnlockDiagnosticVMOptions");
        cmds.add("-XX:+WhiteBoxAPI");
        cmds.add(TestFrameworkPrepareFlags.class.getCanonicalName());
        cmds.add(testClass.getCanonicalName());
        return cmds;
    }

    private void checkFlagVMExitCode(OutputAnalyzer oa) {
        String flagVMOutput = oa.getOutput();
        final int exitCode = oa.getExitValue();
        if (VERBOSE && exitCode == 0) {
            System.out.println("--- OUTPUT TestFramework flag VM ---");
            System.out.println(flagVMOutput);
        }

        if (exitCode != 0) {
            System.out.println("--- OUTPUT TestFramework flag VM ---");
            System.err.println(flagVMOutput);
            throw new RuntimeException("\nTestFramework flag VM exited with " + exitCode);
        }
    }

    /**
     * Parse the test VM flags as prepared by the flag VM. Additionally check the property flag DPrintValidIRRules to determine
     * if IR matching should be done or not.
     */
    private List<String> getTestVMFlags(String flagVMOutput) {
        String patternString = "(?<=" + TestFramework.TEST_VM_FLAGS_START + "\\R)"
                               + "(.*DPrintValidIRRules=(true|false).*)\\R" + "(?=" + IREncodingPrinter.END + ")";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(flagVMOutput);
        if (!matcher.find()) {
            throw new TestFrameworkException("Invalid flag encoding emitted by flag VM");
        }
        VERIFY_IR = Boolean.parseBoolean(matcher.group(2));
        return new ArrayList<>(Arrays.asList(matcher.group(1).split(TEST_VM_FLAGS_DELIMITER)));
    }

    private void runTestVM(Scenario scenario, List<String> testVMflags) {
        List<String> cmds = prepareTestVMFlags(scenario, testVMflags);
        OutputAnalyzer oa;
        try {
            // Calls 'main' of TestFrameworkExecution to run all specified tests with commands 'cmds'.
            // Use executeProcess instead of executeTestJvm as we have already added the JTreg VM and
            // Java options in prepareTestVMFlags().
            oa = ProcessTools.executeProcess(ProcessTools.createJavaProcessBuilder(cmds));
        } catch (Exception e) {
            throw new TestFrameworkException("Error while executing Test VM", e);
        }

        lastVMOutput = oa.getOutput();
        checkTestVMExitCode(oa);
        if (scenario != null) {
            scenario.setVMOutput(lastVMOutput);
        }
        if (VERIFY_IR) {
            IRMatcher irMatcher = new IRMatcher(lastVMOutput, testClass);
            irMatcher.applyRules();
        }
    }

    private List<String> prepareTestVMFlags(Scenario scenario, List<String> testVMflags) {
        ArrayList<String> cmds = new ArrayList<>();

        // Need White Box access in test VM.
        cmds.add("-Xbootclasspath/a:.");
        cmds.add("-XX:+UnlockDiagnosticVMOptions");
        cmds.add("-XX:+WhiteBoxAPI");

        String[] jtregVMFlags = Utils.getTestJavaOpts();
        if (!PREFER_COMMAND_LINE_FLAGS) {
            cmds.addAll(Arrays.asList(jtregVMFlags));
        }
        if (scenario != null) {
            System.out.println("Running Scenario #" + scenario.getIndex() + " - [" + String.join(",", scenario.getFlags()) + "]");
            cmds.addAll(scenario.getFlags());
        }
        cmds.addAll(testVMflags);

        if (PREFER_COMMAND_LINE_FLAGS) {
            // Prefer flags set via the command line over the ones set by scenarios.
            cmds.addAll(Arrays.asList(jtregVMFlags));
        }

        cmds.add(TestFrameworkExecution.class.getCanonicalName());
        cmds.add(testClass.getCanonicalName());
        if (helperClasses != null) {
            helperClasses.forEach(c -> cmds.add(c.getCanonicalName()));
        }
        return cmds;
    }

    private static void checkTestVMExitCode(OutputAnalyzer oa) {
        final int exitCode = oa.getExitValue();
        if (VERBOSE && exitCode == 0) {
            System.out.println("--- OUTPUT TestFramework runner VM ---");
            System.out.println(oa.getOutput());
        }

        if (exitCode != 0) {
            throwTestException(oa, exitCode);
        }
    }

    private static void throwTestException(OutputAnalyzer oa, int exitCode) {
        String stdErr = oa.getStderr();
        if (stdErr.contains("TestFormat.reportIfAnyFailures")) {
            Pattern pattern = Pattern.compile("Violations \\(\\d+\\)[\\s\\S]*(?=/============/)");
            Matcher matcher = pattern.matcher(stdErr);
            TestFramework.check(matcher.find(), "Must find violation matches");
            throw new TestFormatException("\n\n" + matcher.group());
        } else {
            throw new TestRunException("\nTestFramework runner VM exited with " + exitCode + "\n\nError Output:\n" + stdErr);
        }
    }

    static void check(boolean test, String failureMessage) {
        if (!test) {
            throw new TestFrameworkException("Internal TestFrameworkExecution exception - please file a bug:\n" + failureMessage);
        }
    }
}

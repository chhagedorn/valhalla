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

import java.io.*;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Use this framework by using the following JTreg setup in your "some.package.Test"
 * {@literal @}library /test/lib
 * {@literal @}run driver some.package.Test
 */
public class TestFramework {
    static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("Verbose", "false"));
    static final String TEST_VM_FLAGS_START = "##### TestFrameworkPrepareFlags - used by TestFramework #####";
    static final String TEST_VM_FLAGS_DELIMITER = " ";
    static final String TEST_VM_FLAGS_END = "----- END -----";
    private static final int WARMUP_ITERATIONS = Integer.getInteger("Warmup", -1);
    private static final boolean PREFER_COMMAND_LINE_FLAGS = Boolean.parseBoolean(System.getProperty("PreferCommandLineFlags", "false"));
    private static boolean VERIFY_IR = true; // Should we perform IR matching?
    private static String lastTestVMOutput;

    private List<Class<?>> helperClasses = null;
    private List<Scenario> scenarios = null;
    private final List<String> flags = new ArrayList<>();
    private final Class<?> testClass;
    private TestFrameworkSocket socket;
    private int defaultWarmup = -1;
    private final HashMap<String, Method> testMethods = new HashMap<>();

    /*
     * Public interface methods
     */

    /**
     * Creates an instance of TestFramework to test the class from which this constructor was invoked from.
     * Use this constructor if you want to use multiple run options (flags, helper classes, scenarios).
     * Use the associated add methods ({@link #addFlags(String...)}, {@link #addScenarios(Scenario...)},
     * {@link #addHelperClasses(Class...)})
     * to set up everything and then start the testing by invoking {@link #start()}.
     */
    public TestFramework() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        this.testClass = walker.getCallerClass();
        initTestsMap();
        System.out.println(testClass);
    }

    /**
     * Creates an instance of TestFramework to test {@code testClass}.
     * Use this constructor if you want to use multiple run options (flags, helper classes, scenarios).
     * Use the associated add methods ({@link #addFlags(String...)}, @link #addScenarios(Scenario...)},
     * {@link #addHelperClasses(Class...)})
     * to set up everything and then start the testing by invoking {@link #start()}.
     * 
     * @param testClass the class to be tested by the framework.
     * @see #TestFramework()
     */
    public TestFramework(Class<?> testClass) {
        TestRun.check(testClass != null, "Test class cannot be null");
        this.testClass = testClass;
        initTestsMap();
    }

    /**
     * Returns a Method object that matches the specified test method name.
     *
     * @param mName the name of the test method
     * @return the Method object matching the specified name
     */
    public Method getTestMethod(String mName) {
        return testMethods.get(mName);
    }

    /*
     * Helper method that gathers all test methods and put them in Hashtable
     */
    synchronized private void initTestsMap() {
        for (Method m : testClass.getDeclaredMethods()) {
            Test[] testAnnos = m.getAnnotationsByType(Test.class);
            if (testAnnos.length != 0) {
                testMethods.put(m.getName(), m);
            }
        }
    }

    /**
     * Tests the class from which this method was invoked from.
     */
    public static void run() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        run(walker.getCallerClass());
    }

    /**
     * Tests {@code testClass}.
     *
     * @param testClass the class to be tested by the framework.
     * @see #run()
     */
    public static void run(Class<?> testClass) {
        TestFramework framework = new TestFramework(testClass);
        framework.start();
    }

    /**
     * Tests the class from which this method was invoked from. The test VM is called with the specified {@code flags}.
     * <ul>
     *     <li><p>The {@code flags} override any set Java or VM options by JTreg by default.<p>
     *     Use {@code -DPreferCommandLineFlags=true} if you want to prefer the Java and VM options over the {@code flags}.</li>
     *     <li><p>If you want to run your JTreg test with additional flags, use this method.</li>
     *     <li><p>If you want to run your JTreg test with multiple flag combinations,
     *     use {@link #runWithScenarios(Scenario...)}</li>
     * </ul>
     *
     * @param flags VM flags to be used for the test VM.
     */
    public static void runWithFlags(String... flags) {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        runWithFlags(walker.getCallerClass(), flags);
    }

    /**
     * Tests {@code testClass}. The test VM is called with the specified {@code flags}.
     * <ul>
     *     <li><p>The {@code flags} override any set Java or VM options by JTreg by default.<p>
     *     Use {@code -DPreferCommandLineFlags=true} if you want to prefer the Java and VM options over the {@code flags}.</li>
     *     <li><p>If you want to run your JTreg test with additional flags, use this method.</li>
     *     <li><p>If you want to run your JTreg test with multiple flag combinations,
     *     use {@link #runWithScenarios(Class, Scenario...)}</li>
     * </ul>
     * 
     * @param testClass the class to be tested by the framework.
     * @param flags VM flags to be used for the test VM.
     *              
     * @see #runWithFlags(String...)
     */
    public static void runWithFlags(Class<?> testClass, String... flags) {
        TestFramework framework = new TestFramework(testClass);
        framework.addFlags(flags);
        framework.start();
    }

    /**
     * Tests {@code testClass} which uses {@code helperClasses} that can specify additional compile command annotations
     * ({@link ForceCompile}, {@link DontCompile}, {@link ForceInline}, {@link DontInline}) to be applied while testing
     * {@code testClass} (also see description of {@link TestFramework}).
     * <ul>
     *     <li><p>If a helper class is not in the same file as the test class, make sure that JTreg compiles it by using
     *     {@literal @}compile in the JTreg header comment block.</li>
     *     <li><p>If a helper class does not specify any compile command annotations, you do not need to include it. If
     *     no helper class specifies any compile commands, consider using {@link #run()} or {@link #run(Class)}.</li>
     * </ul>
     *
     * @param testClass the class to be tested by the framework.
     * @param helperClasses helper classes containing compile command annotations ({@link ForceCompile},
     *                      {@link DontCompile}, {@link ForceInline}, {@link DontInline}) to be applied
     *                      while testing {@code testClass} (also see description of {@link TestFramework}).
     */
    public static void runWithHelperClasses(Class<?> testClass, Class<?>... helperClasses) {
        TestFramework framework = new TestFramework(testClass);
        framework.addHelperClasses(helperClasses);
        framework.start();
    }

    /**
     * Tests the class from which this method was invoked from. A test VM is called for each scenario in {@code scenarios}
     * by using the specified flags in the scenario.
     * <ul>
     *     <li><p>If there is only one scenario, consider using {@link #runWithFlags(String...)}.</li>
     *     <li><p>The scenario flags override any Java or VM options set by JTreg by default.<p>
     *     Use {@code -DPreferCommandLineFlags=true} if you want to prefer the Java and VM options over the scenario flags.</li>
     * </ul>
     *
     * @param scenarios scenarios which specify specific flags for the test VM.
     */
    public static void runWithScenarios(Scenario... scenarios) {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        runWithScenarios(walker.getCallerClass(), scenarios);
    }

    /**
     * Tests {@code testClass} A test VM is called for each scenario in {@code scenarios} by using the specified flags
     * in the scenario.
     * <ul>
     *     <li><p>If there is only one scenario, consider using {@link #runWithFlags(String...)}.</li>
     *     <li><p>The scenario flags override any Java or VM options set by JTreg by default.<p>
     *     Use {@code -DPreferCommandLineFlags=true} if you want to prefer the Java and VM options over the scenario flags.</li>
     * </ul>
     *
     * @param testClass the class to be tested by the framework.
     * @param scenarios scenarios which specify specific flags for the test VM.
     *
     * @see #runWithScenarios(Scenario...)
     */
    public static void runWithScenarios(Class<?> testClass, Scenario... scenarios) {
        TestFramework framework = new TestFramework(testClass);
        framework.addScenarios(scenarios);
        framework.start();
    }

    /**
     * Add VM flags to be used for the test VM. These flags override any Java or VM options set by JTreg by default.<p>
     * Use {@code -DPreferCommandLineFlags=true} if you want to prefer the Java and VM options over the scenario flags.
     *
     * <p>
     * The testing can be started by invoking {@link #start()}
     *
     * @param flags VM options to be applied to the test VM.
     * @return the same framework instance.
     */
    public TestFramework addFlags(String... flags) {
        TestRun.check(flags != null && Arrays.stream(flags).noneMatch(Objects::isNull), "A flag cannot be null");
        this.flags.addAll(Arrays.asList(flags));
        return this;
    }

    /**
     * Add helper classes that can specify additional compile command annotations ({@link ForceCompile}, {@link DontCompile},
     * {@link ForceInline}, {@link DontInline}) to be applied while testing{@code testClass} (also see description of
     * {@link TestFramework}).
     * <ul>
     *     <li><p>If a helper class is not in the same file as the test class, make sure that JTreg compiles it by using
     *     {@literal @}compile in the JTreg header comment block.</li>
     *     <li><p>If a helper class does not specify any compile command annotations, you do not need to include it. If
     *     no helper class specifies any compile commands, you do not need to use this method</li>
     * </ul>
     *
     * <p>
     * The testing can be started by invoking {@link #start()}
     *
     * @param helperClasses helper classes containing compile command annotations ({@link ForceCompile},
     *                      {@link DontCompile}, {@link ForceInline}, {@link DontInline}) to be applied
     *                      while testing {@code testClass} (also see description of {@link TestFramework}).
     * @return the same framework instance.
     */
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

    /**
     * Add scenarios to be used for the test VM. A test VM is called for each scenario in {@code scenarios} by using the
     * specified flags in the scenario. The scenario flags override any flags set by {@link #addFlags(String...)}
     * and thus also override any Java or VM options set by JTreg by default.<p>
     * Use {@code -DPreferCommandLineFlags=true} if you want to prefer the Java and VM options over the scenario flags.
     *
     * <p>
     * The testing can be started by invoking {@link #start()}
     *
     * @param scenarios scenarios which specify specific flags for the test VM.
     * @return the same framework instance.
     */
    public TestFramework addScenarios(Scenario... scenarios) {
        TestRun.check(scenarios != null && Arrays.stream(scenarios).noneMatch(Objects::isNull), "A scenario cannot be null");
        if (this.scenarios == null) {
            this.scenarios = new ArrayList<>(Arrays.asList(scenarios));
        } else {
            this.scenarios.addAll(Arrays.asList(scenarios));
        }
        return this;
    }

    /**
     * Start the testing of the implicitely set test class by {@link #TestFramework()}
     * or explicitly set by {@link #TestFramework(Class)}.
     */
    public void start() {
        installWhiteBox();
        if (scenarios == null) {
            try {
                start(null);
            } catch (TestVMException e) {
                System.err.println("\n" + e.getExceptionInfo());
                throw e;
            } catch (IRViolationException e) {
                System.out.println("Compilation(s) of failed matche(s):");
                System.out.println(e.getCompilations());
                throw e;
            }
        } else {
            startWithScenarios();
        }
    }

    /**
     * Set a new default warm-up (overriding the framework default of 2000) to be applied for all tests that do
     * not specify an explicit warm-up with {@link Warmup}.
     *
     * @param defaultWarmup a non-negative default warm-up
     * @return the same framework instance.
     */
    public TestFramework setDefaultWarmup(int defaultWarmup) {
        TestFormat.check(defaultWarmup >= 0, "Cannot specify a negative default warm-up");
        this.defaultWarmup = defaultWarmup;
        return this;
    }

    /**
     * Get the VM output of the test VM. Use {@code -DVerbose=true} to enable more debug information. If scenarios
     * were run, use {@link Scenario#getTestVMOutput()}.
     *
     * @return the last test VM output
     */
    public static String getLastTestVMOutput() {
        return lastTestVMOutput;
    }

    /*
     * The following methods can only be called from actual tests and not from the main() method of a test.
     * Calling these methods from main() results in a linking exception (Whitebox not yet loaded and enabled).
     */

    /**
     * Compile {@code m} at compilation level {@code compLevel}. {@code m} is first enqueued and might not be compiled
     * yet upon returning from this method.
     *
     * @param m the method to be compiled
     * @param compLevel the (valid) compilation level at which the method should be compiled.
     * @throws TestRunException if compilation level is {@link CompLevel#SKIP} or {@link CompLevel#WAIT_FOR_COMPILATION}
     */
    public static void compile(Method m, CompLevel compLevel) {
        TestFrameworkExecution.compile(m, compLevel);
    }

    public static void deoptimize(Method m) {
        TestFrameworkExecution.deoptimize(m);
    }

    public void deoptimize(String mName) {
        deoptimize(getTestMethod(mName));
    }

    public static boolean isCompiled(Method m) {
        return TestFrameworkExecution.isCompiled(m);
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
        Map<Scenario, Exception> exceptionMap = new TreeMap<>(Comparator.comparingInt(Scenario::getIndex));
        Set<Integer> scenarioIndices = new HashSet<>();
        for (Scenario scenario : scenarios) {
            int scenarioIndex = scenario.getIndex();
            TestFormat.check(!scenarioIndices.contains(scenarioIndex),
                             "Cannot define two scenarios with the same index " + scenarioIndex);
            scenarioIndices.add(scenarioIndex);
            try {
                start(scenario);
            } catch (TestFormatException e) {
                // Test format violation is wrong for all the scenarios. Only report once.
                throw e;
            } catch (Exception e) {
                exceptionMap.put(scenario, e);
            }
        }
        if (!exceptionMap.isEmpty()) {
            reportScenarioFailures(exceptionMap);
        }
    }

    private void reportScenarioFailures(Map<Scenario, Exception> exceptionMap) {
        String failedScenarios = "The following scenarios have failed: #"
                                 + exceptionMap.keySet().stream()
                                               .map(s -> String.valueOf(s.getIndex()))
                                               .collect(Collectors.joining(", #"));
        StringBuilder builder = new StringBuilder(failedScenarios);
        builder.append("\n\n");
        for (Map.Entry<Scenario, Exception> entry : exceptionMap.entrySet()) {
            Exception e = entry.getValue();
            Scenario scenario = entry.getKey();
            String errorMsg = "";
            if (scenario != null) {
                errorMsg = getScenarioTitleAndFlags(scenario);
            }
            if (e instanceof IRViolationException) {
                // For IR violations, only show the actual violations and not the (uninteresting) stack trace.
                System.out.println((scenario != null ? "Scenario #" + scenario.getIndex() + " - " : "")
                                   + "Compilation(s) of failed matche(s):");
                System.out.println(((IRViolationException) e).getCompilations());
                builder.append(errorMsg).append(e.getMessage());
            } else if (e instanceof TestVMException) {
                builder.append(errorMsg).append("\n").append(((TestVMException) e).getExceptionInfo());
            } else {
                // Print stack trace otherwise
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                builder.append(errors.toString());
            }
            builder.append("\n");
        }
        System.err.println(builder.toString());
        TestRun.fail(failedScenarios + ". Please check stderr for more information.");
    }

    private static String getScenarioTitleAndFlags(Scenario scenario) {
        StringBuilder builder = new StringBuilder();
        String title = "Scenario #" + scenario.getIndex();
        builder.append(title).append("\n").append("=".repeat(title.length())).append("\n");
        builder.append("Scenario flags: [").append(String.join(", ", scenario.getFlags())).append("]\n");
        return builder.toString();
    }

    /**
     * Execute a separate "flag" VM with White Box access to determine all test VM flags. The flag VM sends an encoding of
     * all required flags for the test VM to the driver VM over a socket. Once the flag VM exits, this driver VM parses the
     * test VM flags, which also determine if IR matching should be done, and then starts the test VM to execute all tests.
     */
    private void start(Scenario scenario) {
        if (scenario != null && !scenario.isEnabled()) {
            System.out.println("Disabled scenario #" + scenario.getIndex() + "! This scenario is not present in set flag " +
                               "-DScenarios and is therefore not executed.");
            return;
        }
        socket = TestFrameworkSocket.getSocket();
        try {
            // Use TestFramework flags and scenario flags for new VMs.
            List<String> additionalFlags = new ArrayList<>(flags);
            if (scenario != null) {
                List<String> scenarioFlags = scenario.getFlags();
                String scenarioFlagsString = scenarioFlags.isEmpty() ? "" : " - [" + String.join(", ", scenarioFlags) + "]";
                System.out.println("Scenario #" + scenario.getIndex() + scenarioFlagsString + ":");
                additionalFlags.addAll(scenarioFlags);
            }
            System.out.println("Run Flag VM:");
            runFlagVM(additionalFlags);

            String flagsString = additionalFlags.isEmpty() ? "" : " - [" + String.join(", ", additionalFlags) + "]";
            System.out.println("Run Test VM" + flagsString + ":");
            runTestVM(additionalFlags, scenario);
        } finally {
            System.out.println();
            socket.close();
        }
    }

    private void runFlagVM(List<String> additionalFlags) {
        ArrayList<String> cmds = prepareFlagVMFlags(additionalFlags);
        socket.start();
        OutputAnalyzer oa;
        try {
            // Run "flag" VM with White Box access to determine the test VM flags and if IR verification should be done.
            oa = ProcessTools.executeTestJvm(cmds);
        } catch (Exception e) {
            throw new TestRunException("Failed to execute TestFramework flag VM", e);
        }
        checkFlagVMExitCode(oa);
    }

    /**
     * The "flag" VM needs White Box access to prepare all test VM flags. It sends these as encoding over a socket to the
     * driver VM which afterwards parses the flags and adds them to the test VM.
     */
    private ArrayList<String> prepareFlagVMFlags(List<String> additionalFlags) {
        ArrayList<String> cmds = new ArrayList<>();
        cmds.add("-Dtest.jdk=" + Utils.TEST_JDK);
        // Set java.library.path so JNI tests which rely on jtreg nativepath setting work
        cmds.add("-Djava.library.path=" + Utils.TEST_NATIVE_PATH);
        cmds.add("-cp");
        cmds.add(Utils.TEST_CLASS_PATH);
        cmds.add("-Xbootclasspath/a:.");
        cmds.add("-XX:+UnlockDiagnosticVMOptions");
        cmds.add("-XX:+WhiteBoxAPI");
        cmds.add(socket.getPortPropertyFlag());
        // TestFramework and scenario flags might have an influence on the later used test VM flags. Add them as well.
        cmds.addAll(additionalFlags);
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
            System.err.println("--- OUTPUT TestFramework flag VM ---");
            System.err.println(flagVMOutput);
            throw new RuntimeException("\nTestFramework flag VM exited with " + exitCode);
        }
    }

    private void runTestVM(List<String> additionalFlags, Scenario scenario) {
        List<String> cmds = prepareTestVMFlags(additionalFlags);
        if (VERIFY_IR) {
            // We only need the socket if we are doing IR verification.
            socket.start();
        }

        OutputAnalyzer oa;
        ProcessBuilder process = ProcessTools.createJavaProcessBuilder(cmds);
        try {
            // Calls 'main' of TestFrameworkExecution to run all specified tests with commands 'cmds'.
            // Use executeProcess instead of executeTestJvm as we have already added the JTreg VM and
            // Java options in prepareTestVMFlags().
            oa = ProcessTools.executeProcess(process);
        } catch (Exception e) {
            fail("Error while executing Test VM", e);
            return;
        }
        JVMOutput output = new JVMOutput(oa, scenario, process);
        lastTestVMOutput = oa.getOutput();
        if (scenario != null) {
            scenario.setTestVMOutput(lastTestVMOutput);
        }
        checkTestVMExitCode(output);
        if (VERIFY_IR) {
            new IRMatcher(output.getHotspotPidFileName(), socket.getOutput(), testClass);
        } else {
            System.out.println("IR Verification disabled either through explicitly setting -DVerify=false or due to " +
                               "not running a debug build, running with -Xint, or other VM flags that make the verification " +
                               "inaccurate or impossible (e.g. using -XX:CompileThreshold, running with C1 only etc.).");
        }
    }

    private List<String> prepareTestVMFlags(List<String> additionalFlags) {
        ArrayList<String> cmds = new ArrayList<>();
        // Set java.library.path so JNI tests which rely on jtreg nativepath setting work
        cmds.add("-Djava.library.path=" + Utils.TEST_NATIVE_PATH);
        // Need White Box access in test VM.
        cmds.add("-Xbootclasspath/a:.");
        cmds.add("-XX:+UnlockDiagnosticVMOptions");
        cmds.add("-XX:+WhiteBoxAPI");
        String[] jtregVMFlags = Utils.getTestJavaOpts();
        if (!PREFER_COMMAND_LINE_FLAGS) {
            cmds.addAll(Arrays.asList(jtregVMFlags));
        }
        cmds.addAll(additionalFlags);
        cmds.addAll(getTestVMFlags());

        if (PREFER_COMMAND_LINE_FLAGS) {
            // Prefer flags set via the command line over the ones set by scenarios.
            cmds.addAll(Arrays.asList(jtregVMFlags));
        }

        if (WARMUP_ITERATIONS < 0 && defaultWarmup != -1) {
            // Only use the set warmup for the framework if not overridden by a valid -DWarmup property set by a test.
            cmds.add("-DWarmup=" + defaultWarmup);
        }


            if (VERIFY_IR) {
            // Add server property flag that enables test VM to print encoding for IR verification last.
            cmds.add(socket.getPortPropertyFlag());
        }

        cmds.add(TestFrameworkExecution.class.getName());
        cmds.add(testClass.getName());
        if (helperClasses != null) {
            helperClasses.forEach(c -> cmds.add(c.getName()));
        }
        return cmds;
    }

    /**
     * Parse the test VM flags as prepared by the flag VM. Additionally check the property flag DShouldDoIRVerification
     * to determine if IR matching should be done or not.
     */
    private List<String> getTestVMFlags() {
        String patternString = "(?<=" + TestFramework.TEST_VM_FLAGS_START + "\\R)" + "(.*DShouldDoIRVerification=(true|false).*)\\R" + "(?=" + IREncodingPrinter.END + ")";
        Pattern pattern = Pattern.compile(patternString);
        String flags = socket.getOutput();
        if (VERBOSE) {
            System.out.println("Read sent data from flag VM from socket:");
            System.out.println(flags);
        }
        Matcher matcher = pattern.matcher(flags);
        check(matcher.find(), "Invalid flag encoding emitted by flag VM");
        VERIFY_IR = Boolean.parseBoolean(matcher.group(2));
        return new ArrayList<>(Arrays.asList(matcher.group(1).split(TEST_VM_FLAGS_DELIMITER)));
    }

    private static void checkTestVMExitCode(JVMOutput vmOutput) {
        final int exitCode = vmOutput.getExitCode();
        if (VERBOSE && exitCode == 0) {
            System.out.println("--- OUTPUT TestFramework test VM ---");
            System.out.println(vmOutput.getOutput());
        }

        if (exitCode != 0) {
            throwTestVMException(vmOutput);
        }
    }

    private static void throwTestVMException(JVMOutput vmOutput) {
        String stdErr = vmOutput.getStderr();
        if (stdErr.contains("TestFormat.reportIfAnyFailures")) {
            Pattern pattern = Pattern.compile("Violations \\(\\d+\\)[\\s\\S]*(?=/============/)");
            Matcher matcher = pattern.matcher(stdErr);
            TestFramework.check(matcher.find(), "Must find violation matches");
            throw new TestFormatException("\n\n" + matcher.group());
        } else {
            throw new TestVMException(vmOutput);
        }
    }

    static void check(boolean test, String failureMessage) {
        if (!test) {
            fail(failureMessage);
        }
    }

    static void fail(String failureMessage) {
        throw new TestFrameworkException("Internal Test Framework exception - please file a bug:\n" + failureMessage);
    }

    static void fail(String failureMessage, Throwable e) {
        throw new TestFrameworkException("Internal Test Framework exception - please file a bug:\n" + failureMessage, e);
    }
}

/**
 * Class to encapsulate information about the test VM output, the run process and the scenario.
 */
class JVMOutput {
    private final Scenario scenario;
    private final OutputAnalyzer oa;
    private final ProcessBuilder process;
    private final String hotspotPidFileName;

    JVMOutput(OutputAnalyzer oa, Scenario scenario, ProcessBuilder process) {
        this.oa = oa;
        this.scenario = scenario;
        this.process = process;
        this.hotspotPidFileName = String.format("hotspot_pid%d.log", oa.pid());
    }

    public Scenario getScenario() {
        return scenario;
    }

    public String getCommandLine() {
        return String.join(" ", process.command());
    }

    public int getExitCode() {
        return oa.getExitValue();
    }

    public String getOutput() {
        return oa.getOutput();
    }

    public String getStdout() {
        return oa.getStdout();
    }

    public String getStderr() {
        return oa.getStderr();
    }

    public String getHotspotPidFileName() {
        return hotspotPidFileName;
    }
}

/**
 * Dedicated socket to send data from the flag and test VM back to the driver VM.
 */
class TestFrameworkSocket {
    static final String SERVER_PORT_PROPERTY = "ir.framework.server.port";

    // Static fields used by flag and test VM only.
    private static final int SERVER_PORT = Integer.getInteger(SERVER_PORT_PROPERTY, -1);
    private static final boolean REPRODUCE = Boolean.getBoolean("Reproduce");
    private static final String HOSTNAME = null;

    private final String serverPortPropertyFlag;
    private FutureTask<String> socketTask;
    private ServerSocket serverSocket;

    private static TestFrameworkSocket singleton = null;

    private TestFrameworkSocket() {
        try {
            serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            TestFramework.fail("Failed to create TestFramework server socket", e);
        }
        int port = serverSocket.getLocalPort();
        if (TestFramework.VERBOSE) {
            System.out.println("TestFramework server socket uses port " + port);
        }
        serverPortPropertyFlag = "-D" + SERVER_PORT_PROPERTY + "=" + port;
    }

    public static TestFrameworkSocket getSocket() {
        if (singleton == null || singleton.serverSocket.isClosed()) {
            singleton = new TestFrameworkSocket();
            return singleton;
        }
        return singleton;
    }

    public String getPortPropertyFlag() {
        return serverPortPropertyFlag;
    }

    public void start() {
        socketTask = initSocketTask();
        Thread socketThread = new Thread(socketTask);
        socketThread.start();
    }

    private FutureTask<String> initSocketTask() {
        return new FutureTask<>(() -> {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
            ) {
                StringBuilder builder = new StringBuilder();
                String next;
                while ((next = in.readLine()) != null) {
                    builder.append(next).append("\n");
                }
                return builder.toString();
            } catch (IOException e) {
                TestFramework.fail("Server socket error", e);
                return null;
            }
        });
    }

    public void close() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            TestFramework.fail("Could not close socket", e);
        }
    }

    /**
     * Only called by flag and test VM to write to server socket.
     */
    public static void write(String msg, String type) {
        if (REPRODUCE) {
            System.out.println("Debugging Test VM: Skip writing due to -DReproduce");
            return;
        }
        TestFramework.check(SERVER_PORT != -1, "Server port was not set correctly for flag and/or test VM "
                                              + "or method not called from flag or test VM");
        try (Socket socket = new Socket(HOSTNAME, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.print(msg);
        } catch (Exception e) {
            String failMsg = """
                             
                             ###########################################################
                              Did you directly run the test VM (TestFrameworkExecution) 
                              to reproduce a bug?                                       
                              => Append the flag -DReproduce=true and try again!        
                             ###########################################################
                             """;
            TestRun.fail(failMsg, e);
        }
        if (TestFramework.VERBOSE) {
            System.out.println("Written " + type + " to socket:");
            System.out.println(msg);
        }
    }

    public String getOutput() {
        try {
            return socketTask.get();
        } catch (Exception e) {
            TestFramework.fail("Could not read from socket task", e);
            return null;
        }
    }
}

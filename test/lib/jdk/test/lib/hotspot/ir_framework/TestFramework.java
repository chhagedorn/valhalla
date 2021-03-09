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
    private List<String> flags = new ArrayList<>();
    private final Class<?> testClass;
    private static String lastVMOutput;
    private TestFrameworkSocket socket;

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

    public static void runWithFlags(String... flags) {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        runWithFlags(walker.getCallerClass(), flags);
    }

    public static void runWithFlags(Class<?> testClass, String... flags) {
        TestFramework framework = new TestFramework(testClass);
        framework.addFlags(flags);
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

    public TestFramework addFlags(String... flags) {
        TestRun.check(flags != null && Arrays.stream(flags).noneMatch(Objects::isNull), "A flag cannot be null");
        this.flags.addAll(Arrays.asList(flags));
        return this;
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
        flags.clear();
        helperClasses = null;
        scenarios = null;
    }

    public void start() {
        installWhiteBox();
        socket = new TestFrameworkSocket();
        try {
            if (scenarios == null) {
                start(null);
            } else {
                startWithScenarios();
            }
        } finally {
            socket.close();
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
            for (Map.Entry<String, Exception> entry : exceptionMap.entrySet()) {
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
        runTestVM(additionalFlags);
        if (scenario != null) {
            scenario.setVMOutput(lastVMOutput);
        }
        System.out.println();
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
        cmds.add("-cp");
        cmds.add(Utils.TEST_CLASS_PATH);
        cmds.add("-Xbootclasspath/a:.");
        cmds.add("-XX:+UnlockDiagnosticVMOptions");
        cmds.add("-XX:+WhiteBoxAPI");
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

    private void runTestVM(List<String> additionalFlags) {
        List<String> cmds = prepareTestVMFlags(additionalFlags);
        if (VERIFY_IR) {
            // We only need the socket if we are doing IR verification.
            socket.start();
        }
        OutputAnalyzer oa;
        try {
            // Calls 'main' of TestFrameworkExecution to run all specified tests with commands 'cmds'.
            // Use executeProcess instead of executeTestJvm as we have already added the JTreg VM and
            // Java options in prepareTestVMFlags().
            oa = ProcessTools.executeProcess(ProcessTools.createJavaProcessBuilder(cmds));
        } catch (Exception e) {
            fail("Error while executing Test VM", e);
            return;
        }

        lastVMOutput = oa.getOutput();
        checkTestVMExitCode(oa);
        if (VERIFY_IR) {
            IRMatcher irMatcher = new IRMatcher(lastVMOutput, socket.getOutput(), testClass);
            irMatcher.applyRules();
        }
    }

    private List<String> prepareTestVMFlags(List<String> additionalFlags) {
        ArrayList<String> cmds = new ArrayList<>();

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

        cmds.add(TestFrameworkExecution.class.getCanonicalName());
        cmds.add(testClass.getCanonicalName());
        if (helperClasses != null) {
            helperClasses.forEach(c -> cmds.add(c.getCanonicalName()));
        }
        return cmds;
    }

    /**
     * Parse the test VM flags as prepared by the flag VM. Additionally check the property flag DPrintValidIRRules to determine
     * if IR matching should be done or not.
     */
    private List<String> getTestVMFlags() {
        String patternString = "(?<=" + TestFramework.TEST_VM_FLAGS_START + "\\R)" + "(.*DPrintValidIRRules=(true|false).*)\\R" + "(?=" + IREncodingPrinter.END + ")";
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
            System.err.println("--- Standard Output TestFramework test VM ---");
            System.err.println(oa.getStdout());
            throw new TestRunException("\nTestFramework test VM exited with " + exitCode + "\n\nError Output:\n" + stdErr);
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

    static void fail(String failureMessage, Exception e) {
        throw new TestFrameworkException("Internal Test Framework exception - please file a bug:\n" + failureMessage, e);
    }
}

/**
 * Dedicated socket to send data from flag and test VM back to the driver VM.
 */
class TestFrameworkSocket {
    private static final int SOCKET_PORT = 6672;
    private static final String HOSTNAME = "localhost";

    private FutureTask<String> socketTask;
    private Thread socketThread;
    private ServerSocket serverSocket;

    TestFrameworkSocket() {
        try {
            serverSocket = new ServerSocket(SOCKET_PORT);
        } catch (IOException e) {
            TestFramework.fail("Server socket error", e);
        }
    }

    public void start() {
        socketTask = initSocketTask();
        socketThread = new Thread(socketTask);
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

    public void checkTerminated() {
        try {
            socketThread.join(5000);
            if (socketThread.isAlive()) {
                serverSocket.close();
                TestFramework.fail("Socket thread was not terminated");
            }
        } catch (InterruptedException | IOException e) {
            TestFramework.fail("Socket thread was not closed", e);
        }
    }

    public static void write(String msg, String type) {
        try (Socket socket = new Socket(HOSTNAME, SOCKET_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            out.print(msg);
        } catch (Exception e) {
            TestFramework.fail("Failed to write to socket", e);
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

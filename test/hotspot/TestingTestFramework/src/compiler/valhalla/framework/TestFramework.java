package compiler.valhalla.framework;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.management.InputArguments;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;
import jdk.test.lib.Platform;


public class TestFramework {
    private static final WhiteBox WHITE_BOX;

    static {
        try {
            WHITE_BOX = WhiteBox.getWhiteBox();
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Did you set up the jtreg test properly? Ensure that at least the following settings are set:");
            System.err.println("""
                    * @library /testlibrary /test/lib /compiler/whitebox /
                    * @run driver ClassFileInstaller sun.hotspot.WhiteBox jdk.test.lib.Platform
                    * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
                    *                   -XX:+UnlockExperimentalVMOptions -XX:+WhiteBoxAPI""".indent(1));
            throw e;
        }
    }

    private static final boolean TIERED_COMPILATION = (Boolean)WHITE_BOX.getVMFlag("TieredCompilation");
    private static final CompLevel TIERED_COMPILATION_STOP_AT_LEVEL = CompLevel.forValue(((Long)WHITE_BOX.getVMFlag("TieredStopAtLevel")).intValue());
    static final boolean TEST_C1 = TIERED_COMPILATION && TIERED_COMPILATION_STOP_AT_LEVEL.getValue() < CompLevel.C2.getValue();

    // User defined settings
    static final boolean XCOMP = Platform.isComp();
    private static final boolean PRINT_GRAPH = true;
//    private static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("Verbose", "false"));
    static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("Verbose", "true"));
    private static final boolean PRINT_TIMES = Boolean.parseBoolean(System.getProperty("PrintTimes", "false"));

    private static final boolean COMPILE_COMMANDS = Boolean.parseBoolean(System.getProperty("CompileCommands", "true")) && !XCOMP;
    private static       boolean VERIFY_IR = Boolean.parseBoolean(System.getProperty("VerifyIR", "true"))
                                             && !XCOMP && !TEST_C1 && COMPILE_COMMANDS && Platform.isDebugBuild() && !Platform.isInt();
    private static final boolean VERIFY_VM = Boolean.parseBoolean(System.getProperty("VerifyVM", "false")) && Platform.isDebugBuild();
    private static final String TESTLIST = System.getProperty("Testlist", "");
    private static final String EXCLUDELIST = System.getProperty("Exclude", "");
    public static final int WARMUP_ITERATIONS = Integer.parseInt(System.getProperty("Warmup", "251"));
    private static final boolean DUMP_REPLAY = Boolean.parseBoolean(System.getProperty("DumpReplay", "false"));
    private static final boolean GC_AFTER = Boolean.parseBoolean(System.getProperty("GCAfter", "false"));
    static final boolean STRESS_CC = Boolean.parseBoolean(System.getProperty("StressCC", "false"));
    private static final boolean SHUFFLE_TESTS = Boolean.parseBoolean(System.getProperty("ShuffleTests", "true"));
    private static final boolean PREFER_COMMAND_LINE_FLAGS = Boolean.parseBoolean(System.getProperty("PreferCommandLineFlags", "false"));
    private static final boolean USE_COMPILE_COMMAND_ANNOTATIONS = Boolean.parseBoolean(System.getProperty("UseCompileCommandAnnotations", "true"));
    private static final boolean PRINT_VALID_IR_RULES = Boolean.parseBoolean(System.getProperty("PrintValidIRRules", "false"));
    protected static final long PerMethodTrapLimit = (Long)WHITE_BOX.getVMFlag("PerMethodTrapLimit");
    protected static final boolean ProfileInterpreter = (Boolean)WHITE_BOX.getVMFlag("ProfileInterpreter");
    private static final boolean FLIP_C1_C2 = Boolean.parseBoolean(System.getProperty("FlipC1C2", "false"));

    static final boolean TESTING_TEST_FRAMEWORK = Boolean.parseBoolean(System.getProperty("TestingTestFramework", "false"));

    private final String[] fixedDefaultFlags;
    private final String[] compileCommandFlags;
    private final String[] printFlags;
    private final String[] verifyFlags;
    private static String lastVmOutput; // Only used to test TestFramework

    static final boolean USE_COMPILER = WHITE_BOX.getBooleanVMFlag("UseCompiler");
    static final boolean VERIFY_OOPS = (Boolean)WHITE_BOX.getVMFlag("VerifyOops");

    private final HashMap<Method, DeclaredTest> declaredTests = new HashMap<>();
    private final LinkedHashMap<Method, BaseTest> allTests = new LinkedHashMap<>(); // Keep order
    private final HashMap<String, Method> testMethodMap = new HashMap<>();
    private List<String> excludeList = null;
    private List<String> includeList = null;
    private final IREncodingPrinter irMatchRulePrinter;

    TestFramework() {
        // These flags can be overridden
        fixedDefaultFlags = initDefaultFlags();
        compileCommandFlags = initCompileCommandFlags();
        printFlags = initPrintFlags();
        verifyFlags = initVerifyFlags();

        if (PRINT_VALID_IR_RULES) {
            irMatchRulePrinter = new IREncodingPrinter();
        } else {
            irMatchRulePrinter = null;
        }
    }

    // Only used by a TestVM
    private TestFramework(Class<?> testClass) {
        this();
        includeList = createTestFilterList(TESTLIST, testClass);
        excludeList = createTestFilterList(EXCLUDELIST, testClass);
    }

    protected String[] initDefaultFlags() {
        return new String[] {"-XX:-BackgroundCompilation"};
    }

    protected String[] initCompileCommandFlags() {
        return new String[] {"-XX:CompileCommand=quiet"};
    }

    protected String[] initPrintFlags() {
        return new String[] {"-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions"};
    }

    protected String[] initVerifyFlags() {
        return new String[] {
                "-XX:+UnlockDiagnosticVMOptions", "-XX:+VerifyOops", "-XX:+VerifyStack", "-XX:+VerifyLastFrame", "-XX:+VerifyBeforeGC",
                "-XX:+VerifyAfterGC", "-XX:+VerifyDuringGC", "-XX:+VerifyAdapterSharing"};
    }

    private List<String> createTestFilterList(String list, Class<?> testClass) {
        List<String> filterList = null;
        if (!list.isEmpty()) {
            String classPrefix = testClass.getSimpleName() + ".";
            filterList = new ArrayList<>(Arrays.asList(list.split(",")));
            for (int i = filterList.size() - 1; i >= 0; i--) {
                String test = filterList.get(i);
                if (test.indexOf(".") > 0) {
                    if (test.startsWith(classPrefix)) {
                        test = test.substring(classPrefix.length());
                        filterList.set(i, test);
                    } else {
                        filterList.remove(i);
                    }
                }
            }
        }
        return filterList;
    }

    public static void main(String[] args) {
        String testClassName = args[0];
        System.out.println("Framework main(), about to run test class " + testClassName);
        Class<?> testClass;
        try {
            testClass = Class.forName(testClassName);
        } catch (Exception e) {
            throw new TestRunException("Could not find test class " + testClassName, e);
        }

        TestFramework framework = new TestFramework(testClass);
        framework.runTestsOnSameVM(testClass, getHelperClasses(args));
    }

    private static ArrayList<Class<?>> getHelperClasses(String[] args) {
        ArrayList<Class<?>> helperClasses = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String helperClassName = args[i];
            try {
                helperClasses.add(Class.forName(helperClassName));
            } catch (Exception e) {
                throw new TestRunException("Could not find helper class " + helperClassName, e);
            }
        }
        return helperClasses;
    }


    /*
     * Public interface methods
     */
    public static void run() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        doRun(walker.getCallerClass(), null);
    }

    public static void run(Class<?> testClass) {
        doRun(testClass, null);
    }

    public static void runWithHelperClasses(Class<?> testClass, Class<?>... helperClasses) {
        doRun(testClass, Arrays.asList(helperClasses));
    }

    public static void runWithScenarios(Scenario... scenarios) {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        runWithScenarios(Scenario.Run.EXCLUDE_DEFAULT, walker.getCallerClass(), scenarios);
    }

    public static void runWithScenarios(Scenario.Run runMode, Scenario... scenarios) {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        runWithScenarios(runMode, walker.getCallerClass(), scenarios);
    }

    public static void runWithScenarios(Class<?> testClass, Scenario... scenarios) {
        runWithScenarios(Scenario.Run.EXCLUDE_DEFAULT, testClass, null, scenarios);
    }

    public static void runWithScenarios(Scenario.Run runMode, Class<?> testClass, Scenario... scenarios) {
        runWithScenarios(runMode, testClass, null, scenarios);
    }

    public static void runWithScenarios(Class<?> testClass, List<Class<?>> helperClasses, Scenario... scenarios) {
        runWithScenarios(Scenario.Run.EXCLUDE_DEFAULT, testClass, helperClasses, scenarios);
    }

    public static void runWithScenarios(Scenario.Run runMode, Class<?> testClass, List<Class<?>> helperClasses, Scenario... scenarios) {
        TestFramework framework = new TestFramework();
        Map<String, Exception> exceptionMap = new HashMap<>();
        // First run without additional scenario flags.
        if (runMode == Scenario.Run.INCLUDE_DEFAULT) {
            try {
                framework.runTestVM(testClass, helperClasses, null);
            } catch (Exception e) {
                exceptionMap.put("Default Scenario", e);
            }
        }

        Set<Integer> scenarioIndecies = new HashSet<>();
        for (Scenario scenario : scenarios) {
            int scenarioIndex = scenario.getIndex();
            TestFormat.check(!scenarioIndecies.contains(scenarioIndex),
                             "Cannot define two scenarios with the same index " + scenarioIndex);
            scenarioIndecies.add(scenarioIndex);
            try {
                framework.runTestVM(testClass, helperClasses, scenario);
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
                builder.append(entry.getValue().getMessage()).append("\n");
            }
            TestRun.fail(builder.toString());
        }
    }

    public static void run(Class<?> testClass, List<Class<?>> helperClasses) {
        doRun(testClass, helperClasses);
    }

    // Can be called from tests for non-@Test methods
    public static void compile(Method m, CompLevel compLevel) {
        TestFormat.check(getAnnotation(m, Test.class) == null,
                         "Cannot call enqueueMethodForCompilation() for @Test annotated method " + m);
        enqueueMethodForCompilation(m, compLevel);
    }

    public static boolean isC2Compiled(Method m) {
        return WHITE_BOX.isMethodCompiled(m, false) && WHITE_BOX.getMethodCompilationLevel(m, false) == CompLevel.C2.getValue();
//        return compiledByC2(m) == TriState.Yes;
    }

    public static void assertDeoptimizedByC2(Method m) {
        TestRun.check(compiledByC2(m) != TriState.Yes || PerMethodTrapLimit == 0 || !ProfileInterpreter, m + " should have been deoptimized");
    }

    public static void assertCompiledByC2(Method m) {
        TestRun.check(compiledByC2(m) != TriState.No, m + " should have been compiled");
    }

    public static String getLastVmOutput() {
        return lastVmOutput;
    }
    /*
     * End of public interface
     */


    private static void doRun(Class<?> testClass, List<Class<?>> helperClasses) {
        TestFramework framework = new TestFramework();
        framework.runTestVM(testClass, helperClasses, null);
    }


    private void runTestsOnSameVM(Class<?> testClass, ArrayList<Class<?>> helperClasses) {
        for (Class<?> helperClass : helperClasses) {
            // Process the helper classes and apply the explicit compile commands
            processExplicitCompileCommands(helperClass);
        }
        runTestsOnSameVM(testClass);
    }

    private void runTestsOnSameVM(Class<?> testClass) {
        parseTestClass(testClass);
        runTests();
    }

    // Only called by tests testing the framework itself. Accessed by reflection. Do not expose this to normal users.
    private static void runTestsOnSameVM() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        TestFramework framework = new TestFramework();
        framework.runTestsOnSameVM(walker.getCallerClass());
    }

    private void runTestVM(Class<?> testClass, List<Class<?>> helperClasses, Scenario scenario) {
        if (scenario != null && !scenario.isEnabled()) {
            System.out.println("Disabled scenario #" + scenario.getIndex() + "! This scenario is not present in set flag -DScenarios and" +
                                       "is therefore not executed.");
            return;
        }

        ArrayList<String> cmds = prepareTestVmFlags(testClass, helperClasses, scenario);
        OutputAnalyzer oa;
        try {
            // Calls 'main' of this class to run all specified tests with commands 'cmds'.
            oa = ProcessTools.executeTestJvm(cmds);
        } catch (Exception e) {
            throw new TestRunException("Error while executing Test VM", e);
        }
        String output = oa.getOutput();
        if (VERBOSE) {
            System.out.println(" ----- OUTPUT -----");
            System.out.println(output);
        }
        lastVmOutput = output;
        if (!TESTING_TEST_FRAMEWORK) {
            // Tests for the framework itself might expect certain exceptions.
            oa.shouldHaveExitValue(0);
        }

        if (VERIFY_IR) {
            IRMatcher irMatcher = new IRMatcher(output, testClass);
            irMatcher.applyRules();
        }
    }

    private ArrayList<String> prepareTestVmFlags(Class<?> testClass, List<Class<?>> helperClasses, Scenario scenario) {
        String[] vmInputArguments = InputArguments.getVmInputArgs();
        ArrayList<String> cmds = new ArrayList<>();
        if (!PREFER_COMMAND_LINE_FLAGS) {
            cmds.addAll(Arrays.asList(vmInputArguments));
        }

        if (scenario != null) {
            System.out.println("Running Scenario #" + scenario.getIndex());
            cmds.addAll(scenario.getFlags());
        }
        setupIrVerificationFlags(testClass, cmds);

        if (VERIFY_VM) {
            cmds.addAll(Arrays.asList(verifyFlags));
        }

        cmds.addAll(Arrays.asList(fixedDefaultFlags));
        if (COMPILE_COMMANDS) {
            cmds.addAll(Arrays.asList(compileCommandFlags));
        }

        // TODO: Only for debugging
        if (cmds.get(0).startsWith("-agentlib")) {
            cmds.set(0, "-agentlib:jdwp=transport=dt_socket,address=127.0.0.1:44444,suspend=n,server=y");
        }

        if (PREFER_COMMAND_LINE_FLAGS) {
            // Prefer flags set via the command line over the ones set by scenarios.
            cmds.addAll(Arrays.asList(vmInputArguments));
        }

        cmds.add(getClass().getCanonicalName());
        cmds.add(testClass.getCanonicalName());
        if (helperClasses != null) {
            helperClasses.forEach(c -> cmds.add(c.getCanonicalName()));
        }

        if (VERBOSE) {
            System.out.println("Command Line: " + String.join(" ", cmds));
        }
        return cmds;
    }

    private void setupIrVerificationFlags(Class<?> testClass, ArrayList<String> cmds) {
        if (VERIFY_IR && cmds.stream().anyMatch(flag -> flag.startsWith("-XX:CompileThreshold"))) {
            // Disable IR verification if non-default CompileThreshold is set
            if (VERBOSE) {
                System.out.println("Disabled IR verification due to CompileThreshold flag");
            }
            VERIFY_IR = false;
        }

        if (VERIFY_IR) {
            // Add print flags for IR verification
            cmds.addAll(Arrays.asList(printFlags));
            addBoolOptionForClass(cmds, testClass, "PrintIdeal");
            addBoolOptionForClass(cmds, testClass, "PrintOptoAssembly");
            // Always trap for exception throwing to not confuse IR verification
            cmds.add("-XX:-OmitStackTraceInFastThrow");
            cmds.add("-DPrintValidIRRules=true");
        } else {
            cmds.add("-DPrintValidIRRules=false");
        }
    }

    private void addBoolOptionForClass(ArrayList<String> cmds, Class<?> testClass, String option) {
        cmds.add("-XX:CompileCommand=option," + testClass.getCanonicalName() + "::*,bool," + option + ",true");
    }

    private void parseTestClass(Class<?> clazz) {
        addReplay();
        processExplicitCompileCommands(clazz);
        setupTests(clazz);
        setupCheckAndRunMethods(clazz);

        // All remaining tests are simple base tests without check or specific way to run them
        declaredTests.forEach((key, value) -> allTests.put(key, new BaseTest(value)));
        declaredTests.clear();
        testMethodMap.clear();
    }

    private void addReplay() {
        if (DUMP_REPLAY) {
            // Generate replay compilation files
            String directive = "[{ match: \"*.*\", DumpReplay: true }]";
            TestFramework.check(WHITE_BOX.addCompilerDirective(directive) == 1, "Failed to add DUMP_REPLAY directive");
        }
    }

    private void processExplicitCompileCommands(Class<?> clazz) {
        if (USE_COMPILE_COMMAND_ANNOTATIONS) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method m : methods) {
                applyIndependentCompilationCommands(m);
            }

            // Only force compilation now because above annotations affect inlining
            for (Method m : methods) {
                applyForceCompileCommand(m);
            }
        }
    }

    private void applyIndependentCompilationCommands(Method m) {
        ForceInline forceInlineAnno = getAnnotation(m, ForceInline.class);
        DontInline dontInlineAnno = getAnnotation(m, DontInline.class);
        ForceCompile forceCompileAnno = getAnnotation(m, ForceCompile.class);
        DontCompile dontCompileAnno = getAnnotation(m, DontCompile.class);
        Test testAnno = getAnnotation(m, Test.class);
        TestFormat.check(testAnno == null || Stream.of(forceCompileAnno, dontCompileAnno, forceInlineAnno, dontInlineAnno).noneMatch(Objects::nonNull),
                         "Not allowed to use explicit compile command annotations (@ForceInline, @DontInline," +
                         "@ForceCompile or @DontCompile) together with @Test at " + m + ". Use compLevel and skip in @Test for fine tuning.");
        if (Stream.of(forceCompileAnno, dontCompileAnno, dontInlineAnno).filter(Objects::nonNull).count() > 1) {
            // Failure
            TestFormat.check(dontCompileAnno == null || dontInlineAnno == null,
                             "@DontInline is implicitely done with @DontCompile annotation at " + m);
            TestFormat.fail("Cannot mix @ForceInline, @DontInline and @DontCompile at the same time at " + m);
        }
        TestFormat.check(forceCompileAnno == null || dontCompileAnno == null,
                         "Cannot have @ForceCompile and @DontCompile at the same time at " + m);
        // First handle inline annotations
        if (dontInlineAnno != null) {
            WHITE_BOX.testSetDontInlineMethod(m, true);
        } else if (forceInlineAnno != null) {
            WHITE_BOX.testSetForceInlineMethod(m, true);
        }
        if (dontCompileAnno != null) {
            dontCompileMethod(m);
        }

        if (STRESS_CC) {
            // Exclude some methods from compilation with C2 to stress test the calling convention
            if (Utils.getRandomInstance().nextBoolean()) {
                System.out.println("Excluding from C2 compilation: " + m);
                WHITE_BOX.makeMethodNotCompilable(m, CompLevel.C2.getValue(), false);
            }
        }
    }

    private void dontCompileMethod(Method m) {
        WHITE_BOX.makeMethodNotCompilable(m, CompLevel.ANY.getValue(), true);
        WHITE_BOX.makeMethodNotCompilable(m, CompLevel.ANY.getValue(), false);
        WHITE_BOX.testSetDontInlineMethod(m, true);
    }

    private void applyForceCompileCommand(Method m) {
        ForceCompile forceCompileAnno = getAnnotation(m, ForceCompile.class);
        if (forceCompileAnno != null) {
            enqueueMethodForCompilation(m, forceCompileAnno.value());
        }
    }

    static void enqueueMethodForCompilation(Method m, CompLevel compLevel) {
        if (TestFramework.VERBOSE) {
            System.out.println("enqueueMethodForCompilation " + m + ", level = " + compLevel);
        }
        WHITE_BOX.enqueueMethodForCompilation(m, compLevel.getValue());
    }

    private void setupTests(Class<?> clazz) {
        boolean hasIncludeList = includeList != null;
        boolean hasExcludeList = excludeList != null;
        for (Method m : clazz.getDeclaredMethods()) {
            Test testAnno = getAnnotation(m, Test.class);
            if (testAnno != null) {
                if (hasIncludeList && includeList.contains(m.getName()) && (!hasExcludeList || !excludeList.contains(m.getName()))) {
                    addTest(m);
                } else if (hasExcludeList && !excludeList.contains(m.getName())) {
                    addTest(m);
                } else {
                    addTest(m);
                }
            } else {
                TestFormat.check(!m.isAnnotationPresent(IR.class), "Found @IR annotation on non-@Test method " + m);
            }
        }
        if (PRINT_VALID_IR_RULES) {
            irMatchRulePrinter.dump();
        }
    }

    private void addTest(Method m) {
        TestFormat.check(!testMethodMap.containsKey(m.getName()),
                         "Cannot overload two @Test methods " + m + " and " + testMethodMap.get(m.getName()));
        Test testAnno = getAnnotation(m, Test.class);
        TestFormat.check(testAnno != null, m + " must be a method with a @Test annotation");

        Check checkAnno = getAnnotation(m, Check.class);
        Run runAnno = getAnnotation(m, Run.class);
        TestFormat.check(checkAnno == null && runAnno == null,
                         m + " has invalid @Check or @Run annotation while @Test annotation is present.");

        TestFormat.check(!Arrays.asList(m.getParameterTypes()).contains(TestInfo.class),
                         "Forbidden use of " + TestInfo.class + " as parameter at @Test method " + m);

        Warmup warmup = getAnnotation(m, Warmup.class);
        int warmupIterations = WARMUP_ITERATIONS;
        if (warmup != null) {
            warmupIterations = warmup.value();
        }

        boolean osrOnly = getAnnotation(m, OSROnly.class) != null;

        if (PRINT_VALID_IR_RULES) {
            irMatchRulePrinter.emitRuleEncoding(m);
        }
        // Don't inline test methods
        WHITE_BOX.testSetDontInlineMethod(m, true);
        CompLevel compLevel = testAnno.compLevel();
        if (FLIP_C1_C2) {
            compLevel = flipCompLevel(compLevel);
        }
        compLevel = restrictCompLevel(compLevel);
        DeclaredTest test = new DeclaredTest(m, Argument.getArguments(m), compLevel, warmupIterations, osrOnly);
        declaredTests.put(m, test);
        testMethodMap.put(m.getName(), m);
    }


    // Get the appropriate level as permitted by the test scenario and VM options.
    private static CompLevel restrictCompLevel(CompLevel compLevel) {
        if (!USE_COMPILER) {
            return CompLevel.SKIP;
        }
        if (compLevel == CompLevel.ANY) {
            // Use C2 by default.
            return CompLevel.C2;
        }
        if (!TIERED_COMPILATION && compLevel.getValue() < CompLevel.C2.getValue()) {
            return CompLevel.SKIP;
        }
        if (TIERED_COMPILATION && compLevel.getValue() > TIERED_COMPILATION_STOP_AT_LEVEL.getValue()) {
            return CompLevel.SKIP;
        }
        return compLevel;
    }

    private static CompLevel flipCompLevel(CompLevel compLevel) {
        switch (compLevel) {
            case C1, C1_LIMITED_PROFILE, C1_FULL_PROFILE -> {
                return CompLevel.C2;
            }
            case C2 -> {
                return CompLevel.C1;
            }
        }
        return compLevel;
    }

    private void setupCheckAndRunMethods(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            Check checkAnno = getAnnotation(m, Check.class);
            Run runAnno = getAnnotation(m, Run.class);
            Arguments argumentsAnno = getAnnotation(m, Arguments.class);
            TestFormat.check(argumentsAnno == null || (checkAnno == null && runAnno == null),
                             "Cannot have @Argument annotation in combination with @Run or @Check at " + m);
            if (checkAnno != null) {
                addCheckedTest(m, checkAnno, runAnno);
            } else if (runAnno != null) {
                addCustomRunTest(m, runAnno);
            }
        }
    }

    private void addCheckedTest(Method m, Check checkAnno, Run runAnno) {
        TestFormat.check(runAnno == null, m + " has invalid @Run annotation while @Check annotation is present.");
        Method testMethod = testMethodMap.get(checkAnno.test());
        TestFormat.check(testMethod != null,"Did not find associated test method " + checkAnno.test() + " for @Check at " + m);

        boolean firstParameterTestInfo = m.getParameterCount() > 0 && m.getParameterTypes()[0].equals(TestInfo.class);
        boolean secondParameterTestInfo = m.getParameterCount() > 1 && m.getParameterTypes()[1].equals(TestInfo.class);

        CheckedTest.Parameter parameter = null;
        Class<?> testReturnType = testMethod.getReturnType();
        switch (m.getParameterCount()) {
            case 0 -> parameter = CheckedTest.Parameter.NONE;
            case 1 -> {
                TestFormat.check(firstParameterTestInfo || m.getParameterTypes()[0] == testReturnType,
                                 "Single-parameter version of @Check method " + m + " must match return type of @Test " + testMethod);
                parameter = firstParameterTestInfo ? CheckedTest.Parameter.TEST_INFO_ONLY : CheckedTest.Parameter.RETURN_ONLY;
            }
            case 2 -> {
                TestFormat.check(m.getParameterTypes()[0] == testReturnType && secondParameterTestInfo,
                                 "Two-parameter version of @Check method " + m + " must provide as first parameter the same"
                                 + " return type as @Test method " + testMethod + " and as second parameter an object of " + TestInfo.class);
                parameter = CheckedTest.Parameter.BOTH;
            }
            default -> TestFormat.fail("@Check method " + m + " must provide either a none, single or two-parameter variant.");
        }

        if (allTests.containsKey(testMethod)) {
            TestFormat.fail("Method " + m + " and " + allTests.get(testMethod).getTestName()
                            + " cannot both reference test method " + testMethod);
        }

        DeclaredTest test = declaredTests.remove(testMethod);
        TestFormat.check(test != null, "Missing @Test annotation for associated test method " + checkAnno.test() + " for @Check at " + m);
        applyCompileCommands(m);
        // Don't inline check methods
        WHITE_BOX.testSetDontInlineMethod(m, true);
        CheckedTest checkedTest = new CheckedTest(test, m, checkAnno, parameter);
        allTests.put(testMethod, checkedTest);
    }

    private void applyCompileCommands(Method m) {
        ForceInline forceInlineAnno = getAnnotation(m, ForceInline.class);
        DontInline dontInlineAnno = getAnnotation(m, DontInline.class);
        ForceCompile forceCompileAnno = getAnnotation(m, ForceCompile.class);
        DontCompile dontCompileAnno = getAnnotation(m, DontCompile.class);
        if (Stream.of(forceCompileAnno, dontCompileAnno, forceCompileAnno, dontInlineAnno).anyMatch(Objects::nonNull)) {
            applyIndependentCompilationCommands(m);
            applyForceCompileCommand(m);
        } else {
            // Implicitely @DontCompile if nothing specified
            dontCompileMethod(m);
        }
    }

    private void addCustomRunTest(Method m, Run runAnno) {
        Method testMethod = testMethodMap.get(runAnno.test());
        TestFormat.check(testMethod != null, "Did not find associated test method " + runAnno.test() + " for @Run at " + m);
        DeclaredTest test = declaredTests.remove(testMethod);
        TestFormat.check(test != null, "Missing @Test annotation for associated test method " + runAnno.test() + " for @Run at " + m);
        TestFormat.check(!test.hasArguments(), "Invalid @Arguments annotation for associated test method " + runAnno.test() + " for @Run at " + m);
        TestFormat.check(m.getParameterCount() == 0 || (m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(TestInfo.class)),
                         "@Run method " + m + " must specify either no TestInfo parameter or exactly one");
        applyCompileCommands(m);
        // Don't inline run methods
        WHITE_BOX.testSetDontInlineMethod(m, true);
        CustomRunTest customRunTest = new CustomRunTest(test, m, runAnno);
        allTests.put(m, customRunTest);
    }

    private static <T extends Annotation> T getAnnotation(Method m, Class<T> c) {
        T[] annos =  m.getAnnotationsByType(c);
        TestFormat.check(annos.length < 2, m + " has duplicated annotations");
        return Arrays.stream(annos).findFirst().orElse(null);
    }

    private void runTests() {
        TreeMap<Long, String> durations = (PRINT_TIMES || VERBOSE) ? new TreeMap<>() : null;
        long startTime = System.nanoTime();
        Collection<BaseTest> testCollection = allTests.values();
        if (SHUFFLE_TESTS) {
            // Execute tests in random order (execution sequence affects profiling)
            ArrayList<BaseTest> shuffledList = new ArrayList<>(allTests.values());
            Collections.shuffle(shuffledList);
            testCollection = shuffledList;
        }
        for (BaseTest test : testCollection) {
            test.run();
            if (PRINT_TIMES || VERBOSE) {
                long endTime = System.nanoTime();
                long duration = (endTime - startTime);
                durations.put(duration, test.getTestName());
                if (VERBOSE) {
                    System.out.println("Done " + test.getTestName() + ": " + duration + " ns = " + (duration / 1000000) + " ms");
                }
            }
            if (GC_AFTER) {
                System.out.println("doing GC");
                System.gc();
            }
        }

        // Print execution times
        if (PRINT_TIMES) {
            System.out.println("\n\nTest execution times:");
            for (Map.Entry<Long, String> entry : durations.entrySet()) {
                System.out.format("%-10s%15d ns\n", entry.getValue() + ":", entry.getKey());
            }
        }
    }

    static void check(boolean test, String failureMessage) {
        if (!test) {
            throw new TestFrameworkException("Internal TestFramework exception:\n" + failureMessage);
        }
    }

    enum TriState {
        Maybe,
        Yes,
        No
    }

    private static TriState compiledByC2(Method m) {
//        if (!TestFramework.USE_COMPILER || TestFramework.XCOMP || TestFramework.TEST_C1 ||
//                (TestFramework.STRESS_CC && !WHITE_BOX.isMethodCompilable(m, CompLevel.C2.getValue(), false))) {
//            return TriState.Maybe;
//        }
        if (WHITE_BOX.isMethodCompiled(m, false)
            && WHITE_BOX.getMethodCompilationLevel(m, false) >= CompLevel.C2.getValue()) {
            return TriState.Yes;
        }
        return TriState.No;
    }
}

// Errors in the framework
class TestFrameworkException extends RuntimeException {
    public TestFrameworkException(String message) {
        super(message);
    }
    public TestFrameworkException(String message, Exception e) {
        super(message, e);
    }
}

class DeclaredTest {
    private final Method testMethod;
    private final Argument[] arguments;
    private final int warmupIterations;
    private final CompLevel compLevel;
    private final boolean osrOnly;

    public DeclaredTest(Method testMethod, Argument[] arguments, CompLevel compLevel, int warmupIterations, boolean osrOnly) {
        // Make sure we can also call non-public or public methods in package private classes
        testMethod.setAccessible(true);
        this.testMethod = testMethod;
        this.compLevel = compLevel;
        this.arguments = arguments;
        this.warmupIterations = warmupIterations;
        this.osrOnly = osrOnly;
    }

    public Method getTestMethod() {
        return testMethod;
    }

    public CompLevel getCompLevel() {
        return compLevel;
    }

    public int getWarmupIterations() {
        return warmupIterations;
    }

    public boolean isOSROnly() {
        return osrOnly;
    }

    public boolean hasArguments() {
        return arguments != null;
    }

    public Object[] getArguments() {
        return Arrays.stream(arguments).map(Argument::getArgument).toArray();
    }

    public void printFixedRandomArguments() {
        if (hasArguments()) {
            boolean hasRandomArgs = false;
            StringBuilder builder = new StringBuilder("Random Arguments: ");
            for (int i = 0; i < arguments.length; i++) {
                Argument argument = arguments[i];
                if (argument.isFixedRandom()) {
                    hasRandomArgs = true;
                    builder.append("arg ").append(i).append(": ").append(argument.getArgument()).append(", ");
                }
            }
            if (hasRandomArgs) {
                // Drop the last comma and space.
                builder.setLength(builder.length() - 2);
                System.out.println(builder.toString());
            }
        }
    }

    public Object invoke(Object obj, Object... args) {
        try {
            return testMethod.invoke(obj, args);
        } catch (Exception e) {
            throw new TestRunException("There was an error while invoking @Test method " + testMethod, e);
        }
    }
}

class BaseTest {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int OSR_TEST_TIMEOUT = Integer.parseInt(System.getProperty("OSRTestTimeOut", "5000"));

    protected final DeclaredTest test;
    protected final Method testMethod;
    protected final TestInfo testInfo;
    protected final Object invocationTarget;

    public BaseTest(DeclaredTest test) {
        this.test = test;
        this.testMethod = test.getTestMethod();
        this.testInfo = new TestInfo(testMethod);
        Class<?> clazz = testMethod.getDeclaringClass();
        if (Modifier.isStatic(testMethod.getModifiers())) {
            this.invocationTarget = null;
        } else {
            try {
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                this.invocationTarget = constructor.newInstance();
            } catch (Exception e) {
                throw new TestRunException("Could not create instance of " + clazz
                                                   + ". Make sure there is a constructor without arguments.", e);
            }
        }
    }

    public String getTestName() {
        return testMethod.getName();
    }

    /**
     * Run the associated test
     */
    public void run() {
        if (test.getCompLevel() == CompLevel.SKIP) {
            // Exclude test if compilation level is SKIP either set through test or by not matching the current VM flags.
            return;
        }
        test.printFixedRandomArguments();
        for (int i = 0; i < test.getWarmupIterations(); i++) {
            runMethod();
        }
        testInfo.setWarmUpFinished();

        if (test.isOSROnly()) {
            compileOSRAndRun();
        } else {
            compileNormallyAndRun();
        }
    }

    protected void runMethod() {
        verify(invokeTestMethod());
    }

    private Object invokeTestMethod() {
        try {
            if (test.hasArguments()) {
                return testMethod.invoke(invocationTarget, test.getArguments());
            } else {
                return testMethod.invoke(invocationTarget);
            }
        } catch (Exception e) {
            throw new TestRunException("There was an error while invoking @Test method " + testMethod, e);
        }
    }

    private void compileOSRAndRun() {
        final boolean maybeCodeBufferOverflow = (TestFramework.TEST_C1 && TestFramework.VERIFY_OOPS);
        final long started = System.currentTimeMillis();
        boolean stateCleared = false;
        while (true) {
            long elapsed = System.currentTimeMillis() - started;
            int level = WHITE_BOX.getMethodCompilationLevel(testMethod);
            if (maybeCodeBufferOverflow && elapsed > 5000
                    && (!WHITE_BOX.isMethodCompiled(testMethod, false) || level != test.getCompLevel().getValue())) {
                retryDisabledVerifyOops(stateCleared);
                stateCleared = true;
            } else {
                runMethod();
            }

            boolean b = WHITE_BOX.isMethodCompiled(testMethod, false);
            if (TestFramework.VERBOSE) {
                System.out.println("Is " + testMethod + " compiled? " + b);
            }
            if (b || TestFramework.XCOMP || TestFramework.STRESS_CC || !TestFramework.USE_COMPILER) {
                // Don't control compilation if -Xcomp is enabled, or if compiler is disabled
                break;
            }
            Asserts.assertTrue(OSR_TEST_TIMEOUT < 0 || elapsed < OSR_TEST_TIMEOUT, testMethod + " not compiled after " + OSR_TEST_TIMEOUT + " ms");
        }
    }

    private void retryDisabledVerifyOops(boolean stateCleared) {
        System.out.println("Temporarily disabling VerifyOops");
        try {
            WHITE_BOX.setBooleanVMFlag("VerifyOops", false);
            if (!stateCleared) {
                WHITE_BOX.clearMethodState(testMethod);
            }
            runMethod();
        } finally {
            WHITE_BOX.setBooleanVMFlag("VerifyOops", true);
            System.out.println("Re-enabled VerifyOops");
        }
    }

    private void compileNormallyAndRun() {
        final boolean maybeCodeBufferOverflow = (TestFramework.TEST_C1 && TestFramework.VERIFY_OOPS);
        final Method testMethod = test.getTestMethod();
        enqueueMethodForCompilation();
        if (maybeCodeBufferOverflow && !WHITE_BOX.isMethodCompiled(testMethod, false)) {
            // Let's disable VerifyOops temporarily and retry.
            WHITE_BOX.setBooleanVMFlag("VerifyOops", false);
            WHITE_BOX.clearMethodState(testMethod);
            enqueueMethodForCompilation();
            WHITE_BOX.setBooleanVMFlag("VerifyOops", true);
        }
        if (!TestFramework.STRESS_CC && TestFramework.USE_COMPILER) {
            for (int i = 0; !WHITE_BOX.isMethodCompiled(testMethod, false) && i < 10 ; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    TestFormat.fail("Error while waiting for compilation to be completed of " + testMethod);
                }
            }
            Asserts.assertTrue(WHITE_BOX.isMethodCompiled(testMethod, false), testMethod + " not compiled after waiting 1s");
            checkCompilationLevel();
        }
        runMethod();
    }

    private  void enqueueMethodForCompilation() {
        TestFramework.enqueueMethodForCompilation(test.getTestMethod(), test.getCompLevel());
    }

    protected void checkCompilationLevel() {
        CompLevel level = CompLevel.forValue(WhiteBox.getWhiteBox().getMethodCompilationLevel(testMethod));
        TestRun.check(level == test.getCompLevel(),
                      "Compilation level should be " + test.getCompLevel().name() + " (requested) but was " + level.name() + " for " + testMethod);
    }

    /**
     * Verify the result
     */
    public void verify(Object result) { /* no verification in BaseTests */ }
}

class CheckedTest extends BaseTest {
    private final Method checkMethod;
    private final CheckAt checkAt;
    private final Parameter parameter;

    enum Parameter {
        NONE, RETURN_ONLY, TEST_INFO_ONLY, BOTH
    }

    public CheckedTest(DeclaredTest test, Method checkMethod, Check checkSpecification, Parameter parameter) {
        super(test);
        // Make sure we can also call non-public or public methods in package private classes
        checkMethod.setAccessible(true);
        this.checkMethod = checkMethod;
        this.checkAt = checkSpecification.when();
        this.parameter = parameter;
    }

    @Override
    public void verify(Object result) {
        boolean shouldVerify = false;
        switch (checkAt) {
            case EACH_INVOCATION -> shouldVerify = true;
            case COMPILED -> shouldVerify = !testInfo.isWarmUp();
        }
        if (shouldVerify) {
            try {
                switch (parameter) {
                    case NONE -> checkMethod.invoke(invocationTarget);
                    case RETURN_ONLY -> checkMethod.invoke(invocationTarget, result);
                    case TEST_INFO_ONLY -> checkMethod.invoke(invocationTarget, testInfo);
                    case BOTH -> checkMethod.invoke(invocationTarget, result, testInfo);
                }
            } catch (Exception e) {
                throw new TestRunException("There was an error while invoking @Check method " + checkMethod, e);
            }
        }
    }
}

class CustomRunTest extends BaseTest {
    private final Method runMethod;
    private final RunMode mode;

    public CustomRunTest(DeclaredTest test, Method runMethod, Run runSpecification) {
        super(test);
        // Make sure we can also call non-public or public methods in package private classes
        runMethod.setAccessible(true);
        this.runMethod = runMethod;
        this.mode = runSpecification.mode();
    }

    @Override
    public void run() {
        switch (mode) {
            case ONCE -> runMethod();
            case NORMAL -> super.run();
        }
    }

    /**
     * Do not directly run the test but rather the run method that is responsible for invoking the actual test.
     */
    @Override
    protected void runMethod() {
        try {
            if (runMethod.getParameterCount() == 1) {
                runMethod.invoke(invocationTarget, testInfo);
            } else {
                runMethod.invoke(invocationTarget);
            }
        } catch (Exception e) {
            throw new TestRunException("There was an error while invoking @Run method " + runMethod, e);
        }
    }

    @Override
    protected void checkCompilationLevel() {
        CompLevel level = CompLevel.forValue(WhiteBox.getWhiteBox().getMethodCompilationLevel(testMethod));
        if (level != test.getCompLevel()) {
            String message = "Compilation level should be " + test.getCompLevel().name() + " (requested) but was " + level.name() + " for " + testMethod + ".";
            switch (mode) {
                case ONCE -> message = message + "\nCheck your @Run method (invoked once) " + runMethod + " to ensure that " + testMethod + " will be complied at the requested level.";
                case NORMAL -> message = message + "\nCheck your @Run method " + runMethod + " to ensure that " + testMethod + " is called at least once in each iteration.";
            }
            TestRun.fail(message);
        }
    }
}


class ParsedComparator<T extends Comparable<T>>  {
    private final String strippedString;
    private final BiPredicate<T, T> predicate;

    public ParsedComparator(String strippedString, BiPredicate<T, T> predicate) {
        this.strippedString = strippedString;
        this.predicate = predicate;
    }

    public String getStrippedString() {
        return strippedString;
    }

    public BiPredicate<T, T> getPredicate() {
        return predicate;
    }


    public static <T extends Comparable<T>> ParsedComparator<T> parseComparator(String value) {
        BiPredicate<T, T> comparison = null;
        value = value.trim();
        try {
            switch (value.charAt(0)) {
                case '<':
                    if (value.charAt(1) == '=') {
                        comparison = (x, y) -> x.compareTo(y) <= 0;
                        value = value.substring(2).trim();
                    } else {
                        comparison = (x, y) -> x.compareTo(y) < 0;
                        value = value.substring(1).trim();
                    }
                    break;
                case '>':
                    if (value.charAt(1) == '=') {
                        comparison = (x, y) -> x.compareTo(y) >= 0;
                        value = value.substring(2).trim();
                    } else {
                        comparison = (x, y) -> x.compareTo(y) > 0;
                        value = value.substring(1).trim();
                    }
                    break;
                case '!':
                    TestFormat.check(value.charAt(1) == '=', "Invalid comparator sign used.");
                    comparison = (x, y) -> x.compareTo(y) != 0;
                    value = value.substring(2).trim();
                    break;
                case '=': // Allowed syntax, equivalent to not using any symbol.
                    value = value.substring(1).trim();
                default:
                    comparison = (x, y) -> x.compareTo(y) == 0;
                    value = value.trim();
                    break;
            }
        } catch (IndexOutOfBoundsException e) {
            TestFormat.fail("Invalid value format.");
        }
        return new ParsedComparator<>(value, comparison);
    }
}

package compiler.valhalla.framework;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.test.lib.Asserts;
import jdk.test.lib.Utils;
import jdk.test.lib.management.InputArguments;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;
import jdk.test.lib.Platform;


public class TestFramework {
    public static final int DEFAULT_SCENARIOS = 6;
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

    static final boolean TIERED_COMPILATION = (Boolean)WHITE_BOX.getVMFlag("TieredCompilation");
    static final CompLevel TIERED_COMPILATION_STOP_AT_LEVEL = CompLevel.forValue(((Long)WHITE_BOX.getVMFlag("TieredStopAtLevel")).intValue());
    static final boolean TEST_C1 = TIERED_COMPILATION && TIERED_COMPILATION_STOP_AT_LEVEL.getValue() < Skip.C2_FULL_OPTIMIZATION.getValue();

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
    private static final String SCENARIOS = System.getProperty("Scenarios", "");
    private static final String TESTLIST = System.getProperty("Testlist", "");
    private static final String EXCLUDELIST = System.getProperty("Exclude", "");
    public static final int WARMUP_ITERATIONS = Integer.parseInt(System.getProperty("Warmup", "251"));
    private static final boolean DUMP_REPLAY = Boolean.parseBoolean(System.getProperty("DumpReplay", "false"));
    private static final boolean GC_AFTER = Boolean.parseBoolean(System.getProperty("GCAfter", "false"));
    static final boolean STRESS_CC = Boolean.parseBoolean(System.getProperty("StressCC", "false"));
    private static final boolean SHUFFLE_TESTS = Boolean.parseBoolean(System.getProperty("ShuffleTests", "true"));
    private static final boolean PREFER_CL_FLAGS = Boolean.parseBoolean(System.getProperty("PreferCommandLineFlags", "false"));
    private static final boolean USE_COMPILE_COMMAND_ANNOTATIONS = Boolean.parseBoolean(System.getProperty("UseCompileCommandAnnotations", "true"));
    private static final boolean PRINT_VALID_IR_RULES = Boolean.parseBoolean(System.getProperty("PrintValidIRRules", "false"));

    // "jtreg -DXcomp=true" runs all the scenarios with -Xcomp. This is faster than "jtreg -javaoptions:-Xcomp".
    static final boolean RUN_WITH_XCOMP = Boolean.parseBoolean(System.getProperty("Xcomp", "false"));

    private final String[] fixedDefaultFlags;
    private final String[] compileCommandFlags;
    private final String[] printFlags;
    private final String[] verifyFlags;
    private static String lastVmOutput; // Only used to test TestFramework

    protected String[] setupDefaultFlags() {
        return new String[] {"-XX:-BackgroundCompilation"};
    }

    protected String[] setupCompileCommandFlags() {
        return new String[] {"-XX:CompileCommand=quiet"};
    }

    protected String[] setupPrintFlags() {
        return new String[] {"-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions"};
    }

    protected String[] setupVerifyFlags() {
        return new String[] {
                "-XX:+UnlockDiagnosticVMOptions", "-XX:+VerifyOops", "-XX:+VerifyStack", "-XX:+VerifyLastFrame", "-XX:+VerifyBeforeGC",
                "-XX:+VerifyAfterGC", "-XX:+VerifyDuringGC", "-XX:+VerifyAdapterSharing"};
    }

    static final boolean USE_COMPILER = WHITE_BOX.getBooleanVMFlag("UseCompiler");
    static final boolean PRINT_IDEAL  = WHITE_BOX.getBooleanVMFlag("PrintIdeal");

    static final boolean G1GC = (Boolean)WHITE_BOX.getVMFlag("UseG1GC");
    static final boolean ZGC = (Boolean)WHITE_BOX.getVMFlag("UseZGC");
    static final boolean VerifyOops = (Boolean)WHITE_BOX.getVMFlag("VerifyOops");

    private final HashMap<Method, DeclaredTest> declaredTests = new HashMap<>();
    private final LinkedHashMap<Method, BaseTest> allTests = new LinkedHashMap<>(); // Keep order
    private final HashMap<String, Method> testMethodMap = new HashMap<>();

    // Index into this array is the scenario ID.
    protected final List<Scenario> scenarios = new ArrayList<>();

    private final IREncodingPrinter irMatchRulePrinter;

    // Used to run scenarios
    public TestFramework() {
        // These flags can be overridden
        fixedDefaultFlags = setupDefaultFlags();
        compileCommandFlags = setupCompileCommandFlags();
        printFlags = setupPrintFlags();
        verifyFlags = setupVerifyFlags();
        setupDefaultScenarios();
        if (PRINT_VALID_IR_RULES) {
            irMatchRulePrinter = new IREncodingPrinter();
        } else {
            irMatchRulePrinter = null;
        }
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

        ArrayList<Class<?>> helperClasses = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            String helperClassName = args[i];
            try {
                helperClasses.add(Class.forName(helperClassName));
            } catch (Exception e) {
                throw new TestRunException("Could not find helper class " + helperClassName, e);
            }
        }
        TestFramework framework = new TestFramework();
        framework.runTestsOnSameVM(testClass, helperClasses);
    }

    public static void run() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        doRun(walker.getCallerClass(), null, null);
    }

    public static void run(Class<?> testClass) {
        doRun(testClass, null, null);
    }

    public static void run(Class<?> testClass, Class<?>... helperClasses) {
        doRun(testClass, Arrays.asList(helperClasses), null);
    }

    public static void runWithArguments(String... commandLineArgs) {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        doRun(walker.getCallerClass(), null, Arrays.asList(commandLineArgs));
    }

    public static void runWithArguments(Class<?> testClass, String... commandLineArgs) {
        doRun(testClass, null, Arrays.asList(commandLineArgs));
    }

    private static void doRun(Class<?> testClass, List<Class<?>> helperClasses, List<String> commandLineArgs) {
        TestFramework framework = new TestFramework();
        framework.startTestVM(null, testClass, helperClasses, commandLineArgs);
    }

    public void runTestsOnSameVM() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        runTestsOnSameVM(walker.getCallerClass());
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

    private String startTestVM(ArrayList<String> scenarioFlags, Class<?> testClass, List<Class<?>> helperClasses,
                               List<String> commandLineArgs) {
        ArrayList<String> cmds = new ArrayList<>(Arrays.asList(InputArguments.getVmInputArgs()));
        if (RUN_WITH_XCOMP) {
            cmds.add( "-Xcomp");
        }

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
        
        if (VERIFY_VM) {
            cmds.addAll(Arrays.asList(verifyFlags));
        }

        cmds.addAll(Arrays.asList(fixedDefaultFlags));
        if (COMPILE_COMMANDS) {
            cmds.addAll(Arrays.asList(compileCommandFlags));
        }

        if (scenarioFlags != null) {
            cmds.addAll(scenarioFlags);
        }

        if (commandLineArgs != null) {
            // Ensured to be always set. Useful for testing the framework itself, for example @IR behavior.
            cmds.addAll(commandLineArgs);
        }

        // TODO: Only for debugging
        if (cmds.get(0).startsWith("-agentlib")) {
            cmds.set(0, "-agentlib:jdwp=transport=dt_socket,address=127.0.0.1:44444,suspend=n,server=y");
        }
        cmds.add(getClass().getCanonicalName());
        cmds.add(testClass.getCanonicalName());
        if (helperClasses != null) {
            helperClasses.forEach(c -> cmds.add(c.getCanonicalName()));
        }

        if (VERBOSE) {
            System.out.print("Command Line: ");
            cmds.forEach(flag -> System.out.print(flag + " "));
        }

        OutputAnalyzer oa;
        try {
            // Calls this 'main' of this class to run all specified tests with the flags specified by the scenario.
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
        oa.shouldHaveExitValue(0);

        if (VERIFY_IR) {
            IRMatcher irMatcher = new IRMatcher(output, testClass);
            irMatcher.applyRules();
        }
        return output;
    }

    public static String getLastVmOutput() {
        return lastVmOutput;
    }
    private void addBoolOptionForClass(ArrayList<String> cmds, Class<?> testClass, String option) {
        cmds.add("-XX:CompileCommand=option," + testClass.getCanonicalName() + "::*,bool," + option + ",true");
    }

//    public static void runDefaultScenarios() {
//        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
//        Class<?> testClass = walker.getCallerClass();
//        TestFramework frameWork = new TestFramework();
//        frameWork.setupDefaultScenarios();
//        frameWork.runScenarios(testClass);
//    }

    public void runScenarios() {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        runScenarios(walker.getCallerClass());
    }

    private void runScenarios(Class<?> testClass, Class<?>... helperClasses) {
        if (!SCENARIOS.isEmpty()) {
            setupFlagDefinedScenarios();
        }

        for (int i = 0; i < scenarios.size(); i++) {
            Scenario scenario = scenarios.get(i);
            if (scenario.isIgnored()) {
                System.out.println("Scenario #" + i + " is ignored. Reason:" + scenario.getIgnoreReason());
            }
            System.out.println("Run Scenario #" + i + " -------- ");
            ArrayList<String> scenarioFlags = new ArrayList<>(scenario.getFlags());
            scenario.setOutput(startTestVM(scenarioFlags, testClass, Arrays.asList(helperClasses), null));
        }
    }

    private void setupFlagDefinedScenarios() {
        List<Integer> flagDefinedScenarios = Arrays.stream(SCENARIOS.split("\\s*,\\s*")).map(Integer::getInteger).sorted().collect(Collectors.toList());
        scenarios.forEach(s -> s.disable("Disabled by -Dscenarios"));
        int lastIndex = flagDefinedScenarios.get(flagDefinedScenarios.size() - 1);
        for (int scenarioId : flagDefinedScenarios) {
            if (scenarioId >= scenarios.size()) {
                System.out.println("Scenario #" + scenarioId + " does not exist.");
                continue;
            }
            Scenario scenario = scenarios.get(scenarioId);
            if (scenario.isIgnored()) {
                continue;
            }

            scenario.enable();
        }

        // All remaining flag defined scenarios are invalid
    }

    public void addScenario(Scenario scenario) {
        scenarios.add(scenario);
    }

    public void disableDefaultScenario(int scenarioId) {
        checkScenarioId(scenarioId);
        scenarios.get(scenarioId).disable();
    }

    public void disableDefaultScenario(int scenarioId, String reason) {
        checkScenarioId(scenarioId);
        scenarios.get(scenarioId).disable(reason);
    }

    public void replaceDefaultScenario(Scenario newScenario, int scenarioId) {
        checkScenarioId(scenarioId);
        scenarios.set(scenarioId, newScenario);
    }

    public void replaceDefaultScenarios(Scenario[] newScenarios) {
        scenarios.clear();
        scenarios.addAll(Arrays.asList(newScenarios));
    }

    public void addFlagsToDefaultScenario(int scenarioId, String... flags) {
        checkScenarioId(scenarioId);
        Arrays.stream(flags).forEach(f -> scenarios.get(scenarioId).addFlag(f));
    }

    public String getScenarioOutput(int scenarioId) {
        checkScenarioId(scenarioId);
        return scenarios.get(scenarioId).getOutput();
    }

    private void checkScenarioId(int scenarioId) {
        if (scenarioId < 0 || scenarioId >= scenarios.size()) {
            throw new TestFormatException("Invalid default scenario id " + scenarioId +  ". Must be in [0, " + (scenarios.size() - 1) + "].");
        }
    }

    // Can be overridden, for example by Valhalla
    protected void setupDefaultScenarios() {
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-XX:+IgnoreUnrecognizedVMOptions"))));
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-XX:CompileCommand=quiet"))));
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-Xmx256m"))));
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-DVerifyIR=false"))));
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-XX:+StressGCM"))));
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-XX:+StressLCM"))));
    }

    private void parseTestClass(Class<?> clazz) {
        addReplay();
        processExplicitCompileCommands(clazz);
        setupTestMethodMap(clazz);
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
            if (WHITE_BOX.addCompilerDirective(directive) != 1) {
                throw new TestFormatException("Failed to add DUMP_REPLAY directive");
            }
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
        if (testAnno != null && Stream.of(forceCompileAnno, dontCompileAnno, forceInlineAnno, dontInlineAnno).anyMatch(Objects::nonNull)) {
            throw new TestFormatException("Not allowed to use explicit compile command annotations (@ForceInline, @DontInline," +
                    "@ForceCompile or @DontCompile) together with @Test at " + m + ". Use compLevel and skip in @Test for fine tuning.");
        }
        if (Stream.of(forceCompileAnno, dontCompileAnno, dontInlineAnno).filter(Objects::nonNull).count() > 1) {
            if (dontCompileAnno != null && dontInlineAnno != null) {
                throw new TestFormatException("@DontInline is implicitely done with @DontCompile annotation at " + m);
            }
            throw new TestFormatException("Cannot mix @ForceInline, @DontInline and @DontCompile at the same time at " + m);
        }
        if (forceCompileAnno != null && dontCompileAnno != null) {
            throw new TestFormatException("Cannot have @ForceCompile and @DontCompile at the same time at " + m);
        }
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
                WHITE_BOX.makeMethodNotCompilable(m, CompLevel.C2_FULL_OPTIMIZATION.getValue(), false);
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
            enqueueMethodForCompilation(m, forceCompileAnno.compLevel());
        }
    }

    // Can be called from tests for non-@Test methods
    public static void enqueueMethodForCompilation(Method m, CompLevel compLevel) {
        if (getAnnotation(m, Test.class) != null) {
            throw new TestFormatException("Cannot call enqueueMethodForCompilation() for @Test annotated method " + m);
        }
        TestFrameworkUtils.enqueueMethodForCompilation(m ,compLevel);
    }

    private void setupTestMethodMap(Class<?> clazz) {
        Arrays.stream(clazz.getDeclaredMethods()).forEach(m -> {
            Test testAnno = getAnnotation(m, Test.class);
            if (testAnno != null) {
                if (testMethodMap.containsKey(m.getName())) {
                    throw new TestFormatException("Cannot overload two @Test methods " + m + " and " + testMethodMap.get(m.getName()));
                }
                testMethodMap.put(m.getName(), m);
            } else {
                if (m.isAnnotationPresent(IR.class)) {
                    throw new TestFormatException("Found @IR annotation on non-@Test method " + m);
                }
            }
        });
    }

    private void setupTests(Class<?> clazz) {
        for (Method m : testMethodMap.values()) {
            Argument[] arguments = Argument.getArguments(m);
            addTest(clazz, m, arguments);
        }
        if (PRINT_VALID_IR_RULES) {
            irMatchRulePrinter.dump();
        }
    }

    private void addTest(Class<?> clazz, Method m, Argument[] arguments) {
        Test testAnno = getAnnotation(m, Test.class);
        if (testAnno == null) {
            throw new TestFormatException(m + " must be a method with a @Test annotation");
        }

        Check checkAnno = getAnnotation(m, Check.class);
        Run runAnno = getAnnotation(m, Run.class);
        if (checkAnno != null || runAnno != null) {
            throw new TestFormatException(m.getName() + " has invalid @compiler.valhalla.new_inlinetypes.Check or @compiler.valhalla.new_inlinetypes.Run annotation while @compiler.valhalla.new_inlinetypes.Test annotation is present.");
        }

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
        DeclaredTest test = new DeclaredTest(m, testAnno, arguments, clazz, warmupIterations, osrOnly);
        declaredTests.put(m, test);
    }

    private void setupCheckAndRunMethods(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            Check checkAnno = getAnnotation(m, Check.class);
            Run runAnno = getAnnotation(m, Run.class);

            if (checkAnno != null) {
                addCheckedTest(m, checkAnno, runAnno);
            } else if (runAnno != null) {
                addCustomRunTest(m, runAnno);
            }
        }
    }

    private void addCheckedTest(Method m, Check checkAnno, Run runAnno) {
        if (runAnno != null) {
            throw new TestFormatException(m.getName() + " has invalid @compiler.valhalla.new_inlinetypes.Run annotation while @compiler.valhalla.new_inlinetypes.Check annotation is present.");
        }
        Method testMethod = testMethodMap.get(checkAnno.test());
        if (testMethod == null) {
            throw new TestFormatException("Did not find associated test method " + checkAnno.test() + " for @compiler.valhalla.new_inlinetypes.Check at " + m.getName());
        }
        if (allTests.containsKey(testMethod)) {
            BaseTest baseTest = allTests.get(testMethod);
            throw new TestFormatException("Method " + m.getName() + " and " + baseTest.getAssociatedTestName() +
                    " cannot both reference test method " + testMethod.getName());
        }
        DeclaredTest test = declaredTests.remove(testMethod);
        if (test == null) {
            throw new TestFormatException("Missing @compiler.valhalla.new_inlinetypes.Test annotation for associated test method " + checkAnno.test() + " for @compiler.valhalla.new_inlinetypes.Check at " + m.getName());
        }
        applyCompileCommands(m);
        // Don't inline check methods
        WHITE_BOX.testSetDontInlineMethod(m, true);
        CheckedTest checkedTest = new CheckedTest(test, m, checkAnno);
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
        if (testMethod == null) {
            throw new TestFormatException("Did not find associated test method " + runAnno.test() + " for @Run at " + m.getName());
        }
        DeclaredTest test = declaredTests.remove(testMethod);
        if (test == null) {
            throw new TestFormatException("Missing @Test annotation for associated test method " + runAnno.test() + " for @Run at " + m.getName());
        }
        if (test.hasArguments()) {
            throw new TestFormatException("Invalid @Arguments annotation for associated test method " + runAnno.test() + " for @Run at " + m.getName());
        }
        if (m.getParameterCount() != 1 || !m.getParameterTypes()[0].equals(TestInfo.class)) {
            throw new TestFormatException("@Run method " + m.getName() + " must specify exactly one TestInfo parameter");
        }
        applyCompileCommands(m);
        // Don't inline run methods
        WHITE_BOX.testSetDontInlineMethod(m, true);
        CustomRunTest customRunTest = new CustomRunTest(test, m, runAnno);
        allTests.put(m, customRunTest);
    }

    public static <T extends Annotation> T getAnnotation(Method m, Class<T> c) {
        T[] annos =  m.getAnnotationsByType(c);
        if (annos.length > 1) {
            throw new TestFormatException(m + " has duplicated annotations");
        }
        return Arrays.stream(annos).findFirst().orElse(null);
    }

    public void runTests() {
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
                durations.put(duration, test.getAssociatedTestName());
                if (VERBOSE) {
                    System.out.println("Done " + test.getAssociatedTestName() + ": " + duration + " ns = " + (duration / 1000000) + " ms");
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

    enum TriState {
        Maybe,
        Yes,
        No
    }

    public static boolean isC2Compiled(Method m) {
        return compiledByC2(m) == TriState.Yes;
    }

    public static void assertDeoptimizedByC2(Method m) {
        if (compiledByC2(m) == TriState.Yes) {
            throw new TestRunException("Expected to have deoptimized");
        }
    }

    public static void assertCompiledByC2(Method m) {
        if (compiledByC2(m) == TriState.No) {
            throw new TestRunException("Expected to be compiled");
        }
    }

    private static TriState compiledByC2(Method m) {
        if (!USE_COMPILER || XCOMP || TEST_C1 ||
                (STRESS_CC && !WHITE_BOX.isMethodCompilable(m, CompLevel.C2_FULL_OPTIMIZATION.getValue(), false))) {
            return TriState.Maybe;
        }
        if (WHITE_BOX.isMethodCompiled(m, false) &&
                WHITE_BOX.getMethodCompilationLevel(m, false) >= CompLevel.C2_FULL_OPTIMIZATION.getValue()) {
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
}

class DeclaredTest {
    private final Method testMethod;
    public Method getTestMethod() {
        return testMethod;
    }

    private final Argument[] arguments;
    private final Object invocationTarget;
    private final int warmupIterations;
    private final CompLevel requestedCompLevel;
    private final boolean osrOnly;

    public DeclaredTest(Method testMethod, Test testAnnotation, Argument[] arguments, Class<?> c, int warmupIterations, boolean osrOnly) {
        // Make sure we can also call non-public or public methods in package private classes
        testMethod.setAccessible(true);
        this.testMethod = testMethod;
        this.requestedCompLevel = testAnnotation.compLevel();
        this.arguments = arguments;
        this.warmupIterations = warmupIterations;
        this.osrOnly = osrOnly;
        if (Modifier.isStatic(testMethod.getModifiers())) {
            invocationTarget = null;
        } else {
            try {
                Constructor<?> constructor = c.getDeclaredConstructor();
                constructor.setAccessible(true);
                invocationTarget = constructor.newInstance();
            } catch (Exception e) {
                throw new TestRunException("Could not create instance of " + c
                        + ". Make sure there is a constructor without arguments.", e);
            }
        }
    }

    public CompLevel getRequestedCompLevel() {
        return requestedCompLevel;
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

    public Object invokeWithSpecifiedArguments() {
        try {
            if (hasArguments()) {
                Object[] args = Arrays.stream(arguments).map(Argument::getArgument).toArray();
                return testMethod.invoke(invocationTarget, args);
            } else {
                return testMethod.invoke(invocationTarget);
            }
        } catch (Exception e) {
            throw new TestRunException("There was an error while invoking @Test method " + testMethod, e);
        }
    }

    public Object invoke(Object obj, Object... args) {
        try {
            return testMethod.invoke(obj, args);
        } catch (Exception e) {
            throw new TestRunException("There was an error while invoking @Test method " + testMethod, e);
        }
    }

    public Object getInvocationTarget() {
        return invocationTarget;
    }

    public void checkCompilationLevel() {
        int level = WhiteBox.getWhiteBox().getMethodCompilationLevel(testMethod);
        Asserts.assertEQ(level, TestFrameworkUtils.compLevelToInt(requestedCompLevel), "Unexpected compilation level for " + testMethod);
    }
}

class BaseTest {
    protected static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final int OSR_TEST_TIMEOUT = Integer.parseInt(System.getProperty("OSRTestTimeOut", "5000"));

    protected final DeclaredTest test;
    protected final TestInfo testInfo;

    public BaseTest(DeclaredTest test) {
        this.test = test;
        this.testInfo = new TestInfo();
    }
    public String getAssociatedTestName() {
        return test.getTestMethod().getName();
    }

    /**
     * Run the associated test
     */
    public void run() {
//        ByteArrayOutputStream systemOutStream = new ByteArrayOutputStream();
//        PrintStream ps = new PrintStream(systemOutStream);
//        PrintStream old = System.out;
//        System.setOut(ps);

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
//        System.setOut(old);
//        System.out.print(systemOutStream.toString());
    }

    protected void runMethod() {
        verify(testInfo, test.invokeWithSpecifiedArguments());
    }

    protected void compileOSRAndRun() {
        final boolean maybeCodeBufferOverflow = (TestFramework.TEST_C1 && TestFramework.VerifyOops);
        final long started = System.currentTimeMillis();
        boolean stateCleared = false;
        while (true) {
            long elapsed = System.currentTimeMillis() - started;
            Method testMethod = test.getTestMethod();
            int level = WHITE_BOX.getMethodCompilationLevel(testMethod);
            if (maybeCodeBufferOverflow && elapsed > 5000
                    && (!WHITE_BOX.isMethodCompiled(testMethod, false) || level != test.getRequestedCompLevel().getValue())) {
                System.out.println("Temporarily disabling VerifyOops");
                try {
                    WHITE_BOX.setBooleanVMFlag("VerifyOops", false);
                    if (!stateCleared) {
                        WHITE_BOX.clearMethodState(testMethod);
                        stateCleared = true;
                    }
                    runMethod();
                } finally {
                    WHITE_BOX.setBooleanVMFlag("VerifyOops", true);
                    System.out.println("Re-enabled VerifyOops");
                }
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
            Asserts.assertTrue(OSR_TEST_TIMEOUT < 0 || elapsed < OSR_TEST_TIMEOUT, test + " not compiled after " + OSR_TEST_TIMEOUT + " ms");
        }
    }

    private void compileNormallyAndRun() {
        final boolean maybeCodeBufferOverflow = (TestFramework.TEST_C1 && TestFramework.VerifyOops);
        final Method testMethod = test.getTestMethod();
        TestFrameworkUtils.enqueueMethodForCompilation(test);
        if (maybeCodeBufferOverflow && !WHITE_BOX.isMethodCompiled(testMethod, false)) {
            // Let's disable VerifyOops temporarily and retry.
            WHITE_BOX.setBooleanVMFlag("VerifyOops", false);
            WHITE_BOX.clearMethodState(testMethod);
            TestFrameworkUtils.enqueueMethodForCompilation(test);
            WHITE_BOX.setBooleanVMFlag("VerifyOops", true);
        }
        if (!TestFramework.STRESS_CC && TestFramework.USE_COMPILER) {
            for (int i = 0; !WHITE_BOX.isMethodCompiled(testMethod, false) && i < 10 ; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new TestFormatException("Error while waiting for compilation to be completed of " + testMethod);
                }
            }
            Asserts.assertTrue(WHITE_BOX.isMethodCompiled(testMethod, false), testMethod + " not compiled after waiting 1s");
            test.checkCompilationLevel();
        }
        runMethod();
    }

    /**
     * Verify the result
     */
    public void verify(TestInfo testInfo, Object result) { /* no verification in BaseTests */ }
}

class CheckedTest extends BaseTest {
    Method checkMethod;
    Check checkSpecification;

    public CheckedTest(DeclaredTest test, Method checkMethod, Check checkSpecification) {
        super(test);
        // Make sure we can also call non-public or public methods in package private classes
        checkMethod.setAccessible(true);
        this.checkMethod = checkMethod;
        this.checkSpecification = checkSpecification;
    }

    @Override
    public void verify(TestInfo testInfo, Object result) {
        boolean shouldVerify = false;
        switch (checkSpecification.when()) {
            case EACH_INVOCATION -> shouldVerify = true;
            case C2_COMPILED -> shouldVerify = !testInfo.isWarmUp();
        }
        if (shouldVerify) {
            try {
                checkMethod.invoke(test.getInvocationTarget(), testInfo, result);
            } catch (Exception e) {
                throw new TestRunException("There was an error while invoking @Check method " + checkMethod, e);
            }
        }
    }
}

class CustomRunTest extends BaseTest {
    Method runMethod;
    Run runSpecification;

    public CustomRunTest(DeclaredTest test, Method runMethod, Run runSpecification) {
        super(test);
        // Make sure we can also call non-public or public methods in package private classes
        runMethod.setAccessible(true);
        this.runMethod = runMethod;
        this.runSpecification = runSpecification;
    }

    /**
     * Do not directly run the test but rather the run method that is responsible for invoking the actual test.
     */
    @Override
    protected void runMethod() {
        try {
            runMethod.invoke(test.getInvocationTarget(), testInfo);
        } catch (Exception e) {
            throw new TestRunException("There was an error while invoking @Run method " + runMethod, e);
        }
    }
}


class TestFrameworkUtils {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final boolean FLIP_C1_C2 = Boolean.parseBoolean(System.getProperty("FlipC1C2", "false"));

    public static void enqueueMethodForCompilation(DeclaredTest test) {
        enqueueMethodForCompilation(test.getTestMethod(), test.getRequestedCompLevel());
    }

    // Used for non-@Tests, can also be called from other places in tests.
    public static void enqueueMethodForCompilation(Method m, CompLevel compLevel) {
        compLevel = restrictCompLevel(compLevel);
        if (TestFramework.VERBOSE) {
            System.out.println("enqueueMethodForCompilation " + m + ", level = " + compLevel);
        }
        WHITE_BOX.enqueueMethodForCompilation(m, compLevel.getValue());
    }

    public static int compLevelToInt(CompLevel compLevel) {
        return TestFrameworkUtils.restrictCompLevel(compLevel).getValue();
    }

    // Get the appropriate level as permitted by the test scenario and VM options.
    private static CompLevel restrictCompLevel(CompLevel compLevel) {
        switch (compLevel) {
            case ANY -> compLevel = CompLevel.C2_FULL_OPTIMIZATION;
            case C1_SIMPLE, C1_LIMITED_PROFILE, C1_FULL_PROFILE -> {
                if (FLIP_C1_C2) {
                    // Effectively treat all (compLevel = C1_*) as (compLevel = C2)
                    compLevel = CompLevel.C2_FULL_OPTIMIZATION;
                }
            }
            case C2_FULL_OPTIMIZATION -> {
                if (FLIP_C1_C2) {
                    // Effectively treat all (compLevel = C2) as (compLevel = C1_SIMPLE)
                    compLevel = CompLevel.C1_SIMPLE;
                }
            }
        }

        if (!TestFramework.TEST_C1 && compLevel.getValue() < CompLevel.C2_FULL_OPTIMIZATION.getValue()) {
            compLevel = CompLevel.C2_FULL_OPTIMIZATION;
        }
        if (TestFramework.TIERED_COMPILATION && compLevel.getValue() > TestFramework.TIERED_COMPILATION_STOP_AT_LEVEL.getValue()) {
            compLevel = TestFramework.TIERED_COMPILATION_STOP_AT_LEVEL;
        }
        return compLevel;
    }
}
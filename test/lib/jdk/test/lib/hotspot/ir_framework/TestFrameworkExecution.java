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

import jdk.test.lib.Asserts;
import jdk.test.lib.Platform;
import jdk.test.lib.Utils;
import sun.hotspot.WhiteBox;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;


public class TestFrameworkExecution {
    private static final WhiteBox WHITE_BOX;

    static {
        try {
            WHITE_BOX = WhiteBox.getWhiteBox();
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Did you call a test-related interface method from TestFramework in main() of your test? " +
                               "Make sure to only call setup/run methods and no checks/assertions from main() of your test!");
            throw e;
        }
    }

    private static final boolean TIERED_COMPILATION = (Boolean)WHITE_BOX.getVMFlag("TieredCompilation");
    private static final CompLevel TIERED_COMPILATION_STOP_AT_LEVEL = CompLevel.forValue(((Long)WHITE_BOX.getVMFlag("TieredStopAtLevel")).intValue());
    static final boolean TEST_C1 = TIERED_COMPILATION && TIERED_COMPILATION_STOP_AT_LEVEL.getValue() < CompLevel.C2.getValue();

    // User defined settings
    static final boolean XCOMP = Platform.isComp();
    static final boolean VERBOSE = Boolean.parseBoolean(System.getProperty("Verbose", "false"));
    private static final boolean PRINT_TIMES = Boolean.parseBoolean(System.getProperty("PrintTimes", "false"));

    static final boolean USE_COMPILER = WHITE_BOX.getBooleanVMFlag("UseCompiler");
    static final boolean STRESS_CC = Boolean.parseBoolean(System.getProperty("StressCC", "false"));
    private static final String TESTLIST = System.getProperty("Testlist", "");
    private static final String EXCLUDELIST = System.getProperty("Exclude", "");
    public static final int WARMUP_ITERATIONS = Integer.parseInt(System.getProperty("Warmup", "2000"));
    private static final boolean DUMP_REPLAY = Boolean.parseBoolean(System.getProperty("DumpReplay", "false"));
    private static final boolean GC_AFTER = Boolean.parseBoolean(System.getProperty("GCAfter", "false"));
    private static final boolean SHUFFLE_TESTS = Boolean.parseBoolean(System.getProperty("ShuffleTests", "true"));
    // Use separate flag as VERIFY_IR could have been set by user but due to other flags it was disabled by flag VM.
    private static final boolean PRINT_VALID_IR_RULES = Boolean.parseBoolean(System.getProperty("PrintValidIRRules", "false"));
    protected static final long PerMethodTrapLimit = (Long)WHITE_BOX.getVMFlag("PerMethodTrapLimit");
    protected static final boolean ProfileInterpreter = (Boolean)WHITE_BOX.getVMFlag("ProfileInterpreter");
    private static final boolean FLIP_C1_C2 = Boolean.parseBoolean(System.getProperty("FlipC1C2", "false"));

    private final HashMap<Method, DeclaredTest> declaredTests = new HashMap<>();
    private final LinkedHashMap<Method, BaseTest> allTests = new LinkedHashMap<>(); // Keep order
    private final HashMap<String, Method> testMethodMap = new HashMap<>();
    private final List<String> excludeList;
    private final List<String> includeList;
    private List<Class<?>> helperClasses = null;
    private final IREncodingPrinter irMatchRulePrinter;
    private final Class<?> testClass;

    private TestFrameworkExecution(Class<?> testClass) {
        TestRun.check(testClass != null, "Test class cannot be null");
        this.testClass = testClass;
        this.includeList = createTestFilterList(TESTLIST, testClass);
        this.excludeList = createTestFilterList(EXCLUDELIST, testClass);

        if (PRINT_VALID_IR_RULES) {
            irMatchRulePrinter = new IREncodingPrinter();
        } else {
            irMatchRulePrinter = null;
        }
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
        System.out.println("Framework main(), about to run tests in class " + testClassName);
        Class<?> testClass;
        try {
            testClass = Class.forName(testClassName);
        } catch (Exception e) {
            throw new TestRunException("Could not find test class " + testClassName, e);
        }

        TestFrameworkExecution framework = new TestFrameworkExecution(testClass);
        framework.addHelperClasses(args);
        framework.start();
    }

    private void addHelperClasses(String[] args) {
        Class<?>[] helperClasses = getHelperClasses(args);
        if (helperClasses != null) {
            TestRun.check(Arrays.stream(helperClasses).noneMatch(Objects::isNull), "A Helper class cannot be null");
            this.helperClasses = new ArrayList<>();

            for (Class<?> helperClass : helperClasses) {
                TestRun.check(!this.helperClasses.contains(helperClass), "Cannot add the same class twice: " + helperClass);
                this.helperClasses.add(helperClass);
            }
        }
    }

    private static Class<?>[] getHelperClasses(String[] args) {
        if (args.length == 1) {
            return null;
        }
        Class<?>[] helperClasses = new Class<?>[args.length - 1]; // First argument is test class
        for (int i = 1; i < args.length; i++) {
            String helperClassName = args[i];
            try {
                helperClasses[i - 1] = Class.forName(helperClassName);
            } catch (Exception e) {
                throw new TestRunException("Could not find helper class " + helperClassName, e);
            }
        }
        return helperClasses;
    }

    // Only called by internal tests testing the framework itself. Accessed by reflection. Not exposed to normal users.
    private static void runTestsOnSameVM(Class<?> testClass) {
        TestFrameworkSocket dummy = new TestFrameworkSocket();
        try {
            if (PRINT_VALID_IR_RULES) {
                // Need dummy socket to write to as we are not calling this method from TestFramework.
                dummy.start();
            }
            if (testClass == null) {
                StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
                testClass = walker.getCallerClass();
            }
            TestFrameworkExecution framework = new TestFrameworkExecution(testClass);
            framework.start();
        } finally {
            dummy.close();
        }
    }

    private void start() {
        if (helperClasses != null) {
            for (Class<?> helperClass : helperClasses) {
                // Process the helper classes and apply the explicit compile commands
                checkHelperClass(helperClass);
                processExplicitCompileCommands(helperClass);
            }
        }
        parseTestClass();
        runTests();
    }

    private void checkHelperClass(Class<?> clazz) {
        checkTestAnnotationInnerClass(clazz, "helper");
        for (Class<?> c : clazz.getDeclaredClasses()) {
            checkTestAnnotationInnerClass(c, "nested (and helper)");
        }
    }

    private void checkTestAnnotationInnerClass(Class<?> c, String clazzType) {
        Method[] methods = c.getDeclaredMethods();
        for (Method m : methods) {
            TestFormat.checkNoThrow(getAnnotation(m, Test.class) == null,
                                    "Cannot use @Test annotation in " + clazzType + " class: " + m);
        }
    }

    private void parseTestClass() {
        for (Class<?> clazz : testClass.getDeclaredClasses()) {
            checkTestAnnotationInnerClass(clazz, "inner");
        }
        addReplay();
        processExplicitCompileCommands(testClass);
        setupTests();
        setupCheckAndRunMethods();

        // All remaining tests are simple base tests without check or specific way to run them.
        addBaseTests();
        TestFormat.reportIfAnyFailures();
        declaredTests.clear();
        testMethodMap.clear();
    }

    private void addBaseTests() {
        declaredTests.forEach((m, test) -> {
            if (test.getAttachedMethod() == null) {
                try {
                    Arguments argumentsAnno = getAnnotation(m, Arguments.class);
                    TestFormat.check(argumentsAnno != null || m.getParameterCount() == 0, "Missing @Arguments annotation to define arguments of " + m);
                    allTests.put(m, new BaseTest(test));
                } catch (TestFormatException e) {
                    // Failure logged. Continue and report later.
                }
            }
        });
    }

    private void addReplay() {
        if (DUMP_REPLAY) {
            // Generate replay compilation files
            String directive = "[{ match: \"*.*\", DumpReplay: true }]";
            TestFramework.check(WHITE_BOX.addCompilerDirective(directive) == 1, "Failed to add DUMP_REPLAY directive");
        }
    }

    private void processExplicitCompileCommands(Class<?> clazz) {
        if (!XCOMP) {
            // Don't control compilations if -Xcomp is enabled.
            // Also apply compile commands to all inner classes of 'clazz'.
            ArrayList<Class<?>> classes = new ArrayList<>(Arrays.asList(clazz.getDeclaredClasses()));
            classes.add(clazz);
            for (Class<?> c : classes) {
                Method[] methods = c.getDeclaredMethods();
                for (Method m : methods) {
                    try {
                        applyIndependentCompilationCommands(m);

                        if (STRESS_CC) {
                            if (getAnnotation(m, Test.class) != null) {
                                excludeCompilationRandomly(m);
                            }
                        }
                    } catch (TestFormatException e) {
                        // Failure logged. Continue and report later.
                    }
                }

                // Only force compilation now because above annotations affect inlining
                for (Method m : methods) {
                    try {
                        applyForceCompileCommand(m);
                    } catch (TestFormatException e) {
                        // Failure logged. Continue and report later.
                    }
                }
            }
        }
    }

    static boolean excludeCompilationRandomly(Method m) {
        // Exclude some methods from compilation with C2 to stress test the calling convention
        boolean exclude = Utils.getRandomInstance().nextBoolean();
        if (exclude) {
            System.out.println("Excluding from C2 compilation: " + m);
            WHITE_BOX.makeMethodNotCompilable(m, CompLevel.C2.getValue(), false);
            WHITE_BOX.makeMethodNotCompilable(m, CompLevel.C2.getValue(), true);
        }
        return exclude;
    }

    private void applyIndependentCompilationCommands(Method m) {
        ForceInline forceInlineAnno = getAnnotation(m, ForceInline.class);
        DontInline dontInlineAnno = getAnnotation(m, DontInline.class);
        ForceCompile forceCompileAnno = getAnnotation(m, ForceCompile.class);
        DontCompile dontCompileAnno = getAnnotation(m, DontCompile.class);
        checkCompilationCommandAnnotations(m, forceInlineAnno, dontInlineAnno, forceCompileAnno, dontCompileAnno);
        // First handle inline annotations
        if (dontInlineAnno != null) {
            WHITE_BOX.testSetDontInlineMethod(m, true);
        } else if (forceInlineAnno != null) {
            WHITE_BOX.testSetForceInlineMethod(m, true);
        }
        if (dontCompileAnno != null) {
            CompLevel compLevel = dontCompileAnno.value();
            TestFormat.check(compLevel == CompLevel.C1 || compLevel == CompLevel.C2 || compLevel == CompLevel.ANY,
                             "Can only specify compilation level C1 (no individual C1 levels), " +
                             "C2 or ANY (no compilation, same as specifying anything) in @DontCompile at " + m);
            dontCompileMethodAtLevel(m, compLevel);
        }
    }

    private void checkCompilationCommandAnnotations(Method m, ForceInline forceInlineAnno, DontInline dontInlineAnno, ForceCompile forceCompileAnno, DontCompile dontCompileAnno) {
        Test testAnno = getAnnotation(m, Test.class);
        Run runAnno = getAnnotation(m, Run.class);
        Check checkAnno = getAnnotation(m, Check.class);
        TestFormat.check((testAnno == null && runAnno == null && checkAnno == null) || Stream.of(forceCompileAnno, dontCompileAnno, forceInlineAnno, dontInlineAnno).noneMatch(Objects::nonNull),
                         "Cannot use explicit compile command annotations (@ForceInline, @DontInline," +
                         "@ForceCompile or @DontCompile) together with @Test, @Check or @Run: " + m + ". Use compLevel in @Test for fine tuning.");
        if (Stream.of(forceInlineAnno, dontCompileAnno, dontInlineAnno).filter(Objects::nonNull).count() > 1) {
            // Failure
            TestFormat.check(dontCompileAnno == null || dontInlineAnno == null,
                             "@DontInline is implicitely done with @DontCompile annotation at " + m);
            TestFormat.fail("Cannot mix @ForceInline, @DontInline and @DontCompile at the same time at " + m);
        }
        TestFormat.check(forceInlineAnno == null || dontInlineAnno == null, "Cannot have @ForceInline and @DontInline at the same time at " + m);
        if (forceCompileAnno != null && dontCompileAnno != null) {
            CompLevel forceCompile = forceCompileAnno.value();
            CompLevel dontCompile = dontCompileAnno.value();
            TestFormat.check(dontCompile != CompLevel.ANY,
                             "Cannot have @DontCompile(CompLevel.ANY) and @ForceCompile at the same time at " + m);
            TestFormat.check(forceCompile != CompLevel.ANY,
                             "Cannot have @ForceCompile(CompLevel.ANY) and @DontCompile at the same time at " + m);
            TestFormat.check(!CompLevel.overlapping(dontCompile, forceCompile),
                             "Overlapping compilation levels with @ForceCompile and @DontCompile at " + m);
        }
    }

    private void dontCompileMethod(Method m) {
        WHITE_BOX.makeMethodNotCompilable(m, CompLevel.ANY.getValue(), true);
        WHITE_BOX.makeMethodNotCompilable(m, CompLevel.ANY.getValue(), false);
        WHITE_BOX.testSetDontInlineMethod(m, true);
    }

    private void dontCompileMethodAtLevel(Method m, CompLevel compLevel) {
        if (VERBOSE) {
            System.out.println("dontCompileMethodAtLevel " + m + " , level = " + compLevel.name());
        }
        WHITE_BOX.makeMethodNotCompilable(m, compLevel.getValue(), true);
        WHITE_BOX.makeMethodNotCompilable(m, compLevel.getValue(), false);
        if (compLevel == CompLevel.ANY) {
            WHITE_BOX.testSetDontInlineMethod(m, true);
        }
    }

    private void applyForceCompileCommand(Method m) {
        ForceCompile forceCompileAnno = getAnnotation(m, ForceCompile.class);
        if (forceCompileAnno != null) {
            TestFormat.check(forceCompileAnno.value() != CompLevel.SKIP,
                             "Cannot define compilation level SKIP in @ForceCompile at " + m);
            enqueueMethodForCompilation(m, forceCompileAnno.value());
        }
    }

    static void enqueueMethodForCompilation(Method m, CompLevel compLevel) {
        if (TestFrameworkExecution.VERBOSE) {
            System.out.println("enqueueMethodForCompilation " + m + ", level = " + compLevel);
        }
        compLevel = restrictCompLevel(compLevel);
        WHITE_BOX.enqueueMethodForCompilation(m, compLevel.getValue());
    }

    private void setupTests() {
        boolean hasIncludeList = includeList != null;
        boolean hasExcludeList = excludeList != null;
        for (Method m : testClass.getDeclaredMethods()) {
            Test testAnno = getAnnotation(m, Test.class);
            try {
                if (testAnno != null) {
                    if (hasIncludeList && includeList.contains(m.getName()) && (!hasExcludeList || !excludeList.contains(m.getName()))) {
                        addTest(m);
                    } else if (hasExcludeList && !excludeList.contains(m.getName())) {
                        addTest(m);
                    } else {
                        addTest(m);
                    }
                } else {
                    TestFormat.checkNoThrow(!m.isAnnotationPresent(IR.class), "Found @IR annotation on non-@Test method " + m);
                    TestFormat.checkNoThrow(!m.isAnnotationPresent(Warmup.class) || getAnnotation(m, Run.class) != null,
                                     "Found @Warmup annotation on non-@Test or non-@Run method " + m);
                }
            } catch (TestFormatException e) {
                // Failure logged. Continue and report later.
            }
        }
        if (PRINT_VALID_IR_RULES) {
            irMatchRulePrinter.emit();
        }
    }

    private void addTest(Method m) {
        Test testAnno = getAnnotation(m, Test.class);
        checkTestAnnotations(m, testAnno);
        Warmup warmup = getAnnotation(m, Warmup.class);
        int warmupIterations = WARMUP_ITERATIONS;
        if (warmup != null) {
            warmupIterations = warmup.value();
            TestFormat.checkNoThrow(warmupIterations >= 0, "Cannot have negative value for @Warmup at " + m);
        }

        boolean osrOnly = getAnnotation(m, OSRCompileOnly.class) != null;

        if (PRINT_VALID_IR_RULES) {
            irMatchRulePrinter.emitRuleEncoding(m);
        }

        if (!XCOMP) {
            // Don't inline test methods. Don't care when -Xcomp set.
            WHITE_BOX.testSetDontInlineMethod(m, true);
        }
        CompLevel compLevel = restrictCompLevel(testAnno.compLevel());
        if (FLIP_C1_C2) {
            compLevel = flipCompLevel(compLevel);
        }
        DeclaredTest test = new DeclaredTest(m, ArgumentValue.getArguments(m), compLevel, warmupIterations, osrOnly);
        declaredTests.put(m, test);
        testMethodMap.put(m.getName(), m);
    }

    private void checkTestAnnotations(Method m, Test testAnno) {
        TestFormat.check(!testMethodMap.containsKey(m.getName()),
                         "Cannot overload two @Test methods: " + m + ", " + testMethodMap.get(m.getName()));
        TestFormat.check(testAnno != null, m + " must be a method with a @Test annotation");

        Check checkAnno = getAnnotation(m, Check.class);
        Run runAnno = getAnnotation(m, Run.class);
        TestFormat.check(checkAnno == null && runAnno == null,
                         m + " has invalid @Check or @Run annotation while @Test annotation is present.");

        TestFormat.checkNoThrow(!Arrays.asList(m.getParameterTypes()).contains(TestInfo.class),
                         "Cannot use of " + TestInfo.class + " as parameter type at @Test method " + m);

        TestFormat.checkNoThrow(!m.getReturnType().equals(TestInfo.class),
                         "Cannot use of " + TestInfo.class + " as return type at @Test method " + m);
    }


    // Get the appropriate level as permitted by the test scenario and VM options.
    private static CompLevel restrictCompLevel(CompLevel compLevel) {
        if (!USE_COMPILER) {
            return CompLevel.SKIP;
        }
        if (compLevel == CompLevel.ANY) {
            // Use highest available compilation level by default (usually C2).
            compLevel = TIERED_COMPILATION_STOP_AT_LEVEL;
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

    private void setupCheckAndRunMethods() {
        for (Method m : testClass.getDeclaredMethods()) {
            Check checkAnno = getAnnotation(m, Check.class);
            Run runAnno = getAnnotation(m, Run.class);
            Arguments argumentsAnno = getAnnotation(m, Arguments.class);
            try {
                TestFormat.check(argumentsAnno == null || (checkAnno == null && runAnno == null), "Cannot have @Argument annotation in combination with @Run or @Check at " + m);
                if (checkAnno != null) {
                    addCheckedTest(m, checkAnno, runAnno);
                } else if (runAnno != null) {
                    addCustomRunTest(m, runAnno);
                }
            } catch (TestFormatException e) {
                // Failure logged. Continue and report later.
            }
        }
    }

    private void addCheckedTest(Method m, Check checkAnno, Run runAnno) {
        Method testMethod = testMethodMap.get(checkAnno.test());
        DeclaredTest test = declaredTests.get(testMethod);
        checkCheckedTest(m, checkAnno, runAnno, testMethod, test);
        test.setAttachedMethod(m);
        CheckedTest.Parameter parameter = getCheckedTestParameter(m, testMethod);
        dontCompileMethod(m);
        // Don't inline check methods
        WHITE_BOX.testSetDontInlineMethod(m, true);
        CheckedTest checkedTest = new CheckedTest(test, m, checkAnno, parameter);
        allTests.put(testMethod, checkedTest);
    }

    private void checkCheckedTest(Method m, Check checkAnno, Run runAnno, Method testMethod, DeclaredTest test) {
        TestFormat.check(runAnno == null, m + " has invalid @Run annotation while @Check annotation is present.");
        TestFormat.check(testMethod != null, "Did not find associated test method \"" + m.getDeclaringClass().getName()
                                             + "." + checkAnno.test() + "\" for @Check at " + m);
        TestFormat.check(test != null, "Missing @Test annotation for associated test method " + testMethod + " for @Check at " + m);
        Method attachedMethod = test.getAttachedMethod();
        TestFormat.check(attachedMethod == null,
                         "Cannot use @Test " + testMethod + " for more than one @Run or one @Check method. Found: " + m + ", " + attachedMethod);
    }

    private CheckedTest.Parameter getCheckedTestParameter(Method m, Method testMethod) {
        boolean firstParameterTestInfo = m.getParameterCount() > 0 && m.getParameterTypes()[0].equals(TestInfo.class);
        boolean secondParameterTestInfo = m.getParameterCount() > 1 && m.getParameterTypes()[1].equals(TestInfo.class);
        CheckedTest.Parameter parameter = null;
        Class<?> testReturnType = testMethod.getReturnType();
        switch (m.getParameterCount()) {
            case 0 -> parameter = CheckedTest.Parameter.NONE;
            case 1 -> {
                TestFormat.checkNoThrow(firstParameterTestInfo || m.getParameterTypes()[0] == testReturnType,
                                        "Single-parameter version of @Check method " + m + " must match return type of @Test " + testMethod);
                parameter = firstParameterTestInfo ? CheckedTest.Parameter.TEST_INFO_ONLY : CheckedTest.Parameter.RETURN_ONLY;
            }
            case 2 -> {
                TestFormat.checkNoThrow(m.getParameterTypes()[0] == testReturnType && secondParameterTestInfo,
                                        "Two-parameter version of @Check method " + m + " must provide as first parameter the same"
                                        + " return type as @Test method " + testMethod + " and as second parameter an object of " + TestInfo.class);
                parameter = CheckedTest.Parameter.BOTH;
            }
            default -> TestFormat.failNoThrow("@Check method " + m + " must provide either a none, single or two-parameter variant.");
        }
        return parameter;
    }

    private void addCustomRunTest(Method m, Run runAnno) {
        Method testMethod = testMethodMap.get(runAnno.test());
        DeclaredTest test = declaredTests.get(testMethod);
        checkCustomRunTest(m, runAnno, testMethod, test);
        test.setAttachedMethod(m);
        dontCompileMethod(m);
        // Don't inline run methods
        WHITE_BOX.testSetDontInlineMethod(m, true);
        CustomRunTest customRunTest = new CustomRunTest(test, m, getAnnotation(m, Warmup.class), runAnno);
        allTests.put(m, customRunTest);
    }

    private void checkCustomRunTest(Method m, Run runAnno, Method testMethod, DeclaredTest test) {
        TestFormat.check(testMethod != null, "Did not find associated @Test method \""  + m.getDeclaringClass().getName()
                                             + "." + runAnno.test() + "\" specified in @Run at " + m);
        TestFormat.check(test != null, "Missing @Test annotation for associated test method " + runAnno.test() + " for @Run at " + m);
        Method attachedMethod = test.getAttachedMethod();
        TestFormat.check(attachedMethod == null,
                         "Cannot use @Test " + testMethod + " for more than one @Run/@Check method. Found: " + m + ", " + attachedMethod);
        TestFormat.check(!test.hasArguments(), "Cannot use @Arguments at test method " + testMethod + " in combination with @Run method " + m);
        TestFormat.checkNoThrow(m.getParameterCount() == 0 || (m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(TestInfo.class)),
                         "@Run method " + m + " must specify either no parameter or exactly one of " + TestInfo.class);
        Warmup warmupAnno = getAnnotation(testMethod, Warmup.class);
        TestFormat.checkNoThrow(warmupAnno == null,
                         "Cannot set @Warmup at @Test method " + testMethod + " when used with its @Run method " + m + ". Use @Warmup at @Run method instead.");
        warmupAnno = getAnnotation(m, Warmup.class);
        TestFormat.checkNoThrow(warmupAnno == null || runAnno.mode() != RunMode.STANDALONE,
                         "Cannot set @Warmup at @Run method " + m + " when used with RunMode.STANDALONE. The @Run method is only invoked once.");
        OSRCompileOnly osrAnno = getAnnotation(testMethod, OSRCompileOnly.class);
        TestFormat.checkNoThrow(osrAnno == null || runAnno.mode() != RunMode.STANDALONE,
                                "Cannot set @OSRCompileOnly at @Run method " + m + " when used with RunMode.STANDALONE. The @Run method is responsible for triggering compilation.");
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

    enum TriState {
        Maybe,
        Yes,
        No
    }

    // Can be called from tests for non-@Test methods
    static void compile(Method m, CompLevel compLevel) {
        TestRun.check(getAnnotation(m, Test.class) == null,
                      "Cannot call enqueueMethodForCompilation() for @Test annotated method " + m);
        enqueueMethodForCompilation(m, compLevel);
    }

    static boolean isC1Compiled(Method m) {
        return compiledByC1(m) == TriState.Yes;
    }

    static boolean isC2Compiled(Method m) {
        return compiledByC2(m) == TriState.Yes;
    }

    static boolean isCompiledAtLevel(Method m, CompLevel compLevel) {
        return compiledAtLevel(m, compLevel) == TriState.Yes;
    }

    static void assertDeoptimizedByC1(Method m) {
        TestRun.check(compiledByC1(m) != TriState.Yes || PerMethodTrapLimit == 0 || !ProfileInterpreter, m + " should have been deoptimized by C1");
    }

    static void assertDeoptimizedByC2(Method m) {
        TestRun.check(compiledByC2(m) != TriState.Yes || PerMethodTrapLimit == 0 || !ProfileInterpreter, m + " should have been deoptimized by C2");
    }

    static void assertCompiledByC1(Method m) {
        TestRun.check(compiledByC1(m) != TriState.No, m + " should have been C1 compiled");
    }

    static void assertCompiledByC2(Method m) {
        TestRun.check(compiledByC2(m) != TriState.No, m + " should have been C2 compiled");
    }

    static void assertCompiledAtLevel(Method m, CompLevel level) {
        TestRun.check(compiledAtLevel(m, level) != TriState.No, m + " should have been compiled at level " + level.name());
    }

    static void assertNotCompiled(Method m) {
        TestRun.check(!WHITE_BOX.isMethodCompiled(m, false) && !WHITE_BOX.isMethodCompiled(m, true),
                      m + " should not have been compiled");
    }

    static void assertCompiled(Method m) {
        TestRun.check(WHITE_BOX.isMethodCompiled(m, false) || WHITE_BOX.isMethodCompiled(m, true),
                      m + " should have been compiled");
    }

    private static TriState compiledByC1(Method m) {
        TriState triState = compiledAtLevel(m, CompLevel.C1);
        if (triState != TriState.No) {
            return triState;
        }
        triState = compiledAtLevel(m, CompLevel.C1_LIMITED_PROFILE);
        if (triState != TriState.No) {
            return triState;
        }
        triState = compiledAtLevel(m, CompLevel.C1_FULL_PROFILE);
        return triState;
    }

    private static TriState compiledByC2(Method m) {
        return compiledAtLevel(m, CompLevel.C2);
    }

    private static TriState compiledAtLevel(Method m, CompLevel level) {
        if (!TestFrameworkExecution.USE_COMPILER || TestFrameworkExecution.XCOMP || TestFrameworkExecution.TEST_C1 ||
            (TestFrameworkExecution.STRESS_CC && !WHITE_BOX.isMethodCompilable(m, level.getValue(), false))) {
            return TriState.Maybe;
        }
        if (WHITE_BOX.isMethodCompiled(m, false)
            && WHITE_BOX.getMethodCompilationLevel(m, false) == level.getValue()) {
            return TriState.Yes;
        }
        return TriState.No;
    }
}

class DeclaredTest {
    private final Method testMethod;
    private final ArgumentValue[] arguments;
    private final int warmupIterations;
    private final CompLevel compLevel;
    private final boolean osrOnly;
    private Method attachedMethod;

    public DeclaredTest(Method testMethod, ArgumentValue[] arguments, CompLevel compLevel, int warmupIterations, boolean osrOnly) {
        // Make sure we can also call non-public or public methods in package private classes
        testMethod.setAccessible(true);
        this.testMethod = testMethod;
        this.compLevel = compLevel;
        this.arguments = arguments;
        this.warmupIterations = warmupIterations;
        this.osrOnly = osrOnly;
        this.attachedMethod = null;
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
        return Arrays.stream(arguments).map(ArgumentValue::getArgument).toArray();
    }

    public void setAttachedMethod(Method m) {
        attachedMethod = m;
    }

    public Method getAttachedMethod() {
        return attachedMethod;
    }

    public void printFixedRandomArguments() {
        if (hasArguments()) {
            boolean hasRandomArgs = false;
            StringBuilder builder = new StringBuilder("Fixed random arguments for method ").append(testMethod).append(": ");
            for (int i = 0; i < arguments.length; i++) {
                ArgumentValue argument = arguments[i];
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

    public String getArgumentsString() {
        if (hasArguments()) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < arguments.length; i++) {
                builder.append("arg ").append(i).append(": ").append(arguments[i].getArgument()).append(", ");
            }
            builder.setLength(builder.length() - 2);
            return builder.toString();
        } else {
            return "<void>";
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
    private static final int TEST_COMPILATION_TIMEOUT = Integer.parseInt(System.getProperty("TestCompilationTimeout", "5000"));
    private static final boolean VERIFY_OOPS = (Boolean)WHITE_BOX.getVMFlag("VerifyOops");

    protected final DeclaredTest test;
    protected final Method testMethod;
    protected final TestInfo testInfo;
    protected final Object invocationTarget;
    private final boolean shouldCompile;
    protected int warmupIterations;

    public BaseTest(DeclaredTest test) {
        this.test = test;
        this.testMethod = test.getTestMethod();
        this.testInfo = new TestInfo(testMethod);
        this.warmupIterations = test.getWarmupIterations();
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
        if (!TestFrameworkExecution.USE_COMPILER) {
            this.shouldCompile = false;
        } else if (TestFrameworkExecution.STRESS_CC) {
            this.shouldCompile = !TestFrameworkExecution.excludeCompilationRandomly(testMethod);
        } else {
            this.shouldCompile = true;
        }
    }

    public String getTestName() {
        return testMethod.getName();
    }

    public Method getAttachedMethod() { return null; }

    /**
     * Run the associated test
     */
    public void run() {
        if (test.getCompLevel() == CompLevel.SKIP) {
            // Exclude test if compilation level is SKIP either set through test or by not matching the current VM flags.
            return;
        }
        if (TestFrameworkExecution.VERBOSE) {
            System.out.println("Starting " + testMethod);
        }
        test.printFixedRandomArguments();
        for (int i = 0; i < warmupIterations; i++) {
            runMethod();
        }
        testInfo.setWarmUpFinished();

        if (test.isOSROnly()) {
            compileOSRAndRun(); // TODO: Keep this?
        } else {
            if (shouldCompile) {
                compileTest();
            }
            // Always run method.
            runMethod();
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
            throw new TestRunException("There was an error while invoking @Test method " + testMethod
                                       + ". Used arguments: " + test.getArgumentsString(), e);
        }
    }

    private void compileOSRAndRun() {
        final boolean maybeCodeBufferOverflow = (TestFrameworkExecution.TEST_C1 && VERIFY_OOPS);
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
            if (TestFrameworkExecution.VERBOSE) {
                System.out.println("Is " + testMethod + " compiled? " + b);
            }
            if (b || TestFrameworkExecution.XCOMP || TestFrameworkExecution.STRESS_CC || !TestFrameworkExecution.USE_COMPILER) {
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

    private void compileTest() {
        final boolean maybeCodeBufferOverflow = (TestFrameworkExecution.TEST_C1 && VERIFY_OOPS);
        final Method testMethod = test.getTestMethod();
        long started = System.currentTimeMillis();
        long elapsed = 0;
        Asserts.assertTrue(WHITE_BOX.isMethodCompilable(testMethod, test.getCompLevel().getValue(), false));
        enqueueMethodForCompilation();

        do {
            if (!WHITE_BOX.isMethodQueuedForCompilation(testMethod)) {
                if (elapsed > 0) {
                    if (TestFrameworkExecution.VERBOSE) {
                        System.out.println(testMethod + " is not in queue anymore due to compiling it simultaneously on a different level. Enqueue again.");
                    }
                    enqueueMethodForCompilation();
                }
            }
            if (maybeCodeBufferOverflow && elapsed > 1000 && !WHITE_BOX.isMethodCompiled(testMethod, false)) {
                // Let's disable VerifyOops temporarily and retry.
                WHITE_BOX.setBooleanVMFlag("VerifyOops", false);
                WHITE_BOX.clearMethodState(testMethod);
                enqueueMethodForCompilation();
                WHITE_BOX.setBooleanVMFlag("VerifyOops", true);
            }

            if (WHITE_BOX.getMethodCompilationLevel(testMethod, false) == test.getCompLevel().getValue()) {
                break;
            }
            elapsed = System.currentTimeMillis() - started;
        } while (elapsed < TEST_COMPILATION_TIMEOUT);
        TestRun.check(elapsed < TEST_COMPILATION_TIMEOUT, "Could not compile" + testMethod + " after " + TEST_COMPILATION_TIMEOUT/1000 + "s");
        checkCompilationLevel();
    }

    private void enqueueMethodForCompilation() {
        TestFrameworkExecution.enqueueMethodForCompilation(test.getTestMethod(), test.getCompLevel());
    }

    protected void checkCompilationLevel() {
        CompLevel level = CompLevel.forValue(WHITE_BOX.getMethodCompilationLevel(testMethod));
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
    public Method getAttachedMethod() { return checkMethod; }

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

    public CustomRunTest(DeclaredTest test, Method runMethod, Warmup warmUpAnno, Run runSpecification) {
        super(test);
        // Make sure we can also call non-public or public methods in package private classes
        runMethod.setAccessible(true);
        this.runMethod = runMethod;
        this.mode = runSpecification.mode();
        this.warmupIterations = warmUpAnno != null ? warmUpAnno.value() : test.getWarmupIterations();
        TestFormat.checkNoThrow(warmupIterations >= 0, "Cannot have negative value for @Warmup at " + runMethod);
    }

    @Override
    public Method getAttachedMethod() { return runMethod; }

    @Override
    public void run() {
        switch (mode) {
            case STANDALONE -> runMethod();
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
                case STANDALONE -> message = message + "\nCheck your @Run method (invoked once) " + runMethod + " to ensure that " + testMethod + " will be complied at the requested level.";
                case NORMAL -> message = message + "\nCheck your @Run method " + runMethod + " to ensure that " + testMethod + " is called at least once in each iteration.";
            }
            TestRun.fail(message);
        }
    }
}



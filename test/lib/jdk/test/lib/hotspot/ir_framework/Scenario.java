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

import java.util.*;

/**
 * This class represents a scenario that can be executed by the {@link TestFramework}.
 * <p>
 * A JTreg test should call the test framework with {@code @run driver}, not allowing to specify any additional flags.
 * If a test should run with additional flags, use {@link TestFramework#runWithFlags(String...)} or
 * {@link TestFramework#addFlags(String...)}. If, however, the test should be run with different settings (equivalent
 * to having multiple {@code @run} entries in a normal JTreg test), use scenarios. A scenario will be run with the
 * scenario specific flags, if any, and the flags specified with
 * {@link TestFramework#runWithFlags(String...)}/{@link TestFramework#addFlags(String...)} whereas scenario flags will
 * have precedence.
 *
 * @see TestFramework
 */
public class Scenario {
    private static final String ADDITIONAL_SCENARIO_FLAGS = System.getProperty("ScenarioFlags", "");
    private static final String SCENARIOS = System.getProperty("Scenarios", "");
    private static final List<String> additionalScenarioFlags = new ArrayList<>();
    private static final Set<Integer> enabledScenarios = new HashSet<>();

    private final List<String> flags;
    private final int index;
    private final boolean enabled;
    private String testVMOutput;

    static {
        if (!SCENARIOS.isEmpty()) {
            System.out.println(Arrays.toString(SCENARIOS.split("\\s*,\\s*")));
            try {
                Arrays.stream(SCENARIOS.split("\\s*,\\s*")).map(Integer::parseInt).forEachOrdered(enabledScenarios::add);
            } catch (NumberFormatException e) {
                TestRun.fail("Provided a scenario index in the -DScenario comma-separated list which is not " +
                             "a number: " + SCENARIOS);
            }
        }

        if (!ADDITIONAL_SCENARIO_FLAGS.isEmpty()) {
            additionalScenarioFlags.addAll(Arrays.asList(ADDITIONAL_SCENARIO_FLAGS.split("\\s*,\\s*")));
        }
    }

    /**
     * Create a scenario with index {@code index} that will be run with the additionally specified flags specified in
     * {@code flags} (or without any additional flags if null or parameter not specified).
     * <p>
     * The scenario index must be unique to be distinguishable in stdout and stderr output and when specifying
     * {@code -DScenarios} (see {@link Scenario}).
     *
     * @param index the unique scenario index.
     * @param flags the scenario flags or null (i.e. no parameter specified) if no flags should be used.
     */
    public Scenario(int index, String... flags) {
        this.index = index;
        if (flags != null) {
            this.flags = new ArrayList<>(Arrays.asList(flags));
            this.flags.addAll(additionalScenarioFlags);
        } else {
            this.flags = new ArrayList<>();
        }
        this.enabled = enabledScenarios.isEmpty() || enabledScenarios.contains(index);
    }

    /**
     * Add additional flags to this scenario.
     *
     * @param flags the additional scenario flags.
     */
    public void addFlags(String... flags) {
        if (flags != null) {
            this.flags.addAll(Arrays.asList(flags));
        }
    }

    /**
     * Get all scenario specific flags as defined in {@link #Scenario(int, String...)}.
     *
     * @return the scenario flags.
     */
    public List<String> getFlags() {
        return flags;
    }

    /**
     * Get the unique scenario index as defined in {@link #Scenario(int, String...)}.
     *
     * @return the scenario index.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns a boolean indicating if this scenario will be executed by the test framework. This only depends on
     * the property flag {@code -DScenarios} (see {@link Scenario}).
     *
     * @return {@code true} if {@code -DScenarios} is either not set or if {@code -DScenarios} specifies the scenario
     *         index set by {@link #Scenario(int, String...)}.
     *         {@code false} otherwise.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the test VM output (stdout + stderr) of this scenario from the last execution of the framework.
     *
     * @return the test VM output.
     */
    public String getTestVMOutput() {
        return testVMOutput;
    }

    /**
     * Set the test VM output, called by the framework.
     */
    void setTestVMOutput(String testVMOutput) {
        this.testVMOutput = testVMOutput;
    }
}

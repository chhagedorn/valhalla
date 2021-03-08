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

public class Scenario {

    static final String ADDITIONAL_SCENARIO_FLAGS = System.getProperty("ScenarioFlags", "");
    private static final String SCENARIOS = System.getProperty("Scenarios", "");
    private static final List<String> additionalScenarioFlags = new ArrayList<>();
    private static final Set<Integer> enabledScenarios = new HashSet<>();

    private final List<String> flags;
    private final int index;
    boolean enabled;
    private String vmOutput;

    static {
        if (!SCENARIOS.isEmpty()) {
            Arrays.stream(SCENARIOS.split("\\s*,\\s*")).map(Integer::getInteger).forEachOrdered(enabledScenarios::add);
        }

        if (!ADDITIONAL_SCENARIO_FLAGS.isEmpty()) {
            additionalScenarioFlags.addAll(Arrays.asList(ADDITIONAL_SCENARIO_FLAGS.split("\\s*,\\s*")));
        }
    }

    public Scenario(int index, String... flags) {
        this.index = index;
        if (flags != null && (enabledScenarios.isEmpty() || enabledScenarios.contains(index))) {
            this.flags = new ArrayList<>(Arrays.asList(flags));
            this.flags.addAll(additionalScenarioFlags);
            this.enabled = true;
        } else {
            this.flags = new ArrayList<>();
            this.enabled = false;
        }
    }

    public void addFlags(String... flags) {
        if (flags != null) {
            this.flags.addAll(Arrays.asList(flags));
        }
    }

    public List<String> getFlags() {
        return flags;
    }

    public int getIndex() {
        return index;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setVMOutput(String vmOutput) {
        this.vmOutput = vmOutput;
    }

    public String getVMOutput() {
        return vmOutput;
    }
}

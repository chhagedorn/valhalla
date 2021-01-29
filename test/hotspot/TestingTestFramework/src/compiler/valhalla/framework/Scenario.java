package compiler.valhalla.framework;

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
        if (enabledScenarios.isEmpty() || enabledScenarios.contains(index)) {
            this.flags = new ArrayList<>(Arrays.asList(flags));
            this.flags.addAll(additionalScenarioFlags);
            this.enabled = true;
        } else {
            this.flags = new ArrayList<>();
            this.enabled = false;
        }
    }

    public void addFlag(String flag) {
        flags.add(flag);
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

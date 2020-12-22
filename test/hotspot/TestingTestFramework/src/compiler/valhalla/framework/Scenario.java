package compiler.valhalla.framework;

import java.util.ArrayList;

public class Scenario {
    private final ArrayList<String> flags;
    private boolean enabled;
    private String disableReason;
    private String output;

    public Scenario(ArrayList<String> flags) {
        this.flags = flags;
        this.disableReason = "";
    }

    public boolean isIgnored() {
        return enabled;
    }

    public void addFlag(String flag) {
        flags.add(flag.trim());
    }

    public void disable() {
        enabled = false;
        disableReason = "Disabled by Test.";
    }

    public void disable(String reason) {
        enabled = false;
        disableReason = reason;
    }

    public void enable() {
        enabled = true;
        disableReason = "Disabled by Test.";
    }

    public String getIgnoreReason() {
        return disableReason;
    }

    public ArrayList<String> getFlags() {
        return flags;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getOutput() {
        return output;
    }
}

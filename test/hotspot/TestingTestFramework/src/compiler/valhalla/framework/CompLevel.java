package compiler.valhalla.framework;

import java.util.HashMap;
import java.util.Map;

public enum CompLevel {
    SKIP(-3), // Skip a @Test having this value
    ANY(-2),
    C1(1), // C1
    C1_LIMITED_PROFILE(2), // C1, invocation & backedge counters
    C1_FULL_PROFILE(3), // C1, invocation & backedge counters + mdo
    C2(4); // C2 or JVMCI


    private static final Map<Integer, CompLevel> typesByValue = new HashMap<>();
    private final int value;

    static {
        for (CompLevel level : CompLevel.values()) {
            typesByValue.put(level.value, level);
        }
    }

    CompLevel(int level) {
        this.value = level;
    }

    public int getValue() {
        return value;
    }

    public static CompLevel forValue(int value) {
        return typesByValue.get(value);
    }
}

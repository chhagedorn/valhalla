package compiler.valhalla.framework;

public enum Skip {
    C1_SIMPLE(1), // C1
    C1_LIMITED_PROFILE(2), // C1, invocation & backedge counters
    C1_FULL_PROFILE(3), // C1, invocation & backedge counters + mdo
    C2_FULL_OPTIMIZATION(4), // C2 or JVMCI
    ALL(5), // Special artificial level, skip this test completely.
    C1(6), // Special artificial level to exlude C1 (Level 1-3)
    C2(7); // Special artificial level to exclude C2 (Level 4)

    private final int value;

    Skip(int level) {
        this.value = level;
    }

    public int getValue() {
        return value;
    }
}

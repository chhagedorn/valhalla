package compiler.valhalla.framework;

import java.util.Random;

public class TestInfo {
    private boolean toggleBool = false;
    private boolean onWarmUp = true;
    private static final Random random = new Random();

    public boolean toggleBoolean() {
        toggleBool = !toggleBool;
        return toggleBool;
    }

    public static int getRandomInt() {
        return random.nextInt() % 1000;
    }

    public static long getRandomLong() {
        return random.nextLong() % 1000;
    }

    public static double getRandomDouble() {
        return random.nextDouble() % 1000;
    }

    public boolean isWarmUp() {
        return onWarmUp;
    }

    public void setWarmUpFinished() {
        onWarmUp = false;
    }
}

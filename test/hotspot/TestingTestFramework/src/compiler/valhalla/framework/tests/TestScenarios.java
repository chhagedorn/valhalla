package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.ArgumentValue;
import compiler.valhalla.framework.Arguments;
import compiler.valhalla.framework.Test;
import compiler.valhalla.framework.TestFramework;

public class TestScenarios {
    public static final String[] TEST_KEYS = { "test-key0", "test-key1" };

    public static void main(String[] args) {
        TestFramework framework = new TestFramework();
        framework.runScenarios();
        final int expectedInvocationsPerTest = TestFramework.WARMUP_ITERATIONS + 1;
        for (int i = 0; i < TestFramework.DEFAULT_SCENARIOS; i++) {
            String output = framework.getScenarioOutput(i);
            for (int j = 0; j < TEST_KEYS.length; j++) {
                int invocationCount = getMatches(output, TEST_KEYS[j]);
                if (invocationCount != expectedInvocationsPerTest) {
                    // Warmups + 1 C2 compiled invocation * number of default scenarios
                    throw new RuntimeException("Test " + j + "  was executed " + invocationCount + " times stead of "
                            + expectedInvocationsPerTest + 1 + " times." );
                }
            }
        }
    }

    // How many times do wie find 'toMatch' in 'target'?
    private static int getMatches(String target, String toMatch) {
        int fromIndex = 0;
        int count = 0;
        while (true) {
            fromIndex = target.indexOf(toMatch, fromIndex);
            if (fromIndex == -1) {
                break;
            }
            count++;
            fromIndex += toMatch.length();
        }
        return count;
    }
    @Test
    public void test() {
        System.out.println(TEST_KEYS[0]);
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void test2(int i) {
        System.out.println(TEST_KEYS[1]);
    }
}

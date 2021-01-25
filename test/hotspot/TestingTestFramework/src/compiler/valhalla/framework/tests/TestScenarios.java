package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;
import jdk.test.lib.Asserts;

// Run test with SuspendRetryCount=50
public class TestScenarios {
    public static final String[] TEST_KEYS = { "test-key0", "test-key1" };

    public static void main(String[] args) {
        Scenario s1 = new Scenario(1, "-XX:SuspendRetryCount=51");
        Scenario s2 = new Scenario(2, "-XX:SuspendRetryCount=52");
        Scenario s3 = new Scenario(3, "-XX:SuspendRetryCount=53");
        Scenario s3dup = new Scenario(3, "-XX:SuspendRetryCount=53");
        try {
            TestFramework.runWithScenarios(s1, s2, s3);
        } catch (TestRunException e) {
            Asserts.assertTrue(e.getMessage().contains("The following scenarios have failed: #1, #3, #Default Scenario"));
        }
        TestFramework.runWithScenarios(ScenarioTest.class, s1, s2, s3);
        try {
            TestFramework.runWithScenarios(s1, s3dup, s2, s3);
        } catch (RuntimeException e) {
            Asserts.assertTrue(e.getMessage().contains("Cannot define two scenarios with the same index 3"));
        }
    }

    @Test
    @IR(applyIf = {"SuspendRetryCount", "50"}, failOn = {IRNode.RETURN})
    public void failDefault() {
    }

    @Test
    @IR(applyIf = {"SuspendRetryCount", "51"}, failOn = {IRNode.RETURN})
    @IR(applyIf = {"SuspendRetryCount", "53"}, failOn = {IRNode.RETURN})
    public void failS3() {
    }
}

class ScenarioTest {
    @Test
    @IR(applyIf = {"SuspendRetryCount", "54"}, failOn = {IRNode.RETURN})
    public void doesNotFail() {
    }
}

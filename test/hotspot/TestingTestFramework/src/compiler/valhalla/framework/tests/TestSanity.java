package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.Scenario;
import compiler.valhalla.framework.Test;
import compiler.valhalla.framework.TestFramework;

import java.util.ArrayList;

public class TestSanity {

    public static void main(String[] args) {
        TestFramework.run();
        TestFramework.run(TestSanity.class);
        TestFramework.runWithHelperClasses(TestSanity.class, HelperA.class);
        TestFramework.runWithHelperClasses(TestSanity.class, HelperA.class, HelperB.class);
        Scenario s1 = new Scenario(1, "-XX:SuspendRetryCount=52", "-XX:+UseTLAB");
        Scenario s2 = new Scenario(2, "-XX:SuspendRetryCount=53", "-XX:+UseTLAB");
        TestFramework.runWithScenarios(s1);
        TestFramework.runWithScenarios(s1, s2);
        TestFramework.runWithScenarios(TestSanity.class, s1, s2);
        TestFramework.runWithScenarios(Scenario.Run.INCLUDE_DEFAULT, s1);
        TestFramework.runWithScenarios(Scenario.Run.INCLUDE_DEFAULT, s1, s2);
        TestFramework.runWithScenarios(Scenario.Run.INCLUDE_DEFAULT, TestSanity.class, s1);
        TestFramework.runWithScenarios(Scenario.Run.INCLUDE_DEFAULT, TestSanity.class, s1, s2);
        ArrayList<Class<?>> helperClasses = new ArrayList<>();
        helperClasses.add(HelperA.class);
        helperClasses.add(HelperB.class);
        TestFramework.runWithScenariosAndHelperClasses(TestSanity.class, helperClasses, s2);
        TestFramework.runWithScenariosAndHelperClasses(TestSanity.class, helperClasses, s1, s2);
        TestFramework.runWithScenariosAndHelperClasses(Scenario.Run.INCLUDE_DEFAULT, TestSanity.class, helperClasses, s2);
        TestFramework.runWithScenariosAndHelperClasses(Scenario.Run.INCLUDE_DEFAULT, TestSanity.class, helperClasses, s1, s2);
    }

    @Test
    public void test() {
        System.out.println("test");
    }
}

class HelperA {

}

class HelperB {

}

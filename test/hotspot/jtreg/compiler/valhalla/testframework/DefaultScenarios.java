package compiler.valhalla.testframework;

import jdk.test.lib.hotspot.ir_framework.Scenario;

public class DefaultScenarios {
    /**
     * VM parameters for the 5 built-in test scenarios. If your test needs to append
     * extra parameters for (some of) these scenarios, override getExtraVMParameters().
     */
    public static final Scenario[] SCENARIOS = {
        new Scenario(0,
            "-XX:-UseACmpProfile",
            "-XX:+AlwaysIncrementalInline",
            "-XX:FlatArrayElementMaxOops=5",
            "-XX:FlatArrayElementMaxSize=-1",
            "-XX:-UseArrayLoadStoreProfile",
            "-XX:InlineFieldMaxFlatSize=-1",
            "-XX:+InlineTypePassFieldsAsArgs",
            "-XX:+InlineTypeReturnedAsFields"
        ),
        new Scenario(1,
            "-XX:-UseACmpProfile",
            "-XX:-UseCompressedOops",
            "-XX:FlatArrayElementMaxOops=5",
            "-XX:FlatArrayElementMaxSize=-1",
            "-XX:-UseArrayLoadStoreProfile",
            "-XX:InlineFieldMaxFlatSize=-1",
            "-XX:-InlineTypePassFieldsAsArgs",
            "-XX:-InlineTypeReturnedAsFields"
        ),
        new Scenario(2,
            "-XX:-UseACmpProfile",
            "-XX:-UseCompressedOops",
            "-XX:FlatArrayElementMaxOops=0",
            "-XX:FlatArrayElementMaxSize=0",
            "-XX:-UseArrayLoadStoreProfile",
            "-XX:InlineFieldMaxFlatSize=-1",
            "-XX:+InlineTypePassFieldsAsArgs",
            "-XX:+InlineTypeReturnedAsFields",
            "-XX:+StressInlineTypeReturnedAsFields"
        ),
        new Scenario(3,
            "-DVerifyIR=false",
            "-XX:+AlwaysIncrementalInline",
            "-XX:FlatArrayElementMaxOops=0",
            "-XX:FlatArrayElementMaxSize=0",
            "-XX:InlineFieldMaxFlatSize=0",
            "-XX:+InlineTypePassFieldsAsArgs",
            "-XX:+InlineTypeReturnedAsFields"
        ),
        new Scenario(4,
            "-DVerifyIR=false",
            "-XX:FlatArrayElementMaxOops=-1",
            "-XX:FlatArrayElementMaxSize=-1",
            "-XX:InlineFieldMaxFlatSize=0",
            "-XX:+InlineTypePassFieldsAsArgs",
            "-XX:-InlineTypeReturnedAsFields",
            "-XX:-ReduceInitialCardMarks"
        ),
        new Scenario(5,
            "-XX:-UseACmpProfile",
            "-XX:+AlwaysIncrementalInline",
            "-XX:FlatArrayElementMaxOops=5",
            "-XX:FlatArrayElementMaxSize=-1",
            "-XX:-UseArrayLoadStoreProfile",
            "-XX:InlineFieldMaxFlatSize=-1",
            "-XX:-InlineTypePassFieldsAsArgs",
            "-XX:-InlineTypeReturnedAsFields"
        )
    };
}

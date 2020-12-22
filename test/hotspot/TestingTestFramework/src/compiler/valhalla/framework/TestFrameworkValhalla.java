package compiler.valhalla.framework;

import jdk.test.lib.Platform;
import jdk.test.lib.management.InputArguments;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class TestFrameworkValhalla extends TestFramework {

    @Override
    protected void setupDefaultScenarios() {
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-XX:-UseACmpProfile",
                "-XX:+AlwaysIncrementalInline",
                "-XX:FlatArrayElementMaxOops=5",
                "-XX:FlatArrayElementMaxSize=-1",
                "-XX:-UseArrayLoadStoreProfile",
                "-XX:InlineFieldMaxFlatSize=-1",
                "-XX:+InlineTypePassFieldsAsArgs",
                "-XX:+InlineTypeReturnedAsFields"))));
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-XX:-UseACmpProfile",
                "-XX:-UseCompressedOops",
                "-XX:FlatArrayElementMaxOops=5",
                "-XX:FlatArrayElementMaxSize=-1",
                "-XX:-UseArrayLoadStoreProfile",
                "-XX:InlineFieldMaxFlatSize=-1",
                "-XX:-InlineTypePassFieldsAsArgs",
                "-XX:-InlineTypeReturnedAsFields"))));
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-XX:-UseACmpProfile",
                "-XX:-UseCompressedOops",
                "-XX:FlatArrayElementMaxOops=0",
                "-XX:FlatArrayElementMaxSize=0",
                "-XX:-UseArrayLoadStoreProfile",
                "-XX:InlineFieldMaxFlatSize=-1",
                "-XX:+InlineTypePassFieldsAsArgs",
                "-XX:+InlineTypeReturnedAsFields",
                "-XX:+StressInlineTypeReturnedAsFields"))));
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-DVerifyIR=false",
                "-XX:+AlwaysIncrementalInline",
                "-XX:FlatArrayElementMaxOops=0",
                "-XX:FlatArrayElementMaxSize=0",
                "-XX:InlineFieldMaxFlatSize=0",
                "-XX:+InlineTypePassFieldsAsArgs",
                "-XX:+InlineTypeReturnedAsFields"))));
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-DVerifyIR=false",
                "-XX:FlatArrayElementMaxOops=-1",
                "-XX:FlatArrayElementMaxSize=-1",
                "-XX:InlineFieldMaxFlatSize=0",
                "-XX:+InlineTypePassFieldsAsArgs",
                "-XX:-InlineTypeReturnedAsFields",
                "-XX:-ReduceInitialCardMarks"))));
        scenarios.add(new Scenario(new ArrayList<>(Arrays.asList(
                "-XX:-UseACmpProfile",
                "-XX:+AlwaysIncrementalInline",
                "-XX:FlatArrayElementMaxOops=5",
                "-XX:FlatArrayElementMaxSize=-1",
                "-XX:-UseArrayLoadStoreProfile",
                "-XX:InlineFieldMaxFlatSize=-1",
                "-XX:-InlineTypePassFieldsAsArgs",
                "-XX:-InlineTypeReturnedAsFields"))));
    }
}
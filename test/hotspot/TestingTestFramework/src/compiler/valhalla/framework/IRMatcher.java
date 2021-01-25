package compiler.valhalla.framework;

import jdk.test.lib.Asserts;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class IRMatcher {
    private final Map<String, Integer[]> irRulesMap;
    private final Map<String,String> compilations;
    private final Class<?> testClass;
    private final Map<Method, List<String>> fails;

    public IRMatcher(String output, Class<?> testClass) {
        this.irRulesMap = new HashMap<>();
        this.compilations =  new LinkedHashMap<>();
        this.fails = new HashMap<>();
        this.testClass = testClass;
        parseIREncoding(output);
        splitCompilations(output, testClass);
    }

    private void parseIREncoding(String output) {
        String patternString = "(?<=" + IREncodingPrinter.START + "\\R)[\\s\\S]*(?=" + IREncodingPrinter.END + ")";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(output);

        while (matcher.find()) {
            String[] lines = matcher.group(0).split("\\R");
            // Skip first line containing information about the format only
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                String[] splitComma = line.split(",");
                if (splitComma.length < 2) {
                    throw new TestFrameworkException("Invalid IR match rule encoding");
                }
                String testName = splitComma[0];
                Integer[] irRulesIdx = new Integer[splitComma.length - 1];
                for (int j = 1; j < splitComma.length; j++) {
                    irRulesIdx[j - 1] = Integer.valueOf(splitComma[j]);
                }
                irRulesMap.put(testName, irRulesIdx);
            }
        }
    }

    private void splitCompilations(String output, Class<?> testClass) {
        Pattern comp_re = Pattern.compile("\\n\\s+\\d+\\s+\\d+\\s+([% ])([s ])([! ])b([n ])\\s+\\d?\\s+\\S+\\.(?<name>[^.]+::\\S+)\\s+(?<osr>@ \\d+\\s+)?[(]\\d+ bytes[)]");
        Matcher m = comp_re.matcher(output);
        int prev = 0;
        String methodName = null;
        String keyMatchPrefix = testClass.getSimpleName() + "::";
        while (m.find()) {
            if (methodName != null) {
                if (methodName.startsWith(keyMatchPrefix)) {
                    String shortMethodName = methodName.split("::")[1];
                    if (irRulesMap.containsKey(methodName.split("::")[1])) {
                        compilations.put(shortMethodName, output.substring(prev, m.start() + 1));
                    }
                }

            }
            if (m.group("osr") != null) {
                methodName = null;
            } else {
                methodName = m.group("name");
            }
            prev = m.end();
        }
        if (methodName != null) {
            if (methodName.startsWith(keyMatchPrefix)) {
                String shortMethodName = methodName.split("::")[1];
                if (irRulesMap.containsKey(methodName.split("::")[1])) {
                    compilations.put(shortMethodName, output.substring(prev));
                }
            }
        }
    }

    public void applyRules() {
        fails.clear();
        for (Method m : testClass.getDeclaredMethods()) {
            IR[] irAnnos =  m.getAnnotationsByType(IR.class);
            if (irAnnos.length > 0) {
                TestFormat.check(m.isAnnotationPresent(Test.class), "Found @IR annotation at non-@Test method " + m);
                Integer[] ids = irRulesMap.get(m.getName());
                TestFramework.check(ids != null, "Should find method name in validIrRulesMap for " + m);
                TestFramework.check(ids.length > 0, "Did not find any rule indices for " + m);
                TestFramework.check(ids[ids.length - 1] < irAnnos.length, "Invalid IR rule index found in validIrRulesMap for " + m);
                if (ids[0] != IREncodingPrinter.NO_RULE_APPLIED) {
                    // If -1, than there was no matching IR rule for given conditions.
                    applyRuleToMethod(m, irAnnos, ids);
                }
            }
        }
        if (!fails.isEmpty()) {
            StringBuilder builder = new StringBuilder("\n");
            builder.append("One or more @IR rules failed:\n");
            builder.append("-----------------------------\n");
            fails.forEach((method, list) -> {
                builder.append("- Method \"").append(method).append("\":\n");
                list.forEach(s -> builder.append("  * ").append(s.replace("\n", "\n    ").trim()).append("\n"));
                builder.append("\n");
            });
            builder.append("\n");
            Asserts.fail(builder.toString());
        }
    }

    private void applyRuleToMethod(Method m, IR[] irAnnos, Integer[] ids) {
        String testOutput = compilations.get(m.getName());
        if (TestFramework.VERBOSE) {
            System.out.println(testOutput);
        }
        for (Integer id : ids) {
            IR irAnno = irAnnos[id];
            applyFailOn(m, testOutput, irAnno, id + 1);
            applyCount(m, testOutput, irAnno, id + 1);
        }
    }

    private void applyFailOn(Method m, String testOutput, IR irAnno, int annoId) {
        if (irAnno.failOn().length != 0) {
            String failOnRegex = String.join("|", IRNode.mergeNodes(irAnno.failOn()));
            Pattern pattern = Pattern.compile(failOnRegex);
            Matcher matcher = pattern.matcher(testOutput);
            boolean found = matcher.find();
            if (found) {
                addFail(m, irAnno, annoId, matcher, "contains forbidden node(s)");
            }
        }
    }

    private void applyCount(Method m, String testOutput, IR irAnno, int annoId) {
        if (irAnno.counts().length != 0) {
            final List<String> nodesWithCount = IRNode.mergeNodes(irAnno.counts());
            for (int i = 0; i < nodesWithCount.size(); i += 2) {
                String node = nodesWithCount.get(i);
                TestFormat.check(i + 1 < nodesWithCount.size(), "Missing count for IR node \"" + node + "\" at " + m);
                String countString = nodesWithCount.get(i + 1);
                long expectedCount = 0;

                ParsedComparator<Long> parsedComparator = null;
                try {
                    parsedComparator = ParsedComparator.parseComparator(countString);
                    expectedCount = Long.parseLong(parsedComparator.getStrippedString());
                } catch (NumberFormatException e) {
                    TestFormat.fail("Provided invalid count \"" + countString + "\" for IR node \"" + node + "\" at " + m);
                } catch (Exception e) {
                    TestFormat.fail("Invalid comparator in \"" + countString + "\" for count of node " + node, e);
                }
                TestFormat.check(expectedCount >= 0,"Provided invalid negative count \"" + countString + "\" for IR node \"" + node + "\" at " + m);

                Pattern pattern = Pattern.compile(node);
                Matcher matcher = pattern.matcher(testOutput);
                long actualCount = matcher.results().count();
                if (!parsedComparator.getPredicate().test(actualCount, expectedCount)) {
                    String message = "contains wrong number of nodes. Failed constraint: " + actualCount + " (found) " + countString.trim();
                    addFail(m, irAnno, annoId, matcher, message);
                }
            }
        }
    }

    private void addFail(Method m, IR irAnno, int annoId, Matcher matcher, String message) {
        matcher.reset();
        StringBuilder builder = new StringBuilder();
        builder.append("@IR rule ").append(annoId).append(": \"").append(irAnno).append("\"\n");
        builder.append("Failing Regex: ").append(matcher.pattern().toString()).append("\n");
        builder.append("Failure: Graph for '").append(m).append(" ").append(message).append(":\n");
        matcher.results().forEach(r -> builder.append(r.group()).append("\n"));
        List<String> failsList = fails.computeIfAbsent(m, k -> new ArrayList<>());
        failsList.add(builder.toString());
    }
}

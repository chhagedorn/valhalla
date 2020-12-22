package compiler.valhalla.framework;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;

// Only used by TestVM
class IREncodingPrinter {
    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();
    private static final List<Function<String, Object>> longGetters = Arrays.asList(
            WHITE_BOX::getIntVMFlag, WHITE_BOX::getUintVMFlag, WHITE_BOX::getIntxVMFlag,
            WHITE_BOX::getUintxVMFlag, WHITE_BOX::getUint64VMFlag, WHITE_BOX::getSizeTVMFlag);
    public static final String START = "##### IRMatchRulesEncoding - used by TestFramework #####";
    public static final String END = "----- END -----";
    public static final int NO_RULE_APPLIED = -1;
    private final StringBuilder output = new StringBuilder();

    public IREncodingPrinter() {
        output.append(START).append("\n");
        output.append("<method>,{comma separated applied @IR rule ids}\n");
    }

    /**
     * Emits "<method>,{ids}" where {ids} is either:
     * - indices of all @IR rules that should be applied, separated by a comma
     * - "-1" if no @IR rule should not be applied
     */
    public void emitRuleEncoding(Method m) {
        int i = 0;
        ArrayList<Integer> validRules = new ArrayList<>();
        IR[] irAnnos = m.getAnnotationsByType(IR.class);
        for (IR irAnno : irAnnos) {
            if (shouldApplyIrRule(m, irAnno)) {
                validRules.add(i);
            }
            i++;
        }
        if (irAnnos.length != 0) {
            output.append(m.getName());
            if (validRules.isEmpty()) {
                output.append("," + NO_RULE_APPLIED);
            } else {
                for (i = 0; i < validRules.size(); i++) {
                    output.append(",").append(validRules.get(i));
                }
            }
            output.append("\n");
        }
    }

    public void dump() {
        output.append(END);
        System.out.println(output.toString());
    }

    private boolean shouldApplyIrRule(Method m, IR irAnnotation) {
        checkAnnotation(m, irAnnotation);
        if (irAnnotation.applyIf().length != 0) {
            return hasAllRequiredFlags(m, irAnnotation.applyIf(), "applyIf");
        }

        if (irAnnotation.applyIfNot().length != 0) {
            return hasNoRequiredFlags(m, irAnnotation.applyIfNot(), "applyIfNot");
        }

        if (irAnnotation.applyIfAnd().length != 0) {
            return hasAllRequiredFlags(m, irAnnotation.applyIfAnd(), "applyIfAnd");
        }

        if (irAnnotation.applyIfOr().length != 0) {
            return !hasNoRequiredFlags(m, irAnnotation.applyIfOr(), "applyIfOr");
        }
        // No conditions, always apply.
        return true;
    }

    private static void checkAnnotation(Method m, IR irAnnotation) {
        int applyRules = 0;
        if (irAnnotation.applyIfAnd().length != 0) {
            applyRules++;
            if (irAnnotation.applyIfAnd().length <= 2) {
                throw new TestFormatException("Use [applyIf|applyIfNot] or at least 2 conditions for applyIfAnd in @IR at " + m);
            }
        }
        if (irAnnotation.applyIfOr().length != 0) {
            applyRules++;
            if (irAnnotation.applyIfOr().length <= 2) {
                throw new TestFormatException("Use [applyIf|applyIfNot] or at least 2 conditions for applyIfOr in @IR at " + m);
            }
        }
        if (irAnnotation.applyIf().length != 0) {
            applyRules++;
            if (irAnnotation.applyIf().length > 2) {
                throw new TestFormatException("Use [applyIfAnd|applyIfOr] or only 1 condition for applyIf in @IR at " + m);
            }
        }
        if (irAnnotation.applyIfNot().length != 0) {
            applyRules++;
            if (irAnnotation.applyIfNot().length > 2) {
                throw new TestFormatException("Use [applyIfAnd|applyIfOr] or only 1 condition for applyIfNot in @IR at " + m);
            }
        }
        if (applyRules > 1) {
            throw new TestFormatException("Can only use one of [applyIf|applyIfNot|applyIfAnd|applyIfOr] in @IR at " + m);
        }
    }

    private boolean hasAllRequiredFlags(Method m, String[] andRules, String ruleType) {
        for (int i = 0; i < andRules.length; i++) {
            String flag = andRules[i];
            i++;
            if (i == andRules.length) {
                throw new TestFormatException("Missing value for flag " + flag + " in " + ruleType + " for @IR at " + m);
            }
            String value = andRules[i];
            if (!check(m, flag, value)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasNoRequiredFlags(Method m, String[] orRules, String ruleType) {
        for (int i = 0; i < orRules.length; i++) {
            String flag = orRules[i];
            i++;
            if (i == orRules.length) {
                throw new TestFormatException("Missing value for flag " + flag + " in " + ruleType + " for @IR at " + m);
            }
            String value = orRules[i];
            if (check(m, flag, value)) {
                return false;
            }
        }
        return true;
    }

    private boolean check(Method m, String flag, String value) {
        if (value.length() == 0) {
            throw new TestFormatException("Provided empty value for flag " + flag + " at " + m);
        }
        Object actualFlagValue = longGetters.stream()
                .map(f -> f.apply(flag))
                .filter(Objects::nonNull)
                .findAny().orElse(null);
        if (actualFlagValue != null) {
            long actualLongFlagValue = (Long) actualFlagValue;
            long longValue;
            ParsedComparator<Long> parsedComparator;
            try {
                parsedComparator = parseComparator(value.trim());
                longValue = Long.parseLong(parsedComparator.getStrippedString());
            } catch (NumberFormatException e) {
                throw new TestFormatException("Invalid value " + value + " for number based flag " + flag);
            } catch (Exception e) {
                throw new TestFormatException("Invalid comparator in \"" + value + "\" for number based flag " + flag, e);
            }
            return parsedComparator.getPredicate().test(actualLongFlagValue, longValue);
        }
        actualFlagValue = WHITE_BOX.getBooleanVMFlag(flag);
        if (actualFlagValue != null) {
            boolean actualBooleanFlagValue = (Boolean) actualFlagValue;
            boolean booleanValue;
            try {
                booleanValue = Boolean.parseBoolean(value);
            } catch (Exception e) {
                throw new TestFormatException("Invalid value " + value + " for boolean flag " + flag);
            }
            return booleanValue == actualBooleanFlagValue;
        }
        actualFlagValue = WHITE_BOX.getDoubleVMFlag(flag);
        if (actualFlagValue != null) {
            double actualDoubleFlagValue = (Double) actualFlagValue;
            double doubleValue;
            ParsedComparator<Double> parsedComparator;

            try {
                parsedComparator = parseComparator(value);
                doubleValue = Double.parseDouble(parsedComparator.getStrippedString());
            } catch (NumberFormatException e) {
                throw new TestFormatException("Invalid value " + value + " for number based flag " + flag);
            } catch (Exception e) {
                throw new TestFormatException("Invalid comparator in \"" + value + "\" for number based flag " + flag, e);
            }
            return parsedComparator.getPredicate().test(actualDoubleFlagValue, doubleValue);
        }
        actualFlagValue = WHITE_BOX.getStringVMFlag(flag);
        if (actualFlagValue != null) {
            String actualStringFlagValue = (String) actualFlagValue;
            return actualStringFlagValue.equals(value);
        }
        throw new TestFormatException("Could not find flag " + flag);
    }

    private <T extends Comparable<T>> ParsedComparator<T> parseComparator(String value) {
        BiPredicate<T, T> comparison;
        try {
            switch (value.charAt(0)) {
                case '<':
                    if (value.charAt(1) == '=') {
                        comparison = (x, y) -> x.compareTo(y) <= 0;
                        value = value.substring(2).trim();
                    } else {
                        comparison = (x, y) -> x.compareTo(y) < 0;
                        value = value.substring(1).trim();
                    }
                    break;
                case '>':
                    if (value.charAt(1) == '=') {
                        comparison = (x, y) -> x.compareTo(y) >= 0;
                        value = value.substring(2).trim();
                    } else {
                        comparison = (x, y) -> x.compareTo(y) > 0;
                        value = value.substring(1).trim();
                    }
                    break;
                case '!':
                    if (value.charAt(1) == '=') {
                        comparison = (x, y) -> x.compareTo(y) != 0;
                        value = value.substring(2).trim();
                    } else {
                        throw new TestFormatException("Invalid comparator sign used.");
                    }
                    break;
                case '=': // Allowed syntax, equivalent to not using any symbol.
                    value = value.substring(1).trim();
                default:
                    comparison = (x, y) -> x.compareTo(y) == 0;
                    value = value.trim();
                    break;
            }
        } catch (IndexOutOfBoundsException e) {
            throw new TestFormatException("Invalid value format.");
        }
        return new ParsedComparator<>(value, comparison);
    }
}



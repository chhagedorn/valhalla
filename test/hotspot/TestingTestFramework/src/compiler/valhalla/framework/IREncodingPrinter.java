/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package compiler.valhalla.framework;

import sun.hotspot.WhiteBox;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
            TestFormat.check(irAnnotation.applyIfAnd().length > 2,
                             "Use [applyIf|applyIfNot] or at least 2 conditions for applyIfAnd in @IR at " + m);
        }
        if (irAnnotation.applyIfOr().length != 0) {
            applyRules++;
            TestFormat.check(irAnnotation.applyIfOr().length > 2,
                             "Use [applyIf|applyIfNot] or at least 2 conditions for applyIfOr in @IR at " + m);
        }
        if (irAnnotation.applyIf().length != 0) {
            applyRules++;
            TestFormat.check(irAnnotation.applyIf().length <= 2,
                             "Use [applyIfAnd|applyIfOr] or only 1 condition for applyIf in @IR at " + m);
        }
        if (irAnnotation.applyIfNot().length != 0) {
            applyRules++;
            TestFormat.check(irAnnotation.applyIfNot().length <= 2,
                             "Use [applyIfAnd|applyIfOr] or only 1 condition for applyIfNot in @IR at " + m);
        }
        TestFormat.check(applyRules <= 1, "Can only use one of [applyIf|applyIfNot|applyIfAnd|applyIfOr] in @IR at " + m);
    }

    private boolean hasAllRequiredFlags(Method m, String[] andRules, String ruleType) {
        for (int i = 0; i < andRules.length; i++) {
            String flag = andRules[i];
            i++;
            TestFormat.check(i < andRules.length, "Missing value for flag " + flag + " in " + ruleType + " for @IR at " + m);
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
            TestFormat.check(i < orRules.length, "Missing value for flag " + flag + " in " + ruleType + " for @IR at " + m);
            String value = orRules[i];
            if (check(m, flag, value)) {
                return false;
            }
        }
        return true;
    }

    private boolean check(Method m, String flag, String value) {
        TestFormat.check(!value.isEmpty(), "Provided empty value for flag " + flag + " at " + m);
        Object actualFlagValue = longGetters.stream()
                .map(f -> f.apply(flag))
                .filter(Objects::nonNull)
                .findAny().orElse(null);
        if (actualFlagValue != null) {
            long actualLongFlagValue = (Long) actualFlagValue;
            long longValue;
            ParsedComparator<Long> parsedComparator ;
            try {
                parsedComparator = ParsedComparator.parseComparator(value);
                longValue = Long.parseLong(parsedComparator.getStrippedString());
            } catch (NumberFormatException e) {
                TestFormat.fail("Invalid value " + value + " for number based flag " + flag);
                return false;
            } catch (Exception e) {
                TestFormat.fail("Invalid comparator in \"" + value + "\" for number based flag " + flag + ": " + e.getCause());
                return false;
            }
            return parsedComparator.getPredicate().test(actualLongFlagValue, longValue);
        }
        actualFlagValue = WHITE_BOX.getBooleanVMFlag(flag);
        if (actualFlagValue != null) {
            boolean actualBooleanFlagValue = (Boolean) actualFlagValue;
            boolean booleanValue = false;
            try {
                booleanValue = Boolean.parseBoolean(value);
            } catch (Exception e) {
                TestFormat.fail("Invalid value " + value + " for boolean flag " + flag);
            }
            return booleanValue == actualBooleanFlagValue;
        }
        actualFlagValue = WHITE_BOX.getDoubleVMFlag(flag);
        if (actualFlagValue != null) {
            double actualDoubleFlagValue = (Double) actualFlagValue;
            double doubleValue;
            ParsedComparator<Double> parsedComparator;
            try {
                parsedComparator = ParsedComparator.parseComparator(value);
                doubleValue = Double.parseDouble(parsedComparator.getStrippedString());
            } catch (NumberFormatException e) {
                TestFormat.fail("Invalid value " + value + " for number based flag " + flag);
                return false;
            } catch (Exception e) {
                TestFormat.fail("Invalid comparator in \"" + value + "\" for number based flag " + flag + ": " + e.getCause());
                return false;
            }
            return parsedComparator.getPredicate().test(actualDoubleFlagValue, doubleValue);
        }
        actualFlagValue = WHITE_BOX.getStringVMFlag(flag);
        if (actualFlagValue != null) {
            String actualStringFlagValue = (String) actualFlagValue;
            return actualStringFlagValue.equals(value);
        }
        TestFormat.fail("Could not find flag " + flag);
        return false;
    }
}



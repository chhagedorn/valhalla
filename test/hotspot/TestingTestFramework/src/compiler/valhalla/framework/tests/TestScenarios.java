/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;
import jdk.test.lib.Asserts;

public class TestScenarios {
    public static void main(String[] args) {
        Scenario sDefault = new Scenario(0);
        Scenario s1 = new Scenario(1, "-XX:SuspendRetryCount=51");
        Scenario s2 = new Scenario(2, "-XX:SuspendRetryCount=52");
        Scenario s3 = new Scenario(3, "-XX:SuspendRetryCount=53");
        Scenario s3dup = new Scenario(3, "-XX:SuspendRetryCount=53");
        try {
            TestFramework.runWithScenarios(sDefault, s1, s2, s3);
        } catch (TestRunException e) {
            Asserts.assertTrue(e.getMessage().contains("The following scenarios have failed: #0, #1, #3"));
        }
        try {
            TestFramework.runWithScenarios(s1, s2, s3);
        } catch (TestRunException e) {
            Asserts.assertTrue(e.getMessage().contains("The following scenarios have failed: #1, #3"));
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

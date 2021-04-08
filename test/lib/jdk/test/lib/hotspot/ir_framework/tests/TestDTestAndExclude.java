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

package jdk.test.lib.hotspot.ir_framework.tests;

import jdk.test.lib.Utils;
import jdk.test.lib.hotspot.ir_framework.*;
import jdk.test.lib.Asserts;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @summary Test -DTest and -DExclude property flag.
 * @library /test/lib
 * @run driver jdk.test.lib.hotspot.ir_framework.tests.TestDTestAndExclude
 */

public class TestDTestAndExclude {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            run("good1,good2", "", "good");
            run("good1,good2", "bad1", "good");
            run("good1,bad1", "bad1", "good");
            run("good1,bad1", "bad1,good", "good");
            run("bad1,good1", "", "badrun");
            run("bad1,good1", "good1", "badrun");
            run("bad1,good1", "asdf", "badrun");
            run("asdf", "", "empty");
            run("", "good1,good2,bad1", "empty");
            run("bad1", "bad1", "empty");
            run("good1", "asdf,good,good1", "empty");
        } else {
            switch (args[0]) {
                case "good" -> TestFramework.run();
                case "badrun" -> {
                    try {
                        TestFramework.run();
                        throw new RuntimeException("should not reach");
                    } catch (TestVMException e) {
                        Asserts.assertTrue(e.getExceptionInfo().contains("expected bad1 exception"));
                    }
                }
                case "empty" -> {
                    try {
                        TestFramework.run();
                        throw new RuntimeException("should not reach");
                    } catch (NoTestsRunException e) {
                        // Expected
                    }
                }
                default -> throw new RuntimeException("should not reach");
            }
        }
    }

    /**
     * Create a VM and simulate as if it was a driver VM spawned by JTreg that has -DTest/DExclude set as VM or Javaopts
     */
    protected static void run(String dTest, String dExclude, String arg) throws Exception {
        OutputAnalyzer oa;
        ProcessBuilder process = ProcessTools.createJavaProcessBuilder(
                "-Dtest.class.path=" + Utils.TEST_CLASS_PATH, "-Dtest.jdk=" + Utils.TEST_JDK,
                "-Dtest.vm.opts=-DTest=" + dTest + " -DExclude=" + dExclude,
                "jdk.test.lib.hotspot.ir_framework.tests.TestDTestAndExclude", arg);
        oa = ProcessTools.executeProcess(process);
        oa.shouldHaveExitValue(0);
    }

    @Test
    public void good1() {
    }

    @Test
    public void good2() {
    }

    @Test
    public void bad1() {
        throw new RuntimeException("expected bad1 exception");
    }
}


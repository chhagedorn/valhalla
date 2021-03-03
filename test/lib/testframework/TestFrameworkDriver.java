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

package testframework;

import jdk.test.lib.Utils;
import jdk.test.lib.util.ClassFileInstaller;
import jdk.test.lib.management.InputArguments;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Call the driver with jtreg:
 * @library /test/lib
 * @run driver TestFrameworkDriver some.package.Test
 *
 * package some.package;
 *
 * public class Test { ... }
 */
public class TestFrameworkDriver {

    public static void main(String... args) {
        StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
        var test = walker.getCallerClass();
        if (args.length == 0) {
          installWhiteBox();
          runJtregTestInVM(test);
        } else {
            if (args.length != 1 || !"run".equals(args[0])) {
                throw new TestFormatException("unexpected arguments: " + Arrays.toString(args));
            }
            TestFramework.run(test);
        }
    }

    private static void runJtregTestInVM(Class<?> test) {
        var testVMArgs = new ArrayList<String>();
        testVMArgs.add("-Dtest.jdk=" + Utils.TEST_JDK);
        testVMArgs.add("-cp");
        testVMArgs.add(Utils.TEST_CLASS_PATH);
        testVMArgs.addAll(Arrays.asList(InputArguments.getVmInputArgs()));
        testVMArgs.add("-Xbootclasspath/a:.");
        testVMArgs.add("-XX:+UnlockDiagnosticVMOptions");
        testVMArgs.add("-XX:+WhiteBoxAPI");
        testVMArgs.add(test.getName());
        testVMArgs.add("run");
        OutputAnalyzer oa;
        try {
          oa = ProcessTools.executeProcess(ProcessTools.createTestJvm(testVMArgs));
        } catch (Exception e) {
            throw new TestRunException("Failed to execute test VM", e);
        }
        if (oa.getExitValue() != 0) {
            System.err.println(oa.getOutput());
            throw new TestRunException("Non-zero exit value of VM: " + oa.getExitValue());
        }
    }

    private static void installWhiteBox() {
        try {
            ClassFileInstaller.main(WhiteBox.class.getName());
        } catch (Exception e) {
            throw new Error("failed to install whitebox classes", e);
        }
    }
}

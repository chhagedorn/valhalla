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


import jdk.test.lib.Utils;
import jdk.test.lib.management.InputArguments;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;
import testframework.TestFormatException;
import testframework.TestRunException;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new TestFormatException("Must specify at least the test as argument for the driver:\n" +
                                          "@run driver TestFrameworkDriver some.package.Test");
        }
        compileTest();
        installWhiteBox();
        runJtregTestInVM(args);
    }

    private static void runJtregTestInVM(String[] args) throws Exception {
        LinkedList<String> testVMArgs = new LinkedList<>();
        testVMArgs.add("-Dtest.jdk=" + Utils.TEST_JDK);
        testVMArgs.add("-cp");
        testVMArgs.add(Utils.TEST_CLASS_PATH);
        testVMArgs.addAll(Arrays.asList(InputArguments.getVmInputArgs()));
        testVMArgs.add("-Xbootclasspath/a:.");
        testVMArgs.add("-XX:+UnlockDiagnosticVMOptions");
        testVMArgs.add("-XX:+WhiteBoxAPI");
        testVMArgs.addAll(Arrays.asList(args)); // add all specified flags
        OutputAnalyzer oa = ProcessTools.executeProcess(ProcessTools.createTestJvm(testVMArgs));
        if (oa.getExitValue() != 0) {
            System.err.println(oa.getOutput());
            throw new TestRunException("Non-zero exit value of VM: " + oa.getExitValue());
        }
    }

    private static void installWhiteBox() throws Exception {
        ClassFileInstaller.main(WhiteBox.class.getName());
    }

    private static void compileTest() {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        StringWriter output = new StringWriter();
        StandardJavaFileManager fileManager = javac.getStandardFileManager(null, null, null);
        Iterable<? extends JavaFileObject> compilationSource = fileManager.getJavaFileObjects(Utils.TEST_FILE);
        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-sourcepath");
        javacOptions.add(Utils.TEST_CLASS_PATH + File.pathSeparator + Utils.TEST_SRC_PATH);
        javacOptions.add("-d");
        javacOptions.add(Utils.TEST_CLASS_PATH.split(File.pathSeparator)[0]);
        boolean success = javac.getTask(output, fileManager, null, javacOptions,
                                        null, compilationSource).call();
        if (!success) {
            throw new TestFormatException("Compilation of " + Utils.TEST_FILE + " failed: " + output);
        }
    }
}

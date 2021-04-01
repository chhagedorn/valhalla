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

package jdk.test.lib.hotspot.ir_framework;

/**
 * Exception that is thrown if the test VM has thrown any kind of exception (except for {@link TestFormatException}).
 */
public class TestVMException extends RuntimeException {
    static final boolean REPORT_STDOUT = Boolean.parseBoolean(System.getProperty("ReportStdout", "false"));

    private final JVMOutput vmOutput;

    TestVMException(JVMOutput vmOutput) {
        super("There were one or multiple errors. Please check stderr for more information.");
        this.vmOutput = vmOutput;
    }

    /**
     * Get more detailed information about the exception in a pretty format.
     */
    public String getExceptionInfo() {
        String errorMsg = "Command Line:\n" + vmOutput.getCommandLine() + "\n\n";
        int exitCode = vmOutput.getExitCode();
        String stdErr = vmOutput.getStderr();
        String hintStdout = "";
        String stdOut = "";
        if (REPORT_STDOUT || TestFramework.VERBOSE || exitCode == 134) {
            stdOut = "\n\nStandard Output\n---------------\n" + vmOutput.getOutput();
        } else {
            hintStdout  = """
                            ###########################################################
                             To also get the standard output of the test VM run with\s
                             -DReportStdout=true or for even more fine-grained logging
                             use -DVerbose=true.
                            ###########################################################
                            
                          """;
        }
        return errorMsg + "TestFramework test VM exited with code " + exitCode
               + stdOut + "\n\nError Output\n------------\n" + stdErr + "\n\n" + hintStdout;
    }
}

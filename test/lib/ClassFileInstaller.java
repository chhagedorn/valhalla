/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.util.ClassFileInstaller.Manifest;

/**
 * Dump a class file for a class on the class path in the current directory, or
 * in the specified JAR file. This class is usually used when you build a class
 * from a test library, but want to use this class in a sub-process.
 *
 * For example, to build the following library class:
 * test/lib/sun/hotspot/WhiteBox.java
 *
 * You would use the following tags:
 *
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 *
 * JTREG would build the class file under
 * ${JTWork}/classes/test/lib/sun/hotspot/WhiteBox.class
 *
 * With you run your main test class using "@run main MyMainClass", JTREG would setup the
 * -classpath to include "${JTWork}/classes/test/lib/", so MyMainClass would be able to
 * load the WhiteBox class.
 *
 * However, if you run a sub process, and do not wish to use the exact same -classpath,
 * You can use ClassFileInstaller to ensure that WhiteBox is available in the current
 * directory of your test:
 *
 * @run driver ClassFileInstaller sun.hotspot.WhiteBox
 *
 * Or, you can use the -jar option to store the class in the specified JAR file. If a relative
 * path name is given, the JAR file would be relative to the current directory of
 *
 * @run driver ClassFileInstaller -jar myjar.jar sun.hotspot.WhiteBox
 */
public class ClassFileInstaller {
      /**
     * @param args The names of the classes to dump
     * @throws Exception
     * @deprecated use {@link jdk.test.lib.util.ClassFileInstaller} instead
     */
    public static void main(String... args) throws Exception {
        jdk.test.lib.util.ClassFileInstaller.main(args);
    }

    /*
     * You can call ClassFileInstaller.writeJar() from your main test class instead of
     * using "@run ClassFileInstaller -jar ...". E.g.,
     *
     * String jarPath = ClassFileInstaller.getJarPath("myjar.jar", "sun.hotspot.WhiteBox")
     *
     * If you call this API, make sure you build ClassFileInstaller with the following tags:
     *
     * @library testlibrary
     * @build ClassFileInstaller
     */
    public static String writeJar(String jarFile, String... classes) throws Exception {
        return jdk.test.lib.util.ClassFileInstaller.writeJar(jarFile, classes);
    }

    public static String writeJar(String jarFile, Manifest manifest, String... classes) throws Exception {
        return jdk.test.lib.util.ClassFileInstaller.writeJar(jarFile, manifest, classes);
    }

    /**
     * This returns the absolute path to the file specified in "@ClassFileInstaller -jar myjar.jar",
     * In your test program, instead of using the JAR file name directly:
     *
     * String jarPath = "myjar.jar";
     *
     * you should call this function, like:
     *
     * String jarPath = ClassFileInstaller.getJarPath("myjar.jar")
     *
     * The reasons are:
     * (1) Using absolute path makes it easy to cut-and-paste from the JTR file and rerun your
     *     test in any directory.
     * (2) In the future, we may make the JAR file name unique to avoid clobbering
     *     during parallel JTREG execution.
     *
     */
    public static String getJarPath(String jarFileName) {
        return jdk.test.lib.util.ClassFileInstaller.getJarPath(jarFileName);
    }

    public static void writeClassToDisk(String className) throws Exception {
        jdk.test.lib.util.ClassFileInstaller.writeClassToDisk(className);
    }

    public static void writeClassToDisk(String className, String prependPath) throws Exception {
        jdk.test.lib.util.ClassFileInstaller.writeClassToDisk(className, prependPath);
    }

    public static void writeClassToDisk(String className, byte[] bytecode) throws Exception {
        jdk.test.lib.util.ClassFileInstaller.writeClassToDisk(className, bytecode);
    }

    public static void writeClassToDisk(String className, byte[] bytecode, String prependPath) throws Exception {
        jdk.test.lib.util.ClassFileInstaller.writeClassToDisk(className, bytecode, prependPath);
    }
}

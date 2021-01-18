package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.stream.Stream;

public class TestPackagePrivate {
    public static void main(String[] args) {
        TestFramework.run(PackagePrivate.class);
    }
}

class PackagePrivate {
    @Test
    public void test() {
    }

    @Test
    @Arguments(ArgumentValue.DEFAULT)
    public void test2(int x) {
    }
}

package compiler.valhalla.framework.tests;

import compiler.valhalla.framework.*;

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
    @Arguments(Argument.DEFAULT)
    public void test2(int x) {
    }
}

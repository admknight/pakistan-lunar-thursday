package com.scopewise.lunarthursday;

public final class VersionPolicyTest {
    private static int assertions;

    private static void equal(Object expected, Object got) {
        assertions++;
        if (!expected.equals(got)) throw new AssertionError("Expected " + expected + " but got " + got);
    }

    private static void yes(boolean good) {
        assertions++;
        if (!good) throw new AssertionError("Assertion failed");
    }

    public static void main(String[] args) {
        yes(VersionPolicy.compare("v1.3.0-preview", "1.2.0") > 0);
        yes(VersionPolicy.compare("v1.2.1", "1.2.0") > 0);
        yes(VersionPolicy.compare("v2.0.0", "1.99.99") > 0);
        yes(VersionPolicy.compare("v1.3.0", "1.3.0") == 0);
        yes(VersionPolicy.compare("v1.3.0-preview", "1.3.0") < 0);
        yes(VersionPolicy.compare("v1.3.0", "1.3.0-preview") > 0);
        yes(VersionPolicy.compare("v1.1.999", "1.2.0") < 0);
        yes(VersionPolicy.compare("v1.2.0", "1.2.0") == 0);
        yes(VersionPolicy.compare("v1.2.0-preview", "1.2.0") < 0);
        yes(!VersionPolicy.validTag("v1.2"));
        yes(!VersionPolicy.validTag("latest"));
        yes(!VersionPolicy.validTag("1.2.3/evil"));
        yes(!VersionPolicy.validTag("v01.2.3"));
        yes(!VersionPolicy.validTag("v999999999999999.1.2"));
        yes(!VersionPolicy.validTag(null));
        yes(VersionPolicy.validTag("v1.3.0-preview"));
        yes(VersionPolicy.validTag("1.3.2"));
        equal("1.3.0-preview", VersionPolicy.display("v1.3.0-preview"));
        equal("1.3.2", VersionPolicy.display("1.3.2"));
        equal("", VersionPolicy.display("wat"));
        System.out.println("VersionPolicyTest PASS: " + assertions + " assertions");
    }
}

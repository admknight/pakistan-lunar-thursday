package com.scopewise.lunarthursday;

import org.junit.Test;

/** Runs the existing self-checking test programs under the Gradle unit-test task. */
public final class PortableSuiteTest {
    private static final String[] NONE = new String[0];

    @Test public void engine() { EngineTest.main(NONE); }
    @Test public void feedDatePolicy() { FeedDatePolicyTest.main(NONE); }
    @Test public void versionPolicy() { VersionPolicyTest.main(NONE); }
    @Test public void weatherCode() { WeatherCodeTest.main(NONE); }
    @Test public void alarmTonePolicy() { AlarmTonePolicyTest.main(NONE); }
    @Test public void sunsetMath() { SunsetMathTest.main(NONE); }
    @Test public void reminderTimePolicy() { ReminderTimePolicyTest.main(NONE); }
    @Test public void monthEndChecks() { MonthEndChecksTest.main(NONE); }
    @Test public void sentReminderPolicy() { SentReminderPolicyTest.main(NONE); }
}

package com.scopewise.lunarthursday;

/** Tests critical preference migration and selection/fallback behavior without Android SDK. */
public final class AlarmTonePolicyTest {
    public static void main(String[] args) {
        int count = 0;
        String[] choices = {AlarmTonePolicy.NOOR, AlarmTonePolicy.SUKOON,
                AlarmTonePolicy.NIDA, AlarmTonePolicy.PHONE};
        for (String selected : choices) {
            require(AlarmTonePolicy.valid(selected), "valid " + selected); count++;
            require(selected.equals(AlarmTonePolicy.normalize(selected)), "round trip " + selected); count++;
        }
        String[] invalid = {null, "", "unknown", "NOOR", " noor ", "system", "../file", "http://x"};
        for (String candidate : invalid) {
            require(!AlarmTonePolicy.valid(candidate), "reject invalid"); count++;
            require(AlarmTonePolicy.NOOR.equals(AlarmTonePolicy.normalize(candidate)), "fallback to noor"); count++;
        }
        require(AlarmTonePolicy.NOOR.equals(AlarmTonePolicy.normalize(null)), "old installs default noor");count++;
        require(!AlarmTonePolicy.PHONE.equals(AlarmTonePolicy.NOOR), "default is built in");count++;
        System.out.println("AlarmTonePolicyTest: " + count + " checks passed");
    }
    private static void require(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
}

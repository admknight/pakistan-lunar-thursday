package com.scopewise.lunarthursday;

/** Pure selection policy: tolerant of unknown or pre-upgrade saved choices. */
final class AlarmTonePolicy {
    static final String NOOR = "noor";
    static final String SUKOON = "sukoon";
    static final String NIDA = "nida";
    static final String PHONE = "phone";

    private AlarmTonePolicy() { }

    static boolean valid(String id) {
        return NOOR.equals(id) || SUKOON.equals(id) || NIDA.equals(id) || PHONE.equals(id);
    }

    static String normalize(String id) {
        return valid(id) ? id : NOOR;
    }
}

package com.scopewise.lunarthursday

/** Pure selection policy: tolerant of unknown or pre-upgrade saved choices. */
object AlarmTonePolicy {
    const val NOOR = "noor"
    const val SUKOON = "sukoon"
    const val NIDA = "nida"
    const val PHONE = "phone"

    @JvmStatic
    fun valid(id: String?): Boolean = NOOR == id || SUKOON == id || NIDA == id || PHONE == id

    @JvmStatic
    fun normalize(id: String?): String = if (id != null && valid(id)) id else NOOR
}

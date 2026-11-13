package com.scopewise.lunarthursday

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri

/** Shared source selection for previews and live ringing; never modifies system settings. */
object AlarmTonePlayer {
    @JvmStatic
    fun selection(context: Context): String {
        val saved = ReminderStore.prefs(context).getString(ReminderStore.ALARM_TONE_ID, AlarmTonePolicy.NOOR)
        return AlarmTonePolicy.normalize(saved)
    }

    @JvmStatic
    fun save(context: Context, id: String?, phoneUri: Uri?) {
        val chosen = AlarmTonePolicy.normalize(id)
        val edit = ReminderStore.prefs(context).edit().putString(ReminderStore.ALARM_TONE_ID, chosen)
        if (AlarmTonePolicy.PHONE == chosen && phoneUri != null) {
            edit.putString(ReminderStore.ALARM_TONE_URI, phoneUri.toString())
        }
        edit.apply()
    }

    @JvmStatic
    fun selectedPhoneUri(context: Context): Uri? {
        val raw = ReminderStore.prefs(context).getString(ReminderStore.ALARM_TONE_URI, "")
        if (raw == null || raw.length > 2048) return null
        return try {
            val uri = Uri.parse(raw)
            val scheme = uri.scheme
            if ("content" == scheme || "android.resource" == scheme) uri else null
        } catch (badUri: RuntimeException) {
            null
        }
    }

    @JvmStatic
    fun prepare(context: Context, selection: String?, looping: Boolean): MediaPlayer? {
        val id = AlarmTonePolicy.normalize(selection)
        if (AlarmTonePolicy.PHONE == id) {
            val saved = tryUri(context, selectedPhoneUri(context), looping)
            if (saved != null) return saved
            val def = tryUri(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), looping)
            if (def != null) return def
        } else {
            val builtIn = tryResource(context, raw(id), looping)
            if (builtIn != null) return builtIn
        }
        // No silent failure if a selected device tone is removed or inaccessible.
        val backup = tryResource(context, R.raw.hilal_noor, looping)
        if (backup != null) return backup
        return tryUri(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), looping)
    }

    private fun raw(id: String): Int = when (id) {
        AlarmTonePolicy.SUKOON -> R.raw.sukoon
        AlarmTonePolicy.NIDA -> R.raw.hilal_nida
        else -> R.raw.hilal_noor
    }

    private fun fresh(): MediaPlayer {
        val m = MediaPlayer()
        m.setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        return m
    }

    private fun tryResource(context: Context, resource: Int, looping: Boolean): MediaPlayer? {
        val m = fresh()
        return try {
            val fd = context.resources.openRawResourceFd(resource)
            if (fd != null) {
                fd.use { m.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            } else {
                m.setDataSource(context, Uri.parse("android.resource://" + context.packageName + "/" + resource))
            }
            m.prepare()
            m.isLooping = looping
            m
        } catch (failure: Exception) {
            m.release()
            null
        }
    }

    private fun tryUri(context: Context, uri: Uri?, looping: Boolean): MediaPlayer? {
        if (uri == null) return null
        val m = fresh()
        return try {
            m.setDataSource(context, uri)
            m.prepare()
            m.isLooping = looping
            m
        } catch (failure: Exception) {
            m.release()
            null
        }
    }
}

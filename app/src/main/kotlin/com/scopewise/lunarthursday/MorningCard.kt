package com.scopewise.lunarthursday

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate

/** Loads the bundled verses and remembers the chosen translation. */
object VerseStore {
    const val TRANSLATION = "verse_translation"
    @Volatile private var cached: DailyVerse.Library? = null

    /** The bundled library, or null if the file is missing or fails its checks. */
    @JvmStatic
    fun library(context: Context): DailyVerse.Library? {
        cached?.let { return it }
        val loaded = try {
            context.applicationContext.assets.open("verses.json").use {
                DailyVerse.parse(String(it.readBytes(), Charsets.UTF_8))
            }
        } catch (invalidOrMissing: Exception) {
            null
        }
        cached = loaded
        return loaded
    }

    @JvmStatic
    fun translation(context: Context): String =
        DailyVerse.normaliseTranslation(ReminderStore.prefs(context).getString(TRANSLATION, null))

    @JvmStatic
    fun setTranslation(context: Context, id: String) {
        ReminderStore.prefs(context).edit().putString(TRANSLATION, DailyVerse.normaliseTranslation(id)).apply()
    }
}

/** Draws the daily card. The verse and translation are drawn exactly as stored. */
object MorningCardRenderer {
    private const val WIDTH = 1080
    private const val HEIGHT = 1350
    private const val MARGIN = 96

    @JvmStatic
    fun render(context: Context, verse: DailyVerse.Verse, translationId: String, translator: String,
               today: LocalDate, hijriLine: String?): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val background = Paint()
        background.shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(),
            Color.rgb(16, 44, 52), Color.rgb(28, 78, 70), Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), background)

        val textWidth = WIDTH - 2 * MARGIN
        val arabic = verse.arabic
        val urdu = verse.urdu(translationId)
        // Longer verses get smaller type so the whole text always fits.
        var arabicSize = 64f
        var urduSize = 42f
        var arabicBlock: StaticLayout
        var urduBlock: StaticLayout
        while (true) {
            arabicBlock = block(arabic, arabicSize, Color.WHITE, textWidth, true, 1.35f)
            urduBlock = block(urdu, urduSize, Color.rgb(226, 236, 230), textWidth, false, 1.45f)
            if (arabicBlock.height + urduBlock.height <= 820 || arabicSize <= 34f) break
            arabicSize -= 4f
            urduSize -= 2.5f
        }

        val small = TextPaint(Paint.ANTI_ALIAS_FLAG)
        small.color = Color.rgb(190, 214, 204)
        small.textSize = 34f
        small.textAlign = Paint.Align.CENTER
        val centre = WIDTH / 2f

        canvas.drawText(UiText.date(context, today, true), centre, 120f, small)
        if (hijriLine != null) canvas.drawText(hijriLine, centre, 170f, small)

        var y = 250f + (820 - arabicBlock.height - urduBlock.height) / 2f
        draw(canvas, arabicBlock, y)
        y += arabicBlock.height + 56f
        draw(canvas, urduBlock, y)
        y += urduBlock.height + 60f

        val reference = TextPaint(small)
        reference.color = Color.rgb(240, 214, 150)
        reference.textSize = 38f
        canvas.drawText(context.getString(R.string.verse_reference, verse.surah.toString(), verse.ayah.toString()),
            centre, minOf(y, 1160f), reference)

        val credit = TextPaint(small)
        credit.textSize = 26f
        canvas.drawText(context.getString(R.string.verse_credit, translator), centre, HEIGHT - 110f, credit)
        canvas.drawText(context.getString(R.string.app_name), centre, HEIGHT - 64f, credit)
        return bitmap
    }

    private fun block(text: String, size: Float, color: Int, width: Int, bold: Boolean, spacing: Float): StaticLayout {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, spacing)
            .setIncludePad(true)
            .build()
    }

    private fun draw(canvas: Canvas, layout: StaticLayout, top: Float) {
        canvas.save()
        canvas.translate(MARGIN.toFloat(), top)
        layout.draw(canvas)
        canvas.restore()
    }
}

/** Shows today's card and lets the user pick a translation or share the image. */
class MorningCardActivity : LocalizedActivity() {
    private var image: ImageView? = null
    private var current: Bitmap? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        page.gravity = Gravity.CENTER_HORIZONTAL
        val pad = (16 * resources.displayMetrics.density).toInt()
        page.setPadding(pad, pad * 2, pad, pad)
        padForSystemBars(page)

        val title = TextView(this)
        title.text = getString(R.string.verse_title)
        title.textSize = 20f
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        page.addView(title)

        val picture = ImageView(this)
        picture.adjustViewBounds = true
        val pictureParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        pictureParams.topMargin = pad
        page.addView(picture, pictureParams)
        image = picture

        val share = Button(this)
        share.text = getString(R.string.verse_share)
        share.setOnClickListener { share() }
        page.addView(share)

        val choose = Button(this)
        choose.text = getString(R.string.verse_translation)
        choose.setOnClickListener { chooseTranslation() }
        page.addView(choose)

        val notice = TextView(this)
        notice.text = getString(R.string.verse_notice)
        notice.textSize = 12f
        page.addView(notice)

        val scroll = ScrollView(this)
        scroll.addView(page)
        setContentView(scroll)
        draw()
    }

    private fun draw() {
        val library = VerseStore.library(this)
        if (library == null) {
            Toast.makeText(this, getString(R.string.verse_unavailable), Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val now = Instant.now()
        val today = now.atZone(LunarEngine.PAKISTAN).toLocalDate()
        val translation = VerseStore.translation(this)
        // The Hijri date goes on a shared picture only when its month start is confirmed.
        val confirmed = ReminderStore.confirmedStarts(this)
        val hijri = LunarEngine.expectedOnDate(HijriClock.current(this, now).hijriReferenceDate, confirmed)
        val hijriLine = if (hijri != null && hijri.month.basis == LunarEngine.Basis.LIVE_REPORTED_CONFIRMED) {
            String.format(AppLanguage.locale(this), "%d", hijri.day) + " " + UiText.month(this, hijri.month)
        } else null
        val bitmap = MorningCardRenderer.render(this, DailyVerse.forDate(library, today), translation,
            library.translators[translation] ?: "", today, hijriLine)
        current = bitmap
        image?.setImageBitmap(bitmap)
    }

    private fun chooseTranslation() {
        val library = VerseStore.library(this) ?: return
        val ids = DailyVerse.TRANSLATIONS
        val names = ids.map { library.translators[it] ?: it }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.verse_translation))
            .setSingleChoiceItems(names, ids.indexOf(VerseStore.translation(this))) { dialog, index ->
                VerseStore.setTranslation(this, ids[index])
                dialog.dismiss()
                draw()
            }
            .show()
    }

    private fun share() {
        val bitmap = current ?: return
        try {
            val folder = File(cacheDir, "cards")
            folder.mkdirs()
            val file = File(folder, "hilal-thursday-verse.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(send, getString(R.string.verse_share)))
        } catch (failed: Exception) {
            Toast.makeText(this, getString(R.string.verse_share_failed), Toast.LENGTH_LONG).show()
        }
    }
}

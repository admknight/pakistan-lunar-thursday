package com.scopewise.lunarthursday

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
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
        // The look changes with the date: seven colour themes and three ornaments, 21 combinations.
        val day = today.toEpochDay()
        val theme = THEMES[Math.floorMod(day, THEMES.size.toLong()).toInt()]
        val background = Paint()
        background.shader = LinearGradient(0f, 0f, WIDTH * 0.35f, HEIGHT.toFloat(),
            theme[0], theme[1], Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), background)
        val glow = Paint()
        glow.shader = RadialGradient(WIDTH / 2f, HEIGHT * 0.42f, WIDTH * 0.75f,
            Color.argb(46, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), glow)
        val accent = theme[2]
        when (Math.floorMod(day / THEMES.size, 3L).toInt()) {
            0 -> starLattice(canvas, accent)
            1 -> archFrame(canvas, accent)
            else -> crescentSky(canvas, accent)
        }

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
            if (arabicBlock.height + urduBlock.height <= 800 || arabicSize <= 34f) break
            arabicSize -= 4f
            urduSize -= 2.5f
        }

        val small = TextPaint(Paint.ANTI_ALIAS_FLAG)
        small.color = Color.rgb(190, 214, 204)
        small.textSize = 34f
        small.textAlign = Paint.Align.CENTER
        val centre = WIDTH / 2f

        val dates = TextPaint(small)
        dates.typeface = Typeface.DEFAULT_BOLD
        dates.textSize = 48f
        dates.color = Color.WHITE
        canvas.drawText(UiText.date(context, today, true), centre, 128f, dates)
        dates.color = accent
        if (hijriLine != null) canvas.drawText(hijriLine, centre, 194f, dates)

        var y = 270f + (800 - arabicBlock.height - urduBlock.height) / 2f
        draw(canvas, arabicBlock, y)
        y += arabicBlock.height + 56f
        draw(canvas, urduBlock, y)
        y += urduBlock.height + 60f

        val reference = TextPaint(small)
        reference.color = accent
        reference.textSize = 38f
        canvas.drawText(context.getString(R.string.verse_reference, verse.surah.toString(), verse.ayah.toString()),
            centre, minOf(y, 1140f), reference)

        // The source and translator are credited on the screen under the card, not on the image.
        brand(context, canvas, accent)
        return bitmap
    }

    /** App icon and name, centred above the source credit. */
    private fun brand(context: Context, canvas: Canvas, accent: Int) {
        val line = Paint(Paint.ANTI_ALIAS_FLAG)
        line.color = accent
        line.alpha = 110
        line.strokeWidth = 2f
        canvas.drawLine(WIDTH / 2f - 150f, 1190f, WIDTH / 2f + 150f, 1190f, line)
        val name = TextPaint(Paint.ANTI_ALIAS_FLAG)
        name.color = Color.WHITE
        name.textSize = 40f
        name.typeface = Typeface.DEFAULT_BOLD
        val label = context.getString(R.string.app_name)
        val icon = 68
        val gap = 20f
        val total = icon + gap + name.measureText(label)
        val left = (WIDTH - total) / 2f
        val top = 1218
        val drawable = try {
            context.getDrawable(R.mipmap.ic_launcher)
        } catch (missing: RuntimeException) {
            null
        }
        if (drawable != null) {
            drawable.setBounds(left.toInt(), top, left.toInt() + icon, top + icon)
            drawable.draw(canvas)
        }
        canvas.drawText(label, left + icon + gap, top + icon / 2f + 14f, name)
    }

    private fun ornament(accent: Int, alpha: Int, width: Float): Paint {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        paint.color = accent
        paint.alpha = alpha
        return paint
    }

    /** A faint grid of eight-point stars. */
    private fun starLattice(canvas: Canvas, accent: Int) {
        val paint = ornament(accent, 26, 2f)
        val step = 216f
        val r = 62f
        var cy = 0f
        while (cy <= HEIGHT + step) {
            var cx = 0f
            while (cx <= WIDTH + step) {
                canvas.drawRect(cx - r, cy - r, cx + r, cy + r, paint)
                canvas.save()
                canvas.rotate(45f, cx, cy)
                canvas.drawRect(cx - r, cy - r, cx + r, cy + r, paint)
                canvas.restore()
                cx += step
            }
            cy += step
        }
    }

    /** A pointed arch framing the text. */
    private fun archFrame(canvas: Canvas, accent: Int) {
        for ((inset, alpha) in listOf(36f to 90, 54f to 40)) {
            val l = inset
            val r = WIDTH - inset
            val arch = Path()
            arch.moveTo(l, 1150f)
            arch.lineTo(l, 420f)
            arch.cubicTo(l, 250f, WIDTH * 0.30f, 150f, WIDTH / 2f, 40f + inset)
            arch.cubicTo(WIDTH * 0.70f, 150f, r, 250f, r, 420f)
            arch.lineTo(r, 1150f)
            canvas.drawPath(arch, ornament(accent, alpha, 3f))
        }
    }

    /** A thin crescent with a few stars in the upper corner. */
    private fun crescentSky(canvas: Canvas, accent: Int) {
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.color = accent
        fill.alpha = 70
        val moon = Path()
        moon.addCircle(968f, 300f, 74f, Path.Direction.CW)
        val cut = Path()
        cut.addCircle(998f, 279f, 64f, Path.Direction.CW)
        if (moon.op(cut, Path.Op.DIFFERENCE)) canvas.drawPath(moon, fill)
        fill.alpha = 110
        val stars = arrayOf(floatArrayOf(120f, 90f, 4f), floatArrayOf(250f, 210f, 3f), floatArrayOf(700f, 70f, 3f),
            floatArrayOf(90f, 330f, 2.5f), floatArrayOf(980f, 330f, 3f), floatArrayOf(160f, 1090f, 3f),
            floatArrayOf(930f, 1060f, 4f), floatArrayOf(540f, 215f, 2.5f))
        for (s in stars) canvas.drawCircle(s[0], s[1], s[2], fill)
    }

    /** Top colour, bottom colour and accent for each theme; all dark enough for white text. */
    private val THEMES = arrayOf(
        intArrayOf(Color.rgb(16, 44, 52), Color.rgb(28, 78, 70), Color.rgb(240, 214, 150)),
        intArrayOf(Color.rgb(18, 28, 62), Color.rgb(44, 62, 112), Color.rgb(236, 208, 148)),
        intArrayOf(Color.rgb(52, 22, 44), Color.rgb(98, 44, 72), Color.rgb(244, 208, 170)),
        intArrayOf(Color.rgb(12, 50, 60), Color.rgb(20, 96, 104), Color.rgb(250, 224, 170)),
        intArrayOf(Color.rgb(58, 30, 20), Color.rgb(110, 62, 36), Color.rgb(250, 222, 168)),
        intArrayOf(Color.rgb(26, 30, 40), Color.rgb(58, 66, 84), Color.rgb(224, 206, 160)),
        intArrayOf(Color.rgb(34, 24, 66), Color.rgb(72, 52, 118), Color.rgb(240, 212, 160))
    )

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

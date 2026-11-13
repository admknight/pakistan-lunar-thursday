package com.scopewise.lunarthursday

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.view.View
import java.time.LocalTime

/** Battery-friendly time-aware sky and optional location-weather effects. */
class MoonSkyView(context: Context) : View(context) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var scene: WeatherManager.Scene? = null

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setWeather(value: WeatherManager.Scene?) {
        scene = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val save = canvas.save()
        canvas.scale(width / 360f, height / 160f)
        val hour = LocalTime.now().hour
        var early = hour >= 5 && hour < 9
        var late = hour >= 17 && hour < 20
        var night = hour < 5 || hour >= 20
        val weather = scene
        if (weather != null) {
            // Forecast daylight flag is authoritative for current local weather; clock selects dawn/dusk palette.
            night = !weather.day
            early = weather.day && hour >= 5 && hour < 9
            late = weather.day && hour >= 17 && hour < 20
        }
        var palette = when {
            night -> colors(0xFF012B39, 0xFF295475, 0xFF6B6190)
            early -> colors(0xFF5E9AAA, 0xFFF2B4A0, 0xFFFFE8B0)
            late -> colors(0xFF315B80, 0xFFE49886, 0xFFF6C681)
            else -> colors(0xFF2B8DB3, 0xFF7FC6DC, 0xFFD5F0E9)
        }
        val kind = if (weather == null) WeatherCode.CLEAR else WeatherCode.category(weather.code)
        if (kind == WeatherCode.STORM || kind == WeatherCode.RAIN) palette = colors(0xFF263D53, 0xFF52708B, 0xFF8D9CAB)
        if (kind == WeatherCode.FOG) palette = colors(0xFF648C94, 0xFF9DBAC3, 0xFFC7D4CB)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, 0f, 355f, 160f, palette, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, 360f, 160f, p)
        p.shader = null

        if (night || late) {
            p.color = c(0xFFFEE6AD)
            val stars = arrayOf(
                floatArrayOf(31f, 29f, 1.3f), floatArrayOf(69f, 53f, 1.2f), floatArrayOf(126f, 21f, 1.1f),
                floatArrayOf(239f, 20f, 1.6f), floatArrayOf(300f, 41f, 1.5f), floatArrayOf(326f, 92f, 1.0f),
                floatArrayOf(263f, 76f, 1f), floatArrayOf(99f, 104f, 1f))
            for (s in stars) canvas.drawCircle(s[0], s[1], s[2], p)
            // Subtract the cutout: EVEN_ODD also rendered a second stray crescent.
            val crescent = Path()
            crescent.addCircle(180f, 49f, 29f, Path.Direction.CW)
            val cutout = Path()
            cutout.addCircle(193f, 40f, 25f, Path.Direction.CW)
            if (crescent.op(cutout, Path.Op.DIFFERENCE)) canvas.drawPath(crescent, p)
        } else {
            p.color = c(0xFFFFE7A9)
            canvas.drawCircle(184f, 48f, if (early) 21f else 27f, p)
        }
        if (kind != WeatherCode.CLEAR) {
            cloud(canvas, 72f, 55f, 0.88f, if (night) c(0xFF859AB1) else c(0xFFE6EBEE))
            cloud(canvas, 254f, 38f, 0.94f, if (night) c(0xFF7792AA) else c(0xFFF3F7F6))
            if (kind == WeatherCode.FOG) {
                p.color = c(0x99D8E1E1)
                var y = 68
                while (y < 132) {
                    canvas.drawRoundRect(18f, y.toFloat(), 342f, (y + 6).toFloat(), 3f, 3f, p)
                    y += 16
                }
            }
            if (kind == WeatherCode.RAIN || kind == WeatherCode.STORM) {
                p.color = c(0xC6DDE9F5)
                p.strokeWidth = 2.2f
                p.style = Paint.Style.STROKE
                for (i in 0 until 25) {
                    val x = (16 + (i * 41) % 328).toFloat()
                    val y = (58 + (i * 13) % 80).toFloat()
                    canvas.drawLine(x, y, x - 5, y + 13, p)
                }
                p.style = Paint.Style.FILL
            }
            if (kind == WeatherCode.SNOW) {
                p.color = c(0xFFF7FAFF)
                for (i in 0 until 25) {
                    canvas.drawCircle((12 + (i * 37) % 340).toFloat(), (52 + (i * 29) % 97).toFloat(), 1.8f, p)
                }
            }
            if (kind == WeatherCode.STORM) {
                p.color = c(0xFFFFE7AB)
                val bolt = Path()
                bolt.moveTo(280f, 66f); bolt.lineTo(267f, 90f); bolt.lineTo(280f, 90f)
                bolt.lineTo(269f, 113f); bolt.lineTo(290f, 83f); bolt.lineTo(277f, 83f)
                bolt.close()
                canvas.drawPath(bolt, p)
            }
        }
        val hills = Path()
        hills.moveTo(0f, 143f); hills.cubicTo(65f, 126f, 96f, 151f, 146f, 134f)
        hills.cubicTo(205f, 112f, 249f, 139f, 300f, 127f); hills.lineTo(360f, 144f); hills.lineTo(360f, 160f)
        hills.lineTo(0f, 160f); hills.close()
        p.color = if (night) c(0xFF154354) else c(0xFF356F76)
        canvas.drawPath(hills, p)
        // Stylized mosque and arch are decorative artwork, not a live camera view or official lunar-phase chart.
        p.color = if (night) c(0xFF072D37) else c(0xFF234E58)
        canvas.drawRect(110f, 128f, 252f, 160f, p)
        val dome = Path()
        dome.moveTo(129f, 132f); dome.cubicTo(132f, 91f, 230f, 91f, 230f, 132f); dome.close()
        canvas.drawPath(dome, p)
        canvas.drawRect(120f, 117f, 137f, 160f, p)
        canvas.drawRect(224f, 117f, 240f, 160f, p)
        for (x in floatArrayOf(84f, 271f)) {
            canvas.drawRect(x, 95f, x + 10, 160f, p)
            val cap = Path()
            cap.moveTo(x - 4, 96f); cap.quadTo(x + 5, 72f, x + 14, 96f); cap.close()
            canvas.drawPath(cap, p)
            canvas.drawRect(x + 4, 72f, x + 6, 84f, p)
        }
        p.color = if (night || late) c(0xFFFFD995) else c(0xFFDAA45E)
        canvas.drawCircle(180f, 103f, 2.5f, p)
        canvas.drawRect(178.6f, 95f, 181.4f, 104f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = if (night) c(0xFFEAC68D) else c(0xFFEEE5CB)
        val arch = Path()
        arch.moveTo(52f, 160f); arch.lineTo(52f, 79f); arch.cubicTo(52f, 54f, 68f, 39f, 90f, 31f)
        arch.cubicTo(135f, 15f, 147f, 7f, 180f, 3f); arch.cubicTo(213f, 7f, 225f, 15f, 270f, 31f)
        arch.cubicTo(293f, 40f, 308f, 54f, 308f, 79f); arch.lineTo(308f, 160f)
        canvas.drawPath(arch, p)
        p.style = Paint.Style.FILL
        canvas.restoreToCount(save)
    }

    private fun cloud(canvas: Canvas, x: Float, y: Float, scale: Float, color: Int) {
        p.color = color
        canvas.drawOval(x - 29 * scale, y - 11 * scale, x + 32 * scale, y + 9 * scale, p)
        canvas.drawCircle(x - 11 * scale, y - 11 * scale, 17 * scale, p)
        canvas.drawCircle(x + 11 * scale, y - 14 * scale, 22 * scale, p)
    }

    private fun c(argb: Long): Int = argb.toInt()

    private fun colors(a: Long, b: Long, d: Long): IntArray = intArrayOf(a.toInt(), b.toInt(), d.toInt())
}

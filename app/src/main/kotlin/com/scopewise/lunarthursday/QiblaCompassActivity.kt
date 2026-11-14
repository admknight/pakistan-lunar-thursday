package com.scopewise.lunarthursday

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A live compass pointing to the Qibla for the city chosen in Settings. It
 * uses the phone's rotation sensor and corrects magnetic north to true north.
 */
class QiblaCompassActivity : LocalizedActivity(), SensorEventListener {
    private var sensors: SensorManager? = null
    private var rotation: Sensor? = null
    private var dial: Dial? = null
    private var status: TextView? = null
    private var bearing = 0.0
    private var declination = 0f

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val page = LinearLayout(this)
        page.orientation = LinearLayout.VERTICAL
        page.gravity = Gravity.CENTER_HORIZONTAL
        val pad = (20 * resources.displayMetrics.density).toInt()
        page.setPadding(pad, pad * 2, pad, pad)
        padForSystemBars(page)

        val title = TextView(this)
        title.text = getString(R.string.compass_title)
        title.textSize = 20f
        title.gravity = Gravity.CENTER
        page.addView(title)

        val message = TextView(this)
        message.gravity = Gravity.CENTER
        page.addView(message)
        status = message

        val place = HijriClock.cityCoordinates(this)
        if (place == null) {
            message.text = getString(R.string.prayer_choose_city)
            setContentView(page)
            return
        }
        bearing = Qibla.bearing(place[0], place[1])
        declination = GeomagneticField(place[0].toFloat(), place[1].toFloat(), 0f, System.currentTimeMillis()).declination

        val view = Dial(this)
        page.addView(view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        dial = view

        val note = TextView(this)
        note.text = getString(R.string.compass_note)
        note.textSize = 12f
        note.gravity = Gravity.CENTER
        page.addView(note)
        setContentView(page)

        sensors = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        rotation = sensors?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        message.text = if (rotation == null) getString(R.string.compass_no_sensor)
        else getString(R.string.qibla_line, String.format(AppLanguage.locale(this), "%d", Math.round(bearing)))
    }

    override fun onResume() {
        super.onResume()
        val sensor = rotation ?: return
        sensors?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensors?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        val matrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(matrix, event.values)
        val orientation = FloatArray(3)
        SensorManager.getOrientation(matrix, orientation)
        // Heading of the top of the phone, from true north.
        val heading = (Math.toDegrees(orientation[0].toDouble()) + declination + 360.0) % 360.0
        dial?.show(heading, bearing)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            status?.text = getString(R.string.compass_calibrate)
        }
    }

    /** Draws a ring with a north mark and an arrow to the Qibla, turned against the phone's heading. */
    private class Dial(context: Context) : View(context) {
        private var heading = 0.0
        private var bearing = 0.0
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG)
        private val north = Paint(Paint.ANTI_ALIAS_FLAG)
        private val arrow = Paint(Paint.ANTI_ALIAS_FLAG)
        private val label = Paint(Paint.ANTI_ALIAS_FLAG)

        init {
            ring.style = Paint.Style.STROKE
            ring.strokeWidth = 6f
            ring.color = Color.GRAY
            north.color = Color.rgb(200, 60, 60)
            arrow.color = Color.rgb(30, 107, 79)
            label.color = Color.GRAY
            label.textAlign = Paint.Align.CENTER
        }

        fun show(heading: Double, bearing: Double) {
            this.heading = heading
            this.bearing = bearing
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val radius = Math.min(width, height) * 0.4f
            if (radius <= 0f) return
            label.textSize = radius * 0.16f
            canvas.drawCircle(cx, cy, radius, ring)

            // North mark.
            canvas.save()
            canvas.rotate((-heading).toFloat(), cx, cy)
            canvas.drawCircle(cx, cy - radius, radius * 0.05f, north)
            canvas.drawText("N", cx, cy - radius - radius * 0.1f, label)
            // Qibla arrow.
            canvas.rotate(bearing.toFloat(), cx, cy)
            val path = Path()
            path.moveTo(cx, cy - radius * 0.92f)
            path.lineTo(cx - radius * 0.12f, cy + radius * 0.2f)
            path.lineTo(cx, cy + radius * 0.08f)
            path.lineTo(cx + radius * 0.12f, cy + radius * 0.2f)
            path.close()
            canvas.drawPath(path, arrow)
            canvas.restore()
        }
    }
}

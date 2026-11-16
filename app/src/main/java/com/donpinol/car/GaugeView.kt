package com.donpinol.car

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Velocímetro de arco (270°) con brillo azul. Escala 0–200 km/h. */
class GaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val maxValue = 200f
    private val sweepTotal = 270f
    private val startAngle = 135f

    private var shown = 0f
    private var animator: ValueAnimator? = null

    private val d = resources.displayMetrics.density
    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        color = Color.argb(26, 255, 255, 255); strokeWidth = 12 * d
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#1E9BFF"); strokeWidth = 14 * d
        maskFilter = BlurMaskFilter(8 * d, BlurMaskFilter.Blur.NORMAL)
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#3BB0FF"); strokeWidth = 9 * d
    }
    private val labels = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8FA0BA"); textSize = 11 * d
        textAlign = Paint.Align.CENTER
    }
    private val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#EAF0FA"); textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        setShadowLayer(14 * d, 0f, 0f, Color.parseColor("#8C00AAFF"))
    }
    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8FA0BA"); textAlign = Paint.Align.CENTER; letterSpacing = 0.25f
    }
    private val rect = RectF()

    /** true cuando no hay señal GPS: muestra "--". */
    var noSignal = true
        set(v) { field = v; invalidate() }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // necesario para BlurMaskFilter
    }

    fun setSpeed(kmh: Float) {
        val target = kmh.coerceIn(0f, maxValue)
        animator?.cancel()
        animator = ValueAnimator.ofFloat(shown, target).apply {
            duration = 600
            addUpdateListener { shown = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val size = min(width, height).toFloat()
        val pad = 10 * d
        val r = size / 2f - pad
        val cx = width / 2f
        val cy = height / 2f + size * 0.05f
        track.strokeWidth = r * 0.10f
        glow.strokeWidth = r * 0.12f
        arc.strokeWidth = r * 0.075f
        rect.set(cx - r, cy - r, cx + r, cy + r)

        canvas.drawArc(rect, startAngle, sweepTotal, false, track)
        val sweep = sweepTotal * (shown / maxValue)
        if (sweep > 0.5f) {
            canvas.drawArc(rect, startAngle, sweep, false, glow)
            canvas.drawArc(rect, startAngle, sweep, false, arc)
        }
        // Marcas de escala: solo si hay espacio suficiente
        if (r > 90 * d) {
            val lr = r - r * 0.24f
            labels.textSize = r * 0.09f
            var v = 0
            while (v <= maxValue.toInt()) {
                val ang = Math.toRadians((startAngle + sweepTotal * v / maxValue).toDouble())
                val x = cx + lr * cos(ang).toFloat()
                val y = cy + lr * sin(ang).toFloat() + labels.textSize / 3f
                canvas.drawText(v.toString(), x, y, labels)
                v += 40
            }
        }
        // Número de velocidad y unidad, escalados al tamaño del velocímetro
        numPaint.textSize = r * 0.62f
        unitPaint.textSize = r * 0.14f
        val txt = if (noSignal) "--" else Math.round(shown).toString()
        canvas.drawText(txt, cx, cy + numPaint.textSize * 0.30f, numPaint)
        canvas.drawText("KM/H", cx, cy + numPaint.textSize * 0.30f + unitPaint.textSize * 1.6f, unitPaint)
    }
}

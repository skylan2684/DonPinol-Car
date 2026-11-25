package com.donpinol.car

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Futuristic native gauge. The number is real GPS speed; only the arc is limited to its scale. */
class GaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val start = 160f
    private val sweep = 220f
    private val maxSpeed = 200f
    private var shown = 0f
    private var target = 0f
    private var animator: ValueAnimator? = null
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val digits = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F3F8FF")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD_ITALIC)
    }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#91B8D0")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.ITALIC)
    }

    var noSignal = true
        set(value) {
            field = value
            contentDescription = if (value) "Velocidad GPS no disponible" else "Velocidad GPS"
            invalidate()
        }

    fun setSpeed(kmh: Float) {
        val next = if (kmh.isFinite()) kmh.coerceAtLeast(0f) else 0f
        if (next == target) return
        target = next
        animator?.cancel()
        animator = ValueAnimator.ofFloat(shown, next).apply {
            duration = 350
            addUpdateListener {
                shown = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        shown = target
        super.onDetachedFromWindow()
    }

    private fun stroke(color: String, width: Float) {
        p.style = Paint.Style.STROKE
        p.color = Color.parseColor(color)
        p.strokeWidth = width
        p.strokeCap = Paint.Cap.BUTT
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val r = min(w * .45f, h * .59f)
        if (r <= 2f) return
        val cx = w * .5f
        val cy = h * .65f
        rect.set(cx-r, cy-r, cx+r, cy+r)
        stroke("#0C3E59", r*.034f)
        canvas.drawArc(rect, start-6f, sweep+12f, false, p)
        rect.inset(r*.075f, r*.075f)
        stroke("#183446", r*.085f)
        canvas.drawArc(rect, start, sweep, false, p)
        stroke("#805826", r*.085f)
        canvas.drawArc(rect, start+sweep*.82f, sweep*.18f, false, p)

        val amount = if (noSignal) 0f else (shown/maxSpeed).coerceIn(0f,1f)
        if (amount > 0f) {
            val active = sweep*amount
            stroke("#164E71", r*.14f)
            canvas.drawArc(rect, start, active, false, p)
            stroke("#00D5FF", r*.09f)
            canvas.drawArc(rect, start, min(active,sweep*.82f), false, p)
            if (active>sweep*.82f) {
                stroke("#FF9B32", r*.09f)
                canvas.drawArc(rect,start+sweep*.82f,active-sweep*.82f,false,p)
            }
        }

        for (i in 0..20) {
            val a = Math.toRadians((start+sweep*i/20f).toDouble())
            val x = cos(a).toFloat()
            val y = sin(a).toFloat()
            val inner = if(i%5==0) r*.73f else r*.82f
            stroke(if(i>=17) "#BF8645" else if(i/20f<=amount && !noSignal) "#C2F8FF" else "#28556D", if(i%5==0) r*.013f else r*.007f)
            canvas.drawLine(cx+x*inner,cy+y*inner,cx+x*r*.88f,cy+y*r*.88f,p)
        }
        digits.textSize = r*.71f
        val number = if(noSignal) "--" else Math.round(target).toString()
        canvas.drawText(number,cx,cy-r*.035f,digits)
        label.textSize = r*.18f
        canvas.drawText("km/h",cx,cy+r*.22f,label)
    }
}

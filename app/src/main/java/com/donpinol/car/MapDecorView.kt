package com.donpinol.car

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

/** Abstract perspective grid, not navigation data. The map opens in the selected external application. */
class MapDecorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var gradient: LinearGradient? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w,h,oldw,oldh)
        if(h>0) gradient = LinearGradient(0f,0f,0f,h.toFloat(),
            intArrayOf(Color.parseColor("#071827"),Color.parseColor("#0B3045")),
            null,Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w=width.toFloat()
        val h=height.toFloat()
        if(w<=0f || h<=0f) return
        p.style=Paint.Style.FILL
        p.shader=gradient
        c.drawRect(0f,0f,w,h,p)
        p.shader=null
        p.style=Paint.Style.STROKE
        p.strokeWidth=1.4f*resources.displayMetrics.density
        p.color=Color.parseColor("#175572")
        for(i in -5..5) c.drawLine(w*.50f+i*w*.055f,0f,w*.50f+i*w*.24f,h,p)
        for(i in 1..8) {
            val f=i/8f
            val y=h*f*f
            c.drawLine(0f,y,w,y,p)
        }
        p.color=Color.parseColor("#23677C")
        p.strokeWidth=3f*resources.displayMetrics.density
        path.reset()
        path.moveTo(0f,h*.61f)
        path.lineTo(w*.24f,h*.50f)
        path.lineTo(w*.42f,h*.25f)
        path.lineTo(w*.62f,h*.11f)
        path.lineTo(w,h*.09f)
        c.drawPath(path,p)
        path.reset()
        path.moveTo(w*.77f,h)
        path.lineTo(w*.70f,h*.58f)
        path.lineTo(w*.61f,h*.38f)
        path.lineTo(w*.39f,h*.19f)
        c.drawPath(path,p)
    }
}

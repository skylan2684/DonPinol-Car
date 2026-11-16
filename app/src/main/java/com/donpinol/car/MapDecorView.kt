package com.donpinol.car

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/** Fondo decorativo de mapa para la tarjeta de navegación (no es un mapa real). */
class MapDecorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density
    private val bg = Paint().apply { color = Color.parseColor("#08101F") }
    private val road = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#13213A")
        strokeWidth = 12 * d; strokeCap = Paint.Cap.ROUND
    }
    private val street = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#0E1830"); strokeWidth = 5 * d
    }
    private val route = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#55BCFF")
        strokeWidth = 4.5f * d; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF8A1E") }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.parseColor("#66FF8A1E"); strokeWidth = 2 * d
    }
    private val path = Path()

    override fun onDraw(c: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val rx = 22 * d
        val clip = Path().apply { addRoundRect(0f, 0f, w, h, rx, rx, Path.Direction.CW) }
        c.save(); c.clipPath(clip)
        c.drawRect(0f, 0f, w, h, bg)
        c.drawLine(-10f, h * .21f, w + 10, h * .28f, road)
        c.drawLine(-10f, h * .62f, w + 10, h * .55f, road)
        c.drawLine(w * .26f, -10f, w * .35f, h + 10, road)
        c.drawLine(w * .74f, -10f, w * .68f, h + 10, road)
        c.drawLine(-10f, h * .41f, w + 10, h * .40f, street)
        c.drawLine(w * .5f, -10f, w * .52f, h + 10, street)
        c.drawLine(w * .12f, -10f, w * .18f, h + 10, street)
        c.drawLine(w * .88f, -10f, w * .91f, h + 10, street)
        path.reset()
        // Ruta y destino en la franja superior, que no tapan los botones
        path.moveTo(-10f, h * .40f); path.lineTo(w * .52f, h * .40f)
        path.lineTo(w * .51f, h * .24f); path.lineTo(w * .82f, h * .22f)
        c.drawPath(path, route)
        c.drawCircle(w * .84f, h * .22f, 7 * d, dot)
        c.drawCircle(w * .84f, h * .22f, 13 * d, ring)
        c.restore()
    }
}

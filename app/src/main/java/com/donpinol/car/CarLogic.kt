package com.donpinol.car

import java.util.Locale

/**
 * Reglas del launcher que no dependen de Android (se pueden probar sin la radio).
 * La interfaz (diseño) solo muestra lo que esto calcula.
 */
object CarLogic {
    /** Debajo de esta velocidad se muestra 0, para que no "baile" estando parado. */
    const val SPEED_FLOOR_KMH = 2f
    /** Por encima de esta velocidad se considera que el carro va en movimiento. */
    const val MOVING_KMH = 3f
    /** Lecturas de GPS con más error que esto (metros) no cuentan para el viaje. */
    const val MAX_ACCURACY_M = 30f
    /** Un salto mayor que esto (metros) entre dos lecturas se descarta. */
    const val MAX_JUMP_M = 200f

    private val DIRS = arrayOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")

    /** Velocidad que se muestra, en km/h, a partir de la velocidad del GPS en m/s. */
    fun displayKmh(speedMs: Float, hasSpeed: Boolean): Float {
        if (!hasSpeed) return 0f
        val kmh = speedMs * 3.6f
        return if (kmh < SPEED_FLOOR_KMH) 0f else kmh
    }

    /** Punto cardinal en español (N, NE, E, SE, S, SO, O, NO) para un rumbo en grados. */
    fun compass(bearing: Float): String =
        DIRS[(((bearing % 360) + 360) % 360 / 45f + 0.5f).toInt() % 8]

    /** Texto de rumbo, por ejemplo "NE 45°". */
    fun headingText(bearing: Float): String = "${compass(bearing)} ${bearing.toInt()}°"

    /** ¿Esta lectura de GPS suma metros al viaje? */
    fun countsForTrip(accuracyM: Float, shownKmh: Float, jumpM: Float): Boolean =
        accuracyM < MAX_ACCURACY_M && shownKmh > MOVING_KMH && jumpM < MAX_JUMP_M

    /** Texto del recorrido del viaje, por ejemplo "12.3 km". */
    fun tripText(meters: Double): String = String.format(Locale.US, "%.1f km", meters / 1000.0)

    /** Avance de la canción de 0 a 1000. Sin duración conocida devuelve 0. */
    fun progressPermille(positionMs: Long, durationMs: Long): Int {
        if (durationMs <= 0) return 0
        return ((positionMs.coerceIn(0, durationMs) * 1000) / durationMs).toInt()
    }
}

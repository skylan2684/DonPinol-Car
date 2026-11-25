package com.donpinol.car

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CarLogicTest {

    @Test fun velocidadSinDatoEsCero() {
        assertEquals(0f, CarLogic.displayKmh(10f, false), 0f)
    }

    @Test fun velocidadMuyBajaSeMuestraCero() {
        assertEquals(0f, CarLogic.displayKmh(0.4f, true), 0f)   // 1,4 km/h
    }

    @Test fun velocidadSeConvierteDeMetrosPorSegundoAKmh() {
        assertEquals(36f, CarLogic.displayKmh(10f, true), 0.001f)
    }

    @Test fun puntosCardinales() {
        assertEquals("N", CarLogic.compass(0f))
        assertEquals("N", CarLogic.compass(359f))
        assertEquals("NE", CarLogic.compass(45f))
        assertEquals("E", CarLogic.compass(90f))
        assertEquals("SE", CarLogic.compass(135f))
        assertEquals("S", CarLogic.compass(180f))
        assertEquals("SO", CarLogic.compass(225f))
        assertEquals("O", CarLogic.compass(270f))
        assertEquals("NO", CarLogic.compass(315f))
    }

    @Test fun rumboNegativoOMayorDe360() {
        assertEquals("O", CarLogic.compass(-90f))
        assertEquals("E", CarLogic.compass(450f))
    }

    @Test fun textoDeRumbo() {
        assertEquals("NE 45°", CarLogic.headingText(45.4f))
    }

    @Test fun elViajeSoloSumaConBuenaPrecisionYEnMovimiento() {
        assertTrue(CarLogic.countsForTrip(10f, 40f, 15f))
        assertFalse(CarLogic.countsForTrip(50f, 40f, 15f))   // GPS impreciso
        assertFalse(CarLogic.countsForTrip(10f, 2f, 15f))    // parado
        assertFalse(CarLogic.countsForTrip(10f, 40f, 500f))  // salto raro
    }

    @Test fun textoDelViaje() {
        assertEquals("0.0 km", CarLogic.tripText(0.0))
        assertEquals("12.3 km", CarLogic.tripText(12340.0))
    }

    @Test fun avanceDeLaCancion() {
        assertEquals(0, CarLogic.progressPermille(5000, 0))
        assertEquals(500, CarLogic.progressPermille(30_000, 60_000))
        assertEquals(1000, CarLogic.progressPermille(90_000, 60_000))
        assertEquals(0, CarLogic.progressPermille(-5, 60_000))
    }
}

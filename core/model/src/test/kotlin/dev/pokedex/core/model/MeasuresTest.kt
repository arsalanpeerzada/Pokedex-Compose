package dev.pokedex.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class MeasuresTest {

    @Test
    fun `numbers use a decimal point on any locale`() {
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("0.4 m", Measures.height(0.4, Units.Metric))
        } finally {
            Locale.setDefault(before)
        }
    }

    @Test
    fun `metric reads as metres, kilograms and celsius`() {
        assertEquals("0.4 m", Measures.height(0.4, Units.Metric))
        assertEquals("6.0 kg", Measures.weight(6.0, Units.Metric))
        assertEquals("17°C", Measures.temperature(17.0, Units.Metric))
    }

    @Test
    fun `imperial converts with the exact definitions`() {
        // 0.4 m is 15.75 in, which rounds to 16 in: 1 ft 4 in.
        assertEquals("1′ 4″", Measures.height(0.4, Units.Imperial))
        // 1.7 m is 66.93 in, so 67 in: 5 ft 7 in.
        assertEquals("5′ 7″", Measures.height(1.7, Units.Imperial))
        // 6 kg is 13.228 lb.
        assertEquals("13.2 lb", Measures.weight(6.0, Units.Imperial))
        assertEquals("63°F", Measures.temperature(17.0, Units.Imperial))
        assertEquals("32°F", Measures.temperature(0.0, Units.Imperial))
    }
}

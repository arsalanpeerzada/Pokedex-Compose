package dev.pokedex.core.model

import java.util.Locale
import kotlin.math.roundToInt

enum class Units(val label: String) { Metric("Metric"), Imperial("Imperial") }

/**
 * Measurements as shown to the user. The conversion factors are exact by definition. The app is
 * English only, so numbers always use a decimal point, whatever the phone's locale.
 */
object Measures {
    private const val METRES_PER_INCH = 0.0254
    private const val KILOGRAMS_PER_POUND = 0.45359237

    /** "0.4 m" or "1′ 4″". */
    fun height(metres: Double, units: Units): String = when (units) {
        Units.Metric -> String.format(Locale.UK, "%.1f m", metres)
        Units.Imperial -> {
            val totalInches = (metres / METRES_PER_INCH).roundToInt()
            "${totalInches / 12}′ ${totalInches % 12}″"
        }
    }

    /** "6.0 kg" or "13.2 lb". */
    fun weight(kilograms: Double, units: Units): String = when (units) {
        Units.Metric -> String.format(Locale.UK, "%.1f kg", kilograms)
        Units.Imperial -> String.format(Locale.UK, "%.1f lb", kilograms / KILOGRAMS_PER_POUND)
    }

    /** "17°C" or "63°F". */
    fun temperature(celsius: Double, units: Units): String = when (units) {
        Units.Metric -> "${celsius.roundToInt()}°C"
        Units.Imperial -> "${(celsius * 9 / 5 + 32).roundToInt()}°F"
    }
}

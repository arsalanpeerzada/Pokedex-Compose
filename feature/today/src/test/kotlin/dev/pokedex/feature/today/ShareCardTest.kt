package dev.pokedex.feature.today

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import dev.pokedex.core.data.SampleData
import dev.pokedex.core.model.WeatherScene
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
// Real drawing, so pixels can be checked; the default mode records draw calls only.
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShareCardTest {

    @Test
    fun renders_a_four_by_five_card_even_without_artwork() = runTest {
        // No network in tests, so the artwork is missing; the card must still draw.
        val bitmap = ShareCard.render(
            ApplicationProvider.getApplicationContext(),
            ShareCardContent(SampleData.pikachu, WeatherScene.Storm, "Thursday 1 October", "Thunderstorm in Leeds tonight: Electric weather."),
        )
        assertEquals(1080, bitmap.width)
        assertEquals(1350, bitmap.height)
        // The top-left corner carries the storm sky, not an empty canvas.
        assertNotEquals(0, bitmap.getPixel(4, 4))
        // Saved for a look at the design without a device (artwork is missing offline).
        File("build/outputs/share-card-preview.png").apply { parentFile?.mkdirs() }
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

package dev.pokedex.feature.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import dev.pokedex.core.model.WeatherScene
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WallpaperRendererTest {

    /** A stand-in for artwork: a white disc, which the silhouette turns into a glowing shape. */
    private fun fakeArt(): Bitmap = createBitmap(200, 200).also {
        Canvas(it).drawCircle(100f, 100f, 80f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.RED })
    }

    @Test
    fun every_scene_draws_a_frame() {
        val renderer = WallpaperRenderer().apply { resize(1080, 2340) }
        WeatherScene.entries.forEach { scene ->
            val frame = createBitmap(1080, 2340)
            renderer.draw(Canvas(frame), WallpaperState(scene, silhouette(fakeArt(), 0xE6FFFFFF.toInt()), revealed = false), timeMillis = 2_000)
            assertNotEquals("$scene drew nothing", 0, frame.getPixel(10, 10))
            if (scene == WeatherScene.Rain) {
                // Saved for a look at the design without a device.
                File("build/outputs/wallpaper-preview-rain.png").apply { parentFile?.mkdirs() }
                    .outputStream().use { frame.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
    }

    @Test
    fun the_silhouette_keeps_the_shape_but_not_the_colours() {
        val shape = silhouette(fakeArt(), Color.WHITE)
        assertNotEquals(Color.RED, shape.getPixel(100, 100))
        assertNotEquals(0, Color.alpha(shape.getPixel(100, 100)))
        assert(Color.alpha(shape.getPixel(2, 2)) == 0) { "Transparent corners must stay transparent" }
    }
}

package dev.pokedex.feature.wallpaper

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import dev.pokedex.core.designsystem.theme.stops
import dev.pokedex.core.model.WeatherScene
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** What the wallpaper shows. [art] is a silhouette until today's Pokémon is solved. */
data class WallpaperState(val scene: WeatherScene, val art: Bitmap?, val revealed: Boolean)

/**
 * Draws one frame: the day's sky, a weather effect and today's Pokémon floating in its glow.
 * Pure drawing with no Android lifecycle, so a test can render frames.
 */
class WallpaperRenderer {

    private var width = 0
    private var height = 0
    private var particles: List<Particle> = emptyList()
    private var sky: Paint? = null
    private var skyScene: WeatherScene? = null

    private data class Particle(val x: Float, val y: Float, val speed: Float, val size: Float, val phase: Float)

    fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
        // A fixed seed: the same layout every time, no jumps when the wallpaper restarts.
        val random = Random(25)
        particles = List(90) { Particle(random.nextFloat(), random.nextFloat(), 0.5f + random.nextFloat(), 0.5f + random.nextFloat(), random.nextFloat()) }
        sky = null
    }

    fun draw(canvas: Canvas, state: WallpaperState, timeMillis: Long) {
        if (width == 0 || height == 0) return
        val t = timeMillis / 1000f
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), skyPaint(state.scene))
        weather(canvas, state.scene, t)
        pokemon(canvas, state, t)
    }

    private fun skyPaint(scene: WeatherScene): Paint {
        sky?.takeIf { skyScene == scene }?.let { return it }
        // The deep stops: a wallpaper sits behind white icons and text.
        val (top, bottom) = scene.stops(dark = true)
        return Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, height.toFloat(), top.toInt(), bottom.toInt(), Shader.TileMode.CLAMP)
        }.also {
            sky = it
            skyScene = scene
        }
    }

    private val effect = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun weather(canvas: Canvas, scene: WeatherScene, t: Float) {
        val w = width.toFloat()
        val h = height.toFloat()
        when (scene) {
            WeatherScene.Rain, WeatherScene.Storm -> {
                effect.color = 0x55FFFFFF
                effect.strokeWidth = 3f
                particles.forEach { p ->
                    val y = ((p.y + t * 0.9f * p.speed) % 1f) * (h + 80) - 40
                    val x = p.x * w - y * 0.08f
                    canvas.drawLine(x, y, x - 8f, y + 34f * p.size, effect)
                }
                // A flash every few seconds, as on Today.
                if (scene == WeatherScene.Storm && (t % 6f) in 4.6f..4.75f) canvas.drawColor(0x40FFFFFF)
            }
            WeatherScene.Snow -> {
                effect.color = 0xBBFFFFFF.toInt()
                particles.forEach { p ->
                    val y = ((p.y + t * 0.05f * p.speed) % 1f) * h
                    val x = p.x * w + sin((t + p.phase * 10) * 0.8f) * 18f
                    canvas.drawCircle(x, y, 3f + 4f * p.size, effect)
                }
            }
            WeatherScene.Night -> particles.take(50).forEach { p ->
                val twinkle = 0.35f + 0.65f * ((sin((t * 0.9f + p.phase * 6f) * PI).toFloat() + 1f) / 2f)
                effect.color = (((255 * twinkle).toInt() shl 24) or 0xFFFFFF)
                canvas.drawCircle(p.x * w, p.y * h * 0.6f, 1.5f + 2f * p.size, effect)
            }
            WeatherScene.Wind -> {
                effect.color = 0x33FFFFFF
                effect.strokeWidth = 4f
                particles.take(24).forEach { p ->
                    val x = ((p.x + t * 0.12f * p.speed) % 1.2f) * w - 0.1f * w
                    val y = p.y * h
                    canvas.drawLine(x, y, x + 120f * p.size, y, effect)
                }
            }
            else -> particles.take(14).forEach { p ->
                // Soft drifting lights on clear and cloudy days.
                val x = p.x * w + sin(t * 0.2f + p.phase * 6f) * 40f
                val y = p.y * h + sin(t * 0.15f + p.phase * 4f) * 30f
                val r = 40f + 60f * p.size
                effect.shader = RadialGradient(x, y, r, 0x22FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
                canvas.drawCircle(x, y, r, effect)
                effect.shader = null
            }
        }
    }

    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val image = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val target = RectF()

    private fun pokemon(canvas: Canvas, state: WallpaperState, t: Float) {
        val art = state.art ?: return
        val size = minOf(width, height) * 0.62f
        val cx = width / 2f
        // Lower half, clear of the clock; a slow bob, as on Today.
        val cy = height * 0.62f + sin(t * 2f * PI.toFloat() / 2.6f) * 12f
        glow.shader = RadialGradient(cx, cy, size * 0.75f, if (state.revealed) 0x66FFFFFF else 0x44FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        canvas.drawCircle(cx, cy, size * 0.75f, glow)
        target.set(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        canvas.drawBitmap(art, null, target, image)
    }
}

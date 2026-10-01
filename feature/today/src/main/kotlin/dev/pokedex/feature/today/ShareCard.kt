package dev.pokedex.feature.today

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.graphics.withTranslation
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import dev.pokedex.core.designsystem.R
import dev.pokedex.core.designsystem.theme.stops
import dev.pokedex.core.model.Pokemon
import dev.pokedex.core.model.WeatherScene
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** What goes on the card. Only shared after the reveal, so it's never a spoiler for the sharer. */
data class ShareCardContent(
    val pokemon: Pokemon,
    val scene: WeatherScene,
    val dateLabel: String,
    val reason: String?,
)

/**
 * Today's reveal as a 1080 x 1350 image (4:5, the size Instagram and LinkedIn crop to least),
 * drawn with the app's fonts and the day's sky, then handed to the share sheet.
 */
object ShareCard {

    private const val WIDTH = 1080
    private const val HEIGHT = 1350

    suspend fun share(context: Context, content: ShareCardContent) {
        val file = withContext(Dispatchers.IO) {
            val bitmap = render(context, content)
            val dir = File(context.cacheDir, "shares").apply { mkdirs() }
            // One file at a time is enough; older cards are replaced.
            dir.listFiles()?.forEach { it.delete() }
            File(dir, "pokedex-today-${content.pokemon.id}.png").also { f ->
                f.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.shares", file)
        val text = buildString {
            append("Today's Pokémon is ").append(content.pokemon.name).append('!')
            content.reason?.let { append(' ').append(it) }
        }
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // ClipData lets the share sheet show a preview of the image.
        send.clipData = ClipData.newRawUri(null, uri)
        context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    }

    suspend fun render(context: Context, content: ShareCardContent): Bitmap {
        val artwork = loadArtwork(context, content.pokemon.artworkUrl)
        return withContext(Dispatchers.Default) { draw(context, content, artwork) }
    }

    private fun draw(context: Context, content: ShareCardContent, artwork: Bitmap?): Bitmap {
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)
        // The deep (dark) stops keep white text readable on every scene.
        val (top, bottom) = content.scene.stops(dark = true)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), top.toInt(), bottom.toInt(), Shader.TileMode.CLAMP)
        })

        val heading = font(context, R.font.fredoka_variable, 700)
        val body = font(context, R.font.nunito_variable, 700)
        val numbers = font(context, R.font.silkscreen_regular, 400)

        text(canvas, "TODAY'S POKÉMON", body, 38f, 0xCCFFFFFF.toInt(), y = 96f)
        text(canvas, content.dateLabel, body, 36f, 0x99FFFFFF.toInt(), y = 146f)

        // A soft glow behind the artwork, like the reveal on screen.
        val cx = WIDTH / 2f
        val cy = 520f
        canvas.drawCircle(cx, cy, 380f, Paint().apply {
            shader = RadialGradient(cx, cy, 380f, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP)
        })
        artwork?.let {
            val size = 640
            canvas.drawBitmap(it.scale(size, size), cx - size / 2f, cy - size / 2f, Paint(Paint.FILTER_BITMAP_FLAG))
        }

        // Silkscreen is capitals-only and made for numbers, so the category uses Nunito.
        content.pokemon.category?.let { text(canvas, it, body, 38f, 0xCCFFFFFF.toInt(), y = 868f) }
        text(canvas, content.pokemon.name, heading, 108f, 0xFFFFFFFF.toInt(), y = 966f)
        text(canvas, "#${content.pokemon.number}", numbers, 40f, 0xCCFFFFFF.toInt(), y = 1026f)

        content.reason?.let { reason ->
            val box = RectF(72f, 1066f, WIDTH - 72f, 1236f)
            canvas.drawRoundRect(box, 36f, 36f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2EFFFFFF })
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { typeface = body; textSize = 36f; color = 0xFFFFFFFF.toInt() }
            val layout = StaticLayout.Builder.obtain(reason, 0, reason.length, paint, (box.width() - 72).toInt())
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(3)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build()
            canvas.withTranslation(box.left + 36, box.centerY() - layout.height / 2f) { layout.draw(this) }
        }

        text(canvas, "Pokedex  ·  unofficial fan app", body, 30f, 0x99FFFFFF.toInt(), y = 1300f)
        return bitmap
    }

    private fun text(canvas: Canvas, value: String, typeface: Typeface, size: Float, colour: Int, y: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = size
            color = colour
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(TextUtils.ellipsize(value, TextPaint(paint), WIDTH - 120f, TextUtils.TruncateAt.END).toString(), WIDTH / 2f, y, paint)
    }

    /** The bundled variable fonts at a given weight (weights need Android 9; earlier versions use bold). */
    private fun font(context: Context, res: Int, weight: Int): Typeface {
        val base = ResourcesCompat.getFont(context, res) ?: Typeface.DEFAULT
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Typeface.create(base, weight, false)
        else Typeface.create(base, if (weight >= 600) Typeface.BOLD else Typeface.NORMAL)
    }

    private suspend fun loadArtwork(context: Context, url: String): Bitmap? {
        val request = ImageRequest.Builder(context).data(url).size(640).allowHardware(false).build()
        return (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
    }
}

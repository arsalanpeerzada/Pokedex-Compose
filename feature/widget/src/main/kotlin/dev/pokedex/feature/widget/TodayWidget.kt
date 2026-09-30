package dev.pokedex.feature.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.pokedex.core.data.DailyRepository
import dev.pokedex.core.domain.TodayPicker
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun picker(): TodayPicker
    fun daily(): DailyRepository
}

/** What the widget shows. It never names the Pokémon until it's been guessed in the app. */
internal sealed interface WidgetState {
    data object NoData : WidgetState
    data class Mystery(val silhouette: Bitmap?) : WidgetState
    data class Revealed(val name: String, val number: String, val artwork: Bitmap?) : WidgetState
}

private val White = ColorProvider(Color.White)
private val WhiteSoft = ColorProvider(Color(0xE6FFFFFF))

/** Deep plum at 94%, the same silhouette colour as Today's scene. */
private const val SILHOUETTE = 0xF0120B3A.toInt()

private val Small = DpSize(110.dp, 110.dp)
private val Wide = DpSize(220.dp, 110.dp)

class TodayWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(Small, Wide))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
        provideContent {
            Box(
                GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_background))
                    .padding(12.dp)
                    .then(if (open != null) GlanceModifier.clickable(actionStartActivity(open)) else GlanceModifier),
                contentAlignment = Alignment.Center,
            ) {
                Content(state)
            }
        }
    }

    private suspend fun loadState(context: Context): WidgetState {
        val entry = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        // The same saved pick as Today, so the widget and the app always agree.
        val pick = entry.picker()() ?: return WidgetState.NoData
        val answer = pick.answer
        val solved = entry.daily().result(pick.epochDay).first()?.solved == true
        val artwork = loadBitmap(context, answer.artworkUrl)
        return if (solved) {
            WidgetState.Revealed(answer.name, answer.number, artwork)
        } else {
            WidgetState.Mystery(artwork?.let { silhouette(it, SILHOUETTE) })
        }
    }
}

@Composable
private fun Content(state: WidgetState) {
    val wide = LocalSize.current.width >= Wide.width
    when (state) {
        WidgetState.NoData -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Today's Pokémon", style = TextStyle(color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(4.dp))
            Text("Open Pokedex once to download it.", style = TextStyle(color = WhiteSoft, fontSize = 12.sp))
        }
        is WidgetState.Mystery -> Layout(
            wide = wide,
            image = state.silhouette,
            imageDescription = "Mystery Pokémon",
            title = "Who's today's Pokémon?",
            subtitle = "Tap to guess",
        )
        is WidgetState.Revealed -> Layout(
            wide = wide,
            image = state.artwork,
            imageDescription = state.name,
            title = state.name,
            subtitle = "#${state.number} · caught today",
        )
    }
}

@Composable
private fun Layout(wide: Boolean, image: Bitmap?, imageDescription: String, title: String, subtitle: String) {
    val titleStyle = TextStyle(color = White, fontSize = if (wide) 17.sp else 14.sp, fontWeight = FontWeight.Bold)
    val subtitleStyle = TextStyle(color = WhiteSoft, fontSize = 12.sp)
    if (wide) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Art(image, imageDescription, 84)
            Spacer(GlanceModifier.width(10.dp))
            Column {
                Text(title, style = titleStyle, maxLines = 2)
                Spacer(GlanceModifier.height(2.dp))
                Text(subtitle, style = subtitleStyle, maxLines = 1)
            }
        }
    } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Art(image, imageDescription, 64)
            Text(title, style = titleStyle, maxLines = 2)
            Text(subtitle, style = subtitleStyle, maxLines = 1)
        }
    }
}

@Composable
private fun Art(image: Bitmap?, description: String, sizeDp: Int) {
    if (image == null) {
        Spacer(GlanceModifier.size(sizeDp.dp))
    } else {
        Image(
            provider = ImageProvider(image),
            contentDescription = description,
            modifier = GlanceModifier.size(sizeDp.dp).semantics { contentDescription = description },
        )
    }
}

/** Official artwork from Coil's cache (or the network), small enough for a widget. */
private suspend fun loadBitmap(context: Context, url: String): Bitmap? {
    val request = ImageRequest.Builder(context).data(url).size(256).allowHardware(false).build()
    val result = SingletonImageLoader.get(context).execute(request)
    return (result as? SuccessResult)?.image?.toBitmap()
}

/** The artwork's shape filled with one colour, keeping its transparency. */
private fun silhouette(source: Bitmap, colour: Int): Bitmap {
    val out = createBitmap(source.width, source.height)
    Canvas(out).drawBitmap(source, 0f, 0f, Paint().apply { colorFilter = PorterDuffColorFilter(colour, PorterDuff.Mode.SRC_IN) })
    return out
}

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}

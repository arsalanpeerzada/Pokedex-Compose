package dev.pokedex.feature.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.core.graphics.createBitmap
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
import dev.pokedex.core.model.WeatherScene
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WallpaperEntryPoint {
    fun picker(): TodayPicker
    fun daily(): DailyRepository
}

/**
 * Today as a live wallpaper. It draws only while visible, at about 30 frames a second, and
 * refreshes today's pick at most every half hour or when the date changes.
 */
class TodayWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = TodayEngine()

    private inner class TodayEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val renderer = WallpaperRenderer()
        private var state = WallpaperState(WeatherScene.Night, art = null, revealed = false)
        private var visible = false
        private var loadedAt = 0L
        private var loadedDay = -1L
        private val frame = Runnable { drawFrame() }

        override fun onVisibilityChanged(isVisible: Boolean) {
            visible = isVisible
            handler.removeCallbacks(frame)
            if (isVisible) {
                refreshIfStale()
                drawFrame()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            renderer.resize(width, height)
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(frame)
        }

        override fun onDestroy() {
            handler.removeCallbacks(frame)
            scope.cancel()
        }

        /** Reloads on a new day, after half an hour, or as soon as today's Pokémon has been solved. */
        private fun refreshIfStale() {
            val today = LocalDate.now().toEpochDay()
            val stale = today != loadedDay || SystemClock.elapsedRealtime() - loadedAt >= REFRESH_MILLIS || state.art == null
            scope.launch {
                val entry = EntryPointAccessors.fromApplication(applicationContext, WallpaperEntryPoint::class.java)
                // A cheap local read, so a reveal in the app shows up the next time the home screen does.
                val solvedNow = entry.daily().result(today).first()?.solved == true
                if (!stale && solvedNow == state.revealed) return@launch
                loadedAt = SystemClock.elapsedRealtime()
                loadedDay = today
                val pick = entry.picker()() ?: return@launch
                val solved = entry.daily().result(pick.epochDay).first()?.solved == true
                val artwork = loadBitmap(applicationContext, pick.answer.artworkUrl)
                state = WallpaperState(
                    scene = pick.scene,
                    // The mystery shape glows white on the deep sky; the colours only after the guess.
                    art = if (solved) artwork else artwork?.let { silhouette(it, 0xE6FFFFFF.toInt()) },
                    revealed = solved,
                )
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                canvas?.let { renderer.draw(it, state, SystemClock.uptimeMillis()) }
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
            handler.removeCallbacks(frame)
            if (visible) handler.postDelayed(frame, FRAME_MILLIS)
        }
    }

    private companion object {
        const val FRAME_MILLIS = 33L
        const val REFRESH_MILLIS = 30 * 60 * 1000L
    }
}

private suspend fun loadBitmap(context: Context, url: String): Bitmap? {
    val request = ImageRequest.Builder(context).data(url).size(720).allowHardware(false).build()
    return (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image?.toBitmap()
}

/** The artwork's shape filled with one colour, keeping its transparency. */
internal fun silhouette(source: Bitmap, colour: Int): Bitmap {
    val out = createBitmap(source.width, source.height)
    Canvas(out).drawBitmap(source, 0f, 0f, Paint().apply { colorFilter = PorterDuffColorFilter(colour, PorterDuff.Mode.SRC_IN) })
    return out
}

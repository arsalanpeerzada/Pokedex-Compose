package dev.pokedex.feature.detail

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Streams a cry from PokeAPI's cries repository. One sound at a time; released with the screen. */
class CryPlayer {
    var playing by mutableStateOf(false)
        private set

    private var player: MediaPlayer? = null

    fun play(url: String) {
        release()
        playing = true
        val mp = MediaPlayer()
        player = mp
        mp.setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
        )
        mp.setOnPreparedListener { it.start() }
        mp.setOnCompletionListener { release() }
        mp.setOnErrorListener { _, _, _ -> release(); true }
        try {
            mp.setDataSource(url)
            mp.prepareAsync()
        } catch (_: Exception) {
            release()
        }
    }

    fun release() {
        player?.release()
        player = null
        playing = false
    }
}

@Composable
fun rememberCryPlayer(): CryPlayer {
    val player = remember { CryPlayer() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

package my.kereta.launcher

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationManagerCompat

data class NowPlaying(
    val title: String,
    val artist: String,
    val art: ImageBitmap?,
    val playing: Boolean,
    val position: Long,
    val duration: Long,
    val updatedAt: Long,
    val speed: Float,
    val pkg: String,
    val appName: String
) {
    fun positionNow(): Long {
        val base = position.coerceAtLeast(0L)
        val p = if (playing) base + ((SystemClock.elapsedRealtime() - updatedAt) * speed).toLong() else base
        return if (duration > 0) p.coerceIn(0L, duration) else p.coerceAtLeast(0L)
    }
}

/** Baca dan kawal lagu yang sedang dimainkan oleh mana-mana app muzik. */
class MediaHub(private val context: Context) {
    val nowPlaying = mutableStateOf<NowPlaying?>(null)
    val permitted = mutableStateOf(false)

    private val handler = Handler(Looper.getMainLooper())
    private val manager: MediaSessionManager? = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, MediaListener::class.java)
    private var controller: MediaController? = null
    private var listening = false
    private var artSource: Bitmap? = null
    private var artCache: ImageBitmap? = null

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list) }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publish()
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publish()
        }

        override fun onSessionDestroyed() {
            refresh()
        }
    }

    fun isPermitted(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun start() {
        permitted.value = isPermitted()
        val m = manager
        if (!permitted.value || m == null) {
            detach()
            nowPlaying.value = null
            return
        }
        try {
            if (!listening) {
                m.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
                listening = true
            }
            pick(m.getActiveSessions(component))
        } catch (e: SecurityException) {
            permitted.value = false
        }
    }

    fun stop() {
        if (listening) {
            try {
                manager?.removeOnActiveSessionsChangedListener(sessionsListener)
            } catch (e: Exception) {
                // abaikan
            }
            listening = false
        }
        detach()
    }

    private fun refresh() {
        try {
            pick(manager?.getActiveSessions(component))
        } catch (e: SecurityException) {
            detach()
            nowPlaying.value = null
        }
    }

    private fun detach() {
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun pick(list: List<MediaController>?) {
        val sessions = list.orEmpty()
        val chosen = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull()
        if (chosen?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = chosen
            chosen?.registerCallback(callback, handler)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        if (c == null) {
            nowPlaying.value = null
            return
        }
        val md = c.metadata
        val st = c.playbackState
        val title = md?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = md?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: md?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val bitmap = md?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: md?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        if (bitmap !== artSource) {
            artSource = bitmap
            artCache = try {
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
        val state = st?.state
        val updated = st?.lastPositionUpdateTime ?: 0L
        nowPlaying.value = NowPlaying(
            title = title,
            artist = artist,
            art = artCache,
            playing = state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING,
            position = st?.position ?: 0L,
            duration = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            updatedAt = if (updated > 0L) updated else SystemClock.elapsedRealtime(),
            speed = st?.playbackSpeed ?: 1f,
            pkg = c.packageName,
            appName = Apps.label(context, c.packageName) ?: c.packageName
        )
    }

    fun playPause() {
        val c = controller
        if (c != null) {
            if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause()
            else c.transportControls.play()
        } else {
            sendKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        }
    }

    fun next() {
        val c = controller
        if (c != null) c.transportControls.skipToNext() else sendKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun previous() {
        val c = controller
        if (c != null) c.transportControls.skipToPrevious() else sendKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    private fun sendKey(code: Int) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}

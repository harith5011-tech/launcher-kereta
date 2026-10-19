package my.kereta.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

sealed interface Overlay {
    data object None : Overlay
    data object AllApps : Overlay
    data object SettingsPanel : Overlay
    data class Pick(val slot: String, val title: String, val fromSettings: Boolean) : Overlay
}

/** Semua keadaan launcher di satu tempat. */
class LauncherState(val context: Context) {
    val prefs = Prefs(context)
    val media = MediaHub(context)
    val location = LocationHub(context)

    val apps = mutableStateOf<List<AppEntry>>(emptyList())
    val overlay = mutableStateOf<Overlay>(Overlay.None)
    val bt = mutableStateOf(BtStatus(false, "Bluetooth"))
    val net = mutableStateOf(NetStatus(false, "Luar talian"))
    val weather = mutableStateOf<WeatherNow?>(null)

    fun refreshStatus() {
        bt.value = Status.bluetooth(context)
        net.value = Status.network(context)
    }

    fun loadApps(scope: CoroutineScope) {
        scope.launch {
            val px = (64 * context.resources.displayMetrics.density).roundToInt().coerceAtLeast(48)
            val list = withContext(Dispatchers.IO) {
                runCatching { Apps.load(context, px) }.getOrDefault(emptyList())
            }
            apps.value = list
        }
    }

    /** App untuk butang tertentu: pilihan pengguna, atau app yang dikesan secara automatik. */
    fun slotPackage(slot: String): String? = prefs.slots[slot] ?: when (slot) {
        "nav" -> Apps.firstInstalled(context, Apps.NAV_CANDIDATES)
        "music" -> Apps.firstInstalled(context, Apps.MUSIC_CANDIDATES)
        else -> null
    }

    fun openSlot(slot: String, title: String) {
        val pkg = slotPackage(slot)
        if (pkg != null && Apps.launch(context, pkg)) return
        val fallback = Apps.defaultIntent(slot)
        if (fallback != null && Apps.start(context, fallback)) return
        overlay.value = Overlay.Pick(slot, title, false)
    }

    fun pickFor(slot: String, title: String, fromSettings: Boolean = false) {
        overlay.value = Overlay.Pick(slot, title, fromSettings)
    }

    fun openMusic() {
        val playing = media.nowPlaying.value
        if (playing != null && Apps.launch(context, playing.pkg)) return
        openSlot("music", "Muzik")
    }

    fun goHome() {
        val address = prefs.homeAddress.value
        if (address.isBlank()) {
            overlay.value = Overlay.SettingsPanel
            return
        }
        if (!Apps.navigateTo(context, slotPackage("nav"), address)) {
            Apps.toast(context, "Tiada app navigasi. Pilih satu dulu.")
            overlay.value = Overlay.Pick("nav", "Navigasi", false)
        }
    }

    fun openNotificationAccess() {
        if (!Apps.start(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))) {
            Apps.start(context, Intent(Settings.ACTION_SETTINGS))
        }
    }

    fun close() {
        overlay.value = Overlay.None
    }
}

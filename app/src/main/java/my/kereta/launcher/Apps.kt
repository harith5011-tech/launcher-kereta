package my.kereta.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class AppEntry(val label: String, val pkg: String, val icon: ImageBitmap)

object Apps {
    val NAV_CANDIDATES = listOf(
        "com.waze",
        "com.google.android.apps.maps",
        "com.here.app.maps",
        "net.osmand",
        "net.osmand.plus"
    )

    val MUSIC_CANDIDATES = listOf(
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.apple.android.music",
        "com.tencent.ibg.joox",
        "deezer.android.app",
        "com.google.android.youtube"
    )

    fun load(context: Context, iconPx: Int): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { info ->
                val icon = try {
                    info.loadIcon(pm).toBitmap(iconPx, iconPx).asImageBitmap()
                } catch (e: Exception) {
                    ImageBitmap(iconPx, iconPx)
                }
                AppEntry(info.loadLabel(pm).toString(), info.activityInfo.packageName, icon)
            }
            .sortedBy { it.label.lowercase() }
    }

    fun isInstalled(context: Context, pkg: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(pkg) != null

    fun firstInstalled(context: Context, candidates: List<String>): String? =
        candidates.firstOrNull { isInstalled(context, it) }

    fun label(context: Context, pkg: String): String? = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        null
    }

    fun launch(context: Context, pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return start(context, intent)
    }

    fun start(context: Context, intent: Intent): Boolean = try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    /** Tindakan lalai kalau butang belum diberi app. */
    fun defaultIntent(slot: String): Intent? = when (slot) {
        "phone" -> Intent(Intent.ACTION_DIAL)
        "bt" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        "sound" -> Intent(Settings.ACTION_SOUND_SETTINGS)
        else -> null
    }

    fun navigateTo(context: Context, navPkg: String?, address: String): Boolean {
        val q = Uri.encode(address)
        if (navPkg == "com.waze") {
            val waze = Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?q=$q&navigate=yes")).setPackage(navPkg)
            if (start(context, waze)) return true
        }
        if (navPkg == "com.google.android.apps.maps") {
            val maps = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$q")).setPackage(navPkg)
            if (start(context, maps)) return true
        }
        if (navPkg != null) {
            val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q")).setPackage(navPkg)
            if (start(context, geo)) return true
        }
        return start(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q")))
    }

    fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}

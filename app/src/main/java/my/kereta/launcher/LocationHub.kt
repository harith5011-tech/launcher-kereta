package my.kereta.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat

/** Lokasi GPS untuk peta, kelajuan dan cuaca. */
class LocationHub(private val context: Context) {
    val location = mutableStateOf<Location?>(null)

    private val manager: LocationManager? = context.getSystemService(LocationManager::class.java)
    private var active = false

    private val listener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            val current = location.value
            val gpsIsFresh = current != null &&
                current.provider == LocationManager.GPS_PROVIDER &&
                SystemClock.elapsedRealtimeNanos() - current.elapsedRealtimeNanos < 10_000_000_000L
            if (loc.provider != LocationManager.GPS_PROVIDER && gpsIsFresh) return
            location.value = loc
        }

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}

        @Deprecated("Diperlukan untuk Android 9 dan ke bawah")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun start() {
        val lm = manager ?: return
        if (active || !hasPermission()) return
        try {
            val last = listOfNotNull(
                runCatching { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull(),
                runCatching { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
            ).maxByOrNull { it.time }
            if (last != null && location.value == null) location.value = last

            var requested = false
            if (runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
                requested = true
            }
            if (runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30_000L, 0f, listener, Looper.getMainLooper())
                requested = true
            }
            active = requested
        } catch (e: Exception) {
            active = false
        }
    }

    fun stop() {
        if (!active) return
        try {
            manager?.removeUpdates(listener)
        } catch (e: Exception) {
            // abaikan
        }
        active = false
    }
}

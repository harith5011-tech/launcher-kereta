package my.kereta.launcher

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class BtStatus(val on: Boolean, val label: String)
data class NetStatus(val online: Boolean, val label: String)
data class WeatherNow(val tempC: Int, val code: Int)

object Status {
    @SuppressLint("MissingPermission")
    fun bluetooth(context: Context): BtStatus {
        val adapter = (try {
            context.getSystemService(BluetoothManager::class.java)?.adapter
        } catch (e: Exception) {
            null
        }) ?: return BtStatus(false, "Tiada Bluetooth")
        return try {
            if (!adapter.isEnabled) {
                BtStatus(false, "Bluetooth mati")
            } else {
                val name = connectedName(context, adapter)
                BtStatus(true, if (name != null) "Disambung: $name" else "Bluetooth hidup")
            }
        } catch (e: SecurityException) {
            BtStatus(true, "Bluetooth")
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectedName(context: Context, adapter: BluetoothAdapter): String? {
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) return null
        return try {
            val isConnected = BluetoothDevice::class.java.getMethod("isConnected")
            adapter.bondedDevices?.firstOrNull { (isConnected.invoke(it) as? Boolean) == true }?.name
        } catch (e: Exception) {
            null
        }
    }

    fun network(context: Context): NetStatus {
        val cm = context.getSystemService(ConnectivityManager::class.java)
            ?: return NetStatus(false, "Luar talian")
        val caps = (try {
            cm.getNetworkCapabilities(cm.activeNetwork)
        } catch (e: Exception) {
            null
        }) ?: return NetStatus(false, "Luar talian")
        val label = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Tethering"
            else -> "Dalam talian"
        }
        return NetStatus(true, label)
    }
}

object Weather {
    suspend fun fetch(lat: Double, lon: Double): WeatherNow? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.3f&longitude=%.3f&current=temperature_2m,weather_code",
                    lat,
                    lon
                )
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", "LauncherKereta")
            try {
                if (conn.responseCode != 200) return@withContext null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(body).getJSONObject("current")
                WeatherNow(
                    Math.round(current.getDouble("temperature_2m")).toInt(),
                    current.optInt("weather_code", 0)
                )
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 0 cerah, 1 berawan, 2 hujan, 3 ribut */
    fun kind(code: Int): Int = when (code) {
        0, 1 -> 0
        in 51..67, in 80..82 -> 2
        in 95..99 -> 3
        else -> 1
    }
}

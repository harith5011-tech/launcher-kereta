package my.kereta.launcher

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf

/** Tetapan yang disimpan dalam peranti (tema, warna, alamat rumah, app untuk setiap butang). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("kereta", Context.MODE_PRIVATE)

    /** 0 = Auto, 1 = Malam, 2 = Siang */
    val themeMode = mutableStateOf(sp.getInt("theme_mode", 0))
    val accentIndex = mutableStateOf(sp.getInt("accent", 0))
    val homeAddress = mutableStateOf(sp.getString("home_address", "") ?: "")
    val fullscreen = mutableStateOf(sp.getBoolean("fullscreen", true))

    /** slot -> nama pakej app, contoh "radio" -> "com.vendor.radio" */
    val slots = mutableStateMapOf<String, String>()

    init {
        sp.all.forEach { (key, value) ->
            if (key.startsWith("slot_") && value is String) slots[key.removePrefix("slot_")] = value
        }
    }

    fun setTheme(mode: Int) {
        themeMode.value = mode
        sp.edit().putInt("theme_mode", mode).apply()
    }

    fun setAccent(index: Int) {
        accentIndex.value = index
        sp.edit().putInt("accent", index).apply()
    }

    fun setHome(address: String) {
        homeAddress.value = address
        sp.edit().putString("home_address", address).apply()
    }

    fun setFullscreen(on: Boolean) {
        fullscreen.value = on
        sp.edit().putBoolean("fullscreen", on).apply()
    }

    fun setSlot(slot: String, pkg: String?) {
        if (pkg == null) {
            slots.remove(slot)
            sp.edit().remove("slot_$slot").apply()
        } else {
            slots[slot] = pkg
            sp.edit().putString("slot_$slot", pkg).apply()
        }
    }
}

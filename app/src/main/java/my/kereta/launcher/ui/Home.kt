package my.kereta.launcher.ui

import android.location.Location
import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import my.kereta.launcher.Apps
import my.kereta.launcher.LauncherState
import my.kereta.launcher.NowPlaying
import my.kereta.launcher.Overlay
import my.kereta.launcher.Weather
import java.util.Calendar
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt

private val HARI = listOf("Ahad", "Isnin", "Selasa", "Rabu", "Khamis", "Jumaat", "Sabtu")
private val BULAN = listOf(
    "Januari", "Februari", "Mac", "April", "Mei", "Jun",
    "Julai", "Ogos", "September", "Oktober", "November", "Disember"
)

private data class Tile(val key: String, val label: String, val icon: ImageVector)

private val TILES = listOf(
    Tile("radio", "Radio FM", KIcons.Radio),
    Tile("phone", "Telefon", KIcons.Phone),
    Tile("camera", "Kamera", KIcons.Camera),
    Tile("video", "Video", KIcons.Video),
    Tile("sound", "Bunyi", KIcons.Sliders),
    Tile("bt", "Bluetooth", KIcons.Bluetooth)
)

@Composable
fun LauncherApp(s: LauncherState) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(1000L - value % 1000L)
        }
    }

    // Status Bluetooth dan internet, dikemas kini setiap 5 saat semasa launcher kelihatan.
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                s.refreshStatus()
                delay(5000L)
            }
        }
    }

    // Cuaca ikut lokasi, dikemas kini setiap 30 minit.
    val loc = s.location.location.value
    val weatherKey = loc?.let { String.format(Locale.US, "%.1f,%.1f", it.latitude, it.longitude) }
    LaunchedEffect(weatherKey) {
        val l = loc ?: return@LaunchedEffect
        while (true) {
            Weather.fetch(l.latitude, l.longitude)?.let { s.weather.value = it }
            delay(30L * 60L * 1000L)
        }
    }

    val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    val dark = when (s.prefs.themeMode.value) {
        1 -> true
        2 -> false
        else -> hour >= 19 || hour < 7
    }
    val pal = palette(dark, s.prefs.accentIndex.value)

    BoxWithConstraints(Modifier.fillMaxSize().background(pal.bg)) {
        val scale = min(maxWidth.value / 1280f, maxHeight.value / 720f)
        CompositionLocalProvider(LocalNeu provides pal, LocalScale provides scale) {
            HomeScreen(s, now, hour)
        }
    }
}

@Composable
private fun HomeScreen(s: LauncherState, now: Long, hour: Int) {
    Row(Modifier.fillMaxSize().padding(28.u), horizontalArrangement = Arrangement.spacedBy(24.u)) {
        Dock(s, Modifier.width(88.u).fillMaxHeight())
        Box(Modifier.weight(1f).fillMaxHeight()) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(24.u)) {
                TopBar(s, now, hour, Modifier.fillMaxWidth().height(72.u))
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(24.u)) {
                    NavCard(s, now, Modifier.weight(1.25f).fillMaxHeight())
                    MusicCard(s, now, Modifier.weight(1f).fillMaxHeight())
                }
                TileRow(s, Modifier.fillMaxWidth().height(140.u))
            }
            OverlayHost(s)
        }
    }
}

// ---------- Dock kiri ----------

@Composable
private fun Dock(s: LauncherState, modifier: Modifier) {
    val overlay = s.overlay.value
    NeuSurface(modifier, radius = 44.u, depth = 8.u, contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxHeight().padding(vertical = 20.u),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.u)
        ) {
            DockButton(KIcons.Home, "Utama", active = overlay == Overlay.None) { s.close() }
            DockButton(KIcons.Nav, "Navigasi", onLong = { s.pickFor("nav", "Navigasi") }) {
                s.openSlot("nav", "Navigasi")
            }
            DockButton(KIcons.Music, "Muzik", onLong = { s.pickFor("music", "Muzik") }) { s.openMusic() }
            DockButton(KIcons.Apps, "Aplikasi", active = overlay == Overlay.AllApps) {
                s.overlay.value = Overlay.AllApps
            }
            Spacer(Modifier.weight(1f))
            DockButton(KIcons.Settings, "Tetapan", active = overlay == Overlay.SettingsPanel) {
                s.overlay.value = Overlay.SettingsPanel
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockButton(
    icon: ImageVector,
    label: String,
    active: Boolean = false,
    onLong: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val p = LocalNeu.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val mode = if (active || pressed) NeuMode.Inset else NeuMode.Flat
    Box(
        Modifier
            .size(56.u)
            .neu(18.u, 6.u, mode)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClickLabel = label,
                onLongClick = onLong,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = if (active) p.accent else p.text2, modifier = Modifier.size(26.u))
    }
}

// ---------- Bar atas: jam, tarikh, status ----------

@Composable
private fun TopBar(s: LauncherState, now: Long, hour: Int, modifier: Modifier) {
    val p = LocalNeu.current
    val ctx = LocalContext.current
    val is24 = remember { DateFormat.is24HourFormat(ctx) }
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    val time = if (is24) String.format(Locale.US, "%02d:%02d", h, m)
    else String.format(Locale.US, "%d:%02d", if (h % 12 == 0) 12 else h % 12, m)
    val ampm = if (is24) "" else if (h < 12) "PG" else "PTG"
    val day = HARI[cal.get(Calendar.DAY_OF_WEEK) - 1]
    val date = "${cal.get(Calendar.DAY_OF_MONTH)} ${BULAN[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)}"

    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.u)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.u)) {
                T(time, 56, weight = FontWeight.SemiBold, spacing = -1f)
                if (ampm.isNotEmpty()) T(ampm, 18, Modifier.padding(bottom = 9.u), color = p.text2)
            }
            Box(Modifier.width(4.u).height(40.u).neu(2.u, 2.u, NeuMode.Inset))
            Column {
                T(day, 18, weight = FontWeight.Medium)
                T(date, 15, color = p.text2)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.u)) {
            val bt = s.bt.value
            Chip(KIcons.Bluetooth, bt.label, if (bt.on) p.accent else p.text2) { s.openSlot("bt", "Bluetooth") }

            val w = s.weather.value
            val night = hour >= 19 || hour < 7
            val weatherIcon = when (Weather.kind(w?.code ?: 0)) {
                1 -> KIcons.Cloud
                2 -> KIcons.Rain
                3 -> KIcons.Storm
                else -> if (night) KIcons.Moon else KIcons.Sun
            }
            Chip(weatherIcon, if (w != null) "${w.tempC}°C" else "--°C", p.text, null)

            val net = s.net.value
            Chip(if (net.online) KIcons.Signal else KIcons.Offline, net.label, p.text, null)
        }
    }
}

@Composable
private fun Chip(icon: ImageVector, text: String, iconTint: Color, onClick: (() -> Unit)?) {
    NeuSurface(Modifier.height(48.u).widthIn(max = 300.u), radius = 24.u, depth = 4.u, onClick = onClick, label = text) {
        Row(
            Modifier.padding(horizontal = 20.u),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.u)
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.u))
            T(text, 15, weight = FontWeight.Medium)
        }
    }
}

// ---------- Kad navigasi dengan peta langsung ----------

@Composable
private fun NavCard(s: LauncherState, now: Long, modifier: Modifier) {
    val p = LocalNeu.current
    val ctx = LocalContext.current
    val assigned = s.prefs.slots["nav"]
    val navName = remember(assigned, s.apps.value) { s.slotPackage("nav")?.let { Apps.label(ctx, it) } }
    val loc = s.location.location.value
    val home = s.prefs.homeAddress.value

    NeuSurface(modifier, radius = 32.u, depth = 8.u, contentAlignment = Alignment.TopStart) {
        Column(Modifier.fillMaxSize().padding(24.u), verticalArrangement = Arrangement.spacedBy(16.u)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel("Navigasi")
                Row(
                    Modifier
                        .clip(RoundedCornerShape(12.u))
                        .clickable { s.openSlot("nav", "Navigasi") }
                        .padding(horizontal = 8.u, vertical = 4.u),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.u)
                ) {
                    T(if (navName != null) "Buka $navName" else "Pilih app navigasi", 14, color = p.text2)
                    Icon(KIcons.Chevron, contentDescription = null, tint = p.text2, modifier = Modifier.size(16.u))
                }
            }

            Box(Modifier.fillMaxWidth().weight(1f).neu(22.u, 8.u, NeuMode.Inset, fill = p.well, insetOnTop = true)) {
                LiveMap(
                    loc = loc,
                    dark = p.isDark,
                    accent = p.accent,
                    ring = if (p.isDark) Color(0xFFE7EAEE) else Color.White,
                    onTap = { s.openSlot("nav", "Navigasi") },
                    modifier = Modifier.fillMaxSize()
                )
                SpeedBadge(loc, now, Modifier.align(Alignment.TopStart).padding(14.u))
                if (loc == null) {
                    T("Mencari lokasi GPS…", 15, Modifier.align(Alignment.Center), color = p.text2)
                }
                T(
                    "© OpenStreetMap",
                    11,
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.u)
                        .clip(RoundedCornerShape(6.u))
                        .background(p.bg.copy(alpha = 0.75f))
                        .padding(horizontal = 6.u, vertical = 2.u),
                    color = p.text2
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.u)
            ) {
                Box(Modifier.size(52.u).neu(26.u, 6.u, NeuMode.Inset), contentAlignment = Alignment.Center) {
                    Icon(KIcons.Pin, contentDescription = null, tint = p.accent, modifier = Modifier.size(22.u))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.u)) {
                    T("Rumah", 20, weight = FontWeight.Bold)
                    T(if (home.isBlank()) "Alamat belum ditetapkan" else home, 15, color = p.text2)
                }
                AccentButton(if (home.isBlank()) "Tetapkan" else "Pulang") {
                    if (home.isBlank()) s.overlay.value = Overlay.SettingsPanel else s.goHome()
                }
            }
        }
    }
}

@Composable
private fun SpeedBadge(loc: Location?, now: Long, modifier: Modifier) {
    val p = LocalNeu.current
    val fresh = loc != null && loc.hasSpeed() &&
        SystemClock.elapsedRealtimeNanos() - loc.elapsedRealtimeNanos < 5_000_000_000L
    val kmh = if (fresh && loc != null) (loc.speed * 3.6f).roundToInt().toString() else "--"
    // `now` dibaca supaya lencana dikemas kini setiap saat walaupun GPS senyap.
    val tick = now
    NeuSurface(modifier, radius = 16.u, depth = 4.u) {
        Row(
            Modifier.padding(horizontal = 16.u, vertical = 8.u),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.u)
        ) {
            T(kmh, 26, weight = FontWeight.SemiBold)
            T(if (tick > 0) "km/j" else "", 14, Modifier.padding(bottom = 4.u), color = p.text2)
        }
    }
}

// ---------- Kad muzik ----------

@Composable
private fun MusicCard(s: LauncherState, now: Long, modifier: Modifier) {
    val p = LocalNeu.current
    val np = s.media.nowPlaying.value
    val permitted = s.media.permitted.value

    NeuSurface(modifier, radius = 32.u, depth = 8.u, contentAlignment = Alignment.TopStart) {
        Column(Modifier.fillMaxSize().padding(28.u), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SectionLabel("Sedang dimainkan")
                T(np?.appName ?: "", 14, Modifier.widthIn(max = 180.u), color = p.text2)
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.u)) {
                val art = np?.art
                Box(
                    Modifier
                        .size(128.u)
                        .neu(26.u, 8.u, NeuMode.Inset, insetOnTop = art != null)
                        .clickable { s.openMusic() },
                    contentAlignment = Alignment.Center
                ) {
                    if (art != null) {
                        Image(art, contentDescription = "Kulit album", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(KIcons.Music, contentDescription = null, tint = p.accent, modifier = Modifier.size(48.u))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.u)) {
                    when {
                        np != null && np.title.isNotBlank() -> {
                            T(np.title, 26, weight = FontWeight.SemiBold, maxLines = 2)
                            if (np.artist.isNotBlank()) T(np.artist, 16, color = p.text2)
                            T(if (np.playing) "Sedang main" else "Dijeda", 14, color = p.text2)
                        }
                        !permitted -> {
                            T("Tunjuk lagu di sini", 22, weight = FontWeight.SemiBold, maxLines = 2)
                            T(
                                "Benarkan akses notifikasi supaya launcher boleh baca lagu yang dimainkan.",
                                14,
                                color = p.text2,
                                maxLines = 3
                            )
                            Spacer(Modifier.height(4.u))
                            SmallButton("Benarkan") { s.openNotificationAccess() }
                        }
                        else -> {
                            T("Tiada lagu dimainkan", 22, weight = FontWeight.SemiBold, maxLines = 2)
                            T("Tekan gambar untuk buka app muzik", 14, color = p.text2, maxLines = 2)
                        }
                    }
                }
            }

            Progress(np, now)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.u, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundButton(60, KIcons.Prev, "Lagu sebelum") { s.media.previous() }
                val playing = np?.playing == true
                RoundButton(80, if (playing) KIcons.Pause else KIcons.Play, if (playing) "Jeda" else "Main", accent = true) {
                    s.media.playPause()
                }
                RoundButton(60, KIcons.Next, "Lagu seterusnya") { s.media.next() }
            }
        }
    }
}

@Composable
private fun Progress(np: NowPlaying?, now: Long) {
    val p = LocalNeu.current
    val pos = if (now > 0) np?.positionNow() ?: 0L else 0L
    val dur = np?.duration ?: 0L
    val frac = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Column(verticalArrangement = Arrangement.spacedBy(10.u)) {
        Box(Modifier.fillMaxWidth().height(12.u).neu(6.u, 3.u, NeuMode.Inset).padding(3.u)) {
            if (frac > 0f) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(frac)
                        .clip(RoundedCornerShape(3.u))
                        .background(p.accent)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            T(if (np != null) clock(pos) else "--:--", 13, color = p.text2)
            T(if (dur > 0) clock(dur) else "--:--", 13, color = p.text2)
        }
    }
}

private fun clock(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    val h = total / 3600L
    val m = (total % 3600L) / 60L
    val sec = total % 60L
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
    else String.format(Locale.US, "%d:%02d", m, sec)
}

// ---------- Butang pintas bawah ----------

@Composable
private fun TileRow(s: LauncherState, modifier: Modifier) {
    val p = LocalNeu.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(24.u)) {
        TILES.forEach { t ->
            NeuSurface(
                Modifier.weight(1f).fillMaxHeight(),
                radius = 26.u,
                depth = 8.u,
                onClick = { s.openSlot(t.key, t.label) },
                onLongClick = { s.pickFor(t.key, t.label) },
                label = t.label
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.u)) {
                    Box(Modifier.size(56.u).neu(28.u, 6.u, NeuMode.Inset), contentAlignment = Alignment.Center) {
                        Icon(t.icon, contentDescription = null, tint = p.text, modifier = Modifier.size(26.u))
                    }
                    T(t.label, 16, weight = FontWeight.Medium)
                }
            }
        }
    }
}

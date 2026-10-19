package my.kereta.launcher.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import my.kereta.launcher.AppEntry
import my.kereta.launcher.Apps
import my.kereta.launcher.LauncherState
import my.kereta.launcher.Overlay

private val SLOTS = listOf(
    "nav" to "Navigasi",
    "music" to "Muzik",
    "radio" to "Radio FM",
    "phone" to "Telefon",
    "camera" to "Kamera",
    "video" to "Video",
    "sound" to "Bunyi",
    "bt" to "Bluetooth"
)

@Composable
fun OverlayHost(s: LauncherState) {
    val ctx = LocalContext.current
    when (val o = s.overlay.value) {
        Overlay.None -> Unit
        Overlay.AllApps -> AppGrid(
            s,
            title = "Semua aplikasi",
            onPick = { app ->
                s.close()
                if (!Apps.launch(ctx, app.pkg)) Apps.toast(ctx, "Tak dapat buka ${app.label}")
            },
            onClose = { s.close() }
        )
        Overlay.SettingsPanel -> SettingsPanel(s)
        is Overlay.Pick -> {
            val back = { s.overlay.value = if (o.fromSettings) Overlay.SettingsPanel else Overlay.None }
            AppGrid(
                s,
                title = "Pilih app untuk ${o.title}",
                onPick = { app ->
                    s.prefs.setSlot(o.slot, app.pkg)
                    if (o.fromSettings) {
                        s.overlay.value = Overlay.SettingsPanel
                    } else {
                        s.close()
                        Apps.launch(ctx, app.pkg)
                    }
                },
                onClose = back,
                extra = {
                    SmallButton("Guna lalai") {
                        s.prefs.setSlot(o.slot, null)
                        back()
                    }
                }
            )
        }
    }
}

@Composable
private fun OverlayFrame(content: @Composable ColumnScope.() -> Unit) {
    val p = LocalNeu.current
    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        verticalArrangement = Arrangement.spacedBy(20.u),
        content = content
    )
}

@Composable
private fun AppGrid(
    s: LauncherState,
    title: String,
    onPick: (AppEntry) -> Unit,
    onClose: () -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    val p = LocalNeu.current
    var query by remember { mutableStateOf("") }
    val apps = s.apps.value
    val shown = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    OverlayFrame {
        Row(
            Modifier.fillMaxWidth().height(72.u),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.u)
        ) {
            T(title, 28, Modifier.weight(1f), weight = FontWeight.SemiBold)
            InsetField(query, { query = it }, "Cari app", Modifier.width(320.u).height(56.u), KIcons.Search)
            if (extra != null) extra()
            RoundButton(56, KIcons.Close, "Tutup", onClick = onClose)
        }
        if (apps.isEmpty()) {
            T("Memuatkan aplikasi…", 16, color = p.text2)
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.u),
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(16.u),
            horizontalArrangement = Arrangement.spacedBy(20.u),
            verticalArrangement = Arrangement.spacedBy(20.u)
        ) {
            items(shown, key = { it.pkg }) { app ->
                NeuSurface(
                    Modifier.fillMaxWidth().height(140.u),
                    radius = 24.u,
                    depth = 6.u,
                    onClick = { onPick(app) },
                    label = app.label
                ) {
                    Column(
                        Modifier.padding(horizontal = 10.u),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.u)
                    ) {
                        Image(app.icon, contentDescription = null, modifier = Modifier.size(56.u))
                        T(app.label, 14, weight = FontWeight.Medium, maxLines = 2, align = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsPanel(s: LauncherState) {
    val p = LocalNeu.current
    val ctx = LocalContext.current
    var address by remember { mutableStateOf(s.prefs.homeAddress.value) }
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    OverlayFrame {
        Row(
            Modifier.fillMaxWidth().height(72.u),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.u)
        ) {
            T("Tetapan", 28, Modifier.weight(1f), weight = FontWeight.SemiBold)
            RoundButton(56, KIcons.Close, "Tutup") { s.close() }
        }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(24.u)) {
            SettingsCard(Modifier.weight(1f).fillMaxHeight()) {
                SectionLabel("Tema")
                Segmented(listOf("Auto", "Malam", "Siang"), s.prefs.themeMode.value) { s.prefs.setTheme(it) }
                T("Auto tukar ke gelap dari 7 malam hingga 7 pagi.", 14, color = p.text2, maxLines = 2)

                SectionLabel("Warna aksen")
                Row(horizontalArrangement = Arrangement.spacedBy(20.u)) {
                    val colors = if (p.isDark) DarkAccents else LightAccents
                    colors.forEachIndexed { i, c ->
                        Swatch(c, s.prefs.accentIndex.value == i) { s.prefs.setAccent(i) }
                    }
                }

                SectionLabel("Alamat rumah")
                InsetField(
                    address,
                    { address = it },
                    "Contoh: Jalan Melati, Shah Alam",
                    Modifier.fillMaxWidth().height(56.u),
                    KIcons.Pin
                )
                AccentButton("Simpan alamat") {
                    s.prefs.setHome(address.trim())
                    Apps.toast(ctx, "Alamat disimpan")
                }

                SectionLabel("Paparan")
                ToggleRow("Skrin penuh (sembunyi bar sistem)", s.prefs.fullscreen.value) { s.prefs.setFullscreen(it) }
            }

            SettingsCard(Modifier.weight(1f).fillMaxHeight()) {
                SectionLabel("App untuk butang")
                SLOTS.forEach { (key, label) -> SlotRow(s, key, label) }
                T("Tip: tekan lama butang di skrin utama untuk tukar app.", 14, color = p.text2, maxLines = 2)

                SectionLabel("Kebenaran")
                ActionRow(
                    if (s.media.permitted.value) "Akses notifikasi: dibenarkan" else "Akses notifikasi: belum dibenarkan"
                ) { s.openNotificationAccess() }
                ActionRow("Jadikan launcher utama") {
                    if (!Apps.start(ctx, Intent(Settings.ACTION_HOME_SETTINGS))) Apps.start(ctx, Intent(Settings.ACTION_SETTINGS))
                }
                ActionRow("Tetapan Android") { Apps.start(ctx, Intent(Settings.ACTION_SETTINGS)) }

                T(
                    "Launcher Kereta $version · Peta © penyumbang OpenStreetMap · Cuaca Open-Meteo",
                    13,
                    color = p.text2,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    NeuSurface(modifier, radius = 32.u, depth = 8.u, contentAlignment = Alignment.TopStart) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.u),
            verticalArrangement = Arrangement.spacedBy(18.u),
            content = content
        )
    }
}

@Composable
private fun SlotRow(s: LauncherState, key: String, label: String) {
    val p = LocalNeu.current
    val ctx = LocalContext.current
    val assigned = s.prefs.slots[key]
    val name = remember(assigned, s.apps.value) {
        val pkg = s.slotPackage(key)
        when {
            pkg != null -> Apps.label(ctx, pkg) ?: pkg
            Apps.defaultIntent(key) != null -> "Lalai sistem"
            else -> "Belum dipilih"
        }
    }
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.u)
    ) {
        T(label, 16, Modifier.width(120.u), weight = FontWeight.Medium)
        T(name, 15, Modifier.weight(1f), color = p.text2)
        SmallButton("Tukar") { s.pickFor(key, label, fromSettings = true) }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.u)
    ) {
        T(label, 16, Modifier.weight(1f), weight = FontWeight.Medium, maxLines = 2)
        SmallButton("Buka", onClick)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalNeu.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.u)
    ) {
        T(label, 16, Modifier.weight(1f), weight = FontWeight.Medium, maxLines = 2)
        Box(
            Modifier
                .width(72.u)
                .height(40.u)
                .neu(20.u, 4.u, NeuMode.Inset, fill = if (checked) p.accent else null)
                .clickable(role = Role.Switch, onClickLabel = label) { onChange(!checked) }
                .padding(5.u),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(Modifier.size(30.u).neu(15.u, 2.u, NeuMode.Raised))
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    NeuSurface(Modifier.size(60.u), radius = 30.u, depth = 5.u, inset = selected, onClick = onClick, label = "Warna aksen") {
        Box(Modifier.size(30.u).clip(CircleShape).background(color))
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalNeu.current
    Row(
        Modifier.height(60.u).neu(30.u, 5.u, NeuMode.Inset).padding(6.u),
        horizontalArrangement = Arrangement.spacedBy(6.u)
    ) {
        options.forEachIndexed { i, label ->
            val m = Modifier.fillMaxHeight().width(120.u)
            if (i == selected) {
                NeuSurface(m, radius = 24.u, depth = 3.u, onClick = { onSelect(i) }, label = label) {
                    T(label, 16, color = p.accent, weight = FontWeight.Bold)
                }
            } else {
                Box(
                    m.clip(RoundedCornerShape(24.u)).clickable(role = Role.Button) { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    T(label, 16, color = p.text2, weight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun InsetField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier,
    icon: ImageVector? = null
) {
    val p = LocalNeu.current
    Box(modifier.neu(28.u, 6.u, NeuMode.Inset), contentAlignment = Alignment.CenterStart) {
        Row(
            Modifier.padding(horizontal = 20.u),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.u)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = p.text2, modifier = Modifier.size(20.u))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) T(placeholder, 16, color = p.text2)
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(color = p.text, fontSize = 16.us),
                    cursorBrush = SolidColor(p.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

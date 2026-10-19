package my.kereta.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

// ---------- Warna ----------

data class NeuPalette(
    val bg: Color,
    val dark: Color,
    val light: Color,
    val text: Color,
    val text2: Color,
    val well: Color,
    val accent: Color,
    val onAccent: Color,
    val isDark: Boolean
)

val DarkAccents = listOf(Color(0xFFFF9F43), Color(0xFF3DD6C6), Color(0xFF7AA2FF), Color(0xFFFF7A9A))
val LightAccents = listOf(Color(0xFFB5560A), Color(0xFF0F7C72), Color(0xFF2F5BD3), Color(0xFFB8325A))

fun palette(dark: Boolean, accentIndex: Int): NeuPalette {
    val i = accentIndex.coerceIn(0, 3)
    return if (dark) {
        NeuPalette(
            bg = Color(0xFF23272E),
            dark = Color(0xFF15181C),
            light = Color(0xFF31363F),
            text = Color(0xFFE7EAEE),
            text2 = Color(0xFF9CA4B0),
            well = Color(0xFF1F2329),
            accent = DarkAccents[i],
            onAccent = Color(0xFF23272E),
            isDark = true
        )
    } else {
        NeuPalette(
            bg = Color(0xFFE3E7ED),
            dark = Color(0xFFBCC3CD),
            light = Color(0xFFFFFFFF),
            text = Color(0xFF2A2F38),
            text2 = Color(0xFF5B6370),
            well = Color(0xFFDCE1E8),
            accent = LightAccents[i],
            onAccent = Color(0xFFFFFFFF),
            isDark = false
        )
    }
}

val LocalNeu = staticCompositionLocalOf { palette(true, 0) }

// ---------- Skala: reka bentuk asal 1280×720, diskala ikut saiz skrin sebenar ----------

val LocalScale = staticCompositionLocalOf { 1f }

val Int.u: Dp
    @Composable @ReadOnlyComposable
    get() = (this * LocalScale.current).dp

val Int.us: TextUnit
    @Composable @ReadOnlyComposable
    get() {
        val v = this
        return with(LocalDensity.current) { (v * LocalScale.current).dp.toSp() }
    }

// ---------- Bayang neumorphism ----------

enum class NeuMode { Flat, Raised, Inset }

private fun DrawScope.drawRaised(radius: Float, offset: Float, blur: Float, fill: Int, dark: Int, light: Int) {
    val w = size.width
    val h = size.height
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = fill
        paint.setShadowLayer(blur, offset, offset, dark)
        nc.drawRoundRect(0f, 0f, w, h, radius, radius, paint)
        paint.setShadowLayer(blur, -offset, -offset, light)
        nc.drawRoundRect(0f, 0f, w, h, radius, radius, paint)
    }
}

private fun DrawScope.drawInset(radius: Float, offset: Float, blur: Float, dark: Int, light: Int) {
    val w = size.width
    val h = size.height
    val inner = android.graphics.Path().apply {
        addRoundRect(0f, 0f, w, h, radius, radius, android.graphics.Path.Direction.CW)
    }
    val frame = android.graphics.Path().apply {
        fillType = android.graphics.Path.FillType.EVEN_ODD
        addRect(-blur * 4f, -blur * 4f, w + blur * 4f, h + blur * 4f, android.graphics.Path.Direction.CW)
        addRoundRect(0f, 0f, w, h, radius, radius, android.graphics.Path.Direction.CW)
    }
    drawIntoCanvas { canvas ->
        val nc = canvas.nativeCanvas
        nc.save()
        nc.clipPath(inner)
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = dark
        paint.setShadowLayer(blur, offset, offset, dark)
        nc.drawPath(frame, paint)
        paint.color = light
        paint.setShadowLayer(blur, -offset, -offset, light)
        nc.drawPath(frame, paint)
        nc.restore()
    }
}

/**
 * Permukaan neumorphism. Struktur modifier sentiasa sama untuk semua mod,
 * supaya tekanan butang tidak terputus bila bertukar dari timbul ke tenggelam.
 */
fun Modifier.neu(
    radius: Dp,
    depth: Dp,
    mode: NeuMode,
    fill: Color? = null,
    insetOnTop: Boolean = false
): Modifier = composed {
    val p = LocalNeu.current
    val bg = fill ?: p.bg
    val d = p.dark.toArgb()
    val l = p.light.toArgb()
    val f = bg.toArgb()
    this
        .drawBehind {
            if (mode == NeuMode.Raised) {
                drawRaised(radius.toPx(), depth.toPx(), depth.toPx() * 2f, f, d, l)
            }
        }
        .clip(RoundedCornerShape(radius))
        .background(if (mode == NeuMode.Flat && fill == null) Color.Transparent else bg)
        .drawWithContent {
            val inset = mode == NeuMode.Inset
            if (inset && !insetOnTop) drawInset(radius.toPx(), depth.toPx() * 0.6f, depth.toPx() * 1.4f, d, l)
            drawContent()
            if (inset && insetOnTop) drawInset(radius.toPx(), depth.toPx() * 0.6f, depth.toPx() * 1.4f, d, l)
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NeuSurface(
    modifier: Modifier = Modifier,
    radius: Dp,
    depth: Dp,
    color: Color? = null,
    inset: Boolean = false,
    flat: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    label: String? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val clickable = onClick != null || onLongClick != null
    val mode = when {
        inset || (pressed && clickable) -> NeuMode.Inset
        flat -> NeuMode.Flat
        else -> NeuMode.Raised
    }
    var m = modifier.neu(radius, depth, mode, color)
    if (clickable) {
        m = m.combinedClickable(
            interactionSource = interaction,
            indication = null,
            role = Role.Button,
            onClickLabel = label,
            onLongClick = onLongClick,
            onClick = { onClick?.invoke() }
        )
    }
    Box(m, contentAlignment = contentAlignment, content = content)
}

// ---------- Teks dan butang asas ----------

@Composable
fun T(
    text: String,
    size: Int,
    modifier: Modifier = Modifier,
    color: Color = LocalNeu.current.text,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = 1,
    align: TextAlign? = null,
    spacing: Float = 0f
) {
    val letterSpacing = if (spacing == 0f) TextUnit.Unspecified else {
        val s = LocalScale.current
        with(LocalDensity.current) { (spacing * s).dp.toSp() }
    }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.us,
        fontWeight = weight,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = align,
        letterSpacing = letterSpacing
    )
}

@Composable
fun SectionLabel(text: String) {
    T(text.uppercase(), 14, color = LocalNeu.current.text2, weight = FontWeight.Bold, spacing = 1.5f)
}

@Composable
fun RoundButton(size: Int, icon: ImageVector, label: String, accent: Boolean = false, onClick: () -> Unit) {
    val p = LocalNeu.current
    NeuSurface(
        Modifier.size(size.u),
        radius = (size / 2).u,
        depth = if (size >= 70) 6.u else 5.u,
        color = if (accent) p.accent else null,
        onClick = onClick,
        label = label
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (accent) p.onAccent else p.text,
            modifier = Modifier.size((size * 0.38f).toInt().u)
        )
    }
}

@Composable
fun AccentButton(text: String, onClick: () -> Unit) {
    val p = LocalNeu.current
    NeuSurface(Modifier.height(52.u), radius = 26.u, depth = 4.u, color = p.accent, onClick = onClick, label = text) {
        T(text, 16, Modifier.padding(horizontal = 30.u), color = p.onAccent, weight = FontWeight.Bold)
    }
}

@Composable
fun SmallButton(text: String, onClick: () -> Unit) {
    NeuSurface(Modifier.height(44.u), radius = 22.u, depth = 4.u, onClick = onClick, label = text) {
        T(text, 15, Modifier.padding(horizontal = 22.u), weight = FontWeight.SemiBold)
    }
}

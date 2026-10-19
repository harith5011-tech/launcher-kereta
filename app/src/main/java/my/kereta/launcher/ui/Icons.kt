package my.kereta.launcher.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

private fun rrect(x: Float, y: Float, w: Float, h: Float, r: Float): String {
    val iw = w - 2 * r
    val ih = h - 2 * r
    return "M${x + r},$y h$iw a$r,$r 0 0 1 $r,$r v$ih a$r,$r 0 0 1 ${-r},$r " +
        "h${-iw} a$r,$r 0 0 1 ${-r},${-r} v${-ih} a$r,$r 0 0 1 $r,${-r} Z"
}

private fun vec(name: String, vararg paths: String, stroke: Float = 2f, fill: Boolean = false): ImageVector {
    val b = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    )
    for (d in paths) {
        b.addPath(
            pathData = addPathNodes(d),
            fill = if (fill) SolidColor(Color.Black) else null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = stroke,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    return b.build()
}

/** Ikon garisan ringkas untuk launcher. */
object KIcons {
    val Home = vec("home", "M3 11l9-8 9 8", "M5 10v10h14V10")
    val Nav = vec("nav", "M3 11 L22 2 L13 21 L11 13 Z")
    val Music = vec("music", "M9 18V5l12-2v13", circle(6f, 18f, 3f), circle(18f, 16f, 3f))
    val Apps = vec(
        "apps",
        rrect(3f, 3f, 7f, 7f, 1.5f), rrect(14f, 3f, 7f, 7f, 1.5f),
        rrect(3f, 14f, 7f, 7f, 1.5f), rrect(14f, 14f, 7f, 7f, 1.5f)
    )
    val Settings = vec(
        "settings",
        circle(12f, 12f, 3f),
        "M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1 -2.83 0l-.06 -.06a1.65 1.65 0 0 0 -1.82 -.33 1.65 1.65 0 0 0 -1 1.51V21a2 2 0 0 1 -2 2 2 2 0 0 1 -2 -2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0 -1.82 .33l-.06 .06a2 2 0 0 1 -2.83 0 2 2 0 0 1 0 -2.83l.06 -.06a1.65 1.65 0 0 0 .33 -1.82 1.65 1.65 0 0 0 -1.51 -1H3a2 2 0 0 1 -2 -2 2 2 0 0 1 2 -2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0 -.33 -1.82l-.06 -.06a2 2 0 0 1 0 -2.83 2 2 0 0 1 2.83 0l.06 .06a1.65 1.65 0 0 0 1.82 .33H9a1.65 1.65 0 0 0 1 -1.51V3a2 2 0 0 1 2 -2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82 -.33l.06 -.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06 .06a1.65 1.65 0 0 0 -.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1 -2 2h-.09a1.65 1.65 0 0 0 -1.51 1z"
    )
    val Bluetooth = vec("bluetooth", "M6.5 6.5 L17.5 17.5 L12 23 L12 1 L17.5 6.5 L6.5 17.5")
    val Sun = vec(
        "sun",
        circle(12f, 12f, 4f),
        "M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"
    )
    val Moon = vec("moon", "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z")
    val Cloud = vec("cloud", "M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0 -10z")
    val Rain = vec(
        "rain",
        "M16 13v8", "M8 13v8", "M12 15v8",
        "M20 16.58A5 5 0 0 0 18 7h-1.26A8 8 0 1 0 4 15.25"
    )
    val Storm = vec(
        "storm",
        "M19 16.9A5 5 0 0 0 18 7h-1.26a8 8 0 1 0 -11.62 9",
        "M13 11 L9 17 L15 17 L11 23"
    )
    val Signal = vec("signal", "M4 20v-4M9 20v-8M14 20V8M19 20V4", stroke = 2.4f)
    val Offline = vec("offline", "M4 20v-4M9 20v-8", "M15 9l6 6M21 9l-6 6", stroke = 2.4f)
    val Prev = vec("prev", "M19 20 L9 12 L19 4 Z", "M5 19 L5 5", fill = true)
    val Next = vec("next", "M5 4 L15 12 L5 20 Z", "M19 5 L19 19", fill = true)
    val Play = vec("play", "M8 4 L20 12 L8 20 Z", fill = true)
    val Pause = vec("pause", rrect(6f, 4f, 4f, 16f, 1f), rrect(14f, 4f, 4f, 16f, 1f), fill = true)
    val Radio = vec(
        "radio",
        rrect(2f, 8f, 20f, 13f, 2f), "M7 8l10 -5", circle(8f, 14.5f, 2.5f), "M14 13h4M14 16h4"
    )
    val Phone = vec(
        "phone",
        "M22 16.92v3a2 2 0 0 1 -2.18 2 19.79 19.79 0 0 1 -8.63 -3.07 19.5 19.5 0 0 1 -6 -6 19.79 19.79 0 0 1 -3.07 -8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1 -.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27 -1.27a2 2 0 0 1 2.11 -.45 12.84 12.84 0 0 0 2.81 .7A2 2 0 0 1 22 16.92z"
    )
    val Camera = vec(
        "camera",
        "M23 19a2 2 0 0 1 -2 2H3a2 2 0 0 1 -2 -2V8a2 2 0 0 1 2 -2h4l2 -3h6l2 3h4a2 2 0 0 1 2 2z",
        circle(12f, 13f, 4f)
    )
    val Video = vec("video", rrect(2f, 5f, 15f, 14f, 2f), "M23 7 L17 12 L23 17 Z")
    val Sliders = vec("sliders", "M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6")
    val Pin = vec("pin", "M21 10c0 7 -9 13 -9 13s-9 -6 -9 -13a9 9 0 0 1 18 0z", circle(12f, 10f, 3f))
    val Search = vec("search", circle(11f, 11f, 8f), "M21 21l-4.35 -4.35")
    val Close = vec("close", "M18 6L6 18M6 6l12 12")
    val Chevron = vec("chevron", "M9 18l6 -6 -6 -6")
}

package my.kereta.launcher.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.view.MotionEvent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.abs

/** Peta OpenStreetMap yang mengikut lokasi kereta. Tekan peta untuk buka app navigasi. */
@Composable
fun LiveMap(
    loc: Location?,
    dark: Boolean,
    accent: Color,
    ring: Color,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val tap by rememberUpdatedState(onTap)
    val mapView = remember { createMap(context) { tap() } }
    val marker = remember(mapView) {
        Marker(mapView).also {
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            it.isEnabled = false
            mapView.overlays.add(it)
        }
    }
    var centered by remember { mutableStateOf(false) }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    DisposableEffect(mapView) {
        onDispose { mapView.onDetach() }
    }

    LaunchedEffect(dark) {
        val tiles = mapView.overlayManager.tilesOverlay
        tiles.setColorFilter(if (dark) darkTiles() else null)
        val background = if (dark) 0xFF1F2329.toInt() else 0xFFDCE1E8.toInt()
        tiles.setLoadingBackgroundColor(background)
        tiles.setLoadingLineColor(background)
        mapView.invalidate()
    }

    LaunchedEffect(accent, ring) {
        marker.setIcon(dot(context, accent.toArgb(), ring.toArgb()))
        mapView.invalidate()
    }

    LaunchedEffect(loc) {
        val l = loc ?: return@LaunchedEffect
        val point = GeoPoint(l.latitude, l.longitude)
        marker.setPosition(point)
        marker.isEnabled = true
        if (!centered) {
            mapView.controller.setZoom(16.5)
            mapView.controller.setCenter(point)
            centered = true
        } else {
            mapView.controller.animateTo(point)
        }
        mapView.invalidate()
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun createMap(context: Context, onTap: () -> Unit): MapView =
    MapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(false)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        isTilesScaledToDpi = true
        minZoomLevel = 3.0
        controller.setZoom(6.0)
        controller.setCenter(GeoPoint(4.2, 101.9))
        setOnTouchListener(TapListener(onTap))
    }

/** Peta tidak boleh diseret semasa memandu; sentuhan ringkas buka app navigasi. */
private class TapListener(private val onTap: () -> Unit) : View.OnTouchListener {
    private var downX = 0f
    private var downY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
            }
            MotionEvent.ACTION_UP -> {
                if (abs(event.x - downX) < 40f && abs(event.y - downY) < 40f) onTap()
            }
        }
        return true
    }
}

private fun darkTiles(): ColorMatrixColorFilter {
    val m = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f
        )
    )
    m.postConcat(ColorMatrix().apply { setSaturation(0.3f) })
    m.postConcat(ColorMatrix().apply { setScale(0.78f, 0.82f, 0.9f, 1f) })
    return ColorMatrixColorFilter(m)
}

private fun dot(context: Context, accent: Int, ring: Int): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val size = (30 * d).toInt().coerceAtLeast(24)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    val c = size / 2f
    paint.color = 0x55000000
    canvas.drawCircle(c, c + d, c - d, paint)
    paint.color = ring
    canvas.drawCircle(c, c, c - 2 * d, paint)
    paint.color = accent
    canvas.drawCircle(c, c, c - 6 * d, paint)
    return BitmapDrawable(context.resources, bmp)
}

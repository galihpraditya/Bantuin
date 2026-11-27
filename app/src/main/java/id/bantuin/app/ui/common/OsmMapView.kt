package id.bantuin.app.ui.common

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.ui.theme.BantuinTheme
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMapView(
    centerPoint: GeoPoint,
    markerTitle: String = "Titik Lokasi",
    zoomLevel: Double = 15.0,
    isInteractive: Boolean = true,
    onLocationSelected: ((GeoPoint) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestOnLocationSelected by rememberUpdatedState(onLocationSelected)
    var lastCenter by remember { mutableStateOf<GeoPoint?>(null) }

    val mapView = remember {
        val prefs = context.getSharedPreferences("${context.packageName}_osmdroid", Context.MODE_PRIVATE)
        Configuration.getInstance().load(context, prefs)
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(isInteractive)
            controller.setZoom(zoomLevel)
            controller.setCenter(OsmGeoPoint(centerPoint.lat, centerPoint.lng))
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            update = { view ->
                val osmPoint = OsmGeoPoint(centerPoint.lat, centerPoint.lng)
                if (lastCenter != centerPoint) {
                    view.controller.setZoom(zoomLevel)
                    view.controller.setCenter(osmPoint)
                    lastCenter = centerPoint
                }
                view.setMultiTouchControls(isInteractive)

                view.overlays.clear()
                if (isInteractive && latestOnLocationSelected != null) {
                    val receiver = object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: OsmGeoPoint?): Boolean {
                            if (p != null) {
                                latestOnLocationSelected?.invoke(GeoPoint(p.latitude, p.longitude))
                                return true
                            }
                            return false
                        }

                        override fun longPressHelper(p: OsmGeoPoint?): Boolean = false
                    }
                    view.overlays.add(MapEventsOverlay(receiver))
                }
                val marker = Marker(view).apply {
                    position = osmPoint
                    title = markerTitle
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                view.overlays.add(marker)
                view.invalidate()
            },
            modifier = Modifier.fillMaxSize()
        )

        // Atribusi OSM wajib sesuai PRD Bagian 10.5
        val colors = BantuinTheme.colors
        Text(
            text = "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(colors.surface.copy(alpha = 0.88f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

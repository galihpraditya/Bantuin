package id.bantuin.app.ui.common

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import id.bantuin.app.core.location.LocationHelper
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.ui.theme.BantuinTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun LocationPickerDialog(
    title: String = "Atur Titik Lokasi Layanan",
    currentAddress: String,
    location: GeoPoint,
    isDetectingGps: Boolean,
    gpsError: String?,
    onGpsDetectStart: () -> Unit,
    onGpsError: (String?) -> Unit,
    onDismiss: () -> Unit,
    onLocationConfirmed: (String, GeoPoint, Boolean) -> Unit
) {
    val colors = BantuinTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val presets = remember {
        listOf(
            Triple("Alun-alun Malang", GeoPoint(-7.9797, 112.6304), "Jl. Merdeka Barat, Alun-alun Malang"),
            Triple("UB / Suhat", GeoPoint(-7.9467, 112.6156), "Jl. Soekarno Hatta No. 9, Lowokwaru, Malang"),
            Triple("Sawojajar", GeoPoint(-7.9772, 112.6562), "Jl. Danau Toba, Sawojajar, Malang"),
            Triple("Dinoyo", GeoPoint(-7.9405, 112.6074), "Jl. MT Haryono No. 193, Dinoyo, Malang")
        )
    }

    var textInput by rememberSaveable { mutableStateOf(currentAddress) }
    var selectedLat by rememberSaveable { mutableDoubleStateOf(location.lat) }
    var selectedLng by rememberSaveable { mutableDoubleStateOf(location.lng) }
    val selectedLocation = GeoPoint(selectedLat, selectedLng)
    var fromGps by rememberSaveable { mutableStateOf(false) }

    fun fetchHardwareGps() {
        onGpsDetectStart()
        scope.launch {
            val detected = LocationHelper.getCurrentLocation(context)
            if (detected != null) {
                selectedLat = detected.point.lat
                selectedLng = detected.point.lng
                textInput = detected.address ?: LocationHelper.formatCoordinateFallback(detected.point)
                fromGps = true
                onGpsError(null)
            } else {
                onGpsError("Lokasi GPS tidak tersedia. Ketuk peta atau pilih area cepat di bawah.")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchHardwareGps()
        } else {
            onGpsError("Izin lokasi ditolak. Silakan ketuk peta atau pilih area secara manual.")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedButton(
                    onClick = {
                        if (LocationHelper.hasLocationPermission(context)) {
                            fetchHardwareGps()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    enabled = !isDetectingGps,
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = SolidColor(colors.primary)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    if (isDetectingGps) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mendeteksi GPS...", color = colors.primary)
                    } else {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gunakan Lokasi GPS Saya", color = colors.primary)
                    }
                }

                if (!gpsError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = gpsError,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Atau pilih area cepat / ketuk titik pada peta:",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (name, point, addr) ->
                        val isSelected = abs(selectedLocation.lat - point.lat) < 0.001 &&
                            abs(selectedLocation.lng - point.lng) < 0.001
                        Button(
                            onClick = {
                                selectedLat = point.lat
                                selectedLng = point.lng
                                textInput = addr
                                fromGps = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) colors.primary else colors.surface,
                                contentColor = if (isSelected) colors.onPrimary else colors.textPrimary
                            ),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = SolidColor(if (isSelected) colors.primary else colors.outline)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) colors.onPrimary else colors.textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    OsmMapView(
                        centerPoint = selectedLocation,
                        markerTitle = "Lokasi Layanan",
                        zoomLevel = 14.5,
                        isInteractive = true,
                        onLocationSelected = { tappedPoint ->
                            selectedLat = tappedPoint.lat
                            selectedLng = tappedPoint.lng
                            fromGps = false
                            scope.launch {
                                textInput = LocationHelper.reverseGeocode(context, tappedPoint.lat, tappedPoint.lng)
                                    ?: LocationHelper.formatCoordinateFallback(tappedPoint)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Alamat Lengkap") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onLocationConfirmed(
                        textInput.ifBlank { LocationHelper.formatCoordinateFallback(selectedLocation) },
                        selectedLocation,
                        fromGps
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Simpan Lokasi")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Batal", color = colors.textSecondary)
            }
        }
    )
}

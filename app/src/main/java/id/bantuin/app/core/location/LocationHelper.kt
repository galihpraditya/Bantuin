package id.bantuin.app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import id.bantuin.app.domain.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class DetectedLocation(
    val point: GeoPoint,
    val address: String?
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): DetectedLocation? {
        if (!hasLocationPermission(context)) return null
        val client = LocationServices.getFusedLocationProviderClient(context)

        val location = suspendCancellableCoroutine<Location?> { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }

            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { current ->
                    if (current != null) {
                        if (cont.isActive) cont.resume(current)
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last ->
                                if (cont.isActive) cont.resume(last)
                            }
                            .addOnFailureListener {
                                if (cont.isActive) cont.resume(null)
                            }
                    }
                }
                .addOnFailureListener {
                    client.lastLocation
                        .addOnSuccessListener { last ->
                            if (cont.isActive) cont.resume(last)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
        } ?: return null

        val point = GeoPoint(location.latitude, location.longitude)
        val address = reverseGeocode(context, point.lat, point.lng)
        return DetectedLocation(point = point, address = address)
    }

    suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (!Geocoder.isPresent()) return@withContext null
                val geocoder = Geocoder(context, Locale.forLanguageTag("id-ID"))
                val addressObj = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine<Address?> { cont ->
                        geocoder.getFromLocation(lat, lng, 1) { list ->
                            if (cont.isActive) cont.resume(list.firstOrNull())
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()
                }

                addressObj?.let { addr ->
                    val line = addr.getAddressLine(0)
                    if (!line.isNullOrBlank()) {
                        line
                    } else {
                        listOfNotNull(addr.thoroughfare, addr.subLocality, addr.locality)
                            .filter { it.isNotBlank() }
                            .joinToString(", ")
                            .ifBlank { null }
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    fun formatCoordinateFallback(point: GeoPoint): String {
        return String.format(Locale.US, "Titik GPS (%.4f, %.4f)", point.lat, point.lng)
    }
}

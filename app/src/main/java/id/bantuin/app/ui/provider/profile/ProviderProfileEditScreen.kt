package id.bantuin.app.ui.provider.profile

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.location.LocationHelper
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.LoadingView
import id.bantuin.app.ui.common.OsmMapView
import id.bantuin.app.ui.theme.BantuinTheme
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderProfileEditScreen(
    repository: BantuinRepository,
    onNavigateBack: () -> Unit,
    viewModel: ProviderProfileEditViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = BantuinTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            snackbarHostState.showSnackbar("Profil mitra berhasil diperbarui")
        }
    }

    fun fetchHardwareGps() {
        viewModel.setDetectingGps(true)
        scope.launch {
            val detected = LocationHelper.getCurrentLocation(context)
            if (detected != null) {
                viewModel.onLocationChange(detected.point)
                viewModel.onAddressChange(
                    detected.address ?: LocationHelper.formatCoordinateFallback(detected.point)
                )
                viewModel.setDetectingGps(false)
            } else {
                viewModel.setGpsError("Lokasi GPS tidak tersedia. Ketuk peta atau pilih preset area.")
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
            viewModel.setGpsError("Izin lokasi ditolak. Ketuk peta untuk menentukan titik pangkalan.")
        }
    }

    val presets = remember {
        listOf(
            Triple("Alun-alun Malang", GeoPoint(-7.9797, 112.6304), "Jl. Merdeka Barat, Alun-alun Malang"),
            Triple("UB / Suhat", GeoPoint(-7.9467, 112.6156), "Jl. Soekarno Hatta No. 9, Lowokwaru, Malang"),
            Triple("Sawojajar", GeoPoint(-7.9772, 112.6562), "Jl. Danau Toba, Sawojajar, Malang"),
            Triple("Dinoyo", GeoPoint(-7.9405, 112.6074), "Jl. MT Haryono No. 193, Dinoyo, Malang")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profil & Pangkalan",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = colors.background,
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading && uiState.name.isBlank()) {
            LoadingView(
                message = "Memuat profil mitra...",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Informasi Biodata Mitra",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )

                        OutlinedTextField(
                            value = uiState.name,
                            onValueChange = { viewModel.onNameChange(it) },
                            label = { Text("Nama Lengkap & Gelar") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.bio,
                            onValueChange = { viewModel.onBioChange(it) },
                            label = { Text("Bio / Keahlian Mitra") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.phone,
                            onValueChange = { viewModel.onPhoneChange(it) },
                            label = { Text("Nomor Kontak WhatsApp (contoh: 0812...)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(colors.borderSubtle)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pangkalan & Titik Lokasi Layanan",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Titik ini menjadi dasar penghitungan jarak Haversine ke lokasi pelanggan (F1 & F2).",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )

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
                            enabled = !uiState.isDetectingGps,
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(colors.primary)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            if (uiState.isDetectingGps) {
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
                                Text("Gunakan Titik GPS Saat Ini", color = colors.primary)
                            }
                        }

                        Text(
                            text = "Preset Area Cepat / Ketuk Peta:",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textPrimary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.forEach { (pName, point, pAddr) ->
                                val isSelected = abs(uiState.location.lat - point.lat) < 0.001 &&
                                    abs(uiState.location.lng - point.lng) < 0.001
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        viewModel.onLocationChange(point)
                                        viewModel.onAddressChange(pAddr)
                                    },
                                    label = { Text(pName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = colors.primaryContainer,
                                        selectedLabelColor = colors.onPrimaryContainer
                                    ),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            OsmMapView(
                                centerPoint = uiState.location,
                                markerTitle = "Titik Pangkalan Mitra",
                                zoomLevel = 14.5,
                                isInteractive = true,
                                onLocationSelected = { tappedPoint ->
                                    viewModel.onLocationChange(tappedPoint)
                                    scope.launch {
                                        val addr = LocationHelper.reverseGeocode(
                                            context,
                                            tappedPoint.lat,
                                            tappedPoint.lng
                                        ) ?: LocationHelper.formatCoordinateFallback(tappedPoint)
                                        viewModel.onAddressChange(addr)
                                    }
                                }
                            )
                        }

                        OutlinedTextField(
                            value = uiState.address,
                            onValueChange = { viewModel.onAddressChange(it) },
                            label = { Text("Alamat Tertulis Pangkalan") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.errorContainer)
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.saveProfile() },
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(if (uiState.isSaving) "Menyimpan..." else "Simpan Perubahan Profil")
                }
            }
        }
    }
}

package id.bantuin.app.ui.provider.profile

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.common.OsmMapView
import id.bantuin.app.ui.theme.BorderSubtle
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.NeutralBackground
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.TextMuted
import id.bantuin.app.ui.theme.TextPrimary
import id.bantuin.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderProfileEditScreen(
    repository: FakeBantuinRepository,
    onNavigateBack: () -> Unit,
    viewModel: ProviderProfileEditViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var name by remember { mutableStateOf("Budi Santoso, S.T.") }
    var bio by remember { mutableStateOf("Teknisi spesialis tata udara & kelistrikan rumah tangga berpengalaman 8 tahun di Malang.") }
    var address by remember { mutableStateOf("Jl. Mayjen Panjaitan No. 42, Klojen, Kota Malang") }
    var phone by remember { mutableStateOf("081234567890") }
    var location by remember { mutableStateOf(GeoPoint(-7.9780, 112.6310)) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadProfile("p01")
    }

    LaunchedEffect(uiState.name) {
        if (uiState.name.isNotBlank()) {
            name = uiState.name
            bio = uiState.bio
            address = uiState.address
            phone = uiState.phone
            location = uiState.location
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

    LaunchedEffect(Unit) {
        val res = repository.candidates(2)
        if (res is AppResult.Success) {
            val p = res.data.find { it.providerId == "p01" }
            if (p != null) {
                name = p.name
                bio = p.bio
                address = p.address
                location = p.location
            }
        }
        val user = repository.currentUser().firstOrNull()
        if (user != null && user.phone.isNotBlank()) {
            phone = user.phone
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Profil & Pangkalan",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeutralBackground)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NeutralBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Informasi Biodata Mitra",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                errorMessage = null
                            },
                            label = { Text("Nama Lengkap & Gelar") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Bio / Keahlian Mitra") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Nomor Kontak WhatsApp") },
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
                    colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Pangkalan & Titik Lokasi Layanan",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Titik ini menjadi dasar penghitungan jarak Haversine ke lokasi pelanggan (F1 & F2).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Text(
                            text = "Preset Pangkalan di Malang:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.forEach { (pName, point, pAddr) ->
                                val isSelected = Math.abs(location.lat - point.lat) < 0.001 && Math.abs(location.lng - point.lng) < 0.001
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        location = point
                                        address = pAddr
                                    },
                                    label = { Text(pName) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = OrangePrimary,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.heightIn(min = 48.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            key(location) {
                                OsmMapView(
                                    centerPoint = location,
                                    zoomLevel = 14.0
                                )
                            }
                        }

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Alamat Tertulis Pangkalan") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (errorMessage != null) {
                item {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = RedError
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            errorMessage = "Nama mitra tidak boleh kosong"
                            return@Button
                        }
                        isSaving = true
                        scope.launch {
                            val res = repository.updateProviderProfile(
                                providerId = "p01",
                                name = name.trim(),
                                bio = bio.trim(),
                                location = location,
                                address = address.trim(),
                                phone = phone.trim()
                            )
                            isSaving = false
                            if (res is AppResult.Success) {
                                snackbarHostState.showSnackbar("Profil mitra berhasil diperbarui")
                            } else {
                                errorMessage = "Gagal menyimpan profil mitra"
                            }
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(if (isSaving) "Menyimpan..." else "Simpan Perubahan Profil")
                }
            }
        }
    }
}

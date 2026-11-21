package id.bantuin.app.ui.customer.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.common.BantuinViewModelFactory
import id.bantuin.app.ui.theme.BorderSubtle
import id.bantuin.app.ui.theme.NeutralBackground
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangeContainer
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.RedError
import id.bantuin.app.ui.theme.TextMuted
import id.bantuin.app.ui.theme.TextPrimary
import id.bantuin.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun CustomerProfileScreen(
    repository: FakeBantuinRepository,
    onSwitchRole: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: CustomerProfileViewModel = viewModel(factory = BantuinViewModelFactory(repository)),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeutralBackground)
    ) {
        BantuinTopAppBar(title = "Profil Pengguna")

        Column(modifier = Modifier.padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(OrangeContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user?.fullName?.take(1) ?: "R",
                                style = MaterialTheme.typography.headlineMedium,
                                color = OrangePrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = user?.fullName ?: "Rani Wijaya",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = user?.email ?: "rani@bantuin.test",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Peran Aktif: Pelanggan",
                                style = MaterialTheme.typography.bodySmall,
                                color = OrangePrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tombol Alih ke Mode Mitra
            Button(
                onClick = {
                    viewModel.switchRole("provider", onSwitchRole)
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Handyman, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Beralih ke Mode Mitra Kerja")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tombol Keluar
            OutlinedButton(
                onClick = {
                    viewModel.signOut(onSignOut)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = RedError, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun", color = RedError)
            }
        }
    }
}

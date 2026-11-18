package id.bantuin.app.ui.provider.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.ui.common.BantuinTopAppBar
import id.bantuin.app.ui.theme.BorderSubtle
import id.bantuin.app.ui.theme.GreenSuccess
import id.bantuin.app.ui.theme.NeutralBackground
import id.bantuin.app.ui.theme.NeutralSurface
import id.bantuin.app.ui.theme.OrangePrimary
import id.bantuin.app.ui.theme.TextMuted
import id.bantuin.app.ui.theme.TextPrimary
import id.bantuin.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ProviderCalendarScreen(
    repository: FakeBantuinRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val dayNames = listOf("", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    var days by remember {
        mutableStateOf(FakeData.getAvailability("p01").toMutableList())
    }
    var isSaved by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeutralBackground)
    ) {
        BantuinTopAppBar(title = "Jadwal & Ketersediaan")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Atur ketersediaan operasional Anda per hari:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            items(days, key = { it.dayOfWeek }) { day ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NeutralSurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dayNames.getOrElse(day.dayOfWeek) { "Hari" },
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (day.mode) {
                                            AvailabilityMode.ON -> Color(0xFFE8F5E9)
                                            AvailabilityMode.ALWAYS_ON -> Color(0xFFFFF3E0)
                                            AvailabilityMode.OFF -> Color(0xFFEEEEEE)
                                        }
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = when (day.mode) {
                                        AvailabilityMode.ON -> "Aktif (07.00 - 21.00)"
                                        AvailabilityMode.ALWAYS_ON -> "Siaga 24 Jam"
                                        AvailabilityMode.OFF -> "Libur (OFF)"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = when (day.mode) {
                                        AvailabilityMode.ON -> GreenSuccess
                                        AvailabilityMode.ALWAYS_ON -> OrangePrimary
                                        AvailabilityMode.OFF -> TextMuted
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Opsi Mode: OFF / ON / ALWAYS_ON
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AvailabilityMode.entries.forEach { mode ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        val updated = days.map {
                                            if (it.dayOfWeek == day.dayOfWeek) it.copy(mode = mode) else it
                                        }.toMutableList()
                                        days = updated
                                        isSaved = false
                                    }
                                ) {
                                    RadioButton(
                                        selected = day.mode == mode,
                                        onClick = {
                                            val updated = days.map {
                                                if (it.dayOfWeek == day.dayOfWeek) it.copy(mode = mode) else it
                                            }.toMutableList()
                                            days = updated
                                            isSaved = false
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = OrangePrimary)
                                    )
                                    Text(
                                        text = when (mode) {
                                            AvailabilityMode.OFF -> "Libur"
                                            AvailabilityMode.ON -> "Jam Kerja"
                                            AvailabilityMode.ALWAYS_ON -> "24 Jam"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            repository.saveAvailability("p01", days)
                            isSaved = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSaved) GreenSuccess else OrangePrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(if (isSaved) "✓ Jadwal Tersimpan" else "Simpan Perubahan Jadwal")
                }
            }
        }
    }
}

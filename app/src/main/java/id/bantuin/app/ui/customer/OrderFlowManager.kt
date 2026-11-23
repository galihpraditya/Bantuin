package id.bantuin.app.ui.customer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.Complexity
import id.bantuin.app.domain.model.CostEstimate
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.LockInfo
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ServiceItem

/**
 * Shared state untuk alur pemesanan pelanggan (Screen 1 -> Screen 2 -> Screen 2b -> Screen 3 -> Screen 4 -> Screen 5).
 */
class OrderFlowManager {
    var customerLocation by mutableStateOf(FakeData.MALANG_CENTER)
    var customerAddress by mutableStateOf("Kost Putri Griya Asri, Jl. Kaliurang KM 5, Sleman")
    var selectedCategoryId by mutableStateOf(2) // Default AC & Elektronik
    var selectedProvider by mutableStateOf<ProviderCandidate?>(null)
    var selectedService by mutableStateOf<ServiceItem?>(null)
    var selectedDateIso by mutableStateOf("Selasa, 15 Okt 2024")
    var selectedStartMin by mutableStateOf(870) // 14:30 WIB
    var selectedDurationMin by mutableStateOf(90)
    var selectedComplexity by mutableStateOf(Complexity.SEDANG)
    var isUrgent by mutableStateOf(false)
    var orderNotes by mutableStateOf("AC di kamar lantai 2 meneteskan air terus dan tidak dingin.")
    var currentEstimate by mutableStateOf<CostEstimate?>(null)
    var currentLock by mutableStateOf<LockInfo?>(null)
    var scheduleErrorMessage by mutableStateOf<String?>(null)

    fun toDraft(): OrderDraft? {
        val provider = selectedProvider ?: return null
        val service = selectedService ?: return null
        return OrderDraft(
            categoryId = selectedCategoryId,
            provider = provider,
            service = service,
            dateIso = selectedDateIso,
            startMin = selectedStartMin,
            durationMin = selectedDurationMin,
            complexity = selectedComplexity,
            isUrgent = isUrgent,
            notes = orderNotes,
            customerLocation = customerLocation,
            customerAddress = customerAddress,
            distanceKm = currentEstimate?.distanceKm ?: 1.8,
            costEstimate = currentEstimate,
            lockInfo = currentLock
        )
    }

    fun reset() {
        selectedProvider = null
        selectedService = null
        currentEstimate = null
        currentLock = null
        scheduleErrorMessage = null
    }
}

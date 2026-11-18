package id.bantuin.app.domain.model

import kotlinx.datetime.Instant
import java.math.BigDecimal

data class GeoPoint(val lat: Double, val lng: Double)

enum class Complexity(val displayName: String, val weight: BigDecimal, val description: String) {
    RINGAN("Ringan", BigDecimal("1.00"), "Pemeriksaan standar & pembersihan ringan"),
    SEDANG("Sedang", BigDecimal("1.25"), "Perbaikan komponen & servis berkala"),
    BERAT("Berat", BigDecimal("1.50"), "Bongkar pasang, pengelasan & instalasi ulang")
}

enum class BookingStatus(val displayName: String) {
    PENDING("Menunggu"),
    DIKONFIRMASI("Dikonfirmasi"),
    DALAM_PENGERJAAN("Dalam Pengerjaan"),
    SELESAI("Selesai"),
    DITOLAK("Ditolak"),
    KEDALUWARSA("Kedaluwarsa"),
    DIBATALKAN_PELANGGAN("Dibatalkan Pelanggan"),
    DIBATALKAN_MITRA("Dibatalkan Mitra");

    val isActive get() = this in setOf(PENDING, DIKONFIRMASI, DALAM_PENGERJAAN)
}

enum class AvailabilityMode { OFF, ON, ALWAYS_ON }

data class DayAvailability(
    val dayOfWeek: Int, // 1 = Senin ... 7 = Minggu (ISO)
    val mode: AvailabilityMode,
    val openMin: Int? = null,
    val closeMin: Int? = null
)

enum class ProviderBadge(val label: String) {
    NEW("Mitra Baru"),
    TRUSTED("Mitra Terpercaya"),
    NONE("Reguler")
}

data class ServiceCategory(
    val id: Int,
    val name: String,
    val iconKey: String,
    val defaultDurationMin: Int,
    val description: String = ""
)

data class ServiceItem(
    val id: String,
    val providerId: String,
    val categoryId: Int,
    val name: String,
    val description: String?,
    val baseFare: Int,
    val durationMin: Int,
    val isActive: Boolean = true
)

data class ProviderCandidate(
    val providerId: String,
    val name: String,
    val avatarUrl: String?,
    val location: GeoPoint,
    val address: String = "Malang, Jawa Timur",
    val bio: String = "Teknisi berpengalaman dan bersertifikasi",
    val ratingAvg: Double?,
    val reviewCount: Int = 0,
    val completionRate: Double?,
    val flagAnomaly: Boolean,
    val badge: ProviderBadge,
    val minBaseFare: Int?,
    val responseTimeText: String = "< 15 mnt"
)

data class RankedProvider(
    val candidate: ProviderCandidate,
    val distanceKm: Double,
    val distanceScore: Double,
    val ratingScore: Double,
    val completionScore: Double,
    val finalScore: Double,
    val matchScore: Int
)

data class MatchResult(
    val ranked: List<RankedProvider>,
    val outOfRange: List<ProviderCandidate>
)

enum class SurgeReason(val label: String) {
    NONE("Tarif Normal"),
    PEAK_HOUR("Jam Sibuk (+20%)"),
    NIGHT("Layanan Malam (+20%)"),
    URGENT("Layanan Cepat Tanggap (+20%)"),
    PEAK_AND_URGENT("Jam Sibuk & Darurat (+20%)"),
    NIGHT_AND_URGENT("Malam & Darurat (+20%)")
}

data class CostEstimate(
    val baseFare: Int,
    val distanceKm: Double,
    val distanceCost: Int,
    val complexity: Complexity,
    val baseCost: Int,
    val surgeApplied: Boolean,
    val surgeAmount: Int,
    val surgeReason: SurgeReason,
    val total: Int
)

data class TimeBlock(val startMin: Int, val endMin: Int)

enum class ConflictReason(val message: String) {
    DAY_OFF("Mitra libur pada hari yang dipilih"),
    PAST_TIME("Jam yang dipilih sudah terlewat"),
    OUTSIDE_HOURS("Jam yang dipilih berada di luar jam operasional mitra"),
    OVERLAP("Jadwal beririsan dengan pesanan aktif lain")
}

data class SlotCheckResult(
    val valid: Boolean,
    val reason: ConflictReason?,
    val block: TimeBlock,
    val alternativeStartsMin: List<Int>
)

data class LockInfo(
    val lockId: String,
    val providerId: String,
    val startMin: Int,
    val endMin: Int,
    val expiresAtMillis: Long
)

data class CompletedJob(
    val startAt: Instant,
    val startedAt: Instant
)

data class TrustInput(
    val completed: List<CompletedJob>,
    val mitraCancelledCount: Int,
    val ratings: List<Int>
)

enum class AnomalyReason(val description: String) {
    LOW_TRUST_SCORE("Trust Score di bawah 60"),
    HIGH_CANCEL_RATE("Rasio pembatalan mitra di atas 20%")
}

data class TrustResult(
    val ratingAvg: Double?,
    val ratingScore: Double,
    val onTimeRate: Double,
    val cancelRate: Double,
    val completionRate: Double?,
    val trustScore: Double,
    val txCount: Int,
    val flagAnomaly: Boolean,
    val badge: ProviderBadge,
    val reasons: List<AnomalyReason>
)

data class MetricSnapshot(
    val label: String,
    val trustScore: Double,
    val ratingScore: Double,
    val onTimeRate: Double,
    val cancelRate: Double
)

data class OrderDraft(
    val categoryId: Int,
    val provider: ProviderCandidate,
    val service: ServiceItem,
    val dateIso: String,
    val startMin: Int,
    val durationMin: Int,
    val complexity: Complexity = Complexity.SEDANG,
    val isUrgent: Boolean = false,
    val notes: String = "",
    val customerLocation: GeoPoint,
    val customerAddress: String,
    val distanceKm: Double,
    val costEstimate: CostEstimate? = null,
    val lockInfo: LockInfo? = null
)

data class BookingItem(
    val id: String,
    val customerId: String,
    val customerName: String,
    val providerId: String,
    val providerName: String,
    val serviceName: String,
    val scheduledDate: String,
    val scheduledTime: String,
    val totalCost: Int,
    val status: BookingStatus,
    val address: String,
    val createdAt: Instant,
    val notes: String? = null,
    val review: ReviewItem? = null
)

data class ReviewItem(
    val id: String,
    val bookingId: String,
    val customerName: String,
    val rating: Int,
    val comment: String?,
    val tags: List<String> = emptyList(),
    val createdAtText: String = "Baru saja"
)

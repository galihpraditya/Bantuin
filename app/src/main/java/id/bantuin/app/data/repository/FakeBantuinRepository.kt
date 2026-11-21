package id.bantuin.app.data.repository

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.CompletedJob
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.model.LockInfo
import id.bantuin.app.domain.model.MetricSnapshot
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ReviewItem
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.model.TimeBlock
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.repository.AuthRepository
import id.bantuin.app.domain.repository.BookingRepository
import id.bantuin.app.domain.repository.CatalogRepository
import id.bantuin.app.domain.repository.MetricsRepository
import id.bantuin.app.domain.repository.ReviewRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.domain.usecase.matchmaking.MatchmakingUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class FakeBantuinRepository(
    private val sessionStore: id.bantuin.app.core.session.UserSessionStore = id.bantuin.app.core.session.InMemorySessionStore()
) :
    CatalogRepository,
    BookingRepository,
    ReviewRepository,
    MetricsRepository,
    AuthRepository {

    private val matchmakingUseCase = MatchmakingUseCase()
    private val recalculateMetricsUseCase by lazy {
        id.bantuin.app.domain.usecase.trust.RecalculateProviderMetricsUseCase(this)
    }

    // Data in-memory
    private val categoriesList = FakeData.categories.toMutableList()
    private val providersList = FakeData.providers.toMutableList()
    private val servicesList = FakeData.services.toMutableList()
    private val availabilityMap = mutableMapOf<String, MutableList<DayAvailability>>().apply {
        FakeData.providers.forEach { p ->
            put(p.providerId, FakeData.getAvailability(p.providerId).toMutableList())
        }
    }
    private val bookingsFlow = MutableStateFlow<List<BookingItem>>(emptyList())
    private val reviewsList = FakeData.sampleReviews.toMutableList()
    private val activeLocks = mutableMapOf<String, LockInfo>()

    init {
        // Inisialisasi beberapa pesanan demo
        val initialBooking = BookingItem(
            id = "b01",
            customerId = "user_demo_rani",
            customerName = "Rani Wijaya",
            providerId = "p01",
            providerName = "Budi Santoso, S.T.",
            serviceName = "Servis AC Sedang (1/2 - 1 PK)",
            scheduledDate = "Selasa, 15 Okt 2024",
            scheduledTime = "14:30 WIB",
            totalCost = 94220,
            status = BookingStatus.DALAM_PENGERJAAN,
            address = "Kost Putri Griya Asri, Jl. Kaliurang KM 5, Gang Wuni No. 12, Sleman",
            createdAt = Clock.System.now(),
            notes = "AC di kamar lantai 2 meneteskan air terus dan tidak dingin."
        )
        bookingsFlow.value = listOf(initialBooking)
    }

    // ================= CATALOG REPOSITORY =================
    override suspend fun categories(): AppResult<List<ServiceCategory>> {
        return AppResult.Success(categoriesList.toList())
    }

    override suspend fun candidates(categoryId: Int): AppResult<List<ProviderCandidate>> {
        // Filter mitra yang memiliki layanan di kategori ini dan bukan semua hari OFF
        val activeProviders = providersList.filter { p ->
            val hasService = servicesList.any { it.providerId == p.providerId && it.categoryId == categoryId && it.isActive }
            val days = availabilityMap[p.providerId] ?: emptyList()
            val hasActiveDays = days.any { it.mode != AvailabilityMode.OFF }
            hasService && hasActiveDays
        }
        return AppResult.Success(activeProviders)
    }

    override suspend fun servicesOf(providerId: String): AppResult<List<ServiceItem>> {
        val s = servicesList.filter { it.providerId == providerId && it.isActive }
        return AppResult.Success(s)
    }

    override suspend fun availability(providerId: String): AppResult<List<DayAvailability>> {
        val days = availabilityMap[providerId] ?: FakeData.getAvailability(providerId)
        return AppResult.Success(days)
    }

    override suspend fun saveAvailability(providerId: String, days: List<DayAvailability>): AppResult<Unit> {
        availabilityMap[providerId] = days.toMutableList()
        return AppResult.Success(Unit)
    }

    override suspend fun upsertService(service: ServiceItem): AppResult<Unit> {
        val idx = servicesList.indexOfFirst { it.id == service.id }
        if (idx >= 0) {
            servicesList[idx] = service
        } else {
            servicesList.add(service)
        }
        return AppResult.Success(Unit)
    }

    override suspend fun deleteService(serviceId: String): AppResult<Unit> {
        servicesList.removeAll { it.id == serviceId }
        return AppResult.Success(Unit)
    }

    override suspend fun updateProviderProfile(
        providerId: String,
        name: String,
        bio: String,
        location: id.bantuin.app.domain.model.GeoPoint,
        address: String,
        phone: String
    ): AppResult<Unit> {
        val idx = providersList.indexOfFirst { it.providerId == providerId }
        if (idx >= 0) {
            val p = providersList[idx]
            providersList[idx] = p.copy(
                name = name,
                bio = bio,
                location = location,
                address = address
            )
        }
        val user = sessionStore.currentUserValue
        if (user != null) {
            sessionStore.saveSession(user.copy(fullName = name, phone = phone))
        }
        return AppResult.Success(Unit)
    }

    // ================= BOOKING REPOSITORY =================
    override suspend fun busyBlocks(providerId: String, dateIso: String): AppResult<List<TimeBlock>> {
        // Blok sibuk demo: untuk Pak Budi Santoso (p01), ada pesanan lain 10:00 - 11:30 dan 12:00 - 13:45
        val blocks = if (providerId == "p01") {
            listOf(
                TimeBlock(startMin = 600, endMin = 690), // 10:00 - 11:30 (+ buffer)
                TimeBlock(startMin = 720, endMin = 825)  // 12:00 - 13:45
            )
        } else {
            emptyList()
        }
        return AppResult.Success(blocks)
    }

    override suspend fun acquireLock(providerId: String, startMin: Int, endMin: Int): AppResult<LockInfo> {
        val lockId = UUID.randomUUID().toString()
        val info = LockInfo(
            lockId = lockId,
            providerId = providerId,
            startMin = startMin,
            endMin = endMin,
            expiresAtMillis = System.currentTimeMillis() + 10 * 60 * 1000L
        )
        activeLocks[lockId] = info
        return AppResult.Success(info)
    }

    override suspend fun releaseLock(lockId: String): AppResult<Unit> {
        activeLocks.remove(lockId)
        return AppResult.Success(Unit)
    }

    override suspend fun createBooking(draft: OrderDraft): AppResult<String> {
        val bookingId = "bti_" + UUID.randomUUID().toString().take(6)
        val newBooking = BookingItem(
            id = bookingId,
            customerId = sessionStore.currentUserValue?.id ?: "user_guest",
            customerName = sessionStore.currentUserValue?.fullName ?: "Pelanggan",
            providerId = draft.provider.providerId,
            providerName = draft.provider.name,
            serviceName = draft.service.name,
            scheduledDate = draft.dateIso,
            scheduledTime = TimeUtils.formatMinuteOfDay(draft.startMin),
            totalCost = draft.costEstimate?.total ?: draft.service.baseFare,
            status = BookingStatus.PENDING,
            address = draft.customerAddress,
            createdAt = Clock.System.now(),
            notes = draft.notes.ifBlank { null }
        )

        val updated = bookingsFlow.value.toMutableList()
        updated.add(0, newBooking)
        bookingsFlow.value = updated

        draft.lockInfo?.lockId?.let { activeLocks.remove(it) }
        return AppResult.Success(bookingId)
    }

    override suspend fun updateStatus(bookingId: String, newStatus: BookingStatus): AppResult<Unit> {
        val list = bookingsFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == bookingId }
        if (idx >= 0) {
            val item = list[idx]
            list[idx] = item.copy(status = newStatus)
            bookingsFlow.value = list
            return AppResult.Success(Unit)
        }
        return AppResult.Error("Pesanan tidak ditemukan")
    }

    override suspend fun myBookingsCustomer(): AppResult<List<BookingItem>> {
        val userId = sessionStore.currentUserValue?.id ?: ""
        val list = bookingsFlow.value.filter { it.customerId == userId || it.customerId == "user_demo_rani" }
        return AppResult.Success(list)
    }

    override suspend fun myBookingsProvider(): AppResult<List<BookingItem>> {
        // Menampilkan pesanan yang masuk ke mitra p01 (Pak Budi)
        val list = bookingsFlow.value.filter { it.providerId == "p01" }
        return AppResult.Success(list)
    }

    override fun observeBooking(bookingId: String): Flow<BookingItem?> {
        return bookingsFlow.map { list -> list.find { it.id == bookingId } }
    }

    // ================= REVIEW REPOSITORY =================
    override suspend fun submitReview(
        bookingId: String,
        providerId: String,
        rating: Int,
        comment: String?,
        tags: List<String>
    ): AppResult<Unit> {
        val review = ReviewItem(
            id = UUID.randomUUID().toString(),
            bookingId = bookingId,
            customerName = sessionStore.currentUserValue?.fullName ?: "Pelanggan",
            rating = rating,
            comment = comment,
            tags = tags,
            createdAtText = "Baru saja"
        )
        reviewsList.add(0, review)

        // Update booking dengan ulasan
        val list = bookingsFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == bookingId }
        if (idx >= 0) {
            list[idx] = list[idx].copy(review = review)
            bookingsFlow.value = list
        }

        // Panggil Feedback Loop F4 -> F1
        recalculateMetricsUseCase(providerId)
        return AppResult.Success(Unit)
    }

    override suspend fun reviewsOf(providerId: String): AppResult<List<ReviewItem>> {
        return AppResult.Success(reviewsList.toList())
    }

    // ================= METRICS REPOSITORY =================
    override suspend fun trustInput(providerId: String, sinceDays: Int): AppResult<TrustInput> {
        val now = Clock.System.now()
        val completed = listOf(
            CompletedJob(now - 120.minutes, now - 115.minutes),
            CompletedJob(now - 240.minutes, now - 238.minutes),
            CompletedJob(now - 360.minutes, now - 355.minutes),
            CompletedJob(now - 480.minutes, now - 470.minutes),
            CompletedJob(now - 600.minutes, now - 590.minutes)
        )
        val ratings = reviewsList.map { it.rating }
        return AppResult.Success(
            TrustInput(
                completed = completed,
                mitraCancelledCount = 1,
                ratings = if (ratings.isEmpty()) listOf(5, 5, 4, 5) else ratings
            )
        )
    }

    override suspend fun apply(providerId: String, result: TrustResult): AppResult<Unit> {
        val idx = providersList.indexOfFirst { it.providerId == providerId }
        if (idx >= 0) {
            val p = providersList[idx]
            providersList[idx] = p.copy(
                ratingAvg = result.ratingAvg ?: p.ratingAvg,
                completionRate = result.completionRate ?: p.completionRate,
                flagAnomaly = result.flagAnomaly,
                badge = result.badge
            )
        }
        return AppResult.Success(Unit)
    }

    override suspend fun history(providerId: String): AppResult<List<MetricSnapshot>> {
        return AppResult.Success(FakeData.sampleSnapshots)
    }

    // ================= AUTH REPOSITORY =================
    override suspend fun signIn(email: String, pass: String): AppResult<String> {
        val user = UserProfile(
            id = "user_demo_rani",
            fullName = "Rani Wijaya",
            email = email,
            phone = "081234567890",
            roles = setOf("customer", "provider")
        )
        sessionStore.saveSession(user)
        return AppResult.Success(user.id)
    }

    override suspend fun signUp(
        name: String,
        email: String,
        pass: String,
        phone: String,
        isProvider: Boolean
    ): AppResult<String> {
        val roles = mutableSetOf("customer")
        if (isProvider) roles.add("provider")
        val user = UserProfile(
            id = "user_" + UUID.randomUUID().toString().take(6),
            fullName = name,
            email = email,
            phone = phone,
            roles = roles
        )
        sessionStore.saveSession(user)
        return AppResult.Success(user.id)
    }

    override suspend fun signOut(): AppResult<Unit> {
        sessionStore.clearSession()
        return AppResult.Success(Unit)
    }

    override fun activeRole(): Flow<String> = sessionStore.activeRole

    override suspend fun setActiveRole(role: String) {
        sessionStore.setActiveRole(role)
    }

    override fun currentUser(): Flow<UserProfile?> = sessionStore.currentUser
}

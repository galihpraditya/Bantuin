package id.bantuin.app.data.repository

import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.session.InMemorySessionStore
import id.bantuin.app.core.session.UserSessionStore
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.data.fake.FakeData
import id.bantuin.app.domain.model.AvailabilityMode
import id.bantuin.app.domain.model.BookingEvent
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.ChatMessage
import id.bantuin.app.domain.model.ChatThreadSummary
import id.bantuin.app.domain.model.CompletedJob
import id.bantuin.app.domain.model.CompletionOutcome
import id.bantuin.app.domain.model.reviewOutcomes
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
import kotlinx.coroutines.flow.update
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.LockInfo
import id.bantuin.app.domain.model.MetricSnapshot
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.PaymentMethod
import id.bantuin.app.domain.model.PaymentStatus
import id.bantuin.app.domain.model.PayoutChannelType
import id.bantuin.app.domain.model.PayoutRequest
import id.bantuin.app.domain.model.PayoutStatus
import id.bantuin.app.domain.model.ProviderBadge
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ProviderRevenueAnalytics
import id.bantuin.app.domain.model.ReviewItem
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.model.TimeBlock
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.model.WalletTransaction
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.domain.usecase.trust.RecalculateProviderMetricsUseCase
import id.bantuin.app.domain.usecase.wallet.BuildWalletTransactionsUseCase
import id.bantuin.app.domain.usecase.wallet.CalculateProviderRevenueUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FakeBantuinRepository(
    private val sessionStore: UserSessionStore = InMemorySessionStore()
) : BantuinRepository {

    private val recalculateMetricsUseCase by lazy {
        RecalculateProviderMetricsUseCase(this)
    }
    private val calculateRevenueUseCase = CalculateProviderRevenueUseCase()
    private val buildWalletTransactionsUseCase = BuildWalletTransactionsUseCase()

    private data class StoredReview(val providerId: String, val item: ReviewItem)
    private data class StoredSlotMeta(val dateIso: String, val startMin: Int, val endMin: Int, val startAt: Instant)

    private val categoriesList = FakeData.categories.toMutableList()
    private val providersList = FakeData.providers.toMutableList()
    private val servicesList = FakeData.services.toMutableList()
    private val availabilityMap = mutableMapOf<String, MutableList<DayAvailability>>().apply {
        FakeData.providers.forEach { p ->
            put(p.providerId, FakeData.getAvailability(p.providerId).toMutableList())
        }
    }
    private val acceptingOrdersFlow = MutableStateFlow<Map<String, Boolean>>(
        FakeData.providers.associate { it.providerId to true }
    )
    private val bookingsFlow = MutableStateFlow<List<BookingItem>>(emptyList())
    private val bookingMetaMap = mutableMapOf<String, StoredSlotMeta>()
    private val bookingEventsMap = mutableMapOf<String, MutableList<BookingEvent>>()
    private val chatMessagesFlow = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val payoutsFlow = MutableStateFlow<Map<String, List<PayoutRequest>>>(emptyMap())
    private val reviewsList = (FakeData.sampleReviews.map { StoredReview("p01", it) } +
        listOf(
            ReviewItem("sugeng-review-1", "sugeng-demo-1", "Dewi", 5, "Datang tepat waktu dan MCB kembali normal."),
            ReviewItem("sugeng-review-2", "sugeng-demo-2", "Arif", 5, "Instalasi stop kontak rapi dan sudah dites."),
            ReviewItem("sugeng-review-3", "sugeng-demo-3", "Nina", 4, "Biayanya dijelaskan sebelum pengerjaan.")
        ).map { StoredReview("p04", it) }).map {
            it.copy(item = it.item.copy(createdAt = Clock.System.now() - 2.days))
        }.toMutableList()
    private val trustScore = TrustScoreUseCase()
    private val trustRevision = MutableStateFlow(0L)
    override val trustChanges: Flow<Long> = trustRevision
    private val activeLocks = mutableMapOf<String, Pair<String, LockInfo>>() // lockId -> (lockedByUserId, LockInfo)
    private val providerSnapshotsMap = mutableMapOf<String, MutableList<MetricSnapshot>>()
    private var nextEventId = 100L

    init {
        val now = Clock.System.now()
        val initialStart = now - 60.minutes
        val initialBooking = BookingItem(
            id = "b01",
            customerId = "user_demo_rani",
            customerName = "Rani Wijaya",
            providerId = "p01",
            providerName = "Budi Santoso, S.T.",
            serviceName = "Cuci AC Standar (0.5 - 1 PK)",
            scheduledDate = TimeUtils.todayIso(),
            scheduledTime = "14:30 WIB",
            totalCost = 94220,
            status = BookingStatus.DALAM_PENGERJAAN,
            address = "Jl. Merdeka Barat, Alun-alun Malang",
            createdAt = initialStart,
            notes = "AC di kamar lantai 2 meneteskan air terus dan tidak dingin.",
            startedAt = initialStart + 5.minutes,
            customerLat = FakeData.MALANG_CENTER.lat,
            customerLng = FakeData.MALANG_CENTER.lng,
            providerLocation = FakeData.providers.first().location,
            providerPhone = "081200000001",
            customerPhone = "081234567890",
            paymentMethod = PaymentMethod.QRIS,
            paymentStatus = PaymentStatus.PAID
        )
        val completedQrisDemo = BookingItem(
            id = "b00_qris",
            customerId = "user_demo_dimas",
            customerName = "Dimas Arya",
            providerId = "p01",
            providerName = "Budi Santoso, S.T.",
            serviceName = "Cuci & Tambah Freon R32/R410",
            scheduledDate = TimeUtils.formatInstantToDate(now - 2.days),
            scheduledDateIso = TimeUtils.toWibDateIso(now - 2.days),
            scheduledTime = "10:00 - 12:00 WIB",
            totalCost = 120_000,
            status = BookingStatus.SELESAI,
            address = "Jl. Gajayana No. 18, Malang",
            createdAt = now - 2.days,
            startedAt = now - 2.days + 5.minutes,
            finishedAt = now - 2.days + 100.minutes,
            customerLat = FakeData.MALANG_CENTER.lat,
            customerLng = FakeData.MALANG_CENTER.lng,
            providerLocation = FakeData.providers.first().location,
            providerPhone = "081200000001",
            customerPhone = "081234567891",
            paymentMethod = PaymentMethod.QRIS,
            paymentStatus = PaymentStatus.PAID,
            completionNote = "Tekanan freon normal dan filter sudah dibersihkan."
        )
        val completedCashDemo = BookingItem(
            id = "b00_cash",
            customerId = "user_demo_dimas",
            customerName = "Dimas Arya",
            providerId = "p01",
            providerName = "Budi Santoso, S.T.",
            serviceName = "Cuci AC Standar (0.5 - 1 PK)",
            scheduledDate = TimeUtils.formatInstantToDate(now - 1.days),
            scheduledDateIso = TimeUtils.toWibDateIso(now - 1.days),
            scheduledTime = "13:00 - 14:30 WIB",
            totalCost = 65_000,
            status = BookingStatus.SELESAI,
            address = "Jl. Soekarno Hatta No. 11, Malang",
            createdAt = now - 1.days,
            startedAt = now - 1.days + 4.minutes,
            finishedAt = now - 1.days + 80.minutes,
            customerLat = FakeData.MALANG_CENTER.lat,
            customerLng = FakeData.MALANG_CENTER.lng,
            providerLocation = FakeData.providers.first().location,
            providerPhone = "081200000001",
            customerPhone = "081234567891",
            paymentMethod = PaymentMethod.CASH,
            paymentStatus = PaymentStatus.PAID,
            completionNote = "Pembayaran tunai diterima di lokasi."
        )
        bookingsFlow.value = listOf(initialBooking, completedCashDemo, completedQrisDemo)
        bookingMetaMap["b01"] = StoredSlotMeta(TimeUtils.todayIso(), 870, 990, initialStart)
        bookingEventsMap["b01"] = mutableListOf(
            BookingEvent(1L, "b01", BookingStatus.PENDING, "user_demo_rani", initialStart),
            BookingEvent(2L, "b01", BookingStatus.DIKONFIRMASI, "p01", initialStart + 2.minutes),
            BookingEvent(3L, "b01", BookingStatus.DALAM_PENGERJAAN, "p01", initialStart + 5.minutes)
        )
        chatMessagesFlow.value = mapOf(
            "b01" to listOf(
                ChatMessage(
                    id = "msg_seed_1",
                    bookingId = "b01",
                    senderId = "user_demo_rani",
                    senderName = "Rani Wijaya",
                    senderRole = "customer",
                    text = "Pak Budi, AC di kamar lantai 2 masih menetes terus ya.",
                    createdAt = initialStart + 3.minutes
                ),
                ChatMessage(
                    id = "msg_seed_2",
                    bookingId = "b01",
                    senderId = "p01",
                    senderName = "Budi Santoso, S.T.",
                    senderRole = "provider",
                    text = "Siap Kak Rani, saya sudah membawa selang kuras dan alat cek freon.",
                    createdAt = initialStart + 4.minutes
                )
            )
        )
        providerSnapshotsMap["p01"] = FakeData.sampleSnapshots.filterNot { it.label == "Sekarang" }.toMutableList()
        providersList.map { it.providerId }.forEach { providerId ->
            cacheTrust(providerId, trustScore(buildTrustInput(providerId, 30)), addSnapshot = providerId == "p01")
        }
    }

    // ================= CATALOG REPOSITORY =================
    override suspend fun categories(): AppResult<List<ServiceCategory>> {
        return AppResult.Success(categoriesList.toList())
    }

    override suspend fun candidates(categoryId: Int): AppResult<List<ProviderCandidate>> {
        val selfId = sessionStore.currentUserValue?.id
        val activeProviders = providersList.filter { p ->
            val notSelf = selfId == null || p.providerId != selfId
            val isAccepting = acceptingOrdersFlow.value[p.providerId] ?: true
            val hasService = servicesList.any { it.providerId == p.providerId && it.categoryId == categoryId && it.isActive }
            val days = availabilityMap[p.providerId] ?: emptyList()
            val hasActiveDays = days.any { it.mode != AvailabilityMode.OFF }
            notSelf && isAccepting && hasService && hasActiveDays
        }
        return AppResult.Success(activeProviders)
    }

    override suspend fun providerProfile(providerId: String): AppResult<ProviderCandidate?> {
        return AppResult.Success(providersList.find { it.providerId == providerId })
    }

    override suspend fun servicesOf(providerId: String): AppResult<List<ServiceItem>> {
        val isOwner = sessionStore.currentUserValue?.id == providerId
        val s = servicesList.filter { it.providerId == providerId && (isOwner || it.isActive) }
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
        location: GeoPoint,
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
        } else {
            providersList.add(
                ProviderCandidate(
                    providerId = providerId,
                    name = name,
                    avatarUrl = null,
                    location = location,
                    address = address,
                    bio = bio,
                    ratingAvg = null,
                    reviewCount = 0,
                    completionRate = null,
                    flagAnomaly = true,
                    badge = ProviderBadge.NONE,
                    minBaseFare = null
                )
            )
        }
        val user = sessionStore.currentUserValue
        if (user != null && user.id == providerId) {
            sessionStore.saveSession(user.copy(fullName = name, phone = phone))
        }
        return AppResult.Success(Unit)
    }

    override fun observeAcceptingOrders(providerId: String): Flow<Boolean> {
        return acceptingOrdersFlow.map { it[providerId] ?: true }
    }

    override suspend fun setAcceptingOrders(providerId: String, accepting: Boolean): AppResult<Unit> {
        acceptingOrdersFlow.value = acceptingOrdersFlow.value + (providerId to accepting)
        return AppResult.Success(Unit)
    }

    // ================= BOOKING REPOSITORY =================
    override suspend fun busyBlocks(providerId: String, dateIso: String): AppResult<List<TimeBlock>> {
        expirePending()
        val nowMs = System.currentTimeMillis()
        val selfId = sessionStore.currentUserValue?.id

        val demoBlocks = if (providerId == "p01") {
            listOf(
                TimeBlock(startMin = 600, endMin = 690), // 10:00 - 11:30 (+ buffer)
                TimeBlock(startMin = 720, endMin = 825)  // 12:00 - 13:45
            )
        } else {
            emptyList()
        }

        val dynamicBookingBlocks = bookingsFlow.value
            .filter { it.providerId == providerId && it.status.isActive && it.scheduledDate == dateIso }
            .mapNotNull { booking ->
                bookingMetaMap[booking.id]?.let { TimeBlock(it.startMin, it.endMin) }
            }

        val lockBlocks = activeLocks.values
            .filter { (lockedBy, lock) ->
                lock.providerId == providerId &&
                    lock.dateIso == dateIso &&
                    lock.expiresAtMillis > nowMs &&
                    (selfId == null || lockedBy != selfId)
            }
            .map { (_, lock) -> TimeBlock(lock.startMin, lock.endMin) }

        return AppResult.Success((demoBlocks + dynamicBookingBlocks + lockBlocks).distinct())
    }

    override suspend fun acquireLock(
        providerId: String,
        dateIso: String,
        startMin: Int,
        endMin: Int
    ): AppResult<LockInfo> {
        val currentUserId = sessionStore.currentUserValue?.id ?: "user_guest"
        if (currentUserId == providerId) {
            return AppResult.Error("Tidak bisa memesan layanan sendiri")
        }

        val nowMs = System.currentTimeMillis()
        activeLocks.entries.removeIf { it.value.second.expiresAtMillis <= nowMs || it.value.first == currentUserId }

        val busy = (busyBlocks(providerId, dateIso) as? AppResult.Success)?.data.orEmpty()
        if (busy.any { it.startMin < endMin && startMin < it.endMin }) {
            return AppResult.Error("Slot baru saja diambil, pilih jam lain")
        }

        val lockId = UUID.randomUUID().toString()
        val info = LockInfo(
            lockId = lockId,
            providerId = providerId,
            startMin = startMin,
            endMin = endMin,
            expiresAtMillis = nowMs + BantuinConfig.SLOT_LOCK_MINUTES * 60_000L,
            dateIso = dateIso
        )
        activeLocks[lockId] = currentUserId to info
        return AppResult.Success(info)
    }

    override suspend fun releaseLock(lockId: String): AppResult<Unit> {
        activeLocks.remove(lockId)
        return AppResult.Success(Unit)
    }

    override suspend fun createBooking(draft: OrderDraft): AppResult<String> {
        val user = sessionStore.currentUserValue
        val customerId = user?.id ?: "user_guest"
        if (customerId == draft.provider.providerId) {
            return AppResult.Error("Tidak bisa memesan layanan sendiri")
        }
        val lockId = draft.lockInfo?.lockId
        if (lockId != null) {
            val locked = activeLocks[lockId]
            if (locked != null && locked.second.expiresAtMillis <= System.currentTimeMillis()) {
                activeLocks.remove(lockId)
                return AppResult.Error("Waktu penahanan slot habis, pilih jadwal lagi")
            }
        }

        val now = Clock.System.now()
        val bookingId = "bti_" + UUID.randomUUID().toString().take(8)
        val endWithBuffer = draft.lockInfo?.endMin ?: (draft.startMin + draft.durationMin + BantuinConfig.TRAVEL_BUFFER_MIN)
        val newBooking = BookingItem(
            id = bookingId,
            customerId = customerId,
            customerName = user?.fullName ?: "Pelanggan",
            providerId = draft.provider.providerId,
            providerName = draft.provider.name,
            serviceName = draft.service.name,
            scheduledDate = draft.dateIso,
            scheduledTime = "${TimeUtils.formatMinuteOfDay(draft.startMin, false)} - ${TimeUtils.formatMinuteOfDay(draft.startMin + draft.durationMin)}",
            totalCost = draft.costEstimate?.total ?: draft.service.baseFare,
            status = BookingStatus.PENDING,
            address = draft.customerAddress,
            createdAt = now,
            notes = draft.notes.ifBlank { null },
            expiresAt = now + BantuinConfig.PENDING_EXPIRY_MINUTES.minutes,
            customerLat = draft.customerLocation.lat,
            customerLng = draft.customerLocation.lng,
            providerLocation = draft.provider.location,
            customerPhone = user?.phone,
            paymentMethod = draft.paymentMethod,
            paymentStatus = if (draft.paymentMethod == PaymentMethod.QRIS) {
                PaymentStatus.WAITING_PAYMENT
            } else {
                PaymentStatus.UNPAID
            }
        )

        val startAtInstant = try {
            Instant.parse(TimeUtils.toIsoString(draft.dateIso, draft.startMin))
        } catch (_: Exception) {
            now
        }
        bookingMetaMap[bookingId] = StoredSlotMeta(draft.dateIso, draft.startMin, endWithBuffer, startAtInstant)
        bookingEventsMap[bookingId] = mutableListOf(
            BookingEvent(nextEventId++, bookingId, BookingStatus.PENDING, customerId, now)
        )

        val updated = bookingsFlow.value.toMutableList()
        updated.add(0, newBooking)
        bookingsFlow.value = updated

        lockId?.let { activeLocks.remove(it) }
        return AppResult.Success(bookingId)
    }

    override suspend fun updateStatus(bookingId: String, newStatus: BookingStatus): AppResult<Unit> {
        val list = bookingsFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == bookingId }
        if (idx < 0) return AppResult.Error("Pesanan tidak ditemukan")

        val item = list[idx]
        val now = Clock.System.now()
        if (item.status == BookingStatus.PENDING &&
            newStatus in setOf(BookingStatus.DIKONFIRMASI, BookingStatus.DITOLAK) &&
            item.expiresAt != null &&
            item.expiresAt <= now
        ) {
            list[idx] = item.copy(status = BookingStatus.KEDALUWARSA)
            bookingsFlow.value = list
            bookingEventsMap.getOrPut(bookingId) { mutableListOf() }
                .add(BookingEvent(nextEventId++, bookingId, BookingStatus.KEDALUWARSA, null, now))
            return AppResult.Error("Pesanan sudah kedaluwarsa")
        }

        val updatedItem = when (newStatus) {
            BookingStatus.DALAM_PENGERJAAN -> item.copy(status = newStatus, startedAt = now)
            BookingStatus.SELESAI, BookingStatus.DIBATALKAN_MITRA, BookingStatus.DIBATALKAN_PELANGGAN ->
                item.copy(status = newStatus, finishedAt = now)
            else -> item.copy(status = newStatus)
        }
        list[idx] = updatedItem
        bookingsFlow.value = list
        bookingEventsMap.getOrPut(bookingId) { mutableListOf() }
            .add(BookingEvent(nextEventId++, bookingId, newStatus, sessionStore.currentUserValue?.id, now))

        if (newStatus in setOf(BookingStatus.SELESAI, BookingStatus.DIBATALKAN_MITRA, BookingStatus.DIBATALKAN_PELANGGAN)) {
            recalculateMetricsUseCase(item.providerId)
        }
        return AppResult.Success(Unit)
    }

    override suspend fun confirmQrisPayment(bookingId: String): AppResult<Unit> {
        val list = bookingsFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == bookingId }
        if (idx < 0) return AppResult.Error("Pesanan tidak ditemukan")

        val item = list[idx]
        if (item.paymentMethod != PaymentMethod.QRIS) {
            return AppResult.Error("Pesanan ini menggunakan metode tunai.")
        }
        if (item.status in setOf(
                BookingStatus.DITOLAK,
                BookingStatus.KEDALUWARSA,
                BookingStatus.DIBATALKAN_PELANGGAN,
                BookingStatus.DIBATALKAN_MITRA
            )
        ) {
            return AppResult.Error("Pesanan sudah tidak aktif.")
        }

        list[idx] = item.copy(paymentStatus = PaymentStatus.PAID)
        bookingsFlow.value = list
        return AppResult.Success(Unit)
    }

    override suspend fun completeBooking(
        bookingId: String,
        completionNote: String?,
        cashCollectedConfirmed: Boolean
    ): AppResult<Unit> {
        val list = bookingsFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == bookingId }
        if (idx < 0) return AppResult.Error("Pesanan tidak ditemukan")

        val item = list[idx]
        if (item.status != BookingStatus.DALAM_PENGERJAAN) {
            return AppResult.Error("Pesanan hanya dapat diselesaikan saat berstatus Dalam Pengerjaan.")
        }

        val now = Clock.System.now()
        val cleanedNote = completionNote?.trim()?.takeIf { it.isNotBlank() }

        val updatedItem = when (item.paymentMethod) {
            PaymentMethod.CASH -> {
                if (!cashCollectedConfirmed) {
                    return AppResult.Error("Konfirmasikan bahwa uang tunai sudah diterima dari pelanggan.")
                }
                item.copy(
                    status = BookingStatus.SELESAI,
                    paymentStatus = PaymentStatus.PAID,
                    completionNote = cleanedNote,
                    finishedAt = now
                )
            }
            PaymentMethod.QRIS -> {
                if (item.paymentStatus != PaymentStatus.PAID) {
                    return AppResult.Error("Pembayaran QRIS pelanggan belum lunas.")
                }
                item.copy(
                    status = BookingStatus.SELESAI,
                    completionNote = cleanedNote,
                    finishedAt = now
                )
            }
        }

        list[idx] = updatedItem
        bookingsFlow.value = list
        bookingEventsMap.getOrPut(bookingId) { mutableListOf() }
            .add(BookingEvent(nextEventId++, bookingId, BookingStatus.SELESAI, sessionStore.currentUserValue?.id, now))
        recalculateMetricsUseCase(item.providerId)
        return AppResult.Success(Unit)
    }

    override suspend fun myBookingsCustomer(): AppResult<List<BookingItem>> {
        expirePending()
        val userId = sessionStore.currentUserValue?.id ?: ""
        val list = bookingsFlow.value.filter { it.customerId == userId }
        return AppResult.Success(list)
    }

    override suspend fun myBookingsProvider(): AppResult<List<BookingItem>> {
        expirePending()
        val userId = sessionStore.currentUserValue?.id ?: ""
        val list = bookingsFlow.value.filter { it.providerId == userId }
        return AppResult.Success(list)
    }

    override fun observeBooking(bookingId: String): Flow<BookingItem?> {
        return bookingsFlow.map { list -> list.find { it.id == bookingId } }
    }

    override fun observeCustomerBookings(): Flow<List<BookingItem>> {
        return combine(bookingsFlow, sessionStore.currentUser) { list, user ->
            val userId = user?.id ?: ""
            list.filter { it.customerId == userId }
        }
    }

    override fun observeProviderBookings(): Flow<List<BookingItem>> {
        return combine(bookingsFlow, sessionStore.currentUser) { list, user ->
            val userId = user?.id ?: ""
            list.filter { it.providerId == userId }
        }
    }

    override suspend fun events(bookingId: String): AppResult<List<BookingEvent>> {
        return AppResult.Success(bookingEventsMap[bookingId]?.toList().orEmpty())
    }

    override suspend fun expirePending(): AppResult<Unit> {
        val now = Clock.System.now()
        var changed = false
        val updated = bookingsFlow.value.map { item ->
            if (item.status == BookingStatus.PENDING && item.expiresAt != null && item.expiresAt <= now) {
                changed = true
                bookingEventsMap.getOrPut(item.id) { mutableListOf() }
                    .add(BookingEvent(nextEventId++, item.id, BookingStatus.KEDALUWARSA, null, now))
                item.copy(status = BookingStatus.KEDALUWARSA)
            } else {
                item
            }
        }
        if (changed) {
            bookingsFlow.value = updated
        }
        return AppResult.Success(Unit)
    }

    // ================= REVIEW REPOSITORY =================
    override suspend fun submitReview(
        bookingId: String, providerId: String, rating: Int, comment: String?,
        tags: List<String>, completion: CompletionOutcome?
    ): AppResult<Unit> {
        val booking = bookingsFlow.value.find {
            it.id == bookingId && it.providerId == providerId && it.customerId == sessionStore.currentUserValue?.id
        } ?: return AppResult.Error("Pesanan bukan milik Anda")
        if (rating !in 1..5 || (comment?.length ?: 0) > 300 || tags.size > 4 || tags.any { it.length > 40 }) {
            return AppResult.Error("Rating atau ulasan tidak valid")
        }
        if (completion != null && completion !in booking.status.reviewOutcomes() ||
            completion == null && booking.status !in reviewableStatuses) {
            return AppResult.Error("Hasil penyelesaian tidak sesuai dengan status pesanan")
        }

        val now = Clock.System.now()
        if (booking.status == BookingStatus.PENDING && booking.expiresAt?.let { it <= now } == true) {
            return AppResult.Error("Pesanan sudah kedaluwarsa")
        }
        val cancelsBooking = completion == CompletionOutcome.DIBATALKAN && booking.status.isActive
        val previousReview = booking.review
        val review = ReviewItem(
            id = previousReview?.id ?: UUID.randomUUID().toString(),
            bookingId = bookingId, customerName = booking.customerName,
            rating = rating, comment = comment?.trim()?.takeIf { it.isNotEmpty() },
            tags = tags.toList(), createdAtText = previousReview?.createdAtText ?: "Baru saja",
            createdAt = previousReview?.createdAt ?: now
        )
        val updated = booking.copy(
            status = if (cancelsBooking) BookingStatus.DIBATALKAN_PELANGGAN else booking.status,
            finishedAt = if (cancelsBooking) now else booking.finishedAt,
            completionOutcome = completion ?: booking.completionOutcome,
            review = review
        )
        reviewsList.removeAll { it.item.bookingId == bookingId }
        reviewsList.add(0, StoredReview(providerId, review))
        if (cancelsBooking) {
            bookingEventsMap.getOrPut(bookingId) { mutableListOf() }
                .add(BookingEvent(nextEventId++, bookingId, updated.status, booking.customerId, now))
        }
        bookingsFlow.update { list -> list.map { if (it.id == bookingId) updated else it } }
        return apply(providerId, trustScore(buildTrustInput(providerId, 30)))
    }

    override suspend fun deleteReview(bookingId: String): AppResult<Unit> {
        val booking = bookingsFlow.value.find { it.id == bookingId && it.customerId == sessionStore.currentUserValue?.id }
            ?: return AppResult.Error("Pesanan bukan milik Anda")
        if (booking.review == null) return AppResult.Success(Unit)
        reviewsList.removeAll { it.item.bookingId == bookingId }
        bookingsFlow.update { list -> list.map { if (it.id == bookingId) it.copy(review = null) else it } }
        return apply(booking.providerId, trustScore(buildTrustInput(booking.providerId, 30)))
    }

    private val reviewableStatuses = setOf(
        BookingStatus.SELESAI, BookingStatus.DIBATALKAN_MITRA, BookingStatus.DIBATALKAN_PELANGGAN
    )

    override suspend fun reviewsOf(providerId: String): AppResult<List<ReviewItem>> {
        return AppResult.Success(reviewsList.filter { it.providerId == providerId }.map { it.item })
    }

    // ================= METRICS REPOSITORY =================
    override suspend fun trustInput(providerId: String, sinceDays: Int): AppResult<TrustInput> =
        AppResult.Success(buildTrustInput(providerId, sinceDays))

    private fun buildTrustInput(providerId: String, sinceDays: Int): TrustInput {
        val now = Clock.System.now()
        val cutoff = now - sinceDays.days
        val providerBookings = bookingsFlow.value.filter {
            it.providerId == providerId && (it.finishedAt ?: it.createdAt) in cutoff..now
        }

        val dynamicCompleted = providerBookings
            .filter { it.status == BookingStatus.SELESAI && (it.finishedAt == null || it.finishedAt >= cutoff) }
            .map { b ->
                val startAt = bookingMetaMap[b.id]?.startAt ?: b.createdAt
                val startedAt = b.startedAt ?: startAt
                CompletedJob(startAt, startedAt, b.completionOutcome)
            }
        val dynamicCancelled = providerBookings.count {
            it.status in setOf(BookingStatus.DIBATALKAN_MITRA, BookingStatus.DIBATALKAN_PELANGGAN)
        }

        val fixture = FakeData.providers.any { it.providerId == providerId }
        val baseJobs = if (fixture) seededJobsFor(providerId, now) else emptyList()
        val baseCancels = if (!fixture) 0 else when (providerId) {
            "p05" -> 4
            "p06" -> 2
            "p07", "p11" -> 0
            else -> 1
        }
        val providerRatings = reviewsList.filter {
            it.providerId == providerId && it.item.createdAt?.let { date -> date in cutoff..now } == true
        }.map { it.item.rating }
        val baseRatings = if (!fixture) emptyList() else when (providerId) {
            "p01" -> listOf(5, 5, 5, 5, 5, 5, 4) // + 3 seeded 5-star = 4.9 exact
            "p05" -> listOf(4, 4, 4, 5, 4)
            "p06" -> listOf(2, 3, 2, 2, 3)
            "p07", "p11" -> emptyList()
            else -> listOf(5, 5, 4, 5, 5)
        }

        return TrustInput(
            completed = baseJobs.filter { it.startAt >= cutoff } + dynamicCompleted,
            mitraCancelledCount = baseCancels + dynamicCancelled,
            ratings = baseRatings + providerRatings,
            otherOrderCount = providerBookings.size - dynamicCompleted.size - dynamicCancelled
        )
    }

    private fun seededJobsFor(providerId: String, now: Instant): List<CompletedJob> {
        return when (providerId) {
            "p07", "p11" -> listOf(CompletedJob(now - 2.days, now - 2.days + 5.minutes))
            "p06" -> (1..5).map { idx ->
                val start = now - idx.days
                val delay = if (idx == 1) 5.minutes else 25.minutes
                CompletedJob(start, start + delay)
            }
            else -> (1..5).map { idx ->
                val start = now - idx.days
                CompletedJob(start, start + 5.minutes)
            }
        }
    }

    override suspend fun apply(providerId: String, result: TrustResult): AppResult<Unit> {
        cacheTrust(providerId, result, addSnapshot = true)
        trustRevision.update { it + 1 }
        return AppResult.Success(Unit)
    }

    private fun cacheTrust(providerId: String, result: TrustResult, addSnapshot: Boolean) {
        val idx = providersList.indexOfFirst { it.providerId == providerId }
        if (idx >= 0) {
            val p = providersList[idx]
            providersList[idx] = p.copy(
                ratingAvg = result.ratingAvg,
                completionRate = result.completionRate,
                flagAnomaly = result.flagAnomaly,
                badge = result.badge
            )
        }
        if (!addSnapshot) return
        val history = providerSnapshotsMap.getOrPut(providerId) { mutableListOf() }
        if (history.lastOrNull()?.label == "Sekarang") history.removeAt(history.lastIndex)
        history.add(MetricSnapshot("Sekarang", result.trustScore, result.ratingScore, result.onTimeRate, result.cancelRate))
    }

    override suspend fun history(providerId: String): AppResult<List<MetricSnapshot>> {
        val existing = providerSnapshotsMap[providerId]
        return AppResult.Success(
            existing?.toList() ?: if (providerId == "p01") FakeData.sampleSnapshots else emptyList()
        )
    }

    // ================= AUTH REPOSITORY =================
    override suspend fun signIn(email: String, pass: String): AppResult<String> {
        val normalizedEmail = email.trim()
        val account = FakeData.demoAccounts.firstOrNull {
            it.email.equals(normalizedEmail, ignoreCase = true)
        }
        if (account == null || account.password != pass) {
            return AppResult.Error("Email atau kata sandi salah.")
        }

        sessionStore.setActiveRole(preferredRole(account.user))
        sessionStore.saveSession(account.user)
        return AppResult.Success(account.user.id)
    }

    override suspend fun signUp(
        name: String,
        email: String,
        pass: String,
        phone: String,
        isProvider: Boolean
    ): AppResult<String> {
        val normalizedEmail = email.trim()
        if (FakeData.demoAccounts.any { it.email.equals(normalizedEmail, ignoreCase = true) }) {
            return AppResult.Error("Email sudah terdaftar.")
        }
        if (pass.length < 8) {
            return AppResult.Error("Kata sandi minimal 8 karakter.")
        }

        val roles = mutableSetOf("customer")
        if (isProvider) roles.add("provider")
        val userId = "user_" + UUID.randomUUID().toString().take(8)
        val user = UserProfile(
            id = userId,
            fullName = name.trim(),
            email = normalizedEmail,
            phone = phone.trim(),
            roles = roles
        )
        if (isProvider) {
            ensureProviderProfileAndAvailability(
                providerId = userId,
                name = user.fullName,
                address = "Malang, Jawa Timur",
                location = FakeData.MALANG_CENTER,
                bio = "Mitra jasa Bantuin"
            )
        }
        val active = if (isProvider) "provider" else "customer"
        sessionStore.setActiveRole(active)
        sessionStore.saveSession(user)
        return AppResult.Success(user.id)
    }

    override suspend fun enableProviderRole(
        address: String,
        location: GeoPoint,
        bio: String
    ): AppResult<Unit> {
        val user = sessionStore.currentUserValue
            ?: return AppResult.Error("Sesi login tidak ditemukan.")
        val updatedRoles = user.roles + "provider"
        val updatedUser = user.copy(roles = updatedRoles)
        ensureProviderProfileAndAvailability(
            providerId = user.id,
            name = user.fullName,
            address = address.ifBlank { "Malang, Jawa Timur" },
            location = location,
            bio = bio.ifBlank { "Mitra jasa Bantuin" }
        )
        sessionStore.saveSession(updatedUser)
        sessionStore.setActiveRole("provider")
        return AppResult.Success(Unit)
    }

    private fun ensureProviderProfileAndAvailability(
        providerId: String,
        name: String,
        address: String,
        location: GeoPoint,
        bio: String
    ) {
        if (providersList.none { it.providerId == providerId }) {
            providersList.add(
                ProviderCandidate(
                    providerId = providerId,
                    name = name,
                    avatarUrl = null,
                    location = location,
                    address = address,
                    bio = bio,
                    ratingAvg = null,
                    reviewCount = 0,
                    completionRate = null,
                    flagAnomaly = true,
                    badge = ProviderBadge.NONE,
                    minBaseFare = null
                )
            )
        }
        if (!availabilityMap.containsKey(providerId)) {
            availabilityMap[providerId] = ((1..6).map {
                DayAvailability(it, AvailabilityMode.ON, openMin = 480, closeMin = 1020)
            } + DayAvailability(7, AvailabilityMode.OFF)).toMutableList()
        }
    }

    private fun preferredRole(user: UserProfile): String =
        if ("provider" in user.roles && "customer" !in user.roles) "provider" else "customer"

    override suspend fun signOut(): AppResult<Unit> {
        sessionStore.clearSession()
        return AppResult.Success(Unit)
    }

    override fun activeRole(): Flow<String> = sessionStore.activeRole

    override suspend fun setActiveRole(role: String) {
        sessionStore.setActiveRole(role)
    }

    override fun currentUser(): Flow<UserProfile?> = sessionStore.currentUser

    override suspend fun updateCustomerProfile(
        fullName: String,
        phone: String,
        defaultAddress: String,
        defaultLocation: GeoPoint
    ): AppResult<UserProfile> {
        val user = sessionStore.currentUserValue
            ?: return AppResult.Error("Sesi login tidak ditemukan.")
        val updated = user.copy(
            fullName = fullName.trim(),
            phone = phone.trim(),
            defaultAddress = defaultAddress.trim(),
            defaultLocation = defaultLocation
        )
        sessionStore.saveSession(updated)
        return AppResult.Success(updated)
    }

    // ================= CHAT REPOSITORY =================
    override fun observeChatThreads(role: String): Flow<List<ChatThreadSummary>> {
        return combine(bookingsFlow, chatMessagesFlow, sessionStore.currentUser) { bookings, msgMap, user ->
            val userId = user?.id ?: return@combine emptyList()
            val filtered = bookings.filter { b ->
                if (role == "provider") b.providerId == userId else b.customerId == userId
            }
            filtered.mapNotNull { b ->
                val msgs = msgMap[b.id].orEmpty()
                if (msgs.isEmpty() && !b.status.isActive) return@mapNotNull null
                val last = msgs.lastOrNull()
                val counterpartyId = if (role == "provider") b.customerId else b.providerId
                val counterpartyName = if (role == "provider") b.customerName else b.providerName
                val unreadCount = if (last != null && last.senderId != userId) 1 else 0
                ChatThreadSummary(
                    bookingId = b.id,
                    counterpartyId = counterpartyId,
                    counterpartyName = counterpartyName,
                    serviceName = b.serviceName,
                    bookingStatus = b.status,
                    lastMessageText = last?.text ?: "Ketuk untuk mulai obrolan terkait pesanan",
                    lastMessageAt = last?.createdAt ?: b.createdAt,
                    unreadCount = unreadCount
                )
            }.sortedByDescending { it.lastMessageAt }
        }
    }

    override fun observeMessages(bookingId: String): Flow<List<ChatMessage>> {
        return chatMessagesFlow.map { map ->
            map[bookingId].orEmpty().sortedBy { it.createdAt }
        }
    }

    override suspend fun sendMessage(bookingId: String, text: String): AppResult<Unit> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return AppResult.Error("Pesan tidak boleh kosong.")
        }
        if (trimmed.length > BantuinConfig.MAX_CHAT_MESSAGE_LENGTH) {
            return AppResult.Error("Pesan maksimal ${BantuinConfig.MAX_CHAT_MESSAGE_LENGTH} karakter.")
        }
        val user = sessionStore.currentUserValue
            ?: return AppResult.Error("Sesi login tidak ditemukan.")
        val booking = bookingsFlow.value.find { it.id == bookingId }
            ?: return AppResult.Error("Pesanan tidak ditemukan.")
        if (booking.customerId != user.id && booking.providerId != user.id) {
            return AppResult.Error("Anda tidak memiliki akses ke percakapan ini.")
        }

        val senderRole = if (sessionStore.activeRoleValue == "provider" && booking.providerId == user.id) {
            "provider"
        } else if (booking.customerId == user.id) {
            "customer"
        } else {
            "provider"
        }

        val now = Clock.System.now()
        val msg = ChatMessage(
            id = "msg_" + UUID.randomUUID().toString().take(8),
            bookingId = bookingId,
            senderId = user.id,
            senderName = user.fullName,
            senderRole = senderRole,
            text = trimmed,
            createdAt = now
        )

        val currentMap = chatMessagesFlow.value.toMutableMap()
        val updatedList = currentMap[bookingId].orEmpty().toMutableList()
        updatedList.add(msg)

        // Balasan otomatis demo agar percakapan terasa responsif di mode offline
        if (senderRole == "customer" && booking.providerId != user.id) {
            updatedList.add(
                ChatMessage(
                    id = "msg_auto_" + UUID.randomUUID().toString().take(8),
                    bookingId = bookingId,
                    senderId = booking.providerId,
                    senderName = booking.providerName,
                    senderRole = "provider",
                    text = "Baik, catatan Anda sudah saya terima. Saya segera konfirmasi di aplikasi ya.",
                    createdAt = now + 1.seconds
                )
            )
        }

        currentMap[bookingId] = updatedList
        chatMessagesFlow.value = currentMap
        return AppResult.Success(Unit)
    }

    // ================= WALLET REPOSITORY =================
    override fun observeProviderRevenue(providerId: String): Flow<ProviderRevenueAnalytics> {
        return combine(bookingsFlow, payoutsFlow) { bookings, payoutsMap ->
            val providerBookings = bookings.filter { it.providerId == providerId }
            val providerPayouts = payoutsMap[providerId].orEmpty()
            calculateRevenueUseCase(providerBookings, providerPayouts)
        }
    }

    override fun observePayoutHistory(providerId: String): Flow<List<PayoutRequest>> {
        return payoutsFlow.map { map ->
            map[providerId].orEmpty().sortedByDescending { it.createdAt }
        }
    }

    override fun observeWalletTransactions(providerId: String): Flow<List<WalletTransaction>> {
        return combine(bookingsFlow, payoutsFlow) { bookings, payoutsMap ->
            val providerBookings = bookings.filter { it.providerId == providerId }
            val providerPayouts = payoutsMap[providerId].orEmpty()
            buildWalletTransactionsUseCase(providerBookings, providerPayouts)
        }
    }

    override suspend fun requestPayout(
        providerId: String,
        amount: Int,
        channelType: PayoutChannelType,
        channelName: String,
        accountNumber: String,
        accountHolder: String
    ): AppResult<PayoutRequest> {
        if (amount < BantuinConfig.MIN_PAYOUT_AMOUNT) {
            return AppResult.Error("Minimal penarikan dana adalah Rp20.000.")
        }
        val cleanChannel = channelName.trim()
        val cleanAccNumber = accountNumber.trim()
        val cleanAccHolder = accountHolder.trim()
        if (cleanChannel.isBlank() || cleanAccNumber.isBlank() || cleanAccHolder.isBlank()) {
            return AppResult.Error("Lengkapi data rekening atau e-wallet tujuan.")
        }

        val currentAnalytics = calculateRevenueUseCase(
            bookings = bookingsFlow.value.filter { it.providerId == providerId },
            payouts = payoutsFlow.value[providerId].orEmpty()
        )
        if (amount > currentAnalytics.availableBalance) {
            return AppResult.Error("Saldo siap cair tidak mencukupi.")
        }

        val adminFee = BantuinConfig.PAYOUT_ADMIN_FEE
        val netAmount = amount - adminFee
        if (netAmount <= 0) {
            return AppResult.Error("Nominal penarikan harus lebih besar dari biaya admin.")
        }

        val request = PayoutRequest(
            id = "po_" + UUID.randomUUID().toString().take(8),
            providerId = providerId,
            amount = amount,
            adminFee = adminFee,
            netAmount = netAmount,
            channelType = channelType,
            channelName = cleanChannel,
            accountNumber = cleanAccNumber,
            accountHolder = cleanAccHolder,
            status = PayoutStatus.COMPLETED,
            createdAt = Clock.System.now()
        )
        val currentMap = payoutsFlow.value.toMutableMap()
        val updated = listOf(request) + currentMap[providerId].orEmpty()
        currentMap[providerId] = updated
        payoutsFlow.value = currentMap
        return AppResult.Success(request)
    }
}

package id.bantuin.app.data.repository

import android.util.Log
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.session.UserSessionStore
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.data.remote.SupabaseClientProvider
import id.bantuin.app.data.remote.dto.*
import id.bantuin.app.domain.model.*
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.domain.repository.UserProfile
import id.bantuin.app.domain.usecase.trust.RecalculateProviderMetricsUseCase
import id.bantuin.app.domain.usecase.wallet.CalculateProviderRevenueUseCase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.days

class SupabaseBantuinRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val sessionStore: UserSessionStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val enableRealtime: Boolean = SupabaseClientProvider.isConfigured
) : BantuinRepository {

    private val recalculateMetricsUseCase by lazy {
        RecalculateProviderMetricsUseCase(this)
    }
    private val calculateRevenueUseCase = CalculateProviderRevenueUseCase()

    private val realtimeScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val customerBookingsFlow = MutableStateFlow<List<BookingItem>>(emptyList())
    private val providerBookingsFlow = MutableStateFlow<List<BookingItem>>(emptyList())
    private val acceptingOrdersFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val chatMessagesFlow = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    private val payoutsFlow = MutableStateFlow<Map<String, List<PayoutRequest>>>(emptyMap())

    private var activeRealtimeUserId: String? = null
    private var activeRealtimeChannel: RealtimeChannel? = null
    private var realtimeChangesJob: Job? = null
    private var realtimeStatusJob: Job? = null

    companion object {
        private const val LOG_TAG_LOGIN = "LOGIN"
        private const val LOG_TAG_REALTIME = "BantuinRealtime"
    }

    init {
        if (enableRealtime) {
            observeSessionForRealtime()
        }
    }

    private fun observeSessionForRealtime() {
        sessionStore.currentUser
            .onEach { user ->
                val userId = user?.id
                if (userId != activeRealtimeUserId) {
                    teardownRealtimeChannel()
                    activeRealtimeUserId = userId
                    if (userId != null) {
                        startRealtimeForUser(userId)
                    } else {
                        customerBookingsFlow.value = emptyList()
                        providerBookingsFlow.value = emptyList()
                        chatMessagesFlow.value = emptyMap()
                        payoutsFlow.value = emptyMap()
                    }
                }
            }
            .launchIn(realtimeScope)
    }

    private fun startRealtimeForUser(userId: String) {
        realtimeChangesJob = realtimeScope.launch {
            try {
                client.auth.awaitInitialization()
                val channel = client.realtime.channel("bookings_sync_$userId")
                activeRealtimeChannel = channel

                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "bookings"
                }.onEach {
                    refreshBookingsForCurrentUser()
                }.launchIn(this)

                channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "booking_messages"
                }.onEach {
                    refreshMessagesForKnownBookings()
                }.launchIn(this)

                channel.subscribe()
                refreshBookingsForCurrentUser()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                logDebug(LOG_TAG_REALTIME, "Gagal langganan Realtime: ${t.message}")
            }
        }

        realtimeStatusJob = realtimeScope.launch {
            try {
                client.realtime.status.collect { status ->
                    if (status == Realtime.Status.CONNECTED && activeRealtimeUserId == userId) {
                        refreshBookingsForCurrentUser()
                    }
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Throwable) {
            }
        }
    }

    private suspend fun refreshBookingsForCurrentUser() {
        val user = sessionStore.currentUserValue
        val roles = user?.roles.orEmpty()
        if ("customer" in roles || roles.isEmpty()) {
            myBookingsCustomer()
        }
        if ("provider" in roles) {
            myBookingsProvider()
            user?.id?.let { refreshPayoutHistory(it) }
        }
        refreshMessagesForKnownBookings()
    }

    private suspend fun teardownRealtimeChannel() {
        realtimeChangesJob?.cancel()
        realtimeStatusJob?.cancel()
        realtimeChangesJob = null
        realtimeStatusJob = null
        val prev = activeRealtimeChannel
        activeRealtimeChannel = null
        if (prev != null) {
            try {
                client.realtime.removeChannel(prev)
            } catch (_: Throwable) {
            }
        }
    }

    private fun currentUidOrNull(): String? =
        sessionStore.currentUserValue?.id ?: try {
            client.auth.currentUserOrNull()?.id
        } catch (_: Throwable) {
            null
        }

    // =========================================================================
    // CATALOG REPOSITORY (CRUD-1, CRUD-2, CRUD-3, F1)
    // =========================================================================

    override suspend fun categories(): AppResult<List<ServiceCategory>> = withContext(ioDispatcher) {
        try {
            val dtoList = client.postgrest["service_categories"]
                .select {
                    order(column = "id", order = Order.ASCENDING)
                }
                .decodeList<ServiceCategoryDto>()
            val list = dtoList.map { dto ->
                ServiceCategory(
                    id = dto.id,
                    name = dto.name,
                    iconKey = dto.icon,
                    defaultDurationMin = dto.defaultDurationMin,
                    description = categorySubtitle(dto.id)
                )
            }
            AppResult.Success(list)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memuat kategori layanan")
        }
    }

    override suspend fun candidates(categoryId: Int): AppResult<List<ProviderCandidate>> = withContext(ioDispatcher) {
        try {
            val selfId = currentUidOrNull()
            val servicesDto = client.postgrest["services"]
                .select {
                    filter {
                        eq("category_id", categoryId)
                        eq("is_active", true)
                    }
                }
                .decodeList<ServiceDto>()

            val providerIds = servicesDto
                .map { it.providerId }
                .distinct()
                .filter { selfId == null || it != selfId }
            if (providerIds.isEmpty()) {
                return@withContext AppResult.Success(emptyList())
            }

            val ppDtoList = client.postgrest["provider_profiles"]
                .select {
                    filter {
                        isIn("user_id", providerIds)
                        eq("is_suspended", false)
                    }
                }
                .decodeList<ProviderProfileDto>()

            val profilesDto = client.postgrest["profiles"]
                .select {
                    filter {
                        isIn("id", providerIds)
                    }
                }
                .decodeList<ProfileDto>()

            val availDtoList = client.postgrest["availability"]
                .select {
                    filter {
                        isIn("provider_id", providerIds)
                    }
                }
                .decodeList<AvailabilityDto>()

            val reviewCountMap = runCatching {
                client.postgrest["reviews"]
                    .select(columns = Columns.list("provider_id")) {
                        filter {
                            isIn("provider_id", providerIds)
                        }
                    }
                    .decodeList<ReviewProviderRefDto>()
                    .groupingBy { it.providerId }
                    .eachCount()
            }.getOrNull()

            val candidates = ppDtoList.mapNotNull { pp ->
                if (!pp.isAcceptingOrders) return@mapNotNull null
                val profile = profilesDto.find { it.id == pp.userId } ?: return@mapNotNull null
                val userAvail = availDtoList.filter { it.providerId == pp.userId }
                val isAvailableAnyDay = userAvail.isNotEmpty() && userAvail.any { it.mode != "OFF" }
                if (!isAvailableAnyDay) return@mapNotNull null

                val pServices = servicesDto.filter { it.providerId == pp.userId }
                val minFare = pServices.minOfOrNull { it.baseFare }
                mapProviderCandidate(
                    pp = pp,
                    profile = profile,
                    minFare = minFare,
                    reviewCount = reviewCountMap?.get(pp.userId)
                )
            }
            AppResult.Success(candidates)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memuat daftar mitra")
        }
    }

    override suspend fun providerProfile(providerId: String): AppResult<ProviderCandidate?> = withContext(ioDispatcher) {
        try {
            val pp = client.postgrest["provider_profiles"]
                .select { filter { eq("user_id", providerId) } }
                .decodeList<ProviderProfileDto>()
                .firstOrNull()
                ?: return@withContext AppResult.Success(null)

            val profile = client.postgrest["profiles"]
                .select { filter { eq("id", providerId) } }
                .decodeList<ProfileDto>()
                .firstOrNull()
                ?: return@withContext AppResult.Success(null)

            val services = client.postgrest["services"]
                .select {
                    filter {
                        eq("provider_id", providerId)
                        eq("is_active", true)
                    }
                }
                .decodeList<ServiceDto>()

            val reviewCount = runCatching {
                client.postgrest["reviews"]
                    .select(columns = Columns.list("provider_id")) {
                        filter { eq("provider_id", providerId) }
                    }
                    .decodeList<ReviewProviderRefDto>()
                    .size
            }.getOrNull()

            AppResult.Success(
                mapProviderCandidate(
                    pp = pp,
                    profile = profile,
                    minFare = services.minOfOrNull { it.baseFare },
                    reviewCount = reviewCount
                )
            )
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memuat profil mitra")
        }
    }

    override suspend fun servicesOf(providerId: String): AppResult<List<ServiceItem>> = withContext(ioDispatcher) {
        try {
            val isOwner = currentUidOrNull() == providerId
            val listDto = client.postgrest["services"]
                .select {
                    filter {
                        eq("provider_id", providerId)
                        if (!isOwner) {
                            eq("is_active", true)
                        }
                    }
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList<ServiceDto>()
            val items = listDto.map { dto ->
                ServiceItem(
                    id = dto.id,
                    providerId = dto.providerId,
                    categoryId = dto.categoryId,
                    name = dto.name,
                    description = dto.description,
                    baseFare = dto.baseFare,
                    durationMin = dto.durationMin,
                    isActive = dto.isActive
                )
            }
            AppResult.Success(items)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memuat daftar layanan")
        }
    }

    override suspend fun availability(providerId: String): AppResult<List<DayAvailability>> = withContext(ioDispatcher) {
        try {
            val listDto = client.postgrest["availability"]
                .select {
                    filter {
                        eq("provider_id", providerId)
                    }
                    order(column = "day_of_week", order = Order.ASCENDING)
                }
                .decodeList<AvailabilityDto>()
            val existingByDay = listDto.associateBy { it.dayOfWeek }
            val list = (1..7).map { day ->
                val dto = existingByDay[day]
                if (dto == null) {
                    DayAvailability(
                        dayOfWeek = day,
                        mode = if (day == 7) AvailabilityMode.OFF else AvailabilityMode.ON,
                        openMin = if (day == 7) null else 480,
                        closeMin = if (day == 7) null else 1020
                    )
                } else {
                    DayAvailability(
                        dayOfWeek = dto.dayOfWeek,
                        mode = when (dto.mode) {
                            "ALWAYS_ON" -> AvailabilityMode.ALWAYS_ON
                            "ON" -> AvailabilityMode.ON
                            else -> AvailabilityMode.OFF
                        },
                        openMin = dto.openTime?.let { parseTimeToMin(it) },
                        closeMin = dto.closeTime?.let { parseTimeToMin(it) }
                    )
                }
            }
            AppResult.Success(list)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memuat jadwal ketersediaan")
        }
    }

    override suspend fun saveAvailability(providerId: String, days: List<DayAvailability>): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val dtoList = days.map { day ->
                AvailabilityDto(
                    providerId = providerId,
                    dayOfWeek = day.dayOfWeek,
                    mode = day.mode.name,
                    openTime = if (day.mode == AvailabilityMode.ON) day.openMin?.let { formatMinToTime(it) } else null,
                    closeTime = if (day.mode == AvailabilityMode.ON) day.closeMin?.let { formatMinToTime(it) } else null
                )
            }
            client.postgrest["availability"].upsert(dtoList)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal menyimpan jadwal ketersediaan")
        }
    }

    override suspend fun upsertService(service: ServiceItem): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val validUuid = try {
                UUID.fromString(service.id).toString()
            } catch (_: Exception) {
                UUID.randomUUID().toString()
            }
            val dto = ServiceDto(
                id = validUuid,
                providerId = service.providerId,
                categoryId = service.categoryId,
                name = service.name,
                description = service.description,
                baseFare = service.baseFare,
                durationMin = service.durationMin,
                isActive = service.isActive
            )
            client.postgrest["services"].upsert(dto)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal menyimpan layanan")
        }
    }

    override suspend fun deleteService(serviceId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.postgrest["services"].delete {
                filter {
                    eq("id", serviceId)
                }
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal menghapus layanan")
        }
    }

    override suspend fun updateProviderProfile(
        providerId: String,
        name: String,
        bio: String,
        location: GeoPoint,
        address: String,
        phone: String
    ): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.postgrest["profiles"].update(
                mapOf("full_name" to name, "phone" to phone)
            ) {
                filter { eq("id", providerId) }
            }

            val existing = client.postgrest["provider_profiles"]
                .select { filter { eq("user_id", providerId) } }
                .decodeList<ProviderProfileDto>()
                .firstOrNull()

            if (existing != null) {
                client.postgrest["provider_profiles"].update(
                    mapOf(
                        "bio" to bio,
                        "address" to address,
                        "lat" to location.lat,
                        "lng" to location.lng
                    )
                ) {
                    filter { eq("user_id", providerId) }
                }
            } else {
                client.postgrest["provider_profiles"].insert(
                    ProviderProfileUpsertDto(
                        userId = providerId,
                        bio = bio,
                        address = address,
                        lat = location.lat,
                        lng = location.lng
                    )
                )
            }

            val user = sessionStore.currentUserValue
            if (user != null && user.id == providerId) {
                sessionStore.saveSession(user.copy(fullName = name, phone = phone))
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memperbarui profil mitra")
        }
    }

    override fun observeAcceptingOrders(providerId: String): Flow<Boolean> {
        if (!acceptingOrdersFlow.value.containsKey(providerId) && SupabaseClientProvider.isConfigured) {
            realtimeScope.launch {
                runCatching {
                    val pp = client.postgrest["provider_profiles"]
                        .select { filter { eq("user_id", providerId) } }
                        .decodeList<ProviderProfileDto>()
                        .firstOrNull()
                    if (pp != null) {
                        acceptingOrdersFlow.value = acceptingOrdersFlow.value + (providerId to pp.isAcceptingOrders)
                    }
                }
            }
        }
        return acceptingOrdersFlow.map { it[providerId] ?: true }
    }

    override suspend fun setAcceptingOrders(providerId: String, accepting: Boolean): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.postgrest["provider_profiles"].update(
                buildJsonObject { put("is_accepting_orders", accepting) }
            ) {
                filter { eq("user_id", providerId) }
            }
            acceptingOrdersFlow.value = acceptingOrdersFlow.value + (providerId to accepting)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal mengubah status penerimaan pesanan")
        }
    }

    // =========================================================================
    // BOOKING REPOSITORY (F2, F3 & Realtime RPC)
    // =========================================================================

    override suspend fun busyBlocks(providerId: String, dateIso: String): AppResult<List<TimeBlock>> = withContext(ioDispatcher) {
        try {
            val fromIso = TimeUtils.toIsoString(dateIso, 0)
            val toIso = TimeUtils.toIsoString(dateIso, 1440)

            val result = client.postgrest.rpc(
                function = "get_busy_blocks",
                parameters = mapOf(
                    "p_provider" to providerId,
                    "p_from" to fromIso,
                    "p_to" to toIso
                )
            ).decodeList<BusyBlockDto>()

            val blocks = result.mapNotNull { dto ->
                val startMin = parseIsoToWibMinutesOnDate(dto.startAt, dateIso)
                val endMin = parseIsoToWibMinutesOnDate(dto.endAt, dateIso)
                if (endMin > startMin) TimeBlock(startMin, endMin) else null
            }
            AppResult.Success(blocks)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal mengecek jadwal bentrok")
        }
    }

    override suspend fun acquireLock(
        providerId: String,
        dateIso: String,
        startMin: Int,
        endMin: Int
    ): AppResult<LockInfo> = withContext(ioDispatcher) {
        try {
            val startIso = TimeUtils.toIsoString(dateIso, startMin)
            val endIso = TimeUtils.toIsoString(dateIso, endMin)

            val lockId = client.postgrest.rpc(
                function = "acquire_slot_lock",
                parameters = AcquireSlotLockRpcParams(
                    providerId = providerId,
                    startAt = startIso,
                    endAt = endIso
                )
            ).decodeAs<String>()

            val lockInfo = LockInfo(
                lockId = lockId,
                providerId = providerId,
                startMin = startMin,
                endMin = endMin,
                expiresAtMillis = Clock.System.now().toEpochMilliseconds() + 600_000L,
                dateIso = dateIso
            )
            AppResult.Success(lockInfo)
        } catch (e: Exception) {
            mapSupabaseError(e, "Slot sedang dikunci pengguna lain")
        }
    }

    override suspend fun releaseLock(lockId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.postgrest.rpc("release_slot_lock")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal melepas kunci slot")
        }
    }

    override suspend fun createBooking(draft: OrderDraft): AppResult<String> = withContext(ioDispatcher) {
        try {
            val lockId = draft.lockInfo?.lockId
                ?: return@withContext AppResult.Error("Waktu penahanan slot habis, pilih jadwal lagi")
            val estimate = draft.costEstimate
            val baseFare = estimate?.baseFare ?: draft.service.baseFare
            val baseCost = estimate?.baseCost ?: baseFare
            val surgeApplied = estimate?.surgeApplied ?: false
            val totalCost = estimate?.total ?: baseCost

            val bookingId = client.postgrest.rpc(
                function = "create_booking",
                parameters = CreateBookingRpcParams(
                    lockId = lockId,
                    serviceId = draft.service.id,
                    complexity = draft.complexity.name,
                    isUrgent = draft.isUrgent,
                    distanceKm = draft.distanceKm,
                    baseFare = baseFare,
                    baseCost = baseCost,
                    surgeApplied = surgeApplied,
                    totalCost = totalCost,
                    customerLat = draft.customerLocation.lat,
                    customerLng = draft.customerLocation.lng,
                    notes = draft.notes.ifBlank { null },
                    paymentMethod = draft.paymentMethod.name
                )
            ).decodeAs<String>()

            myBookingsCustomer()
            AppResult.Success(bookingId)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal membuat pesanan")
        }
    }

    override suspend fun confirmQrisPayment(bookingId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.postgrest.rpc(
                function = "confirm_qris_payment",
                parameters = ConfirmQrisPaymentRpcParams(bookingId = bookingId)
            )
            refreshBookingsForCurrentUser()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal mengonfirmasi pembayaran QRIS")
        }
    }

    override suspend fun completeBooking(
        bookingId: String,
        completionNote: String?,
        cashCollectedConfirmed: Boolean
    ): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.postgrest.rpc(
                function = "complete_booking",
                parameters = CompleteBookingRpcParams(
                    bookingId = bookingId,
                    completionNote = completionNote?.trim()?.takeIf { it.isNotBlank() },
                    cashCollected = cashCollectedConfirmed
                )
            )
            refreshBookingsForCurrentUser()
            val targetBooking = (customerBookingsFlow.value + providerBookingsFlow.value)
                .find { it.id == bookingId }
            if (targetBooking != null) {
                recalculateMetricsUseCase(targetBooking.providerId)
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal menyelesaikan pesanan")
        }
    }

    override suspend fun updateStatus(bookingId: String, newStatus: BookingStatus): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.postgrest.rpc(
                function = "update_booking_status",
                parameters = UpdateBookingStatusRpcParams(
                    bookingId = bookingId,
                    newStatus = newStatus.name
                )
            )
            refreshBookingsForCurrentUser()
            if (newStatus in setOf(BookingStatus.SELESAI, BookingStatus.DIBATALKAN_MITRA)) {
                val targetBooking = (customerBookingsFlow.value + providerBookingsFlow.value)
                    .find { it.id == bookingId }
                if (targetBooking != null) {
                    recalculateMetricsUseCase(targetBooking.providerId)
                }
            }
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memperbarui status pesanan")
        }
    }

    override suspend fun myBookingsCustomer(): AppResult<List<BookingItem>> = withContext(ioDispatcher) {
        try {
            val uid = currentUidOrNull()
                ?: return@withContext AppResult.Error("Sesi login berakhir, silakan masuk kembali")

            val dtoList = client.postgrest["bookings"]
                .select {
                    filter { eq("customer_id", uid) }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<BookingDto>()

            val items = enrichBookingDtos(dtoList)
            customerBookingsFlow.value = items
            AppResult.Success(items)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memuat pesanan pelanggan")
        }
    }

    override suspend fun myBookingsProvider(): AppResult<List<BookingItem>> = withContext(ioDispatcher) {
        try {
            val uid = currentUidOrNull()
                ?: return@withContext AppResult.Error("Sesi login berakhir, silakan masuk kembali")

            val dtoList = client.postgrest["bookings"]
                .select {
                    filter { eq("provider_id", uid) }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<BookingDto>()

            val items = enrichBookingDtos(dtoList)
            providerBookingsFlow.value = items
            AppResult.Success(items)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memuat antrean pesanan mitra")
        }
    }

    override fun observeBooking(bookingId: String): Flow<BookingItem?> {
        return combine(customerBookingsFlow, providerBookingsFlow) { customerList, providerList ->
            customerList.find { it.id == bookingId } ?: providerList.find { it.id == bookingId }
        }
    }

    override fun observeCustomerBookings(): Flow<List<BookingItem>> {
        return customerBookingsFlow.asStateFlow()
    }

    override fun observeProviderBookings(): Flow<List<BookingItem>> {
        return providerBookingsFlow.asStateFlow()
    }

    override suspend fun events(bookingId: String): AppResult<List<BookingEvent>> = withContext(ioDispatcher) {
        try {
            val dtoList = client.postgrest["booking_events"]
                .select {
                    filter { eq("booking_id", bookingId) }
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList<BookingEventDto>()

            val events = dtoList.map { dto ->
                val status = try {
                    BookingStatus.valueOf(dto.status)
                } catch (_: Exception) {
                    BookingStatus.PENDING
                }
                val created = try {
                    dto.createdAt?.let { Instant.parse(it) } ?: Clock.System.now()
                } catch (_: Exception) {
                    Clock.System.now()
                }
                BookingEvent(
                    id = dto.id ?: 0L,
                    bookingId = dto.bookingId,
                    status = status,
                    actorId = dto.actorId,
                    createdAt = created
                )
            }
            AppResult.Success(events)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memuat riwayat status pesanan")
        }
    }

    override suspend fun expirePending(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.postgrest.rpc("expire_pending_bookings")
            refreshBookingsForCurrentUser()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal menyinkronkan batas waktu pesanan")
        }
    }

    // =========================================================================
    // REVIEW REPOSITORY (CRUD-4, F4 Feedback Loop)
    // =========================================================================

    override suspend fun submitReview(
        bookingId: String,
        providerId: String,
        rating: Int,
        comment: String?,
        tags: List<String>
    ): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val currentUserId = currentUidOrNull()
                ?: return@withContext AppResult.Error("Sesi login berakhir, silakan masuk kembali")
            val reviewDto = ReviewDto(
                id = UUID.randomUUID().toString(),
                bookingId = bookingId,
                customerId = currentUserId,
                providerId = providerId,
                rating = rating,
                comment = comment?.takeIf { it.isNotBlank() }
            )
            client.postgrest["reviews"].insert(reviewDto)

            // Jalankan feedback loop rekalkulasi metrik mitra (F4 -> F1)
            recalculateMetricsUseCase(providerId)
            refreshBookingsForCurrentUser()

            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal mengirim ulasan")
        }
    }

    override suspend fun reviewsOf(providerId: String): AppResult<List<ReviewItem>> = withContext(ioDispatcher) {
        try {
            val listDto = client.postgrest["reviews"]
                .select {
                    filter { eq("provider_id", providerId) }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<ReviewDto>()

            val customerIds = listDto.map { it.customerId }.distinct()
            val profilesMap = if (customerIds.isNotEmpty()) {
                client.postgrest["profiles"]
                    .select { filter { isIn("id", customerIds) } }
                    .decodeList<ProfileDto>()
                    .associateBy { it.id }
            } else {
                emptyMap()
            }

            val reviews = listDto.map { dto ->
                ReviewItem(
                    id = dto.id,
                    bookingId = dto.bookingId,
                    customerName = profilesMap[dto.customerId]?.fullName ?: "Pelanggan",
                    rating = dto.rating,
                    comment = dto.comment,
                    tags = emptyList(),
                    createdAtText = dto.createdAt?.take(10) ?: "Baru saja"
                )
            }
            AppResult.Success(reviews)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memuat ulasan")
        }
    }

    // =========================================================================
    // METRICS REPOSITORY (F4 Trust & Anomaly)
    // =========================================================================

    override suspend fun trustInput(providerId: String, sinceDays: Int): AppResult<TrustInput> = withContext(ioDispatcher) {
        try {
            val cutoff = Clock.System.now() - sinceDays.days
            val cutoffIso = cutoff.toString()

            val reviewsDto = client.postgrest["reviews"]
                .select {
                    filter {
                        eq("provider_id", providerId)
                        gte("created_at", cutoffIso)
                    }
                }
                .decodeList<ReviewDto>()
            val ratings = reviewsDto.map { it.rating }

            val bookingsDto = client.postgrest.rpc(
                "get_provider_trust_bookings",
                TrustBookingsRpcParams(providerId = providerId, cutoffIso = cutoffIso)
            ).decodeList<TrustBookingDto>()

            val recentBookings = bookingsDto.filter { b ->
                val refIso = b.finishedAt ?: b.createdAt ?: b.startAt
                val refInstant = parseInstantOrNull(refIso)
                refInstant == null || refInstant >= cutoff
            }

            val finished = recentBookings.filter { it.status == "SELESAI" }
            val cancelledByProvider = recentBookings.filter { it.status == "DIBATALKAN_MITRA" }

            val jobs = finished.map { b ->
                val startInstant = parseInstantOrNull(b.startAt) ?: Clock.System.now()
                val startedInstant = b.startedAt?.let { parseInstantOrNull(it) } ?: startInstant
                CompletedJob(startAt = startInstant, startedAt = startedInstant)
            }

            AppResult.Success(
                TrustInput(
                    completed = jobs,
                    mitraCancelledCount = cancelledByProvider.size,
                    ratings = ratings
                )
            )
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal menghitung data trust mitra")
        }
    }

    override suspend fun apply(providerId: String, result: TrustResult): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.postgrest.rpc(
                function = "apply_provider_metrics",
                parameters = ApplyProviderMetricsRpcParams(
                    providerId = providerId,
                    ratingAvg = result.ratingAvg,
                    completionRate = result.completionRate,
                    onTimeRate = result.onTimeRate,
                    cancelRate = result.cancelRate,
                    trustScore = result.trustScore,
                    txCount = result.txCount,
                    flagAnomaly = result.flagAnomaly,
                    badge = result.badge.name,
                    ratingScore = result.ratingScore
                )
            )
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal menerapkan metrik evaluasi")
        }
    }

    override suspend fun history(providerId: String): AppResult<List<MetricSnapshot>> = withContext(ioDispatcher) {
        try {
            val listDto = client.postgrest["provider_metric_snapshots"]
                .select {
                    filter {
                        eq("provider_id", providerId)
                    }
                    order(column = "computed_at", order = Order.ASCENDING)
                }
                .decodeList<ProviderMetricSnapshotDto>()

            val history = listDto.map { dto ->
                MetricSnapshot(
                    label = dto.computedAt?.take(10) ?: "Snapshot",
                    trustScore = dto.trustScore,
                    ratingScore = dto.ratingScore ?: 70.0,
                    onTimeRate = dto.onTimeRate ?: 0.0,
                    cancelRate = dto.cancelRate ?: 0.0
                )
            }
            AppResult.Success(history)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal memuat riwayat metrik")
        }
    }

    // =========================================================================
    // AUTH REPOSITORY (CRUD-0 Multi-Role)
    // =========================================================================

    override suspend fun signIn(email: String, pass: String): AppResult<String> = withContext(ioDispatcher) {
        val normalizedEmail = email.trim()
        try {
            client.auth.awaitInitialization()
            logDebug(LOG_TAG_LOGIN, "Mencoba login dengan email: $normalizedEmail")
            client.auth.signInWith(Email) {
                this.email = normalizedEmail
                this.password = pass
            }

            val authUser = client.auth.currentUserOrNull()
                ?: return@withContext AppResult.Error("Gagal masuk: sesi pengguna tidak ditemukan.")

            val user = loadUserProfile(authUser, email)
            sessionStore.setActiveRole(preferredRole(user))
            sessionStore.saveSession(user)
            AppResult.Success(user.id)
        } catch (e: Exception) {
            logError(LOG_TAG_LOGIN, "Login gagal: ${e.message}", e)
            AppResult.Error(authErrorMessage(e, "masuk"), e)
        }
    }

    override suspend fun signUp(
        name: String,
        email: String,
        pass: String,
        phone: String,
        isProvider: Boolean
    ): AppResult<String> = withContext(ioDispatcher) {
        try {
            val roleList = if (isProvider) listOf("customer", "provider") else listOf("customer")
            client.auth.awaitInitialization()
            client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = pass
                data = buildJsonObject {
                    put("full_name", name.trim())
                    if (phone.isNotBlank()) put("phone", phone.trim())
                    putJsonArray("roles") { roleList.forEach { add(it) } }
                }
            }

            val authUser = client.auth.currentUserOrNull()
                ?: return@withContext AppResult.Error(
                    "Pendaftaran berhasil. Silakan konfirmasi email Anda sebelum masuk."
                )

            val user = loadUserProfile(authUser, email)
            val initialActiveRole = if (isProvider && "provider" in user.roles) "provider" else preferredRole(user)
            sessionStore.setActiveRole(initialActiveRole)
            sessionStore.saveSession(user)
            AppResult.Success(user.id)
        } catch (e: Exception) {
            AppResult.Error(authErrorMessage(e, "mendaftar"), e)
        }
    }

    override suspend fun enableProviderRole(
        address: String,
        location: GeoPoint,
        bio: String
    ): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.postgrest.rpc(
                function = "enable_provider_role",
                parameters = EnableProviderRoleRpcParams(
                    bio = bio.trim(),
                    address = address.trim(),
                    lat = location.lat,
                    lng = location.lng
                )
            )
            val authUser = client.auth.currentUserOrNull()
            if (authUser != null) {
                val updated = loadUserProfile(authUser, authUser.email.orEmpty())
                sessionStore.saveSession(updated)
            } else {
                sessionStore.currentUserValue?.let { cur ->
                    sessionStore.saveSession(cur.copy(roles = cur.roles + "provider"))
                }
            }
            sessionStore.setActiveRole("provider")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal mengaktifkan peran mitra")
        }
    }

    override suspend fun signOut(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            teardownRealtimeChannel()
            client.auth.signOut()
            sessionStore.clearSession()
            AppResult.Success(Unit)
        } catch (_: Exception) {
            sessionStore.clearSession()
            AppResult.Success(Unit)
        }
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
    ): AppResult<UserProfile> = withContext(ioDispatcher) {
        try {
            val uid = currentUidOrNull()
                ?: return@withContext AppResult.Error("Sesi login berakhir, silakan masuk kembali")
            val trimmedName = fullName.trim()
            val trimmedPhone = phone.trim()
            val trimmedAddress = defaultAddress.trim()

            client.postgrest["profiles"].update(
                buildJsonObject {
                    put("full_name", trimmedName)
                    put("phone", trimmedPhone)
                    put("default_address", trimmedAddress)
                    put("default_lat", defaultLocation.lat)
                    put("default_lng", defaultLocation.lng)
                }
            ) {
                filter { eq("id", uid) }
            }

            val current = sessionStore.currentUserValue ?: UserProfile(
                id = uid,
                fullName = trimmedName,
                email = "",
                phone = trimmedPhone,
                roles = setOf("customer")
            )
            val updated = current.copy(
                fullName = trimmedName,
                phone = trimmedPhone,
                defaultAddress = trimmedAddress,
                defaultLocation = defaultLocation
            )
            sessionStore.saveSession(updated)
            AppResult.Success(updated)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal menyimpan profil pelanggan")
        }
    }

    // =========================================================================
    // CHAT REPOSITORY (Built-in Chat per Pesanan)
    // =========================================================================

    override fun observeChatThreads(role: String): Flow<List<ChatThreadSummary>> {
        return combine(
            customerBookingsFlow,
            providerBookingsFlow,
            chatMessagesFlow,
            sessionStore.currentUser
        ) { customerList, providerList, msgMap, user ->
            val userId = user?.id ?: return@combine emptyList()
            val source = if (role == "provider") providerList else customerList
            source.mapNotNull { b ->
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
        if (!chatMessagesFlow.value.containsKey(bookingId) && SupabaseClientProvider.isConfigured) {
            realtimeScope.launch {
                refreshMessagesForBooking(bookingId)
            }
        }
        return chatMessagesFlow.map { map ->
            map[bookingId].orEmpty().sortedBy { it.createdAt }
        }
    }

    override suspend fun sendMessage(bookingId: String, text: String): AppResult<Unit> = withContext(ioDispatcher) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) {
            return@withContext AppResult.Error("Pesan tidak boleh kosong.")
        }
        if (trimmed.length > BantuinConfig.MAX_CHAT_MESSAGE_LENGTH) {
            return@withContext AppResult.Error("Pesan maksimal ${BantuinConfig.MAX_CHAT_MESSAGE_LENGTH} karakter.")
        }
        try {
            val uid = currentUidOrNull()
                ?: return@withContext AppResult.Error("Sesi login berakhir, silakan masuk kembali")
            val role = if (sessionStore.activeRoleValue == "provider") "provider" else "customer"

            client.postgrest["booking_messages"].insert(
                BookingMessageInsertDto(
                    bookingId = bookingId,
                    senderId = uid,
                    senderRole = role,
                    messageText = trimmed
                )
            )
            refreshMessagesForBooking(bookingId)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal mengirim pesan")
        }
    }

    private suspend fun refreshMessagesForKnownBookings() {
        val ids = (customerBookingsFlow.value + providerBookingsFlow.value)
            .map { it.id }
            .distinct()
        if (ids.isEmpty() || !SupabaseClientProvider.isConfigured) return
        runCatching {
            val msgsDto = client.postgrest["booking_messages"]
                .select {
                    filter { isIn("booking_id", ids) }
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList<BookingMessageDto>()
            val senderIds = msgsDto.map { it.senderId }.distinct()
            val profilesMap = if (senderIds.isNotEmpty()) {
                runCatching {
                    client.postgrest["profiles"]
                        .select { filter { isIn("id", senderIds) } }
                        .decodeList<ProfileDto>()
                        .associateBy { it.id }
                }.getOrDefault(emptyMap())
            } else {
                emptyMap()
            }
            val grouped = msgsDto.groupBy { it.bookingId }.mapValues { (_, list) ->
                list.map { dto ->
                    ChatMessage(
                        id = dto.id,
                        bookingId = dto.bookingId,
                        senderId = dto.senderId,
                        senderName = profilesMap[dto.senderId]?.fullName
                            ?: if (dto.senderRole == "provider") "Mitra" else "Pelanggan",
                        senderRole = dto.senderRole,
                        text = dto.messageText,
                        createdAt = parseInstantOrNull(dto.createdAt) ?: Clock.System.now()
                    )
                }
            }
            chatMessagesFlow.value = chatMessagesFlow.value + grouped
        }
    }

    private suspend fun refreshMessagesForBooking(bookingId: String) {
        if (!SupabaseClientProvider.isConfigured) return
        runCatching {
            val msgsDto = client.postgrest["booking_messages"]
                .select {
                    filter { eq("booking_id", bookingId) }
                    order(column = "created_at", order = Order.ASCENDING)
                }
                .decodeList<BookingMessageDto>()
            val senderIds = msgsDto.map { it.senderId }.distinct()
            val profilesMap = if (senderIds.isNotEmpty()) {
                runCatching {
                    client.postgrest["profiles"]
                        .select { filter { isIn("id", senderIds) } }
                        .decodeList<ProfileDto>()
                        .associateBy { it.id }
                }.getOrDefault(emptyMap())
            } else {
                emptyMap()
            }
            val mapped = msgsDto.map { dto ->
                ChatMessage(
                    id = dto.id,
                    bookingId = dto.bookingId,
                    senderId = dto.senderId,
                    senderName = profilesMap[dto.senderId]?.fullName
                        ?: if (dto.senderRole == "provider") "Mitra" else "Pelanggan",
                    senderRole = dto.senderRole,
                    text = dto.messageText,
                    createdAt = parseInstantOrNull(dto.createdAt) ?: Clock.System.now()
                )
            }
            chatMessagesFlow.value = chatMessagesFlow.value + (bookingId to mapped)
        }
    }

    // =========================================================================
    // WALLET REPOSITORY (Saldo QRIS, Analisis Gabungan, & Pencairan Mitra)
    // =========================================================================

    override fun observeProviderRevenue(providerId: String): Flow<ProviderRevenueAnalytics> {
        if (!payoutsFlow.value.containsKey(providerId) && SupabaseClientProvider.isConfigured) {
            realtimeScope.launch {
                refreshPayoutHistory(providerId)
            }
        }
        return combine(providerBookingsFlow, payoutsFlow) { bookings, payoutsMap ->
            val providerBookings = bookings.filter { it.providerId == providerId }
            val providerPayouts = payoutsMap[providerId].orEmpty()
            calculateRevenueUseCase(providerBookings, providerPayouts)
        }
    }

    override fun observePayoutHistory(providerId: String): Flow<List<PayoutRequest>> {
        if (!payoutsFlow.value.containsKey(providerId) && SupabaseClientProvider.isConfigured) {
            realtimeScope.launch {
                refreshPayoutHistory(providerId)
            }
        }
        return payoutsFlow.map { map ->
            map[providerId].orEmpty().sortedByDescending { it.createdAt }
        }
    }

    override suspend fun requestPayout(
        providerId: String,
        amount: Int,
        channelType: PayoutChannelType,
        channelName: String,
        accountNumber: String,
        accountHolder: String
    ): AppResult<PayoutRequest> = withContext(ioDispatcher) {
        if (amount < BantuinConfig.MIN_PAYOUT_AMOUNT) {
            return@withContext AppResult.Error("Minimal penarikan dana adalah Rp20.000.")
        }
        val cleanChannel = channelName.trim()
        val cleanAccNumber = accountNumber.trim()
        val cleanAccHolder = accountHolder.trim()
        if (cleanChannel.isBlank() || cleanAccNumber.isBlank() || cleanAccHolder.isBlank()) {
            return@withContext AppResult.Error("Lengkapi data rekening atau e-wallet tujuan.")
        }

        try {
            val payoutId = client.postgrest.rpc(
                function = "request_provider_payout",
                parameters = RequestPayoutRpcParams(
                    amount = amount,
                    channelType = channelType.name,
                    channelName = cleanChannel,
                    accountNumber = cleanAccNumber,
                    accountHolder = cleanAccHolder
                )
            ).decodeAs<String>()

            refreshPayoutHistory(providerId)
            val created = payoutsFlow.value[providerId]?.find { it.id == payoutId } ?: PayoutRequest(
                id = payoutId,
                providerId = providerId,
                amount = amount,
                adminFee = BantuinConfig.PAYOUT_ADMIN_FEE,
                netAmount = (amount - BantuinConfig.PAYOUT_ADMIN_FEE).coerceAtLeast(0),
                channelType = channelType,
                channelName = cleanChannel,
                accountNumber = cleanAccNumber,
                accountHolder = cleanAccHolder,
                status = PayoutStatus.COMPLETED,
                createdAt = Clock.System.now()
            )
            AppResult.Success(created)
        } catch (e: Exception) {
            mapSupabaseError(e, "Gagal mengajukan pencairan dana")
        }
    }

    private suspend fun refreshPayoutHistory(providerId: String) {
        if (!SupabaseClientProvider.isConfigured) return
        runCatching {
            val dtoList = client.postgrest["payout_requests"]
                .select {
                    filter { eq("provider_id", providerId) }
                    order(column = "created_at", order = Order.DESCENDING)
                }
                .decodeList<PayoutRequestDto>()
            val items = dtoList.map { dto ->
                PayoutRequest(
                    id = dto.id,
                    providerId = dto.providerId,
                    amount = dto.amount,
                    adminFee = dto.adminFee,
                    netAmount = dto.netAmount,
                    channelType = runCatching { PayoutChannelType.valueOf(dto.channelType) }
                        .getOrDefault(PayoutChannelType.BANK),
                    channelName = dto.channelName,
                    accountNumber = dto.accountNumber,
                    accountHolder = dto.accountHolder,
                    status = runCatching { PayoutStatus.valueOf(dto.status) }
                        .getOrDefault(PayoutStatus.COMPLETED),
                    createdAt = parseInstantOrNull(dto.createdAt) ?: Clock.System.now()
                )
            }
            payoutsFlow.value = payoutsFlow.value + (providerId to items)
        }
    }

    // =========================================================================
    // ERROR MAPPING & HELPERS
    // =========================================================================

    internal fun mapSupabaseError(e: Throwable, defaultMessage: String): AppResult.Error {
        val raw = buildString {
            append(e.message.orEmpty())
            e.cause?.message?.let { append(" "); append(it) }
        }
        val mapped = when {
            raw.contains("SLOT_CONFLICT", ignoreCase = true) ||
                raw.contains("SLOT_LOCKED", ignoreCase = true) ||
                raw.contains("23P01", ignoreCase = true) ->
                "Slot baru saja diambil, pilih jam lain"
            raw.contains("LOCK_EXPIRED", ignoreCase = true) ->
                "Waktu penahanan slot habis, pilih jadwal lagi"
            raw.contains("EXPIRED", ignoreCase = true) ->
                "Pesanan sudah kedaluwarsa"
            raw.contains("INVALID_TRANSITION", ignoreCase = true) ||
                raw.contains("FORBIDDEN", ignoreCase = true) ->
                "Aksi tidak diizinkan"
            raw.contains("SELF_BOOKING", ignoreCase = true) ->
                "Tidak bisa memesan layanan sendiri"
            raw.contains("CASH_CONFIRMATION_REQUIRED", ignoreCase = true) ->
                "Konfirmasikan bahwa uang tunai sudah diterima dari pelanggan."
            raw.contains("QRIS_UNPAID", ignoreCase = true) ->
                "Pembayaran QRIS pelanggan belum lunas."
            raw.contains("MIN_PAYOUT_NOT_MET", ignoreCase = true) ->
                "Minimal penarikan dana adalah Rp20.000."
            raw.contains("INSUFFICIENT_BALANCE", ignoreCase = true) ->
                "Saldo siap cair tidak mencukupi."
            raw.contains("INVALID_PAYMENT_METHOD", ignoreCase = true) ->
                "Metode pembayaran tidak valid."
            raw.contains("INVALID_CHANNEL", ignoreCase = true) ||
                raw.contains("INVALID_ACCOUNT_DETAILS", ignoreCase = true) ->
                "Lengkapi data rekening atau e-wallet tujuan."
            raw.contains("UNAUTHENTICATED", ignoreCase = true) ||
                raw.contains("JWT", ignoreCase = true) ->
                "Sesi login berakhir, silakan masuk kembali"
            raw.contains("NOT_FOUND", ignoreCase = true) ->
                "Data tidak ditemukan"
            raw.contains("timeout", ignoreCase = true) ||
                raw.contains("Unable to resolve host", ignoreCase = true) ||
                raw.contains("connect", ignoreCase = true) ->
                "Koneksi ke server terputus. Periksa internet Anda lalu coba lagi."
            else -> defaultMessage
        }
        return AppResult.Error(mapped, e)
    }

    private suspend fun enrichBookingDtos(dtoList: List<BookingDto>): List<BookingItem> {
        if (dtoList.isEmpty()) return emptyList()

        val profileIds = (dtoList.map { it.customerId } + dtoList.map { it.providerId }).distinct()
        val providerIds = dtoList.map { it.providerId }.distinct()
        val serviceIds = dtoList.map { it.serviceId }.distinct()
        val bookingIds = dtoList.map { it.id }.distinct()

        val profilesMap = runCatching {
            client.postgrest["profiles"]
                .select { filter { isIn("id", profileIds) } }
                .decodeList<ProfileDto>()
                .associateBy { it.id }
        }.getOrDefault(emptyMap())

        val providerProfilesMap = runCatching {
            client.postgrest["provider_profiles"]
                .select { filter { isIn("user_id", providerIds) } }
                .decodeList<ProviderProfileDto>()
                .associateBy { it.userId }
        }.getOrDefault(emptyMap())

        val servicesMap = runCatching {
            client.postgrest["services"]
                .select { filter { isIn("id", serviceIds) } }
                .decodeList<ServiceDto>()
                .associateBy { it.id }
        }.getOrDefault(emptyMap())

        val reviewsMap = runCatching {
            client.postgrest["reviews"]
                .select { filter { isIn("booking_id", bookingIds) } }
                .decodeList<ReviewDto>()
                .associateBy { it.bookingId }
        }.getOrDefault(emptyMap())

        return dtoList.map { dto ->
            val customerProfile = profilesMap[dto.customerId]
            val providerProfile = profilesMap[dto.providerId]
            val providerLocationDto = providerProfilesMap[dto.providerId]
            val serviceDto = servicesMap[dto.serviceId]
            val reviewDto = reviewsMap[dto.id]

            val startInstant = parseInstantOrNull(dto.startAt)
            val endInstant = parseInstantOrNull(dto.endAt)
            val startMin = startInstant?.let { TimeUtils.toWibMinutesOfDay(it) } ?: 480
            val endMin = serviceDto?.durationMin?.let { startMin + it }
            ?: endInstant?.let { TimeUtils.toWibMinutesOfDay(it) }
            ?: (startMin + 60)
            val status = try {
                BookingStatus.valueOf(dto.status)
            } catch (_: Exception) {
                BookingStatus.PENDING
            }

            BookingItem(
                id = dto.id,
                customerId = dto.customerId,
                customerName = customerProfile?.fullName ?: "Pelanggan",
                providerId = dto.providerId,
                providerName = providerProfile?.fullName ?: "Mitra Bantuin",
                serviceName = serviceDto?.name ?: "Layanan Bantuin",
                scheduledDate = startInstant?.let { TimeUtils.formatInstantToDate(it) } ?: dto.startAt.take(10),
                scheduledDateIso = startInstant?.let { TimeUtils.toWibDateIso(it) } ?: dto.startAt.take(10),
                scheduledTime = String.format(
                    Locale.US,
                    "%02d:%02d - %02d:%02d WIB",
                    startMin / 60,
                    startMin % 60,
                    endMin / 60,
                    endMin % 60
                ),
                totalCost = dto.totalCost,
                status = status,
                address = String.format(Locale.US, "%.4f, %.4f", dto.customerLat, dto.customerLng),
                createdAt = parseInstantOrNull(dto.createdAt) ?: Clock.System.now(),
                notes = dto.notes,
                review = reviewDto?.let { r ->
                    ReviewItem(
                        id = r.id,
                        bookingId = r.bookingId,
                        customerName = customerProfile?.fullName ?: "Pelanggan",
                        rating = r.rating,
                        comment = r.comment,
                        tags = emptyList(),
                        createdAtText = r.createdAt?.take(10) ?: "Baru saja"
                    )
                },
                expiresAt = parseInstantOrNull(dto.expiresAt),
                startedAt = parseInstantOrNull(dto.startedAt),
                finishedAt = parseInstantOrNull(dto.finishedAt),
                customerLat = dto.customerLat,
                customerLng = dto.customerLng,
                providerLocation = providerLocationDto?.let { GeoPoint(it.lat, it.lng) },
                providerPhone = providerProfile?.phone,
                customerPhone = customerProfile?.phone,
                paymentMethod = runCatching { PaymentMethod.valueOf(dto.paymentMethod) }
                    .getOrDefault(PaymentMethod.CASH),
                paymentStatus = runCatching { PaymentStatus.valueOf(dto.paymentStatus) }
                    .getOrDefault(PaymentStatus.UNPAID),
                completionNote = dto.completionNote
            )
        }
    }

    private fun mapProviderCandidate(
        pp: ProviderProfileDto,
        profile: ProfileDto,
        minFare: Int?,
        reviewCount: Int? = null
    ): ProviderCandidate {
        return ProviderCandidate(
            providerId = pp.userId,
            name = profile.fullName,
            avatarUrl = profile.avatarUrl,
            location = GeoPoint(pp.lat, pp.lng),
            address = pp.address ?: "Malang, Jawa Timur",
            bio = pp.bio ?: "Mitra jasa Bantuin",
            ratingAvg = pp.ratingAvg,
            reviewCount = reviewCount ?: pp.txCount30d,
            completionRate = pp.completionRate,
            flagAnomaly = pp.flagAnomaly,
            badge = when (pp.badge) {
                "TRUSTED" -> ProviderBadge.TRUSTED
                "NEW" -> ProviderBadge.NEW
                else -> ProviderBadge.NONE
            },
            minBaseFare = minFare,
            responseTimeText = when {
                pp.trustScore >= 90.0 -> "< 5 mnt"
                pp.trustScore >= 80.0 -> "~10 mnt"
                else -> "~20 mnt"
            }
        )
    }

    private fun categorySubtitle(categoryId: Int): String = when (categoryId) {
        1 -> "Korslet, saklar, panel & MCB"
        2 -> "Cuci AC, perbaikan kulkas & TV"
        3 -> "Atap bocor, kran air, cat dinding"
        4 -> "Deep clean kamar mandi & kos"
        5 -> "Pindahan kos, perabot & lemari"
        6 -> "Matematika, sains, bahasa & les"
        else -> "Layanan jasa profesional"
    }

    private suspend fun loadUserProfile(authUser: UserInfo, fallbackEmail: String): UserProfile {
        val profileDto = try {
            client.postgrest["profiles"]
                .select { filter { eq("id", authUser.id) } }
                .decodeList<ProfileDto>()
                .firstOrNull()
        } catch (_: Exception) {
            null
        }

        if (profileDto != null) {
            val defaultLoc = if (profileDto.defaultLat != null && profileDto.defaultLng != null) {
                GeoPoint(profileDto.defaultLat, profileDto.defaultLng)
            } else {
                null
            }
            return UserProfile(
                id = profileDto.id,
                fullName = profileDto.fullName,
                email = authUser.email ?: fallbackEmail.trim(),
                phone = profileDto.phone.orEmpty(),
                roles = profileDto.roles.toSet().ifEmpty { setOf("customer") },
                avatarUrl = profileDto.avatarUrl,
                defaultAddress = profileDto.defaultAddress,
                defaultLocation = defaultLoc
            )
        }

        val metadata = authUser.userMetadata
        val name = metadata?.get("full_name")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: fallbackEmail.substringBefore("@")
        val phone = metadata?.get("phone")?.jsonPrimitive?.contentOrNull.orEmpty()
        val roles = metadata?.get("roles")?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: setOf("customer")

        return UserProfile(
            id = authUser.id,
            fullName = name,
            email = authUser.email ?: fallbackEmail.trim(),
            phone = phone,
            roles = roles
        )
    }

    private fun preferredRole(user: UserProfile): String =
        if ("provider" in user.roles && "customer" !in user.roles) "provider" else "customer"

    private fun logDebug(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (_: Throwable) {
        }
    }

    private fun logError(tag: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        } catch (_: Throwable) {
        }
    }

    private fun authErrorMessage(e: Exception, action: String): String {
        val code = (e as? AuthRestException)?.errorCode
        return when (code) {
            AuthErrorCode.InvalidCredentials -> "Email atau kata sandi salah."
            AuthErrorCode.EmailNotConfirmed -> "Email belum dikonfirmasi. Cek kotak masuk Anda."
            AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> "Email sudah terdaftar."
            AuthErrorCode.WeakPassword -> "Kata sandi terlalu lemah. Gunakan minimal 8 karakter."
            AuthErrorCode.OverRequestRateLimit -> "Terlalu banyak percobaan. Coba lagi beberapa saat lagi."
            else -> when {
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Koneksi ke server habis waktu. Periksa internet Anda lalu coba lagi."
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    e.message?.contains("connect", ignoreCase = true) == true ->
                    "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
                else -> "Gagal $action. Periksa email, kata sandi, dan koneksi internet Anda."
            }
        }
    }

    private fun parseInstantOrNull(iso: String?): Instant? {
        if (iso.isNullOrBlank()) return null
        return try {
            Instant.parse(iso)
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseIsoToWibMinutesOnDate(iso: String, dateIso: String): Int {
        return runCatching {
            val instant = Instant.parse(iso)
            val dayStart = Instant.parse(TimeUtils.toIsoString(dateIso, 0))
            val diffMin = ((instant.epochSeconds - dayStart.epochSeconds) / 60L).toInt()
            diffMin.coerceIn(0, 1440)
        }.getOrDefault(0)
    }

    private fun parseIsoToWibMinutes(isoString: String): Int {
        return try {
            val instant = Instant.parse(isoString)
            TimeUtils.toWibMinutesOfDay(instant)
        } catch (_: Exception) {
            480
        }
    }

    private fun parseTimeToMin(timeStr: String): Int {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }

    private fun formatMinToTime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return String.format(Locale.US, "%02d:%02d:00", h, m)
    }
}

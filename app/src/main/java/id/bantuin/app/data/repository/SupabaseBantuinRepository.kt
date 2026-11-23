package id.bantuin.app.data.repository

import android.util.Log
import id.bantuin.app.core.result.AppResult
import id.bantuin.app.core.session.UserSessionStore
import id.bantuin.app.core.util.TimeUtils
import id.bantuin.app.core.config.BantuinConfig
import id.bantuin.app.data.remote.ProviderTrustDataSource
import id.bantuin.app.data.remote.SupabaseClientProvider
import id.bantuin.app.domain.usecase.trust.TrustScoreUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.days
import id.bantuin.app.data.remote.dto.*
import id.bantuin.app.domain.model.*
import id.bantuin.app.domain.repository.BantuinRepository
import id.bantuin.app.domain.repository.UserProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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

class SupabaseBantuinRepository(
    private val client: SupabaseClient = SupabaseClientProvider.client,
    private val sessionStore: UserSessionStore,
    private val fallback: FakeBantuinRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BantuinRepository {

    private val trustSource = ProviderTrustDataSource(client)
    private val trustScore = TrustScoreUseCase()
    private val metricRevisions = MutableStateFlow(0L)
    override val trustChanges = metricRevisions.asStateFlow()

    private val customerBookingsFlow = MutableStateFlow<List<BookingItem>>(emptyList())

    companion object {
        private const val LOG_TAG_LOGIN = "LOGIN"
    }

    // =========================================================================
    // CATALOG REPOSITORY (CRUD-1, CRUD-2, CRUD-3, F1)
    // =========================================================================

    override suspend fun categories(): AppResult<List<ServiceCategory>> = withContext(ioDispatcher) {
        try {
            val dtoList = client.postgrest["service_categories"]
                .select()
                .decodeList<ServiceCategoryDto>()
            val list = dtoList.map { dto ->
                ServiceCategory(
                    id = dto.id,
                    name = dto.name,
                    iconKey = dto.icon,
                    defaultDurationMin = dto.defaultDurationMin,
                    description = ""
                )
            }
            AppResult.Success(list)
        } catch (e: Exception) {
            fallback?.categories() ?: AppResult.Error(e.message ?: "Gagal memuat kategori", e)
        }
    }

    override suspend fun candidates(categoryId: Int): AppResult<List<ProviderCandidate>> = withContext(ioDispatcher) {
        try {
            val servicesDto = client.postgrest["services"]
                .select {
                    filter {
                        eq("category_id", categoryId)
                        eq("is_active", true)
                    }
                }
                .decodeList<ServiceDto>()

            val providerIds = servicesDto.map { it.providerId }.distinct()
            if (providerIds.isEmpty()) {
                return@withContext fallback?.candidates(categoryId) ?: AppResult.Success(emptyList())
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

            val inputs = trustSource.inputs(ppDtoList.map { it.userId }, BantuinConfig.TRUST_WINDOW_DAYS)
                .associateBy { it.providerId }
            val assessments = withContext(Dispatchers.Default) {
                inputs.mapValues { (_, input) -> trustScore(input.toDomain()) }
            }
            val candidates = ppDtoList.mapNotNull { pp ->
                val profile = profilesDto.find { it.id == pp.userId } ?: return@mapNotNull null
                val userAvail = availDtoList.filter { it.providerId == pp.userId }
                val isAvailableAnyDay = userAvail.isNotEmpty() && userAvail.any { it.mode != "OFF" }
                if (!isAvailableAnyDay) return@mapNotNull null

                val pServices = servicesDto.filter { it.providerId == pp.userId }
                val minFare = pServices.minOfOrNull { it.baseFare } ?: 50000

                val assessment = assessments[pp.userId]
                ProviderCandidate(
                    providerId = pp.userId,
                    name = profile.fullName,
                    avatarUrl = profile.avatarUrl,
                    location = GeoPoint(pp.lat, pp.lng),
                    address = pp.address ?: "Malang, Jawa Timur",
                    bio = pp.bio ?: "Teknisi profesional Bantuin",
                    ratingAvg = assessment?.ratingAvg,
                    reviewCount = inputs[pp.userId]?.ratingCount ?: 0,
                    completionRate = assessment?.completionRate,
                    flagAnomaly = assessment?.flagAnomaly ?: true,
                    badge = assessment?.badge ?: ProviderBadge.NONE,
                    minBaseFare = minFare,
                    responseTimeText = when {
                        pp.trustScore >= 90.0 -> "< 5 mnt"
                        pp.trustScore >= 80.0 -> "~10 mnt"
                        else -> "~20 mnt"
                    }
                )
            }
            AppResult.Success(candidates)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            fallback?.candidates(categoryId) ?: AppResult.Error(e.message ?: "Gagal memuat mitra", e)
        }
    }

    override suspend fun servicesOf(providerId: String): AppResult<List<ServiceItem>> = withContext(ioDispatcher) {
        try {
            val listDto = client.postgrest["services"]
                .select {
                    filter {
                        eq("provider_id", providerId)
                        eq("is_active", true)
                    }
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
            fallback?.servicesOf(providerId) ?: AppResult.Error(e.message ?: "Gagal memuat layanan", e)
        }
    }

    override suspend fun availability(providerId: String): AppResult<List<DayAvailability>> = withContext(ioDispatcher) {
        try {
            val listDto = client.postgrest["availability"]
                .select {
                    filter {
                        eq("provider_id", providerId)
                    }
                }
                .decodeList<AvailabilityDto>()
            val list = listDto.map { dto ->
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
            AppResult.Success(list)
        } catch (e: Exception) {
            fallback?.availability(providerId) ?: AppResult.Error(e.message ?: "Gagal memuat ketersediaan", e)
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
            fallback?.saveAvailability(providerId, days) ?: AppResult.Error(e.message ?: "Gagal menyimpan jadwal", e)
        }
    }

    override suspend fun upsertService(service: ServiceItem): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            val dto = ServiceDto(
                id = if (service.id.isBlank()) UUID.randomUUID().toString() else service.id,
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
            fallback?.upsertService(service) ?: AppResult.Error(e.message ?: "Gagal menyimpan katalog", e)
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
            fallback?.deleteService(serviceId) ?: AppResult.Error(e.message ?: "Gagal menghapus layanan", e)
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
            AppResult.Success(Unit)
        } catch (e: Exception) {
            fallback?.updateProviderProfile(providerId, name, bio, location, address, phone)
                ?: AppResult.Error(e.message ?: "Gagal memperbarui profil", e)
        }
    }

    // =========================================================================
    // BOOKING REPOSITORY (F2, F3 & Realtime RPC)
    // =========================================================================

    override suspend fun busyBlocks(providerId: String, dateIso: String): AppResult<List<TimeBlock>> = withContext(ioDispatcher) {
        try {
            val fromIso = "${dateIso}T00:00:00+07:00"
            val toIso = "${dateIso}T23:59:59+07:00"

            val result = client.postgrest.rpc(
                function = "get_busy_blocks",
                parameters = mapOf(
                    "p_provider" to providerId,
                    "p_from" to fromIso,
                    "p_to" to toIso
                )
            ).decodeList<BusyBlockDto>()

            val blocks = result.map { dto ->
                val startMin = parseIsoToWibMinutes(dto.startAt)
                val endMin = parseIsoToWibMinutes(dto.endAt)
                TimeBlock(startMin, endMin)
            }
            AppResult.Success(blocks)
        } catch (e: Exception) {
            fallback?.busyBlocks(providerId, dateIso)
                ?: AppResult.Error(e.message ?: "Gagal mengecek jadwal bentrok", e)
        }
    }

    override suspend fun acquireLock(providerId: String, startMin: Int, endMin: Int): AppResult<LockInfo> = withContext(ioDispatcher) {
        try {
            val todayIso = TimeUtils.todayIso()
            val startIso = TimeUtils.toIsoString(todayIso, startMin)
            val endIso = TimeUtils.toIsoString(todayIso, endMin)

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
                expiresAtMillis = Clock.System.now().toEpochMilliseconds() + 600_000L
            )
            AppResult.Success(lockInfo)
        } catch (e: Exception) {
            fallback?.acquireLock(providerId, startMin, endMin)
                ?: AppResult.Error(e.message ?: "Slot sedang dikunci pengguna lain", e)
        }
    }

    override suspend fun releaseLock(lockId: String): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.postgrest.rpc("release_slot_lock")
            AppResult.Success(Unit)
        } catch (e: Exception) {
            fallback?.releaseLock(lockId) ?: AppResult.Error(e.message ?: "Gagal melepas kunci slot", e)
        }
    }

    override suspend fun createBooking(draft: OrderDraft): AppResult<String> = withContext(ioDispatcher) {
        try {
            val lockId = draft.lockInfo?.lockId ?: UUID.randomUUID().toString()
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
                    notes = draft.notes
                )
            ).decodeAs<String>()

            myBookingsCustomer()
            AppResult.Success(bookingId)
        } catch (e: Exception) {
            fallback?.createBooking(draft) ?: AppResult.Error(e.message ?: "Gagal membuat pesanan", e)
        }
    }

    override suspend fun updateStatus(bookingId: String, newStatus: BookingStatus): AppResult<Unit> = withContext(ioDispatcher) {
        if (fallback?.hasLocalBooking(bookingId) == true) {
            return@withContext localTrustWrite { fallback.updateStatus(bookingId, newStatus) }
        }
        try {
            client.postgrest.rpc(
                function = "update_booking_status",
                parameters = UpdateBookingStatusRpcParams(
                    bookingId = bookingId,
                    newStatus = newStatus.name
                )
            )
            metricRevisions.update { it + 1 }
            myBookingsCustomer()
            myBookingsProvider()
            AppResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Gagal memperbarui status", e)
        }
    }

    override suspend fun myBookingsCustomer(): AppResult<List<BookingItem>> = withContext(ioDispatcher) {
        try {
            val dtoList = client.postgrest["bookings"]
                .select()
                .decodeList<BookingDto>()

            val items = dtoList.map { dto ->
                mapDtoToBookingItem(dto)
            }
            customerBookingsFlow.value = items
            AppResult.Success(items)
        } catch (e: Exception) {
            fallback?.myBookingsCustomer() ?: AppResult.Error(e.message ?: "Gagal memuat pesanan", e)
        }
    }

    override suspend fun myBookingsProvider(): AppResult<List<BookingItem>> = withContext(ioDispatcher) {
        try {
            val dtoList = client.postgrest["bookings"]
                .select()
                .decodeList<BookingDto>()
            val items = dtoList.map { dto ->
                mapDtoToBookingItem(dto)
            }
            AppResult.Success(items)
        } catch (e: Exception) {
            fallback?.myBookingsProvider() ?: AppResult.Error(e.message ?: "Gagal memuat antrean mitra", e)
        }
    }

    override fun observeBooking(bookingId: String): Flow<BookingItem?> {
        if (fallback?.hasLocalBooking(bookingId) == true) return fallback.observeBooking(bookingId)
        return customerBookingsFlow.map { list -> list.find { it.id == bookingId } }
    }

    override fun observeCustomerBookings(): Flow<List<BookingItem>> = customerBookingsFlow.asStateFlow()

    override suspend fun bookingDetails(bookingId: String): AppResult<BookingItem?> {
        if (fallback?.hasLocalBooking(bookingId) == true) return fallback.bookingDetails(bookingId)
        return trustRequest("Gagal memuat pesanan") {
            val dto = client.postgrest["bookings"].select(Columns.raw("*,reviews(*),booking_outcomes(*)")) {
                filter { eq("id", bookingId) }
            }.decodeList<BookingDto>().firstOrNull() ?: return@trustRequest null
            val item = mapDtoToBookingItem(dto).copy(review = dto.review?.toDomain(), outcome = dto.outcome?.toDomain())
            customerBookingsFlow.update { items -> items.filterNot { it.id == bookingId } + item }
            item
        }
    }

    override suspend fun bookingEvents(bookingId: String): AppResult<List<BookingEvent>> {
        if (fallback?.hasLocalBooking(bookingId) == true) return fallback.bookingEvents(bookingId)
        return trustRequest("Gagal memuat riwayat status") {
            trustSource.bookingEvents(bookingId).mapNotNull { event ->
                val status = BookingStatus.entries.find { it.name == event.status } ?: return@mapNotNull null
                val date = event.createdAt?.let(Instant::parse) ?: return@mapNotNull null
                BookingEvent(status, date)
            }
        }
    }

    override suspend fun submitReview(
        bookingId: String, providerId: String, rating: Int, comment: String?, tags: List<String>
    ): AppResult<Unit> {
        if (fallback?.hasLocalBooking(bookingId) == true) {
            return localTrustWrite { fallback.submitReview(bookingId, providerId, rating, comment, tags) }
        }
        return trustWrite("Gagal menyimpan ulasan") {
            trustSource.saveReview(bookingId, rating, comment, tags, null)
        }
    }

    override suspend fun confirmCompletion(
        bookingId: String, providerId: String, status: CompletionStatus,
        rating: Int, comment: String?, tags: List<String>
    ): AppResult<Unit> {
        if (fallback?.hasLocalBooking(bookingId) == true) {
            return localTrustWrite { fallback.confirmCompletion(bookingId, providerId, status, rating, comment, tags) }
        }
        return trustWrite("Gagal menyimpan konfirmasi") {
            trustSource.saveReview(bookingId, rating, comment, tags, status)
        }
    }

    override suspend fun deleteReview(bookingId: String): AppResult<Unit> {
        if (fallback?.hasLocalBooking(bookingId) == true) return localTrustWrite { fallback.deleteReview(bookingId) }
        return trustWrite("Gagal menghapus ulasan") { trustSource.deleteReview(bookingId) }
    }

    override suspend fun deleteTransactionOutcome(bookingId: String): AppResult<Unit> {
        if (fallback?.hasLocalBooking(bookingId) == true) {
            return localTrustWrite { fallback.deleteTransactionOutcome(bookingId) }
        }
        return trustWrite("Gagal menghapus konfirmasi") { trustSource.deleteOutcome(bookingId) }
    }

    override suspend fun reviewsOf(providerId: String): AppResult<List<ReviewItem>> {
        if (fallback?.hasDemoProvider(providerId) == true) return fallback.reviewsOf(providerId)
        return trustRequest("Gagal memuat ulasan") { trustSource.reviewsOf(providerId).map { it.toDomain() } }
    }

    override suspend fun trustInput(providerId: String, sinceDays: Int): AppResult<TrustInput> {
        if (fallback?.hasDemoProvider(providerId) == true) return fallback.trustInput(providerId, sinceDays)
        return trustRequest("Gagal memuat metrik") {
            trustSource.inputs(listOf(providerId), sinceDays).firstOrNull()?.toDomain() ?: TrustInput()
        }
    }

    override suspend fun transactionHistory(
        providerId: String, sinceDays: Int, status: CompletionStatus?
    ): AppResult<List<TransactionOutcome>> {
        if (fallback?.hasDemoProvider(providerId) == true) return fallback.transactionHistory(providerId, sinceDays, status)
        return trustRequest("Gagal memuat riwayat transaksi") {
            val now = Clock.System.now()
            val since = now - sinceDays.coerceIn(1, BantuinConfig.TRUST_WINDOW_DAYS).days
            trustSource.outcomes(providerId, since, now, status).map { it.toDomain() }
        }
    }

    override suspend fun refreshTrust(providerId: String): AppResult<Unit> {
        if (fallback?.hasDemoProvider(providerId) == true) {
            return localTrustWrite { fallback.refreshTrust(providerId) }
        }
        if (sessionStore.currentUserValue?.id != providerId) return AppResult.Error("Sesi mitra diperlukan")
        return trustWrite("Gagal menghitung ulang metrik") { trustSource.refresh() }
    }

    override suspend fun history(providerId: String): AppResult<List<MetricSnapshot>> {
        if (fallback?.hasDemoProvider(providerId) == true) return fallback.history(providerId)
        return trustRequest("Gagal memuat grafik performa") {
            val since = Clock.System.now() - BantuinConfig.TRUST_WINDOW_DAYS.days
            trustSource.snapshots(providerId, since).map { snapshot ->
                MetricSnapshot(
                    label = snapshot.computedAt?.take(10).orEmpty(),
                    trustScore = snapshot.trustScore,
                    ratingScore = snapshot.ratingScore ?: 0.0,
                    onTimeRate = snapshot.onTimeRate ?: 0.0,
                    cancelRate = snapshot.cancelRate ?: 0.0
                )
            }
        }
    }

    private suspend fun <T> trustRequest(error: String, request: suspend () -> T): AppResult<T> =
        withContext(ioDispatcher) {
            try {
                AppResult.Success(request())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppResult.Error(error, e)
            }
        }

    private suspend fun trustWrite(error: String, request: suspend () -> Unit): AppResult<Unit> =
        trustRequest(error) {
            request()
            metricRevisions.update { it + 1 }
        }

    private suspend fun localTrustWrite(request: suspend () -> AppResult<Unit>): AppResult<Unit> =
        request().also { if (it is AppResult.Success) metricRevisions.update { revision -> revision + 1 } }

    private fun ReviewDto.toDomain() = ReviewItem(
        id = id,
        bookingId = bookingId,
        customerName = "Pelanggan Terverifikasi",
        rating = rating,
        comment = comment,
        tags = tags,
        createdAtText = createdAt?.take(10) ?: "Baru saja",
        providerId = providerId,
        createdAt = createdAt?.let(Instant::parse)
    )

    private fun BookingOutcomeDto.toDomain() = TransactionOutcome(
        bookingId, providerId, CompletionStatus.valueOf(status), Instant.parse(resolvedAt), customerConfirmed
    )

    private fun ProviderTrustInputDto.toDomain() =
        TrustInput(onTimeCount, lateCount, cancelledCount, ratingCount, ratingTotal, otherCount)

    // =========================================================================
    // AUTH REPOSITORY (CRUD-0 Multi-Role)
    // =========================================================================

    override suspend fun signIn(email: String, pass: String): AppResult<String> = withContext(ioDispatcher) {
        val normalizedEmail = email.trim()
        try {
            client.auth.awaitInitialization()
            logLoginDebug("Mencoba login dengan email: $normalizedEmail")
            client.auth.signInWith(Email) {
                this.email = normalizedEmail
                this.password = pass
            }
            logLoginDebug("Login berhasil")

            val authUser = client.auth.currentUserOrNull()
                ?: run {
                    logLoginError("Login gagal: sesi pengguna tidak ditemukan setelah signInWith")
                    return@withContext AppResult.Error("Gagal masuk: sesi pengguna tidak ditemukan.")
                }
            logLoginDebug("Auth user: id=${authUser.id}, email=${authUser.email}")

            val user = loadUserProfile(authUser, email)
            sessionStore.setActiveRole(preferredRole(user))
            sessionStore.saveSession(user)
            logLoginDebug("Sesi tersimpan: ${user.fullName}, roles=${user.roles}, activeRole=${preferredRole(user)}")
            AppResult.Success(user.id)
        } catch (e: Exception) {
            // Log diagnostik: jangan pernah mencatat password.
            logLoginError("Login gagal: ${e.message}", e)
            if (e is AuthRestException) {
                logLoginError(
                    "Detail login gagal | exception=${e::class.java.simpleName} | " +
                        "code=${e.errorCode?.value ?: e.error} | " +
                        "description=${e.errorDescription} | " +
                        "http=${e.response.status.value}"
                )
            }
            // Jangan pernah fallback ke akun demo di sini. Fallback pada autentikasi
            // membuat kredensial salah seolah-olah berhasil masuk sebagai Rani Wijaya.
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
            val initialRole = if (isProvider) "provider" else "customer"
            client.auth.awaitInitialization()
            client.auth.signUpWith(Email) {
                this.email = email.trim()
                this.password = pass
                data = buildJsonObject {
                    put("full_name", name.trim())
                    if (phone.isNotBlank()) put("phone", phone.trim())
                    putJsonArray("roles") { add(initialRole) }
                }
            }

            val authUser = client.auth.currentUserOrNull()
                ?: return@withContext AppResult.Error(
                    "Pendaftaran berhasil. Silakan konfirmasi email Anda sebelum masuk."
                )

            val user = loadUserProfile(authUser, email)
            sessionStore.setActiveRole(preferredRole(user))
            sessionStore.saveSession(user)
            AppResult.Success(user.id)
        } catch (e: Exception) {
            AppResult.Error(authErrorMessage(e, "mendaftar"), e)
        }
    }

    override suspend fun signOut(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            client.auth.signOut()
            sessionStore.clearSession()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            sessionStore.clearSession()
            AppResult.Success(Unit)
        }
    }

    override fun activeRole(): Flow<String> = sessionStore.activeRole

    override suspend fun setActiveRole(role: String) {
        sessionStore.setActiveRole(role)
    }

    override fun currentUser(): Flow<UserProfile?> = sessionStore.currentUser

    // =========================================================================
    // AUTH HELPERS
    // =========================================================================

    /**
     * Memuat profil asli pengguna dari tabel public.profiles. Metadata auth hanya
     * dipakai sebagai cadangan bila baris profil belum tersedia (misalnya trigger
     * handle_new_user belum berjalan).
     */
    private suspend fun loadUserProfile(authUser: UserInfo, fallbackEmail: String): UserProfile {
        val profileDto = try {
            client.postgrest["profiles"]
                .select {
                    filter {
                        eq("id", authUser.id)
                    }
                }
                .decodeList<ProfileDto>()
                .firstOrNull()
        } catch (_: Exception) {
            null
        }

        if (profileDto != null) {
            return UserProfile(
                id = profileDto.id,
                fullName = profileDto.fullName,
                email = authUser.email ?: fallbackEmail.trim(),
                phone = profileDto.phone.orEmpty(),
                roles = profileDto.roles.toSet().ifEmpty { setOf("customer") },
                avatarUrl = profileDto.avatarUrl
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

    /**
     * Pembungkus Log agar repository tetap aman dipakai dari local JVM unit test,
     * di mana android.util.Log merupakan stub yang melempar exception.
     */
    private fun logLoginDebug(message: String) {
        try {
            Log.d(LOG_TAG_LOGIN, message)
        } catch (_: Throwable) {
        }
    }

    private fun logLoginError(message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.e(LOG_TAG_LOGIN, message, throwable)
            } else {
                Log.e(LOG_TAG_LOGIN, message)
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

    // =========================================================================
    // HELPER MAPPERS
    // =========================================================================

    private fun mapDtoToBookingItem(dto: BookingDto): BookingItem {
        val startMin = parseIsoToWibMinutes(dto.startAt)
        val endMin = parseIsoToWibMinutes(dto.endAt)
        val status = try {
            BookingStatus.valueOf(dto.status)
        } catch (_: Exception) {
            BookingStatus.PENDING
        }

        return BookingItem(
            id = dto.id,
            customerId = dto.customerId,
            customerName = "Pelanggan Demo",
            providerId = dto.providerId,
            providerName = "Mitra Bantuin",
            serviceName = "Layanan Pemesanan",
            scheduledDate = dto.startAt.take(10),
            scheduledTime = String.format(Locale.US, "%02d:%02d - %02d:%02d WIB", startMin / 60, startMin % 60, endMin / 60, endMin % 60),
            totalCost = dto.totalCost,
            status = status,
            address = "Malang, Jawa Timur",
            createdAt = try { dto.createdAt?.let { Instant.parse(it) } ?: Clock.System.now() } catch (_: Exception) { Clock.System.now() },
            notes = dto.notes,
            review = null
        )
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

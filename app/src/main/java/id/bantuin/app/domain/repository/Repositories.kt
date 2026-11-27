package id.bantuin.app.domain.repository

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingEvent
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.ChatMessage
import id.bantuin.app.domain.model.ChatThreadSummary
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.model.MetricSnapshot
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.PayoutChannelType
import id.bantuin.app.domain.model.PayoutRequest
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ProviderRevenueAnalytics
import id.bantuin.app.domain.model.ReviewItem
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.model.TimeBlock
import id.bantuin.app.domain.model.CompletionOutcome
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import id.bantuin.app.domain.model.WalletTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface CatalogRepository {
    suspend fun categories(): AppResult<List<ServiceCategory>>
    suspend fun candidates(categoryId: Int): AppResult<List<ProviderCandidate>>
    suspend fun providerProfile(providerId: String): AppResult<ProviderCandidate?>
    suspend fun servicesOf(providerId: String): AppResult<List<ServiceItem>>
    suspend fun availability(providerId: String): AppResult<List<DayAvailability>>
    suspend fun saveAvailability(providerId: String, days: List<DayAvailability>): AppResult<Unit>
    suspend fun upsertService(service: ServiceItem): AppResult<Unit>
    suspend fun deleteService(serviceId: String): AppResult<Unit>
    suspend fun updateProviderProfile(
        providerId: String,
        name: String,
        bio: String,
        location: GeoPoint,
        address: String,
        phone: String
    ): AppResult<Unit>
    fun observeAcceptingOrders(providerId: String): Flow<Boolean>
    suspend fun setAcceptingOrders(providerId: String, accepting: Boolean): AppResult<Unit>
}

interface BookingRepository {
    suspend fun busyBlocks(providerId: String, dateIso: String): AppResult<List<TimeBlock>>
    suspend fun createBooking(draft: OrderDraft): AppResult<String>
    suspend fun updateStatus(bookingId: String, newStatus: BookingStatus): AppResult<Unit>
    suspend fun confirmQrisPayment(bookingId: String): AppResult<Unit>
    suspend fun completeBooking(
        bookingId: String,
        completionNote: String? = null,
        cashCollectedConfirmed: Boolean = false
    ): AppResult<Unit>
    suspend fun myBookingsCustomer(): AppResult<List<BookingItem>>
    suspend fun myBookingsProvider(): AppResult<List<BookingItem>>
    fun observeBooking(bookingId: String): Flow<BookingItem?>
    fun observeCustomerBookings(): Flow<List<BookingItem>>
    fun observeProviderBookings(): Flow<List<BookingItem>>
    suspend fun events(bookingId: String): AppResult<List<BookingEvent>>
    suspend fun expirePending(): AppResult<Unit>
}

interface ReviewRepository {
    suspend fun submitReview(bookingId: String, providerId: String, rating: Int, comment: String?,
        tags: List<String>, completion: CompletionOutcome? = null
    ): AppResult<Unit>
    suspend fun deleteReview(bookingId: String): AppResult<Unit>
    suspend fun reviewsOf(providerId: String): AppResult<List<ReviewItem>>
}

interface MetricsRepository {
    val trustChanges: Flow<Long> get() = flowOf(0L)
    suspend fun trustInput(providerId: String, sinceDays: Int): AppResult<TrustInput>
    suspend fun apply(providerId: String, result: TrustResult): AppResult<Unit>
    suspend fun history(providerId: String): AppResult<List<MetricSnapshot>>
}

interface AuthRepository {
    suspend fun signIn(email: String, pass: String): AppResult<String>
    suspend fun signUp(name: String, email: String, pass: String, phone: String, isProvider: Boolean): AppResult<String>
    suspend fun signOut(): AppResult<Unit>
    fun activeRole(): Flow<String> // "customer" or "provider"
    suspend fun setActiveRole(role: String)
    fun currentUser(): Flow<UserProfile?>
    suspend fun enableProviderRole(
        address: String = "Malang, Jawa Timur",
        location: GeoPoint = GeoPoint(-7.9797, 112.6304),
        bio: String = ""
    ): AppResult<Unit>
    suspend fun updateCustomerProfile(
        fullName: String,
        phone: String,
        defaultAddress: String,
        defaultLocation: GeoPoint
    ): AppResult<UserProfile>
}

interface ChatRepository {
    fun observeChatThreads(role: String): Flow<List<ChatThreadSummary>>
    fun observeMessages(bookingId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(bookingId: String, text: String): AppResult<Unit>

    /** Error terakhir saat memuat pesan, dipakai UI untuk tombol coba lagi. */
    fun observeChatError(bookingId: String): Flow<String?> = flowOf(null)

    /** Paksa muat ulang pesan setelah gagal. */
    suspend fun retryMessages(bookingId: String): AppResult<Unit> = AppResult.Success(Unit)

    /** Tandai percakapan telah dibaca sehingga badge unread hilang. */
    suspend fun markThreadRead(bookingId: String) = Unit
}

interface WalletRepository {
    fun observeProviderRevenue(providerId: String): Flow<ProviderRevenueAnalytics>
    fun observePayoutHistory(providerId: String): Flow<List<PayoutRequest>>
    fun observeWalletTransactions(providerId: String): Flow<List<WalletTransaction>>
    suspend fun requestPayout(
        providerId: String,
        amount: Int,
        channelType: PayoutChannelType,
        channelName: String,
        accountNumber: String,
        accountHolder: String
    ): AppResult<PayoutRequest>
}

interface BantuinRepository :
    CatalogRepository,
    BookingRepository,
    ReviewRepository,
    MetricsRepository,
    AuthRepository,
    ChatRepository,
    WalletRepository

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val roles: Set<String>,
    val avatarUrl: String? = null,
    val defaultAddress: String? = null,
    val defaultLocation: GeoPoint? = null
)

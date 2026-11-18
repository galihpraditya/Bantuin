package id.bantuin.app.domain.repository

import id.bantuin.app.core.result.AppResult
import id.bantuin.app.domain.model.BookingItem
import id.bantuin.app.domain.model.BookingStatus
import id.bantuin.app.domain.model.DayAvailability
import id.bantuin.app.domain.model.LockInfo
import id.bantuin.app.domain.model.MetricSnapshot
import id.bantuin.app.domain.model.OrderDraft
import id.bantuin.app.domain.model.ProviderCandidate
import id.bantuin.app.domain.model.ReviewItem
import id.bantuin.app.domain.model.ServiceCategory
import id.bantuin.app.domain.model.ServiceItem
import id.bantuin.app.domain.model.TimeBlock
import id.bantuin.app.domain.model.TrustInput
import id.bantuin.app.domain.model.TrustResult
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

interface CatalogRepository {
    suspend fun categories(): AppResult<List<ServiceCategory>>
    suspend fun candidates(categoryId: Int): AppResult<List<ProviderCandidate>>
    suspend fun servicesOf(providerId: String): AppResult<List<ServiceItem>>
    suspend fun availability(providerId: String): AppResult<List<DayAvailability>>
    suspend fun saveAvailability(providerId: String, days: List<DayAvailability>): AppResult<Unit>
    suspend fun upsertService(service: ServiceItem): AppResult<Unit>
    suspend fun deleteService(serviceId: String): AppResult<Unit>
}

interface BookingRepository {
    suspend fun busyBlocks(providerId: String, dateIso: String): AppResult<List<TimeBlock>>
    suspend fun acquireLock(providerId: String, startMin: Int, endMin: Int): AppResult<LockInfo>
    suspend fun releaseLock(lockId: String): AppResult<Unit>
    suspend fun createBooking(draft: OrderDraft): AppResult<String>
    suspend fun updateStatus(bookingId: String, newStatus: BookingStatus): AppResult<Unit>
    suspend fun myBookingsCustomer(): AppResult<List<BookingItem>>
    suspend fun myBookingsProvider(): AppResult<List<BookingItem>>
    fun observeBooking(bookingId: String): Flow<BookingItem?>
}

interface ReviewRepository {
    suspend fun submitReview(bookingId: String, providerId: String, rating: Int, comment: String?, tags: List<String>): AppResult<Unit>
    suspend fun reviewsOf(providerId: String): AppResult<List<ReviewItem>>
}

interface MetricsRepository {
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
}

data class UserProfile(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val roles: Set<String>,
    val avatarUrl: String? = null
)

package id.bantuin.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("roles") val roles: List<String> = listOf("customer"),
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("default_address") val defaultAddress: String? = null,
    @SerialName("default_lat") val defaultLat: Double? = null,
    @SerialName("default_lng") val defaultLng: Double? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ServiceCategoryDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("icon") val icon: String = "build",
    @SerialName("default_duration_min") val defaultDurationMin: Int = 60
)

@Serializable
data class ProviderProfileDto(
    @SerialName("user_id") val userId: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double,
    @SerialName("is_suspended") val isSuspended: Boolean = false,
    @SerialName("rating_avg") val ratingAvg: Double? = null,
    @SerialName("completion_rate") val completionRate: Double? = null,
    @SerialName("on_time_rate") val onTimeRate: Double? = null,
    @SerialName("cancel_rate") val cancelRate: Double = 0.0,
    @SerialName("trust_score") val trustScore: Double = 70.0,
    @SerialName("tx_count_30d") val txCount30d: Int = 0,
    @SerialName("flag_anomaly") val flagAnomaly: Boolean = false,
    @SerialName("badge") val badge: String = "NEW",
    @SerialName("is_accepting_orders") val isAcceptingOrders: Boolean = true,
    @SerialName("metrics_updated_at") val metricsUpdatedAt: String? = null
)

@Serializable
data class ProviderProfileUpsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("bio") val bio: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("lat") val lat: Double,
    @SerialName("lng") val lng: Double
)

@Serializable
data class EnableProviderRoleRpcParams(
    @SerialName("p_bio") val bio: String,
    @SerialName("p_address") val address: String,
    @SerialName("p_lat") val lat: Double,
    @SerialName("p_lng") val lng: Double
)

@Serializable
data class ServiceDto(
    @SerialName("id") val id: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("category_id") val categoryId: Int,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("base_fare") val baseFare: Int,
    @SerialName("duration_min") val durationMin: Int,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AvailabilityDto(
    @SerialName("provider_id") val providerId: String,
    @SerialName("day_of_week") val dayOfWeek: Int,
    @SerialName("mode") val mode: String,
    @SerialName("open_time") val openTime: String? = null,
    @SerialName("close_time") val closeTime: String? = null
)

@Serializable
data class BookingDto(
    @SerialName("id") val id: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("service_id") val serviceId: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("complexity") val complexity: String,
    @SerialName("is_urgent") val isUrgent: Boolean = false,
    @SerialName("distance_km") val distanceKm: Double,
    @SerialName("base_fare") val baseFare: Int,
    @SerialName("base_cost") val baseCost: Int,
    @SerialName("surge_applied") val surgeApplied: Boolean = false,
    @SerialName("total_cost") val totalCost: Int,
    @SerialName("customer_lat") val customerLat: Double,
    @SerialName("customer_lng") val customerLng: Double,
    @SerialName("notes") val notes: String? = null,
    @SerialName("status") val status: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("payment_method") val paymentMethod: String = "CASH",
    @SerialName("payment_status") val paymentStatus: String = "UNPAID",
    @SerialName("completion_note") val completionNote: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class SlotLockDto(
    @SerialName("id") val id: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("locked_by") val lockedBy: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
data class ReviewDto(
    @SerialName("id") val id: String,
    @SerialName("booking_id") val bookingId: String,
    @SerialName("customer_id") val customerId: String,
    @SerialName("provider_id") val providerId: String,
    @SerialName("rating") val rating: Int,
    @SerialName("comment") val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class BookingEventDto(
    @SerialName("id") val id: Long? = null,
    @SerialName("booking_id") val bookingId: String,
    @SerialName("status") val status: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class ProviderMetricSnapshotDto(
    @SerialName("id") val id: Long? = null,
    @SerialName("provider_id") val providerId: String,
    @SerialName("computed_at") val computedAt: String? = null,
    @SerialName("trust_score") val trustScore: Double,
    @SerialName("rating_score") val ratingScore: Double? = null,
    @SerialName("on_time_rate") val onTimeRate: Double? = null,
    @SerialName("cancel_rate") val cancelRate: Double? = null,
    @SerialName("tx_count") val txCount: Int,
    @SerialName("flag_anomaly") val flagAnomaly: Boolean
)

// ===== RPC DTOs =====

@Serializable
data class BusyBlockDto(
    @SerialName("start_at") val startAt: String,
    @SerialName("end_at") val endAt: String
)

@Serializable
data class AcquireSlotLockRpcParams(
    @SerialName("p_provider") val providerId: String,
    @SerialName("p_start") val startAt: String,
    @SerialName("p_end") val endAt: String
)

@Serializable
data class CreateBookingRpcParams(
    @SerialName("p_lock_id") val lockId: String,
    @SerialName("p_service_id") val serviceId: String,
    @SerialName("p_complexity") val complexity: String,
    @SerialName("p_is_urgent") val isUrgent: Boolean,
    @SerialName("p_distance_km") val distanceKm: Double,
    @SerialName("p_base_fare") val baseFare: Int,
    @SerialName("p_base_cost") val baseCost: Int,
    @SerialName("p_surge_applied") val surgeApplied: Boolean,
    @SerialName("p_total_cost") val totalCost: Int,
    @SerialName("p_cust_lat") val customerLat: Double,
    @SerialName("p_cust_lng") val customerLng: Double,
    @SerialName("p_notes") val notes: String? = null,
    @SerialName("p_payment_method") val paymentMethod: String = "CASH"
)

@Serializable
data class UpdateBookingStatusRpcParams(
    @SerialName("p_booking") val bookingId: String,
    @SerialName("p_new") val newStatus: String
)

@Serializable
data class ApplyProviderMetricsRpcParams(
    @SerialName("p_provider") val providerId: String,
    @SerialName("p_rating_avg") val ratingAvg: Double?,
    @SerialName("p_completion_rate") val completionRate: Double?,
    @SerialName("p_on_time_rate") val onTimeRate: Double?,
    @SerialName("p_cancel_rate") val cancelRate: Double,
    @SerialName("p_trust_score") val trustScore: Double,
    @SerialName("p_tx_count") val txCount: Int,
    @SerialName("p_flag") val flagAnomaly: Boolean,
    @SerialName("p_badge") val badge: String,
    @SerialName("p_rating_score") val ratingScore: Double?
)

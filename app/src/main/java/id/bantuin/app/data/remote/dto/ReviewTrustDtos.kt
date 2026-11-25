package id.bantuin.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrustBookingDto(
    @SerialName("status") val status: String,
    @SerialName("start_at") val startAt: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class TrustBookingsRpcParams(
    @SerialName("p_provider") val providerId: String,
    @SerialName("p_cutoff") val cutoffIso: String
)

@Serializable
data class ReviewProviderRefDto(
    @SerialName("provider_id") val providerId: String
)

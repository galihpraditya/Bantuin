package id.bantuin.app.data.remote

import id.bantuin.app.data.remote.dto.BookingEventDto
import id.bantuin.app.data.remote.dto.BookingOutcomeDto
import id.bantuin.app.data.remote.dto.ProviderMetricSnapshotDto
import id.bantuin.app.data.remote.dto.ProviderTrustInputDto
import id.bantuin.app.data.remote.dto.ReviewDto
import id.bantuin.app.domain.model.CompletionStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.datetime.Instant
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal class ProviderTrustDataSource(client: SupabaseClient) {
    private val api = client.postgrest

    suspend fun saveReview(
        bookingId: String,
        rating: Int,
        comment: String?,
        tags: List<String>,
        outcome: CompletionStatus?
    ) {
        api.rpc("save_booking_review", buildJsonObject {
            put("p_booking", bookingId)
            put("p_rating", rating)
            put("p_comment", comment)
            putJsonArray("p_tags") { tags.forEach { add(it) } }
            put("p_outcome", outcome?.name)
        })
    }

    suspend fun deleteReview(bookingId: String) {
        api.rpc("delete_booking_review", buildJsonObject { put("p_booking", bookingId) })
    }

    suspend fun deleteOutcome(bookingId: String) {
        api.rpc("delete_booking_outcome", buildJsonObject { put("p_booking", bookingId) })
    }

    suspend fun refresh() {
        api.rpc("refresh_my_provider_trust")
    }

    suspend fun reviewsOf(providerId: String): List<ReviewDto> = api["reviews"].select {
        filter { eq("provider_id", providerId) }
        order("created_at", Order.DESCENDING)
    }.decodeList()

    suspend fun bookingEvents(bookingId: String): List<BookingEventDto> = api["booking_events"].select {
        filter { eq("booking_id", bookingId) }
        order("created_at", Order.ASCENDING)
        order("id", Order.ASCENDING)
    }.decodeList()

    suspend fun outcomes(
        providerId: String,
        since: Instant,
        until: Instant,
        status: CompletionStatus?
    ): List<BookingOutcomeDto> {
        val outcomes = mutableListOf<BookingOutcomeDto>()
        val pageSize = 200L
        while (true) {
            val offset = outcomes.size.toLong()
            val page = api["booking_outcomes"].select {
                filter {
                    eq("provider_id", providerId)
                    gte("resolved_at", since.toString())
                    lte("resolved_at", until.toString())
                    status?.let { eq("status", it.name) }
                }
                order("resolved_at", Order.DESCENDING)
                order("booking_id", Order.ASCENDING)
                range(offset, offset + pageSize - 1)
            }.decodeList<BookingOutcomeDto>()
            outcomes.addAll(page)
            if (page.size < pageSize) return outcomes
        }
    }

    suspend fun inputs(providerIds: List<String>, days: Int): List<ProviderTrustInputDto> {
        // PostgREST's default maximum response size is 1,000 rows.
        return providerIds.chunked(200).flatMap { ids ->
            api.rpc("provider_trust_inputs", buildJsonObject {
                putJsonArray("p_providers") { ids.forEach { add(it) } }
                put("p_days", days)
            }).decodeList<ProviderTrustInputDto>()
        }
    }

    suspend fun snapshots(providerId: String, since: Instant): List<ProviderMetricSnapshotDto> =
        api["provider_metric_snapshots"].select {
            filter {
                eq("provider_id", providerId)
                eq("formula_version", 2)
                gte("computed_at", since.toString())
            }
            order("computed_at", Order.DESCENDING)
            order("id", Order.DESCENDING)
            limit(12)
        }.decodeList<ProviderMetricSnapshotDto>().asReversed()
}

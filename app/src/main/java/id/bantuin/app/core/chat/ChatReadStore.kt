package id.bantuin.app.core.chat

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/** key unik per akun + pesanan agar dua akun di satu perangkat tidak saling menimpa. */
fun chatReadKey(userId: String, bookingId: String): String = "$userId|$bookingId"

interface ChatReadStore {
    val lastReadAt: Flow<Map<String, Instant>>
    fun lastReadAtValue(userId: String, bookingId: String): Instant?
    suspend fun markRead(userId: String, bookingId: String, at: Instant = Clock.System.now())
}

class InMemoryChatReadStore(
    initial: Map<String, Instant> = emptyMap()
) : ChatReadStore {
    private val _lastReadAt = MutableStateFlow(initial)
    override val lastReadAt: Flow<Map<String, Instant>> = _lastReadAt.asStateFlow()

    override fun lastReadAtValue(userId: String, bookingId: String): Instant? =
        _lastReadAt.value[chatReadKey(userId, bookingId)]

    override suspend fun markRead(userId: String, bookingId: String, at: Instant) {
        _lastReadAt.value = _lastReadAt.value + (chatReadKey(userId, bookingId) to at)
    }
}

class SharedPreferencesChatReadStore(
    private val context: Context
) : ChatReadStore {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("bantuin_chat_reads", Context.MODE_PRIVATE)
    }
    private val _lastReadAt = MutableStateFlow(readAll())
    override val lastReadAt: Flow<Map<String, Instant>> = _lastReadAt.asStateFlow()

    override fun lastReadAtValue(userId: String, bookingId: String): Instant? =
        _lastReadAt.value[chatReadKey(userId, bookingId)]

    override suspend fun markRead(userId: String, bookingId: String, at: Instant) {
        val key = chatReadKey(userId, bookingId)
        _lastReadAt.value = _lastReadAt.value + (key to at)
        try {
            prefs.edit().putLong(key, at.toEpochMilliseconds()).apply()
        } catch (_: Exception) {
        }
    }

    private fun readAll(): Map<String, Instant> = try {
        prefs.all
            .filterValues { it is Long }
            .mapValues { (_, value) -> Instant.fromEpochMilliseconds(value as Long) }
    } catch (_: Exception) {
        emptyMap()
    }
}

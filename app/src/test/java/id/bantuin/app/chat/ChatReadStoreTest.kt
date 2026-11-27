package id.bantuin.app.chat

import id.bantuin.app.core.chat.InMemoryChatReadStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatReadStoreTest {
    @Test
    fun markRead_storesLastReadPerUserAndBooking() = runTest {
        val store = InMemoryChatReadStore()
        val at = Instant.fromEpochMilliseconds(1_700_000_000_000L)

        store.markRead("user-1", "booking-1", at)

        assertEquals(at, store.lastReadAtValue("user-1", "booking-1"))
        assertNull(store.lastReadAtValue("user-2", "booking-1"))
        assertNull(store.lastReadAtValue("user-1", "booking-2"))
        assertEquals(at, store.lastReadAt.first()["user-1|booking-1"])
    }
}

package id.bantuin.app.domain.usecase.bookmark

import id.bantuin.app.domain.model.BookmarkedProvider
import id.bantuin.app.domain.model.ProviderCandidate
import kotlinx.datetime.Clock

/**
 * SCALE FEATURE: Bookmark / Mitra Favorit Pelanggan
 *
 * Menyimpan dan mengelola daftar mitra favorit pelanggan secara in-memory
 * (dengan antarmuka siap untuk integrasi database lokal Room / Supabase table `bookmarks`).
 */
class BookmarkUseCase(
    private val store: MutableMap<String, MutableMap<String, BookmarkedProvider>> = sharedStore
) {
    companion object {
        val sharedStore = mutableMapOf<String, MutableMap<String, BookmarkedProvider>>()
    }

    /**
     * Tambah atau hapus bookmark mitra untuk pelanggan tertentu (toggle).
     * @return true jika setelah operasi mitra dalam keadaan ter-bookmark.
     */
    fun toggle(customerId: String, candidate: ProviderCandidate, categoryId: Int): Boolean {
        val userBookmarks = store.getOrPut(customerId) { mutableMapOf() }
        return if (userBookmarks.containsKey(candidate.providerId)) {
            userBookmarks.remove(candidate.providerId)
            false
        } else {
            userBookmarks[candidate.providerId] = candidate.toBookmark(categoryId)
            true
        }
    }

    /** Cek apakah mitra sudah di-bookmark pelanggan. */
    fun isBookmarked(customerId: String, providerId: String): Boolean {
        return store[customerId]?.containsKey(providerId) == true
    }

    /** Ambil seluruh daftar mitra favorit pelanggan, diurutkan terbaru dulu. */
    fun getBookmarks(customerId: String): List<BookmarkedProvider> {
        return store[customerId]?.values?.sortedByDescending { it.savedAt } ?: emptyList()
    }

    /** Hapus semua bookmark pelanggan (untuk keperluan logout/reset). */
    fun clearAll(customerId: String) {
        store.remove(customerId)
    }

    private fun ProviderCandidate.toBookmark(categoryId: Int) = BookmarkedProvider(
        providerId = providerId,
        name = name,
        avatarUrl = avatarUrl,
        address = address,
        ratingAvg = ratingAvg,
        badge = badge,
        minBaseFare = minBaseFare,
        categoryId = categoryId,
        savedAt = Clock.System.now()
    )
}

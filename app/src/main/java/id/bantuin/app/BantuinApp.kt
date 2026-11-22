package id.bantuin.app

import android.app.Application
import android.preference.PreferenceManager
import id.bantuin.app.core.session.SharedPreferencesSessionStore
import id.bantuin.app.core.session.UserSessionStore
import id.bantuin.app.data.remote.SupabaseClientProvider
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.data.repository.SupabaseBantuinRepository
import id.bantuin.app.domain.repository.BantuinRepository
import org.osmdroid.config.Configuration

class BantuinApp : Application() {

    lateinit var sessionStore: UserSessionStore
        private set

    lateinit var repository: BantuinRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Inisialisasi konfigurasi OSMDroid untuk peta open-source tanpa API key
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        sessionStore = SharedPreferencesSessionStore(this)
        val fakeRepo = FakeBantuinRepository(sessionStore = sessionStore)

        // Aktifkan sesi Supabase yang persisten sebelum client pertama kali dibuat.
        SupabaseClientProvider.initialize(this)

        // Gunakan Supabase jika URL & ANON_KEY sudah dikonfigurasi, jika belum fallback ke in-memory
        repository = if (SupabaseClientProvider.isConfigured) {
            SupabaseBantuinRepository(
                client = SupabaseClientProvider.client,
                sessionStore = sessionStore,
                fallback = fakeRepo
            )
        } else {
            fakeRepo
        }
    }

    companion object {
        lateinit var instance: BantuinApp
            private set
    }
}


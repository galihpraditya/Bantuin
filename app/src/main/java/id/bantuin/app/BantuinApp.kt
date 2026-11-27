package id.bantuin.app

import android.app.Application
import id.bantuin.app.core.chat.SharedPreferencesChatReadStore
import id.bantuin.app.core.session.SharedPreferencesSessionStore
import id.bantuin.app.core.session.UserSessionStore
import id.bantuin.app.core.settings.SettingsPreferenceStore
import id.bantuin.app.core.settings.SharedPreferencesSettingsStore
import id.bantuin.app.core.theme.SharedPreferencesThemeStore
import id.bantuin.app.core.theme.ThemePreferenceStore
import id.bantuin.app.data.remote.SupabaseClientProvider
import id.bantuin.app.data.repository.FakeBantuinRepository
import id.bantuin.app.data.repository.SupabaseBantuinRepository
import id.bantuin.app.domain.repository.BantuinRepository
import org.osmdroid.config.Configuration

class BantuinApp : Application() {

    lateinit var sessionStore: UserSessionStore
        private set

    lateinit var themeStore: ThemePreferenceStore
        private set

    val settingsStore: SettingsPreferenceStore by lazy {
        SharedPreferencesSettingsStore(this)
    }

    lateinit var repository: BantuinRepository
        private set

    lateinit var chatReadStore: id.bantuin.app.core.chat.ChatReadStore
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Inisialisasi konfigurasi OSMDroid untuk peta open-source tanpa API key
        val osmPrefs = getSharedPreferences("${packageName}_osmdroid", MODE_PRIVATE)
        Configuration.getInstance().load(this, osmPrefs)
        Configuration.getInstance().userAgentValue = packageName

        sessionStore = SharedPreferencesSessionStore(this)
        themeStore = SharedPreferencesThemeStore(this)
        chatReadStore = SharedPreferencesChatReadStore(this)

        // Aktifkan sesi Supabase yang persisten sebelum client pertama kali dibuat.
        SupabaseClientProvider.initialize(this)

        // Gunakan Supabase jika URL & ANON_KEY sudah dikonfigurasi, jika belum gunakan in-memory offline
        repository = if (SupabaseClientProvider.isConfigured) {
            SupabaseBantuinRepository(
                client = SupabaseClientProvider.client,
                sessionStore = sessionStore,
                chatReadStore = chatReadStore
            )
        } else {
            FakeBantuinRepository(
                sessionStore = sessionStore,
                chatReadStore = chatReadStore
            )
        }
    }

    companion object {
        lateinit var instance: BantuinApp
            private set
    }
}

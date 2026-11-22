package id.bantuin.app.data.remote

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import id.bantuin.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.CodeVerifierCache
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.SettingsCodeVerifierCache
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.engine.android.Android

object SupabaseClientProvider {

    private var persistentSessionManager: SessionManager? = null
    private var persistentCodeVerifierCache: CodeVerifierCache? = null

    /**
     * Mengaktifkan penyimpanan sesi Supabase di SharedPreferences perangkat.
     * Tanpa ini, token akses hilang setiap aplikasi ditutup sehingga pengguna
     * tampak login secara lokal tetapi semua request cloud berjalan tanpa JWT.
     *
     * Harus dipanggil sebelum [client] pertama kali diakses.
     */
    fun initialize(context: Context) {
        if (persistentSessionManager != null) return
        val prefs = context.getSharedPreferences("bantuin_supabase_auth", Context.MODE_PRIVATE)
        val settings = SharedPreferencesSettings(prefs)
        persistentSessionManager = SettingsSessionManager(settings)
        persistentCodeVerifierCache = SettingsCodeVerifierCache(settings)
    }

    val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            try {
                httpEngine = Android.create()
            } catch (_: Throwable) {
                // JVM host / Unit test environment
            }
            install(Postgrest)
            install(Auth) {
                val savedSessionManager = persistentSessionManager
                val savedCodeVerifierCache = persistentCodeVerifierCache
                if (savedSessionManager != null) {
                    sessionManager = savedSessionManager
                    codeVerifierCache = savedCodeVerifierCache
                } else {
                    // Unit test / JVM host: tidak ada Context untuk SharedPreferences.
                    sessionManager = MemorySessionManager()
                    codeVerifierCache = MemoryCodeVerifierCache()
                }
            }
            install(Realtime)
        }
    }

    val isConfigured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() &&
                !BuildConfig.SUPABASE_URL.contains("placeholder") &&
                BuildConfig.SUPABASE_ANON_KEY.isNotBlank() &&
                !BuildConfig.SUPABASE_ANON_KEY.contains("placeholder")
}

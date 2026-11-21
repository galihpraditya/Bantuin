package id.bantuin.app

import android.app.Application
import android.preference.PreferenceManager
import id.bantuin.app.core.session.SharedPreferencesSessionStore
import id.bantuin.app.core.session.UserSessionStore
import id.bantuin.app.data.repository.FakeBantuinRepository
import org.osmdroid.config.Configuration

class BantuinApp : Application() {

    lateinit var sessionStore: UserSessionStore
        private set

    lateinit var repository: FakeBantuinRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Inisialisasi konfigurasi OSMDroid untuk peta open-source tanpa API key
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName

        sessionStore = SharedPreferencesSessionStore(this)
        repository = FakeBantuinRepository(sessionStore = sessionStore)
    }

    companion object {
        lateinit var instance: BantuinApp
            private set
    }
}

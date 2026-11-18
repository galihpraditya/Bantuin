package id.bantuin.app

import android.app.Application
import android.preference.PreferenceManager
import org.osmdroid.config.Configuration

class BantuinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inisialisasi konfigurasi OSMDroid untuk peta open-source tanpa API key
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName
    }
}

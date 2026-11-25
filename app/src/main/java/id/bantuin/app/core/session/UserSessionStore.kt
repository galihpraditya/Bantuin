package id.bantuin.app.core.session

import android.content.Context
import android.content.SharedPreferences
import id.bantuin.app.domain.model.GeoPoint
import id.bantuin.app.domain.repository.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

interface UserSessionStore {
    val activeRole: Flow<String>
    val activeRoleValue: String
    val currentUser: Flow<UserProfile?>
    val currentUserValue: UserProfile?
    suspend fun setActiveRole(role: String)
    suspend fun saveSession(user: UserProfile)
    suspend fun clearSession()
}

class SharedPreferencesSessionStore(
    private val context: Context
) : UserSessionStore {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("bantuin_user_session", Context.MODE_PRIVATE)
    }

    private val _activeRole = MutableStateFlow(readActiveRole())
    override val activeRole: Flow<String> = _activeRole.asStateFlow()
    override val activeRoleValue: String get() = _activeRole.value

    private val _currentUser = MutableStateFlow(readCurrentUser())
    override val currentUser: Flow<UserProfile?> = _currentUser.asStateFlow()
    override val currentUserValue: UserProfile? get() = _currentUser.value

    private fun readActiveRole(): String {
        return try {
            prefs.getString(KEY_ACTIVE_ROLE, "customer") ?: "customer"
        } catch (e: Exception) {
            "customer"
        }
    }

    private fun readCurrentUser(): UserProfile? {
        return try {
            // Tidak ada sesi tersimpan berarti pengguna belum login. Jangan pernah
            // mengembalikan akun demo secara implisit karena itu membuat kredensial
            // apa pun tampak berhasil masuk sebagai Rani Wijaya.
            val id = prefs.getString(KEY_USER_ID, null) ?: return null
            val name = prefs.getString(KEY_USER_NAME, "") ?: ""
            val email = prefs.getString(KEY_USER_EMAIL, "") ?: ""
            val phone = prefs.getString(KEY_USER_PHONE, "") ?: ""
            val rolesSet = prefs.getStringSet(KEY_USER_ROLES, setOf("customer")) ?: setOf("customer")
            val avatar = prefs.getString(KEY_USER_AVATAR, null)
            val defaultAddress = prefs.getString(KEY_DEFAULT_ADDRESS, null)
            val defaultLocation = if (prefs.contains(KEY_DEFAULT_LAT) && prefs.contains(KEY_DEFAULT_LNG)) {
                GeoPoint(
                    lat = prefs.getFloat(KEY_DEFAULT_LAT, 0f).toDouble(),
                    lng = prefs.getFloat(KEY_DEFAULT_LNG, 0f).toDouble()
                )
            } else {
                null
            }
            UserProfile(id, name, email, phone, rolesSet, avatar, defaultAddress, defaultLocation)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun setActiveRole(role: String) {
        _activeRole.value = role
        try {
            prefs.edit().putString(KEY_ACTIVE_ROLE, role).apply()
        } catch (_: Exception) {
        }
    }

    override suspend fun saveSession(user: UserProfile) {
        _currentUser.value = user
        try {
            val editor = prefs.edit()
                .putString(KEY_USER_ID, user.id)
                .putString(KEY_USER_NAME, user.fullName)
                .putString(KEY_USER_EMAIL, user.email)
                .putString(KEY_USER_PHONE, user.phone)
                .putStringSet(KEY_USER_ROLES, user.roles)
                .putString(KEY_USER_AVATAR, user.avatarUrl)
                .putString(KEY_DEFAULT_ADDRESS, user.defaultAddress)
            if (user.defaultLocation != null) {
                editor.putFloat(KEY_DEFAULT_LAT, user.defaultLocation.lat.toFloat())
                editor.putFloat(KEY_DEFAULT_LNG, user.defaultLocation.lng.toFloat())
            } else {
                editor.remove(KEY_DEFAULT_LAT).remove(KEY_DEFAULT_LNG)
            }
            editor.apply()
        } catch (_: Exception) {
        }
    }

    override suspend fun clearSession() {
        _currentUser.value = null
        _activeRole.value = "customer"
        try {
            prefs.edit().clear().apply()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val KEY_ACTIVE_ROLE = "key_active_role"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_USER_PHONE = "key_user_phone"
        private const val KEY_USER_ROLES = "key_user_roles"
        private const val KEY_USER_AVATAR = "key_user_avatar"
        private const val KEY_DEFAULT_ADDRESS = "key_default_address"
        private const val KEY_DEFAULT_LAT = "key_default_lat"
        private const val KEY_DEFAULT_LNG = "key_default_lng"
    }
}

class InMemorySessionStore(
    initialRole: String = "customer",
    initialUser: UserProfile? = UserProfile(
        id = "user_demo_rani",
        fullName = "Rani Wijaya",
        email = "rani@bantuin.test",
        phone = "081234567890",
        roles = setOf("customer", "provider")
    )
) : UserSessionStore {
    private val _activeRole = MutableStateFlow(initialRole)
    override val activeRole: Flow<String> = _activeRole.asStateFlow()
    override val activeRoleValue: String get() = _activeRole.value

    private val _currentUser = MutableStateFlow(initialUser)
    override val currentUser: Flow<UserProfile?> = _currentUser.asStateFlow()
    override val currentUserValue: UserProfile? get() = _currentUser.value

    override suspend fun setActiveRole(role: String) {
        _activeRole.value = role
    }

    override suspend fun saveSession(user: UserProfile) {
        _currentUser.value = user
    }

    override suspend fun clearSession() {
        _currentUser.value = null
    }
}

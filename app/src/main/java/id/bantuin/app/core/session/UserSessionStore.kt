package id.bantuin.app.core.session

import android.content.Context
import android.content.SharedPreferences
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
            val id = prefs.getString(KEY_USER_ID, null) ?: return defaultDemoUser()
            val name = prefs.getString(KEY_USER_NAME, "Rani Wijaya") ?: "Rani Wijaya"
            val email = prefs.getString(KEY_USER_EMAIL, "rani@example.com") ?: "rani@example.com"
            val phone = prefs.getString(KEY_USER_PHONE, "081234567890") ?: "081234567890"
            val rolesSet = prefs.getStringSet(KEY_USER_ROLES, setOf("customer", "provider")) ?: setOf("customer", "provider")
            val avatar = prefs.getString(KEY_USER_AVATAR, null)
            UserProfile(id, name, email, phone, rolesSet, avatar)
        } catch (e: Exception) {
            defaultDemoUser()
        }
    }

    private fun defaultDemoUser() = UserProfile(
        id = "user_demo_rani",
        fullName = "Rani Wijaya",
        email = "rani@example.com",
        phone = "081234567890",
        roles = setOf("customer", "provider")
    )

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
            prefs.edit()
                .putString(KEY_USER_ID, user.id)
                .putString(KEY_USER_NAME, user.fullName)
                .putString(KEY_USER_EMAIL, user.email)
                .putString(KEY_USER_PHONE, user.phone)
                .putStringSet(KEY_USER_ROLES, user.roles)
                .putString(KEY_USER_AVATAR, user.avatarUrl)
                .apply()
        } catch (_: Exception) {
        }
    }

    override suspend fun clearSession() {
        _currentUser.value = null
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

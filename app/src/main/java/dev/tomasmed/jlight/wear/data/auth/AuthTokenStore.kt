package dev.tomasmed.jlight.wear.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stored credentials for authenticating with JlightCalendar (ADR 0003).
 * [agentPat] is redacted in [toString] so it can never leak into Logcat.
 */
data class StoredAuthCredentials(
    val baseUrl: String,
    val galaxyId: String,
    val agentPat: String,
    val agentName: String = "Galaxy Watch",
) {
    override fun toString(): String =
        "StoredAuthCredentials(baseUrl=$baseUrl, galaxyId=$galaxyId, agentName=$agentName, agentPat=<REDACTED>)"
}

interface AuthTokenStore {
    fun getCredentials(): StoredAuthCredentials?

    fun saveCredentials(credentials: StoredAuthCredentials)

    fun clearCredentials()
}

/**
 * Hardware-backed Android Keystore implementation using [EncryptedSharedPreferences].
 */
@Suppress("DEPRECATION")
class EncryptedPrefsAuthTokenStore(
    context: Context,
) : AuthTokenStore {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override fun getCredentials(): StoredAuthCredentials? {
        val baseUrl = prefs.getString(KEY_BASE_URL, null) ?: return null
        val galaxyId = prefs.getString(KEY_GALAXY_ID, null) ?: return null
        val pat = prefs.getString(KEY_AGENT_PAT, null) ?: return null
        val agentName = prefs.getString(KEY_AGENT_NAME, "Galaxy Watch") ?: "Galaxy Watch"
        if (pat.isBlank() || galaxyId.isBlank()) return null
        return StoredAuthCredentials(
            baseUrl = baseUrl,
            galaxyId = galaxyId,
            agentPat = pat,
            agentName = agentName,
        )
    }

    override fun saveCredentials(credentials: StoredAuthCredentials) {
        prefs.edit()
            .putString(KEY_BASE_URL, credentials.baseUrl.trimEnd('/'))
            .putString(KEY_GALAXY_ID, credentials.galaxyId.trim())
            .putString(KEY_AGENT_PAT, credentials.agentPat.trim())
            .putString(KEY_AGENT_NAME, credentials.agentName.trim())
            .apply()
    }

    override fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_FILE_NAME = "jlight_wear_encrypted_auth"
        const val KEY_BASE_URL = "base_url"
        const val KEY_GALAXY_ID = "galaxy_id"
        const val KEY_AGENT_PAT = "agent_pat"
        const val KEY_AGENT_NAME = "agent_name"
    }
}

/**
 * In-memory implementation for fast JVM unit tests.
 */
class InMemoryAuthTokenStore(
    initial: StoredAuthCredentials? = null,
) : AuthTokenStore {
    private var current: StoredAuthCredentials? = initial

    override fun getCredentials(): StoredAuthCredentials? = current

    override fun saveCredentials(credentials: StoredAuthCredentials) {
        current = credentials
    }

    override fun clearCredentials() {
        current = null
    }
}

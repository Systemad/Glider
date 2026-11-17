package nl.viter.glider.shared.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.webkit.CookieManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Preferences(
    val theme: String = "system",
    val dynamic: Boolean = true,
    val pure: Boolean = false,
    val large: Boolean = true,
    val avatars: Boolean = true,
    val rainbowThreads: Boolean = false,
    val metadata: Boolean = true,
    val favicons: Boolean = false,
    val articleThumbnails: Boolean = false,
    val actions: Boolean = false,
    val jobs: Boolean = true,
    val threadNavigation: Boolean = true,
    val downvoting: Boolean = false,
    val customTabs: Boolean = false,
    val drawer: Boolean = false,
    val lines: Int = 2,
    val font: String = "Noto Sans",
    val seed: Long = 0xff6750a4,
    val variant: String = "Tonal spot",
    val words: Set<String> = emptySet(),
    val domains: Set<String> = emptySet(),
    val favorites: Set<Long> = emptySet(),
    val visited: Set<Long> = emptySet(),
    val upvoted: Set<Long> = emptySet(),
    val downvoted: Set<Long> = emptySet(),
    val flagged: Set<Long> = emptySet(),
    val blocked: Set<String> = emptySet(),
    val username: String = ""
)
private val Context.gliderDataStore by preferencesDataStore("glider")

@Singleton
class AppStore @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.gliderDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val stateKey = stringPreferencesKey("state")
    private val prefs = context.getSharedPreferences("native_glider", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val mutableState =
        MutableStateFlow(
            runCatching {
                json.decodeFromString<Preferences>(prefs.getString("state", "{}")!!)
            }.getOrDefault(Preferences())
        )
    val state = mutableState.asStateFlow()
    private val mutableReady = MutableStateFlow(false)
    val ready = mutableReady.asStateFlow()
    private val writes = kotlinx.coroutines.channels.Channel<String>(
        kotlinx.coroutines.channels.Channel.UNLIMITED
    )
    init {
        scope.launch {
            val saved = dataStore.data.first()[stateKey]
            mutableState.value =
                if (saved !=
                    null
                ) {
                    json.decodeFromString<Preferences>(saved)
                } else {
                    runCatching {
                        importFlutterPreferences(context)
                    }.getOrDefault(Preferences())
                }
            if (saved == null) dataStore.edit { it[stateKey] = json.encodeToString(state.value) }
            mutableReady.value = true
            for (encoded in writes)dataStore.edit { it[stateKey] = encoded }
        }
    }
    fun update(change: (Preferences) -> Preferences) {
        mutableState.value = change(state.value)
        val encoded = json.encodeToString(state.value)
        writes.trySend(encoded)
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("glider_session", null) as? SecretKey)
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(
                    KeyGenParameterSpec.Builder(
                        "glider_session",
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(
                            KeyProperties.BLOCK_MODE_GCM
                        ).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build()
                )
            }.generateKey()
    }
    fun session(): String? = runCatching {
        val bytes = Base64.decode(prefs.getString("session", null) ?: return null, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)))
    }.getOrNull()
    fun login(cookie: String, username: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key())
        }
        prefs.edit().putString(
            "session",
            Base64.encodeToString(cipher.iv + cipher.doFinal(cookie.toByteArray()), Base64.NO_WRAP)
        ).apply()
        update { it.copy(username = username) }
    }
    fun logout() {
        prefs.edit().remove("session").apply()
        CookieManager.getInstance().removeAllCookies(null)
        update {
            it.copy(
                username = "",
                upvoted = emptySet(),
                downvoted = emptySet(),
                flagged = emptySet()
            )
        }
    }
    fun draft(key: String): String = prefs.getString("draft_$key", "").orEmpty()
    fun draft(key: String, value: String) {
        prefs.edit().putString("draft_$key", value).apply()
    }
}

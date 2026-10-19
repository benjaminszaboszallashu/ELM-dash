package hu.elmdash.connection

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.net.URI
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class TrafficConfig(val enabled: Boolean = false, val url: String = "") {
    fun validate() {
        if (!enabled && url.isBlank()) return
        val uri = runCatching { URI(url) }.getOrNull()
        require(uri != null && uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.rawUserInfo == null && uri.rawFragment == null) {
            "HTTPS lekérési link szükséges, beágyazott felhasználónév nélkül."
        }
    }
}
/** The subscription URL may contain a token: encrypted at rest, never logged or backed up. */
class TrafficSettings(context: Context) {
    private val prefs = context.getSharedPreferences("elm-traffic", Context.MODE_PRIVATE)
    fun load(): TrafficConfig {
        val url = prefs.getString("secret", null)?.let { encrypted ->
            val bytes = Base64.decode(encrypted, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12))) }
            cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
        }.orEmpty()
        return TrafficConfig(prefs.getBoolean("enabled", false), url)
    }
    fun save(config: TrafficConfig) {
        config.validate()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = Base64.encodeToString(cipher.iv + cipher.doFinal(config.url.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        check(prefs.edit().putBoolean("enabled", config.enabled).putString("secret", encrypted).commit())
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("elm-traffic-v1", null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("elm-traffic-v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}

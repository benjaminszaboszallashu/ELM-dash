package hu.elmdash.connection.receiptai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Separate Android Keystore key; app backups are disabled. Never log these records. */
class ChatGptStore(context: Context) {
    private val prefs=context.getSharedPreferences("elm-chatgpt",Context.MODE_PRIVATE)
    val host: String get() = prefs.getString("host",null) ?: ("urn:uuid:"+UUID.randomUUID()).also { check(prefs.edit().putString("host",it).commit()) }
    var active: String? get()=prefs.getString("active",null)
        set(v) { check(prefs.edit().putString("active",v).commit()) }
    var model: String get()=prefs.getString("model","").orEmpty()
        set(v) { check(prefs.edit().putString("model",v).commit()) }
    fun accounts(): List<JSONObject> {
        val text=prefs.getString("secret",null) ?: return emptyList()
        val bytes=Base64.decode(text,Base64.NO_WRAP)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12))) }
        val array=JSONArray(String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8))
        return (0 until array.length()).map { array.getJSONObject(it) }
    }
    fun selected()=accounts().firstOrNull { it.getString("client_id")==active }
    fun save(record:JSONObject) {
        val records=accounts().filterNot { it.getString("client_id")==record.getString("client_id") }+record
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()) }
        val text=Base64.encodeToString(cipher.iv+cipher.doFinal(JSONArray(records).toString().toByteArray()),Base64.NO_WRAP)
        check(prefs.edit().putString("secret",text).commit())
    }
    private fun key():SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return ks.getKey("elm-chatgpt-v1",null) as? SecretKey ?: KeyGenerator.getInstance("AES","AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("elm-chatgpt-v1",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}

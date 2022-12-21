package mobi.lab.labencryptedstorage.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.text.TextUtils
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import mobi.lab.labencryptedstorage.R
import mobi.lab.labencryptedstorage.entity.KeyValueStorageException
import mobi.lab.labencryptedstorage.inter.KeyValueEncryptedStorage
import java.lang.reflect.Type

/**
 * Android EncryptedSharedPreferences based implementation of [KeyValueEncryptedStorage].
 *
 * @property appContext Application context
 */
public class KeyValueStorageEncryptedSharedPreferences constructor(private val appContext: Context) : KeyValueEncryptedStorage {

    @SuppressLint("ApplySharedPref")
    @Suppress("SwallowedException")
    override fun store(key: String, value: Any?) {
        val pref = getEncryptedSharedPreferencesFor(key)

        // Convert the result to JSON and store
        val dataJson = if (value != null) createGson().toJson(value) else null
        // Strategy:
        // We cache the old value temporarily
        // We try to store and write the new value to the disk
        // If success, then we are done
        // If fail then we try to restore the old value and throw an Exception
        try {
            // Get a copy of the old data
            val oldDataJson = pref.getString(getPrimaryDataKey(key), null)
            val success: Boolean = try {
                // Write the new one to storage it to storage
                pref.edit().putString(getPrimaryDataKey(key), dataJson).commit()
            } catch (e: Throwable) {
                false
            }
            if (!success) {
                // Restore the memory cache value to the old one
                try {
                    pref.edit().putString(getPrimaryDataKey(key), oldDataJson).commit()
                } catch (e: Throwable) {
                    // Ignore any and all errors from there
                }
                throw KeyValueStorageException.StoreException(appContext.getString(R.string.error_store_generic_1))
            }
        } catch (e: Throwable) {
            throw KeyValueStorageException.StoreException(appContext.getString(R.string.error_store_generic_1), e)
        }
    }

    @Suppress("SwallowedException")
    override fun <T> read(key: String, valueType: Type): T? {
        val pref = getEncryptedSharedPreferencesFor(key)

        // Get the value if we have any
        val dataJson = pref.getString(getPrimaryDataKey(key), null)
        if (TextUtils.isEmpty(dataJson)) {
            return null // We have nothing stored here
        }

        return try {
            createGson().fromJson(dataJson, valueType)
        } catch (e: JsonSyntaxException) {
            return null // We have nothing
        }
    }

    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    @Suppress("SwallowedException")
    override fun delete(key: String) {
        try {
            val pref = getEncryptedSharedPreferencesFor(key)
            pref.edit().remove(getPrimaryDataKey(key)).commit()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    appContext.deleteSharedPreferences(getStoragePrefix(key))
                } catch (e: Throwable) {
                    // Swallow, we do not care about the result or any exceptions
                }
            }
        } catch (e: Throwable) {
            throw KeyValueStorageException.DeleteException(appContext.getString(R.string.error_delete_generic_1), e)
        }
    }

    private fun getEncryptedSharedPreferencesFor(tag: String): SharedPreferences {
        val masterKey: MasterKey = createOrGetMasterKey()

        return EncryptedSharedPreferences.create(
            appContext,
            getStoragePrefix(tag),
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun createOrGetMasterKey(): MasterKey {
        return MasterKey.Builder(appContext, STORAGE_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(true)
            .build()
    }

    private fun getStoragePrefix(tag: String): String {
        return "$STORAGE_BASE_ID.STORAGE_$tag"
    }

    private fun getPrimaryDataKey(tag: String): String {
        return "$STORAGE_BASE_ID.KEY_PRIMARY_DATA_$tag"
    }

    private fun createGson(): Gson {
        return GsonBuilder().create()
    }

    private companion object {
        const val STORAGE_MASTER_KEY_ALIAS: String = "_tester_master_key_1"
        const val STORAGE_BASE_ID: String = "mobi.lab.labencryptedstorage_encrypted"
    }
}

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
import com.google.gson.TypeAdapterFactory
import mobi.lab.labencryptedstorage.R
import mobi.lab.labencryptedstorage.entity.EncryptionPreferredType
import mobi.lab.labencryptedstorage.entity.KeyValueStorageException
import mobi.lab.labencryptedstorage.entity.SelectedStoragePersistenceId
import mobi.lab.labencryptedstorage.inter.KeyValueEncryptedStorage
import mobi.lab.labencryptedstorage.internal.BundleTypeAdapterFactory
import mobi.lab.labencryptedstorage.internal.exhaustive
import java.lang.reflect.Type

/**
 * Android EncryptedSharedPreferences based implementation of [KeyValueEncryptedStorage].
 *
 * @property appContext Application context
 * @property customGsonTypeAdapterFactories Custom Gson adapter factories to use during serialization and deserialization.
 * By default uses [BundleTypeAdapterFactory].
 */
public class KeyValueStorageEncryptedSharedPreferences constructor(
    private val appContext: Context,
    private var encryptionPreferredTypeInternal: EncryptionPreferredType = EncryptionPreferredType.Tee,
    private val customGsonTypeAdapterFactories: Array<TypeAdapterFactory> = arrayOf(BundleTypeAdapterFactory())
) : KeyValueEncryptedStorage {
    private val gson: Gson

    init {
        gson = createGson()
    }

    override fun updateEncryptionPreferredType(actualEncryptionPreferredType: EncryptionPreferredType) {
        this.encryptionPreferredTypeInternal = actualEncryptionPreferredType
    }

    override fun getEncryptionPreferredType(): EncryptionPreferredType {
        return encryptionPreferredTypeInternal
    }

    @SuppressLint("ApplySharedPref")
    @Suppress("SwallowedException")
    override fun store(key: String, value: Any?) {
        val pref = getEncryptedSharedPreferencesFor(key)

        // Convert the result to JSON and store
        val dataJson = if (value != null) gson.toJson(value) else null
        // Strategy:
        // We cache the old value temporarily
        // We try to store and write the new value to the disk
        // If success, then we are done
        // If fail then we try to restore the old value and throw an Exception
        try {
            // Get a copy of the old data
            val oldDataJson = pref.getString(key, null)
            val success: Boolean = try {
                // Write the new one to storage it to storage
                pref.edit().putString(key, dataJson).commit()
            } catch (e: Throwable) {
                false
            }
            if (!success) {
                // Restore the memory cache value to the old one
                try {
                    pref.edit().putString(key, oldDataJson).commit()
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
        val dataJson = pref.getString(key, null)
        if (TextUtils.isEmpty(dataJson)) {
            return null // We have nothing stored here
        }

        return try {
            gson.fromJson(dataJson, valueType)
        } catch (e: JsonSyntaxException) {
            null // We have nothing
        }
    }

    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    @Suppress("SwallowedException")
    override fun delete(key: String) {
        try {
            val pref = getEncryptedSharedPreferencesFor(key)
            pref.edit().remove(key).commit()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    appContext.deleteSharedPreferences(key)
                } catch (e: Throwable) {
                    // Swallow, we do not care about the result or any exceptions
                }
            }
        } catch (e: Throwable) {
            throw KeyValueStorageException.DeleteException(appContext.getString(R.string.error_delete_generic_1), e)
        }
    }

    override fun getStorageName(): String {
        return "KeyValueStorageEncryptedSharedPreferences with $encryptionPreferredTypeInternal"
    }

    override fun getSelectedStoragePersistenceId(): SelectedStoragePersistenceId {
        return when (encryptionPreferredTypeInternal) {
            EncryptionPreferredType.StrongBox -> SelectedStoragePersistenceId.ENCRYPTED_STRONG_BOX_PREFERRED
            EncryptionPreferredType.Tee -> SelectedStoragePersistenceId.ENCRYPTED_TEE_PREFERRED
        }.exhaustive
    }

    private fun getEncryptedSharedPreferencesFor(filename: String): SharedPreferences {
        val masterKey: MasterKey = createOrGetMasterKey()

        return EncryptedSharedPreferences.create(
            appContext,
            filename,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun createOrGetMasterKey(): MasterKey {
        return MasterKey.Builder(appContext, STORAGE_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .setRequestStrongBoxBacked(getIsStrongBoxBackedFromEncryptionPreferredTypeValue())
            .build()
    }

    private fun getIsStrongBoxBackedFromEncryptionPreferredTypeValue(): Boolean {
        return when (encryptionPreferredTypeInternal) {
            EncryptionPreferredType.StrongBox -> true
            EncryptionPreferredType.Tee -> false
        }.exhaustive
    }

    private fun createGson(): Gson {
        val builder = GsonBuilder()
        for (customGsonTypeAdapterFactory in customGsonTypeAdapterFactories) {
            builder.registerTypeAdapterFactory(customGsonTypeAdapterFactory)
        }
        return builder.create()
    }

    private companion object {
        const val STORAGE_MASTER_KEY_ALIAS: String = "mobi.lab.labencryptedstorage_master_key_1"
    }
}

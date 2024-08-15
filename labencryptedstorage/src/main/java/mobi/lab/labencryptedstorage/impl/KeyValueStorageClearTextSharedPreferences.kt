package mobi.lab.labencryptedstorage.impl

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.text.TextUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.TypeAdapterFactory
import mobi.lab.labencryptedstorage.R
import mobi.lab.labencryptedstorage.entity.KeyValueStorageException
import mobi.lab.labencryptedstorage.entity.SelectedStoragePersistenceId
import mobi.lab.labencryptedstorage.inter.KeyValueClearTextStorage
import java.lang.reflect.Type

/**
 * Android SharedPreferences based implementation of [KeyValueClearTextStorage].
 *
 * @property appContext Application context
 * @property customGsonTypeAdapterFactories Custom Gson adapter factories to use during serialization and deserialization.
 * By default uses no custom factories.
 */
public class KeyValueStorageClearTextSharedPreferences constructor(
    private val appContext: Context,
    private val customGsonTypeAdapterFactories: Array<TypeAdapterFactory> = arrayOf()
) : KeyValueClearTextStorage {
    private val gson: Gson

    init {
        gson = createGson()
    }

    @SuppressLint("ApplySharedPref")
    @Suppress("SwallowedException")
    override fun store(key: String, value: Any?) {
        val pref = getSharedPrefsFor(key)

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
            pref.commitForResult(key, dataJson)
                .onFailure { pref.commitForResult(key, oldDataJson) }
                .getOrThrow()
        } catch (e: Throwable) {
            throw KeyValueStorageException.StoreException(appContext.getString(R.string.error_store_generic_1), e)
        }
    }

    private fun SharedPreferences.commitForResult(key: String, value: String?): Result<Boolean> =
        runCatching { edit().putString(key, value).commit() }

    @Suppress("SwallowedException")
    override fun <T> read(key: String, valueType: Type): T? {
        val pref = getSharedPrefsFor(key)

        // Get the value if we have any
        val dataJson = pref.getString(key, null)
        if (TextUtils.isEmpty(dataJson)) {
            return null // We have nothing stored here
        }

        return try {
            gson.fromJson(dataJson, valueType)
        } catch (e: JsonParseException) {
            null // We have nothing
        }
    }

    @SuppressLint("CommitPrefEdits", "ApplySharedPref")
    @Suppress("SwallowedException")
    override fun delete(key: String) {
        try {
            val pref = getSharedPrefsFor(key)
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
        return "KeyValueStorageClearTextSharedPreferences"
    }

    override fun getSelectedStoragePersistenceId(): SelectedStoragePersistenceId {
        return SelectedStoragePersistenceId.CLEAR_TEXT
    }

    private fun getSharedPrefsFor(filename: String): SharedPreferences {
        return appContext.getSharedPreferences(filename, Context.MODE_PRIVATE)
    }

    private fun createGson(): Gson {
        val builder = GsonBuilder()
        for (customGsonTypeAdapterFactory in customGsonTypeAdapterFactories) {
            builder.registerTypeAdapterFactory(customGsonTypeAdapterFactory)
        }
        return builder.create()
    }
}

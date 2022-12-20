package mobi.lab.labencryptedstorage

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import mobi.lab.labencryptedstorage.impl.KeyValueStorageClearTextSharedPreferences
import mobi.lab.labencryptedstorage.impl.KeyValueStorageEncryptedSharedPreferences
import mobi.lab.labencryptedstorage.inter.EncryptedStorageCompatibilityTester
import mobi.lab.labencryptedstorage.inter.KeyValueStorage
import mobi.lab.labencryptedstorage.inter.LabEncryptedStorageManagerInterface
import mobi.lab.labencryptedstorage.internal.exhaustive
import java.io.IOException

public object LabEncryptedStorageManager : LabEncryptedStorageManagerInterface {
    private val selectionLock = Object()

    override fun getLastSelectedStorageImplOrSelectOneNow(
        applicationContext: Context,
        hardwareKeyStoreBasedStorageEncryptionEnabled: Boolean,
        hardwareKeyStoreBasedStorageEncryptionBlacklist: List<String>,
        storageOpDeviceCompatibilityTester: EncryptedStorageCompatibilityTester
    ): KeyValueStorage {
        synchronized(selectionLock) {
            var selectedImpl: KeyValueStorage? = getLastSelectedStorageImpl(applicationContext)
            if (selectedImpl == null) {
                Log.d("LabEncryptedStorageManager", "StorageConfigurationManagerImpl: No storage selected, finding and selecting one ..")
                selectedImpl = findTheBestStorageImpl(
                    applicationContext,
                    hardwareKeyStoreBasedStorageEncryptionEnabled,
                    hardwareKeyStoreBasedStorageEncryptionBlacklist,
                    storageOpDeviceCompatibilityTester
                )
                Log.d("LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Selected \"${selectedImpl.toSelection()}\" for storage")
            } else {
                Log.d(
                    "LabEncryptedStorageManager",
                    "StorageConfigurationManagerImpl: Storage already selected, using last selection \"${selectedImpl.toSelection()}\""
                )
            }
            setLastSelectedStorageImpl(applicationContext, selectedImpl)
            return selectedImpl
        }
    }

    private fun findTheBestStorageImpl(
        applicationContext: Context,
        encryptionEnabled: Boolean,
        deviceBlacklist: List<String>,
        storageOpDeviceCompatibilityTester: EncryptedStorageCompatibilityTester
    ): KeyValueStorage {
        return if (shouldUseClearTextStorage(applicationContext, encryptionEnabled, deviceBlacklist, storageOpDeviceCompatibilityTester)) {
            getClearTextStorageOp(applicationContext)
        } else {
            getEncryptedStorageOp(applicationContext)
        }
    }

    private fun shouldUseClearTextStorage(
        applicationContext: Context,
        encryptionEnabled: Boolean,
        deviceBlacklist: List<String>,
        storageOpDeviceCompatibilityTester: EncryptedStorageCompatibilityTester
    ): Boolean {
        return if (!encryptionEnabled) {
            Log.d(
                "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Caller disallows encrypted storage for all devices."
            )
            true
        } else if (hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice(deviceBlacklist)) {
            Log.d(
                "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Caller disallows encrypted storage for this device."
            )
            true
        } else if (!deviceSupportsEncryptedStorage(applicationContext, storageOpDeviceCompatibilityTester)) {
            Log.d(
                "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: This device doesn't support encrypted storage."
            )
            true
        } else {
            Log.d(
                "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Encrypted storage is allowed and supported, defaulting to that."
            )
            false
        }
    }

    internal fun hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice(
        hardwareKeyStoreBasedStorageEncryptionBlacklist: List<String>
    ): Boolean {
        // Device info in the form of ["manufacturer1 model1","manufacturer2 model2"]
        val currentDeviceManufacturerModel = "${Build.MANUFACTURER} ${Build.MODEL}".lowercase()
        for (deviceManufacturerModel in hardwareKeyStoreBasedStorageEncryptionBlacklist) {
            if (
                deviceManufacturerModel.lowercase() == currentDeviceManufacturerModel ||
                deviceManufacturerModel.lowercase().trim() == currentDeviceManufacturerModel
            ) {
                Log.d(
                    "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Encrypted storage disabled for \"$deviceManufacturerModel\""
                )
                return true
            }
        }
        return false
    }

    internal fun deviceSupportsEncryptedStorage(
        applicationContext: Context,
        storageOpDeviceCompatibilityTester: EncryptedStorageCompatibilityTester
    ): Boolean {
        try {
            // Try some reads and writes
            Log.d(
                "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Testing encryptedStorage .."
            )
            storageOpDeviceCompatibilityTester.runTest(applicationContext, getEncryptedStorageOp(applicationContext))
            Log.d(
                "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Testing success!"
            )
        } catch (throwable: Throwable) {
            Log.d(
                "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Testing failed!", throwable
            )
            // Doesn't support
            return false
        }

        // Supports
        return true
    }

    /**
     * Return the storage impl that is used to store the last selected impl info.
     *
     * @param applicationContext Context
     * @return StorageOp
     */
    private fun getSelectionStorage(applicationContext: Context): KeyValueStorage {
        // Always the clear-text one as this is the safest
        return getClearTextStorageOp(applicationContext)
    }

    /**
     * Get the last selected storage implementation if any has been set.
     *
     * @param applicationContext Context
     * @return StorageOp or null if non selected
     */
    override fun getLastSelectedStorageImpl(applicationContext: Context): KeyValueStorage? {
        val lastSelection = getSelectionStorage(applicationContext).read<StorageOpImplSelection?>(
            LAST_STORAGE_IMPL_SELECTION_TAG,
            object : TypeToken<StorageOpImplSelection?>() {}.type
        )
        // Return null if lastSelection is null
        return lastSelection?.toOpImpl(applicationContext)
    }

    /**
     * Remember the last selected storage implementation.
     *
     * @param applicationContext Context
     * @param storageOp Selected StorageOp
     */
    private fun setLastSelectedStorageImpl(applicationContext: Context, storageOp: KeyValueStorage) {
        getSelectionStorage(applicationContext).store(LAST_STORAGE_IMPL_SELECTION_TAG, storageOp.toSelection())
    }

    private fun getClearTextStorageOp(context: Context): KeyValueStorage {
        return KeyValueStorageClearTextSharedPreferences(context)
    }

    private fun getEncryptedStorageOp(context: Context): KeyValueStorage {
        return KeyValueStorageEncryptedSharedPreferences(context)
    }

    private enum class StorageOpImplSelection {
        @SerializedName("StorageOpClearText")
        STORAGE_OP_CLEAR_TEXT {
            override fun toString(): String {
                return "StorageOpClearText"
            }
        },

        @SerializedName("StorageOpEncryptedTEE")
        STORAGE_OP_ENCRYPTED_TEE {
            override fun toString(): String {
                return "StorageOpEncryptedTEE"
            }
        },

        @SerializedName("StorageOpNone")
        NONE {
            override fun toString(): String {
                return "StorageOpNone"
            }
        };
    }

    private fun StorageOpImplSelection.toOpImpl(context: Context): KeyValueStorage? {
        return when (this) {
            StorageOpImplSelection.STORAGE_OP_CLEAR_TEXT -> getClearTextStorageOp(context)
            StorageOpImplSelection.STORAGE_OP_ENCRYPTED_TEE -> getEncryptedStorageOp(context)
            StorageOpImplSelection.NONE -> null
        }.exhaustive
    }

    private fun KeyValueStorage.toSelection(): StorageOpImplSelection {
        return when (this) {
            is KeyValueStorageClearTextSharedPreferences -> StorageOpImplSelection.STORAGE_OP_CLEAR_TEXT
            is KeyValueStorageEncryptedSharedPreferences -> StorageOpImplSelection.STORAGE_OP_ENCRYPTED_TEE
            else -> {
                throw IOException("Unknown storage impl \"${this::class.simpleName}\" used!")
            }
        }
    }

    private const val LAST_STORAGE_IMPL_SELECTION_TAG: String = "LabEncryptedStorageManager.LAST_STORAGE_IMPL_SELECTION_TAG"
}

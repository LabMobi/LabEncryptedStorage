package mobi.lab.labencryptedstorage

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import mobi.lab.labencryptedstorage.LabEncryptedStorageManager.Builder
import mobi.lab.labencryptedstorage.impl.KeyValueStorageClearTextSharedPreferences
import mobi.lab.labencryptedstorage.impl.KeyValueStorageEncryptedSharedPreferences
import mobi.lab.labencryptedstorage.impl.SimpleEncryptedStorageDeviceCompatibilityTester
import mobi.lab.labencryptedstorage.inter.EncryptedStorageCompatibilityTester
import mobi.lab.labencryptedstorage.inter.KeyValueClearTextStorage
import mobi.lab.labencryptedstorage.inter.KeyValueEncryptedStorage
import mobi.lab.labencryptedstorage.inter.KeyValueStorage
import mobi.lab.labencryptedstorage.inter.LabEncryptedStorageManagerInterface
import mobi.lab.labencryptedstorage.internal.exhaustive
import java.io.IOException

/**
 * Storage manager instance.
 * Use the builder [Builder] to configure the instance.
 *
 * @property applicationContext Context
 * @property hardwareKeyStoreBasedStorageEncryptionEnabled if hardware key store based encryption is allowed.
 * @property hardwareKeyStoreBasedStorageEncryptionBlocklist if there are any specific devices for which hardware
 * key store based encryption should never be allowed.
 * Device info in the form of ["manufacturer1 model1","manufacturer2 model2"].
 * @property clearTextStorage Implementation for the clear-text storage.
 * Used for fallbacks and remembering the selected storage.
 * @property encryptedTextStorage Implementation for the encrypted  storage.
 * Used if allowed and storageCompatibilityTester shows the device supports it.
 * @property storageCompatibilityTester Implementation for tester to test if the encrypted storage works on this given device.
 */
public class LabEncryptedStorageManager(
    private val applicationContext: Context,
    private val hardwareKeyStoreBasedStorageEncryptionEnabled: Boolean,
    private val hardwareKeyStoreBasedStorageEncryptionBlocklist: List<String>,
    private val clearTextStorage: KeyValueClearTextStorage,
    private val encryptedTextStorage: KeyValueEncryptedStorage,
    private val storageCompatibilityTester: EncryptedStorageCompatibilityTester
) : LabEncryptedStorageManagerInterface {
    private val selectionLock = Any()

    override fun getOrSelectStorage(): KeyValueStorage {
        synchronized(selectionLock) {
            var selectedImpl: KeyValueStorage? = getLastSelectedStorageOrNullIfNoneSelectedYet()
            if (selectedImpl == null) {
                Log.d("LabEncryptedStorageManager", "StorageConfigurationManagerImpl: No storage selected, finding and selecting one ..")
                selectedImpl = findTheBestStorageImpl()
                Log.d("LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Selected \"${selectedImpl.toSelection()}\" for storage")
            } else {
                Log.d(
                    "LabEncryptedStorageManager",
                    "StorageConfigurationManagerImpl: Storage already selected, using last selection \"${selectedImpl.toSelection()}\""
                )
            }
            setLastSelectedStorageImpl(selectedImpl)
            return selectedImpl
        }
    }

    private fun findTheBestStorageImpl(): KeyValueStorage {
        return if (shouldUseClearTextStorage()) {
            getSuppliedClearTextStorageImplementation()
        } else {
            getSuppliedEncryptedStorageImplementation()
        }
    }

    private fun shouldUseClearTextStorage(): Boolean {
        return if (!hardwareKeyStoreBasedStorageEncryptionEnabled) {
            Log.d(
                "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Caller disallows encrypted storage for all devices."
            )
            true
        } else if (hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice()) {
            Log.d(
                "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Caller disallows encrypted storage for this device."
            )
            true
        } else if (!deviceSupportsEncryptedStorage(storageCompatibilityTester)) {
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

    private fun hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice(): Boolean {
        // Device info in the form of ["manufacturer1 model1","manufacturer2 model2"]
        val currentDeviceManufacturerModel = "${Build.MANUFACTURER} ${Build.MODEL}".lowercase()
        for (deviceManufacturerModel in hardwareKeyStoreBasedStorageEncryptionBlocklist) {
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

    private fun deviceSupportsEncryptedStorage(
        storageOpDeviceCompatibilityTester: EncryptedStorageCompatibilityTester
    ): Boolean {
        try {
            // Try some reads and writes
            Log.d(
                "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Testing encryptedStorage .."
            )
            storageOpDeviceCompatibilityTester.runTest(applicationContext, getSuppliedEncryptedStorageImplementation())
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
     * @return StorageOp
     */
    private fun getSelectionStorage(): KeyValueStorage {
        // Always the clear-text one as this is the safest
        return getSuppliedClearTextStorageImplementation()
    }

    /**
     * Get the last selected storage implementation if any has been set.
     *
     * @return StorageOp or null if non selected
     */
    override fun getLastSelectedStorageOrNullIfNoneSelectedYet(): KeyValueStorage? {
        val lastSelection = getSelectionStorage().read<StorageOpImplSelection?>(
            LAST_STORAGE_IMPL_SELECTION_TAG,
            object : TypeToken<StorageOpImplSelection?>() {}.type
        )
        // Return null if lastSelection is null
        return lastSelection?.toOpImpl()
    }

    /**
     * Remember the last selected storage implementation.
     *
     * @param storageOp Selected StorageOp
     */
    private fun setLastSelectedStorageImpl(storageOp: KeyValueStorage) {
        getSelectionStorage().store(LAST_STORAGE_IMPL_SELECTION_TAG, storageOp.toSelection())
    }

    public override fun getSuppliedClearTextStorageImplementation(): KeyValueClearTextStorage {
        return clearTextStorage
    }

    public override fun getSuppliedEncryptedStorageImplementation(): KeyValueEncryptedStorage {
        return encryptedTextStorage
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

    private fun StorageOpImplSelection.toOpImpl(): KeyValueStorage? {
        return when (this) {
            StorageOpImplSelection.STORAGE_OP_CLEAR_TEXT -> getSuppliedClearTextStorageImplementation()
            StorageOpImplSelection.STORAGE_OP_ENCRYPTED_TEE -> getSuppliedEncryptedStorageImplementation()
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

    /**
     * Builder class to create a [LabEncryptedStorageManager].
     * @property applicationContext Android Application context
     */
    public data class Builder(
        private val applicationContext: Context
    ) {
        private var hardwareKeyStoreBasedStorageEncryptionEnabled: Boolean = true
        private var hardwareKeyStoreBasedStorageEncryptionBlocklist: List<String> = arrayListOf()
        private var clearTextStorage: KeyValueClearTextStorage = KeyValueStorageClearTextSharedPreferences(applicationContext)
        private var encryptedTextStorage: KeyValueEncryptedStorage = KeyValueStorageEncryptedSharedPreferences(applicationContext)
        private var storageCompatibilityTester: EncryptedStorageCompatibilityTester = SimpleEncryptedStorageDeviceCompatibilityTester()

        /**
         * If hardware key store based encryption is allowed.
         * Default: true
         *
         * @param hardwareKeyStoreBasedStorageEncryptionEnabled true/false
         */
        public fun hardwareKeyStoreBasedStorageEncryptionEnabled(hardwareKeyStoreBasedStorageEncryptionEnabled: Boolean): Builder =
            apply { this.hardwareKeyStoreBasedStorageEncryptionEnabled = hardwareKeyStoreBasedStorageEncryptionEnabled }

        /**
         * If there are any specific devices for which hardware
         * key store based encryption should never be allowed.
         * Device info in the form of ["manufacturer1 model1","manufacturer2 model2"].
         * Default: none.
         *
         * @param hardwareKeyStoreBasedStorageEncryptionBlocklist List of "manufacturer1 model1","manufacturer2 model2"
         */
        public fun hardwareKeyStoreBasedStorageEncryptionBlocklist(hardwareKeyStoreBasedStorageEncryptionBlocklist: List<String>): Builder =
            apply { this.hardwareKeyStoreBasedStorageEncryptionBlocklist = hardwareKeyStoreBasedStorageEncryptionBlocklist }

        /**
         * Implementation for the clear-text storage.
         * Used for fallbacks and remembering the selected storage.
         * Default: [KeyValueStorageClearTextSharedPreferences].
         *
         * @param clearTextStorage Implementation for the clear-text storage.
         */
        public fun clearTextStorage(clearTextStorage: KeyValueClearTextStorage): Builder =
            apply { this.clearTextStorage = clearTextStorage }

        /**
         * Implementation for the encrypted  storage.
         * Used if allowed and storageCompatibilityTester shows the device supports it.
         * Default: [KeyValueStorageEncryptedSharedPreferences].
         *
         * @param encryptedTextStorage Implementation for the encrypted  storage.
         */
        public fun encryptedTextStorage(encryptedTextStorage: KeyValueEncryptedStorage): Builder =
            apply { this.encryptedTextStorage = encryptedTextStorage }

        /**
         * Implementation for tester to test if the encrypted storage works on this given device.
         * Default: [SimpleEncryptedStorageDeviceCompatibilityTester].
         * @param storageCompatibilityTester Implementation for tester to test if the encrypted storage works on this given device
         */
        public fun storageCompatibilityTester(storageCompatibilityTester: EncryptedStorageCompatibilityTester): Builder =
            apply { this.storageCompatibilityTester = storageCompatibilityTester }

        /**
         * Build an instance of [LabEncryptedStorageManager].
         */
        public fun build(): LabEncryptedStorageManager = LabEncryptedStorageManager(
            applicationContext.applicationContext,
            hardwareKeyStoreBasedStorageEncryptionEnabled,
            hardwareKeyStoreBasedStorageEncryptionBlocklist,
            clearTextStorage,
            encryptedTextStorage,
            storageCompatibilityTester,
        )
    }

    private companion object {
        private const val LAST_STORAGE_IMPL_SELECTION_TAG: String = "LabEncryptedStorageManager.LAST_STORAGE_IMPL_SELECTION_TAG"
    }
}

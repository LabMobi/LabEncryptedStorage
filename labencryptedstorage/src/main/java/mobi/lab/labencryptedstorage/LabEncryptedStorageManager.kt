package mobi.lab.labencryptedstorage

import android.content.Context
import android.os.Build
import android.util.Log
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

/**
 * Storage manager instance.
 * Use the builder [Builder] to configure the instance.
 *
 * @property applicationContext Context
 * @property hardwareKeyStoreBasedStorageEncryptionEnabled if hardware key store based encryption is allowed.
 * @property hardwareKeyStoreBasedStorageEncryptionBlocklist if there are any specific devices for which hardware
 * key store based encryption should never be allowed.
 * Device info in the form of ["manufacturer1 model1","manufacturer2 model2"].
 * @property internalChoiceStorage Implementation for internal storage where to remember the storage choice.
 * Used for remembering the selected storage. Usually the same as [clearTextStorage].
 * @property clearTextStorage Implementation for the clear-text storage.
 * Used for fallbacks if encrypted storage does not work. Usually the same as [internalChoiceStorage].
 * @property hardwareKeyStoreBasedEncryptedStorage Implementation for the encrypted  storage.
 * Used if allowed and storageCompatibilityTester shows the device supports it.
 * @property hardwareKeyStoreBasedEncryptedStorageCompatibilityTester Implementation for tester to test if
 * the encrypted storage works on this given device.
 */
@Suppress("LongParameterList")
public open class LabEncryptedStorageManager(
    private val applicationContext: Context,
    private val hardwareKeyStoreBasedStorageEncryptionEnabled: Boolean,
    private val hardwareKeyStoreBasedStorageEncryptionBlocklist: List<String>,
    private val internalChoiceStorage: KeyValueClearTextStorage,
    private val clearTextStorage: KeyValueClearTextStorage,
    private val hardwareKeyStoreBasedEncryptedStorage: KeyValueEncryptedStorage,
    private val hardwareKeyStoreBasedEncryptedStorageCompatibilityTester: EncryptedStorageCompatibilityTester
) : LabEncryptedStorageManagerInterface {
    private val selectionLock = Any()

    override fun getOrSelectStorage(): KeyValueStorage {
        synchronized(selectionLock) {
            var selectedImpl: KeyValueStorage? = getLastSelectedStorageOrNullIfNoneSelectedYet()
            if (selectedImpl == null) {
                Log.d("LabEncryptedStorageManager", "StorageConfigurationManagerImpl: No storage selected, finding and selecting one ..")
                selectedImpl = findTheBestStorageImpl()
                Log.d("LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Selected \"${selectedImpl.getStorageName()}\" for storage")
            } else {
                Log.d(
                    "LabEncryptedStorageManager",
                    "StorageConfigurationManagerImpl: Storage already selected, using last selection \"${selectedImpl.getStorageName()}\""
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
        } else if (!deviceSupportsEncryptedStorage()) {
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

    internal fun hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice(): Boolean {
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

    internal fun deviceSupportsEncryptedStorage(): Boolean {
        try {
            // Try some reads and writes
            Log.d(
                "LabEncryptedStorageManager", "StorageConfigurationManagerImpl: Testing encryptedStorage .."
            )
            hardwareKeyStoreBasedEncryptedStorageCompatibilityTester.runTest(applicationContext, getSuppliedEncryptedStorageImplementation())
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
     * @return KeyValueStorage
     */
    private fun getSelectionStorage(): KeyValueStorage {
        return getSuppliedInternalChoiceStorageImplementation()
    }

    /**
     * Get the last selected storage implementation if any has been set.
     *
     * @return KeyValueStorage or null if non selected
     */
    override fun getLastSelectedStorageOrNullIfNoneSelectedYet(): KeyValueStorage? {
        val lastSelection = getSelectionStorage().read<String?>(
            LAST_STORAGE_IMPL_SELECTION_TAG,
            object : TypeToken<String?>() {}.type
        )
        // Return null if lastSelection is null
        return storageIdToStorageImplementation(lastSelection)
    }

    /**
     * Remember the last selected storage implementation.
     *
     * @param storage Selected KeyValueStorage
     */
    private fun setLastSelectedStorageImpl(storage: KeyValueStorage) {
        getSelectionStorage().store(LAST_STORAGE_IMPL_SELECTION_TAG, storage.getStorageId())
    }

    public override fun getSuppliedInternalChoiceStorageImplementation(): KeyValueClearTextStorage {
        return internalChoiceStorage
    }

    public override fun getSuppliedClearTextStorageImplementation(): KeyValueClearTextStorage {
        return clearTextStorage
    }

    public override fun getSuppliedEncryptedStorageImplementation(): KeyValueEncryptedStorage {
        return hardwareKeyStoreBasedEncryptedStorage
    }

    private fun storageIdToStorageImplementation(id: String?): KeyValueStorage? {
        return when (id) {
            getSuppliedClearTextStorageImplementation().getStorageId() -> {
                getSuppliedClearTextStorageImplementation()
            }
            getSuppliedEncryptedStorageImplementation().getStorageId() -> {
                return getSuppliedEncryptedStorageImplementation()
            }
            else -> {
                null
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
        private var hardwareKeyStoreBasedStorageEncryptionBlocklist: ArrayList<String> = arrayListOf()
        private var clearTextStorage: KeyValueClearTextStorage = KeyValueStorageClearTextSharedPreferences(applicationContext)
        private val internalChoiceStorage: KeyValueClearTextStorage = clearTextStorage
        private var encryptedStorage: KeyValueEncryptedStorage = KeyValueStorageEncryptedSharedPreferences(applicationContext)
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
         * @param deviceManufacturerAndModel List of "manufacturer1 model1","manufacturer2 model2"
         */
        public fun hardwareKeyStoreBasedStorageEncryptionBlocklist(vararg deviceManufacturerAndModel: String): Builder =
            apply { this.hardwareKeyStoreBasedStorageEncryptionBlocklist.addAll(deviceManufacturerAndModel) }

        /**
         *
         * Override implementation for the clear-text storage.
         * WARNING: In 99% of cases you do not want to set this.
         * If a storage is already decided and
         * this is override then all the already written values will become unreadable!
         * Used for fallback flow and remembering the selected storage.
         * Default: [KeyValueStorageClearTextSharedPreferences].
         *
         * @param clearTextStorageOverride Implementation for the clear-text storage.
         */
        public fun overrideClearTextStorage(clearTextStorageOverride: KeyValueClearTextStorage): Builder =
            apply { this.clearTextStorage = clearTextStorageOverride }

        /**
         * Override implementation for the encrypted storage.
         * WARNING: In 99% of cases you do not want to set this.
         * If a storage is already decided and
         * this is override then all the already written values will become unreadable!
         * Used if allowed and storageCompatibilityTester shows the device supports it.
         * Default: [KeyValueStorageEncryptedSharedPreferences].
         *
         * @param encryptedStorageOverride Implementation for the encrypted  storage.
         */
        public fun overrideEncryptedTextStorageImplementation(encryptedStorageOverride: KeyValueEncryptedStorage): Builder =
            apply { this.encryptedStorage = encryptedStorageOverride }

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
            applicationContext = applicationContext.applicationContext,
            hardwareKeyStoreBasedStorageEncryptionEnabled = hardwareKeyStoreBasedStorageEncryptionEnabled,
            hardwareKeyStoreBasedStorageEncryptionBlocklist = hardwareKeyStoreBasedStorageEncryptionBlocklist,
            internalChoiceStorage = internalChoiceStorage,
            clearTextStorage = clearTextStorage,
            hardwareKeyStoreBasedEncryptedStorage = encryptedStorage,
            hardwareKeyStoreBasedEncryptedStorageCompatibilityTester = storageCompatibilityTester,
        )
    }

    private companion object {
        private const val LAST_STORAGE_IMPL_SELECTION_TAG: String = "LabEncryptedStorageManager.LAST_STORAGE_IMPL_SELECTION_TAG"
    }
}

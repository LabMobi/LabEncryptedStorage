package mobi.lab.labencryptedstorage

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import com.google.gson.reflect.TypeToken
import mobi.lab.labencryptedstorage.LabEncryptedStorageManager.Builder
import mobi.lab.labencryptedstorage.entity.EncryptionPreferredType
import mobi.lab.labencryptedstorage.entity.SelectedStoragePersistenceId
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
 * @property encryptionEnabled if hardware key store based encryption is allowed.
 * @property encryptionBlocklist if there are any specific devices for which hardware
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
    private val encryptionEnabled: Boolean,
    private val encryptionBlocklist: List<String>,
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
                selectedImpl = findTheBestStorageImpl()
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

    @Suppress("RedundantIf")
    private fun shouldUseClearTextStorage(): Boolean {
        return if (!encryptionEnabled) {
            true
        } else if (hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice()) {
            true
        } else if (!deviceSupportsEncryptedStorage()) {
            true
        } else {
            false
        }
    }

    @VisibleForTesting
    internal fun hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice(): Boolean {
        // Device info in the form of ["manufacturer1 model1","manufacturer2 model2"]
        val currentDeviceManufacturerModel = "${Build.MANUFACTURER} ${Build.MODEL}"
        for (deviceManufacturerModel in encryptionBlocklist) {
            // Just in case we want to compare in original form, in lowercase and with input trimmed also.
            if (
                deviceManufacturerModel == currentDeviceManufacturerModel ||
                deviceManufacturerModel.lowercase() == currentDeviceManufacturerModel.lowercase() ||
                deviceManufacturerModel.lowercase().trim() == currentDeviceManufacturerModel.lowercase()
            ) {
                return true
            }
        }
        return false
    }

    @Suppress("SwallowedException")
    internal fun deviceSupportsEncryptedStorage(): Boolean {
        try {
            // Try some reads and writes
            hardwareKeyStoreBasedEncryptedStorageCompatibilityTester.runTest(applicationContext, getSuppliedEncryptedStorageImplementation())
        } catch (throwable: Throwable) {
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
        getSelectionStorage().store(LAST_STORAGE_IMPL_SELECTION_TAG, storage.getSelectedStoragePersistenceId().name)
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

    private fun storageIdToStorageImplementation(name: String?): KeyValueStorage? {
        val selectedStoragePersistenceId = SelectedStoragePersistenceId.byNameOrNull(name)
        return if (selectedStoragePersistenceId != null) {
            when (selectedStoragePersistenceId) {
                SelectedStoragePersistenceId.CLEAR_TEXT -> getSuppliedClearTextStorageImplementation()
                SelectedStoragePersistenceId.ENCRYPTED_TEE_PREFERRED -> getSuppliedEncryptedStorageImplementation().apply {
                    updateEncryptionPreferredType(
                        EncryptionPreferredType.Tee
                    )
                }
                SelectedStoragePersistenceId.ENCRYPTED_STRONG_BOX_PREFERRED -> getSuppliedEncryptedStorageImplementation().apply {
                    updateEncryptionPreferredType(
                        EncryptionPreferredType.StrongBox
                    )
                }
            }
        } else {
            null
        }
    }

    /**
     * Builder class to create a [LabEncryptedStorageManager].
     * @property applicationContext Android Application context
     */
    public data class Builder(
        private val applicationContext: Context
    ) {
        private var encryptionEnabled: Boolean = true
        private var encryptionBlocklist: ArrayList<String> = arrayListOf()
        private var encryptionPreferredType: EncryptionPreferredType = EncryptionPreferredType.Tee
        private var encryptedStorageCompatibilityTester: EncryptedStorageCompatibilityTester =
            SimpleEncryptedStorageDeviceCompatibilityTester()

        /**
         * If hardware key store based encryption is allowed.
         * Default: true
         *
         * @param encryptionEnabled true/false
         */
        public fun encryptionEnabled(encryptionEnabled: Boolean): Builder =
            apply { this.encryptionEnabled = encryptionEnabled }

        /**
         * If there are any specific devices for which hardware
         * key store based encryption should never be allowed.
         * Device info in the form of ["manufacturer1 model1","manufacturer2 model2"].
         * Default: none.
         *
         * @param blockedDeviceManufacturerAndModel List of "manufacturer1 model1","manufacturer2 model2"
         */
        public fun encryptionBlocklist(vararg blockedDeviceManufacturerAndModel: String): Builder =
            apply { this.encryptionBlocklist.addAll(blockedDeviceManufacturerAndModel) }

        /**
         * Set the preferred hardware key store based encryption element.
         * Available options are [mobi.lab.labencryptedstorage.entity.EncryptionPreferredType.Tee] and
         * [mobi.lab.labencryptedstorage.entity.EncryptionPreferredType.StrongBox].
         * Default is the former.
         *
         * @param encryptionPreferredType Preferred type
         */
        public fun encryptionPreferredType(encryptionPreferredType: EncryptionPreferredType): Builder =
            apply { this.encryptionPreferredType = encryptionPreferredType }

        /**
         * Implementation for tester to test if the encrypted storage works on this given device.
         * Default: [SimpleEncryptedStorageDeviceCompatibilityTester].
         *
         * @param encryptedStorageCompatibilityTester Implementation for tester to test if the encrypted storage works on this given device
         */
        public fun encryptedStorageCompatibilityTester(
            encryptedStorageCompatibilityTester: EncryptedStorageCompatibilityTester
        ): Builder =
            apply { this.encryptedStorageCompatibilityTester = encryptedStorageCompatibilityTester }

        /**
         * Build an instance of [LabEncryptedStorageManager].
         */
        public fun build(): LabEncryptedStorageManager {
            val clearTextStorage = KeyValueStorageClearTextSharedPreferences(applicationContext.applicationContext)
            return LabEncryptedStorageManager(
                applicationContext = applicationContext.applicationContext,
                encryptionEnabled = encryptionEnabled,
                encryptionBlocklist = encryptionBlocklist,
                clearTextStorage = clearTextStorage,
                internalChoiceStorage = clearTextStorage,
                hardwareKeyStoreBasedEncryptedStorage = KeyValueStorageEncryptedSharedPreferences(
                    applicationContext.applicationContext,
                    encryptionPreferredType
                ),
                hardwareKeyStoreBasedEncryptedStorageCompatibilityTester = encryptedStorageCompatibilityTester,
            )
        }
    }

    private companion object {
        private const val LAST_STORAGE_IMPL_SELECTION_TAG: String = "LabEncryptedStorageManager.LAST_STORAGE_IMPL_SELECTION_TAG"
    }
}

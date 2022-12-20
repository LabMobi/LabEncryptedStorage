package mobi.lab.labencryptedstorage.inter

import android.content.Context
import mobi.lab.labencryptedstorage.impl.SimpleEncryptedStorageDeviceCompatibilityTester

public interface LabEncryptedStorageManagerInterface {
    /**
     * Get the last selected KeyValueStorage impl or select one now.
     * NOTE: This creates side-artifacts!
     * Calling this will either just return the impl to the last used value, or will do the selection and remember it for next times.
     * This method is safe to call multiple times.
     *
     * @param applicationContext Context
     * @param hardwareKeyStoreBasedStorageEncryptionEnabled if hardware key store based encryption is allowed. Default: true
     * @param hardwareKeyStoreBasedStorageEncryptionBlacklist if there are any specific devices for which hardware
     * key store based encryption should never be allowed.
     * Device info in the form of ["manufacturer1 model1","manufacturer2 model2"]. Default: none.
     * @param storageOpDeviceCompatibilityTester Tester to test if the encrypted storage works on this given device.
     * Default [SimpleEncryptedStorageDeviceCompatibilityTester].
     *
     *
     * @return KeyValueStorage
     */
    public fun getLastSelectedStorageImplOrSelectOneNow(
        applicationContext: Context,
        hardwareKeyStoreBasedStorageEncryptionEnabled: Boolean = true,
        hardwareKeyStoreBasedStorageEncryptionBlacklist: List<String> = arrayListOf(),
        storageOpDeviceCompatibilityTester: EncryptedStorageCompatibilityTester = SimpleEncryptedStorageDeviceCompatibilityTester()
    ): KeyValueStorage

    /**
     * Get the last selected KeyValueStorage or null.
     * NOTE: Does not do any selection itself,
     * only returns the selection if the selection is done already.
     * Useful to check if the selection has been done.
     *
     * @param applicationContext Context
     *
     * @return KeyValueStorage or null if non selected
     */
    public fun getLastSelectedStorageImpl(applicationContext: Context): KeyValueStorage?
}

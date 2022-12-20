package mobi.lab.labencryptedstorage.inter

import android.content.Context
import mobi.lab.labencryptedstorage.entity.KeyValueStorageException

/**
 * Interface to test if encrypted storage works on a given device.
 */
public interface EncryptedStorageCompatibilityTester {
    /**
     * Method to test if encrypted storage works on a given device.
     * Should throw a [KeyValueStorageException] if storage should fail.
     *
     * Note: The test should clean up after itself is possible.
     *
     * @param appContext Context
     * @param keyValueStorage KeyValueStorage to test, usually some [KeyValueEncryptedStorage].
     * @throws KeyValueStorageException
     */
    public fun runTest(appContext: Context, keyValueStorage: KeyValueStorage)
}

package mobi.lab.labencryptedstorage.impl

import mobi.lab.labencryptedstorage.inter.EncryptedStorageCompatibilityTester
import mobi.lab.labencryptedstorage.inter.KeyValueStorage

public class SimpleEncryptedStorageDeviceCompatibilityTester : EncryptedStorageCompatibilityTester {
    override fun runTest(keyValueStorage: KeyValueStorage) {
        // NO-op
    }
}

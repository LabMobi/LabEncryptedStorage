package mobi.lab.labencryptedstorage.inter

import mobi.lab.labencryptedstorage.entity.StorageEncryptionType

/**
 * Some TEE / Strongbox key based encrypted implementation of [KeyValueStorage].
 */
public interface KeyValueEncryptedStorage : KeyValueStorage {
    public fun updateEncryptionPreferredType(actualEncryptionPreferredType: StorageEncryptionType)
    public fun getEncryptionPreferredType(): StorageEncryptionType
}

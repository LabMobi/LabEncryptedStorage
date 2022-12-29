package mobi.lab.labencryptedstorage.inter

import mobi.lab.labencryptedstorage.entity.EncryptionPreferredType

/**
 * Some TEE / Strongbox key based encrypted implementation of [KeyValueStorage].
 */
public interface KeyValueEncryptedStorage : KeyValueStorage {
    /**
     * Update the actualEncryptionPreferredType value to the actual stored value.
     *
     * @param actualEncryptionPreferredType actual StorageEncryptionType
     */
    public fun updateEncryptionPreferredType(actualEncryptionPreferredType: EncryptionPreferredType)

    /**
     * Return the StorageEncryptionType.
     *
     * @return StorageEncryptionType
     */
    public fun getEncryptionPreferredType(): EncryptionPreferredType
}

package mobi.lab.labencryptedstorage.inter

public interface EncryptedStorageCompatibilityTester {
    public fun runTest(keyValueStorage: KeyValueStorage)
}

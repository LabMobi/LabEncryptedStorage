package mobi.lab.labencryptedstorage.impl

import android.content.Context
import com.google.gson.reflect.TypeToken
import mobi.lab.labencryptedstorage.R
import mobi.lab.labencryptedstorage.entity.KeyValueStorageException
import mobi.lab.labencryptedstorage.inter.EncryptedStorageCompatibilityTester
import mobi.lab.labencryptedstorage.inter.KeyValueStorage

public class SimpleEncryptedStorageDeviceCompatibilityTester : EncryptedStorageCompatibilityTester {

    @Suppress("MagicNumber")
    override fun runTest(appContext: Context, keyValueStorage: KeyValueStorage) {
        try {
            val x1Tag = "x1"
            val x1Data = "323f330-b09d-4c94-b742-524e763d33c1"
            val x2Tag = "x2"
            val x2Data = "f1abed9e-4cb4-4c4d-aa4c-5e9b471fcf87"

            keyValueStorage.store(x1Tag, x1Data)
            keyValueStorage.store(x2Tag, x2Data)

            val data1XStored = keyValueStorage.read<String>(x1Tag, object : TypeToken<String>() {}.type)
            assert(data1XStored != null)
            assert(x1Data.contentEquals(data1XStored)) { "StorageConfigurationManagerImpl: Stored x1 does not match the original!" }
            val data2XStored = keyValueStorage.read<String>(x2Tag, object : TypeToken<String>() {}.type)!!
            assert(x2Data.contentEquals(data2XStored)) { "StorageConfigurationManagerImpl: Stored x2 does not match the original!" }

            keyValueStorage.delete(x1Tag)
            keyValueStorage.delete(x2Tag)

            val dataX1Deleted = keyValueStorage.read<String>(x1Tag, object : TypeToken<String>() {}.type)
            assert(dataX1Deleted == null) { "StorageConfigurationManagerImpl: The x1 stored value was still available: $dataX1Deleted" }
            val dataX2Deleted = keyValueStorage.read<String>(x2Tag, object : TypeToken<String>() {}.type)
            assert(dataX2Deleted == null) { "StorageConfigurationManagerImpl: The x2 stored value was still available: $dataX2Deleted" }
        } catch (storageException: KeyValueStorageException) {
            // Just re-throw
            throw storageException
        } catch (throwable: Throwable) {
            // Wrap and throw
            throw KeyValueStorageException.StoreException(
                appContext.getString(R.string.err_encrypted_storage_test_failed),
                throwable
            )
        }
    }
}

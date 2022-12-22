package mobi.lab.labencryptedstorage

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import mobi.lab.labencryptedstorage.entity.KeyValueStorageException
import mobi.lab.labencryptedstorage.impl.KeyValueStorageClearTextSharedPreferences
import mobi.lab.labencryptedstorage.impl.KeyValueStorageEncryptedSharedPreferences
import mobi.lab.labencryptedstorage.inter.EncryptedStorageCompatibilityTester
import mobi.lab.labencryptedstorage.inter.KeyValueStorage
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [LabEncryptedStorageManager].
 */
@RunWith(AndroidJUnit4::class)
public class LabEncryptedStorageManagerTest : BaseTestCase() {
    private val deviceTesterThatThrows = object : EncryptedStorageCompatibilityTester {
        override fun runTest(appContext: Context, keyValueStorage: KeyValueStorage) {
            throw KeyValueStorageException.StoreException("Device storage fails!")
        }
    }

    private val deviceTesterThatSucceeds = object : EncryptedStorageCompatibilityTester {
        override fun runTest(appContext: Context, keyValueStorage: KeyValueStorage) {
            // No-op
        }
    }

    /**
     * Test getOrSelectStorage() method when storage already selected vol1.
     */
    @Test
    public fun test_if_storage_is_selected_then_encryption_disabled_boolean_does_not_count() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .hardwareKeyStoreBasedStorageEncryptionEnabled(false)
            .build()
        val implSpy = spy(impl)
        // KeyValueStorageEncryptedSharedPreferences is last selected
        val encImpl = KeyValueStorageEncryptedSharedPreferences(getContextForTarget())
        doReturn(encImpl).whenever(implSpy).getLastSelectedStorageOrNullIfNoneSelectedYet()
        val storageImpl = implSpy.getOrSelectStorage()
        assertTrue(
            storageImpl is KeyValueStorageEncryptedSharedPreferences,
            "Wrong impl was returned: ${storageImpl::class.simpleName}, " +
                "should have been ${KeyValueStorageEncryptedSharedPreferences::class.simpleName}"
        )
    }

    /**
     * Test getOrSelectStorage() method when storage is not yet selected vol1.
     */
    @Test
    public fun test_if_storage_is_not_selected_then_encryption_disabled_boolean_does_count() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .hardwareKeyStoreBasedStorageEncryptionEnabled(false)
            .build()
        val implSpy = spy(impl)
        // Nothing selected
        val encImpl = null
        doReturn(encImpl).whenever(implSpy).getLastSelectedStorageOrNullIfNoneSelectedYet()
        val storageImpl = implSpy.getOrSelectStorage()
        assertTrue(
            storageImpl is KeyValueStorageClearTextSharedPreferences,
            "Wrong impl was returned: ${storageImpl::class.simpleName}, " +
                "should have been ${KeyValueStorageClearTextSharedPreferences::class.simpleName}"
        )
    }

    /**
     * Test getOrSelectStorage() method when storage already selected vol2.
     */
    @Test
    public fun test_if_storage_is_selected_then_device_disallowed_does_not_count() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .hardwareKeyStoreBasedStorageEncryptionBlocklist("${Build.MANUFACTURER} ${Build.MODEL}")
            .build()
        val implSpy = spy(impl)
        // KeyValueStorageEncryptedSharedPreferences is last selected
        val encImpl = KeyValueStorageEncryptedSharedPreferences(getContextForTarget())
        doReturn(encImpl).whenever(implSpy).getLastSelectedStorageOrNullIfNoneSelectedYet()
        val storageImpl = implSpy.getOrSelectStorage()
        assertTrue(
            storageImpl is KeyValueStorageEncryptedSharedPreferences,
            "Wrong impl was returned: ${storageImpl::class.simpleName}, " +
                "should have been ${KeyValueStorageEncryptedSharedPreferences::class.simpleName}"
        )
    }

    /**
     * Test getOrSelectStorage() method when storage is not yet selected vol2.
     */
    @Test
    public fun test_if_storage_is_not_selected_then_device_disallowed_does_count() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .hardwareKeyStoreBasedStorageEncryptionBlocklist("${Build.MANUFACTURER} ${Build.MODEL}")
            .build()
        val implSpy = spy(impl)
        // Nothing selected
        val encImpl = null
        doReturn(encImpl).whenever(implSpy).getLastSelectedStorageOrNullIfNoneSelectedYet()
        val storageImpl = implSpy.getOrSelectStorage()
        assertTrue(
            storageImpl is KeyValueStorageClearTextSharedPreferences,
            "Wrong impl was returned: ${storageImpl::class.simpleName}, " +
                "should have been ${KeyValueStorageClearTextSharedPreferences::class.simpleName}"
        )
    }

    /**
     * Test getOrSelectStorage() method when storage already selected vol3.
     */
    @Test
    public fun test_if_storage_is_selected_then_test_failed_does_not_count() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .storageCompatibilityTester(deviceTesterThatThrows)
            .build()
        val implSpy = spy(impl)
        // KeyValueStorageEncryptedSharedPreferences is last selected
        val encImpl = KeyValueStorageEncryptedSharedPreferences(getContextForTarget())
        doReturn(encImpl).whenever(implSpy).getLastSelectedStorageOrNullIfNoneSelectedYet()
        val storageImpl = implSpy.getOrSelectStorage()
        assertTrue(
            storageImpl is KeyValueStorageEncryptedSharedPreferences,
            "Wrong impl was returned: ${storageImpl::class.simpleName}, " +
                "should have been ${KeyValueStorageEncryptedSharedPreferences::class.simpleName}"
        )
    }

    /**
     * Test getOrSelectStorage() method when storage is not yet selected vol3.
     */
    @Test
    public fun test_if_storage_is_not_selected_then_test_failed_does_count() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .storageCompatibilityTester(deviceTesterThatThrows)
            .build()
        val implSpy = spy(impl)
        // Nothing selected
        val encImpl = null
        doReturn(encImpl).whenever(implSpy).getLastSelectedStorageOrNullIfNoneSelectedYet()
        val storageImpl = implSpy.getOrSelectStorage()
        assertTrue(
            storageImpl is KeyValueStorageClearTextSharedPreferences,
            "Wrong impl was returned: ${storageImpl::class.simpleName}, " +
                "should have been ${KeyValueStorageClearTextSharedPreferences::class.simpleName}"
        )
    }

    /**
     * Test getOrSelectStorage() method when storage already selected vol4.
     */
    @Test
    public fun test_if_storage_is_not_selected_and_encrypted_storage_is_allowed_then_it_is_returned() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .hardwareKeyStoreBasedStorageEncryptionEnabled(true)
            .storageCompatibilityTester(deviceTesterThatSucceeds)
            .build()
        val implSpy = spy(impl)
        // Nothing selected
        val encImpl = null
        doReturn(encImpl).whenever(implSpy).getLastSelectedStorageOrNullIfNoneSelectedYet()
        val storageImpl = implSpy.getOrSelectStorage()
        assertTrue(
            storageImpl is KeyValueStorageEncryptedSharedPreferences,
            "Wrong impl was returned: ${storageImpl::class.simpleName}, " +
                "should have been ${KeyValueStorageEncryptedSharedPreferences::class.simpleName}"
        )
    }

    /**
     * Test that deviceSupportsEncryptedStorage method does return false if the test fails.
     */
    @Test
    public fun test_device_does_not_support_encrypted_storage() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .storageCompatibilityTester(deviceTesterThatThrows)
            .build()
        assertFalse(
            impl.deviceSupportsEncryptedStorage(),
            "Device should not support encrypted storage when the test failed!"
        )
    }

    /**
     * Test that deviceSupportsEncryptedStorage method does return true if the test succeeds.
     */
    @Test
    public fun test_device_does_support_encrypted_storage() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .storageCompatibilityTester(deviceTesterThatSucceeds)
            .build()
        assertTrue(impl.deviceSupportsEncryptedStorage(), "Device support encrypted storage when test succeeded!")
    }

    /**
     * Test that hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice() method return true if this device is blocklisted.
     */
    @Test
    public fun test_blocklist_result_when_device_block_listed() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .hardwareKeyStoreBasedStorageEncryptionBlocklist("${Build.MANUFACTURER} ${Build.MODEL}")
            .build()
        assertTrue(impl.hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice())
    }

    /**
     * Test that hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice() method
     * return false if this device is not blacklisted by 1:1 Build.MANUFACTURER Build.MODEL match, only by Build.MANUFACTURER.
     */
    @Test
    public fun test_blocklist_result_when_only_manufacturer_is_block_listed() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .hardwareKeyStoreBasedStorageEncryptionBlocklist(Build.MANUFACTURER)
            .build()
        assertFalse(impl.hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice())
    }

    /**
     * Test that hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice() method return
     * false if this device is not blacklisted by 1:1 Build.MANUFACTURER Build.MODEL match, only by Build.MODEL.
     */
    @Test
    public fun test_blocklist_result_when_only_model_is_block_listed() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .hardwareKeyStoreBasedStorageEncryptionBlocklist(Build.MODEL)
            .build()
        assertFalse(impl.hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice())
    }

    /**
     * Test that hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice() method returns
     * true if the configuration is in all uppercase.
     */
    @Test
    public fun test_blocklist_result_when_different_case() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .hardwareKeyStoreBasedStorageEncryptionBlocklist("${Build.MANUFACTURER} ${Build.MODEL}".uppercase())
            .build()
        assertTrue(impl.hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice())
    }

    /**
     * Test that hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice() method returns
     * true if the configuration has extra white space at start.
     */
    @Test
    public fun test_blocklist_result_when_extra_whitespace() {
        val impl = LabEncryptedStorageManager.Builder(getContextForTarget())
            .hardwareKeyStoreBasedStorageEncryptionBlocklist(" ${Build.MANUFACTURER} ${Build.MODEL}")
            .build()
        assertTrue(impl.hardwareKeyStoreBasedStorageEncryptionDisabledForThisDevice())
    }
}

package mobi.lab.labencryptedstorage.impl

import mobi.lab.labencryptedstorage.BaseTestCase
import mobi.lab.labencryptedstorage.entity.KeyValueStorageException
import mobi.lab.labencryptedstorage.inter.KeyValueStorage
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Tests for [SimpleEncryptedStorageDeviceCompatibilityTester].
 */
public class SimpleEncryptedStorageDeviceCompatibilityTesterTest : BaseTestCase() {
    /**
     * Test that runTest() method does throw KeyValueStorageException if null is returned.
     */
    @Test(expected = KeyValueStorageException::class)
    public fun test_that_tester_throws_if_no_actual_storage() {
        val impl = SimpleEncryptedStorageDeviceCompatibilityTester()
        // Nothing is selected
        val storage: KeyValueStorage = mock()
        impl.runTest(getContextForTarget(), storage)
    }

    /**
     * Test that runTest() method does throw KeyValueStorageException if "" value is returned.
     */
    @Test(expected = KeyValueStorageException::class)
    public fun test_that_tester_throws_if_reads_fail() {
        val impl = SimpleEncryptedStorageDeviceCompatibilityTester()
        // Nothing is selected
        val storage: KeyValueStorage = mock()
        doReturn("").whenever(storage).read<String>(any(), any())
        impl.runTest(getContextForTarget(), storage)
    }
}

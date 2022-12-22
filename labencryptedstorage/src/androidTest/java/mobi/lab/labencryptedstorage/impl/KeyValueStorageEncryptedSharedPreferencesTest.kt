package mobi.lab.labencryptedstorage.impl

import android.os.Bundle
import com.google.gson.reflect.TypeToken
import mobi.lab.labencryptedstorage.BaseTestCase
import mobi.lab.labencryptedstorage.inter.KeyValueStorage
import org.junit.Assert
import org.junit.Test
import java.io.Serializable
import java.math.BigInteger

/**
 * Tests for {@link KeyValueStorageEncryptedSharedPreferences} class.
 */
public class KeyValueStorageEncryptedSharedPreferencesTest : BaseTestCase() {
    private fun createStorageImplementation(): KeyValueStorage {
        return KeyValueStorageEncryptedSharedPreferences(getContextForTarget())
    }

    /**
     * Test storage operations for String value store, retrieve, remove.
     */
    @Test
    public fun test_basic_string_store_and_read_and_delete() {
        val impl = createStorageImplementation()

        // Store something
        val key1 = "test_key1"
        val key2 = "test_tag2"
        val value1 = "value1"
        val value2 = "value2"
        impl.store(key1, value1)
        // Is it stored?
        Assert.assertEquals(value1, impl.read(key1, object : TypeToken<String?>() {}.type))

        // Can we overwrite?
        impl.store(key1, value2)
        Assert.assertEquals(value2, impl.read(key1, object : TypeToken<String?>() {}.type))

        // Is it only under that tag?
        Assert.assertNull(impl.read(key2, object : TypeToken<String?>() {}.type))

        // Can we delete?
        impl.delete(key1)
        Assert.assertNull(impl.read(key1, object : TypeToken<String?>() {}.type))
    }

    /**
     * Do multiple operations in linear order.
     */
    @Test
    public fun test_store_and_read_multiple_values() {
        val impl = createStorageImplementation()
        val max = 5
        for (i in 0 until max) {
            impl.store("tag$i", "value$i")
            if (i > 0) {
                Assert.assertEquals("value${i - 1}", impl.read("tag${i - 1}", object : TypeToken<String?>() {}.type))
                impl.delete("tag${i - 1}")
                Assert.assertEquals(null as String?, impl.read("tag${i - 1}" + (i - 1), object : TypeToken<String?>() {}.type))
            }
        }
        Assert.assertEquals("value${max - 1}", impl.read("tag${max - 1}", object : TypeToken<String?>() {}.type))
        impl.delete("tag${max - 1}")
        Assert.assertNull(impl.read("tag${max - 1}", object : TypeToken<String?>() {}.type))
    }

    /**
     * Store read and delete a BigInteger value.
     */
    @Test
    public fun test_store_and_read_and_delete_a_big_integer() {
        val lib = createStorageImplementation()
        val key = "test_key1"
        val value = BigInteger(
            "618430312143666956789162465473278885529581524111368392844806530231732554079152202334003837296485" +
                "884782967837115040798940965456422961770531628724113902466166452407256623522190356040552892821535" +
                "7295565477260980307213113484265046351909059812347201822768589804722376443712337181904824067128157" +
                "2090526385921750567331953515598499385253257688326837594745848780237974211796867677377183341271610" +
                "8739372782018655072248012451780419579864598090258724125593068131685593536710653174460156026070683" +
                "3786877338363502734425428908188256209162178041242614995480732063667506772238023873006792578337984" +
                "067206313529835248736628265157815164"
        )
        lib.store(key, value)
        Assert.assertEquals(value, lib.read(key, object : TypeToken<BigInteger>() {}.type))
        lib.delete(key)
        Assert.assertNull(lib.read(key, object : TypeToken<BigInteger?>() {}.type))
    }

    /**
     * Test storing an array.
     */
    @Test
    public fun test_store_and_read_and_delete_an_array() {
        val lib = createStorageImplementation()

        val key = "test_key1"
        val arrayOriginal: ArrayList<Pair<String, Int>> = ArrayList()
        val pair1 = Pair("One", 1)
        arrayOriginal.add(pair1)
        val pair2 = Pair("Two", 2)
        arrayOriginal.add(pair2)
        val pair3 = Pair("Three", 3)
        arrayOriginal.add(pair3)

        lib.store(key, arrayOriginal)
        val arrayStored: ArrayList<Pair<String, Int>> = lib.read(key, object : TypeToken<ArrayList<Pair<String, Int>>>() {}.type)!!

        Assert.assertEquals(pair1, arrayStored[0])
        Assert.assertEquals(pair2, arrayStored[1])
        Assert.assertEquals(pair3, arrayStored[2])
        lib.delete(key)
        val arrayStored2: ArrayList<Pair<String, Int>>? = lib.read(key, object : TypeToken<ArrayList<Pair<String, Int>>>() {}.type)
        Assert.assertNull(arrayStored2)
    }

    /**
     * Test storing a bundle with some basic primitives and with serializable.
     */
    @Test
    public fun test_store_and_retrieve_bundle() {
        val lib = createStorageImplementation()

        val key = "test_key1"
        val bundleOriginal = Bundle()
        bundleOriginal.putString("String", "test")
        bundleOriginal.putInt("Int", 42)
        bundleOriginal.putBoolean("bool", true)
        bundleOriginal.putSerializable("serial", Pair("123", "456"))

        lib.store(key, bundleOriginal)
        val bundleStored: Bundle = lib.read(key, object : TypeToken<Bundle>() {}.type)!!
        Assert.assertEquals("test", bundleStored.getString("String"))
        Assert.assertEquals(42, bundleStored.getInt("Int"))
        Assert.assertTrue(bundleStored.getBoolean("bool"))
        @Suppress("DEPRECATION")
        val serial1: Serializable? = bundleStored.getSerializable("serial")
        Assert.assertNull(serial1)

        lib.delete(key)
        Assert.assertNull(lib.read(key, object : TypeToken<Bundle?>() {}.type))
    }
}

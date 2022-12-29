package mobi.lab.labencryptedstorage.inter

import mobi.lab.labencryptedstorage.entity.KeyValueStorageException
import mobi.lab.labencryptedstorage.entity.SelectedStoragePersistenceId
import java.lang.reflect.Type

/**
 * Interface for storage operations.
 * Can be any implementation.
 */
public interface KeyValueStorage {

    /**
     * Store a value.
     *
     * @param key Key to use.
     * @param value Value to store.
     */
    @Throws(KeyValueStorageException::class)
    public fun store(key: String, value: Any?)

    /**
     * Read a stored value.
     *
     *  @param key Key of the stored value
     *  @return Value of type valueType or null
     */
    @Throws(KeyValueStorageException::class)
    public fun <T> read(key: String, valueType: Type): T?

    /**
     * Delete a stored value.
     *
     * @param key Key of the stored value.
     */
    @Throws(KeyValueStorageException::class)
    public fun delete(key: String)

    /**
     * Get the storage name.
     *
     * @return name
     */
    public fun getStorageName(): String

    /**
     * Unique id used to remember the storage choice.
     *
     * @return SelectedStoragePersistenceId
     */
    public fun getSelectedStoragePersistenceId(): SelectedStoragePersistenceId
}

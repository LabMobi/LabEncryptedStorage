package mobi.lab.labencryptedstorage.inter

/**
 * Storage manager interface.
 */
public interface LabEncryptedStorageManagerInterface {
    /**
     * Get the last selected KeyValueStorage impl or select one now.
     * NOTE: This creates side-artifacts!
     * Calling this will either just return the impl to the last used value, or will do the selection and remember it for next times.
     * This method is safe to call multiple times.
     *
     * @return KeyValueStorage
     */
    public fun getOrSelectStorage(): KeyValueStorage

    /**
     * Get the last selected KeyValueStorage or null.
     * NOTE: Does not do any selection itself,
     * only returns the selection if the selection is done already.
     * Useful to check if the selection has been done.
     *
     * @return KeyValueStorage or null if non selected
     */
    public fun getLastSelectedStorageOrNullIfNoneSelectedYet(): KeyValueStorage?

    /**
     * Get the [KeyValueClearTextStorage] supplied during [LabEncryptedStorageManagerInterface] object creation.
     * Added just-in-case, most likely you want to call [getOrSelectStorage] instead.
     *
     * @return KeyValueClearTextStorage
     */
    public fun getSuppliedClearTextStorageImplementation(): KeyValueClearTextStorage

    /**
     * Get the [KeyValueEncryptedStorage] supplied during [LabEncryptedStorageManagerInterface] object creation.
     * Added just-in-case, most likely you want to call [getOrSelectStorage] instead.
     *
     * @return KeyValueEncryptedStorage
     */
    public fun getSuppliedEncryptedStorageImplementation(): KeyValueEncryptedStorage
}

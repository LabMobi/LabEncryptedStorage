package mobi.lab.labencryptedstorage.entity

/**
 * Enum for remembering the selected storage.
 */
public enum class SelectedStoragePersistenceId {
    CLEAR_TEXT, ENCRYPTED_TEE_PREFERRED, ENCRYPTED_STRONG_BOX_PREFERRED;

    public companion object {
        /**
         * Convert a string to SelectedStoragePersistenceId or return null.
         */
        public fun byNameOrNull(input: String?): SelectedStoragePersistenceId? {
            return values().firstOrNull { it.name == input }
        }
    }
}

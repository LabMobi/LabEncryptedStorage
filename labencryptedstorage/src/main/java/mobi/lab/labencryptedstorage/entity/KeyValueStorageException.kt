package mobi.lab.labencryptedstorage.entity

import java.io.IOException

/**
 * Exception for KeyValueStorage failures.
 *
 * @property message Failure message
 * @property cause Throwable cause if any
 */
public sealed class KeyValueStorageException(message: String, cause: Throwable? = null) : IOException(message, cause) {
    /**
     * Exception for KeyValueStorage read failures.
     *
     * @property message Failure message
     * @property cause Throwable cause if any
     */
    public class ReadException(message: String, cause: Throwable? = null) : KeyValueStorageException(message, cause) {
        public companion object {
            private const val serialVersionUID: Long = 2486361801511789893L
        }
    }

    /**
     * Exception for KeyValueStorage store failures.
     *
     * @property message Failure message
     * @property cause Throwable cause if any
     */
    public class StoreException(message: String, cause: Throwable? = null) : KeyValueStorageException(message, cause) {
        public companion object {
            private const val serialVersionUID: Long = 3696965201118567248L
        }
    }

    /**
     * Exception for KeyValueStorage delete failures.
     *
     * @property message Failure message
     * @property cause Throwable cause if any
     */
    public class DeleteException(message: String, cause: Throwable? = null) : KeyValueStorageException(message, cause) {
        public companion object {
            private const val serialVersionUID: Long = -3673223347930691000L
        }
    }

    public companion object {
        private const val serialVersionUID: Long = 4056948796570965885L
    }
}

package mobi.lab.labencryptedstorage.entity

import java.io.IOException

public sealed class KeyValueStorageException(message: String, cause: Throwable? = null) : IOException(message, cause) {
    public class ReadException(message: String, cause: Throwable? = null) : KeyValueStorageException(message, cause) {
        public companion object {
            private const val serialVersionUID: Long = 2486361801511789893L
        }
    }

    public class StoreException(message: String, cause: Throwable? = null) : KeyValueStorageException(message, cause) {
        public companion object {
            private const val serialVersionUID: Long = 3696965201118567248L
        }
    }

    public class DeleteException(message: String, cause: Throwable? = null) : KeyValueStorageException(message, cause) {
        public companion object {
            private const val serialVersionUID: Long = -3673223347930691000L
        }
    }

    public companion object {
        private const val serialVersionUID: Long = 4056948796570965885L
    }
}

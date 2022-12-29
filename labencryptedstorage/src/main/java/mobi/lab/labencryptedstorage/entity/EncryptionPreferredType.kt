package mobi.lab.labencryptedstorage.entity

/**
 * The preferred type for the hardware element that contains the encryption keys.
 * Suggestion only, Android may decide otherwise if the preferred type is not available.
 */
public sealed interface EncryptionPreferredType {
    /**
     * TEE is preferred.
     * See https://source.android.com/docs/security/features/trusty
     * Supported by a wide range of Android devices.
     * Faster option than [StrongBox].
     */
    public object Tee : EncryptionPreferredType {
        override fun toString(): String {
            return "TEE preferred"
        }
    }

    /**
     * StrongBox Keymaster is preferred.
     * See https://developer.android.com/about/versions/pie/android-9.0#hardware-security-module
     * Supported by a limited set of devices.
     * Slower option than [StrongBox].
     * If not available then the system will default to [Tee].
     */
    public object StrongBox : EncryptionPreferredType {
        override fun toString(): String {
            return "StrongBox Keymaster preferred"
        }
    }
}

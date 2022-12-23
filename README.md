Mobi Lab / We make robots talk to humans

# Lab Encrypted Storage

NOTE: Get the latest version number from: https://central.sonatype.dev/namespace/mobi.lab.labencryptedstorage

Encrypted key-value storage library for Android. Uses Google's [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences) and [SharedPreferences](https://developer.android.com/training/data-storage/shared-preferences) as the backing storage in synchronous mode.

Main benefits of `LabEncryptedStorage` over using Google's `SharedPreferences` directly:

- Allows the user to select the preferred type of hardware Keystore usage (Strongbox, TEE).
- Automatically detects if `EncryptedSharedPreferences` works on a given device and automatically allows it to fall back to `SharedPreferences` instead. In the latter case, data storage still works, and the data will still have the protection of the application sandbox.
- Uses synchronous storage - every operation either succeeds or throws immediately. Useful in cases where data must be stored, or failure must be known immediately. 
- Reduces risks of directly depending on Google's alpha/beta level libraries like `EncryptedSharedPreferences`. Useful for libraries with extended code freezes or certified binaries.

## Requirements

`LabEncryptedStorage` works on Android 5.0+ (API level 21+). It requires the Android `compileSdkVersion` to be set to `android-33`.

## Usage

Use the `LabEncryptedStorageManager.Builder` to get the manager instance:

```kotlin
// Create a manager via the builder
val manager = with(LabEncryptedStorageManager.Builder(this)) {
	// Configure if needed
	// For example, allow hardware-key based encrypted storage
	hardwareKeyStoreBasedStorageEncryptionEnabled(true)
	// For example, add device that should not use encrypted storage ever
	// First device
	hardwareKeyStoreBasedStorageEncryptionBlocklist("Samsung FirstDevice")
	// Add multiple
	val blocklist = arrayOf("Google SomePixel", "Samsung SomeOtherDeviceModel")
	hardwareKeyStoreBasedStorageEncryptionBlocklist(*blocklist)
	// Build it
	build()
}
```

And then use the manager to select the storage implementation:

```kotlin
 // Get the best storage to be used
 val storage: KeyValueStorage = manager.getOrSelectStorage()
```

This two things you only need to do once during app runtime. For example, in you dependency injection logic or Application object. After that you can use the `KeyValueStorage` instance multiple times.

Now with the `KeyValueStorage` you can use the storage.

```kotlin
val key1 = "key1"
val value1a = "value1"

// Write a value
storage.store(key1, value1a)

// Read out the same value
val value1b = storage.read<String>(key1, String::class.java)

// Delete the value
storage.delete(key1)
```

NOTE: Do not use it directly from UI thread.

NOTE: If the storage operation fails then `KeyValueStorageException` will be thrown!

## Known Issues and considerations

### Not the best way to store Android Bundle objects

When storing an Android bundle the library will only be able to support a few primitives like Strings, Booleans, Integers, .. - see the `BundleTypeAdapterFactory.java`.

#### Be careful with automatic backup when using encrypted storage

Most likely you will want to disable backups entirely. You can do this via your application's Android Manifest.

In the Manifest you want to define the following configuration:

```xml
<application
    android:allowBackup="false"
    android:fullBackupContent="@xml/full_backup_content"
    android:dataExtractionRules="@xml/data_extraction_rules"
    ..>
</application>
```

and the configuration files themselves can be the following ones below.

#### File full_backup_content

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <!-- All SharedPreferences And EncryptedSharedPreferences are turned off for now for Android 11 and older -->
    <exclude domain="sharedpref" />
</full-backup-content>
```

#### File data_extraction_rules

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <!-- All SharedPreferences And EncryptedSharedPreferences are turned off for now for Android 12 and newer -->
    <cloud-backup>
        <exclude domain="sharedpref" path="."/>
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="."/>
    </device-transfer>
</data-extraction-rules>
```

## Releases

NOTE: Get the latest version number from: https://central.sonatype.dev/namespace/mobi.lab.labencryptedstorage

Available via Maven - https://repo1.maven.org/maven2/mobi/lab/labencryptedstorage/labencryptedstorage/ 

Add the dependency declaration to your module's `build.gradle` file, to the dependencies enclosure:

```groovy
dependencies {
// ..
    implementation "mobi.lab.labencryptedstorage:labencryptedstorage:LATEST_VERSION"
// ..
}
```

The artifact is available in Maven Central, make sure you add the repository definition:

```groovy
repositories {
    mavenCentral()
}
```

## Adding the library to a project

Until the library is published to Maven Central, you need the `.aar` artefact. Either build it locally or take it from GitHub Releases.

##### Gradle

Add the following to your main application module's `build.gradle` file:

```groovy
repositories {
   flatDir {
        dirs 'libs'
    }
}

dependencies {
    implementation(name: 'labencryptedstorage-1.0.0-release', ext: 'aar')
}
```

This assumes 2 things:
* The version is `1.0.0`
* The main application module has a `libs/` directory that contains the `labencryptedstorage-1.0.0-release.aar` artefact.

## Building the library

Full build can be done via:

```bash
.\gradlew buildAll
```

This builds all variants and runs all linters.

Code linters (Detekt, ktlint) can also be run separately via:

```bash
.\gradlew checkCode
```

NOTE: This skips the Android Lint as that takes a long time to run and rarely has something to say.

## Running instrumentation tests

```bash
.\gradlew connectedAndroidTest
```

## Contact

### Mobi Lab

Email: [hello@lab.mobi](mailto:hello@lab.mobi)

Twitter: https://mobile.twitter.com/LabMobi

Web: https://lab.mobi/

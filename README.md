Mobi Lab / We make robots talk to humans

# Lab Encrypted Storage

Encrypted key-value storage library for Android. Uses Google's [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences) and [SharedPreferences](https://developer.android.com/training/data-storage/shared-preferences) as the backing storage in synchronous mode.

Main benefits of `LabEncryptedStorage` over using Google's `SharedPreferences` directly:

- Allows the user to select the preferred type of hardware Keystore usage (Strongbox, TEE).
- Automatically detects if `EncryptedSharedPreferences` works on a given device and automatically allows it to fall back to `SharedPreferences` instead. In the latter case, data storage still works, and the data will still have the protection of the application sandbox.
- Uses synchronous storage - every operation either succeeds or throws immediately. Useful in cases where data must be stored, or failure must be known immediately. 
- Reduces risks of directly depending on Google's alpha/beta level libraries like `EncryptedSharedPreferences`. Useful for libraries with extended code freezes or certified binaries.

## Requirements

`LabEncryptedStorage` works on Android 5.0+ (API level 21+). But requires the Android `compileSdkVersion` to be set to `android-33`.

## Releases

TODO: Maven Central artifact will be available soon.

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
.\gradlew buildRelease
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

Web: https://lab.mobi/

Twitter: https://mobile.twitter.com/LabMobi

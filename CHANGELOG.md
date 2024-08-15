# Changelog

## [0.10.2] - 2024-08-15

### Changed
- Updated the storage operation so that if the commit() throws the root cause is propagated to the caller. Pull request contributed by Gennadi Kudrjavtsev (https://github.com/ydanneg)
- Updated target and compile SDK versions to 34
- Updated dependency versions for the build script and sample app


## [0.10.1] - 2023-05-23

### Changed

- Updated dependency versions. Jetpack Security Crypto dependency version updated to 1.1.0-alpha06.
 
## [0.10.0] - 2023-02-21

### Changed

- Breaking change: Removed the incomplete built-in support for storing Android Bundles.

## [0.9.6] - 2022-12-29

### Changed

- Breaking change: Added an option to set a preferred hardware keystore type - TEE or StrongBox Keymaster. Before it always defaulted to TEE.
- Breaking change: Removed the file and key prefixes added by the library.

## [0.9.5] - 2022-12-27

### Changed

- Breaking change: setRequestStrongBoxBacked is set to false for hardware backed storage by default. This will be configurable soon.
- Made the blocklist device manufacturer and model comparison safer.

## [0.9.4] - 2022-12-23

### Changed

- Removed the AppCompat dependency from the library, this was added accidentally.
- Made the file and key prefixes the library adds a lot shorter. Less chance to create too long file or key names for EncryptedSharedPreferences and SharedPreferences.

## [0.9.3] - 2022-12-23

### Changed

- Set the minSDK to API 21

## [0.9.2] - 2022-12-23

### Added
- First version of the library.

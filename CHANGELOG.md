# Changelog
## [0.9.5] - 2022-12-27
- Breaking change: setRequestStrongBoxBacked is set to false for hardware backed storage by default. This will be configurable soon.
- Made the blocklist device manufacturer and model comparison safer.

### Changed

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

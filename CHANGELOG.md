
## 3.1.3 (2019-27-01)

### Fixed
* `ColorPreference` crash on rotation changed

## 3.1.2 (2019-21-01)

### Fixed
* Inflating `preferences.xml` causes `java.lang.ClassCastException: android.content.res.XmlBlock$Parser cannot be cast to java.lang.AutoCloseable`

## 3.1.1 (2019-20-01)

### Enhancements
* Updated dependencies
* Version `3.1.0` and lower are no longer available to download

## 3.1.0 (2019-05-01)

### Enhancements
* Added preference key generator. With this feature, you don't need to save all preference keys into a constant class each time you modify `preferences.xml`

### Fixed
* `ListPreference` does not update summary correctly
* `FolderPreference` now maintains current path on orientation change

## 3.0.1 (2019-01-01)

### Enhancements
* Migrated to Kotlin
* New `ColorPreference`
* `FolderPreference` now supports API level 19 and lower

### Fixed
* Cannot extends from `PreferenceActivityMaterial` and `PreferenceFragmentMaterial`

## 2.1.0 (2018-12-16)

### Enhancements
* Now you can donate to this library

### Fixed
* Added proguard rules for `DatePreference` and `TimePreference`

## 2.0.0 (2018-12-18)
Version 2.0.0 contains breaking changes that migrates Support Library to AndroidX Jetpack. In the future, version 3 of this library will be written in Kotlin.

### Enhancements
* Added `FolderPreference`
* Added `DatePreference`
* Added `TimePreference`

### Fixed
* None

## 1.0.0 (2018-12-16)

### Enhancements
* Stable release

### Fixed
* None

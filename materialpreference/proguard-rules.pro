# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepnames class com.anggrayudi.materialpreference.Preference
-keepnames class com.anggrayudi.materialpreference.PreferenceScreen
-keepnames class com.anggrayudi.materialpreference.PreferenceCategory
-keepnames class com.anggrayudi.materialpreference.SeekBarPreference
-keepnames class com.anggrayudi.materialpreference.SeekBarDialogPreference
-keepnames class com.anggrayudi.materialpreference.RingtonePreference
-keepnames class com.anggrayudi.materialpreference.ListPreference
-keepnames class com.anggrayudi.materialpreference.IntegerListPreference
-keepnames class com.anggrayudi.materialpreference.MultiSelectListPreference
-keepnames class com.anggrayudi.materialpreference.EditTextPreference
-keepnames class com.anggrayudi.materialpreference.IndicatorPreference
-keepnames class com.anggrayudi.materialpreference.FolderPreference
-keepnames class com.anggrayudi.materialpreference.ColorPreference
-keepnames class com.anggrayudi.materialpreference.FilePreference
-keepnames class com.anggrayudi.materialpreference.TwoStatePreference

-keep class com.anggrayudi.materialpreference.CheckBoxPreference
-keep class com.anggrayudi.materialpreference.SwitchPreference
-keep class com.anggrayudi.materialpreference.TimePreference
-keep class com.anggrayudi.materialpreference.DatePreference

-keepclassmembers class * extends com.anggrayudi.materialpreference.Preference { public <init>(...); }

### Coroutines
# ServiceLoader support
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}

# Most of volatile fields are updated with AFU and should not be mangled
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
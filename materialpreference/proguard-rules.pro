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

-keep public class com.anggrayudi.materialpreference.Preference { *; }
-keep public class com.anggrayudi.materialpreference.PreferenceScreen { *; }
-keep public class com.anggrayudi.materialpreference.PreferenceCategory { *; }
-keep public class com.anggrayudi.materialpreference.SeekBarPreference { *; }
-keep public class com.anggrayudi.materialpreference.SeekBarDialogPreference { *; }
-keep public class com.anggrayudi.materialpreference.CheckBoxPreference { *; }
-keep public class com.anggrayudi.materialpreference.SwitchPreference { *; }
-keep public class com.anggrayudi.materialpreference.RingtonePreference { *; }
-keep public class com.anggrayudi.materialpreference.ListPreference { *; }
-keep public class com.anggrayudi.materialpreference.MultiSelectListPreference { *; }
-keep public class com.anggrayudi.materialpreference.EditTextPreference { *; }
-keep public class com.anggrayudi.materialpreference.IndicatorPreference { *; }
-keep public class com.anggrayudi.materialpreference.FolderPreference { *; }
-keep public class com.anggrayudi.materialpreference.FilePreference { *; }
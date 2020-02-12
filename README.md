# Material Preference
A library designed for people who love simplicity. Hate the old preference style? Try this library.

It combines libraries from `androidx.preference` and `net.xpece.android.support.preference`.
Available from API 17.
<br><a href="https://play.google.com/store/apps/details?id=com.anggrayudi.materialpreference.sample" target="_blank"><img alt="Google Play" height="80" src="https://play.google.com/intl/en_US/badges/images/generic/en_badge_web_generic.png" align="right"/></a><br><br><br>

## Screenshots

![Alt text](art/1-generic.png?raw=true "Material Preference")
![Alt text](art/2-generic.png?raw=true "Material Preference")
![Alt text](art/3-generic.png?raw=true "DatePreference")
![Alt text](art/4-generic.png?raw=true "ListPreference")

## Note

Even though Material Preference is built in Kotlin, but you can use this library in Java with a little setup in your `build.gradle`. Read section [Java compatibility support](https://github.com/anggrayudi/MaterialPreference#java-compatibility-support).

## Usage

### Basic [ ![jcenter](https://api.bintray.com/packages/anggrayudi/maven/materialpreference/images/download.svg)](https://bintray.com/anggrayudi/maven/materialpreference/_latestVersion)

```gradle
android {
    // add these lines
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
dependencies {
    implementation 'com.anggrayudi:materialpreference:3.5.2'
}
```

**Note:** Since v3.5.0, we only distribute this library through [OSS Sonatype Maven](https://oss.sonatype.org/#nexus-search;quick~com.anggrayudi).  
Distributing it on Bintray became more difficult because Bintray seem lazy to maintain their [Gradle Plugin](https://github.com/bintray/gradle-bintray-plugin/).

From your [`preferences.xml`](https://github.com/anggrayudi/MaterialPreference/blob/master/sample/src/main/res/xml/preferences.xml):

```xml
<PreferenceScreen xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- To make Preferences floating, you must wrap them inside PreferenceCategory -->
    <PreferenceCategory>
        <Preference
            android:key="about"
            android:title="About"
            android:icon="@drawable/..."
            app:tintIcon="?colorAccent"
            app:legacySummary="false"/>
    </PreferenceCategory>
</PreferenceScreen>
```

From your [`SettingsFragment`](https://github.com/anggrayudi/MaterialPreference/blob/master/sample/src/main/java/com/anggrayudi/materialpreference/sample/SettingsFragment.kt):

```kotlin
class SettingsFragment : PreferenceFragmentMaterial() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
    }

    companion object {
        fun newInstance(rootKey: String?) = SettingsFragment().apply {
            arguments = Bundle()
            arguments!!.putString(PreferenceFragmentMaterial.ARG_PREFERENCE_ROOT, rootKey)
        }
    }
}
```

From your [`SettingsActivity`](https://github.com/anggrayudi/MaterialPreference/blob/master/sample/src/main/java/com/anggrayudi/materialpreference/sample/SettingsActivity.kt):

```kotlin
class SettingsActivity : PreferenceActivityMaterial() {

    private var settingsFragment: SettingsFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        if (savedInstanceState == null) {
            settingsFragment = SettingsFragment.newInstance(null)
            supportFragmentManager.beginTransaction().add(R.id.fragment_container, settingsFragment!!, TAG).commit()
        } else {
            onBackStackChanged()
        }
    }

    override fun onBuildPreferenceFragment(rootKey: String?): PreferenceFragmentMaterial {
        return SettingsFragment.newInstance(rootKey)
    }

    override fun onBackStackChanged() {
        settingsFragment = supportFragmentManager.findFragmentByTag(TAG) as SettingsFragment?
        title = settingsFragment!!.preferenceFragmentTitle
    }
}
```

### Preference Migration

During app development, you may encounter a condition where you need to modify your current `SharedPreferences`, such as renaming a preference's key.
Material Preference v3.4.0 introduces a new feature called Preference Migration.
Define your migration plan by implementing `PreferenceMigration` interface:

```kotlin
class MyPreferenceMigration : PreferenceMigration {

    override fun migrate(plan: MigrationPlan, currentVersion: Int) {
        // Implement your migration plan here.
        // Read sample code in class App.kt for more information. 
    }

    override fun onMigrationCompleted(preferences: SharedPreferences) {
    }
    
    companion object {
        const val PREFERENCE_VERSION = 3
    }
}
```

Finally, start the migration:

```kotlin
PreferenceMigration.setupMigration(MyPreferenceMigration(), preferences, PREFERENCE_VERSION)
```

### Preference Key Constants Generator [ ![jcenter](https://api.bintray.com/packages/anggrayudi/maven/materialpreference-compiler/images/download.svg)](https://bintray.com/anggrayudi/maven/materialpreference-compiler/_latestVersion)

Material Preference has a capability to auto-generate your preference keys in a constant class. By default, this class is named `PrefKey`. With this generator, you don't need to rewrite constant field each time you modify preference key from file `res/xml/preferences.xml`. It improves accuracy in writing constant values.

To enable this feature, simply add the following configuration to your `build.gradle`:

````gradle
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'
apply plugin: 'kotlin-kapt' // Add this line

dependencies {
    implementation 'com.anggrayudi:materialpreference:3.x.x'
    kapt 'com.anggrayudi:materialpreference-compiler:1.6'
}
````

From your `SettingsFragment` class:

````kotlin
@PreferenceKeysConfig // Add this annotation
class SettingsFragment : PreferenceFragmentMaterial() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)
        // You can access the constant values with auto-generated class named PrefKey
        findPreference(PrefKey.ABOUT)?.summary = BuildConfig.VERSION_NAME

        val volume = findPreferenceAs<SeekBarPreference>(PrefKey.NOTIFICATION_VOLUME)
        volume?.summaryFormatter = { "$it%" }
    }
}
````

**Note:**
* If `PrefKey` does not update constant fields, click ![Alt text](art/make-project.png?raw=true "Make Project") Make Project in Android Studio.
* This generator wont work with Android Studio 3.3.0 Stable, 3.4 Beta 3, and 3.5 Canary 3 because of [this bug](https://issuetracker.google.com/issues/122883561). The fixes are available in the next version of Android Studio.

### SharedPreferencesHelper

Since v3.5.0, the annotation processor will generate `SharedPreferencesHelper`, so you don't need to retrieve `SharedPreferences` value like this: `SharedPreferences.get<DataType>(key, defaultValue)`.
Take advantage of using it with dependency injection such as [Dagger 2](https://github.com/google/dagger) and [Koin](https://github.com/InsertKoinIO/koin).
Personally, I would recommend you to use Koin because of its simplicity.

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ...
        val preferencesHelper = SharedPreferencesHelper(this)
        setTitle(preferencesHelper.accountName)
    }
}
```

From above code, `accountName` is a generated method based on your configuration on `preferences.xml`.
It takes `android:defaultValue` if method `accountName` can't find any value stored in the `SharedPreferences`.
You can customize the method name via `app:helperMethodName="yourMethodName"`. Read our sample code for more information.

### Java compatibility support

Kotlin is interoperable with Java. Just configure the following setup in your project.

#### Setup

In your `build.gradle` of project level:

````gradle
buildscript {
    // add this line
    ext.kotlin_version = '1.3.61'

    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.5.3'
        // add this line
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}
````

And for your app's `build.gradle`:

````gradle
dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlin_version"
    implementation 'com.anggrayudi:materialpreference:3.x.x'
}
````

#### Example

From your [`SettingsFragment.java`](https://github.com/anggrayudi/MaterialPreference/blob/master/sample/src/main/java/com/anggrayudi/materialpreference/sample/java/SettingsFragment.java):

````java
@PreferenceKeysConfig
public class SettingsFragment extends PreferenceFragmentMaterial {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        Preference preferenceAbout = findPreference(PrefKey.ABOUT);
        preferenceAbout.setSummary(BuildConfig.VERSION_NAME);

        SeekBarDialogPreference vibration = (SeekBarDialogPreference) findPreference(PrefKey.VIBRATE_DURATION);

        // summary formatter example
        vibration.setSummaryFormatter(new Function1<Integer, String>() {
            @Override
            public String invoke(Integer duration) {
                return duration + "ms";
            }
        });

        IndicatorPreference indicatorPreference = (IndicatorPreference) findPreference(PrefKey.ACCOUNT_STATUS);

        // click listener example
        indicatorPreference.setOnPreferenceClickListener(new Function1<Preference, Boolean>() {
            @Override
            public Boolean invoke(Preference preference) {
                new MaterialDialog(getContext())
                    .message(null, "Your account has been verified.", false, 1f)
                    .positiveButton(android.R.string.ok, null, null)
                    .show();
                return true;
            }
        });

        // long click listener example
        indicatorPreference.setOnPreferenceLongClickListener(new Function1<Preference, Boolean>() {
            @Override
            public Boolean invoke(Preference preference) {
                Toast.makeText(getContext(), "onLogClick: " + preference.getTitle(), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }
}
````

From your [`SettingsActivity.java`](https://github.com/anggrayudi/MaterialPreference/blob/master/sample/src/main/java/com/anggrayudi/materialpreference/sample/java/SettingsActivity.java):

````java
public class SettingsActivity extends PreferenceActivityMaterial {

    private static final String TAG = "Settings";

    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            settingsFragment = SettingsFragment.newInstance(null);
            getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, settingsFragment, TAG)
                .commit();
        } else {
            onBackStackChanged();
        }
    }

    @NotNull
    @Override
    protected PreferenceFragmentMaterial onBuildPreferenceFragment(@Nullable String rootKey) {
        return SettingsFragment.newInstance(rootKey);
    }

    @Override
    public void onBackStackChanged() {
        settingsFragment = (SettingsFragment) getSupportFragmentManager().findFragmentByTag(TAG);
        setTitle(settingsFragment.getPreferenceFragmentTitle());
    }
}
````

## Preferences

- `Preference`
- `CheckBoxPreference`
- `SwitchPreference`
- `EditTextPreference`
- `ListPreference`
- `IntegerListPreference`
- `MultiSelectListPreference`
- `SeekBarDialogPreference`
- `SeekBarPreference`
- `RingtonePreference`
- `IndicatorPreference`
- `FolderPreference`
- `DatePreference`
- `TimePreference`
- `ColorPreference`

### RingtonePreference

`RingtonePreference` will show only system ringtone sounds by default.
If you want to include sounds from the external storage your app needs to request
`android.permission.READ_EXTERNAL_STORAGE` permission in its manifest.
Don't forget to check this runtime permission before opening ringtone picker on API 23.

## Donation
Any donation you give is really helpful for us to develop this library. It feels like energy from power stone.

<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=TGPGSY66LKUMN&source=url" target="_blank"><img alt="Donate with PayPal" src="https://www.paypalobjects.com/en_US/i/btn/btn_donateCC_LG.gif" border="0"/></a>

## License

    Copyright 2018-2020 Anggrayudi Hardiannicko A.
 
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
 
        http://www.apache.org/licenses/LICENSE-2.0
 
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

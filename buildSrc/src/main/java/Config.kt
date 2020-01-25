/**
 * This class is being tested
 */
object Config {
    object Versions {
        const val kotlinVersion = "1.3.61"
        const val dialogVersion = "2.8.1"
        const val coroutinesVersion = "1.3.0"
    }

    object Android {
        const val appVersionCode = 14
        const val minSdkVersion = 17
        const val targetSdkVersion = 29
        const val compileSdkVersion = 29
        const val buildToolsVersion = "29.0.2"

        const val groupId = "com.anggrayudi"
        const val materialPreferenceVersion = "3.4.1"
        const val processorVersion = "1.1"
    }

    object Libs {
        const val Kotlin_stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlinVersion}"
        const val Xpp3 = "org.ogce:xpp3:1.1.6"
        const val XmlPullParser = "xmlpull:xmlpull:1.1.3.1"
        const val KotlinPoet = "com.squareup:kotlinpoet:1.5.0"

        const val AutoService = "com.google.auto.service:auto-service:1.0-rc6"
    }
}
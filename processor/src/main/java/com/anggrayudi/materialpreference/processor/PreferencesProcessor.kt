package com.anggrayudi.materialpreference.processor

import com.anggrayudi.materialpreference.annotation.PreferencesConfig
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStreamReader
import java.util.*
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(PreferencesProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class PreferencesProcessor : AbstractProcessor() {

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(PreferencesConfig::class.java).forEach {
            val generatedSourcesRoot: String? = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME]
            if (generatedSourcesRoot.isNullOrEmpty()) {
                processingEnv.messager.errorMessage { "Can't find the target directory for generated Kotlin files." }
                return false
            }

            // D:\YourProjectDirs\app\build\generated\source\kaptKotlin\debug
            val resFile = File(
                File(generatedSourcesRoot).parentFile.parentFile.parentFile.parentFile.parentFile,
                "src${File.separator}main${File.separator}res${File.separator}"
            )

            val a = it.getAnnotation(PreferencesConfig::class.java)
            val xmlRes = a.preferencesXmlRes.toXmlFileName()
            val xmlFile = File(resFile, "xml${File.separator}$xmlRes")
            if (!xmlFile.isFile) {
                processingEnv.messager.warningMessage { "Can't find file $xmlRes in path ${xmlFile.absolutePath}" }
                return false
            }

            val prefKeyClassBuilder = TypeSpec.objectBuilder(a.prefKeyClassName)

            val prefHelperClassBuilder = TypeSpec.classBuilder(a.prefHelperClassName)
                .primaryConstructor(FunSpec.constructorBuilder().addParameter("context", ClassName("android.content", "Context")).build())
                .addProperty(
                    PropertySpec.builder("preferences", ClassName("android.content", "SharedPreferences"))
                        .initializer("%T.getDefaultSharedPreferences(context)", ClassName("com.anggrayudi.materialpreference", "PreferenceManager"))
                        .build()
                )

            val specs = getPreferenceSpecs(xmlFile, a.capitalStyle)
            specs.forEach { spec ->
                prefKeyClassBuilder.addProperty(
                    PropertySpec.builder(spec.constantName, String::class, KModifier.CONST)
                        .initializer("%S", spec.key)
                        .build()
                )

                if (spec.dataType != PreferenceDataType.NOTHING && spec.methodName != null) {
                    val getterBuilder = FunSpec.getterBuilder()
                    val setterBuilder = FunSpec.setterBuilder()
                    val propertyBuilder: PropertySpec.Builder = when (spec.dataType) {
                        PreferenceDataType.STRING_SET -> {
                            val set = ClassName("kotlin.collections", "MutableSet")
                            val parameterSpec = set.parameterizedBy(String::class.asTypeName())

                            val defaultValue = getStringSetDefaultValue(spec)
                            if (defaultValue.isNotEmpty()) {
                                getterBuilder.addStatement(
                                    "return preferences.getStringSet(%S, mutableSetOf(%L)) ?: mutableSetOf(%L)",
                                    spec.key,
                                    defaultValue,
                                    defaultValue
                                )
                            } else {
                                getterBuilder.addStatement("return preferences.getStringSet(%S, mutableSetOf()) ?: mutableSetOf()", spec.key)
                            }

                            setterBuilder.addParameter("value", parameterSpec)
                                .addStatement("preferences.edit().putStringSet(%S, value).apply()", spec.key)

                            PropertySpec.builder(spec.methodName!!, parameterSpec)
                        }

                        PreferenceDataType.BOOLEAN -> {
                            if (spec.defaultValue != null) {
                                getterBuilder.addStatement("return preferences.getBoolean(%S, %L)", spec.key, spec.defaultValue!!)
                            } else {
                                getterBuilder.addStatement("return preferences.getBoolean(%S, false)", spec.key)
                            }

                            setterBuilder.addParameter("value", Boolean::class)
                                .addStatement("preferences.edit().putBoolean(%S, value).apply()", spec.key)

                            PropertySpec.builder(spec.methodName!!, Boolean::class)
                        }

                        PreferenceDataType.INTEGER -> {
                            if (spec.defaultValue != null) {
                                getterBuilder.addStatement("return preferences.getInt(%S, %L)", spec.key, spec.defaultValue!!)
                            } else {
                                getterBuilder.addStatement("return preferences.getInt(%S, 0)", spec.key)
                            }

                            setterBuilder.addParameter("value", Int::class)
                                .addStatement("preferences.edit().putInt(%S, value).apply()", spec.key)

                            PropertySpec.builder(spec.methodName!!, Int::class)
                        }

                        PreferenceDataType.FLOAT -> {
                            if (spec.defaultValue != null) {
                                getterBuilder.addStatement("return preferences.getFloat(%S, %L)", spec.key, spec.defaultValue!!)
                            } else {
                                getterBuilder.addStatement("return preferences.getFloat(%S, 0)", spec.key)
                            }

                            setterBuilder.addParameter("value", Float::class)
                                .addStatement("preferences.edit().putFloat(%S, value).apply()", spec.key)

                            PropertySpec.builder(spec.methodName!!, Float::class)
                        }

                        PreferenceDataType.LONG -> {
                            if (spec.defaultValue != null) {
                                getterBuilder.addStatement("return preferences.getLong(%S, %L)", spec.key, spec.defaultValue!!)
                            } else {
                                getterBuilder.addStatement("return preferences.getLong(%S, 0)", spec.key)
                            }

                            setterBuilder.addParameter("value", Long::class)
                                .addStatement("preferences.edit().putLong(%S, value).apply()", spec.key)

                            PropertySpec.builder(spec.methodName!!, Long::class)
                        }

                        PreferenceDataType.TIME -> {
                            val timeClass = ClassName("com.anggrayudi.materialpreference", "TimePreference").nestedClass("Time")
                            if (spec.defaultValue != null && spec.defaultValue != "@null") {
                                getterBuilder.addStatement("val time = preferences.getString(%S, %S) ?: %S", spec.key, spec.defaultValue!!, spec.defaultValue!!)
                                    .addStatement("return TimePreference.toTime(time)")

                                setterBuilder.addParameter("value", timeClass)
                                    .addStatement("preferences.edit().putString(%S, value.toString()).apply()", spec.key)

                                PropertySpec.builder(spec.methodName!!, timeClass)
                            } else {
                                getterBuilder.addStatement("val time = preferences.getString(%S, null)", spec.key)
                                    .addStatement("return if (time != null) TimePreference.toTime(time) else null", spec.key)

                                val nullableTime = timeClass.copy(nullable = true)
                                setterBuilder.addParameter("value", nullableTime)
                                    .addStatement("preferences.edit().putString(%S, value?.toString()).apply()", spec.key)

                                PropertySpec.builder(spec.methodName!!, nullableTime)
                            }
                        }

                        else -> {
                            if (spec.defaultValue != null && spec.defaultValue != "@null") {
                                getterBuilder.addStatement("return preferences.getString(%S, %S) ?: %S", spec.key, spec.defaultValue!!, spec.defaultValue!!)

                                setterBuilder.addParameter("value", String::class)
                                    .addStatement("preferences.edit().putString(%S, value).apply()", spec.key)

                                PropertySpec.builder(spec.methodName!!, String::class)
                            } else {
                                getterBuilder.addStatement("return preferences.getString(%S, null)", spec.key)

                                val parameterSpec = String::class.asTypeName().copy(nullable = true)
                                setterBuilder.addParameter("value", parameterSpec)
                                    .addStatement("preferences.edit().putString(%S, value).apply()", spec.key)

                                PropertySpec.builder(spec.methodName!!, parameterSpec)
                            }
                        }
                    }

                    propertyBuilder.mutable(true)
                        .getter(getterBuilder.build())
                        .setter(setterBuilder.build())

                    prefHelperClassBuilder.addProperty(propertyBuilder.build()).build()
                }
            }

            val withDefaultValue = specs.filter { spec -> spec.defaultValue != null || spec.dataType == PreferenceDataType.STRING_SET }
            if (withDefaultValue.isNotEmpty()) {
                val functionBuilder = FunSpec.builder("setDefaultPreferenceValues")
                    .addKdoc(
                        "All preferences that do not define `android:defaultValue` wont be included into this method." +
                                "\nBTW, you can use `@null` to define `null` value like this => `android:defaultValue=\"@null\"`"
                    )
                    .addAnnotation(JvmStatic::class)
                    .addParameter("context", ClassName("android.content", "Context"))
                    .addStatement("%T.getDefaultSharedPreferences(context).edit()", ClassName("com.anggrayudi.materialpreference", "PreferenceManager"))

                withDefaultValue.forEach { spec ->
                    when (spec.dataType) {
                        PreferenceDataType.STRING_SET -> {
                            val defaultValue = getStringSetDefaultValue(spec)
                            if (defaultValue.isEmpty()) {
                                functionBuilder.addStatement(".putStringSet(%S, emptySet())")
                            } else {
                                functionBuilder.addStatement(".putStringSet(%S, setOf(%L))", spec.key, defaultValue)
                            }
                        }

                        PreferenceDataType.BOOLEAN -> {
                            functionBuilder.addStatement(".putBoolean(%S, %L)", spec.key, spec.defaultValue!!)
                        }

                        PreferenceDataType.INTEGER -> {
                            functionBuilder.addStatement(".putInt(%S, %L)", spec.key, spec.defaultValue!!)
                        }

                        PreferenceDataType.LONG -> {
                            functionBuilder.addStatement(".putLong(%S, %L)", spec.key, spec.defaultValue!!)
                        }

                        PreferenceDataType.FLOAT -> {
                            functionBuilder.addStatement(".putFloat(%S, %L)", spec.key, spec.defaultValue!!)
                        }

                        else -> {
                            if (spec.defaultValue == "@null") {
                                functionBuilder.addStatement(".putString(%S, null)", spec.key)
                            } else {
                                functionBuilder.addStatement(".putString(%S, %S)", spec.key, spec.defaultValue!!)
                            }
                        }
                    }
                }
                functionBuilder.addStatement(".apply()")

                val companionBlock = TypeSpec.companionObjectBuilder()
                    .addFunction(functionBuilder.build())
                    .build()
                prefHelperClassBuilder.addType(companionBlock)
            }

            // Remove suffix ".java" from package com.anggrayudi.materialpreference.sample.java
            val packageName = processingEnv.elementUtils.getPackageOf(it).qualifiedName.toString().substringBeforeLast(".java")

            FileSpec.builder(packageName, a.prefKeyClassName)
                .addType(prefKeyClassBuilder.build())
                .build()
                .writeTo(File(generatedSourcesRoot).apply { mkdirs() })

            FileSpec.builder(packageName, a.prefHelperClassName)
                .addType(prefHelperClassBuilder.build())
                .build()
                .writeTo(File(generatedSourcesRoot))
            return false
        }
        return false
    }

    private fun getPreferenceSpecs(preferencesXmlFile: File, capitalStyle: Boolean): List<PreferenceSpec> {
        val parser = createXmlParser()
        val stream = InputStreamReader(preferencesXmlFile.inputStream())
        parser.setInput(stream)

        val preferenceSpecs = mutableListOf<PreferenceSpec>()

        var eventType = parser.eventType
        loopWhile@ while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType != XmlPullParser.START_TAG || parser.name == null) {
                eventType = parser.next()
                continue
            }

            var isBasePreference = false
            var includeBasePreference = false

            val spec = PreferenceSpec()
            spec.dataType = when (parser.name) {
                "EditTextPreference", "FolderPreference", "RingtonePreference",
                "ListPreference", "Preference" -> {
                    isBasePreference = parser.name == "Preference"
                    PreferenceDataType.STRING
                }

                "CheckBoxPreference", "SwitchPreference" -> PreferenceDataType.BOOLEAN

                "ColorPreference", "IntegerListPreference", "SeekBarDialogPreference",
                "SeekBarPreference" -> PreferenceDataType.INTEGER

                "TimePreference" -> PreferenceDataType.TIME

                "DatePreference" -> PreferenceDataType.LONG

                "MultiSelectListPreference" -> PreferenceDataType.STRING_SET

                "IndicatorPreference", "PreferenceCategory", "PreferenceScreen" -> PreferenceDataType.NOTHING

                else -> {
                    eventType = parser.next()
                    continue@loopWhile
                }
            }

            var needCreateMethodName = true
            for (i in 0 until parser.attributeCount) {
                when (parser.getAttributeName(i)) {
                    ATTR_KEY -> {
                        val key = parser.getAttributeValue(i)
                        if (key[0].isLetter()) {
                            spec.key = key
                            spec.constantName = createConstant(key, capitalStyle)
                        } else {
                            processingEnv.messager.errorMessage { "$key must be started with letter." }
                            return preferenceSpecs
                        }
                    }

                    ATTR_METHOD_NAME -> {
                        val methodName = parser.getAttributeValue(i).trim()
                        if (methodName.isNotEmpty()) {
                            spec.methodName = methodName
                            needCreateMethodName = false
                        }
                    }

                    ATTR_DEFAULT_VALUE -> {
                        spec.defaultValue = when (spec.dataType) {
                            PreferenceDataType.STRING, PreferenceDataType.TIME, PreferenceDataType.STRING_SET -> parser.getAttributeValue(i)
                            PreferenceDataType.BOOLEAN -> parser.getAttributeValue(i)?.toBoolean()
                            PreferenceDataType.INTEGER -> parser.getAttributeValue(i)?.replace("I", "")?.toInt()
                            PreferenceDataType.LONG -> parser.getAttributeValue(i)?.replace("L", "")?.toLong()
                            else -> null
                        }
                    }

                    ATTR_INCLUDE_TO_HELPER -> {
                        includeBasePreference = parser.getAttributeValue(i)?.toBoolean() ?: false
                    }
                }
            }

            if (spec.key.isNotBlank()) {
                if (needCreateMethodName) {
                    spec.methodName = createMethodName(spec.key, spec.dataType)
                }

                if (isBasePreference && !includeBasePreference) {
                    spec.methodName = null
                }

                if (spec.methodName != null && preferenceSpecs.any { it.methodName == spec.methodName }) {
                    processingEnv.messager.errorMessage { "Found duplicate method name: {spec.methodName}" }
                    return preferenceSpecs
                }

                preferenceSpecs.add(spec)
            }

            eventType = parser.next()
        }

        stream.close()
        return preferenceSpecs.distinctBy { it.key }
    }

    private fun createXmlParser() = XmlPullParserFactory.newInstance().run {
        isNamespaceAware = true
        newPullParser()
    }

    private fun createConstant(key: String, capitalStyle: Boolean): String {
        return if (!capitalStyle)
            key
        else if (key.contains("_")) {
            key.uppercase()
        } else {
            val resolvedKey = key.split(Regex("(?=[A-Z])")).joinToString("_").uppercase()
            if (resolvedKey.startsWith("_"))
                resolvedKey.replaceFirst("_", "")
            else
                resolvedKey
        }
    }

    private fun createMethodName(key: String, dataType: PreferenceDataType): String {
        val methodWords = (if (key.contains("_")) key.split("_") else key.split(Regex("(?=[A-Z])"))).filter { it.isNotBlank() }
        return if (dataType == PreferenceDataType.BOOLEAN) {
            "is" + methodWords.joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
        } else {
            methodWords[0].replaceFirstChar { it.lowercase() } + methodWords.takeLast(methodWords.size - 1)
                .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
        }
    }

    private fun getStringSetDefaultValue(spec: PreferenceSpec) = spec.defaultValue?.toString().orEmpty()
        .split(",")
        .map { s -> s.trim() }
        .filter { s -> s.isNotEmpty() }
        .joinToString(",") { s -> "\"$s\"" }

    private fun String.toXmlFileName() = if (endsWith(".xml")) this else "$this.xml"

    override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf(PreferencesConfig::class.java.canonicalName)

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

        private const val ATTR_KEY = "key"
        private const val ATTR_METHOD_NAME = "helperMethodName"
        private const val ATTR_DEFAULT_VALUE = "defaultValue"
        private const val ATTR_INCLUDE_TO_HELPER = "includeToHelper"
    }
}

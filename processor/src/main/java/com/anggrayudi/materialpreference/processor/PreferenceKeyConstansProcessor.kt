package com.anggrayudi.materialpreference.processor

import com.anggrayudi.materialpreference.annotation.PreferenceKeysConfig
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStreamReader
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(PreferenceKeyConstansProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class PreferenceKeyConstansProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        roundEnv.getElementsAnnotatedWith(PreferenceKeysConfig::class.java).forEach {
            val generatedSourcesRoot: String = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()
            if(generatedSourcesRoot.isEmpty()) {
                processingEnv.messager.errorMessage { "Can't find the target directory for generated Kotlin files." }
                return false
            }

            // D:\YourProjectDirs\app\build\generated\source\kaptKotlin\debug
            val resFile = File(File(generatedSourcesRoot).parentFile.parentFile.parentFile.parentFile.parentFile,
                    "src${File.separator}main${File.separator}res${File.separator}")

            val a = it.getAnnotation(PreferenceKeysConfig::class.java)
            val xmlRes = if (a.xmlResName.endsWith(".xml")) a.xmlResName else a.xmlResName + ".xml"
            val xmlFile = File(resFile, "xml${File.separator}$xmlRes")
            if (!xmlFile.isFile) {
                processingEnv.messager.warningMessage { "Can't find file $xmlRes in path ${xmlFile.absolutePath}" }
                return false
            }
            val strRes = if (a.stringResName.endsWith(".xml")) a.stringResName else a.stringResName + ".xml"

            val o = TypeSpec.objectBuilder(a.className)
            getPreferenceKeys(xmlFile, File(resFile, "values${File.separator}$strRes"), a.capitalStyle).forEach { map ->
                o.addProperty(PropertySpec.builder(map.value, String::class, KModifier.CONST)
                        .initializer("%S", map.key)
                        .build())
            }
            val targetPath = File(generatedSourcesRoot)
            targetPath.mkdirs()
            val packageName = processingEnv.elementUtils.getPackageOf(it).qualifiedName.toString()
            FileSpec.builder(packageName, a.className)
                    .addType(o.build())
                    .build()
                    .writeTo(targetPath)
            return false
        }
        return false
    }

    private fun getPreferenceKeys(file: File, stringFile: File, capitalStyle: Boolean): Map<String, String> {
        val keys = mutableMapOf<String, String>()
        val stringResKeys = mutableListOf<String>()
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        var stream = InputStreamReader(file.inputStream())
        parser.setInput(stream)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name != null) {
                for (i in 0 until parser.attributeCount) {
                    if (parser.getAttributeName(i) == "key") {
                        val key = parser.getAttributeValue(i)
                        if (key.startsWith("@string/")) {
                            stringResKeys.add(key.replace("@string/", ""))
                        } else if (!key[0].isLetter()) {
                            processingEnv.messager.errorMessage { "$key should be started with letter" }
                            return keys
                        } else {
                            keys[key] = createConstant(key, capitalStyle)
                        }
                        break
                    }
                }
            }
            eventType = parser.next()
        }
        stream.close()
        if (stringResKeys.isEmpty()){
            return keys
        } else if (!stringFile.isFile) {
            processingEnv.messager.errorMessage { "Can't find file ${stringFile.name} in path ${stringFile.absolutePath}" }
            return keys
        }

        stream = InputStreamReader(stringFile.inputStream())
        parser.setInput(stream)
        eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name != null && stringResKeys.isNotEmpty()) {
                if (parser.name == "string") {
                    for (i in 0 until parser.attributeCount) {
                        if (parser.getAttributeName(i) == "name" && stringResKeys.contains(parser.getAttributeValue(i))) {
                            stringResKeys.remove(parser.getAttributeValue(i))
                            parser.next()
                            val originalKey = parser.text
                            val key = originalKey.replace("\\s+".toRegex(), "")
                            if (!key[0].isLetter()) {
                                processingEnv.messager.errorMessage { "$key should be started with letter" }
                                return keys
                            } else {
                                keys[originalKey] = createConstant(key, capitalStyle)
                            }
                            break
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        stream.close()
        return keys
    }

    private fun createConstant(key: String, capitalStyle: Boolean): String {
        if (!capitalStyle)
            return key
        return if (key.contains("_")) {
            key.toUpperCase()
        } else {
            val resolvedKey = key.split(Regex("(?=[A-Z])")).joinToString("_").toUpperCase()
            if (resolvedKey.startsWith("_"))
                resolvedKey.replaceFirst("_", "")
            else
                resolvedKey
        }
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String>
            = mutableSetOf(PreferenceKeysConfig::class.java.canonicalName)
}
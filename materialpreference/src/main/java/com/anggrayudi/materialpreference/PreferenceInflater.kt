/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.anggrayudi.materialpreference

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Xml
import android.view.InflateException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.lang.reflect.Constructor
import java.util.*

/**
 * The [PreferenceInflater] is used to inflate preference hierarchies from XML files.
 */
internal class PreferenceInflater(val context: Context, private val preferenceManager: PreferenceManager) {

    private val mConstructorArgs = arrayOfNulls<Any>(2)

    /**
     * Sets the default package that will be searched for classes to construct
     * for tag names that have no explicit package.
     *
     * @return Default package, or null if it is not set.
     */
    var defaultPackages: Array<String>? = null

    init {
        defaultPackages = arrayOf("com.anggrayudi.materialpreference.")
    }

    /**
     * Inflate a new item hierarchy from the specified xml resource.
     * Throws `InflaterException` if there is an error.
     *
     * @param resource ID for an XML resource to load (e.g., `R.layout.main_page`)
     * @param root Optional parent of the generated hierarchy.
     * @return The root of the inflated hierarchy. If root was supplied,
     * this is the root item; otherwise it is the root of the inflated XML file.
     */
    fun inflate(resource: Int, root: PreferenceGroup?): Preference {
        context.resources.getXml(resource).use { parser ->
            return inflate(parser, root)
        }
    }

    /**
     * Inflate a new hierarchy from the specified XML node. Throws
     * `InflaterException` if there is an error.
     *
     * ***Important***&nbsp;&nbsp;&nbsp;For performance
     * reasons, inflation relies heavily on pre-processing of XML files
     * that is done at build time. Therefore, it is not currently possible to
     * use inflater with an XmlPullParser over a plain XML file at runtime.
     *
     * @param parser XML dom node containing the description of the hierarchy.
     * @param root Optional to be the parent of the generated hierarchy (if
     * *attachToRoot* is true), or else simply an object that
     * provides a set of values for root of the returned hierarchy (if *attachToRoot* is false.)
     * @return The root of the inflated hierarchy. If root was supplied,
     * this is root; otherwise it is the root of the inflated XML file.
     */
    fun inflate(parser: XmlPullParser, root: PreferenceGroup?): Preference {
        synchronized(mConstructorArgs) {
            val attrs = Xml.asAttributeSet(parser)
            mConstructorArgs[0] = context
            val result: Preference

            try {
                // Look for the root node.
                var type: Int
                do {
                    type = parser.next()
                } while (type != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT)

                if (type != XmlPullParser.START_TAG) {
                    throw InflateException(parser.positionDescription + ": No start tag found!")
                }

                // Temp is the root that was found in the xml
                val xmlRoot = createItemFromTag(parser.name, attrs)

                result = onMergeRoots(root, xmlRoot as PreferenceGroup)

                // Inflate all children under temp
                rInflate(parser, result, attrs)

            } catch (e: InflateException) {
                throw e
            } catch (e: XmlPullParserException) {
                val ex = InflateException(e.message)
                ex.initCause(e)
                throw ex
            } catch (e: IOException) {
                val ex = InflateException(parser.positionDescription + ": " + e.message)
                ex.initCause(e)
                throw ex
            }

            return result
        }
    }

    private fun onMergeRoots(givenRoot: PreferenceGroup?, xmlRoot: PreferenceGroup): PreferenceGroup {
        // If we were given a Preferences, use it as the root (ignoring the root
        // Preferences from the XML file).
        return if (givenRoot == null) {
            xmlRoot.onAttachedToHierarchy(preferenceManager)
            xmlRoot
        } else {
            givenRoot
        }
    }

    /**
     * Low-level function for instantiating by name. This attempts to
     * instantiate class of the given <var>name</var> found in this inflater's ClassLoader.
     *
     * There are two things that can happen in an error case: either the
     * exception describing the error will be thrown, or a null will be
     * returned. You must deal with both possibilities -- the former will happen
     * the first time createItem() is called for a class of a particular name,
     * the latter every time there-after for that class name.
     *
     * @param name The full name of the class to be instantiated.
     * @param attrs The XML attributes supplied for this instance.
     * @return The newly instantiated item, or null.
     */
    @Throws(ClassNotFoundException::class, InflateException::class)
    private fun createItem(name: String, prefixes: Array<String>?, attrs: AttributeSet): Preference {
        var constructor: Constructor<*>? = CONSTRUCTOR_MAP[name]

        try {
            if (constructor == null) {
                // Class not found in the cache, see if it's real,
                // and try to add it
                val classLoader = context.classLoader
                var clazz: Class<*>? = null
                if (prefixes == null || prefixes.isEmpty()) {
                    clazz = classLoader.loadClass(name)
                } else {
                    var notFoundException: ClassNotFoundException? = null
                    for (prefix in prefixes) {
                        try {
                            clazz = classLoader.loadClass(prefix + name)
                            break
                        } catch (e: ClassNotFoundException) {
                            notFoundException = e
                        }

                    }
                    if (clazz == null) {
                        if (notFoundException == null) {
                            throw InflateException(attrs.positionDescription + ": Error inflating class " + name)
                        } else {
                            throw notFoundException
                        }
                    }
                }
                constructor = clazz!!.getConstructor(*CONSTRUCTOR_SIGNATURE)
                constructor!!.isAccessible = true
                CONSTRUCTOR_MAP[name] = constructor
            }

            val args = mConstructorArgs
            args[1] = attrs
            return constructor.newInstance(*args) as Preference

        } catch (e: ClassNotFoundException) {
            // If loadClass fails, we should propagate the exception.
            throw e
        } catch (e: Exception) {
            val ie = InflateException(attrs.positionDescription + ": Error inflating class " + name)
            ie.initCause(e)
            throw ie
        }
    }

    /**
     * This routine is responsible for creating the correct subclass of item
     * given the xml element name. Override it to handle custom item objects. If
     * you override this in your subclass be sure to call through to
     * `super.onCreateItem(name)` for names you do not recognize.
     *
     * @param name The fully qualified class name of the item to be create.
     * @param attrs An `AttributeSet` of attributes to apply to the item.
     * @return The item created.
     */
    @Throws(ClassNotFoundException::class)
    protected fun onCreateItem(name: String, attrs: AttributeSet): Preference {
        return createItem(name, defaultPackages, attrs)
    }

    private fun createItemFromTag(name: String, attrs: AttributeSet): Preference {
        try {
            return if (-1 == name.indexOf('.')) {
                onCreateItem(name, attrs)
            } else {
                createItem(name, null, attrs)
            }
        } catch (e: InflateException) {
            throw e
        } catch (e: ClassNotFoundException) {
            val ie = InflateException(attrs.positionDescription + ": Error inflating class (not found)" + name)
            ie.initCause(e)
            throw ie
        } catch (e: Exception) {
            val ie = InflateException(attrs.positionDescription + ": Error inflating class " + name)
            ie.initCause(e)
            throw ie
        }
    }

    /**
     * Recursive method used to descend down the xml hierarchy and instantiate
     * items, instantiate their children, and then call `onFinishInflate()`.
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun rInflate(parser: XmlPullParser, parent: Preference, attrs: AttributeSet) {
        val depth = parser.depth

        var type = parser.next()
        while ((type != XmlPullParser.END_TAG || parser.depth > depth) && type != XmlPullParser.END_DOCUMENT) {

            if (type != XmlPullParser.START_TAG) {
                type = parser.next()
                continue
            }

            val name = parser.name

            when {
                INTENT_TAG_NAME == name -> {
                    val intent: Intent

                    try {
                        intent = Intent.parseIntent(context.resources, parser, attrs)
                    } catch (e: IOException) {
                        val ex = XmlPullParserException("Error parsing preference")
                        ex.initCause(e)
                        throw ex
                    }
                    parent.intent = intent
                }
                EXTRA_TAG_NAME == name -> {
                    context.resources.parseBundleExtra(EXTRA_TAG_NAME, attrs, parent.extras)
                    try {
                        skipCurrentTag(parser)
                    } catch (e: IOException) {
                        val ex = XmlPullParserException("Error parsing preference")
                        ex.initCause(e)
                        throw ex
                    }
                }
                else -> {
                    val item = createItemFromTag(name, attrs)
                    (parent as PreferenceGroup).addItemFromInflater(item)
                    rInflate(parser, item, attrs)
                }
            }
            type = parser.next()
        }
    }

    companion object {
        private const val TAG = "PreferenceInflater"

        private val CONSTRUCTOR_SIGNATURE = arrayOf(Context::class.java, AttributeSet::class.java)

        private val CONSTRUCTOR_MAP = HashMap<String, Constructor<*>>()

        private const val INTENT_TAG_NAME = "intent"
        private const val EXTRA_TAG_NAME = "extra"

        @Throws(XmlPullParserException::class, IOException::class)
        private fun skipCurrentTag(parser: XmlPullParser) {
            val outerDepth = parser.depth
            var type: Int
            do {
                type = parser.next()
            } while (type != XmlPullParser.END_DOCUMENT && (type != XmlPullParser.END_TAG || parser.depth > outerDepth))
        }
    }
}

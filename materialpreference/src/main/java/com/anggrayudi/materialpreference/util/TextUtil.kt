package com.anggrayudi.materialpreference.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.widget.Toast
import androidx.annotation.WorkerThread


/**
 * @author Anggrayudi H on 02/11/2015.
 */
object TextUtil {

    //    public static final String[] PROHIBITED_FILE_NAME = {"\\", "/", ":", "*", "?", "\"", "<", ">", "|"};

    fun removeEndCharacter(character: String, end: String): String {
        var character = character
        if (character.isNotEmpty() && character.endsWith(end)) {
            character = character.substring(0, character.length - end.length)
        }
        return character
    }

    fun removeEndCharacter(character: String, totalCharToRemove: Int): String {
        var character = character
        if (character.isNotEmpty() && totalCharToRemove > 0) {
            character = character.substring(0, character.length - totalCharToRemove)
        }
        return character
    }

    fun getStartCharacterByIndex(character: String, index: String): String {
        return removeEndCharacter(character, index + getLastCharacterByIndex(character, index))
    }

    fun replaceEndCharacter(character: String, end: String): String {
        return removeEndCharacter(character, end) + end
    }

    fun getLastCharacterByIndex(character: String, lastIndex: String): String {
        return character.substring(character.lastIndexOf(lastIndex) + 1)
    }

    fun getLastCharacter(character: String, totalCharacterToGet: Int): String {
        return character.substring(character.length - totalCharacterToGet)
    }

    @WorkerThread
    fun backgroundToast(handler: Handler?, context: Context, text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
        handler?.post { Toast.makeText(context, text, duration).show() }
    }

    fun isEmpty(s: String?): Boolean {
        return s == null || s.trim { it <= ' ' }.isEmpty()
    }

    fun getStringBetween(str: String, start: Char, end: Char): String {
        if (start != end) {
            for (i in str.indexOf(end) - 1 downTo 0) {
                if (str[i] == start) {
                    return str.substring(i + 1, str.indexOf(end))
                }
            }
        } else {
            for (i in str.indexOf(end) + 1 until str.length) {
                if (str[i] == start) {
                    return str.substring(str.indexOf(start) + 1, i)
                }
            }
        }
        return ""
    }

    fun copyToClipboard(context: Context, text: String, label: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.primaryClip = clip
    }

    fun validUrl(url: String): Boolean {
        val splitted = url.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return ((url.startsWith("http://") || url.startsWith("https://"))
                && splitted.size > 3
                && splitted[2].split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().size > 1)
    }

    fun equalsAny(str: String, vararg strings: String): Boolean {
        for (s in strings) {
            if (str == s)
                return true
        }
        return false
    }

    fun equalsAnyIgnoreCase(str: String, vararg strings: String): Boolean {
        for (s in strings) {
            if (str.equals(s, ignoreCase = true))
                return true
        }
        return false
    }

    fun endsWithAny(str: String?, vararg strings: String): Boolean {
        if (str != null) {
            for (s in strings) {
                if (str.endsWith(s))
                    return true
            }
        }
        return false
    }

    fun containsAny(str: String?, vararg strings: String): Boolean {
        if (str != null) {
            for (s in strings) {
                if (str.contains(s))
                    return true
            }
        }
        return false
    }

    fun arrayContains(a: Array<Int>, i: Int?): Boolean {
        for (x in a)
            if (i == x)
                return true
        return false
    }

    fun startsWithAny(str: String, vararg a: String): Boolean {
        for (s in a) {
            if (str.startsWith(s))
                return true
        }
        return false
    }

    fun substringAfter(str: String, word: String): String {
        if (str.length >= word.length) {
            var temp: String
            for (i in 0 until str.length) {
                temp = str.substring(i)
                if (temp.startsWith(word))
                    return temp.substring(word.length)
            }
        }
        return str
    }

    fun ordinalIndexOf(str: String, substr: String, n: Int): Int {
        var n = n
        var pos = str.indexOf(substr)
        while (--n > 0 && pos != -1)
            pos = str.indexOf(substr, pos + 1)
        return pos
    }
}

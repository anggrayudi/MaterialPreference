package com.anggrayudi.materialpreference.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;
import android.widget.Toast;


/**
 * Created by Nicko on 02/11/2015.
 */
public final class TextUtil {

//    public static final String[] PROHIBITED_FILE_NAME = {"\\", "/", ":", "*", "?", "\"", "<", ">", "|"};

    public static String removeEndCharacter(String character, String end) {
        if (character.length() > 0 && character.endsWith(end)) {
            character = character.substring(0, character.length() - end.length());
        }
        return character;
    }

    public static String removeEndCharacter(String character, int totalCharToRemove) {
        if (character.length() > 0 && totalCharToRemove > 0) {
            character = character.substring(0, character.length() - totalCharToRemove);
        }
        return character;
    }

    public static String getStartCharacterByIndex(String character, String index) {
        return removeEndCharacter(character, index + getLastCharacterByIndex(character, index));
    }

    public static String replaceEndCharacter(String character, String end) {
        return removeEndCharacter(character, end) + end;
    }

    public static String getLastCharacterByIndex(String character, String lastIndex) {
        return character.substring(character.lastIndexOf(lastIndex) + 1);
    }

    public static String getLastCharacter(String character, int totalCharacterToGet) {
        return character.substring(character.length() - totalCharacterToGet);
    }

    @WorkerThread
    public static void backgroundToast(Handler handler, final Context context, final CharSequence text, final int duration) {
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    public static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static String getStringBetween(String str, char start, char end) {
        if (start != end) {
            for (int i = str.indexOf(end) - 1; i >= 0; i--) {
                if (str.charAt(i) == start) {
                    return str.substring(i + 1, str.indexOf(end));
                }
            }
        } else {
            for (int i = str.indexOf(end) + 1; i < str.length(); i++) {
                if (str.charAt(i) == start) {
                    return str.substring(str.indexOf(start) + 1, i);
                }
            }
        }
        return "";
    }

    public static void copyToClipboard(Context context, String text, String label) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }

    public static boolean validUrl(String url) {
        String[] splitted = url.split("/");
        return (url.startsWith("http://") || (url.startsWith("https://")))
                && splitted.length > 3
                && splitted[2].split("\\.").length > 1;
    }

    public static void toastError(Context context, @StringRes int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        toast.getView().setBackgroundColor(Color.parseColor("#e02b00"));
        toast.show();
    }

    public static void toastWarning(Context context, @StringRes int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_SHORT);
        toast.getView().setBackgroundColor(Color.parseColor("#D99A13"));
        toast.show();
    }

    public static boolean equalsAny(String str, String... strings) {
        for (String s : strings) {
            if (str.equals(s))
                return true;
        }
        return false;
    }

    public static boolean equalsAnyIgnoreCase(String str, String... strings) {
        for (String s : strings) {
            if (str.equalsIgnoreCase(s))
                return true;
        }
        return false;
    }

    public static boolean endsWithAny(String str, String... strings) {
        if (str != null) {
            for (String s : strings) {
                if (str.endsWith(s))
                    return true;
            }
        }
        return false;
    }

    public static boolean containsAny(String str, String... strings) {
        if (str != null) {
            for (String s : strings) {
                if (str.contains(s))
                    return true;
            }
        }
        return false;
    }

    public static boolean arrayContains(Integer[] a, Integer i) {
        for (int x : a)
            if (i == x)
                return true;
        return false;
    }

    public static boolean startsWithAny(String str, String... a) {
        for (String s : a) {
            if (str.startsWith(s))
                return true;
        }
        return false;
    }

    public static String substringAfter(String str, String word) {
        if (str.length() >= word.length()) {
            String temp;
            for (int i = 0; i < str.length(); i++) {
                temp = str.substring(i);
                if (temp.startsWith(word))
                    return temp.substring(word.length());
            }
        }
        return str;
    }

    public static int ordinalIndexOf(String str, String substr, int n) {
        int pos = str.indexOf(substr);
        while (--n > 0 && pos != -1)
            pos = str.indexOf(substr, pos + 1);
        return pos;
    }
}

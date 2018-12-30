package com.anggrayudi.materialpreference.sample

import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {

    @Test
    fun uji() {
//        println("Uji: $isLegacySummary")
//        isLegacySummary = true
//        println("Uji: $isLegacySummary")

        val hexColor = String.format("#%06X", 0xFFFFFF and -16711936)
        print(hexColor)
    }

    var isLegacySummary: Boolean = false
        set(value) {
            field = value
            println("Value $value")
        }
}
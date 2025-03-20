// File: TextUtilsTest.kt
package com.gentestrana.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class TextUtilsTest {

    @Test
    fun `removeSpaces should remove extra spaces and trim`() {
        val input = "   ciao   mondo  "
        val expected = "ciao mondo"
        assertEquals(expected, removeSpaces(input))
    }

    @Test
    fun `removeSpaces should remove multiple newlines`() {
        val input = "ciao\n\n\n\n"
        val expected = "ciao"
        assertEquals(expected, removeSpaces(input))
    }

    @Test
    fun `removeSpaces should remove spaces and newlines together`() {
        val input = "   ciao  \n\n\n\n mondo   "
        val expected = "ciao mondo"
        assertEquals(expected, removeSpaces(input))
    }
}

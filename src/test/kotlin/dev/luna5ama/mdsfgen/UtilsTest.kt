package dev.luna5ama.mdsfgen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.acos
import kotlin.math.sqrt

class UtilsTest {
    private val EPSILON = 51e-3f

    @Test
    fun nonZeroSignTest() {
        assertEquals(-1, nonZeroSign(-114514.0f))
        assertEquals(-1, nonZeroSign(-1.0f))
        assertEquals(-1, nonZeroSign(-0.1f))
        assertEquals(1, nonZeroSign(0.0f))
        assertEquals(1, nonZeroSign(0.1f))
        assertEquals(1, nonZeroSign(1.0f))
        assertEquals(1, nonZeroSign(114514.0f))
    }

    @Test
    fun medianTest() {
        assertEquals(0.0f, median(0.0f, 0.0f, 0.0f), EPSILON)
        assertEquals(0.0f, median(0.0f, 0.0f, 1.0f), EPSILON)
        assertEquals(0.0f, median(0.0f, 1.0f, 0.0f), EPSILON)
        assertEquals(1.0f, median(0.0f, 1.0f, 1.0f), EPSILON)
        assertEquals(0.0f, median(1.0f, 0.0f, 0.0f), EPSILON)
        assertEquals(1.0f, median(1.0f, 0.0f, 1.0f), EPSILON)
        assertEquals(1.0f, median(1.0f, 1.0f, 0.0f), EPSILON)
        assertEquals(1.0f, median(1.0f, 1.0f, 1.0f), EPSILON)
    }

    @Test
    fun mixFloatTest() {
        assertEquals(0.0f, mix(0.0f, 0.0f, 0.0f), EPSILON)
        assertEquals(0.0f, mix(0.0f, 0.0f, 1.0f), EPSILON)

        assertEquals(0.0f, mix(0.0f, 1.0f, 0.0f), EPSILON)
        assertEquals(1.0f, mix(0.0f, 1.0f, 1.0f), EPSILON)
        assertEquals(0.5f, mix(0.0f, 1.0f, 0.5f), EPSILON)

        assertEquals(0.0f, mix(10.0f, -10.0f, 0.5f), EPSILON)
        assertEquals(-5.0f, mix(10.0f, -10.0f, 0.75f), EPSILON)
    }

    @Test
    fun fastInvSqrtTest() {
        fun test(i: Float) {
            assertEquals(1.0f / sqrt(i), fastInvSqrt(i), EPSILON)
        }

        test(1.0f)
        test(2.0f)
        test(3.0f)
        test(4.0f)
        test(5.0f)
        test(6.0f)
        test(7.0f)
        test(8.0f)
    }

    @Test
    fun fastACosTest() {
        fun test(i: Float) {
            val e = acos(i)
            val a = fastACos(i)
            assertEquals(e, a, EPSILON)
        }

        test(-1.01f)
        test(-1.0f)
        test(-0.9995117f)
        test(-0.99f)
        test(-1.0f)
        test(-0.75f)
        test(-0.5f)
        test(-0.25f)
        test(-0.01f)
        test(0.0f)
        test(0.01f)
        test(0.25f)
        test(0.5f)
        test(0.75f)
        test(0.99f)
        test(0.9995117f)
        test(1.0f)
        test(1.01f)
    }
}
package dev.luna5ama.mdsfgen

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class EquationSolverTest {
    private val epsilon = 1e-4f

    @Test
    fun quadratic() {
        val x = FloatArray(3)
        var i: Int

        i =solveQuadratic(x, 3.0f, 2.0f, -5.0f)
        assertEquals(2, i)
        assertEquals(1.0f, x[0], epsilon)
        assertEquals(-5.0f / 3.0f, x[1], epsilon)

        i = solveQuadratic(x, 1.0f, 2.0f, 1.0f)
        assertEquals(1,i )
        assertEquals(-1.0f, x[0], epsilon)

        i = solveQuadratic(x, 1.0f, 0.0f, 1.0f)
        assertEquals(0, i)

        i = solveQuadratic(x, -5.0f, 4.0f, 3.0f)
        assertEquals(2, i)
        assertEquals(-(-2.0f + sqrt(19.0f)) / 5.0f, x[0], epsilon)
        assertEquals((2.0f + sqrt(19.0f)) / 5.0f, x[1], epsilon)
    }
}
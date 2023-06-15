package dev.luna5ama.mdsfgen

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class Vector2Test {
    private val epsilon = 5E-3F

    @Test
    fun xy() {
        var vec = Vector2(0.0f, 0.0f)
        assertEquals(0.0f, vec.x, epsilon)
        assertEquals(0.0f, vec.y, epsilon)

        vec = Vector2(1.0f, 2.0f)
        assertEquals(1.0f, vec.x, epsilon)
        assertEquals(2.0f, vec.y, epsilon)

        vec = Vector2(-1.0f, -2.0f)
        assertEquals(-1.0f, vec.x, epsilon)
        assertEquals(-2.0f, vec.y, epsilon)
    }
}
package dev.luna5ama.mdsfgen

import kotlin.math.abs

/**
 * Represents a signed distance and alignment,
 * which together can be compared to uniquely determine the closest edge segment.
 */
data class SignedDistance(var distance: Float, var dot: Float) {
    constructor() : this(-Float.MAX_VALUE, 1.0f)

    fun reset() {
        distance = -Float.MAX_VALUE
        dot = 1.0f
    }

    fun set(other: SignedDistance) {
        distance = other.distance
        dot = other.dot
    }

    operator fun compareTo(other: SignedDistance): Int {
        var result = abs(distance).compareTo(abs(other.distance))
        if (result == 0) {
            result = dot.compareTo(other.dot)
        }
        return result
    }
}
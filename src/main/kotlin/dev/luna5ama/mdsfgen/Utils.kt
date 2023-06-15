package dev.luna5ama.mdsfgen

import kotlin.math.acos
import kotlin.math.min

const val PI_F = 3.1415927f
const val HALF_PI_F = 1.5707964f
const val ACOS_TABLE_SIZE = 1024

/**
 * @return a linear interpolation between a and b.
 */
fun mix(a: Float, b: Float, t: Float): Float {
    return a * (1 - t) + b * t
}

/**
 * @return a linear interpolation between a and b.
 */
fun mix(a: Vector2, b: Vector2, t: Float): Vector2 {
    return Vector2(mix(a.x, b.x, t), mix(a.y, b.y, t))
}

/**
 * @return -1 if negative, 1 if non-negative.
 */
fun nonZeroSign(v: Float): Int {
    return if (v < 0) -1 else 1
}

/**
 * Adjusts the bound to include the point.
 */
fun pointBounds(p: Point2, bound: Bound) {
    if (p.x < bound.l) bound.l = p.x
    if (p.y < bound.b) bound.b = p.y
    if (p.x > bound.r) bound.r = p.x
    if (p.y > bound.t) bound.t = p.y
}

/**
 * @return 1 if true, 0 if false
 */
fun Boolean.toInt(): Int = if (this) 1 else 0

/**
 * @return true if not 0
 */
fun Int.toBoolean(): Boolean = this != 0

/**
 * @return true if not 0
 */
fun Float.toBoolean(): Boolean = this != 0.0f

/**
 * @return the median of three values.
 */
fun median(a: Float, b: Float, c: Float): Float {
    return if (a < b) {
        if (b < c) b else if (a < c) c else a
    } else {
        if (a < c) a else if (b < c) c else b
    }
}

/**
 * Fast approximation of 1/sqrt(x) from Quake.
 */
fun fastInvSqrt(x: Float): Float {
    val xhalf = 0.5f * x
    var i = x.toRawBits()
    i = 0x5f3759df - (i shr 1)
    var y = Float.fromBits(i)
    y *= 1.5f - xhalf * y * y
    return y
}

private val acosTable = FloatArray(ACOS_TABLE_SIZE + 1) {
    acos(min(it / ACOS_TABLE_SIZE.toFloat(), 1.0f))
}

/**
 * Fast approximation of acos(x) for x in [-1, 1].
 * Uses a lookup table of size [ACOS_TABLE_SIZE].
 *
 * @return the angle in radians. [Float.NaN] if x is [Float.NaN] or not in [-1, 1].
 */
fun fastACos(x: Float): Float {
    if (x.isNaN() || x < -1.0 || x > 1.0) return Float.NaN

    when (x) {
        0.0f -> return HALF_PI_F
        1.0f -> return 0.0f
        -1.0f -> return PI_F
        else -> {
            if (x > 0.0f) {
                val f = x * ACOS_TABLE_SIZE
                val index = f.toInt()
                val delta = f - index
                return mix(acosTable[index], acosTable[index + 1], delta)
            } else {
                val f = -x * ACOS_TABLE_SIZE
                val index = f.toInt()
                val delta = f - index
                return PI_F - mix(acosTable[index], acosTable[index + 1], delta)
            }
        }
    }

}
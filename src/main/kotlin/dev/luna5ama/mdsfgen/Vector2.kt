package dev.luna5ama.mdsfgen

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * A 2-dimensional euclidean vector with double precision.
 * Implementation based on the Vector2 template from Artery Engine.
 *
 * Packaged as a value class to avoid unnecessary heap allocations.
 */
@JvmInline
value class Vector2(val bits: Long) {
    constructor(x: Float, y: Float) : this((x.toRawBits().toLong() shl 32) or (y.toRawBits().toLong() and 0xFFFFFFFF))
    constructor(v: Float = 0.0f) : this(v, v)

    val x: Float
        get() = Float.fromBits((bits ushr 32).toInt())

    val y: Float
        get() = Float.fromBits((bits and 0xFFFFFFFF).toInt())

    /**
     * @return the vector's length.
     */
    fun length(): Float {
        return sqrt(x * x + y * y)
    }

    /**
     * @return the inverse of vector's length.
     */
    fun invLength(): Float {
        return 1.0f / sqrt(x * x + y * y)
    }

    /**
     * @return the vector's direction in radians.
     */
    fun direction(): Float {
        return atan2(y, x)
    }

    /**
     * @return the normalized vector - one that has the same direction but unit length.
     */
    fun normalize(allowZero: Boolean = false): Vector2 {
        if (isZero()) return Vector2(0.0f, (!allowZero).toInt().toFloat())
        val invLen = invLength()
        return Vector2(x * invLen, y * invLen)
    }

    /**
     * @return a vector with the same length that is orthogonal to this one.
     */
    fun getOrthogonal(polarity: Boolean = true): Vector2 {
        return if (polarity) Vector2(-y, x) else Vector2(y, -x)
    }

    /**
     * @return a vector with unit length that is orthogonal to this one.
     */
    fun getOrthonormal(polarity: Boolean = true, allowZero: Boolean = false): Vector2 {
        if (isZero()) {
            return if (polarity) {
                Vector2(0.0f, (!allowZero).toInt().toFloat())
            } else {
                Vector2(0.0f, -(!allowZero).toInt().toFloat())
            }
        }
        val invLen = invLength()
        return if (polarity) Vector2(-y * invLen, x * invLen) else Vector2(y * invLen, -x * invLen)
    }

    fun isZero(): Boolean {
        return x == 0.0f && y == 0.0f
    }

    operator fun unaryMinus(): Vector2 {
        return Vector2(-x, -y)
    }

    operator fun plus(v: Vector2): Vector2 {
        return Vector2(x + v.x, y + v.y)
    }

    operator fun minus(v: Vector2): Vector2 {
        return Vector2(x - v.x, y - v.y)
    }

    operator fun times(v: Vector2): Vector2 {
        return Vector2(x * v.x, y * v.y)
    }

    operator fun div(v: Vector2): Vector2 {
        return Vector2(x / v.x, y / v.y)
    }

    operator fun times(v: Float): Vector2 {
        return Vector2(x * v, y * v)
    }

    operator fun div(v: Float): Vector2 {
        return Vector2(x / v, y / v)
    }

    /**
     * Array of [Vector2] without boxing
     */
    @JvmInline
    value class Array(val array: LongArray) {
        operator fun get(index: Int): Vector2 {
            return Vector2(array[index])
        }

        operator fun set(index: Int, value: Vector2) {
            array[index] = value.bits
        }

        companion object {
            fun of(a: Vector2): Array {
                return Array(longArrayOf(a.bits))
            }

            fun of(a: Vector2, b: Vector2): Array {
                return Array(longArrayOf(a.bits, b.bits))
            }

            fun of(a: Vector2, b: Vector2, c: Vector2): Array {
                return Array(longArrayOf(a.bits, b.bits, c.bits))
            }

            fun of(a: Vector2, b: Vector2, c: Vector2, d: Vector2): Array {
                return Array(longArrayOf(a.bits, b.bits, c.bits, d.bits))
            }
        }
    }
}

/**
 * @return Dot product of two vectors.
 */
fun dotProduct(a: Vector2, b: Vector2): Float {
    return a.x * b.x + a.y * b.y
}

/**
 * A special version of the cross product for 2D vectors (returns scalar value).
 */
fun crossProduct(a: Vector2, b: Vector2): Float {
    return a.x * b.y - a.y * b.x
}

operator fun Float.times(v: Vector2): Vector2 {
    return Vector2(this * v.x, this * v.y)
}

operator fun Float.div(v: Vector2): Vector2 {
    return Vector2(this / v.x, this / v.y)
}

/**
 * A vector may also represent a point, which shall be differentiated semantically using the alias Point2.
 */
typealias Point2 = Vector2

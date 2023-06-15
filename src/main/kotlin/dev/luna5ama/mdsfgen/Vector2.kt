package dev.luna5ama.mdsfgen

import kotlin.math.atan2
import kotlin.math.sqrt

//data class Vector2(var x: Float = 0.0f, var y: Float = 0.0f) {
@JvmInline
value class Vector2(val bits: Long) {
    constructor(x: Float, y: Float) : this((x.toRawBits().toLong() shl 32) or (y.toRawBits().toLong() and 0xFFFFFFFF))
    constructor(v: Float = 0.0f) : this(v, v)

    val x: Float
        get() = Float.fromBits((bits ushr 32).toInt())

    val y: Float
        get() = Float.fromBits((bits and 0xFFFFFFFF).toInt())

    fun length(): Float {
        return sqrt(x * x + y * y)
    }

    fun invLength(): Float {
        return fastInvSqrt(x * x + y * y)
    }

    fun direction(): Float {
        return atan2(y, x)
    }

    fun normalize(allowZero: Boolean = false): Vector2 {
        if (isZero()) return Vector2(0.0f, (!allowZero).toInt().toFloat())
        val invLen = invLength()
        return Vector2(x * invLen, y * invLen)
    }

    fun getOrthogonal(polarity: Boolean = true): Vector2 {
        return if (polarity) Vector2(-y, x) else Vector2(y, -x)
    }

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
}


fun dotProduct(a: Vector2, b: Vector2): Float {
    return a.x * b.x + a.y * b.y
}

fun crossProduct(a: Vector2, b: Vector2): Float {
    return a.x * b.y - a.y * b.x
}

operator fun Float.times(v: Vector2): Vector2 {
    return Vector2(this * v.x, this * v.y)
}

operator fun Float.div(v: Vector2): Vector2 {
    return Vector2(this / v.x, this / v.y)
}

typealias Point2 = Vector2

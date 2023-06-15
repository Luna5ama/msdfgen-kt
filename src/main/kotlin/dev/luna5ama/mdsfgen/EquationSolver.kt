package dev.luna5ama.mdsfgen

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sqrt

fun solveQuadratic(x: FloatArray, a: Float, b: Float, c: Float): Int {
    // a == 0 -> linear equation
    if (a == 0.0f || abs(b) > 1e12 * abs(a)) {
        if (b == 0.0f) {
            if (c == 0.0f) {
                return -1 // 0 == 0
            }
            return 0
        }
        x[0] = -c / b
        return 1
    }

    var dscr = b * b - 4 * a * c

    return if (dscr > 0) {
        dscr = sqrt(dscr)
        x[0] = (-b + dscr) / (2 * a)
        x[1] = (-b - dscr) / (2 * a)
        2
    } else if (dscr == 0.0f) {
        x[0] = -b / (2 * a)
        1
    } else {
        0
    }
}

fun solveCubicNormed(x: FloatArray, aIn: Float, b: Float, c: Float): Int {
    var a = aIn

    val a2 = a * a
    var q = 1 / 9.0f * (a2 - 3.0f * b)
    val r = 1 / 54.0f * (a * (2.0f * a2 - 9.0f * b) + 27.0f * c)
    val r2 = r * r
    val q3 = q * q * q
    a *= 1.0f / 3.0f

    if (r2 < q3) {
        var t = r * fastInvSqrt(q3)
        if (t < -1) t = -1.0f
        if (t > 1) t = 1.0f
        t = fastACos(t)
        q = -2.0f * sqrt(q)
        x[0] = q * cos(1.0f / 3.0f * t) - a
        x[1] = (q * cos(1.0f / 3.0f * (t + 2.0f * PI_F)) - a)
        x[2] = q * cos(1.0f / 3.0f * (t - 2.0f * PI_F)) - a
        return 3
    } else {
        val u = (if (r < 0.0f) 1.0f else -1.0f) * (abs(r) + sqrt(r2 - q3)).pow(1 / 3.0f)
        val v = if (u == 0.0f) 0.0f else q / u
        x[0] = (u + v) - a
        if (u == v || abs(u - v) < 1e-12 * abs(u + v)) {
            x[1] = -0.5f * (u + v) - a
            return 2
        }
        return 1
    }
}

fun solveCubic(x: FloatArray, a: Float, b: Float, c: Float, d: Float): Int {
    if (a != 0.0f) {
        val bn = b / a
        if (abs(bn) < 1e6) {
            return solveCubicNormed(x, bn, c / a, d / a)
        }
    }
    return solveQuadratic(x, b, c, d)
}
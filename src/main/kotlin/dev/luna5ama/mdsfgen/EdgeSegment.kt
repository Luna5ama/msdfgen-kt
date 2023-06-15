package dev.luna5ama.mdsfgen

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sign
import kotlin.math.sqrt

private const val MSDFGEN_CUBIC_SEARCH_STARTS = 4
private const val MSDFGEN_CUBIC_SEARCH_STEPS = 4

sealed class EdgeSegment(var color: EdgeColor = EdgeColor.WHITE) {
    abstract fun clone(): EdgeSegment
    abstract fun point(param: Float): Point2
    abstract fun direction(param: Float): Vector2
    abstract fun directionChange(param: Float): Vector2
    abstract fun length(): Float
    abstract fun signedDistance(origin: Point2, param: BoxedFloat): SignedDistance

    fun distanceToPseudoDistance(distance: SignedDistance, origin: Point2, param: Float) {
        if (param < 0) {
            val dir = direction(0.0f).normalize()
            val aq = origin - point(0.0f)
            val ts = dotProduct(aq, dir)
            if (ts < 0) {
                val pseudoDistance = crossProduct(aq, dir)
                if (abs(pseudoDistance) <= abs(distance.distance)) {
                    distance.distance = pseudoDistance
                    distance.dot = 0.0f
                }
            }
        } else if (param > 1) {
            val dir = direction(1.0f).normalize()
            val bq = origin - point(1.0f)
            val ts = dotProduct(bq, dir)
            if (ts > 0) {
                val pseudoDistance = crossProduct(bq, dir)
                if (abs(pseudoDistance) <= abs(distance.distance)) {
                    distance.distance = pseudoDistance
                    distance.dot = 0.0f
                }
            }
        }
    }

    abstract fun scanlineIntersections(x: FloatArray, dy: IntArray, y: Float): Int

    abstract fun bound(bound: Bound)

    abstract fun reverse()

    abstract fun moveStartPoint(to: Point2)

    abstract fun moveEndPoint(to: Point2)

    abstract fun splitInThirds(
        parts: Array<EdgeSegment?>,
        i1: Int = 0,
        i2: Int = 1,
        i3: Int = 2,
    )

    class Linear(p0: Point2, p1: Point2, edgeColor: EdgeColor = EdgeColor.WHITE) :
        EdgeSegment(edgeColor) {
        val p = arrayOf(p0, p1)

        override fun clone(): Linear {
            return Linear(p[0], p[1], color)
        }

        override fun point(param: Float): Point2 {
            return mix(p[0], p[1], param)
        }

        override fun direction(param: Float): Vector2 {
            return p[1] - p[0]
        }

        override fun directionChange(param: Float): Vector2 {
            return Vector2()
        }

        override fun length(): Float {
            return (p[1] - p[0]).length()
        }

        override fun signedDistance(origin: Point2, param: BoxedFloat): SignedDistance {
            val aq = origin - p[0]
            val ab = p[1] - p[0]
            param.v = dotProduct(aq, ab) / dotProduct(ab, ab)
            val eq = p[(param.v > 0.5f).toInt()] - origin
            val endpointDistance = eq.length()
            if (param.v > 0.0f && param.v < 1.0f) {
                val orthoDistance = dotProduct(ab.getOrthonormal(false), aq)
                if (abs(orthoDistance) < endpointDistance) {
                    return SignedDistance(orthoDistance, 0.0f)
                }
            }
            return SignedDistance(
                nonZeroSign(crossProduct(aq, ab)) * endpointDistance,
                abs(dotProduct(ab.normalize(), eq.normalize()))
            )
        }

        override fun scanlineIntersections(x: FloatArray, dy: IntArray, y: Float): Int {
            if ((y >= p[0].y && y < p[1].y) || (y >= p[1].y && y < p[0].y)) {
                val param = (y - p[0].y) / (p[1].y - p[0].y)
                x[0] = mix(p[0].x, p[1].x, param)
                dy[0] = sign(p[1].y - p[0].y).toInt()
                return 1
            }
            return 0
        }

        override fun bound(bound: Bound) {
            pointBounds(p[0], bound)
            pointBounds(p[1], bound)
        }

        override fun reverse() {
            val temp = p[0]
            p[0] = p[1]
            p[1] = temp
        }

        override fun moveStartPoint(to: Point2) {
            p[0] = to
        }

        override fun moveEndPoint(to: Point2) {
            p[1] = to
        }

        override fun splitInThirds(
            parts: Array<EdgeSegment?>,
            i1: Int,
            i2: Int,
            i3: Int
        ) {
            parts[i1] = Linear(p[0], point(1 / 3.0f), color)
            parts[i2] = Linear(point(1 / 3.0f), point(2 / 3.0f), color)
            parts[i3] = Linear(point(2 / 3.0f), p[1], color)
        }
    }

    class Quadratic(p0: Point2, p1: Point2, p2: Point2, edgeColor: EdgeColor = EdgeColor.WHITE) : EdgeSegment(edgeColor) {
        val p = arrayOf(p0, p1, p2)

        init {
            if (p[1] == p[0] || p[1] == p[2]) {
                p[1] = 0.5f * (p[0] + p[2])
            }
        }

        override fun clone(): Quadratic {
            return Quadratic(p[0], p[1], p[2], color)
        }

        override fun point(param: Float): Point2 {
            return mix(mix(p[0], p[1], param), mix(p[1], p[2], param), param)
        }

        override fun direction(param: Float): Vector2 {
            val tangent = mix(p[1] - p[0], p[2] - p[1], param)
            if (tangent.isZero()) return p[2] - p[0]
            return tangent
        }

        override fun directionChange(param: Float): Vector2 {
            return (p[2] - p[1]) - (p[1] - p[0])
        }

        override fun length(): Float {
            val ab = p[1] - p[0]
            val br = p[2] - p[1] - ab
            val abab = dotProduct(ab, ab)
            val abbr = dotProduct(ab, br)
            val brbr = dotProduct(br, br)
            val abLen = sqrt(abab)
            val brLen = sqrt(brbr)
            val crs = crossProduct(ab, br)
            val h = sqrt(abab + abbr + abbr + brbr)
            return (brLen * ((abbr + brbr) * h - abbr * abLen) +
                crs * crs * ln((brLen * h + abbr + brbr) / (brLen * abLen + abbr))) / (brbr * brLen)
        }

        private val t = FloatArray(3)

        override fun signedDistance(origin: Point2, param: BoxedFloat): SignedDistance {
            val qa = p[0] - origin
            val ab = p[1] - p[0]
            val br = p[2] - p[1] - ab
            val a = dotProduct(br, br)
            val b = 3 * dotProduct(ab, br)
            val c = 2 * dotProduct(ab, ab) + dotProduct(qa, br)
            val d = dotProduct(qa, ab)
            val solutions = solveCubic(t, a, b, c, d)

            var epDir = direction(0.0f)
            var minDistance = nonZeroSign(crossProduct(epDir, qa)) * qa.length() // distance from A
            param.v = -dotProduct(qa, epDir) / dotProduct(epDir, epDir)

            epDir = direction(1.0f)
            val distance = (p[2] - origin).length() // distance from B
            if (distance < abs(minDistance)) {
                minDistance = nonZeroSign(crossProduct(epDir, p[2] - origin)) * distance
                param.v = dotProduct(origin - p[1], epDir) / dotProduct(epDir, epDir)
            }

            for (i in 0 until solutions) {
                if (t[i] > 0 && t[i] < 1) {
                    val qe = qa + 2.0f * t[i] * ab + t[i] * t[i] * br
                    val qeDist = qe.length()
                    if (qeDist - abs(minDistance) <= 1e-6) {
                        minDistance = nonZeroSign(crossProduct(ab + t[i] * br, qe)) * qeDist
                        param.v = t[i]
                    }
                }
            }

            return if (param.v in 0.0f..1.0f) {
                SignedDistance(minDistance, 0.0f)
            } else if (param.v < 0.5f) {
                SignedDistance(minDistance, abs(dotProduct(direction(0.0f).normalize(), qa.normalize())))
            } else {
                SignedDistance(minDistance, abs(dotProduct(direction(1.0f).normalize(), (p[2] - origin).normalize())))
            }
        }

        override fun scanlineIntersections(x: FloatArray, dy: IntArray, y: Float): Int {
            var total = 0
            var nextDY = if (y > p[0].y) 1 else -1
            x[total] = p[0].x

            if (p[0].y == y) {
                if (p[0].y < p[1].y || (p[0].y == p[1].y && p[0].y < p[2].y)) {
                    dy[total++] = 1
                } else {
                    nextDY = 1
                }
            }

            val ab = p[1] - p[0]
            val br = p[2] - p[1] - ab
            val t = FloatArray(2)
            val solutions = solveQuadratic(t, br.y, 2 * ab.y, p[0].y - y)

            // Sort solutions
            if (solutions >= 2 && t[0] > t[1]) {
                val tmp = t[0]
                t[0] = t[1]
                t[1] = tmp
            }

            for (i in 0 until solutions) {
                if (t[i] >= 0 && t[i] <= 1) {
                    x[total] = p[0].x + 2 * t[i] * ab.x + t[i] * t[i] * br.x
                    if (nextDY * (ab.y + t[i] * br.y) >= 0) {
                        dy[total++] = nextDY
                        nextDY = -nextDY
                    }
                }
                if (total >= 2) {
                    break
                }
            }

            if (p[2].y == y) {
                if (nextDY > 0 && total > 0) {
                    --total
                    nextDY = -1
                }
                if ((p[2].y < p[1].y || (p[2].y == p[1].y && p[2].y < p[0].y)) && total < 2) {
                    x[total] = p[2].x
                    if (nextDY < 0) {
                        dy[total++] = -1
                        nextDY = 1
                    }
                }
            }
            if (nextDY != if (y >= p[2].y) 1 else -1) {
                if (total > 0) {
                    --total
                } else {
                    if (abs(p[2].y - y) < abs(p[0].y - y))
                        x[total] = p[2].x
                    dy[total++] = nextDY
                }
            }

            return total
        }

        override fun bound(bound: Bound) {
            pointBounds(p[0], bound)
            pointBounds(p[2], bound)
            val bot = (p[1] - p[0]) - (p[2] - p[1])
            if (bot.x != 0.0f) {
                val param = (p[1].x - p[0].x) / bot.x
                if (param > 0 && param < 1) {
                    pointBounds(point(param), bound)
                }
            }
            if (bot.y != 0.0f) {
                val param = (p[1].y - p[0].y) / bot.y
                if (param > 0 && param < 1) {
                    pointBounds(point(param), bound)
                }
            }
        }

        override fun reverse() {
            val tmp = p[0]
            p[0] = p[2]
            p[2] = tmp
        }

        override fun moveStartPoint(to: Point2) {
            val origSDir = p[0] - p[1]
            val origP1 = p[1]
            p[1] += crossProduct(p[0] - p[1], to - p[0]) / crossProduct(p[0] - p[1], p[2] - p[1]) * (p[2] - p[1])
            p[0] = to

            if (dotProduct(origSDir, p[0] - p[1]) < 0) {
                p[1] = origP1
            }
        }

        override fun moveEndPoint(to: Point2) {
            val origEDir = p[2] - p[1]
            val origP1 = p[1]
            p[1] += crossProduct(p[0] - p[1], to - p[0]) / crossProduct(p[0] - p[1], p[2] - p[1]) * (p[2] - p[1])
            p[2] = to
            if (dotProduct(origEDir, p[2] - p[1]) < 0) {
                p[1] = origP1
            }
        }

        override fun splitInThirds(
            parts: Array<EdgeSegment?>,
            i1: Int,
            i2: Int,
            i3: Int
        ) {
            parts[i1] = Quadratic(p[0], mix(p[0], p[1], 1 / 3.0f), point(1 / 3.0f), color)
            parts[i2] = Quadratic(point(1 / 3.0f), mix(mix(p[0], p[1], 5 / 9.0f), mix(p[1], p[2], 4 / 9.0f), 0.5f), point(2 / 3.0f), color)
            parts[i3] = Quadratic(point(2 / 3.0f), mix(p[1], p[2], 2 / 3.0f), p[2], color)
        }

        fun convertToCubic(): EdgeSegment {
            return Cubic(p[0], mix(p[0], p[1], 2 / 3.0f), mix(p[1], p[2], 1 / 3.0f), p[2], color)
        }
    }

    class Cubic(p0: Point2, p1: Point2, p2: Point2, p3: Point2, edgeColor: EdgeColor = EdgeColor.WHITE) :
        EdgeSegment(edgeColor) {
        val p = arrayOf(p0, p1, p2, p3)

        init {
            if ((p[1] == p[0] || p[1] == p[3]) && (p[2] == p[0] || p[2] == p[3])) {
                p[1] = mix(p[0], p[3], 1 / 3.0f)
                p[2] = mix(p[0], p[3], 2 / 3.0f)
            }
        }

        override fun clone(): EdgeSegment {
            return Cubic(p[0], p[1], p[2], p[3], color)
        }

        override fun point(param: Float): Point2 {
            val p12 = mix(p[1], p[2], param)
            return mix(mix(mix(p[0], p[1], param), p12, param), mix(p12, mix(p[2], p[3], param), param), param)
        }

        override fun direction(param: Float): Vector2 {
            val tangent = mix(mix(p[1] - p[0], p[2] - p[1], param), mix(p[2] - p[1], p[3] - p[2], param), param)
            if (tangent.isZero()) {
                if (param == 0.0f) {
                    return p[2] - p[0]
                }
                if (param == 1.0f) {
                    return p[3] - p[1]
                }
            }
            return tangent
        }

        override fun directionChange(param: Float): Vector2 {
            return mix((p[2] - p[1]) - (p[1] - p[0]), (p[3] - p[2]) - (p[2] - p[1]), param)
        }

        override fun length(): Float {
            throw UnsupportedOperationException()
        }

        override fun signedDistance(origin: Point2, param: BoxedFloat): SignedDistance {
            val qa = p[0] - origin
            val ab = p[1] - p[0]
            val br = p[2] - p[1] - ab
            val `as` = p[3] - p[2] - (p[2] - p[1]) - br

            var epDir = direction(0.0f)
            var minDistance = nonZeroSign(crossProduct(epDir, qa)) * qa.length() // distance from A

            param.v = -dotProduct(qa, epDir) / dotProduct(epDir, epDir)
            epDir = direction(1.0f)
            val distance = (p[3] - origin).length() // distance from B
            if (distance < abs(minDistance)) {
                minDistance = nonZeroSign(crossProduct(epDir, p[3] - origin)) * distance
                param.v = dotProduct(epDir - (p[3] - origin), epDir) / dotProduct(epDir, epDir)
            }

            // Iterative minimum distance search
            // Iterative minimum distance search
            for (i in 0..MSDFGEN_CUBIC_SEARCH_STARTS) {
                var t = i.toFloat() / MSDFGEN_CUBIC_SEARCH_STARTS
                var qe = qa + 3 * t * ab + 3 * t * t * br + t * t * t * `as`
                for (step in 0 until MSDFGEN_CUBIC_SEARCH_STEPS) {
                    // Improve t
                    val d1 = 3.0f * ab + 6.0f * t * br + 3.0f * t * t * `as`
                    val d2 = 6.0f * br + 6.0f * t * `as`
                    t -= dotProduct(qe, d1) / (dotProduct(d1, d1) + dotProduct(qe, d2))
                    if (t <= 0 || t >= 1) {
                        break
                    }
                    qe = qa + 3 * t * ab + 3 * t * t * br + t * t * t * `as`
                    val distQE = qe.length()
                    if (distQE < abs(minDistance)) {
                        minDistance = nonZeroSign(crossProduct(d1, qe)) * distQE
                        param.v = t
                    }
                }
            }

            if (param.v >= 0 && param.v <= 1) {
                return SignedDistance(minDistance, 0.0f)
            }
            return if (param.v < 0.5f) {
                SignedDistance(minDistance, abs(dotProduct(direction(0.0f).normalize(), qa.normalize())))
            } else {
                SignedDistance(minDistance, abs(dotProduct(direction(1.0f).normalize(), (p[3] - origin).normalize())))
            }
        }

        override fun scanlineIntersections(x: FloatArray, dy: IntArray, y: Float): Int {
            var total = 0
            var nextDY = if (y > p[0].y) 1 else -1
            x[total] = p[0].x
            if (p[0].y == y) {
                if (p[0].y < p[1].y || (p[0].y == p[1].y && (p[0].y < p[2].y || (p[0].y == p[2].y && p[0].y < p[3].y)))) {
                    dy[total++] = 1
                } else {
                    nextDY = 1
                }
            }

            val ab = p[1] - p[0]
            val br = p[2] - p[1] - ab
            val asVec = (p[3] - p[2]) - (p[2] - p[1]) - br
            val t = FloatArray(3)
            val solutions = solveCubic(t, asVec.y, 3 * br.y, 3 * ab.y, p[0].y - y)
            if (solutions >= 2) {
                if (t[0] > t[1]) {
                    val tmp = t[0]
                    t[0] = t[1]
                    t[1] = tmp
                }
                if (solutions >= 3 && t[1] > t[2]) {
                    val tmp = t[1]
                    t[1] = t[2]
                    t[2] = tmp
                    if (t[0] > t[1]) {
                        val tmp2 = t[0]
                        t[0] = t[1]
                        t[1] = tmp2
                    }
                }
            }
            for (i in 0 until solutions) {
                if (t[i] >= 0 && t[i] <= 1 && total < 3) {
                    x[total] = p[0].x + 3 * t[i] * ab.x + 3 * t[i] * t[i] * br.x + t[i] * t[i] * t[i] * asVec.x
                    if (nextDY * (ab.y + 2 * t[i] * br.y + t[i] * t[i] * asVec.y) >= 0) {
                        dy[total++] = nextDY
                        nextDY = -nextDY
                    }
                }
            }

            if (p[3].y == y) {
                if (nextDY > 0 && total > 0) {
                    --total
                    nextDY = -1
                }
                if (p[3].y < p[2].y || (p[3].y == p[2].y && (p[3].y < p[1].y || (p[3].y == p[1].y && p[3].y < p[0].y)))) {
                    x[total] = p[3].x
                    if (nextDY < 0) {
                        dy[total++] = -1
                        nextDY = 1
                    }
                }
            }

            if (nextDY != if (y >= p[3].y) 1 else -1) {
                if (total > 0) {
                    --total
                } else {
                    if (Math.abs(p[3].y - y) < Math.abs(p[0].y - y)) {
                        x[total] = p[3].x
                    }
                    dy[total++] = nextDY
                }
            }

            return total
        }

        override fun bound(bound: Bound) {
            pointBounds(p[0], bound)
            pointBounds(p[3], bound)
            val a0 = p[1] - p[0]
            val a1 = 2.0f * (p[2] - p[1] - a0)
            val a2 = p[3] - 3.0f * p[2] + 3.0f * p[1] - p[0]
            val params = FloatArray(2)
            var solutions = solveQuadratic(params, a2.x, a1.x, a0.x)
            for (i in 0 until solutions) {
                if (params[i] > 0 && params[i] < 1) {
                    pointBounds(point(params[i]), bound)
                }
            }
            solutions = solveQuadratic(params, a2.y, a1.y, a0.y)
            for (i in 0 until solutions) {
                if (params[i] > 0 && params[i] < 1) {
                    pointBounds(point(params[i]), bound)
                }
            }
        }

        override fun reverse() {
            var tmp = p[0]
            p[0] = p[3]
            p[3] = tmp
            tmp = p[1]
            p[1] = p[2]
            p[2] = tmp
        }

        override fun moveStartPoint(to: Point2) {
            p[1] += to - p[0]
            p[0] = to
        }

        override fun moveEndPoint(to: Point2) {
            p[2] += to - p[3]
            p[3] = to
        }

        override fun splitInThirds(
            parts: Array<EdgeSegment?>,
            i1: Int,
            i2: Int,
            i3: Int
        ) {
            parts[i1] = Cubic(
                p[0],
                if (p[0] == p[1]) p[0] else mix(p[0], p[1], 1 / 3.0f),
                mix(mix(p[0], p[1], 1 / 3.0f), mix(p[1], p[2], 1 / 3.0f), 1 / 3.0f),
                point(1 / 3.0f),
                color
            )
            parts[i2] = Cubic(
                point(1 / 3.0f),
                mix(
                    mix(mix(p[0], p[1], 1 / 3.0f), mix(p[1], p[2], 1 / 3.0f), 1 / 3.0f),
                    mix(mix(p[1], p[2], 1 / 3.0f), mix(p[2], p[3], 1 / 3.0f), 1 / 3.0f),
                    2 / 3.0f
                ),
                mix(
                    mix(mix(p[0], p[1], 2 / 3.0f), mix(p[1], p[2], 2 / 3.0f), 2 / 3.0f),
                    mix(mix(p[1], p[2], 2 / 3.0f), mix(p[2], p[3], 2 / 3.0f), 2 / 3.0f),
                    1 / 3.0f
                ),
                point(2 / 3.0f), color
            )
            parts[i3] = Cubic(
                point(2 / 3.0f),
                mix(mix(p[1], p[2], 2 / 3.0f), mix(p[2], p[3], 2 / 3.0f), 2 / 3.0f),
                if (p[2] == p[3]) p[3] else mix(p[2], p[3], 2 / 3.0f),
                p[3],
                color
            )
        }

        fun deconverge(param: Int, amount: Float) {
            val dir = direction(param.toFloat())
            val normal = dir.getOrthonormal()
            val h = dotProduct(directionChange(param.toFloat()) - dir, normal)
            when (param) {
                0 -> p[1] += amount * (dir + sign(h) * sqrt(abs(h)) * normal)
                1 -> p[2] -= amount * (dir - sign(h) * sqrt(abs(h)) * normal)
            }
        }
    }
}
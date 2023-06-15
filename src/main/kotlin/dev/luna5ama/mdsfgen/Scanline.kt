package dev.luna5ama.mdsfgen

import kotlin.math.min

/**
 * Fill rule dictates how intersection total is interpreted during rasterization.
 */
enum class FillRule {
    FILL_NONZERO,
    FILL_ODD, // "even-odd"
    FILL_POSITIVE,
    FILL_NEGATIVE
}

/**
 * Resolves the number of intersection into a binary fill value based on fill rule.
 */
class Scanline {
    private var intersections = mutableListOf<Intersection>()
    private var lastIndex = 0

    private fun preprocess() {
        lastIndex = 0
        if (intersections.isNotEmpty()) {
            intersections.sortBy { it.x }
            var totalDirection = 0
            for (intersection in intersections) {
                totalDirection += intersection.direction
                intersection.direction = totalDirection
            }
        }
    }

    private fun moveTo(x: Float): Int {
        if (intersections.isEmpty()) return -1
        var index = lastIndex

        if (x < intersections[index].x) {
            do {
                if (index == 0) {
                    lastIndex = 0
                    return -1
                }
                --index
            } while (x < intersections[index].x)
        } else {
            while (index < intersections.size - 1 && x >= intersections[index + 1].x) ++index
        }

        lastIndex = index
        return index
    }

    /**
     * Populates the intersection list.
     */
    fun setIntersections(intersections: MutableList<Intersection>) {
        this.intersections = intersections
        preprocess()
    }

    /**
     * Returns the number of intersections left of x.
     */
    fun countIntersections(x: Float): Int {
        return moveTo(x) + 1
    }

    /**
     * Returns the total sign of intersections left of x.
     */
    fun sumIntersections(x: Float): Int {
        val index = moveTo(x)
        if (index >= 0) {
            return intersections[index].direction
        }
        return 0
    }

    /**
     * Decides whether the scanline is filled at x based on fill rule.
     */
    fun filled(x: Float, fillRule: FillRule): Boolean {
        return interpretFillRule(sumIntersections(x), fillRule)
    }

    /**
     * An intersection with the scanline.
     */
    class Intersection(
        /**
         * X coordinate.
         */
        val x: Float,
        /**
         * Normalized Y direction of the oriented edge at the point of intersection.
         */
        var direction: Int
    )

    companion object {
        private fun interpretFillRule(intersections: Int, fillRule: FillRule): Boolean {
            return when (fillRule) {
                FillRule.FILL_NONZERO -> intersections != 0
                FillRule.FILL_ODD -> (intersections and 1) == 1
                FillRule.FILL_POSITIVE -> intersections > 0
                FillRule.FILL_NEGATIVE -> intersections < 0
            }
        }

        fun overlap(a: Scanline, b: Scanline, xFrom: Float, xTo: Float, fillRule: FillRule): Float {
            var total = 0.0f

            var aInside = false
            var bInside = false

            var ai = 0
            var bi = 0

            var ax = if (a.intersections.isNotEmpty()) {
                a.intersections[ai].x
            } else {
                xTo
            }
            var bx = if (b.intersections.isNotEmpty()) {
                b.intersections[bi].x
            } else {
                xTo
            }

            while (ax < xFrom || bx < xFrom) {
                val xNext: Float = min(ax, bx)
                if (ax == xNext && ai < a.intersections.size) {
                    aInside = interpretFillRule(a.intersections[ai].direction, fillRule)
                    ax = if (++ai < a.intersections.size) a.intersections[ai].x else xTo
                }
                if (bx == xNext && bi < b.intersections.size) {
                    bInside = interpretFillRule(b.intersections[bi].direction, fillRule)
                    bx = if (++bi < b.intersections.size) b.intersections[bi].x else xTo
                }
            }
            var x = xFrom
            while (ax < xTo || bx < xTo) {
                val xNext: Float = min(ax, bx)
                if (aInside == bInside) total += xNext - x
                if (ax == xNext && ai < a.intersections.size) {
                    aInside = interpretFillRule(a.intersections[ai].direction, fillRule)
                    ax = if (++ai < a.intersections.size) a.intersections[ai].x else xTo
                }
                if (bx == xNext && bi < b.intersections.size) {
                    bInside = interpretFillRule(b.intersections[bi].direction, fillRule)
                    bx = if (++bi < b.intersections.size) b.intersections[bi].x else xTo
                }
                x = xNext
            }
            if (aInside == bInside) total += xTo - x

            return total
        }
    }
}
package dev.luna5ama.mdsfgen

import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sqrt

private fun boundPoints(bound: Bound, p: Point2) {
    if (p.x < bound.l) bound.l = p.x
    if (p.y < bound.b) bound.b = p.y
    if (p.x > bound.r) bound.r = p.x
    if (p.y > bound.t) bound.t = p.y
}

/**
 *  A single closed contour of a shape.
 */
class Contour {
    /**
     * The sequence of edges that make up the contour.
     */
    val edges = mutableListOf<EdgeSegment>()

    /**
     * Adds an edge to the contour.
     */
    fun addEdge(edge: EdgeSegment) {
        edges.add(edge)
    }

    /**
     * Adjusts the bounding box to fit the contour.
     */
    fun bound(bound: Bound) {
        for (edge in edges) {
            edge.bound(bound)
        }
    }

    /**
     * Adjusts the bounding box to fit the contour border's mitered corners.
     */
    fun boundMitered(bound: Bound, border: Float, miterLimit: Float, polarity: Int) {
        if (edges.isEmpty()) return

        var prevDir = edges.last().direction(1.0f).normalize(true)
        for (edge in edges) {
            val dir = -edge.direction(0.0f).normalize(true)
            if (polarity * crossProduct(prevDir, dir) >= 0.0f) {
                var miterLength = miterLimit
                val q = 0.5f * (1.0f - dotProduct(prevDir, dir))
                if (q > 0) miterLength = min(1.0f / sqrt(q), miterLimit)
                val miter = edge.point(0.0f) + border * miterLength * (prevDir + dir).normalize(true)
                boundPoints(bound, miter)
            }
            prevDir = edge.direction(1.0f).normalize(true)
        }
    }

    /**
     * Computes the winding of the contour.
     *
     * @return 1 if positive, -1 if negative.
     */
    fun winding(): Int {
        if (edges.isEmpty()) {
            return 0
        }
        var total = 0.0f

        when (edges.size) {
            1 -> {
                val a = edges[0].point(0.0f)
                val b = edges[0].point(1.0f / 3.0f)
                val c = edges[0].point(2.0f / 3.0f)
                total += shoelace(a, b)
                total += shoelace(b, c)
                total += shoelace(c, a)
            }
            2 -> {
                val a = edges[0].point(0.0f)
                val b = edges[0].point(0.5f)
                val c = edges[1].point(0.0f)
                val d = edges[1].point(0.5f)
                total += shoelace(a, b)
                total += shoelace(b, c)
                total += shoelace(c, d)
                total += shoelace(d, a)
            }
            else -> {
                var prev = edges.last().point(0.0f)
                for (edge in edges) {
                    val cur = edge.point(0.0f)
                    total += shoelace(prev, cur)
                    prev = cur
                }
            }
        }

        return sign(total).toInt()
    }

    private fun shoelace(a: Point2, b: Point2): Float {
        return (b.x - a.x) * (a.y + b.y)
    }

    /**
     * Reverses the sequence of edges on the contour.
     */
    fun reverse() {
        edges.reverse()
        for (edge in edges) {
            edge.reverse()
        }
    }
}
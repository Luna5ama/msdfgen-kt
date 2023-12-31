package dev.luna5ama.mdsfgen

import kotlin.math.sqrt

/**
 * Threshold of the dot product of adjacent edge directions to be considered convergent.
 */
private const val MSDFGEN_CORNER_DOT_EPSILON = 0.000001f

/**
 * The proportional amount by which a curve's control point will be adjusted to eliminate convergent corners.
 */
private const val MSDFGEN_DECONVERGENCE_FACTOR = 0.000001f

/**
 * Vector shape representation.
 */
class Shape {
    /**
     * The list of contours the shape consists of.
     */
    val contours = mutableListOf<Contour>()

    /**
     * Specifies whether the shape uses bottom-to-top (false) or top-to-bottom (true) Y coordinates.
     */
    var inverseYAxis = false

    /**
     * Adds a contour.
     */
    fun addContour(contour: Contour) {
        contours.add(contour)
    }

    private fun MutableList<EdgeSegment>.deconvergeEdge(edgeIndex: Int, param: Int) {
        val segment = this[edgeIndex]
        if (segment is EdgeSegment.Quadratic) {
            this[edgeIndex] = segment.convertToCubic()
        } else if (segment is EdgeSegment.Cubic) {
            segment.deconverge(param, MSDFGEN_DECONVERGENCE_FACTOR)
        }
    }

    /**
     * Normalizes the shape geometry for distance field generation.
     */
    fun normalize() {
        for (contour in contours) {
            val edges = contour.edges
            if (edges.size == 1) {
                val parts = arrayOfNulls<EdgeSegment>(3)
                edges[0].splitInThirds(parts)
                edges.clear()
                edges.add(parts[0]!!)
                edges.add(parts[1]!!)
                edges.add(parts[2]!!)
            } else {
                var prevIndex = edges.lastIndex
                for (i in edges.indices) {
                    val edge = edges[i]
                    val prevDir = edges[prevIndex].direction(1.0f).normalize()
                    val curDir = edge.direction(0.0f).normalize()

                    if (dotProduct(prevDir, curDir) < MSDFGEN_CORNER_DOT_EPSILON - 1) {
                        edges.deconvergeEdge(prevIndex, 1)
                            edges.deconvergeEdge(i, 0)
                    }
                    prevIndex = i
                }
            }
        }
    }

    /**
     * Performs basic checks to determine if the object represents a valid shape.
     */
    fun validate(): Boolean {
        for (contour in contours) {
            if (contour.edges.isNotEmpty()) {
                var corner = contour.edges.last().point(1.0f)
                for (edge in contour.edges) {
                    if (edge.point(0.0f) != corner) return false
                    corner = edge.point(1.0f)
                }
            }
        }
        return true
    }

    /**
     * Adjusts the bounding box to fit the shape.
     */
    fun bound(bound: Bound) {
        for (contour in contours) {
            contour.bound(bound)
        }
    }

    /**
     * Adjusts the bounding box to fit the shape border's mitered corners.
     */
    fun boundMiters(bound: Bound, border: Float, miterLimit: Float, polarity: Int) {
        for (contour in contours) {
            contour.boundMitered(bound, border, miterLimit, polarity)
        }
    }

    /**
     * Computes the minimum bounding box that fits the shape, optionally with a (mitered) border.
     */
    fun getBounds(border: Float = 0.0f, miterLimit: Float = 0.0f, polarity: Int = 0): Bound {
        val bound = Bound()
        bound(bound)
        if (border > 0) {
            bound.l -= border
            bound.b -= border
            bound.r += border
            bound.t += border

            if (miterLimit > 0) {
                boundMiters(bound, border, miterLimit, polarity)
            }
        }
        return bound
    }

    /**
     * Outputs the scanline that intersects the shape at [y].
     */
    fun scanline(line: Scanline, y: Float) {
        val intersections = mutableListOf<Scanline.Intersection>()
        val x = FloatArray(3)
        val dy = IntArray(3)
        for (contour in contours) {
            for (edge in contour.edges) {
                val n = edge.scanlineIntersections(x, dy, y)
                for (i in 0 until n) {
                    intersections.add(Scanline.Intersection(x[i], dy[i]))
                }
            }
        }
        line.setIntersections(intersections)
    }

    /**
     * @return the total number of edge segments
     */
    fun edgeCount(): Int {
        var total = 0
        for (contour in contours) {
            total += contour.edges.size
        }
        return total
    }

    /**
     * Assumes its contours are unoriented (even-odd fill rule). Attempts to orient them to conform to the non-zero winding rule.
     *
     */
    fun orientContours() {
        val ratio =
            0.5f * (sqrt(5.0f) - 1.0f) // an irrational number to minimize the chance of intersecting a corner or other point of interest
        val orientations = IntArray(contours.size)
        val intersections = mutableListOf<Intersection>()

        for (i in contours.indices) {
            if (orientations[i] != 0 || contours[i].edges.isEmpty()) continue

            // Find a Y that crosses the contour
            val y0 = contours[i].edges.first().point(0.0f).y
            var y1 = y0

            for (edge in contours[i].edges) {
                if (y0 != y1) break
                y1 = edge.point(1.0f).y
            }

            for (edge in contours[i].edges) {
                if (y0 != y1) break
                y1 = edge.point(ratio).y // in case all endpoints are in a horizontal line
            }

            val y = mix(y0, y1, ratio)

            // Scanline through the whole shape at Y
            val x1 = FloatArray(3)
            val dy = IntArray(3)

            for (j in contours.indices) {
                for (edge in contours[j].edges) {
                    val n = edge.scanlineIntersections(x1, dy, y)
                    for (k in 0 until n) {
                        val intersection = Intersection(x1[k], dy[k], j)
                        intersections.add(intersection)
                    }
                }
            }

            intersections.sort()

            // Disqualify multiple intersections
            for (j in 1 until intersections.size) {
                if (intersections[j].x != intersections[j - 1].x) continue
                intersections[j].direction = 0
                intersections[j - 1].direction = 0
            }

            // Inspect scanline and deduce orientations of intersected contours
            for (j in intersections.indices) {
                if (intersections[j].direction == 0) continue
                val orientation = 2 * ((j and 1) xor (intersections[j].direction > 0).toInt()) - 1
                orientations[intersections[j].contourIndex] += orientation
            }

            intersections.clear()
        }

        // Reverse contours that have the opposite orientation
        for (i in contours.indices) {
            if (orientations[i] >= 0) continue
            contours[i].reverse()
        }
    }

    private data class Intersection(val x: Float, var direction: Int, val contourIndex: Int) :
        Comparable<Intersection> {
        override fun compareTo(other: Intersection): Int {
            return x.compareTo(other.x)
        }
    }
}
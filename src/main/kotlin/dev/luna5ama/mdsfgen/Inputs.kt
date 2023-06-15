package dev.luna5ama.mdsfgen

import java.awt.font.GlyphVector
import java.awt.geom.PathIterator

/**
 * Converts a [GlyphVector] to a [Shape].
 */
fun GlyphVector.toShape(): Shape {
    val shape = Shape()
    var contour = Contour()

    val pathIterator = getGlyphOutline(0).getPathIterator(null)
    val coords = FloatArray(6)

    var lastX = 0.0f
    var lastY = 0.0f

    while (!pathIterator.isDone) {
        when (pathIterator.currentSegment(coords)) {
            PathIterator.SEG_MOVETO -> {
                val x = coords[0]
                val y = coords[1]
                lastX = x
                lastY = y
            }
            PathIterator.SEG_LINETO -> {
                val x = coords[0]
                val y = coords[1]

                contour.addEdge(EdgeSegment.Linear(Vector2(lastX, lastY), Vector2(x, y)))
                lastX = x
                lastY = y
            }
            PathIterator.SEG_QUADTO -> {
                val x1 = coords[0]
                val y1 = coords[1]
                val x2 = coords[2]
                val y2 = coords[3]

                contour.addEdge(EdgeSegment.Quadratic(Vector2(lastX, lastY), Vector2(x1, y1), Vector2(x2, y2)))
                lastX = x2
                lastY = y2
            }
            PathIterator.SEG_CUBICTO -> {
                val x1 = coords[0]
                val y1 = coords[1]
                val x2 = coords[2]
                val y2 = coords[3]
                val x3 = coords[4]
                val y3 = coords[5]

                contour.addEdge(EdgeSegment.Cubic(Vector2(lastX, lastY), Vector2(x1, y1), Vector2(x2, y2), Vector2(x3, y3)))
                lastX = x3
                lastY = y3
            }
            PathIterator.SEG_CLOSE -> {
                shape.addContour(contour)
                contour = Contour()
            }
        }

        pathIterator.next()
    }

    return shape
}
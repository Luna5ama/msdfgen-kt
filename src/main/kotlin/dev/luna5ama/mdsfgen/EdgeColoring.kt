package dev.luna5ama.mdsfgen

import kotlin.math.abs
import kotlin.math.sin

object EdgeColoring {
    private const val MSDFGEN_EDGE_LENGTH_PRECISION = 4

    fun isCorner(aDir: Vector2, bDir: Vector2, crossThreshold: Float): Boolean {
        val v = dotProduct(aDir, bDir) <= 0 || abs(crossProduct(aDir, bDir)) > crossThreshold
        return v
    }

    fun estimateEdgeLength(edge: EdgeSegment): Float {
        var len = 0.0f
        var prev = edge.point(0.0f)
        for (i in 1..MSDFGEN_EDGE_LENGTH_PRECISION) {
            val cur = edge.point(1.0f / MSDFGEN_EDGE_LENGTH_PRECISION * i)
            len += (cur - prev).length()
            prev = cur
        }
        return len
    }

    fun edgeColoringSimple(shape: Shape, angleThreshold: Float = 3.0f, seedIn: Long) {
        val seed = BoxedLong(seedIn)
        val crossThreshold = sin(angleThreshold)
        val corners = mutableListOf<Int>()

        for (contour in shape.contours) {
            // Identify corners
            corners.clear()
            val edges = contour.edges

            if (edges.isNotEmpty()) {
                var prevDirection = edges.last().get().direction(1.0f).normalize()
                for ((i, edgeHolder) in edges.withIndex()) {
                    val edge = edgeHolder.get()
                    if (isCorner(prevDirection.normalize(), edge.direction(0.0f).normalize(), crossThreshold)) {
                        corners.add(i)
                    }
                    prevDirection = edge.direction(1.0f)
                }
            }

            // Smooth contour
            when (corners.size) {
                0 -> {
                    for (edge in edges) {
                        edge.get().color = EdgeColor.WHITE
                    }
                }
                // "Teardrop" case
                1 -> {
                    val colors = EdgeColor.Array.of(EdgeColor.WHITE, EdgeColor.WHITE, EdgeColor.BLACK)
                    switchColor(colors, 0, seed)
                    colors[2] = colors[0]
                    switchColor(colors, 2, seed)
                    val corner = corners[0]
                    if (edges.size >= 3) {
                        val m = edges.size
                        for (i in 0 until m) {
                            val colorIndex = 1 + ((3 + 2.875f * i / (m - 1) - 1.4375f + 0.5f).toInt() - 3)
                            edges[(corner + i) % m].get().color = colors[colorIndex]
                        }
                    } else if (edges.size >= 1) {
                        val parts = arrayOfNulls<EdgeSegment>(7)
                        edges[0].get().splitInThirds(
                            parts,
                            0 + 3 * corner,
                            1 + 3 * corner,
                            2 + 3 * corner
                        )
                        if (edges.size >= 2) {
                            edges[1].get().splitInThirds(
                                parts,
                                3 - 3 * corner,
                                4 - 3 * corner,
                                5 - 3 * corner
                            )
                            parts[0]!!.color = colors[0]
                            parts[1]!!.color = colors[0]

                            parts[2]!!.color = colors[1]
                            parts[3]!!.color = colors[1]

                            parts[4]!!.color = colors[2]
                            parts[5]!!.color = colors[2]
                        } else {
                            parts[0]!!.color = colors[0]
                            parts[1]!!.color = colors[1]
                            parts[2]!!.color = colors[2]
                        }
                        edges.clear()
                        for (part in parts) {
                            edges.add(EdgeHolder(part!!))
                        }
                    }
                }
                else -> {
                    val cornerCount = corners.size
                    var spline = 0
                    val start = corners[0]
                    val colors = EdgeColor.Array.of(EdgeColor.WHITE)
                    switchColor(colors, seed)
                    val initialColor = colors[0]

                    val m = edges.size
                    for (i in 0 until m) {
                        val index = (start + i) % m
                        if (spline + 1 < cornerCount && corners[spline + 1] == index) {
                            ++spline
                            switchColor(colors, seed, EdgeColor((spline == cornerCount - 1).toInt() * initialColor.bit))
                        }
                        edges[index].get().color = colors[0]
                    }
                }
            }
        }
    }

    private fun switchColor(colors: EdgeColor.Array, seedHolder: BoxedLong, banned: EdgeColor = EdgeColor.BLACK) {
        switchColor(colors, 0, seedHolder, banned)
    }

    private val START = EdgeColor.Array.of(EdgeColor.CYAN, EdgeColor.MAGENTA, EdgeColor.YELLOW)

    private fun switchColor(
        colors: EdgeColor.Array,
        i: Int,
        seedHolder: BoxedLong,
        banned: EdgeColor = EdgeColor.BLACK
    ) {
        var seed by seedHolder
        val color = colors[i]

        val combined = color and banned
        if (combined == EdgeColor.RED || combined == EdgeColor.GREEN || combined == EdgeColor.BLUE) {
            colors[i] = combined xor EdgeColor.WHITE
            return
        }

        if (color == EdgeColor.BLACK || color == EdgeColor.WHITE) {
            colors[i] = START[Math.floorMod(seed, 3).toInt()]
            seed /= 3
            return
        }

        val shifted = color.bit shl (1 + (seed and 1).toInt())
        colors[i] = EdgeColor((shifted or (shifted shr 3)) and EdgeColor.WHITE.bit)
        seed = seed shr 1
    }
}
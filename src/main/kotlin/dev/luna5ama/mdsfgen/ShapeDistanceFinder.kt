package dev.luna5ama.mdsfgen

/**
 * Finds the distance between a point and a Shape.
 * [ContourCombiner] dictates the distance metric and its data type.
 */
class ShapeDistanceFinder<DistanceType, EdgeCache : Any>(
    private val contourCombiner: ContourCombiner<EdgeSelectorRef<DistanceType, EdgeCache>, DistanceType, EdgeCache>,
    private val shape: Shape
) {
    private val shapeEdgeCache = mutableListOf<EdgeCache>()

    init {
        repeat(shape.edgeCount()) {
            shapeEdgeCache.add(contourCombiner.edgeSelectorFactory.createCache())
        }
    }

    /**
     * Finds the distance from origin. Not thread-safe! Is fastest when subsequent queries are close together.
     */
    fun distance(origin: Point2): DistanceType {
        contourCombiner.reset(origin)
        var edgeCacheIndex = 0

        for (i in shape.contours.indices) {
            val contour = shape.contours[i]
            val edges = contour.edges
            if (edges.isEmpty()) continue
            val edgeSelector = contourCombiner.edgeSelector(i)

            var prevEdge = (if (edges.size >= 2) edges[edges.lastIndex - 1] else edges.first())
            var curEdge = (edges.last())
            for (j in edges.indices) {
                val nextEdge = edges[j]
                edgeSelector.addEdge(shapeEdgeCache[edgeCacheIndex++], prevEdge, curEdge, nextEdge)
                prevEdge = curEdge
                curEdge = nextEdge
            }
        }

        return contourCombiner.distance()
    }

    /**
     *Finds the distance between shape and origin. Does not allocate result cache used to optimize performance of multiple queries.
     */
    fun oneShotDistance(shape: Shape, origin: Point2): DistanceType {
        val contourCombiner = contourCombiner.newInstance(shape)
        contourCombiner.reset(origin)

        for ((i, contour) in shape.contours.withIndex()) {
            val edges = contour.edges
            if (edges.isEmpty()) continue
            val edgeSelector = contourCombiner.edgeSelector(i)

            var prevEdge = (if (edges.size >= 2) edges[edges.lastIndex - 1] else edges.first())
            var curEdge = (edges.last())
            for (edge in edges) {
                val nextEdge = edge
                val dummy = contourCombiner.edgeSelectorFactory.createCache()
                edgeSelector.addEdge(dummy, prevEdge, curEdge, nextEdge)
                prevEdge = curEdge
                curEdge = nextEdge
            }
        }

        return contourCombiner.distance()
    }
}
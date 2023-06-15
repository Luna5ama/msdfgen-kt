package dev.luna5ama.mdsfgen

import kotlin.math.abs

private fun initDistanceSingle(): BoxedFloat {
    return BoxedFloat(-Float.MAX_VALUE)
}

private fun initDistanceMulti(): MultiDistance {
    return MultiDistance(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
}

private fun resolveDistanceSingle(distance: BoxedFloat): Float {
    return distance.v
}

private fun resolveDistanceMulti(distance: MultiDistance): Float {
    return median(distance.r, distance.g, distance.b)
}

@Suppress("UNCHECKED_CAST")
sealed class ContourCombiner<EdgeSelector : EdgeSelectorRef<DistanceType, EdgeCache>, DistanceType, EdgeCache : Any>(
    edgeSelectorClass: Class<EdgeSelector>
) {
    val edgeSelectorFactory =
        edgeSelectorClass.getDeclaredField("Companion")[null] as EdgeSelectorFactory<EdgeSelector, DistanceType, EdgeCache>
    private val initDistance0 =
        (if (edgeSelectorFactory.distanceType == BoxedFloat::class.java) ::initDistanceSingle
        else ::initDistanceMulti) as () -> DistanceType
    private val resolveDistance0 =
        (if (edgeSelectorFactory.distanceType == BoxedFloat::class.java) ::resolveDistanceSingle
        else ::resolveDistanceMulti) as (DistanceType) -> Float

    fun newEdgeSelector(): EdgeSelector {
        return edgeSelectorFactory.create()
    }

    fun initDistance(): DistanceType {
        return initDistance0()
    }

    fun resolveDistance(distance: DistanceType): Float {
        return resolveDistance0(distance)
    }

    abstract fun reset(p: Point2)
    abstract fun edgeSelector(i: Int): EdgeSelector
    abstract fun distance(): DistanceType

    abstract fun newInstance(shape: Shape): ContourCombiner<EdgeSelector, DistanceType, EdgeCache>
}

/**
 * Simply selects the nearest contour.
 */
class SimpleContourCombiner<EdgeSelector : EdgeSelectorRef<DistanceType, EdgeCache>, DistanceType, EdgeCache : Any>
    (edgeSelectorClass: Class<EdgeSelector>, shape: Shape) :
    ContourCombiner<EdgeSelector, DistanceType, EdgeCache>(edgeSelectorClass) {
    private val shapeEdgeSelector = newEdgeSelector()

    override fun reset(p: Point2) {
        shapeEdgeSelector.reset(p)
    }

    override fun edgeSelector(i: Int): EdgeSelector {
        return shapeEdgeSelector
    }

    override fun distance(): DistanceType {
        return shapeEdgeSelector.distance()
    }

    override fun newInstance(shape: Shape): ContourCombiner<EdgeSelector, DistanceType, EdgeCache> {
        return SimpleContourCombiner(edgeSelectorFactory.type, shape)
    }

    companion object {
        inline operator fun <reified DistanceType> invoke(
            shape: Shape
        ): SimpleContourCombiner<EdgeSelectorRef<DistanceType, Any>, DistanceType, Any> {
            val edgeSelectorClass = EdgeSelector<DistanceType>()
            return SimpleContourCombiner(edgeSelectorClass, shape)
        }
    }
}

/**
 * Selects the nearest contour that actually forms a border between filled and unfilled area.
 */
class OverlappingContourCombiner<EdgeSelector : EdgeSelectorRef<DistanceType, EdgeCache>, DistanceType, EdgeCache : Any>
    (edgeSelectorClass: Class<EdgeSelector>, shape: Shape) :
    ContourCombiner<EdgeSelector, DistanceType, EdgeCache>(edgeSelectorClass) {
    private var p = Point2()
    private val windings = mutableListOf<Int>()
    private val edgeSelectors = mutableListOf<EdgeSelector>()
    private val shapeEdgeSelector = newEdgeSelector()
    private val innerEdgeSelector = newEdgeSelector()
    private val outerEdgeSelector = newEdgeSelector()

    init {
        for (i in shape.contours.indices) {
            val contour = shape.contours[i]
            windings.add(contour.winding())
            edgeSelectors.add(newEdgeSelector())
        }
    }

    override fun reset(p: Point2) {
        this.p = p
        for (i in edgeSelectors.indices) {
            edgeSelectors[i].reset(p)
        }
    }

    override fun edgeSelector(i: Int): EdgeSelector {
        return edgeSelectors[i]
    }

    override fun distance(): DistanceType {
        val contourCount = edgeSelectors.size
        shapeEdgeSelector.init()
        innerEdgeSelector.init()
        outerEdgeSelector.init()
        shapeEdgeSelector.reset(p)
        innerEdgeSelector.reset(p)
        outerEdgeSelector.reset(p)

        for (i in 0 until contourCount) {
            val edgeDistance = edgeSelectors[i].distance()
            shapeEdgeSelector.merge(edgeSelectors[i])
            if (windings[i] > 0 && resolveDistance(edgeDistance) >= 0) {
                innerEdgeSelector.merge(edgeSelectors[i])
            }
            if (windings[i] < 0 && resolveDistance(edgeDistance) <= 0) {
                outerEdgeSelector.merge(edgeSelectors[i])
            }
        }

        val shapeDistance = shapeEdgeSelector.distance()
        val innerDistance = innerEdgeSelector.distance()
        val outerDistance = outerEdgeSelector.distance()
        val innerScalarDistance = resolveDistance(innerDistance)
        val outerScalarDistance = resolveDistance(outerDistance)
        var distance = initDistance()

        val winding: Int
        if (innerScalarDistance >= 0 && abs(innerScalarDistance) <= abs(outerScalarDistance)) {
            distance = innerDistance
            winding = 1
            for (i in 0 until contourCount) {
                if (windings[i] <= 0) continue
                val contourDistance = edgeSelectors[i].distance()
                if (abs(resolveDistance(contourDistance)) < abs(outerScalarDistance) &&
                    resolveDistance(contourDistance) > resolveDistance(distance)
                ) {
                    distance = contourDistance
                }
            }
        } else if (outerScalarDistance <= 0 && abs(outerScalarDistance) < abs(innerScalarDistance)) {
            distance = outerDistance
            winding = -1
            for (i in 0 until contourCount) {
                if (windings[i] >= 0) continue
                val contourDistance = edgeSelectors[i].distance()
                if (abs(resolveDistance(contourDistance)) < abs(innerScalarDistance) &&
                    resolveDistance(contourDistance) < resolveDistance(distance)
                ) {
                    distance = contourDistance
                }
            }
        } else {
            return shapeDistance
        }

        for (i in 0 until contourCount) {
            if (windings[i] == winding) continue
            val contourDistance = edgeSelectors[i].distance()
            if (resolveDistance(contourDistance) * resolveDistance(distance) >= 0 &&
                abs(resolveDistance(contourDistance)) < abs(resolveDistance(distance))
            ) {
                distance = contourDistance
            }
        }

        if (resolveDistance(distance) == resolveDistance(shapeDistance)) {
            distance = shapeDistance
        }

        return distance
    }

    override fun newInstance(shape: Shape): ContourCombiner<EdgeSelector, DistanceType, EdgeCache> {
        return OverlappingContourCombiner(edgeSelectorFactory.type, shape)
    }

    companion object {
        inline operator fun <reified DistanceType> invoke(
            shape: Shape
        ): OverlappingContourCombiner<EdgeSelectorRef<DistanceType, Any>, DistanceType, Any> {
            val edgeSelector = EdgeSelector<DistanceType>()
            return OverlappingContourCombiner(edgeSelector, shape)
        }
    }
}
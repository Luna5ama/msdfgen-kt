package dev.luna5ama.mdsfgen

import kotlin.math.abs

private const val DISTANCE_DELTA_FACTOR = 1.001f

typealias TrueDistance = BoxedFloat

typealias PseudoDistance = BoxedFloat

open class MultiDistance(
    var r: Float = 0.0f,
    var g: Float = 0.0f,
    var b: Float = 0.0f
)

class MultiAndTrueDistance(
    r: Float = 0.0f,
    g: Float = 0.0f,
    b: Float = 0.0f,
    var a: Float = 0.0f
) : MultiDistance(r, g, b)

typealias EdgeSelectorRef<DistanceType, EdgeCache> = EdgeSelector<EdgeSelector<*, DistanceType, EdgeCache>, DistanceType, EdgeCache>

sealed interface EdgeSelectorFactory<E : EdgeSelector<*, DistanceType, EdgeCache>, DistanceType, EdgeCache : Any> {
    val type: Class<E>
    val distanceType: Class<DistanceType>

    fun create(): E
    fun createCache(): EdgeCache
}

sealed interface EdgeSelector<Self, DistanceType, EdgeCache : Any> {
    fun init()
    fun reset(p: Point2)
    fun addEdge(
        cache: EdgeCache,
        prevEdge: EdgeSegment,
        edge: EdgeSegment,
        nextEdge: EdgeSegment,
    )

    fun merge(other: Self)
    fun distance(): DistanceType

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified DistanceType> invoke(): Class<EdgeSelectorRef<DistanceType, Any>> {
            return when (DistanceType::class.java) {
                BoxedFloat::class.java -> TrueDistanceSelector::class.java
                MultiDistance::class.java -> MultiDistanceSelector::class.java
                MultiAndTrueDistance::class.java -> MultiDistanceSelector::class.java
                else -> throw IllegalArgumentException("Unsupported distance type: ${DistanceType::class.java}")
            } as Class<EdgeSelectorRef<DistanceType, Any>>
        }
    }
}

/**
 * Selects the nearest edge by its true distance.
 */
class TrueDistanceSelector : EdgeSelector<TrueDistanceSelector, BoxedFloat, TrueDistanceSelector.EdgeCache> {
    private var p = Point2()
    private var minDistance = SignedDistance()
    private val dummy = BoxedFloat()
    private val output = BoxedFloat()

    override fun init() {
        p = Point2()
        minDistance.reset()
    }

    override fun reset(p: Point2) {
        val delta = DISTANCE_DELTA_FACTOR * (p - this.p).length()
        minDistance.distance += nonZeroSign(minDistance.distance) * delta
        this.p = p
    }

    override fun addEdge(
        cache: EdgeCache,
        prevEdge: EdgeSegment,
        edge: EdgeSegment,
        nextEdge: EdgeSegment,
    ) {
        val delta = DISTANCE_DELTA_FACTOR * (p - cache.point).length()
        if (cache.absDistance - delta <= abs(minDistance.distance)) {
            val distance = edge.signedDistance(p, dummy)
            if (distance < minDistance) {
                minDistance = distance
            }
            cache.point = p
            cache.absDistance = abs(distance.distance)
        }
    }

    override fun merge(other: TrueDistanceSelector) {
        if (other.minDistance < minDistance) {
            minDistance = other.minDistance
        }
    }

    override fun distance(): BoxedFloat {
        output.v = minDistance.distance
        return output
    }

    class EdgeCache {
        var point = Point2()
        var absDistance = 0.0f
    }

    companion object : EdgeSelectorFactory<TrueDistanceSelector, BoxedFloat, EdgeCache> {
        override val type = TrueDistanceSelector::class.java
        override val distanceType: Class<BoxedFloat> = BoxedFloat::class.java

        override fun create(): TrueDistanceSelector {
            return TrueDistanceSelector()
        }

        override fun createCache(): EdgeCache {
            return EdgeCache()
        }
    }
}

private fun getPseudoDistance(distanceIn: BoxedFloat, ep: Vector2, edgeDir: Vector2): Boolean {
    var distance by distanceIn

    val ts = dotProduct(ep, edgeDir)
    if (ts > 0) {
        val pseudoDistance = crossProduct(ep, edgeDir)
        if (abs(pseudoDistance) < abs(distance)) {
            distance = pseudoDistance
            return true
        }
    }
    return false
}

sealed class PseudoDistanceSelectorBase<Self : PseudoDistanceSelectorBase<Self>> :
    EdgeSelector<Self, BoxedFloat, PseudoDistanceSelectorBase.EdgeCache> {
    private var minTrueDistance = SignedDistance()
    private var minNegativePseudoDistance = -abs(minTrueDistance.distance)
    private var minPositivePseudoDistance = abs(minTrueDistance.distance)
    private var nearEdge: EdgeSegment? = null
    private var nearEdgeParam = 0.0f

    override fun init() {
        minTrueDistance.reset()
        minNegativePseudoDistance = -abs(minTrueDistance.distance)
        minPositivePseudoDistance = abs(minTrueDistance.distance)
        nearEdge = null
        nearEdgeParam = 0.0f
    }

    fun reset(delta: Float) {
        minTrueDistance.distance += nonZeroSign(minTrueDistance.distance) * delta
        minNegativePseudoDistance = -abs(minTrueDistance.distance)
        minPositivePseudoDistance = abs(minTrueDistance.distance)
        nearEdge = null
        nearEdgeParam = 0.0f
    }

    fun isEdgeRelevant(cache: EdgeCache, p: Point2): Boolean {
        val delta = DISTANCE_DELTA_FACTOR * (p - cache.point).length()
        return (
            cache.absDistance - delta <= abs(minTrueDistance.distance) ||
                abs(cache.aDomainDistance) < delta ||
                abs(cache.bDomainDistance) < delta ||
                (cache.aDomainDistance > 0 && (if (cache.aPseudoDistance < 0)
                    cache.aPseudoDistance + delta >= minNegativePseudoDistance else
                    cache.aPseudoDistance - delta <= minPositivePseudoDistance
                    )) ||
                (cache.bDomainDistance > 0 && (if (cache.bPseudoDistance < 0)
                    cache.bPseudoDistance + delta >= minNegativePseudoDistance else
                    cache.bPseudoDistance - delta <= minPositivePseudoDistance
                    ))
            )
    }

    fun addEdgeTrueDistance(edge: EdgeSegment, distance: SignedDistance, param: Float) {
        if (distance < minTrueDistance) {
            minTrueDistance.set(distance)
            nearEdge = edge
            nearEdgeParam = param
        }
    }

    fun addEdgePseudoDistance(distance: Float) {
        if (distance <= 0 && distance > minNegativePseudoDistance) {
            minNegativePseudoDistance = distance
        }
        if (distance >= 0 && distance < minPositivePseudoDistance) {
            minPositivePseudoDistance = distance
        }
    }

    override fun merge(other: Self) {
        if (other.minTrueDistance < minTrueDistance) {
            minTrueDistance.set(other.minTrueDistance)
            nearEdge = other.nearEdge
            nearEdgeParam = other.nearEdgeParam
        }
        if (other.minNegativePseudoDistance > minNegativePseudoDistance) {
            minNegativePseudoDistance = other.minNegativePseudoDistance
        }
        if (other.minPositivePseudoDistance < minPositivePseudoDistance) {
            minPositivePseudoDistance = other.minPositivePseudoDistance
        }
    }

    fun computeDistance(p: Point2, distance: SignedDistance): Float {
        var minDistance = if (minTrueDistance.distance < 0) minNegativePseudoDistance else minPositivePseudoDistance
        val nearEdge = nearEdge
        if (nearEdge != null) {
            distance.set(minTrueDistance)
            nearEdge.distanceToPseudoDistance(distance, p, nearEdgeParam)
            if (abs(distance.distance) < abs(minDistance)) {
                minDistance = distance.distance
            }
        }
        return minDistance
    }

    fun trueDistance(): SignedDistance {
        return minTrueDistance
    }

    class EdgeCache {
        var point = Point2()
        var absDistance = 0.0f
        var aDomainDistance = 0.0f
        var bDomainDistance = 0.0f
        var aPseudoDistance = 0.0f
        var bPseudoDistance = 0.0f
    }
}

/**
 * Selects the nearest edge by its pseudo-distance.
 */
class PseudoDistanceSelector : PseudoDistanceSelectorBase<PseudoDistanceSelector>() {
    private var p = Point2()

    override fun init() {
        super.init()
        p = Point2()
    }

    override fun reset(p: Point2) {
        val delta = DISTANCE_DELTA_FACTOR * (p - this.p).length()
        super.reset(delta)
        this.p = p
    }

    private val pd0 = BoxedFloat()

    override fun addEdge(
        cache: EdgeCache,
        prevEdge: EdgeSegment,
        edge: EdgeSegment,
        nextEdge: EdgeSegment,
    ) {
        if (!isEdgeRelevant(cache, p)) return

        val param = BoxedFloat()
        val distance = edge.signedDistance(p, param)
        addEdgeTrueDistance(edge, distance, param.v)
        cache.point = p
        cache.absDistance = abs(distance.distance)

        val ap = p - edge.point(0.0f)
        val bp = p - edge.point(1.0f)
        val aDir = edge.direction(0.0f).normalize(true)
        val bDir = edge.direction(1.0f).normalize(true)
        val prevDir = prevEdge.direction(1.0f).normalize(true)
        val nextDir = nextEdge.direction(0.0f).normalize(true)
        val add = dotProduct(ap, (prevDir + aDir).normalize(true))
        val bdd = -dotProduct(bp, (bDir + nextDir).normalize(true))

        var pd by pd0

        if (add > 0) {
            pd = distance.distance
            if (getPseudoDistance(pd0, ap, -aDir)) {
                pd = -pd
                addEdgePseudoDistance(pd)
            }
            cache.aPseudoDistance = pd
        }

        if (bdd > 0) {
            pd = distance.distance
            if (getPseudoDistance(pd0, bp, bDir)) {
                addEdgePseudoDistance(pd)
            }
            cache.bPseudoDistance = pd
        }

        cache.aDomainDistance = add
        cache.bDomainDistance = bdd
    }

    private val outputSignedDistance = SignedDistance()
    private val output = BoxedFloat()

    override fun distance(): BoxedFloat {
        output.v = computeDistance(p, outputSignedDistance)
        return output
    }

    companion object : EdgeSelectorFactory<PseudoDistanceSelector, BoxedFloat, EdgeCache> {
        override val type = PseudoDistanceSelector::class.java
        override val distanceType = BoxedFloat::class.java

        override fun create(): PseudoDistanceSelector {
            return PseudoDistanceSelector()
        }

        override fun createCache(): EdgeCache {
            return EdgeCache()
        }
    }
}

sealed class MultiDistanceSelectorBase<DistanceType : MultiDistance> :
    EdgeSelector<MultiDistanceSelectorBase<DistanceType>, DistanceType, PseudoDistanceSelectorBase.EdgeCache> {
    protected var p = Point2()
    protected val r = PseudoDistanceSelector()
    protected val g = PseudoDistanceSelector()
    protected val b = PseudoDistanceSelector()

    override fun init() {
        p = Point2()
        r.init()
        g.init()
        b.init()
    }

    override fun reset(p: Point2) {
        val delta = DISTANCE_DELTA_FACTOR * (p - this.p).length()
        r.reset(delta)
        g.reset(delta)
        b.reset(delta)
        this.p = p
    }

    private val param = BoxedFloat()
    private val pd0 = BoxedFloat()

    override fun addEdge(
        cache: PseudoDistanceSelectorBase.EdgeCache,
        prevEdge: EdgeSegment,
        edge: EdgeSegment,
        nextEdge: EdgeSegment,
    ) {
        if (!((edge.color and EdgeColor.RED).toBoolean() && r.isEdgeRelevant(cache, p)) &&
            !((edge.color and EdgeColor.GREEN).toBoolean() && g.isEdgeRelevant(cache, p)) &&
            !((edge.color and EdgeColor.BLUE).toBoolean() && b.isEdgeRelevant(cache, p))
        ) return

        val distance = edge.signedDistance(p, param)
        if ((edge.color and EdgeColor.RED).toBoolean()) {
            r.addEdgeTrueDistance(edge, distance, param.v)
        }
        if ((edge.color and EdgeColor.GREEN).toBoolean()) {
            g.addEdgeTrueDistance(edge, distance, param.v)
        }
        if ((edge.color and EdgeColor.BLUE).toBoolean()) {
            b.addEdgeTrueDistance(edge, distance, param.v)
        }
        cache.point = p
        cache.absDistance = abs(distance.distance)

        val ap = p - edge.point(0.0f)
        val bp = p - edge.point(1.0f)
        val aDir = edge.direction(0.0f).normalize(true)
        val bDir = edge.direction(1.0f).normalize(true)
        val prevDir = prevEdge.direction(1.0f).normalize(true)
        val nextDir = nextEdge.direction(0.0f).normalize(true)
        val add = dotProduct(ap, (prevDir + aDir).normalize(true))
        val bdd = -dotProduct(bp, (bDir + nextDir).normalize(true))

        var pd by pd0

        if (add > 0) {
            pd = distance.distance
            if (getPseudoDistance(pd0, ap, -aDir)) {
                pd = -pd
                if ((edge.color and EdgeColor.RED).toBoolean()) {
                    r.addEdgePseudoDistance(pd)
                }
                if ((edge.color and EdgeColor.GREEN).toBoolean()) {
                    g.addEdgePseudoDistance(pd)
                }
                if ((edge.color and EdgeColor.BLUE).toBoolean()) {
                    b.addEdgePseudoDistance(pd)
                }
            }
            cache.aPseudoDistance = pd
        }

        if (bdd > 0) {
            pd = distance.distance
            if (getPseudoDistance(pd0, bp, bDir)) {
                if ((edge.color and EdgeColor.RED).toBoolean()) {
                    r.addEdgePseudoDistance(pd)
                }
                if ((edge.color and EdgeColor.GREEN).toBoolean()) {
                    g.addEdgePseudoDistance(pd)
                }
                if ((edge.color and EdgeColor.BLUE).toBoolean()) {
                    b.addEdgePseudoDistance(pd)
                }
            }
            cache.bPseudoDistance = pd
        }

        cache.aDomainDistance = add
        cache.bDomainDistance = bdd
    }

    override fun merge(other: MultiDistanceSelectorBase<DistanceType>) {
        r.merge(other.r)
        g.merge(other.g)
        b.merge(other.b)
    }

    fun trueDistance(): SignedDistance {
        val distanceR = r.trueDistance()
        val distanceG = g.trueDistance()
        val distanceB = b.trueDistance()
        var distance = distanceR
        if (distanceG < distance) {
            distance = distanceG
        }
        if (distanceB < distance) {
            distance = distanceB
        }
        return distance
    }

    protected class PseudoDistanceSelector : PseudoDistanceSelectorBase<PseudoDistanceSelector>() {
        override fun reset(p: Point2) {
            throw UnsupportedOperationException()
        }

        override fun distance(): BoxedFloat {
            throw UnsupportedOperationException()
        }

        override fun addEdge(cache: EdgeCache, prevEdge: EdgeSegment, edge: EdgeSegment, nextEdge: EdgeSegment) {
            throw UnsupportedOperationException()
        }
    }
}


class MultiDistanceSelector : MultiDistanceSelectorBase<MultiDistance>() {
    private val outputSignedDistance = SignedDistance()
    private val outputMultiDistance = MultiDistance()

    override fun distance(): MultiDistance {
        outputMultiDistance.r = r.computeDistance(p, outputSignedDistance)
        outputMultiDistance.g = g.computeDistance(p, outputSignedDistance)
        outputMultiDistance.b = b.computeDistance(p, outputSignedDistance)
        return outputMultiDistance
    }

    companion object : EdgeSelectorFactory<MultiDistanceSelector, MultiDistance, PseudoDistanceSelectorBase.EdgeCache> {
        override val type = MultiDistanceSelector::class.java
        override val distanceType = MultiDistance::class.java

        override fun create(): MultiDistanceSelector {
            return MultiDistanceSelector()
        }

        override fun createCache(): PseudoDistanceSelectorBase.EdgeCache {
            return PseudoDistanceSelectorBase.EdgeCache()
        }
    }
}

class MultiAndTrueDistanceSelector : MultiDistanceSelectorBase<MultiAndTrueDistance>() {
    private val outputSignedDistance = SignedDistance()
    private val outputMultiDistance = MultiAndTrueDistance()

    override fun distance(): MultiAndTrueDistance {
        outputMultiDistance.r = r.computeDistance(p, outputSignedDistance)
        outputMultiDistance.g = g.computeDistance(p, outputSignedDistance)
        outputMultiDistance.b = b.computeDistance(p, outputSignedDistance)
        outputMultiDistance.a = trueDistance().distance
        return outputMultiDistance
    }

    companion object :
        EdgeSelectorFactory<MultiAndTrueDistanceSelector, MultiAndTrueDistance, PseudoDistanceSelectorBase.EdgeCache> {
        override val type = MultiAndTrueDistanceSelector::class.java
        override val distanceType = MultiAndTrueDistance::class.java

        override fun create(): MultiAndTrueDistanceSelector {
            return MultiAndTrueDistanceSelector()
        }

        override fun createCache(): PseudoDistanceSelectorBase.EdgeCache {
            return PseudoDistanceSelectorBase.EdgeCache()
        }
    }
}
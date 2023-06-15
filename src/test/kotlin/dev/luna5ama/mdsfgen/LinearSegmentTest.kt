package dev.luna5ama.mdsfgen

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Suppress("JoinDeclarationAndAssignment")
class LinearSegmentTest {
    private val epsilon = 5e-3f

    @Test
    fun signedDistance() {
        var seg: EdgeSegment.Linear

        seg = EdgeSegment.Linear(Point2(0.0f, 0.0f), Point2(1.0f, 1.0f))
        seg.testSignedDistance(0.0f, 0.0f, 0.0f, 0.707107f,0.0f)
        seg.testSignedDistance(1.0f, 1.0f, 0.0f, 0.707107f,1.0f)
        seg.testSignedDistance(0.5f, 0.5f, 0.0f, 0.0f, 0.5f)
        seg.testSignedDistance(0.5f, 10.0f, -9.01388f, 0.666795f, 5.25f)
        seg.testSignedDistance(0.0f, 0.5f, -0.353553f, 0.0f, 0.25f)

        seg = EdgeSegment.Linear(Point2(8.0f, 5.0f), Point2(-10.0f, 9.9f))
        seg.testSignedDistance(0.0f, 0.0f, -6.92575f, 0.0f, 0.343381f)
        seg.testSignedDistance(1.0f, 1.0f, -5.6982f, 0.0f, 0.305738f)
        seg.testSignedDistance(0.5f, 0.5f, -6.31197f, 0.0f, 0.32456f)
        seg.testSignedDistance(0.5f, 10.0f,2.85446f, 0.0f, 0.45832f)
        seg.testSignedDistance(0.0f, 0.5f, -6.4433f, 0.0f, 0.350421f)
    }

    private fun EdgeSegment.Linear.testSignedDistance(
        pointX: Float,
        pointY: Float,
        distance: Float,
        dot: Float,
        param: Float
    ) {
        val param0 = BoxedFloat()
        val result = signedDistance(Point2(pointX, pointY), param0)
        assertEquals(distance, result.distance, epsilon)
        assertEquals(dot, result.dot, epsilon)
        assertEquals(param, param0.v, epsilon)
    }

    @Test
    fun direction() {
        var seg: EdgeSegment.Linear

        seg = EdgeSegment.Linear(Point2(0.0f, 0.0f), Point2(1.0f, 1.0f))
        seg.testDirection(0.0f, 1.0f, 1.0f)
        seg.testDirection(0.25f, 1.0f, 1.0f)
        seg.testDirection(0.5f, 1.0f, 1.0f)
        seg.testDirection(0.75f, 1.0f, 1.0f)
        seg.testDirection(1.0f, 1.0f, 1.0f)

        seg = EdgeSegment.Linear(Point2(8.0f, 5.0f), Point2(-10.0f, 9.9f))
        seg.testDirection(0.0f, -18.0f, 4.9f)
        seg.testDirection(0.25f, -18.0f, 4.9f)
        seg.testDirection(0.5f, -18.0f, 4.9f)
        seg.testDirection(0.75f, -18.0f, 4.9f)
        seg.testDirection(1.0f, -18.0f, 4.9f)
    }

    fun EdgeSegment.Linear.testDirection(param: Float, directionX: Float, directionY: Float) {
        val result = direction(param)
        assertEquals(directionX, result.x, epsilon)
        assertEquals(directionY, result.y, epsilon)
    }
}
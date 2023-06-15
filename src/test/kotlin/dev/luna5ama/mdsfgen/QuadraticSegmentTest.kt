package dev.luna5ama.mdsfgen

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Suppress("JoinDeclarationAndAssignment")
class QuadraticSegmentTest {
    private val epsilon = 5e-3f

    @Test
    fun signedDistance() {
        var seg: EdgeSegment.Quadratic

        seg = EdgeSegment.Quadratic(Point2(0.0f, 0.0f), Point2(-1.0f, 2.0f), Point2(1.0f, 1.0f))
        seg.testSignedDistance(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        seg.testSignedDistance(1.0f, 1.0f, 0.0f, 0.0f, 1.0f)
        seg.testSignedDistance(0.5f, 0.5f,0.666667f, 0.0f, 0.940959f)
        seg.testSignedDistance(0.5f, 10.0f,-8.68f, 0.0f, 0.685411f)
        seg.testSignedDistance(0.0f, 0.5f,0.206287f, 0.0f, 0.116204f)

        seg = EdgeSegment.Quadratic(Point2(8.0f, 5.0f), Point2(3.0f, 2.0f), Point2(-10.0f, 9.9f))
        seg.testSignedDistance(0.0f, 0.0f,  -4.82327f, 0.0f, 0.48796f)
        seg.testSignedDistance(1.0f, 1.0f,  -3.6148f, 0.0f, 0.454725f)
        seg.testSignedDistance(0.5f, 0.5f,  -4.2161f, 0.0f, 0.471851f)
        seg.testSignedDistance(0.5f, 10.0f, 4.86498f, 0.0f, 0.616546f)
        seg.testSignedDistance(0.0f, 0.5f,  -4.33974f, 0.0f, 0.49357f)
    }

    private fun EdgeSegment.Quadratic.testSignedDistance(pointX: Float, pointY: Float, distance: Float, dot: Float, param: Float) {
        val param0 = BoxedFloat()
        val result = signedDistance(Point2(pointX, pointY), param0)
        assertEquals(distance, result.distance, epsilon)
        assertEquals(dot, result.dot, epsilon)
        assertEquals(param, param0.v, epsilon)
    }

    @Test
    fun direction() {
        var seg: EdgeSegment.Quadratic

        seg = EdgeSegment.Quadratic(Point2(0.0f, 0.0f), Point2(-1.0f, 2.0f), Point2(1.0f, 1.0f))
        seg.testDirection(0.0f, -1.0f, 2.0f)
        seg.testDirection(0.25f, -0.25f, 1.25f)
        seg.testDirection(0.5f, 0.5f, 0.5f)
        seg.testDirection(0.75f, 1.25f, -0.25f)
        seg.testDirection(1.0f, 2.0f, -1.0f)

        seg = EdgeSegment.Quadratic(Point2(8.0f, 5.0f), Point2(3.0f, 2.0f), Point2(-10.0f, 9.9f))
        seg.testDirection(0.0f, -5.0f, -3.0f)
        seg.testDirection(0.25f, -7.0f, -0.275f)
        seg.testDirection(0.5f, -9.0f, 2.45f)
        seg.testDirection(0.75f, -11.0f, 5.175f)
        seg.testDirection(1.0f, -13.0f, 7.9f)
    }

    fun EdgeSegment.Quadratic.testDirection(param: Float, directionX: Float, directionY: Float) {
        val result = direction(param)
        assertEquals(directionX, result.x, epsilon)
        assertEquals(directionY, result.y, epsilon)
    }
}
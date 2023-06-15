package dev.luna5ama.mdsfgen

class EdgeHolder(private var edgeSegment: EdgeSegment? = null) {
    constructor(p0: Point2, p1: Point2, edgeColor: EdgeColor = EdgeColor.WHITE) : this(
        EdgeSegment.Linear(
            p0,
            p1,
            edgeColor
        )
    )

    constructor(p0: Point2, p1: Point2, p2: Point2, edgeColor: EdgeColor = EdgeColor.WHITE) : this(
        EdgeSegment.Quadratic(
            p0,
            p1,
            p2,
            edgeColor
        )
    )

    constructor(p0: Point2, p1: Point2, p2: Point2, p3: Point2, edgeColor: EdgeColor = EdgeColor.WHITE) : this(
        EdgeSegment.Cubic(
            p0,
            p1,
            p2,
            p3,
            edgeColor
        )
    )

    constructor(orig: EdgeHolder) : this(orig.edgeSegment?.clone())

    fun set(orig: EdgeHolder) {
        if (this !== orig) {
            edgeSegment = orig.edgeSegment?.clone()
        }
    }

    fun set(new: EdgeSegment) {
        edgeSegment = new
    }

    fun get(): EdgeSegment {
        return edgeSegment!!
    }

    operator fun not(): Boolean {
        return edgeSegment == null
    }

    operator fun getValue(thisRef: Any?, property: Any?) = edgeSegment!!

    operator fun setValue(thisRef: Any?, property: Any?, value: EdgeSegment) {
        edgeSegment = value
    }
}
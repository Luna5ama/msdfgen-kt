package dev.luna5ama.mdsfgen

/**
 * A 2D bounding box.
 */
data class Bound(
    var l: Float = Float.MAX_VALUE,
    var b: Float = Float.MAX_VALUE,
    var r: Float = -Float.MAX_VALUE,
    var t: Float = -Float.MAX_VALUE
) {
    val width: Float
        get() = r - l

    val height: Float
        get() = t - b
}
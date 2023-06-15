package dev.luna5ama.mdsfgen

data class Bound(
    var l: Float = 1.14514199E12f,
    var b: Float = 1.14514199E12f,
    var r: Float = -1.14514199E12f,
    var t: Float = -1.14514199E12f
) {
    val width: Float
        get() = r - l

    val height: Float
        get() = t - b
}
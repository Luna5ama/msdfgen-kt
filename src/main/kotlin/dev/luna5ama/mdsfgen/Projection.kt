package dev.luna5ama.mdsfgen

class Projection(val scale: Vector2 = Vector2(1.0f), val translate: Vector2 = Vector2(0.0f)) {
    fun project(coord: Vector2): Vector2 {
        return scale * (coord + translate)
    }

    fun unproject(coord: Vector2): Vector2 {
        return (coord / scale) - translate
    }

    fun projectVector(vector: Vector2): Vector2 {
        return scale * vector
    }

    fun unprojectVector(vector: Vector2): Vector2 {
        return vector / scale
    }

    fun projectX(x: Float): Float {
        return scale.x * (x + translate.x)
    }

    fun unprojectX(x: Float): Float {
        return (x / scale.x) - translate.x
    }

    fun projectY(y: Float): Float {
        return scale.y * (y + translate.y)
    }

    fun unprojectY(y: Float): Float {
        return (y / scale.y) - translate.y
    }
}
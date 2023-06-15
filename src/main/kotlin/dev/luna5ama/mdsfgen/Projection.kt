package dev.luna5ama.mdsfgen

/**
 * A transformation from shape coordinates to pixel coordinates.
 */
class Projection(val scale: Vector2 = Vector2(1.0f), val translate: Vector2 = Vector2(0.0f)) {
    /**
     * Converts the shape coordinate to pixel coordinate.
     */
    fun project(coord: Vector2): Vector2 {
        return scale * (coord + translate)
    }

    /**
     * Converts the pixel coordinate to shape coordinate.
     */
    fun unproject(coord: Vector2): Vector2 {
        return (coord / scale) - translate
    }

    /**
     * Converts the vector to pixel coordinate space.
     */
    fun projectVector(vector: Vector2): Vector2 {
        return scale * vector
    }

    /**
     * Converts the vector to shape coordinate space.
     */
    fun unprojectVector(vector: Vector2): Vector2 {
        return vector / scale
    }

    /**
     * Converts the X-coordinate from shape to pixel coordinate space.
     */
    fun projectX(x: Float): Float {
        return scale.x * (x + translate.x)
    }

    /**
     * Converts the X-coordinate from pixel to shape coordinate space.
     */
    fun unprojectX(x: Float): Float {
        return (x / scale.x) - translate.x
    }

    /**
     * Converts the Y-coordinate from shape to pixel coordinate space.
     */
    fun projectY(y: Float): Float {
        return scale.y * (y + translate.y)
    }

    /**
     * Converts the Y-coordinate from pixel to shape coordinate space.
     */
    fun unprojectY(y: Float): Float {
        return (y / scale.y) - translate.y
    }
}
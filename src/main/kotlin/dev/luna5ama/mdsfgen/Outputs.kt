package dev.luna5ama.mdsfgen

import java.awt.image.BufferedImage

/**
 * A function that maps a float value to a byte value.
 */
fun interface MapPixelFunction {
    fun map(value: Float): Float

    operator fun invoke(value: Float): Byte {
        return map(value * 255.0f).toInt().coerceIn(0, 255).toByte()
    }
}

/**
 * Converts the output data to a buffered image.
 */
fun MsdfGen.OutputData<*>.toBufferedImage(mapFunc: MapPixelFunction): BufferedImage {
    return when (this.channels) {
        1 -> {
            val image = BufferedImage(this.width, this.height, BufferedImage.TYPE_BYTE_GRAY)
            val raster = image.raster
            val data = ByteArray(1)
            for (y in 0 until this.height) {
                for (x in 0 until this.width) {
                    data[0] = mapFunc(this[x, y, 0])
                    raster.setDataElements(x, y, data)
                }
            }
            image
        }
        3 -> {
            val image = BufferedImage(this.width, this.height, BufferedImage.TYPE_3BYTE_BGR)
            val raster = image.raster
            val data = ByteArray(3)
            for (y in 0 until this.height) {
                for (x in 0 until this.width) {
                    data[0] = mapFunc(this[x, y, 0])
                    data[1] = mapFunc(this[x, y, 1])
                    data[2] = mapFunc(this[x, y, 2])
                    raster.setDataElements(x, y, data)
                }
            }
            image
        }
        4 -> {
            val image = BufferedImage(this.width, this.height, BufferedImage.TYPE_4BYTE_ABGR)
            val raster = image.raster
            val data = ByteArray(4)
            for (y in 0 until this.height) {
                for (x in 0 until this.width) {
                    data[0] = mapFunc(this[x, y, 3])
                    data[1] = mapFunc(this[x, y, 2])
                    data[2] = mapFunc(this[x, y, 1])
                    data[3] = mapFunc(this[x, y, 0])
                    raster.setDataElements(x, y, data)
                }
            }
            image
        }
        else -> throw IllegalArgumentException("Unsupported number of channels: ${this.channels}")
    }
}
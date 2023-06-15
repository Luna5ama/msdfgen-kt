package dev.luna5ama.mdsfgen

@JvmInline
value class EdgeColor(val bit: Int) {
    companion object {
        const val BLACK_BIT = 0b000
        const val RED_BIT = 0b001
        const val GREEN_BIT = 0b010
        const val YELLOW_BIT = 0b011
        const val BLUE_BIT = 0b100
        const val MAGENTA_BIT = 0b101
        const val CYAN_BIT =  0b110
        const val WHITE_BIT = 0b111

        val BLACK = EdgeColor(BLACK_BIT)
        val RED = EdgeColor(RED_BIT)
        val GREEN = EdgeColor(GREEN_BIT)
        val YELLOW = EdgeColor(YELLOW_BIT)
        val BLUE = EdgeColor(BLUE_BIT)
        val MAGENTA = EdgeColor(MAGENTA_BIT)
        val CYAN = EdgeColor(CYAN_BIT)
        val WHITE = EdgeColor(WHITE_BIT)
    }

    infix fun or(other: EdgeColor) = EdgeColor(bit or other.bit)

    infix fun and(other: EdgeColor) = EdgeColor(bit and other.bit)

    infix fun xor(other: EdgeColor) = EdgeColor(bit xor other.bit)

    fun toBoolean() = bit.toBoolean()

    @JvmInline
    value class Array(val array: IntArray) {
        operator fun get(index: Int) = EdgeColor(array[index])
        operator fun set(index: Int, value: EdgeColor) {
            array[index] = value.bit
        }

        companion object {
            fun of(c: EdgeColor) = Array(intArrayOf(c.bit))

            fun of(c1: EdgeColor, c2: EdgeColor) = Array(intArrayOf(c1.bit, c2.bit))

            fun of(c1: EdgeColor, c2: EdgeColor, c3: EdgeColor) = Array(intArrayOf(c1.bit, c2.bit, c3.bit))
        }
    }
}
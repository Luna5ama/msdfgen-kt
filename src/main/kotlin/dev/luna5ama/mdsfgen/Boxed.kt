package dev.luna5ama.mdsfgen

class BoxedFloat(var v: Float = 0.0f) {
    operator fun getValue(thisRef: Any?, property: Any?) = v
    operator fun setValue(thisRef: Any?, property: Any?, value: Float) {
        v = value
    }
}

class BoxedLong(var v: Long = 0L) {
    operator fun getValue(thisRef: Any?, property: Any?) = v
    operator fun setValue(thisRef: Any?, property: Any?, value: Long) {
        v = value
    }
}
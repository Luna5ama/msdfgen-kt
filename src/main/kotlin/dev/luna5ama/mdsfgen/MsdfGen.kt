package dev.luna5ama.mdsfgen

object MsdfGen {
    fun <DistanceType> generateDistanceField(
        output: OutputData<DistanceType>,
        contourCombiner: ContourCombiner<EdgeSelectorRef<DistanceType, Any>, DistanceType, Any>,
        shape: Shape,
        projection: Projection
    ) {
        val distanceFinder = ShapeDistanceFinder(contourCombiner, shape)
        var rightToLeft = false

        for (y in 0 until output.height) {
            val row = if (shape.inverseYAxis) output.height - y - 1 else y
            for (col in 0 until output.width) {
                val x = if (rightToLeft) output.width - col - 1 else col
                val p = projection.unproject(Point2(x + 0.5f, y + 0.5f))
                val distance = distanceFinder.distance(p)
                output[x, row] = distance
            }
            rightToLeft = !rightToLeft
        }
    }

    fun generateSDF(
        output: OutputData<BoxedFloat>,
        shape: Shape,
        projection: Projection,
        overlapSupport: Boolean = true
    ) {
        val combiner = if (overlapSupport) {
            OverlappingContourCombiner<BoxedFloat>(shape)
        } else {
            SimpleContourCombiner<BoxedFloat>(shape)
        }
        generateDistanceField(output, combiner, shape, projection)
    }

    fun generatePseudoSDF(
        output: OutputData<BoxedFloat>,
        shape: Shape,
        projection: Projection,
        overlapSupport: Boolean = true
    ) {
        val combiner = if (overlapSupport) {
            OverlappingContourCombiner<BoxedFloat>(shape)
        } else {
            SimpleContourCombiner<BoxedFloat>(shape)
        }
        generateDistanceField(output, combiner, shape, projection)
    }

    fun generateMSDF(
        output: OutputData<MultiDistance>,
        shape: Shape,
        projection: Projection,
        overlapSupport: Boolean = true
    ) {
        val combiner = if (overlapSupport) {
            OverlappingContourCombiner<MultiDistance>(shape)
        } else {
            SimpleContourCombiner<MultiDistance>(shape)
        }
        generateDistanceField(output, combiner, shape, projection)
    }

    fun generateMTSDF(
        output: OutputData<MultiAndTrueDistance>,
        shape: Shape,
        projection: Projection,
        overlapSupport: Boolean = true
    ) {
        val combiner = if (overlapSupport) {
            OverlappingContourCombiner<MultiAndTrueDistance>(shape)
        } else {
            SimpleContourCombiner<MultiAndTrueDistance>(shape)
        }
        generateDistanceField(output, combiner, shape, projection)
    }

    fun generateSDFLegacy(
        output: OutputData<BoxedFloat>,
        shape: Shape,
        projection: Projection
    ) {
        for (y in 0 until output.height) {
            val row = if (shape.inverseYAxis) output.height - y - 1 else y
            for (x in 0 until output.width) {
                val dummy = BoxedFloat()
                val p = projection.unproject(Point2(x + 0.5f, y + 0.5f))
                var minDistance = SignedDistance()
                for (contour in shape.contours) {
                    for (edge in contour.edges) {
                        val distance = edge.get().signedDistance(p, dummy)
                        if (distance < minDistance) {
                            minDistance = distance
                        }
                    }
                }
                dummy.v = minDistance.distance
                output[x, row] = dummy
            }
        }
    }

    fun generateMSDFLegacy(
        output: OutputData<MultiDistance>,
        shape: Shape,
        projection: Projection
    ) {
        for (y in 0 until output.height) {
            val row = if (shape.inverseYAxis) output.height - y - 1 else y
            for (x in 0 until output.width) {
                val p = projection.unproject(Point2(x + 0.5f, y + 0.5f))

                val r = Temp()
                val g = Temp()
                val b = Temp()

                for (contour in shape.contours) {
                    for (edgeHolder in contour.edges) {
                        val edge = edgeHolder.get()
                        val param = BoxedFloat()
                        val distance = edge.signedDistance(p, param)
                        if ((edge.color and EdgeColor.RED).toBoolean() && distance < r.minDistance) {
                            r.minDistance = distance
                            r.nearEdge = edge
                            r.nearParam = param.v
                        }
                        if ((edge.color and EdgeColor.GREEN).toBoolean() && distance < g.minDistance) {
                            g.minDistance = distance
                            g.nearEdge = edge
                            g.nearParam = param.v
                        }
                        if ((edge.color and EdgeColor.BLUE).toBoolean() && distance < b.minDistance) {
                            b.minDistance = distance
                            b.nearEdge = edge
                            b.nearParam = param.v
                        }
                    }
                }

                if (r.nearEdge != null) {
                    r.nearEdge!!.distanceToPseudoDistance(r.minDistance, p, r.nearParam)
                }
                if (g.nearEdge != null) {
                    g.nearEdge!!.distanceToPseudoDistance(g.minDistance, p, g.nearParam)
                }
                if (b.nearEdge != null) {
                    b.nearEdge!!.distanceToPseudoDistance(b.minDistance, p, b.nearParam)
                }
                output[x, row] = MultiDistance(r.minDistance.distance, g.minDistance.distance, b.minDistance.distance)
            }
        }
    }

    private data class Temp(
        var minDistance: SignedDistance = SignedDistance(),
        var nearEdge: EdgeSegment? = null,
        var nearParam: Float = 0.0f,
    )


    sealed class OutputData<DistanceType>(
        val width: Int,
        val height: Int,
        val channels: Int
    ) {
        val data = FloatArray(width * height * channels)

        operator fun get(x: Int, y: Int, c: Int): Float {
            return data[(x + y * width) * channels + c]
        }

        abstract operator fun set(x: Int, y: Int, distance: DistanceType)

        class Single(width: Int, height: Int) : OutputData<BoxedFloat>(width, height, 1) {
            override fun set(x: Int, y: Int, distance: BoxedFloat) {
                data[x + y * width] = distance.v
            }
        }

        class Multi(width: Int, height: Int) : OutputData<MultiDistance>(width, height, 3) {
            override fun set(x: Int, y: Int, distance: MultiDistance) {
                val i = (x + y * width) * channels
                data[i] = distance.r
                data[i + 1] = distance.g
                data[i + 2] = distance.b
            }
        }

        class MultiAndTrue(width: Int, height: Int) : OutputData<MultiAndTrueDistance>(width, height, 4) {
            override fun set(x: Int, y: Int, distance: MultiAndTrueDistance) {
                val i = (x + y * width) * channels
                data[i] = distance.r
                data[i + 1] = distance.g
                data[i + 2] = distance.b
                data[i + 3] = distance.a
            }
        }

        companion object {
            @Suppress("UNCHECKED_CAST")
            inline operator fun <reified DistanceType> invoke(width: Int, height: Int): OutputData<DistanceType> {
                return when (DistanceType::class.java) {
                    BoxedFloat::class.java -> Single(width, height)
                    MultiDistance::class.java -> Multi(width, height)
                    MultiAndTrueDistance::class.java -> MultiAndTrue(width, height)
                    else -> throw IllegalArgumentException("Unsupported distance type: ${DistanceType::class.java}")
                } as OutputData<DistanceType>
            }
        }
    }
}
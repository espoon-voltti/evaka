package fi.espoo.evaka.ai

import fi.espoo.evaka.shared.domain.Coordinate
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val r = 6371000
private const val hc = Math.PI / 180
fun calcDistance(from: Coordinate, to: Coordinate): Double {
    val p1 = from.lat * hc
    val p2 = to.lat * hc
    val dp = (to.lat - from.lat) * hc
    val dl = (to.lon - from.lon) * hc

    val sinDP = sin(dp / 2)
    val sinDL = sin(dl / 2)
    val a = sinDP * sinDP + cos(p1) * cos(p2) * sinDL * sinDL
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return r * c
}

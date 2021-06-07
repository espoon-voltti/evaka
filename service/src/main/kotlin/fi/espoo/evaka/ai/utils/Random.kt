package fi.espoo.evaka.ai.utils

import fi.espoo.evaka.ai.Parameters
import java.lang.IllegalArgumentException
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

val random = Random(Parameters.randomSeed)

fun <T> Collection<T>.pickRandom(): T {
    if (this.isEmpty()) throw IllegalArgumentException("Cannot pick from empty collection")

    val index = floor(random.nextDouble() * this.size).roundToInt()
    return this.toList()[index]
}

fun <T> Collection<T>.pickRandomWithExpDistribution(exp: Double): T {
    if (this.isEmpty()) throw IllegalArgumentException("Cannot pick from empty collection")

    val index = floor(random.nextDouble().pow(exp) * this.size).roundToInt()
    return this.toList()[index]
}

fun randomGene(): Int {
    return random.nextDouble().let {
        when {
            it < 0.005 -> 4
            it < 0.015 -> 3
            it < 0.2 -> 2
            it < 0.5 -> 1
            else -> 0
        }
    }
}

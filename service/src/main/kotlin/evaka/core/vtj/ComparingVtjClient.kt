// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.identity.Ssn
import evaka.core.shared.EvakaUserId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.RealEvakaClock
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

private const val MAX_ATTEMPTS = 3

/**
 * Returns the results of [soapClient], and compares them with [dvvClient] in a background thread
 * every [interval], so that the comparison does not slow down or break the calls. A call inside
 * [withoutComparison] is not compared.
 */
class ComparingVtjClient(
    private val soapClient: VtjClient,
    private val dvvClient: VtjClient,
    interval: Duration = Duration.ofSeconds(30),
    queueSize: Int = 1000,
    private val clock: EvakaClock = RealEvakaClock(),
) : VtjClient, AutoCloseable {
    private val queue = ArrayBlockingQueue<Pending<*>>(queueSize)
    @Volatile private var pausedUntil = clock.now()
    private val executor = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "vtj-comparison").apply { isDaemon = true }
    }

    init {
        executor.scheduleWithFixedDelay(
            {
                try {
                    compareQueued()
                } catch (e: Exception) {
                    logger.error(e) { "VTJ comparison failed" }
                }
            },
            interval.toMillis(),
            interval.toMillis(),
            TimeUnit.MILLISECONDS,
        )
    }

    override fun fetchPersons(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        soapClient.fetchPersons(requestingUser, ssns).also {
            enqueue(personsComparison, requestingUser, ssns, it)
        }

    override fun fetchWithGuardians(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        soapClient.fetchWithGuardians(requestingUser, ssns).also {
            enqueue(guardiansComparison, requestingUser, ssns, it)
        }

    override fun fetchWithDependants(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        soapClient.fetchWithDependants(requestingUser, ssns).also {
            enqueue(dependantsComparison, requestingUser, ssns, it)
        }

    /** Compares the queued calls with one DVV query for each query type. */
    fun compareQueued() {
        if (clock.now() < pausedUntil) return
        val pending = buildList { queue.drainTo(this) }
        listOf(personsComparison, guardiansComparison, dependantsComparison).forEach { comparison ->
            val items = pending.filter { it.comparison == comparison }
            // The query of an earlier query type in this round failed
            if (pausedUntil > clock.now()) items.forEach(::offer)
            else if (!compare(comparison, items)) {
                // The failure can be the rate limit of DVV
                pausedUntil = clock.now().plusMinutes(1)
                items.forEach { item ->
                    if (item.failedAttempts + 1 < MAX_ATTEMPTS) {
                        offer(item.attempted())
                    } else {
                        log(item, ComparisonResult.DVV_ERROR)
                    }
                }
            }
        }
    }

    override fun close() {
        executor.shutdownNow()
    }

    private fun <T : Any> enqueue(
        comparison: Comparison<T>,
        requestingUser: EvakaUserId,
        ssns: Collection<Ssn>,
        soap: Map<Ssn, T>,
    ) {
        if (skipComparison.get()) return
        val distinct = ssns.distinct()
        logger.info(mapOf("queryType" to comparison.name, "batchSize" to distinct.size)) {
            "VTJ call ${comparison.name} with ${distinct.size} SSNs"
        }
        distinct.forEach { ssn -> offer(Pending(comparison, ssn, requestingUser, soap[ssn])) }
    }

    private fun offer(pending: Pending<*>) {
        if (!queue.offer(pending)) log(pending, ComparisonResult.DROPPED)
    }

    private fun <T : Any> compare(comparison: Comparison<T>, items: List<Pending<*>>): Boolean {
        if (items.isEmpty()) return true
        @Suppress("UNCHECKED_CAST") val pending = items as List<Pending<T>>
        val dvv =
            try {
                comparison.fetch(
                    dvvClient,
                    AuthenticatedUser.SystemInternalUser.evakaUserId,
                    pending.map { it.ssn }.distinct(),
                )
            } catch (e: Exception) {
                logger.error(e) { "DVV basic info query for the VTJ comparison failed" }
                return false
            }
        pending.forEach { item ->
            val soap = item.soap
            val dvvResult = dvv[item.ssn]
            when {
                soap == null && dvvResult == null -> log(item, ComparisonResult.EQUAL)
                soap == null -> log(item, ComparisonResult.SOAP_NOT_FOUND)
                dvvResult == null -> log(item, ComparisonResult.DVV_NOT_FOUND)
                else -> {
                    val differences = comparison.compare(soap, dvvResult)
                    log(
                        item,
                        if (differences.isEmpty()) ComparisonResult.EQUAL
                        else ComparisonResult.DIFFERENT,
                        differences,
                    )
                }
            }
        }
        return true
    }

    private fun log(
        pending: Pending<*>,
        result: ComparisonResult,
        differences: List<VtjFieldDifference> = emptyList(),
    ) =
        logger.info(
            mapOf(
                "path" to "BASIC_INFO",
                "queryType" to pending.comparison.name,
                "result" to result.name,
                "differences" to differences.map { mapOf("field" to it.field) + it.details },
                "requestingUser" to pending.requestingUser.raw.toString(),
                "targetId" to pending.ssn.value.take(6),
            )
        ) {
            "VTJ comparison of ${pending.comparison.name}: ${result.name}"
        }

    companion object {
        private val skipComparison = ThreadLocal.withInitial { false }

        fun <T> withoutComparison(f: () -> T): T {
            skipComparison.set(true)
            try {
                return f()
            } finally {
                skipComparison.set(false)
            }
        }
    }
}

enum class ComparisonResult {
    EQUAL,
    DIFFERENT,
    SOAP_NOT_FOUND,
    DVV_NOT_FOUND,
    DVV_ERROR,
    DROPPED,
}

private class Comparison<T : Any>(
    val name: String,
    val fetch: (VtjClient, EvakaUserId, Collection<Ssn>) -> Map<Ssn, T>,
    val compare: (T, T) -> List<VtjFieldDifference>,
)

private val personsComparison =
    Comparison(
        "fetchPersons",
        { client, user, ssns -> client.fetchPersons(user, ssns) },
        { soap, dvv -> compareVtjPersons(soap, dvv) },
    )

private val guardiansComparison =
    Comparison(
        "fetchWithGuardians",
        { client, user, ssns -> client.fetchWithGuardians(user, ssns) },
        { soap, dvv -> compareVtjPersons(soap, dvv) },
    )

private val dependantsComparison =
    Comparison(
        "fetchWithDependants",
        { client, user, ssns -> client.fetchWithDependants(user, ssns) },
        { soap, dvv -> compareVtjPersons(soap, dvv) },
    )

private class Pending<T : Any>(
    val comparison: Comparison<T>,
    val ssn: Ssn,
    val requestingUser: EvakaUserId,
    val soap: T?,
    val failedAttempts: Int = 0,
) {
    fun attempted() = Pending(comparison, ssn, requestingUser, soap, failedAttempts + 1)
}

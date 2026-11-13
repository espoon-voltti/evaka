// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.dvv.DvvBasicInfo
import evaka.core.dvv.DvvModificationsServiceClient
import evaka.core.dvv.Tietoryhma
import evaka.core.dvv.custodyCounts
import evaka.core.dvv.startDate
import evaka.core.identity.Ssn
import evaka.core.shared.EvakaUserId
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.RealEvakaClock
import fi.espoo.voltti.logging.loggers.auditVTJ
import fi.espoo.voltti.logging.loggers.info
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

/** Fetches persons from the basic info query (perustiedot) of the DVV modifications service */
class DvvVtjClient(
    private val client: DvvModificationsServiceClient,
    private val clock: EvakaClock = RealEvakaClock(),
) : VtjClient {
    override fun fetchPersons(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch(requestingUser, ssns, dvvPersonInfoGroups) { ssn, groups, today ->
            mapDvvPerson(ssn, groups, today)
        }

    override fun fetchWithGuardians(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch(requestingUser, ssns, dvvPersonInfoGroups + HUOLTAJA_SUPPEA) { ssn, groups, today ->
            VtjPersonAndGuardians(mapDvvPerson(ssn, groups, today), mapDvvGuardians(groups, today))
        }

    override fun fetchWithDependants(requestingUser: EvakaUserId, ssns: Collection<Ssn>) =
        fetch(requestingUser, ssns, dvvPersonInfoGroups + HUOLLETTAVA_SUPPEA) { ssn, groups, today
            ->
            VtjPersonAndDependants(
                mapDvvPerson(ssn, groups, today),
                mapDvvDependants(groups, today),
            )
        }

    private fun <T> fetch(
        requestingUser: EvakaUserId,
        ssns: Collection<Ssn>,
        infoGroups: List<String>,
        map: (Ssn, List<Tietoryhma>, LocalDate) -> T,
    ): Map<Ssn, T> {
        val today = clock.today()
        val rows = query(requestingUser, ssns.distinct(), infoGroups)

        // For a corrected SSN, DVV returns only the SSN correction, so the data comes from a second
        // query with the active SSN.
        val activeSsns =
            rows.mapNotNull { (ssn, row) -> row.correctedToSsn()?.let { ssn to it } }.toMap()
        val activeRows = query(requestingUser, activeSsns.values.distinct(), infoGroups)

        return rows
            .mapNotNull { (ssn, row) ->
                val activeRow =
                    activeSsns[ssn]?.let { activeRows[it] ?: return@mapNotNull null } ?: row
                ssn to map(Ssn.from(activeRow.henkilotunnus), activeRow.tietoryhmat, today)
            }
            .toMap()
    }

    private fun query(
        requestingUser: EvakaUserId,
        ssns: List<Ssn>,
        infoGroups: List<String>,
    ): Map<Ssn, DvvBasicInfo> =
        ssns
            .chunked(1000)
            .flatMap { chunk ->
                val dvvTraceId = UUID.randomUUID()
                val response =
                    try {
                        client.getBasicInfo(
                            chunk.map { it.value },
                            infoGroups,
                            dvvTraceId,
                            requestingUser,
                        )
                    } catch (e: Exception) {
                        chunk.forEach { audit(it, dvvTraceId, "ERROR") }
                        throw e
                    }
                val groups = response.perustiedot.flatMap { it.tietoryhmat }
                val today = clock.today()
                logger.info(
                    mapOf(
                        "dvvTraceId" to dvvTraceId.toString(),
                        "requested" to chunk.size,
                        "rows" to response.perustiedot.size,
                        "groups" to groups.groupingBy { it.tietoryhma }.eachCount(),
                        "futureGroups" to
                            groups
                                .filter { it.startDate()?.isAfter(today) == true }
                                .groupingBy { it.tietoryhma }
                                .eachCount(),
                        "custody" to custodyCounts(groups),
                    )
                ) {
                    "DVV basic info of ${chunk.size} SSNs returned ${response.perustiedot.size} rows"
                }
                val rows =
                    response.perustiedot
                        .mapNotNull { row -> Ssn.parse(row.henkilotunnus)?.let { it to row } }
                        .toMap()
                chunk.forEach { ssn ->
                    val row = rows[ssn]
                    audit(
                        ssn,
                        dvvTraceId,
                        when {
                            row == null -> "NOT_FOUND"
                            row.correctedToSsn() != null -> "CORRECTED"
                            else -> "FOUND"
                        },
                    )
                }
                rows.toList()
            }
            .toMap()

    private fun audit(ssn: Ssn, dvvTraceId: UUID, result: String) =
        logger.auditVTJ(
            mapOf(
                "meta" to
                    mapOf("queryName" to "perustiedot", "dvvTraceId" to dvvTraceId.toString()),
                "result" to result,
                "targetId" to ssn.value.take(6),
            )
        ) {
            "DVV basic info query: $result"
        }
}

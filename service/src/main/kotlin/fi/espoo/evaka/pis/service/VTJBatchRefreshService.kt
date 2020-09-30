// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.VTJRefresh
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.shared.db.runAfterCommit
import fi.espoo.evaka.shared.db.withSpringTx
import mu.KotlinLogging
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.lang.Exception
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class VTJBatchRefreshService(
    private val personService: PersonService,
    private val parentshipService: ParentshipService,
    private val asyncJobRunner: AsyncJobRunner,
    private val jdbc: NamedParameterJdbcTemplate,
    private val txManager: PlatformTransactionManager
) {

    init {
        asyncJobRunner.vtjRefresh = ::doVTJRefresh
    }

    @Transactional
    fun scheduleBatch(): Int {
        deleteOldJobs()

        val requestingUserId = UUID.fromString("00000000-0000-0000-0000-000000000000")
        val personIds = getPersonIdsToRefresh()
        personIds.forEachIndexed { i, personId ->
            kotlin.run {
                asyncJobRunner.plan(
                    payloads = listOf(
                        VTJRefresh(
                            personId = personId,
                            requestingUserId = requestingUserId
                        )
                    ),
                    runAt = Instant.now().plusSeconds(i.toLong())
                )
            }
        }

        runAfterCommit { asyncJobRunner.scheduleImmediateRun() }
        return personIds.size
    }

    private fun deleteOldJobs() {
        // language=sql
        val sql = "DELETE FROM async_job WHERE type = 'VTJ_REFRESH' AND (claimed_by IS NULL OR completed_at IS NOT NULL);"
        jdbc.update(sql, mapOf<String, String>())
    }

    private fun getPersonIdsToRefresh(): Set<UUID> {
        // language=sql
        val sql =
            """
            SELECT head_of_child FROM fridge_child 
            WHERE daterange(start_date, end_date, '[]') @> current_date AND conflict = false
            """.trimIndent()
        return jdbc.query(sql, mapOf<String, String>()) { rs, _ -> rs.getUUID("head_of_child") }.toSet()
    }

    private fun livesInSameAddress(set1: Set<PersonAddressDTO>, set2: Set<PersonAddressDTO>): Boolean {
        val sameResidencyCode = set1
            .map { it.residenceCode }
            .filter { !it.isNullOrEmpty() }
            .intersect(
                set2
                    .map { it.residenceCode }
                    .filter { !it.isNullOrEmpty() }
            )
            .isNotEmpty()

        val sameStreetAddress = set1
            .filter { it.streetAddress.isNotEmpty() }
            .map { Pair(it.streetAddress, it.city) }
            .intersect(
                set2
                    .filter { it.streetAddress.isNotEmpty() }
                    .map { Pair(it.streetAddress, it.city) }
            )
            .isNotEmpty()

        return sameResidencyCode || sameStreetAddress
    }

    @Transactional
    fun doVTJRefresh(msg: VTJRefresh) {
        logger.info("Refreshing ${msg.personId} from VTJ")
        val head = personService.getUpToDatePersonWithChildren(
            user = AuthenticatedUser(msg.requestingUserId, setOf()),
            id = msg.personId
        ) ?: return
        logger.info("Person to refresh has ${head.children.size} children")

        val partner = getPartnerId(msg.personId)
            ?.also { logger.info("Person has fridge partner $it") }
            ?.let { partnerId ->
                personService.getUpToDatePersonWithChildren(
                    user = AuthenticatedUser(msg.requestingUserId, setOf()),
                    id = partnerId
                )
            }
            ?.takeIf { livesInSameAddress(it.addresses, head.addresses) }
        if (partner != null) logger.info("Partner lives in the same address and has ${partner.children.size} children")

        val children = head.children + (partner?.children ?: emptyList())

        val currentFridgeChildren = getCurrentFridgeChildren(msg.personId)
        logger.info("Currently person has ${currentFridgeChildren.size} fridge children in evaka")

        val newChildrenInSameAddress = children
            .asSequence()
            .distinct()
            .filter { child -> !child.socialSecurityNumber.isNullOrBlank() }
            .filter { child -> child.dateOfBirth.isAfter(LocalDate.now().minusYears(18)) }
            .filter { livesInSameAddress(it.addresses, head.addresses) }
            .filter { !currentFridgeChildren.contains(it.id) }
            .toList()
        logger.info("New frige children to add: ${newChildrenInSameAddress.size}")

        newChildrenInSameAddress.forEach { child ->
            try {
                withSpringTx(txManager = txManager, requiresNew = true) {
                    parentshipService.createParentship(
                        childId = child.id,
                        headOfChildId = msg.personId,
                        startDate = LocalDate.now(),
                        endDate = child.dateOfBirth.plusYears(18).minusDays(1)
                    )
                }
                logger.info("Child ${child.id} added")
            } catch (e: Exception) {
                logger.info("Ignored exception", e)
            }
        }
        logger.info("Completed refreshing person ${msg.personId}")
    }

    fun getPartnerId(personId: UUID): UUID? {
        // language=sql
        val sql =
            """
            SELECT p2.person_id AS partner_id
            FROM fridge_partner p1
            LEFT OUTER JOIN fridge_partner p2 ON p1.partnership_id = p2.partnership_id AND p1.person_id != p2.person_id
            WHERE p1.person_id = :personId AND daterange(p1.start_date, p1.end_date, '[]') @> current_date AND p1.conflict = false AND p2.conflict = false
            """.trimIndent()

        return jdbc.query(sql, mapOf("personId" to personId)) { rs, _ -> rs.getUUID("partner_id") }.firstOrNull()
    }

    private fun getCurrentFridgeChildren(personId: UUID): Set<UUID> {
        // language=sql
        val sql =
            """
            SELECT child_id FROM fridge_child 
            WHERE head_of_child = :personId AND daterange(start_date, end_date, '[]') @> current_date AND conflict = false
            """.trimIndent()
        return jdbc.query(sql, mapOf("personId" to personId)) { rs, _ -> rs.getUUID("child_id") }.toSet()
    }
}

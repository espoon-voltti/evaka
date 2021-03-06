// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.VTJRefresh
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class FridgeFamilyService(
    private val personService: PersonService,
    private val parentshipService: ParentshipService,
    private val asyncJobRunner: AsyncJobRunner
) {

    fun doVTJRefresh(db: Database, msg: VTJRefresh) {
        logger.info("Refreshing ${msg.personId} from VTJ")
        val head = db.transaction {
            personService.getUpToDatePersonWithChildren(
                it,
                user = AuthenticatedUser.Employee(msg.requestingUserId, setOf()),
                id = msg.personId
            )
        }
        if (head != null) {
            logger.info("Person to refresh has ${head.children.size} children")

            val partner = db.read { getPartnerId(it, msg.personId) }
                ?.also { logger.info("Person has fridge partner $it") }
                ?.let { partnerId ->
                    db.transaction {
                        personService.getUpToDatePersonWithChildren(
                            it,
                            user = AuthenticatedUser.Employee(msg.requestingUserId, setOf()),
                            id = partnerId
                        )
                    }
                }
                ?.takeIf { livesInSameAddress(it.addresses, head.addresses) }
            if (partner != null) logger.info("Partner lives in the same address and has ${partner.children.size} children")

            val children = head.children + (partner?.children ?: emptyList())

            val currentFridgeChildren = db.read { getCurrentFridgeChildren(it, msg.personId) }
            logger.info("Currently person has ${currentFridgeChildren.size} fridge children in evaka")

            val newChildrenInSameAddress = children
                .asSequence()
                .distinct()
                .filter { child -> !child.socialSecurityNumber.isNullOrBlank() }
                .filter { child -> child.dateOfBirth.isAfter(LocalDate.now().minusYears(18)) }
                .filter { livesInSameAddress(it.addresses, head.addresses) }
                .filter { !currentFridgeChildren.contains(it.id) }
                .toList()
            logger.info("New fridge children to add: ${newChildrenInSameAddress.size}")

            newChildrenInSameAddress.forEach { child ->
                try {
                    db.transaction { tx ->
                        parentshipService.createParentship(
                            tx,
                            childId = child.id,
                            headOfChildId = msg.personId,
                            startDate = LocalDate.now(),
                            endDate = child.dateOfBirth.plusYears(18).minusDays(1)
                        )
                    }
                    asyncJobRunner.scheduleImmediateRun()
                    logger.info("Child ${child.id} added")
                } catch (e: Exception) {
                    logger.debug("Ignored the following:", e)
                }
            }
            logger.info("Completed refreshing person ${msg.personId}")
        }
    }

    fun getPartnerId(tx: Database.Read, personId: UUID): UUID? {
        // language=sql
        val sql =
            """
            SELECT p2.person_id AS partner_id
            FROM fridge_partner p1
            LEFT OUTER JOIN fridge_partner p2 ON p1.partnership_id = p2.partnership_id AND p1.person_id != p2.person_id
            WHERE p1.person_id = :personId AND daterange(p1.start_date, p1.end_date, '[]') @> current_date AND p1.conflict = false AND p2.conflict = false
            """.trimIndent()

        return tx.createQuery(sql).bind("personId", personId).mapTo<UUID>().firstOrNull()
    }

    private fun getCurrentFridgeChildren(tx: Database.Read, personId: UUID): Set<UUID> {
        // language=sql
        val sql =
            """
            SELECT child_id FROM fridge_child 
            WHERE head_of_child = :personId AND daterange(start_date, end_date, '[]') @> current_date AND conflict = false
            """.trimIndent()
        return tx.createQuery(sql).bind("personId", personId).mapTo<UUID>().list().toHashSet()
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
            .map { Pair(it.streetAddress.lowercase(), it.city.lowercase()) }
            .intersect(
                set2
                    .filter { it.streetAddress.isNotEmpty() }
                    .map { Pair(it.streetAddress.lowercase(), it.city.lowercase()) }
            )
            .isNotEmpty()

        return sameResidencyCode || sameStreetAddress
    }
}

// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.shared.async.VTJRefresh
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.transaction
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.stereotype.Service
import java.lang.Exception
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Service
class FridgeFamilyService(
    private val personService: PersonService,
    private val parentshipService: ParentshipService,
    private val jdbi: Jdbi
) {

    fun doVTJRefresh(msg: VTJRefresh) {
        logger.info("Refreshing ${msg.personId} from VTJ")
        val head = jdbi.transaction {
            personService.getUpToDatePersonWithChildren(
                it,
                user = AuthenticatedUser(msg.requestingUserId, setOf()),
                id = msg.personId
            )
        }
        if (head != null) {
            logger.info("Person to refresh has ${head.children.size} children")

            val partner = jdbi.transaction { h ->
                getPartnerId(h, msg.personId)
                    ?.also { logger.info("Person has fridge partner $it") }
                    ?.let { partnerId ->
                        personService.getUpToDatePersonWithChildren(
                            h,
                            user = AuthenticatedUser(msg.requestingUserId, setOf()),
                            id = partnerId
                        )
                    }
                    ?.takeIf { livesInSameAddress(it.addresses, head.addresses) }
            }
            if (partner != null) logger.info("Partner lives in the same address and has ${partner.children.size} children")

            val children = head.children + (partner?.children ?: emptyList())

            val currentFridgeChildren = jdbi.transaction { getCurrentFridgeChildren(it, msg.personId) }
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
                    jdbi.transaction { t ->
                        parentshipService.createParentship(
                            t,
                            childId = child.id,
                            headOfChildId = msg.personId,
                            startDate = LocalDate.now(),
                            endDate = child.dateOfBirth.plusYears(18).minusDays(1)
                        )
                    }
                    logger.info("Child ${child.id} added")
                } catch (e: Exception) {
                    logger.debug("Ignored the following:", e)
                }
            }
            logger.info("Completed refreshing person ${msg.personId}")
        }
    }

    fun getPartnerId(h: Handle, personId: UUID): UUID? {
        // language=sql
        val sql =
            """
            SELECT p2.person_id AS partner_id
            FROM fridge_partner p1
            LEFT OUTER JOIN fridge_partner p2 ON p1.partnership_id = p2.partnership_id AND p1.person_id != p2.person_id
            WHERE p1.person_id = :personId AND daterange(p1.start_date, p1.end_date, '[]') @> current_date AND p1.conflict = false AND p2.conflict = false
            """.trimIndent()

        return h.createQuery(sql).bind("personId", personId).mapTo<UUID>().firstOrNull()
    }

    private fun getCurrentFridgeChildren(h: Handle, personId: UUID): Set<UUID> {
        // language=sql
        val sql =
            """
            SELECT child_id FROM fridge_child 
            WHERE head_of_child = :personId AND daterange(start_date, end_date, '[]') @> current_date AND conflict = false
            """.trimIndent()
        return h.createQuery(sql).bind("personId", personId).mapTo<UUID>().list().toHashSet()
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
            .map { Pair(it.streetAddress.toLowerCase(), it.city.toLowerCase()) }
            .intersect(
                set2
                    .filter { it.streetAddress.isNotEmpty() }
                    .map { Pair(it.streetAddress.toLowerCase(), it.city.toLowerCase()) }
            )
            .isNotEmpty()

        return sameResidencyCode || sameStreetAddress
    }
}

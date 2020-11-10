// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.identity.ExternalIdentifier.SSN
import fi.espoo.evaka.pis.createParentship
import fi.espoo.evaka.pis.createPartnership
import fi.espoo.evaka.pis.getParentships
import fi.espoo.evaka.pis.getPartnershipsForPerson
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.InitializeFamilyFromApplication
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import mu.KotlinLogging
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class FamilyInitializerService(
    private val personService: PersonService,
    private val jdbi: Jdbi,
    asyncJobRunner: AsyncJobRunner
) {
    private val logger = KotlinLogging.logger {}

    init {
        asyncJobRunner.initializeFamilyFromApplication = ::handleInitializeFamilyFromApplication
    }

    fun handleInitializeFamilyFromApplication(msg: InitializeFamilyFromApplication) {
        val user = msg.user
        val application = jdbi.handle { h -> fetchApplicationDetails(h, msg.applicationId) }
        if (application != null) {
            val members = parseFridgeFamilyMembersFromApplication(user, application)
            tryInitFamilyFromApplication(members)
        } else {
            logger.warn("Could not initialize family, daycare application ${msg.applicationId} not found")
        }
    }

    private fun tryInitFamilyFromApplication(members: FridgeFamilyMembers) {
        try {
            if (members.headOfFamilyId == null) {
                logger.warn("Cannot create family because head of family could not be found")
                return
            }
            if (members.fridgeChildId == null) {
                logger.warn("Cannot create family because the main child on application could not be found")
                return
            }

            try {
                createParentship(
                    childId = members.fridgeChildId,
                    headOfChildId = members.headOfFamilyId
                )
            } catch (e: Throwable) {
                logger.warn("Adding ${members.fridgeChildId} as the main fridge child to ${members.headOfFamilyId} failed.")
            }

            if (members.fridgePartnerId != null) {
                try {
                    createPartnership(members.headOfFamilyId, members.fridgePartnerId)
                } catch (e: Throwable) {
                    logger.warn("Adding fridge partner ${members.fridgePartnerId} to ${members.headOfFamilyId} failed. Continuing with the rest of the family...")
                }
            }

            members.fridgeSiblingIds.forEach { siblingId ->
                try {
                    createParentship(childId = siblingId, headOfChildId = members.headOfFamilyId)
                } catch (e: Throwable) {
                    logger.warn("Adding $siblingId as a fridge child to ${members.headOfFamilyId} failed. Continuing with the rest of the family...")
                }
            }
        } catch (e: Throwable) {
            logger.warn("Unexpected error when initializing family from application", e)
        }
    }

    private data class FridgeFamilyMembers(
        val headOfFamilyId: UUID?,
        val fridgePartnerId: UUID?,
        val fridgeChildId: UUID?,
        val fridgeSiblingIds: List<UUID>
    )

    private fun parseFridgeFamilyMembersFromApplication(
        user: AuthenticatedUser,
        application: ApplicationDetails
    ): FridgeFamilyMembers = jdbi.transaction { h ->
        val updateStale = false

        val headOfFamilyId = application.guardianId

        val otherGuardianId = application.otherGuardianId
        val fridgePartnerSSN = if (
            otherGuardianId != null &&
            personService.personsLiveInTheSameAddress(h, user, headOfFamilyId, otherGuardianId)
        ) {
            (h.getPersonById(otherGuardianId)?.identity as? SSN)?.ssn
        } else {
            application.form.otherPartner?.socialSecurityNumber
        }

        val fridgePartnerId = fridgePartnerSSN
            ?.let { stringToSSN(it) }
            ?.let { personService.getOrCreatePerson(h, user, it, updateStale) }
            ?.id

        val fridgeChildId = application.childId

        val fridgeSiblingIds = application.form.otherChildren
            .mapNotNull { it.socialSecurityNumber }
            .mapNotNull { stringToSSN(it) }
            .mapNotNull { personService.getOrCreatePerson(h, user, it, updateStale)?.id }

        FridgeFamilyMembers(headOfFamilyId, fridgePartnerId, fridgeChildId, fridgeSiblingIds)
    }

    private fun stringToSSN(ssn: String): SSN? {
        return try {
            SSN.getInstance(ssn)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun createParentship(childId: UUID, headOfChildId: UUID) {
        val startDate = LocalDate.now()
        val alreadyExists = jdbi.handle { h ->
            h.getParentships(
                headOfChildId = headOfChildId,
                childId = childId,
                includeConflicts = true
            )
        }.any {
            (it.startDate.isBefore(startDate) || it.startDate.isEqual(startDate)) &&
                (it.endDate == null || it.endDate.isAfter(startDate))
        }
        if (alreadyExists) {
            logger.debug("Similar parentship already exists between $headOfChildId and $childId")
        } else {
            try {
                jdbi.transaction {
                    it.createParentship(
                        childId = childId,
                        headOfChildId = headOfChildId,
                        startDate = startDate,
                        endDate = null,
                        conflict = false
                    )
                }
            } catch (e: UnableToExecuteStatementException) {
                jdbi.transaction {
                    it.createParentship(
                        childId = childId,
                        headOfChildId = headOfChildId,
                        startDate = startDate,
                        endDate = null,
                        conflict = true
                    )
                }
            }
        }
    }

    private fun createPartnership(personId1: UUID, personId2: UUID) {
        val startDate = LocalDate.now()
        val alreadyExists =
            jdbi.handle { h -> h.getPartnershipsForPerson(personId = personId1, includeConflicts = true) }
                .any { partnership ->
                    partnership.partners.any { partner -> partner.id == personId2 } &&
                        (partnership.startDate.isBefore(startDate) || partnership.startDate.isEqual(startDate)) &&
                        (partnership.endDate == null || partnership.endDate.isAfter(startDate))
                }
        if (alreadyExists) {
            logger.debug("Similar partnership already exists between $personId1 and $personId2")
        } else {
            try {
                jdbi.transaction {
                    it.createPartnership(
                        personId1 = personId1,
                        personId2 = personId2,
                        startDate = startDate,
                        endDate = null,
                        conflict = false
                    )
                }
            } catch (e: UnableToExecuteStatementException) {
                jdbi.transaction {
                    it.createPartnership(
                        personId1 = personId1,
                        personId2 = personId2,
                        startDate = startDate,
                        endDate = null,
                        conflict = true
                    )
                }
            }
        }
    }
}

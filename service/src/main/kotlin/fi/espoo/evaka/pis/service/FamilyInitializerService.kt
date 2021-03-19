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
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.psqlCause
import mu.KotlinLogging
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.postgresql.util.PSQLState
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class FamilyInitializerService(
    private val personService: PersonService,
    asyncJobRunner: AsyncJobRunner
) {
    private val logger = KotlinLogging.logger {}

    init {
        asyncJobRunner.initializeFamilyFromApplication = ::handleInitializeFamilyFromApplication
    }

    fun handleInitializeFamilyFromApplication(db: Database, msg: InitializeFamilyFromApplication) =
        db.connect { handleInitializeFamilyFromApplication(it, msg) }

    fun handleInitializeFamilyFromApplication(db: Database.Connection, msg: InitializeFamilyFromApplication) {
        val user = msg.user
        val application = db.read { fetchApplicationDetails(it.handle, msg.applicationId) }
        if (application != null) {
            val members = db.transaction { parseFridgeFamilyMembersFromApplication(it, user, application) }
            db.transaction { tryInitFamilyFromApplication(it, members) }
        } else {
            logger.warn("Could not initialize family, daycare application ${msg.applicationId} not found")
        }
    }

    private fun tryInitFamilyFromApplication(tx: Database.Transaction, members: FridgeFamilyMembers) {
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
                tx.subTransaction {
                    createParentship(
                        tx,
                        childId = members.fridgeChildId,
                        headOfChildId = members.headOfFamilyId
                    )
                }
            } catch (e: Throwable) {
                logger.warn("Adding ${members.fridgeChildId} as the main fridge child to ${members.headOfFamilyId} failed.")
            }

            if (members.fridgePartnerId != null) {
                try {
                    tx.subTransaction {
                        createPartnership(tx, members.headOfFamilyId, members.fridgePartnerId)
                    }
                } catch (e: Throwable) {
                    logger.warn("Adding fridge partner ${members.fridgePartnerId} to ${members.headOfFamilyId} failed. Continuing with the rest of the family...")
                }
            }

            members.fridgeSiblingIds.forEach { siblingId ->
                try {
                    tx.subTransaction {
                        createParentship(tx, childId = siblingId, headOfChildId = members.headOfFamilyId)
                    }
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
        tx: Database.Transaction,
        user: AuthenticatedUser,
        application: ApplicationDetails
    ): FridgeFamilyMembers {
        val updateStale = false

        val headOfFamilyId = application.guardianId

        val otherGuardianId = application.otherGuardianId
        val fridgePartnerSSN = if (
            otherGuardianId != null &&
            personService.personsLiveInTheSameAddress(tx, headOfFamilyId, otherGuardianId)
        ) {
            (tx.handle.getPersonById(otherGuardianId)?.identity as? SSN)?.ssn
        } else {
            application.form.otherPartner?.socialSecurityNumber
        }

        val fridgePartnerId = fridgePartnerSSN
            ?.let { stringToSSN(it) }
            ?.let { personService.getOrCreatePerson(tx, user, it, updateStale) }
            ?.id

        val fridgeChildId = application.childId

        val fridgeSiblingIds = application.form.otherChildren
            .mapNotNull { it.socialSecurityNumber }
            .mapNotNull { stringToSSN(it) }
            .mapNotNull { personService.getOrCreatePerson(tx, user, it, updateStale)?.id }

        return FridgeFamilyMembers(headOfFamilyId, fridgePartnerId, fridgeChildId, fridgeSiblingIds)
    }

    private fun stringToSSN(ssn: String): SSN? {
        return try {
            SSN.getInstance(ssn)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun createParentship(tx: Database.Transaction, childId: UUID, headOfChildId: UUID) {
        val startDate = LocalDate.now()
        val alreadyExists = tx.handle.getParentships(
            headOfChildId = headOfChildId,
            childId = childId,
            includeConflicts = true
        )
            .any {
                (it.startDate.isBefore(startDate) || it.startDate.isEqual(startDate)) &&
                    (it.endDate.isAfter(startDate))
            }
        if (alreadyExists) {
            logger.debug("Similar parentship already exists between $headOfChildId and $childId")
        } else {
            val child = tx.handle.getPersonById(childId)
                ?: error("Couldn't find child ($childId) to create parentship")
            val endDate = child.dateOfBirth.plusYears(18).minusDays(1)
            if (startDate > endDate) {
                logger.debug("Skipped adding a child that is at least 18 years old to a family")
                return
            }
            try {
                tx.subTransaction {
                    tx.handle.createParentship(
                        childId = childId,
                        headOfChildId = headOfChildId,
                        startDate = startDate,
                        endDate = endDate,
                        conflict = false
                    )
                }
            } catch (e: UnableToExecuteStatementException) {
                when (e.psqlCause()?.sqlState) {
                    PSQLState.UNIQUE_VIOLATION.state, PSQLState.EXCLUSION_VIOLATION.state -> {
                        val constraint = e.psqlCause()?.serverErrorMessage?.constraint ?: "-"
                        logger.warn("Creating conflict parentship between $headOfChildId and $childId (conflicting constraint is $constraint)")
                        tx.handle.createParentship(
                            childId = childId,
                            headOfChildId = headOfChildId,
                            startDate = startDate,
                            endDate = endDate,
                            conflict = true
                        )
                    }
                    else -> throw e
                }
            }
        }
    }

    private fun createPartnership(tx: Database.Transaction, personId1: UUID, personId2: UUID) {
        val startDate = LocalDate.now()
        val alreadyExists =
            tx.handle.getPartnershipsForPerson(personId = personId1, includeConflicts = true)
                .any { partnership ->
                    partnership.partners.any { partner -> partner.id == personId2 } &&
                        (partnership.startDate.isBefore(startDate) || partnership.startDate.isEqual(startDate)) &&
                        (partnership.endDate == null || partnership.endDate.isAfter(startDate))
                }
        if (alreadyExists) {
            logger.debug("Similar partnership already exists between $personId1 and $personId2")
        } else {
            try {
                tx.subTransaction {
                    tx.handle.createPartnership(
                        personId1 = personId1,
                        personId2 = personId2,
                        startDate = startDate,
                        endDate = null,
                        conflict = false
                    )
                }
            } catch (e: UnableToExecuteStatementException) {
                when (e.psqlCause()?.sqlState) {
                    PSQLState.UNIQUE_VIOLATION.state, PSQLState.EXCLUSION_VIOLATION.state -> {
                        val constraint = e.psqlCause()?.serverErrorMessage?.constraint ?: "-"
                        logger.warn("Creating conflict partnership between $personId1 and $personId2 (conflicting constraint is $constraint)")
                        tx.handle.createPartnership(
                            personId1 = personId1,
                            personId2 = personId2,
                            startDate = startDate,
                            endDate = null,
                            conflict = true
                        )
                    }
                    else -> throw e
                }
            }
        }
    }
}

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
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.psqlCause
import fi.espoo.evaka.shared.domain.EvakaClock
import mu.KotlinLogging
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.postgresql.util.PSQLState
import org.springframework.stereotype.Service

@Service
class FamilyInitializerService(
    private val personService: PersonService,
    asyncJobRunner: AsyncJobRunner<AsyncJob>
) {
    private val logger = KotlinLogging.logger {}

    init {
        asyncJobRunner.registerHandler(::handleInitializeFamilyFromApplication)
    }

    fun handleInitializeFamilyFromApplication(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.InitializeFamilyFromApplication
    ) {
        val user = msg.user
        val application =
            db.read { it.fetchApplicationDetails(msg.applicationId) }
                ?: error("Could not initialize family, application ${msg.applicationId} not found")

        val members =
            db.transaction { parseFridgeFamilyMembersFromApplication(it, user, application) }
        db.transaction { initFamilyFromApplication(it, clock, members) }
    }

    private fun initFamilyFromApplication(
        tx: Database.Transaction,
        evakaClock: EvakaClock,
        members: FridgeFamilyMembers
    ) {
        tx.subTransaction {
            createParentship(
                tx,
                evakaClock,
                child = members.fridgeChild,
                headOfChildId = members.headOfFamily.id
            )
        }

        if (members.fridgePartner != null) {
            tx.subTransaction {
                createPartnership(tx, evakaClock, members.headOfFamily.id, members.fridgePartner.id)
            }
        }

        members.fridgeSiblings.forEach { sibling ->
            tx.subTransaction {
                createParentship(
                    tx,
                    evakaClock,
                    child = sibling,
                    headOfChildId = members.headOfFamily.id
                )
            }
        }
    }

    private data class FridgeFamilyMembers(
        val headOfFamily: PersonDTO,
        val fridgePartner: PersonDTO?,
        val fridgeChild: PersonDTO,
        val fridgeSiblings: List<PersonDTO>
    )

    private fun parseFridgeFamilyMembersFromApplication(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        application: ApplicationDetails
    ): FridgeFamilyMembers {
        val headOfFamily =
            tx.getPersonById(application.guardianId)
                ?: error("Application guardian not found with id ${application.guardianId}")
        val child =
            tx.getPersonById(application.childId)
                ?: error("Application child not found with id ${application.childId}")

        val otherGuardian =
            personService
                .getGuardians(tx, user, application.childId)
                .firstOrNull { it.id != application.guardianId }
                ?.takeIf { otherGuardian ->
                    personService.personsLiveInTheSameAddress(headOfFamily, otherGuardian)
                }

        val fridgePartnerSSN =
            application.form.otherPartner?.socialSecurityNumber
                ?: (otherGuardian?.identity as? SSN)?.ssn

        val fridgePartner =
            fridgePartnerSSN
                ?.let { stringToSSN(it) }
                ?.let { personService.getOrCreatePerson(tx, user, it) }

        val fridgeSiblings =
            application.form.otherChildren
                .mapNotNull { it.socialSecurityNumber }
                .mapNotNull { stringToSSN(it) }
                .mapNotNull { personService.getOrCreatePerson(tx, user, it) }

        return FridgeFamilyMembers(headOfFamily, fridgePartner, child, fridgeSiblings)
    }

    private fun stringToSSN(ssn: String): SSN? {
        return try {
            SSN.getInstance(ssn)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun createParentship(
        tx: Database.Transaction,
        evakaClock: EvakaClock,
        child: PersonDTO,
        headOfChildId: PersonId
    ) {
        val startDate = evakaClock.today()
        val alreadyExists =
            tx.getParentships(
                    headOfChildId = headOfChildId,
                    childId = child.id,
                    includeConflicts = true
                )
                .any {
                    (it.startDate.isBefore(startDate) || it.startDate.isEqual(startDate)) &&
                        (it.endDate.isAfter(startDate))
                }
        if (alreadyExists) {
            logger.debug("Similar parentship already exists between $headOfChildId and ${child.id}")
        } else {
            val endDate = child.dateOfBirth.plusYears(18).minusDays(1)
            if (startDate > endDate) {
                logger.debug("Skipped adding a child that is at least 18 years old to a family")
                return
            }
            try {
                tx.subTransaction {
                    tx.createParentship(
                        childId = child.id,
                        headOfChildId = headOfChildId,
                        startDate = startDate,
                        endDate = endDate,
                        conflict = false
                    )
                }
            } catch (e: UnableToExecuteStatementException) {
                when (e.psqlCause()?.sqlState) {
                    PSQLState.UNIQUE_VIOLATION.state,
                    PSQLState.EXCLUSION_VIOLATION.state -> {
                        val constraint = e.psqlCause()?.serverErrorMessage?.constraint ?: "-"
                        logger.warn(
                            "Creating conflict parentship between $headOfChildId and ${child.id} (conflicting constraint is $constraint)"
                        )
                        tx.createParentship(
                            childId = child.id,
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

    private fun createPartnership(
        tx: Database.Transaction,
        evakaClock: EvakaClock,
        personId1: PersonId,
        personId2: PersonId
    ) {
        val startDate = evakaClock.today()
        val alreadyExists =
            tx.getPartnershipsForPerson(personId = personId1, includeConflicts = true).any {
                partnership ->
                partnership.partners.any { partner -> partner.id == personId2 } &&
                    (partnership.startDate.isBefore(startDate) ||
                        partnership.startDate.isEqual(startDate)) &&
                    (partnership.endDate == null || partnership.endDate.isAfter(startDate))
            }
        if (alreadyExists) {
            logger.debug("Similar partnership already exists between $personId1 and $personId2")
        } else {
            try {
                tx.subTransaction {
                    tx.createPartnership(
                        personId1 = personId1,
                        personId2 = personId2,
                        startDate = startDate,
                        endDate = null,
                        conflict = false
                    )
                }
            } catch (e: UnableToExecuteStatementException) {
                when (e.psqlCause()?.sqlState) {
                    PSQLState.UNIQUE_VIOLATION.state,
                    PSQLState.EXCLUSION_VIOLATION.state -> {
                        val constraint = e.psqlCause()?.serverErrorMessage?.constraint ?: "-"
                        logger.warn(
                            "Creating conflict partnership between $personId1 and $personId2 (conflicting constraint is $constraint)"
                        )
                        tx.createPartnership(
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

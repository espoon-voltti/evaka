// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.pis.service

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.identity.ExternalIdentifier.SSN
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.InitializeFamilyFromApplication
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.db.withSpringTx
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@Service
class FamilyInitializerService(
    private val personService: PersonService,
    private val parentshipService: ParentshipService,
    private val partnershipService: PartnershipService,
    private val txm: PlatformTransactionManager,
    private val dataSource: DataSource,
    private val asyncJobRunner: AsyncJobRunner,
    private val jdbc: NamedParameterJdbcTemplate,
    private val jackson: ObjectMapper
) {
    private val logger = KotlinLogging.logger {}

    init {
        asyncJobRunner.initializeFamilyFromApplication = ::handleInitializeFamilyFromApplication
    }

    @Transactional
    fun handleInitializeFamilyFromApplication(h: Handle, msg: InitializeFamilyFromApplication) {
        val user = msg.user
        val daycareForm = getForm(msg.applicationId)
        if (daycareForm != null) {
            tryInitFamilyFromApplication(h, user, daycareForm, msg.applicationId, getGuardianId(msg.applicationId))
        } else {
            logger.warn("Could not initialize family, daycare application ${msg.applicationId} not found")
        }
    }

    private fun getForm(applicationId: UUID): DaycareFormV0? {
        // language=sql
        val sql =
            """
            SELECT document FROM application_view
            WHERE id = :id AND type != 'club'
            """.trimIndent()
        return jdbc
            .query(sql, mapOf("id" to applicationId)) { rs, _ -> rs.getString("document") }
            .firstOrNull()
            ?.let { jackson.readValue(it, DaycareFormV0::class.java) }
    }

    private fun getGuardianId(applicationId: UUID): UUID {
        // language=sql
        val sql =
            """
            SELECT guardian_id
            FROM application
            WHERE id = :id
            """.trimIndent()
        return jdbc
            .query(sql, mapOf("id" to applicationId)) { rs, _ -> rs.getString("guardian_id") }
            .mapNotNull { guardianId -> UUID.fromString(guardianId) }
            .first()
    }

    fun tryInitFamilyFromApplication(
        h: Handle,
        user: AuthenticatedUser,
        form: DaycareFormV0,
        childId: UUID,
        guardianId: UUID
    ) {
        try {
            val members = parseFridgeFamilyMembersFromApplication(user, form, guardianId, childId)
            tryInitFamilyFromApplication(h, members)
        } catch (e: Throwable) {
            logger.warn("Unexpected error when initializing family from application", e)
        }
    }

    @Transactional
    fun tryInitFamilyFromApplication(user: AuthenticatedUser, application: ApplicationDetails) {
        try {
            val members = parseFridgeFamilyMembersFromApplication(user, application)
            withSpringTx(txm) {
                withSpringHandle(dataSource) { h ->
                    tryInitFamilyFromApplication(h, members)
                }
            }
        } catch (e: Throwable) {
            logger.warn("Unexpected error when initializing family from application", e)
        }
    }

    private fun tryInitFamilyFromApplication(h: Handle, members: FridgeFamilyMembers) {
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
                createParentship(h, childId = members.fridgeChildId, headOfChildId = members.headOfFamilyId)
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
                    createParentship(h, childId = siblingId, headOfChildId = members.headOfFamilyId)
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
        form: DaycareFormV0,
        guardianId: UUID,
        childId: UUID
    ): FridgeFamilyMembers {
        return withSpringTx(txm, requiresNew = true) {
            val updateStale = false

            val headOfFamilyId = stringToSSN(form.guardian.socialSecurityNumber)
                ?.let { personService.getOrCreatePerson(user, it, updateStale) }
                ?.id

            val otherGuardianId = personService.getOtherGuardian(user, guardianId, childId)?.let { it.id }

            val fridgePartnerSSN = if (otherGuardianId != null && personService.personsLiveInTheSameAddress(
                user,
                guardianId,
                otherGuardianId
            )
            ) {
                form.guardian2?.socialSecurityNumber
            } else {
                form.otherAdults.firstOrNull()?.socialSecurityNumber
            }
            val fridgePartnerId = fridgePartnerSSN
                ?.let { stringToSSN(it) }
                ?.let { personService.getOrCreatePerson(user, it, updateStale) }
                ?.id

            val fridgeChildId = form.child.socialSecurityNumber
                .let { stringToSSN(it) }
                ?.let { personService.getOrCreatePerson(user, it, updateStale) }
                ?.id

            val fridgeSiblingIds = form.otherChildren
                .mapNotNull { stringToSSN(it.socialSecurityNumber) }
                .mapNotNull { personService.getOrCreatePerson(user, it, updateStale)?.id }

            FridgeFamilyMembers(headOfFamilyId, fridgePartnerId, fridgeChildId, fridgeSiblingIds)
        }
    }

    private fun parseFridgeFamilyMembersFromApplication(
        user: AuthenticatedUser,
        application: ApplicationDetails
    ): FridgeFamilyMembers {
        return withSpringTx(txm, requiresNew = true) {
            val updateStale = false

            val headOfFamilyId = application.guardianId

            val otherGuardianId = application.otherGuardianId
            val fridgePartnerSSN = if (
                otherGuardianId != null &&
                personService.personsLiveInTheSameAddress(user, headOfFamilyId, otherGuardianId)
            ) {
                (personService.getPerson(otherGuardianId)?.identity as? SSN)?.ssn
            } else {
                application.form.otherPartner?.socialSecurityNumber
            }

            val fridgePartnerId = fridgePartnerSSN
                ?.let { stringToSSN(it) }
                ?.let { personService.getOrCreatePerson(user, it, updateStale) }
                ?.id

            val fridgeChildId = application.childId

            val fridgeSiblingIds = application.form.otherChildren
                .mapNotNull { it.socialSecurityNumber }
                .mapNotNull { stringToSSN(it) }
                .mapNotNull { personService.getOrCreatePerson(user, it, updateStale)?.id }

            FridgeFamilyMembers(headOfFamilyId, fridgePartnerId, fridgeChildId, fridgeSiblingIds)
        }
    }

    private fun stringToSSN(ssn: String): SSN? {
        return try {
            SSN.getInstance(ssn)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun createParentship(t: Handle, childId: UUID, headOfChildId: UUID) {
        t.transaction { h ->
            val startDate = LocalDate.now()
            val alreadyExists = parentshipService.getParentships(
                h,
                headOfChildId = headOfChildId,
                childId = childId,
                includeConflicts = true
            ).any {
                (it.startDate.isBefore(startDate) || it.startDate.isEqual(startDate)) &&
                    (it.endDate == null || it.endDate.isAfter(startDate))
            }
            if (alreadyExists) {
                logger.debug("Similar parentship already exists between $headOfChildId and $childId")
            } else {
                parentshipService.createParentship(
                    h,
                    childId = childId,
                    headOfChildId = headOfChildId,
                    startDate = startDate,
                    endDate = null,
                    allowConflicts = true
                )
            }
        }
    }

    private fun createPartnership(personId1: UUID, personId2: UUID) {
        withSpringTx(txm, requiresNew = true) {
            val startDate = LocalDate.now()
            val alreadyExists =
                partnershipService.getPartnershipsForPerson(personId = personId1, includeConflicts = true)
                    .any { partnership ->
                        partnership.partners.any { partner -> partner.id == personId2 } &&
                            (partnership.startDate.isBefore(startDate) || partnership.startDate.isEqual(startDate)) &&
                            (partnership.endDate == null || partnership.endDate.isAfter(startDate))
                    }
            if (alreadyExists) {
                logger.debug("Similar partnership already exists between $personId1 and $personId2")
            } else {
                partnershipService.createPartnership(
                    personId1 = personId1,
                    personId2 = personId2,
                    startDate = startDate,
                    endDate = null,
                    allowConflicts = true
                )
            }
        }
    }
}

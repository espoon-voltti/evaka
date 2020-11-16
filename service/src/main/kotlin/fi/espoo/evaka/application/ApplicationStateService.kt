// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import com.fasterxml.jackson.databind.ObjectMapper
import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.ApplicationStatus.ACTIVE
import fi.espoo.evaka.application.ApplicationStatus.CANCELLED
import fi.espoo.evaka.application.ApplicationStatus.CREATED
import fi.espoo.evaka.application.ApplicationStatus.REJECTED
import fi.espoo.evaka.application.ApplicationStatus.SENT
import fi.espoo.evaka.application.ApplicationStatus.WAITING_CONFIRMATION
import fi.espoo.evaka.application.ApplicationStatus.WAITING_DECISION
import fi.espoo.evaka.application.ApplicationStatus.WAITING_MAILING
import fi.espoo.evaka.application.ApplicationStatus.WAITING_PLACEMENT
import fi.espoo.evaka.application.ApplicationStatus.WAITING_UNIT_CONFIRMATION
import fi.espoo.evaka.application.persistence.DatabaseForm
import fi.espoo.evaka.daycare.controllers.AdditionalInformation
import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getDaycareApplyFlags
import fi.espoo.evaka.daycare.upsertChild
import fi.espoo.evaka.decision.DecisionDraftService
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.fetchDecisionDrafts
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.decision.markDecisionAccepted
import fi.espoo.evaka.decision.markDecisionRejected
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.controller.validateIncome
import fi.espoo.evaka.invoicing.data.getIncomesForPerson
import fi.espoo.evaka.invoicing.data.splitEarlierIncome
import fi.espoo.evaka.invoicing.data.upsertIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.updatePersonBasicContactInfo
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementPlanRejectReason
import fi.espoo.evaka.placement.PlacementPlanService
import fi.espoo.evaka.placement.deletePlacementPlan
import fi.espoo.evaka.placement.getPlacementPlan
import fi.espoo.evaka.placement.updatePlacementPlanUnitConfirmation
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.InitializeFamilyFromApplication
import fi.espoo.evaka.shared.async.NotifyIncomeUpdated
import fi.espoo.evaka.shared.async.SendApplicationEmail
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.Period
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger { }

@Service
class ApplicationStateService(
    private val acl: AccessControlList,
    private val placementPlanService: PlacementPlanService,
    private val decisionService: DecisionService,
    private val decisionDraftService: DecisionDraftService,
    private val personService: PersonService,
    private val asyncJobRunner: AsyncJobRunner,
    private val mapper: ObjectMapper
) {
    // STATE TRANSITIONS

    fun sendApplication(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID, isEnduser: Boolean = false) {
        Audit.ApplicationSend.log(targetId = applicationId)

        val application = getApplication(tx.handle, applicationId)
        if (isEnduser) {
            if (application.guardianId != user.id) {
                throw Forbidden("User does not own this application")
            }
        } else {
            user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)
        }

        verifyStatus(application, CREATED)
        validateApplication(tx.handle, application)

        val applicationFlags = applicationFlags(tx.handle, application)
        updateApplicationFlags(tx.handle, application.id, applicationFlags)

        val sentDate = application.sentDate ?: LocalDate.now()
        val dueDate = calculateDueDate(
            application.type,
            sentDate,
            application.form.preferences.urgent,
            applicationFlags.isTransferApplication
        )
        updateApplicationDates(tx.handle, application.id, sentDate, dueDate)
        tx.handle.updatePersonBasicContactInfo(
            id = application.guardianId,
            email = application.form.guardian.email,
            phone = application.form.guardian.phoneNumber
        )

        if (!application.hideFromGuardian && application.type == ApplicationType.DAYCARE) {
            val preferredUnit =
                tx.handle.getDaycare(application.form.preferences.preferredUnits.first().id)!! // should never be null after validation

            if (preferredUnit.providerType != ProviderType.PRIVATE_SERVICE_VOUCHER) {
                asyncJobRunner.plan(tx, listOf(SendApplicationEmail(application.guardianId, preferredUnit.language)))
            }
        }

        updateApplicationStatus(tx.handle, application.id, SENT)
    }

    fun moveToWaitingPlacement(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationVerify.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, SENT)

        tx.handle.updatePersonBasicContactInfo(
            id = application.guardianId,
            email = application.form.guardian.email,
            phone = application.form.guardian.phoneNumber
        )
        tx.handle.upsertChild(
            Child(
                id = application.childId,
                additionalInformation = AdditionalInformation(
                    allergies = application.form.child.allergies,
                    diet = application.form.child.diet
                )
            )
        )

        setCheckedByAdminToDefault(tx.handle, applicationId, application.form)
        if (application.form.maxFeeAccepted) setHighestFeeForUser(tx, application)

        asyncJobRunner.plan(tx, listOf(InitializeFamilyFromApplication(application.id, user)))
        updateApplicationStatus(tx.handle, application.id, WAITING_PLACEMENT)
    }

    fun returnToSent(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationReturnToSent.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, WAITING_PLACEMENT)
        updateApplicationStatus(tx.handle, application.id, SENT)
    }

    fun cancelApplication(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationCancel.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, setOf(SENT, WAITING_PLACEMENT))
        updateApplicationStatus(tx.handle, application.id, CANCELLED)
    }

    fun setVerified(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationAdminDetailsUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, WAITING_PLACEMENT)
        setApplicationVerified(tx.handle, applicationId, true)
    }

    fun setUnverified(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationAdminDetailsUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, WAITING_PLACEMENT)
        setApplicationVerified(tx.handle, applicationId, false)
    }

    fun createPlacementPlan(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID, placementPlan: DaycarePlacementPlan) {
        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, WAITING_PLACEMENT)

        val guardian = personService.getUpToDatePerson(tx, user, application.guardianId)
            ?: throw NotFound("Guardian not found")
        val secondDecisionTo = personService
            .getGuardians(tx, user, application.childId)
            .firstOrNull {
                it.id != guardian.id && !livesInSameAddress(guardian.residenceCode, it.residenceCode)
            }?.id

        updateApplicationOtherGuardian(tx.handle, applicationId, secondDecisionTo)
        placementPlanService.createPlacementPlan(tx, application, placementPlan)
        decisionDraftService.createDecisionDrafts(tx.handle, user, application)

        updateApplicationStatus(tx.handle, application.id, WAITING_DECISION)
    }

    fun cancelPlacementPlan(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationReturnToWaitingPlacement.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, WAITING_DECISION)
        deletePlacementPlan(tx.handle, application.id)
        decisionDraftService.clearDecisionDrafts(tx.handle, application.id)
        updateApplicationStatus(tx.handle, application.id, WAITING_PLACEMENT)
    }

    fun confirmPlacementWithoutDecision(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.PlacementCreate.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, setOf(WAITING_DECISION, WAITING_UNIT_CONFIRMATION))
        val plan = getPlacementPlan(tx.handle, applicationId)
            ?: throw IllegalStateException("Application $applicationId has no placement plan")
        placementPlanService.applyPlacementPlan(
            tx,
            application.childId,
            plan,
            allowPreschool = true,
            allowPreschoolDaycare = true
        )
        decisionDraftService.clearDecisionDrafts(tx.handle, application.id)
        placementPlanService.softDeleteUnusedPlacementPlanByApplication(tx, applicationId)
        updateApplicationStatus(tx.handle, application.id, ACTIVE)
    }

    fun sendDecisionsWithoutProposal(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.DecisionCreate.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, WAITING_DECISION)
        finalizeDecisions(tx, user, application)
    }

    fun sendPlacementProposal(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.PlacementProposalCreate.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, WAITING_DECISION)
        updateApplicationStatus(tx.handle, application.id, WAITING_UNIT_CONFIRMATION)
    }

    fun withdrawPlacementProposal(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationReturnToWaitingDecision.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, WAITING_UNIT_CONFIRMATION)
        updateApplicationStatus(tx.handle, application.id, WAITING_DECISION)
    }

    fun respondToPlacementProposal(
        h: Handle,
        user: AuthenticatedUser,
        applicationId: UUID,
        status: PlacementPlanConfirmationStatus,
        rejectReason: PlacementPlanRejectReason? = null,
        rejectOtherReason: String? = null
    ) {
        Audit.PlacementPlanRespond.log(targetId = applicationId)
        acl.getRolesForApplication(user, applicationId).requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)

        val application = getApplication(h, applicationId)
        verifyStatus(application, WAITING_UNIT_CONFIRMATION)

        if (status == PlacementPlanConfirmationStatus.REJECTED) {
            if (rejectReason == null)
                throw BadRequest("Must give reason for rejecting")
            if (rejectReason == PlacementPlanRejectReason.OTHER && rejectOtherReason.isNullOrBlank())
                throw BadRequest("Must describe other reason for rejecting")

            updatePlacementPlanUnitConfirmation(h, applicationId, status, rejectReason, rejectOtherReason)
        } else {
            updatePlacementPlanUnitConfirmation(h, applicationId, status, null, null)
        }
    }

    fun acceptPlacementProposal(tx: Database.Transaction, user: AuthenticatedUser, unitId: UUID) {
        Audit.PlacementProposalAccept.log(targetId = unitId)
        if (!acl.getAuthorizedUnits(user).isAuthorized(unitId))
            throw Forbidden("Not authorized to accept placement proposal for unit $unitId")

        // language=sql
        val sql =
            """
            SELECT application_id, unit_confirmation_status
            FROM placement_plan pp
            JOIN application a ON a.id = pp.application_id
            WHERE a.status = 'WAITING_UNIT_CONFIRMATION'::application_status_type AND pp.unit_id = :unitId
            """.trimIndent()
        val applicationStates = tx.createQuery(sql)
            .bind("unitId", unitId)
            .map { row ->
                Pair<UUID, PlacementPlanConfirmationStatus>(
                    row.mapColumn("application_id"),
                    row.mapColumn("unit_confirmation_status")
                )
            }
            .toList()

        if (applicationStates.any { it.second != PlacementPlanConfirmationStatus.ACCEPTED })
            throw BadRequest("Must accept all children")

        applicationStates
            .map { getApplication(tx.handle, it.first) }
            .forEach { finalizeDecisions(tx, user, it) }
    }

    fun confirmDecisionMailed(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.DecisionConfirmMailed.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(tx.handle, applicationId)
        verifyStatus(application, WAITING_MAILING)
        updateApplicationStatus(tx.handle, application.id, WAITING_CONFIRMATION)
    }

    fun acceptDecision(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID, decisionId: UUID, requestedStartDate: LocalDate, isEnduser: Boolean = false) {
        Audit.DecisionAccept.log(targetId = decisionId)
        val application = getApplication(tx.handle, applicationId)
        if (isEnduser) {
            if (application.guardianId != user.id) {
                throw Forbidden("User does not own this application")
            }
        } else {
            acl.getRolesForApplication(user, applicationId).requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER, Roles.UNIT_SUPERVISOR)
        }

        verifyStatus(application, setOf(WAITING_CONFIRMATION, ACTIVE))

        val decisions = getDecisionsByApplication(tx.handle, applicationId, AclAuthorization.All)

        val decision = decisions.find { it.id == decisionId }
            ?: throw NotFound("Decision $decisionId not found on application $applicationId")

        if (decision.status != DecisionStatus.PENDING) {
            throw BadRequest("Decision is not pending")
        }

        if (decision.type == DecisionType.PRESCHOOL_DAYCARE && decisions.any { it.type in listOf(DecisionType.PRESCHOOL, DecisionType.PREPARATORY_EDUCATION) && it.status != DecisionStatus.ACCEPTED }) {
            throw BadRequest("Primary decision must be accepted first")
        }

        if (!decision.validRequestedStartDatePeriod().includes(requestedStartDate)) {
            throw BadRequest(
                "Invalid start date $requestedStartDate for ${decision.type} [${decision.startDate}, ${decision.endDate}]",
                "decision.validation.invalid-requested-start-date"
            )
        }

        val plan = getPlacementPlan(tx.handle, applicationId)
            ?: throw IllegalStateException("Application $applicationId has no placement plan")

        // everything validated now!

        markDecisionAccepted(tx.handle, user, decision.id, requestedStartDate)

        placementPlanService.applyPlacementPlan(
            tx,
            application.childId,
            plan,
            allowPreschool = decision.type in listOf(DecisionType.PRESCHOOL, DecisionType.PREPARATORY_EDUCATION),
            allowPreschoolDaycare = decision.type in listOf(DecisionType.PRESCHOOL_DAYCARE),
            requestedStartDate = requestedStartDate
        )

        placementPlanService.softDeleteUnusedPlacementPlanByApplication(tx, applicationId)

        if (application.status == WAITING_CONFIRMATION) {
            updateApplicationStatus(tx.handle, application.id, ACTIVE)
        }
    }

    fun rejectDecision(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID, decisionId: UUID, isEnduser: Boolean = false) {
        Audit.DecisionReject.log(targetId = decisionId)
        val application = getApplication(tx.handle, applicationId)
        if (isEnduser) {
            if (application.guardianId != user.id) {
                throw Forbidden("User does not own this application")
            }
        } else {
            acl.getRolesForApplication(user, applicationId).requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER, Roles.UNIT_SUPERVISOR)
        }

        verifyStatus(application, setOf(WAITING_CONFIRMATION, ACTIVE, REJECTED))

        val decisions = getDecisionsByApplication(tx.handle, applicationId, AclAuthorization.All)
        val decision = decisions.find { it.id == decisionId }
            ?: throw NotFound("Decision $decisionId not found on application $applicationId")

        if (decision.status != DecisionStatus.PENDING) {
            throw BadRequest("Decision is not pending")
        }

        markDecisionRejected(tx.handle, user, decisionId)

        val alsoReject = if (decision.type in listOf(DecisionType.PRESCHOOL, DecisionType.PREPARATORY_EDUCATION)) {
            decisions.find { it.type === DecisionType.PRESCHOOL_DAYCARE && it.status == DecisionStatus.PENDING }
        } else null
        alsoReject?.let { markDecisionRejected(tx.handle, user, it.id) }

        placementPlanService.softDeleteUnusedPlacementPlanByApplication(tx, applicationId)

        if (application.status == WAITING_CONFIRMATION) {
            updateApplicationStatus(tx.handle, application.id, REJECTED)
        }
    }

    // CONTENT UPDATE

    fun updateApplicationContents(h: Handle, user: AuthenticatedUser, applicationId: UUID, form: ApplicationForm) {
        Audit.ApplicationUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        val original = fetchApplicationDetails(h, applicationId)
            ?: throw NotFound("Application $applicationId was not found")

        updateApplicationContents(h, original, form)
    }

    fun updateOwnApplicationContents(h: Handle, user: AuthenticatedUser, applicationId: UUID, formV0: DatabaseForm): ApplicationDetails {
        val original = fetchApplicationDetails(h, applicationId)
            ?.takeIf { it.guardianId == user.id }
            ?: throw NotFound("Application $applicationId of guardian ${user.id} not found")

        val form = ApplicationForm.fromV0(formV0, original.childRestricted, original.guardianRestricted)

        updateApplicationContents(h, original, form)
        return getApplication(h, applicationId)
    }

    private fun updateApplicationContents(h: Handle, original: ApplicationDetails, form: ApplicationForm) {
        if (!listOf(CREATED, SENT).contains(original.status))
            throw BadRequest("Cannot update application with status ${original.status}")

        val updated = original.form
            .let { updateApplicationPreferences(it, form, original.type) }
            .let { updateApplicationAssistanceNeed(it, form) }
            .let { updateApplicationContactInfo(it, form) }
            .let { updateApplicationAdditionalInfo(it, form) }
            .let { updateApplicationMaxFeeAccepted(it, form) }
            .let { updateApplicationClubDetails(it, form) }

        updateForm(h, original.id, form, original.type, original.childRestricted, original.guardianRestricted)
        setCheckedByAdminToDefault(h, original.id, updated)
        updateDueDate(h, original, updated)
    }

    private fun updateApplicationPreferences(
        original: ApplicationForm,
        updated: ApplicationForm,
        type: ApplicationType
    ): ApplicationForm {
        return original.copy(
            preferences = original.preferences.copy(
                preferredStartDate = updated.preferences.preferredStartDate,
                urgent = if (type === ApplicationType.DAYCARE) updated.preferences.urgent else false,
                preferredUnits = updated.preferences.preferredUnits,
                serviceNeed = updated.preferences.serviceNeed,
                siblingBasis = updated.preferences.siblingBasis,
                preparatory = if (type === ApplicationType.PRESCHOOL) updated.preferences.preparatory else false
            )
        )
    }

    private fun updateApplicationAssistanceNeed(
        original: ApplicationForm,
        updated: ApplicationForm
    ): ApplicationForm {
        return original.copy(
            child = original.child.copy(
                assistanceNeeded = updated.child.assistanceNeeded,
                assistanceDescription = updated.child.assistanceDescription
            )
        )
    }

    private fun updateApplicationContactInfo(
        original: ApplicationForm,
        updated: ApplicationForm
    ): ApplicationForm {
        return original.copy(
            child = original.child.copy(futureAddress = updated.child.futureAddress),
            guardian = original.guardian.copy(
                phoneNumber = updated.guardian.phoneNumber,
                email = updated.guardian.email,
                futureAddress = updated.guardian.futureAddress
            ),
            secondGuardian = updated.secondGuardian,
            otherPartner = updated.otherPartner,
            otherChildren = updated.otherChildren
        )
    }

    private fun updateApplicationAdditionalInfo(
        original: ApplicationForm,
        updated: ApplicationForm
    ): ApplicationForm {
        return original.copy(
            otherInfo = updated.otherInfo,
            child = original.child.copy(allergies = updated.child.allergies, diet = updated.child.diet)
        )
    }

    private fun updateApplicationMaxFeeAccepted(
        original: ApplicationForm,
        updated: ApplicationForm
    ): ApplicationForm {
        return original.copy(
            maxFeeAccepted = updated.maxFeeAccepted
        )
    }

    private fun updateApplicationClubDetails(
        original: ApplicationForm,
        updated: ApplicationForm
    ): ApplicationForm {
        return original.copy(
            clubDetails = updated.clubDetails
        )
    }

    private fun updateDueDate(h: Handle, original: ApplicationDetails, updated: ApplicationForm) {
        if (original.sentDate == null || original.form.preferences.urgent == updated.preferences.urgent) return

        // If an application is flagged as urgent afterwards, the new due date is calculated from current date
        val sentDate = if (updated.preferences.urgent) LocalDate.now() else original.sentDate
        val newDueDate =
            calculateDueDate(original.type, sentDate, updated.preferences.urgent, original.transferApplication)

        h.createUpdate("UPDATE application SET duedate = :dueDate WHERE id = :id")
            .bind("id", original.id)
            .bind("dueDate", newDueDate)
            .execute()
    }

    // HELPERS

    private fun getApplication(h: Handle, applicationId: UUID): ApplicationDetails {
        return fetchApplicationDetails(h, applicationId)
            ?: throw NotFound("Application $applicationId not found")
    }

    private fun verifyStatus(application: ApplicationDetails, status: ApplicationStatus) {
        if (application.status != status)
            throw BadRequest("Expected status $status but was ${application.status}")
    }

    private fun verifyStatus(application: ApplicationDetails, statuses: Set<ApplicationStatus>) {
        if (!statuses.contains(application.status))
            throw BadRequest("Expected status to be one of [${statuses.joinToString(separator = ", ")}] but was ${application.status}")
    }

    private fun validateApplication(h: Handle, application: ApplicationDetails) {
        val result = ValidationResult()

        val unitIds = application.form.preferences.preferredUnits.map { it.id }
        if (unitIds.isEmpty()) {
            result.add(ValidationError("form.preferences.preferredUnits", "Must have at least one preferred unit"))
        } else {
            val daycares = h.getDaycareApplyFlags(unitIds.toSet())
            if (daycares.size < unitIds.toSet().size) {
                result.add(ValidationError("form.preferences.preferredUnits", "Some unit was not found"))
            }
            if (application.origin == ApplicationOrigin.ELECTRONIC) {
                for (daycare in daycares) {
                    if (application.type == ApplicationType.DAYCARE && !daycare.canApplyDaycare)
                        result.add(ValidationError("form.preferences.preferredUnits", "Cannot apply for daycare in ${daycare.id}"))
                    if (application.type == ApplicationType.PRESCHOOL && !daycare.canApplyPreschool)
                        result.add(ValidationError("form.preferences.preferredUnits", "Cannot apply for preschool in ${daycare.id}"))
                }
            }
        }

        if (!result.isValid()) throw ValidationException(result)
    }

    private fun calculateDueDate(
        applicationType: ApplicationType,
        sentDate: LocalDate,
        isUrgent: Boolean,
        isTransferApplication: Boolean
    ): LocalDate? {
        return if (isTransferApplication) {
            null
        } else if (applicationType == ApplicationType.PRESCHOOL) {
            sentDate // todo: is this correct? seems weird
        } else {
            if (isUrgent) {
                sentDate.plusWeeks(2)
            } else {
                sentDate.plusMonths(4)
            }
        }
    }

    private fun finalizeDecisions(tx: Database.Transaction, user: AuthenticatedUser, application: ApplicationDetails) {
        val sendBySfi = canSendDecisionsBySfi(tx, user, application)
        val decisionDrafts = fetchDecisionDrafts(tx.handle, application.id)
        if (decisionDrafts.any { it.planned }) {
            decisionService.finalizeDecisions(tx, user, application.id, sendBySfi)
            updateApplicationStatus(tx.handle, application.id, if (sendBySfi) WAITING_CONFIRMATION else WAITING_MAILING)
        } else {
            confirmPlacementWithoutDecision(tx, user, application.id)
        }
    }

    private fun canSendDecisionsBySfi(tx: Database.Transaction, user: AuthenticatedUser, application: ApplicationDetails): Boolean {
        val hasSsn = (
            personService.getUpToDatePerson(tx, user, application.guardianId)!!
                .identity is ExternalIdentifier.SSN && personService.getUpToDatePerson(tx, user, application.childId)!!
                .identity is ExternalIdentifier.SSN
            )
        val guardianIsVtjGuardian =
            personService.getGuardians(tx, user, application.childId).any { it.id == application.guardianId }

        return hasSsn && guardianIsVtjGuardian
    }

    private fun livesInSameAddress(residenceCode1: String?, residenceCode2: String?): Boolean =
        !residenceCode1.isNullOrBlank() && !residenceCode2.isNullOrBlank() && residenceCode1 == residenceCode2

    private fun setHighestFeeForUser(tx: Database.Transaction, application: ApplicationDetails) {
        val incomes = getIncomesForPerson(tx.handle, mapper, application.guardianId)
        val preferredStartDate = application.form.preferences.preferredStartDate
        val preferredOrNow = preferredStartDate ?: LocalDate.now()

        val hasOverlappingDefiniteIncome = incomes.any { income ->
            income.validTo != null &&
                Period(income.validFrom, income.validTo).overlaps(Period(preferredOrNow, null))
        }

        val hasLaterIncome = incomes.any { income ->
            income.validFrom.plusDays(1).isAfter(preferredOrNow)
        }

        if (hasOverlappingDefiniteIncome || hasLaterIncome || preferredStartDate == null) {
            logger.error { "Could not add a new max fee accepted income when moving to WAITING_FOR_PLACEMENT state" }
        } else {
            val period = Period(start = preferredOrNow, end = null)
            val validIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.MAX_FEE_ACCEPTED,
                notes = "created automatically from application",
                personId = application.guardianId,
                validFrom = preferredOrNow,
                validTo = null
            ).let(::validateIncome)
            splitEarlierIncome(tx.handle, validIncome.personId, period)
            upsertIncome(tx.handle, mapper, validIncome, application.guardianId)
            asyncJobRunner.plan(tx, listOf(NotifyIncomeUpdated(validIncome.personId, period.start, period.end)))
        }
    }
}

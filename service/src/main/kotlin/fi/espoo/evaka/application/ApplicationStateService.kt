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
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.service.AdditionalInformation
import fi.espoo.evaka.daycare.service.Child
import fi.espoo.evaka.daycare.service.DaycareService
import fi.espoo.evaka.daycare.upsertChild
import fi.espoo.evaka.decision.DecisionDraftService
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.controller.validateIncome
import fi.espoo.evaka.invoicing.data.getIncomesForPerson
import fi.espoo.evaka.invoicing.data.splitEarlierIncome
import fi.espoo.evaka.invoicing.data.upsertIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.pis.service.FamilyInitializerService
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.updatePersonBasicContactInfo
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementPlanRejectReason
import fi.espoo.evaka.placement.PlacementPlanService
import fi.espoo.evaka.placement.updatePlacementPlanUnitConfirmation
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.NotifyIncomeUpdated
import fi.espoo.evaka.shared.async.SendApplicationEmail
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.db.withSpringHandle
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.Period
import mu.KotlinLogging
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource
import kotlin.reflect.KFunction2

private val logger = KotlinLogging.logger { }

@Service
class ApplicationStateService(
    private val dataSource: DataSource,
    private val jdbi: Jdbi,
    private val acl: AccessControlList,
    private val daycareService: DaycareService,
    private val familyInitializerService: FamilyInitializerService,
    private val placementPlanService: PlacementPlanService,
    private val decisionService: DecisionService,
    private val decisionDraftService: DecisionDraftService,
    private val applicationDetailsService: ApplicationDetailsService,
    private val personService: PersonService,
    private val asyncJobRunner: AsyncJobRunner,
    private val mapper: ObjectMapper
) {
    @Transactional
    fun batchAction(user: AuthenticatedUser, applicationIds: Set<UUID>, action: KFunction2<AuthenticatedUser, UUID, Unit>) {
        applicationIds.forEach { action.invoke(user, it) }
    }

    // STATE TRANSITIONS

    @Transactional
    fun sendApplication(user: AuthenticatedUser, applicationId: UUID, isEnduser: Boolean = false) {
        Audit.ApplicationSend.log(targetId = applicationId)

        val application = getApplication(applicationId)
        if (isEnduser) {
            if (application.guardianId != user.id) {
                throw Forbidden("User does not own this application")
            }
        } else {
            user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)
        }

        verifyStatus(application, CREATED)
        validateApplication(application)

        val applicationFlags = applicationDetailsService.applicationFlags(application)
        withSpringHandle(dataSource) { h -> updateApplicationFlags(h, application.id, applicationFlags) }

        val sentDate = application.sentDate ?: LocalDate.now()
        val dueDate = calculateDueDate(
            application.type,
            sentDate,
            application.form.preferences.urgent,
            applicationFlags.isTransferApplication
        )
        withSpringHandle(dataSource) { h -> updateApplicationDates(h, application.id, sentDate, dueDate) }

        withSpringHandle(dataSource) { h ->
            h.updatePersonBasicContactInfo(
                id = application.guardianId,
                email = application.form.guardian.email,
                phone = application.form.guardian.phoneNumber
            )
        }

        if (!application.hideFromGuardian && application.type == ApplicationType.DAYCARE) {
            val preferredUnit = withSpringHandle(dataSource) { h ->
                h.getDaycare(application.form.preferences.preferredUnits.first().id)!! // should never be null after validation
            }

            if (preferredUnit.providerType != ProviderType.PRIVATE_SERVICE_VOUCHER) {
                withSpringHandle(dataSource) { h ->
                    asyncJobRunner.plan(h, listOf(SendApplicationEmail(application.guardianId, preferredUnit.language)))
                }
            }
        }

        updateStatus(application, SENT)
    }

    @Transactional
    fun moveToWaitingPlacement(user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationVerify.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, SENT)

        withSpringHandle(dataSource) { h ->
            h.updatePersonBasicContactInfo(
                id = application.guardianId,
                email = application.form.guardian.email,
                phone = application.form.guardian.phoneNumber
            )
        }

        withSpringHandle(dataSource) { h ->
            h.upsertChild(
                Child(
                    id = application.childId,
                    additionalInformation = AdditionalInformation(
                        allergies = application.form.child.allergies,
                        diet = application.form.child.diet
                    )
                )
            )
        }

        withSpringHandle(dataSource) { h -> setCheckedByAdminToDefault(h, applicationId, application.form) }

        familyInitializerService.tryInitFamilyFromApplication(user, application)

        if (application.form.maxFeeAccepted) withSpringHandle(dataSource) { h -> setHighestFeeForUser(h, application) }

        updateStatus(application, WAITING_PLACEMENT)
    }

    @Transactional
    fun returnToSent(user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationReturnToSent.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, WAITING_PLACEMENT)
        updateStatus(application, SENT)
    }

    @Transactional
    fun cancelApplication(user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationCancel.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, setOf(SENT, WAITING_PLACEMENT))
        updateStatus(application, CANCELLED)
    }

    @Transactional
    fun setVerified(user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationAdminDetailsUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, WAITING_PLACEMENT)
        withSpringHandle(dataSource) { h -> setApplicationVerified(h, applicationId, true) }
    }

    @Transactional
    fun setUnverified(user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationAdminDetailsUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, WAITING_PLACEMENT)
        withSpringHandle(dataSource) { h -> setApplicationVerified(h, applicationId, false) }
    }

    @Transactional
    fun createPlacementPlan(user: AuthenticatedUser, applicationId: UUID, placementPlan: DaycarePlacementPlan) {
        Audit.PlacementPlanCreate.log(targetId = applicationId, objectId = placementPlan.unitId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, WAITING_PLACEMENT)

        val guardian = personService.getUpToDatePerson(user, application.guardianId)
            ?: throw NotFound("Guardian not found")
        val secondDecisionTo = personService
            .getGuardians(user, application.childId)
            .firstOrNull {
                it.id != guardian.id && !livesInSameAddress(guardian.residenceCode, it.residenceCode)
            }?.id

        withSpringHandle(dataSource) { h ->
            updateApplicationOtherGuardian(h, applicationId, secondDecisionTo)
            placementPlanService.createPlacementPlan(h, application, placementPlan)
            decisionDraftService.createDecisionDrafts(h, user, application)
        }

        updateStatus(application, WAITING_DECISION)
    }

    @Transactional
    fun cancelPlacementPlan(user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationReturnToWaitingPlacement.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, WAITING_DECISION)
        withSpringHandle(dataSource) { h ->
            placementPlanService.deletePlacementPlanByApplication(h, application.id)
            decisionDraftService.clearDecisionDrafts(h, application.id)
        }
        updateStatus(application, WAITING_PLACEMENT)
    }

    @Transactional
    fun confirmPlacementWithoutDecision(user: AuthenticatedUser, applicationId: UUID) {
        Audit.PlacementCreate.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, setOf(WAITING_DECISION, WAITING_UNIT_CONFIRMATION))
        val plan = placementPlanService.getPlacementPlanByApplication(applicationId)
            ?: throw IllegalStateException("Application $applicationId has no placement plan")
        placementPlanService.applyPlacementPlan(
            application.childId,
            plan,
            allowPreschool = true,
            allowPreschoolDaycare = true
        )
        withSpringHandle(dataSource) { h ->
            decisionDraftService.clearDecisionDrafts(h, application.id)
            placementPlanService.softDeleteUnusedPlacementPlanByApplication(h, applicationId)
        }
        updateStatus(application, ACTIVE)
    }

    @Transactional
    fun sendDecisionsWithoutProposal(user: AuthenticatedUser, applicationId: UUID) {
        Audit.DecisionCreate.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, WAITING_DECISION)
        finalizeDecisions(user, application)
    }

    @Transactional
    fun sendPlacementProposal(user: AuthenticatedUser, applicationId: UUID) {
        Audit.PlacementProposalCreate.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, WAITING_DECISION)
        updateStatus(application, WAITING_UNIT_CONFIRMATION)
    }

    @Transactional
    fun withdrawPlacementProposal(user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationReturnToWaitingDecision.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, WAITING_UNIT_CONFIRMATION)
        updateStatus(application, WAITING_DECISION)
    }

    @Transactional
    fun respondToPlacementProposal(
        user: AuthenticatedUser,
        applicationId: UUID,
        status: PlacementPlanConfirmationStatus,
        rejectReason: PlacementPlanRejectReason? = null,
        rejectOtherReason: String? = null
    ) {
        Audit.PlacementPlanRespond.log(targetId = applicationId)
        acl.getRolesForApplication(user, applicationId).requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)

        val application = getApplication(applicationId)
        verifyStatus(application, WAITING_UNIT_CONFIRMATION)

        if (status == PlacementPlanConfirmationStatus.REJECTED) {
            if (rejectReason == null)
                throw BadRequest("Must give reason for rejecting")
            if (rejectReason == PlacementPlanRejectReason.OTHER && rejectOtherReason.isNullOrBlank())
                throw BadRequest("Must describe other reason for rejecting")

            withSpringHandle(dataSource) { h -> updatePlacementPlanUnitConfirmation(h, applicationId, status, rejectReason, rejectOtherReason) }
        } else {
            withSpringHandle(dataSource) { h -> updatePlacementPlanUnitConfirmation(h, applicationId, status, null, null) }
        }
    }

    @Transactional
    fun acceptPlacementProposal(user: AuthenticatedUser, unitId: UUID) {
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
        val applicationStates = withSpringHandle(dataSource) { h ->
            h.createQuery(sql).bind("unitId", unitId).map { row ->
                Pair<UUID, PlacementPlanConfirmationStatus>(
                    row.mapColumn("application_id"),
                    row.mapColumn("unit_confirmation_status")
                )
            }.list()
        }

        if (applicationStates.any { it.second != PlacementPlanConfirmationStatus.ACCEPTED })
            throw BadRequest("Must accept all children")

        applicationStates
            .map { getApplication(it.first) }
            .forEach { finalizeDecisions(user, it) }
    }

    @Transactional
    fun confirmDecisionMailed(user: AuthenticatedUser, applicationId: UUID) {
        Audit.DecisionConfirmMailed.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        val application = getApplication(applicationId)
        verifyStatus(application, WAITING_MAILING)
        updateStatus(application, WAITING_CONFIRMATION)
    }

    @Transactional
    fun acceptDecision(user: AuthenticatedUser, applicationId: UUID, decisionId: UUID, requestedStartDate: LocalDate, isEnduser: Boolean = false) {
        Audit.DecisionAccept.log(targetId = decisionId)
        val application = getApplication(applicationId)
        if (isEnduser) {
            if (application.guardianId != user.id) {
                throw Forbidden("User does not own this application")
            }
        } else {
            acl.getRolesForApplication(user, applicationId).requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER, Roles.UNIT_SUPERVISOR)
        }

        verifyStatus(application, setOf(WAITING_CONFIRMATION, ACTIVE))

        val decisions = decisionService.getDecisionsByApplication(applicationId)

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

        val plan = placementPlanService.getPlacementPlanByApplication(applicationId)
            ?: throw IllegalStateException("Application $applicationId has no placement plan")

        // everything validated now!

        decisionService.markDecisionAccepted(user, decision.id, requestedStartDate)

        placementPlanService.applyPlacementPlan(
            application.childId,
            plan,
            allowPreschool = decision.type in listOf(DecisionType.PRESCHOOL, DecisionType.PREPARATORY_EDUCATION),
            allowPreschoolDaycare = decision.type in listOf(DecisionType.PRESCHOOL_DAYCARE),
            requestedStartDate = requestedStartDate
        )

        withSpringHandle(dataSource) {
            placementPlanService.softDeleteUnusedPlacementPlanByApplication(it, applicationId)
        }

        if (application.status == WAITING_CONFIRMATION) {
            updateStatus(application, ACTIVE)
        }
    }

    @Transactional
    fun rejectDecision(user: AuthenticatedUser, applicationId: UUID, decisionId: UUID, isEnduser: Boolean = false) {
        Audit.DecisionReject.log(targetId = decisionId)
        val application = getApplication(applicationId)
        if (isEnduser) {
            if (application.guardianId != user.id) {
                throw Forbidden("User does not own this application")
            }
        } else {
            acl.getRolesForApplication(user, applicationId).requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER, Roles.UNIT_SUPERVISOR)
        }

        verifyStatus(application, setOf(WAITING_CONFIRMATION, ACTIVE, REJECTED))

        val decisions = decisionService.getDecisionsByApplication(applicationId)
        val decision = decisions.find { it.id == decisionId }
            ?: throw NotFound("Decision $decisionId not found on application $applicationId")

        if (decision.status != DecisionStatus.PENDING) {
            throw BadRequest("Decision is not pending")
        }

        decisionService.markDecisionRejected(user, decisionId)

        val alsoReject = if (decision.type in listOf(DecisionType.PRESCHOOL, DecisionType.PREPARATORY_EDUCATION)) {
            decisions.find { it.type === DecisionType.PRESCHOOL_DAYCARE && it.status == DecisionStatus.PENDING }
        } else null
        alsoReject?.let { decisionService.markDecisionRejected(user, it.id) }

        withSpringHandle(dataSource) {
            placementPlanService.softDeleteUnusedPlacementPlanByApplication(it, applicationId)
        }

        if (application.status == WAITING_CONFIRMATION) {
            updateStatus(application, REJECTED)
        }
    }

    // CONTENT UPDATE

    fun updateApplicationContents(user: AuthenticatedUser, applicationId: UUID, form: ApplicationForm) {
        Audit.ApplicationUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        jdbi.transaction { h ->
            val original = fetchApplicationDetails(h, applicationId)
                ?: throw NotFound("Application $applicationId was not found")

            updateApplicationContents(h, original, form)
        }
    }

    fun updateOwnApplicationContents(user: AuthenticatedUser, applicationId: UUID, form: ApplicationForm): ApplicationDetails {
        Audit.ApplicationUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.END_USER)

        return jdbi.transaction { h ->
            val original = fetchApplicationDetails(h, applicationId)
                ?.takeIf { it.guardianId == user.id }
                ?: throw NotFound("Application $applicationId of guardian ${user.id} not found")

            updateApplicationContents(h, original, form)
            getApplication(applicationId)
        }
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

    private fun getApplication(applicationId: UUID): ApplicationDetails {
        return withSpringHandle(dataSource) { h -> fetchApplicationDetails(h, applicationId) }
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

    private fun updateStatus(application: ApplicationDetails, newStatus: ApplicationStatus) {
        withSpringHandle(dataSource) { h -> updateApplicationStatus(h, application.id, newStatus) }
    }

    private fun validateApplication(application: ApplicationDetails) {
        val result = ValidationResult()

        val unitIds = application.form.preferences.preferredUnits.map { it.id }
        if (unitIds.isEmpty()) {
            result.add(ValidationError("form.preferences.preferredUnits", "Must have at least one preferred unit"))
        } else {
            val daycares = daycareService.getDaycareApplyFlags(unitIds.toSet())
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

    private fun finalizeDecisions(user: AuthenticatedUser, application: ApplicationDetails) {
        val sendBySfi = canSendDecisionsBySfi(user, application)

        val decisionDrafts = withSpringHandle(dataSource) {
            decisionDraftService.getDecisionDrafts(it, application.id)
        }
        if (decisionDrafts.any { it.planned }) {
            decisionService.finalizeDecisions(user, application.id, sendBySfi)
            updateStatus(application, if (sendBySfi) WAITING_CONFIRMATION else WAITING_MAILING)
        } else {
            confirmPlacementWithoutDecision(user, application.id)
        }
    }

    private fun canSendDecisionsBySfi(user: AuthenticatedUser, application: ApplicationDetails): Boolean {
        val hasSsn = (
            personService.getUpToDatePerson(user, application.guardianId)!!
                .identity is ExternalIdentifier.SSN && personService.getUpToDatePerson(user, application.childId)!!
                .identity is ExternalIdentifier.SSN
            )
        val guardianIsVtjGuardian = personService.getGuardians(user, application.childId).any { it.id == application.guardianId }

        return hasSsn && guardianIsVtjGuardian
    }

    private fun livesInSameAddress(residenceCode1: String?, residenceCode2: String?): Boolean =
        !residenceCode1.isNullOrBlank() && !residenceCode2.isNullOrBlank() && residenceCode1 == residenceCode2

    private fun setHighestFeeForUser(h: Handle, application: ApplicationDetails) {
        val incomes = getIncomesForPerson(h, mapper, application.guardianId)
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
            splitEarlierIncome(h, validIncome.personId, period)
            upsertIncome(h, mapper, validIncome, application.guardianId)
            asyncJobRunner.plan(h, listOf(NotifyIncomeUpdated(validIncome.personId, period.start, period.end)))
        }
    }
}

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
import fi.espoo.evaka.attachment.deleteAttachmentsByApplicationAndType
import fi.espoo.evaka.daycare.controllers.AdditionalInformation
import fi.espoo.evaka.daycare.controllers.Child
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.getActivePreschoolTermAt
import fi.espoo.evaka.daycare.getDaycare
import fi.espoo.evaka.daycare.getUnitApplyPeriods
import fi.espoo.evaka.daycare.upsertChild
import fi.espoo.evaka.decision.DecisionDraftService
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.clearDecisionDrafts
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
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.updatePersonBasicContactInfo
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementPlanRejectReason
import fi.espoo.evaka.placement.PlacementPlanService
import fi.espoo.evaka.placement.deletePlacementPlans
import fi.espoo.evaka.placement.getPlacementPlan
import fi.espoo.evaka.placement.updatePlacementPlanUnitConfirmation
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.InitializeFamilyFromApplication
import fi.espoo.evaka.shared.async.NotifyIncomeUpdated
import fi.espoo.evaka.shared.async.SendApplicationEmail
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.utils.europeHelsinki
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.core.env.Environment
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
    private val mapper: ObjectMapper,
    private val documentClient: DocumentService,
    private val env: Environment
) {
    // STATE TRANSITIONS

    fun sendApplication(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID, isEnduser: Boolean = false) {
        Audit.ApplicationSend.log(targetId = applicationId)

        val application = getApplication(tx, applicationId)
        if (isEnduser) {
            if (application.guardianId != user.id) {
                throw Forbidden("User does not own this application")
            }
        } else {
            user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)
        }

        verifyStatus(application, CREATED)
        validateApplication(tx, application.type, application.form, strict = isEnduser)

        val applicationFlags = tx.applicationFlags(application)
        tx.updateApplicationFlags(application.id, applicationFlags)

        val sentDate = application.sentDate ?: LocalDate.now()
        val dueDate = calculateDueDate(
            application.type,
            sentDate,
            application.form.preferences.urgent,
            applicationFlags.isTransferApplication,
            application.attachments,
        )
        tx.updateApplicationDates(application.id, sentDate, dueDate)

        tx.getPersonById(application.guardianId)?.let {
            val email = if (!application.form.guardian.email.isNullOrBlank()) {
                application.form.guardian.email
            } else {
                it.email
            }

            tx.updatePersonBasicContactInfo(
                id = application.guardianId,
                email = email ?: "",
                phone = application.form.guardian.phoneNumber
            )
        }

        if (!application.hideFromGuardian && application.type == ApplicationType.DAYCARE) {
            val preferredUnit =
                tx.getDaycare(application.form.preferences.preferredUnits.first().id)!! // should never be null after validation

            if (preferredUnit.providerType != ProviderType.PRIVATE_SERVICE_VOUCHER) {
                asyncJobRunner.plan(tx, listOf(SendApplicationEmail(application.guardianId, preferredUnit.language, ApplicationType.DAYCARE)))
            }
        }

        if (!application.hideFromGuardian && application.type == ApplicationType.CLUB) {
            asyncJobRunner.plan(tx, listOf(SendApplicationEmail(application.guardianId, Language.fi, ApplicationType.CLUB)))
        }

        if (!application.hideFromGuardian && application.type == ApplicationType.PRESCHOOL) {
            val sentWithinPreschoolApplicationPeriod = tx.sentWithinPreschoolApplicationPeriod(sentDate)
            asyncJobRunner.plan(tx, listOf(SendApplicationEmail(application.guardianId, Language.fi, ApplicationType.PRESCHOOL, sentWithinPreschoolApplicationPeriod)))
        }

        tx.updateApplicationStatus(application.id, SENT)
    }

    fun moveToWaitingPlacement(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationVerify.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        val application = getApplication(tx, applicationId)
        verifyStatus(application, SENT)

        tx.getPersonById(application.guardianId)?.let {
            val email = if (!application.form.guardian.email.isNullOrBlank()) {
                application.form.guardian.email
            } else {
                it.email
            }

            tx.updatePersonBasicContactInfo(
                id = application.guardianId,
                email = email ?: "",
                phone = application.form.guardian.phoneNumber
            )
        }

        tx.upsertChild(
            Child(
                id = application.childId,
                additionalInformation = AdditionalInformation(
                    allergies = application.form.child.allergies,
                    diet = application.form.child.diet
                )
            )
        )

        tx.setCheckedByAdminToDefault(applicationId, application.form)

        asyncJobRunner.plan(tx, listOf(InitializeFamilyFromApplication(application.id, user)))
        tx.updateApplicationStatus(application.id, WAITING_PLACEMENT)
    }

    fun returnToSent(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationReturnToSent.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        val application = getApplication(tx, applicationId)
        verifyStatus(application, WAITING_PLACEMENT)
        tx.updateApplicationStatus(application.id, SENT)
    }

    fun cancelApplication(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationCancel.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        val application = getApplication(tx, applicationId)
        verifyStatus(application, setOf(SENT, WAITING_PLACEMENT))
        tx.updateApplicationStatus(application.id, CANCELLED)
    }

    fun setVerified(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationAdminDetailsUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        val application = getApplication(tx, applicationId)
        verifyStatus(application, WAITING_PLACEMENT)
        tx.setApplicationVerified(applicationId, true)
    }

    fun setUnverified(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationAdminDetailsUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        val application = getApplication(tx, applicationId)
        verifyStatus(application, WAITING_PLACEMENT)
        tx.setApplicationVerified(applicationId, false)
    }

    fun createPlacementPlan(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID, placementPlan: DaycarePlacementPlan) {
        val application = getApplication(tx, applicationId)
        verifyStatus(application, WAITING_PLACEMENT)

        val guardian = personService.getUpToDatePerson(tx, user, application.guardianId)
            ?: throw NotFound("Guardian not found")
        val secondDecisionTo = personService
            .getGuardians(tx, user, application.childId)
            .firstOrNull {
                it.id != guardian.id && !livesInSameAddress(guardian.residenceCode, it.residenceCode)
            }?.id

        tx.updateApplicationOtherGuardian(applicationId, secondDecisionTo)
        placementPlanService.createPlacementPlan(tx, application, placementPlan)
        decisionDraftService.createDecisionDrafts(tx, user, application)

        tx.updateApplicationStatus(application.id, WAITING_DECISION)
    }

    fun cancelPlacementPlan(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationReturnToWaitingPlacement.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        val application = getApplication(tx, applicationId)
        verifyStatus(application, WAITING_DECISION)
        tx.deletePlacementPlans(listOf(application.id))
        tx.clearDecisionDrafts(listOf(application.id))
        tx.updateApplicationStatus(application.id, WAITING_PLACEMENT)
    }

    fun sendDecisionsWithoutProposal(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.DecisionCreate.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        val application = getApplication(tx, applicationId)
        verifyStatus(application, WAITING_DECISION)
        finalizeDecisions(tx, user, application)
    }

    fun sendPlacementProposal(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.PlacementProposalCreate.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        val application = getApplication(tx, applicationId)
        verifyStatus(application, WAITING_DECISION)
        tx.updateApplicationStatus(application.id, WAITING_UNIT_CONFIRMATION)
    }

    fun withdrawPlacementProposal(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.ApplicationReturnToWaitingDecision.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        val application = getApplication(tx, applicationId)
        verifyStatus(application, WAITING_UNIT_CONFIRMATION)
        tx.updateApplicationStatus(application.id, WAITING_DECISION)
    }

    fun respondToPlacementProposal(
        tx: Database.Transaction,
        user: AuthenticatedUser,
        applicationId: UUID,
        status: PlacementPlanConfirmationStatus,
        rejectReason: PlacementPlanRejectReason? = null,
        rejectOtherReason: String? = null
    ) {
        Audit.PlacementPlanRespond.log(targetId = applicationId)
        acl.getRolesForApplication(user, applicationId).requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)

        val application = getApplication(tx, applicationId)
        verifyStatus(application, WAITING_UNIT_CONFIRMATION)

        if (status == PlacementPlanConfirmationStatus.REJECTED) {
            if (rejectReason == null)
                throw BadRequest("Must give reason for rejecting")
            if (rejectReason == PlacementPlanRejectReason.OTHER && rejectOtherReason.isNullOrBlank())
                throw BadRequest("Must describe other reason for rejecting")

            tx.updatePlacementPlanUnitConfirmation(applicationId, status, rejectReason, rejectOtherReason)
        } else {
            tx.updatePlacementPlanUnitConfirmation(applicationId, status, null, null)
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
            .map { getApplication(tx, it.first) }
            .forEach { finalizeDecisions(tx, user, it) }
    }

    fun confirmDecisionMailed(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID) {
        Audit.DecisionConfirmMailed.log(targetId = applicationId)
        user.requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER)

        val application = getApplication(tx, applicationId)
        verifyStatus(application, WAITING_MAILING)
        tx.updateApplicationStatus(application.id, WAITING_CONFIRMATION)
    }

    fun acceptDecision(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID, decisionId: UUID, requestedStartDate: LocalDate, isEnduser: Boolean = false) {
        Audit.DecisionAccept.log(targetId = decisionId)
        val application = getApplication(tx, applicationId)
        if (isEnduser) {
            if (application.guardianId != user.id) {
                throw Forbidden("User does not own this application")
            }
        } else {
            acl.getRolesForApplication(user, applicationId).requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        }

        verifyStatus(application, setOf(WAITING_CONFIRMATION, ACTIVE))

        val decisions = tx.getDecisionsByApplication(applicationId, AclAuthorization.All)

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

        val plan = tx.getPlacementPlan(applicationId)
            ?: throw IllegalStateException("Application $applicationId has no placement plan")

        // everything validated now!

        tx.markDecisionAccepted(user, decision.id, requestedStartDate)

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
            if (application.form.maxFeeAccepted) setHighestFeeForUser(tx, application, requestedStartDate)
            tx.updateApplicationStatus(application.id, ACTIVE)
        }
    }

    fun rejectDecision(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID, decisionId: UUID, isEnduser: Boolean = false) {
        Audit.DecisionReject.log(targetId = decisionId)
        val application = getApplication(tx, applicationId)
        if (isEnduser) {
            if (application.guardianId != user.id) {
                throw Forbidden("User does not own this application")
            }
        } else {
            acl.getRolesForApplication(user, applicationId).requireOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER, UserRole.UNIT_SUPERVISOR)
        }

        verifyStatus(application, setOf(WAITING_CONFIRMATION, ACTIVE, REJECTED))

        val decisions = tx.getDecisionsByApplication(applicationId, AclAuthorization.All)
        val decision = decisions.find { it.id == decisionId }
            ?: throw NotFound("Decision $decisionId not found on application $applicationId")

        if (decision.status != DecisionStatus.PENDING) {
            throw BadRequest("Decision is not pending")
        }

        tx.markDecisionRejected(user, decisionId)

        val alsoReject = if (decision.type in listOf(DecisionType.PRESCHOOL, DecisionType.PREPARATORY_EDUCATION)) {
            decisions.find { it.type === DecisionType.PRESCHOOL_DAYCARE && it.status == DecisionStatus.PENDING }
        } else null
        alsoReject?.let { tx.markDecisionRejected(user, it.id) }

        placementPlanService.softDeleteUnusedPlacementPlanByApplication(tx, applicationId)

        if (application.status == WAITING_CONFIRMATION) {
            tx.updateApplicationStatus(application.id, REJECTED)
        }
    }

    // CONTENT UPDATE

    fun updateOwnApplicationContentsCitizen(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID, update: ApplicationFormUpdate, asDraft: Boolean = false): ApplicationDetails {
        val original = tx.fetchApplicationDetails(applicationId)
            ?.takeIf { it.guardianId == user.id }
            ?: throw NotFound("Application $applicationId of guardian ${user.id} not found")

        val updatedForm = original.form.update(update)

        val filesBucket = env.getProperty("fi.espoo.voltti.document.bucket.attachments")!!
        if (!updatedForm.preferences.urgent) {
            val deleted = tx.deleteAttachmentsByApplicationAndType(applicationId, AttachmentType.URGENCY)
            deleted.forEach { documentClient.delete(filesBucket, "$it") }
        }

        if (updatedForm.preferences.serviceNeed?.shiftCare != true) {
            val deleted = tx.deleteAttachmentsByApplicationAndType(applicationId, AttachmentType.EXTENDED_CARE)
            deleted.forEach { documentClient.delete(filesBucket, "$it") }
        }

        if (asDraft) {
            if (original.status !== CREATED) throw BadRequest("Cannot save as draft, application already sent")
        } else {
            validateApplication(tx, original.type, updatedForm, strict = true)

            if (listOf(SENT).contains(original.status)) {
                original.form.preferences.preferredStartDate?.let { previousStartDate ->
                    updatedForm.preferences.preferredStartDate?.let { newStartDate ->
                        if (previousStartDate.isAfter(newStartDate))
                            throw BadRequest("Moving start date $previousStartDate earlier to $newStartDate is not allowed")
                    }
                }
            }
        }

        tx.updateApplicationContents(original, updatedForm)
        return getApplication(tx, applicationId)
    }

    fun updateApplicationContentsServiceWorker(tx: Database.Transaction, user: AuthenticatedUser, applicationId: UUID, update: ApplicationUpdate) {
        val original = tx.fetchApplicationDetails(applicationId)
            ?: throw NotFound("Application $applicationId was not found")

        val updatedForm = original.form.update(update.form)
        validateApplication(tx, original.type, updatedForm, strict = false)

        val filesBucket = env.getProperty("fi.espoo.voltti.document.bucket.attachments")!!
        if (!updatedForm.preferences.urgent) {
            val deleted = tx.deleteAttachmentsByApplicationAndType(applicationId, AttachmentType.URGENCY)
            deleted.forEach { documentClient.delete(filesBucket, "$it") }
        }

        if (updatedForm.preferences.serviceNeed?.shiftCare != true) {
            val deleted = tx.deleteAttachmentsByApplicationAndType(applicationId, AttachmentType.EXTENDED_CARE)
            deleted.forEach { documentClient.delete(filesBucket, "$it") }
        }

        tx.updateApplicationContents(original, updatedForm, manuallySetDueDate = update.dueDate)
    }

    private fun Database.Read.sentWithinPreschoolApplicationPeriod(sentDate: LocalDate): Boolean {
        return createQuery("SELECT 1 FROM preschool_term WHERE application_period @> :date")
            .bind("date", sentDate)
            .mapTo<Boolean>()
            .toList()
            .firstOrNull() ?: false
    }

    private fun Database.Transaction.updateApplicationContents(original: ApplicationDetails, updatedForm: ApplicationForm, manuallySetDueDate: LocalDate? = null) {
        if (!listOf(CREATED, SENT).contains(original.status))
            throw BadRequest("Cannot update application with status ${original.status}")

        updateForm(original.id, updatedForm, original.type, original.childRestricted, original.guardianRestricted)
        setCheckedByAdminToDefault(original.id, updatedForm)
        when (manuallySetDueDate) {
            null -> calculateAndUpdateDueDate(original, updatedForm.preferences.urgent)
            else -> updateManuallySetDueDate(original.id, manuallySetDueDate)
        }
    }

    private fun Database.Transaction.updateManuallySetDueDate(applicationId: UUID, manuallySetDueDate: LocalDate) {
        createUpdate("UPDATE application SET duedate = :dueDate, duedate_set_manually_at = :dueDateSetManuallyAt WHERE id = :id")
            .bind("id", applicationId)
            .bind("dueDate", manuallySetDueDate)
            .bind("dueDateSetManuallyAt", HelsinkiDateTime.now())
            .execute()
    }

    private fun Database.Transaction.calculateAndUpdateDueDate(original: ApplicationDetails, urgent: Boolean) {
        if (original.sentDate == null || original.dueDateSetManuallyAt != null) return

        // If an application is flagged as urgent afterwards, the new due date is calculated from current date
        val sentDate = if (urgent && !original.form.preferences.urgent) LocalDate.now() else original.sentDate
        val newDueDate =
            calculateDueDate(original.type, sentDate, urgent, original.transferApplication, original.attachments)

        if (newDueDate == original.dueDate) return

        createUpdate("UPDATE application SET duedate = :dueDate WHERE id = :id")
            .bind("id", original.id)
            .bind("dueDate", newDueDate)
            .execute()
    }

    fun reCalculateDueDate(tx: Database.Transaction, applicationId: UUID) {
        val application = tx.fetchApplicationDetails(applicationId)
            ?: throw NotFound("Application $applicationId was not found")
        tx.calculateAndUpdateDueDate(application, application.form.preferences.urgent)
    }

    // HELPERS

    private fun getApplication(tx: Database.Read, applicationId: UUID): ApplicationDetails {
        return tx.fetchApplicationDetails(applicationId)
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

    private fun validateApplication(tx: Database.Read, type: ApplicationType, application: ApplicationForm, strict: Boolean) {
        val preferredStartDate = application.preferences.preferredStartDate
        if (type == ApplicationType.PRESCHOOL && preferredStartDate != null) {
            val canApplyForPreferredDate = tx.getActivePreschoolTermAt(preferredStartDate)
                ?.isApplicationAccepted(LocalDate.now(europeHelsinki))
                ?: false
            if (!canApplyForPreferredDate) {
                throw BadRequest("Cannot apply to preschool on $preferredStartDate at the moment")
            }
        }

        val unitIds = application.preferences.preferredUnits.map { it.id }

        if (unitIds.isEmpty()) {
            throw BadRequest("Must have at least one preferred unit")
        } else {
            val daycares = tx.getUnitApplyPeriods(unitIds.toSet())
            if (daycares.size < unitIds.toSet().size) {
                throw BadRequest("Some unit was not found")
            }
            if (strict) {
                if (preferredStartDate != null) {
                    for (daycare in daycares) {
                        if (type == ApplicationType.DAYCARE && (daycare.daycareApplyPeriod == null || !daycare.daycareApplyPeriod.includes(preferredStartDate)))
                            throw BadRequest("Cannot apply for daycare in ${daycare.id} (preferred start date $preferredStartDate, apply period ${daycare.daycareApplyPeriod})")
                        if (type == ApplicationType.PRESCHOOL && (daycare.preschoolApplyPeriod == null || !daycare.preschoolApplyPeriod.includes(preferredStartDate)))
                            throw BadRequest("Cannot apply for preschool in ${daycare.id} (preferred start date $preferredStartDate, apply period ${daycare.daycareApplyPeriod})")
                        if (type == ApplicationType.CLUB && (daycare.clubApplyPeriod == null || !daycare.clubApplyPeriod.includes(preferredStartDate)))
                            throw BadRequest("Cannot apply for club in ${daycare.id} (preferred start date $preferredStartDate, apply period ${daycare.daycareApplyPeriod})")
                    }
                }
            }
        }
    }

    private fun calculateDueDate(
        applicationType: ApplicationType,
        sentDate: LocalDate,
        isUrgent: Boolean,
        isTransferApplication: Boolean,
        attachments: List<Attachment>
    ): LocalDate? {
        return if (isTransferApplication) {
            null
        } else if (applicationType == ApplicationType.PRESCHOOL) {
            sentDate // todo: is this correct? seems weird
        } else {
            if (isUrgent) {
                // due date should not be set at all if attachments are missing
                if (attachments.isEmpty()) return null
                // due date is two weeks from application.sentDate or the first attachment, whichever is later
                val minAttachmentDate = attachments.minByOrNull { it.receivedAt }?.let { LocalDate.from(it.receivedAt) }
                listOfNotNull(minAttachmentDate, sentDate).maxOrNull()?.plusWeeks(2)
            } else {
                sentDate.plusMonths(4)
            }
        }
    }

    private fun finalizeDecisions(tx: Database.Transaction, user: AuthenticatedUser, application: ApplicationDetails) {
        val sendBySfi = canSendDecisionsBySfi(tx, user, application)
        val decisionDrafts = tx.fetchDecisionDrafts(application.id)
        if (decisionDrafts.any { it.planned }) {
            decisionService.finalizeDecisions(tx, user, application.id, sendBySfi)
            tx.updateApplicationStatus(application.id, if (sendBySfi) WAITING_CONFIRMATION else WAITING_MAILING)
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

    private fun setHighestFeeForUser(tx: Database.Transaction, application: ApplicationDetails, validFrom: LocalDate) {
        val incomes = tx.getIncomesForPerson(mapper, application.guardianId)

        val hasOverlappingDefiniteIncome = incomes.any { income ->
            income.validTo != null &&
                DateRange(income.validFrom, income.validTo).overlaps(DateRange(validFrom, null))
        }

        val hasLaterIncome = incomes.any { income ->
            income.validFrom.plusDays(1).isAfter(validFrom)
        }

        if (hasOverlappingDefiniteIncome || hasLaterIncome) {
            logger.debug { "Could not add a new max fee accepted income from application ${application.id}" }
        } else {
            val period = DateRange(start = validFrom, end = null)
            val validIncome = Income(
                id = UUID.randomUUID(),
                data = mapOf(),
                effect = IncomeEffect.MAX_FEE_ACCEPTED,
                notes = "created automatically from application",
                personId = application.guardianId,
                validFrom = validFrom,
                validTo = null,
                applicationId = application.id
            ).let(::validateIncome)
            tx.splitEarlierIncome(validIncome.personId, period)
            tx.upsertIncome(mapper, validIncome, application.guardianId)
            asyncJobRunner.plan(tx, listOf(NotifyIncomeUpdated(validIncome.personId, period.start, period.end)))
        }
    }
}

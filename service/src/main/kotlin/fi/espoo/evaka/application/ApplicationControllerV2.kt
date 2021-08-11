// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.application.utils.currentDateInFinland
import fi.espoo.evaka.application.utils.ok
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionDraft
import fi.espoo.evaka.decision.DecisionDraftService
import fi.espoo.evaka.decision.DecisionUnit
import fi.espoo.evaka.decision.fetchDecisionDrafts
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.controller.parseUUID
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementPlanDraft
import fi.espoo.evaka.placement.PlacementPlanRejectReason
import fi.espoo.evaka.placement.PlacementPlanService
import fi.espoo.evaka.placement.getPlacementPlanUnitName
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.Paged
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

enum class ApplicationTypeToggle {
    CLUB,
    DAYCARE,
    PRESCHOOL,
    ALL
}

enum class ApplicationPreschoolTypeToggle {
    PRESCHOOL_ONLY,
    PRESCHOOL_DAYCARE,
    PREPARATORY_ONLY,
    PREPARATORY_DAYCARE,
    DAYCARE_ONLY
}

enum class ApplicationDistinctions {
    SECONDARY
}

enum class ApplicationDateType {
    DUE,
    START,
    ARRIVAL
}

enum class ApplicationStatusOption {
    SENT,
    WAITING_PLACEMENT,
    WAITING_UNIT_CONFIRMATION,
    WAITING_DECISION,
    WAITING_MAILING,
    WAITING_CONFIRMATION,
    REJECTED,
    ACTIVE,
    CANCELLED;

    fun toStatus(): ApplicationStatus = when (this) {
        SENT -> ApplicationStatus.SENT
        WAITING_PLACEMENT -> ApplicationStatus.WAITING_PLACEMENT
        WAITING_UNIT_CONFIRMATION -> ApplicationStatus.WAITING_UNIT_CONFIRMATION
        WAITING_DECISION -> ApplicationStatus.WAITING_DECISION
        WAITING_MAILING -> ApplicationStatus.WAITING_MAILING
        WAITING_CONFIRMATION -> ApplicationStatus.WAITING_CONFIRMATION
        REJECTED -> ApplicationStatus.REJECTED
        ACTIVE -> ApplicationStatus.ACTIVE
        CANCELLED -> ApplicationStatus.CANCELLED
    }
}

enum class TransferApplicationFilter {
    TRANSFER_ONLY, NO_TRANSFER, ALL
}

enum class VoucherApplicationFilter {
    VOUCHER_FIRST_CHOICE, VOUCHER_ONLY, NO_VOUCHER
}

@RestController
@RequestMapping("/v2/applications")
class ApplicationControllerV2(
    private val acl: AccessControlList,
    private val accessControl: AccessControl,
    private val personService: PersonService,
    private val applicationStateService: ApplicationStateService,
    private val placementPlanService: PlacementPlanService,
    private val decisionDraftService: DecisionDraftService
) {
    @PostMapping
    fun createPaperApplication(
        db: Database,
        user: AuthenticatedUser,
        @RequestBody body: PaperApplicationCreateRequest
    ): ResponseEntity<ApplicationId> {
        Audit.ApplicationCreate.log(targetId = body.guardianId, objectId = body.childId)
        accessControl.requirePermissionFor(user, Action.Global.CREATE_PAPER_APPLICATION)

        val id = db.transaction { tx ->
            val child = personService.getUpToDatePerson(tx, user, body.childId)
                ?: throw BadRequest("Could not find the child with id ${body.childId}")

            val guardianId =
                body.guardianId
                    ?: if (!body.guardianSsn.isNullOrEmpty())
                        personService.getOrCreatePerson(
                            tx,
                            user,
                            ExternalIdentifier.SSN.getInstance(body.guardianSsn)
                        )?.id ?: throw BadRequest("Could not find the guardian with ssn ${body.guardianSsn}")
                    else if (body.guardianToBeCreated != null)
                        createPerson(tx, body.guardianToBeCreated)
                    else
                        throw BadRequest("Could not find guardian info from paper application request for ${body.childId}")

            val guardian = personService.getUpToDatePerson(tx, user, guardianId)
                ?: throw BadRequest("Could not find the guardian with id $guardianId")

            val id = tx.insertApplication(
                guardianId = guardianId,
                childId = body.childId,
                origin = ApplicationOrigin.PAPER,
                hideFromGuardian = body.hideFromGuardian,
                sentDate = body.sentDate
            )
            val form = ApplicationForm.initForm(
                type = body.type,
                guardian = guardian,
                child = child
            )
            tx.updateForm(id, form, body.type, child.restrictedDetailsEnabled, guardian.restrictedDetailsEnabled)
            id
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(id)
    }

    @GetMapping
    fun getApplicationSummaries(
        db: Database,
        user: AuthenticatedUser,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) pageSize: Int?,
        @RequestParam(required = false) sortBy: ApplicationSortColumn?,
        @RequestParam(required = false) sortDir: ApplicationSortDirection?,
        @RequestParam(required = false) area: String?,
        @RequestParam(required = false) units: String?,
        @RequestParam(required = false) basis: String?,
        @RequestParam(required = true) type: ApplicationTypeToggle,
        @RequestParam(required = false) preschoolType: String?,
        @RequestParam(required = true) status: String?,
        @RequestParam(required = false) dateType: String?,
        @RequestParam(required = false) distinctions: String?,
        @RequestParam(
            "periodStart",
            required = false
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) periodStart: LocalDate?,
        @RequestParam(
            "periodEnd",
            required = false
        ) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) periodEnd: LocalDate?,
        @RequestParam(required = false) searchTerms: String?,
        @RequestParam(required = false) transferApplications: TransferApplicationFilter?,
        @RequestParam(required = false) voucherApplications: VoucherApplicationFilter?
    ): ResponseEntity<Paged<ApplicationSummary>> {
        Audit.ApplicationSearch.log()
        if (periodStart != null && periodEnd != null && periodStart > periodEnd)
            throw BadRequest("Date parameter periodEnd ($periodEnd) cannot be before periodStart ($periodStart)")

        val authorizedUnitsForApplicationsWithAssistanceNeed = acl.getAuthorizedUnits(
            user = user,
            roles = accessControl.getPermittedRoles(Action.Global.SEARCH_APPLICATION_WITH_ASSISTANCE_NEED)
        )
        val authorizedUnitsForApplicationsWithoutAssistanceNeed = acl.getAuthorizedUnits(
            user = user,
            roles = accessControl.getPermittedRoles(Action.Global.SEARCH_APPLICATION_WITHOUT_ASSISTANCE_NEED)
        )

        if (authorizedUnitsForApplicationsWithAssistanceNeed.isEmpty() && authorizedUnitsForApplicationsWithoutAssistanceNeed.isEmpty()) {
            throw Forbidden("application search not allowed for any unit")
        }

        return db.read { tx ->
            tx.fetchApplicationSummaries(
                user = user,
                page = page ?: 1,
                pageSize = pageSize ?: 100,
                sortBy = sortBy ?: ApplicationSortColumn.CHILD_NAME,
                sortDir = sortDir ?: ApplicationSortDirection.ASC,
                areas = area?.split(",") ?: listOf(),
                units = units?.split(",")?.map { parseUUID(it) } ?: listOf(),
                basis = basis?.split(",")?.map { ApplicationBasis.valueOf(it) } ?: listOf(),
                type = type,
                preschoolType = preschoolType?.split(",")?.map { ApplicationPreschoolTypeToggle.valueOf(it) }
                    ?: listOf(),
                statuses = status?.split(",")?.map { ApplicationStatusOption.valueOf(it) } ?: listOf(),
                dateType = dateType?.split(",")?.map { ApplicationDateType.valueOf(it) } ?: listOf(),
                distinctions = distinctions?.split(",")?.map { ApplicationDistinctions.valueOf(it) } ?: listOf(),
                periodStart = periodStart,
                periodEnd = periodEnd,
                searchTerms = searchTerms ?: "",
                transferApplications = transferApplications ?: TransferApplicationFilter.ALL,
                voucherApplications = voucherApplications,
                authorizedUnitsForApplicationsWithoutAssistanceNeed = authorizedUnitsForApplicationsWithoutAssistanceNeed,
                authorizedUnitsForApplicationsWithAssistanceNeed = authorizedUnitsForApplicationsWithAssistanceNeed
            )
        }.let { ResponseEntity.ok(it) }
    }

    @GetMapping("/by-guardian/{guardianId}")
    fun getGuardianApplicationSummaries(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "guardianId") guardianId: UUID
    ): ResponseEntity<List<PersonApplicationSummary>> {
        Audit.ApplicationRead.log(targetId = guardianId)
        accessControl.requirePermissionFor(user, Action.Global.READ_PERSON_APPLICATION)

        return db.read { it.fetchApplicationSummariesForGuardian(guardianId) }
            .let { ResponseEntity.ok().body(it) }
    }

    @GetMapping("/by-child/{childId}")
    fun getChildApplicationSummaries(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "childId") childId: UUID
    ): ResponseEntity<List<PersonApplicationSummary>> {
        Audit.ApplicationRead.log(targetId = childId)
        accessControl.requirePermissionFor(user, Action.Child.READ_APPLICATION, childId)

        return db.read { it.fetchApplicationSummariesForChild(childId) }
            .let { ResponseEntity.ok().body(it) }
    }

    @GetMapping("/{applicationId}")
    fun getApplicationDetails(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: ApplicationId
    ): ResponseEntity<ApplicationResponse> {
        Audit.ApplicationRead.log(targetId = applicationId)
        Audit.DecisionRead.log(targetId = applicationId)

        return db.transaction { tx ->
            val application = tx.fetchApplicationDetails(applicationId)
                ?: throw NotFound("Application $applicationId was not found")

            accessControl.requirePermissionFor(
                user = user,
                action = if (application.form.child.assistanceNeeded) Action.Application.READ_WITH_ASSISTANCE_NEED else Action.Application.READ_WITHHOUT_ASSISTANCE_NEED,
                id = applicationId
            )

            val decisions = tx.getDecisionsByApplication(applicationId, acl.getAuthorizedUnits(user))
            val guardians =
                personService.getGuardians(tx, user, application.childId).map { personDTO -> PersonJSON.from(personDTO) }

            // todo: can this be refactored under Action model?
            val roles = acl.getRolesForApplication(user, applicationId)
            val attachments: List<Attachment> = when {
                roles.hasOneOfRoles(UserRole.ADMIN, UserRole.SERVICE_WORKER) ->
                    tx.getApplicationAttachments(applicationId)
                roles.hasOneOfRoles(UserRole.UNIT_SUPERVISOR) ->
                    tx.getApplicationAttachmentsForUnitSupervisor(applicationId)
                else -> listOf()
            }

            ResponseEntity.ok(
                ApplicationResponse(
                    application = application.copy(attachments = attachments),
                    decisions = decisions,
                    guardians = guardians,
                    attachments = attachments
                )
            )
        }
    }

    @PutMapping("/{applicationId}")
    fun updateApplication(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId,
        @RequestBody application: ApplicationUpdate
    ): ResponseEntity<Unit> {
        Audit.ApplicationUpdate.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Application.UPDATE, applicationId)

        db.transaction {
            applicationStateService.updateApplicationContentsServiceWorker(
                it,
                user,
                applicationId,
                application,
                user.id,
                currentDateInFinland()
            )
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/send-application")
    fun sendApplication(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId
    ): ResponseEntity<Unit> {
        db.transaction { applicationStateService.sendApplication(it, user, applicationId, currentDateInFinland()) }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{applicationId}/placement-draft")
    fun getPlacementPlanDraft(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: ApplicationId
    ): ResponseEntity<PlacementPlanDraft> {
        Audit.PlacementPlanDraftRead.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Application.READ_PLACEMENT_PLAN_DRAFT, applicationId)
        return db.read { placementPlanService.getPlacementPlanDraft(it, applicationId) }
            .let { ResponseEntity.ok(it) }
    }

    @GetMapping("/{applicationId}/decision-drafts")
    fun getDecisionDrafts(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: ApplicationId
    ): ResponseEntity<DecisionDraftJSON> {
        Audit.DecisionDraftRead.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Application.READ_DECISION_DRAFT, applicationId)

        return db.transaction { tx ->
            val application = tx.fetchApplicationDetails(applicationId)
                ?: throw NotFound("Application $applicationId not found")

            if (application.status !== ApplicationStatus.WAITING_DECISION) {
                throw Conflict("Cannot get decision drafts for application with status ${application.status}")
            }

            val placementUnitName = tx.getPlacementPlanUnitName(applicationId)

            val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
            val unit = decisionDraftService.getDecisionUnit(tx, decisionDrafts[0].unitId)

            val applicationGuardian = personService.getUpToDatePerson(tx, user, application.guardianId)
                ?: throw NotFound("Guardian ${application.guardianId} not found")
            val child = personService.getUpToDatePerson(tx, user, application.childId)
                ?: throw NotFound("Child ${application.childId} not found")
            val vtjGuardians = personService.getGuardians(tx, user, child.id)

            val applicationGuardianIsVtjGuardian: Boolean = vtjGuardians.any { it.id == application.guardianId }
            val otherGuardian = application.otherGuardianId?.let { personService.getUpToDatePerson(tx, user, it) }

            ok(
                DecisionDraftJSON(
                    decisions = decisionDrafts,
                    placementUnitName = placementUnitName,
                    unit = unit,
                    guardian = GuardianInfo(
                        firstName = applicationGuardian.firstName ?: "",
                        lastName = applicationGuardian.lastName ?: "",
                        ssn = (applicationGuardian.identity as? ExternalIdentifier.SSN)?.toString(),
                        isVtjGuardian = applicationGuardianIsVtjGuardian
                    ),
                    child = ChildInfo(
                        firstName = child.firstName ?: "",
                        lastName = child.lastName ?: "",
                        ssn = (child.identity as? ExternalIdentifier.SSN)?.toString()
                    ),
                    otherGuardian = otherGuardian?.let {
                        GuardianInfo(
                            id = it.id,
                            firstName = it.firstName ?: "",
                            lastName = it.lastName ?: "",
                            ssn = (it.identity as? ExternalIdentifier.SSN)?.toString(),
                            isVtjGuardian = true
                        )
                    }
                )
            )
        }
    }

    @PutMapping("/{applicationId}/decision-drafts")
    fun updateDecisionDrafts(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: ApplicationId,
        @RequestBody body: List<DecisionDraftService.DecisionDraftUpdate>
    ): ResponseEntity<Unit> {
        Audit.DecisionDraftUpdate.log(targetId = applicationId)
        accessControl.requirePermissionFor(user, Action.Application.UPDATE_DECISION_DRAFT, applicationId)

        db.transaction { decisionDraftService.updateDecisionDrafts(it, applicationId, body) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/placement-proposals/{unitId}/accept")
    fun acceptPlacementProposal(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable(value = "unitId") unitId: DaycareId
    ): ResponseEntity<Unit> {
        db.transaction { applicationStateService.acceptPlacementProposal(it, user, unitId) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/batch/actions/{action}")
    fun simpleBatchAction(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable action: String,
        @RequestBody body: SimpleBatchRequest
    ): ResponseEntity<Unit> {
        val simpleBatchActions = mapOf(
            "move-to-waiting-placement" to applicationStateService::moveToWaitingPlacement,
            "return-to-sent" to applicationStateService::returnToSent,
            "cancel-placement-plan" to applicationStateService::cancelPlacementPlan,
            "send-decisions-without-proposal" to applicationStateService::sendDecisionsWithoutProposal,
            "send-placement-proposal" to applicationStateService::sendPlacementProposal,
            "withdraw-placement-proposal" to applicationStateService::withdrawPlacementProposal,
            "confirm-decision-mailed" to applicationStateService::confirmDecisionMailed
        )

        val actionFn = simpleBatchActions[action] ?: throw NotFound("Batch action not recognized")
        db.transaction { tx ->
            body.applicationIds.forEach { applicationId -> actionFn.invoke(tx, user, applicationId) }
        }

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/create-placement-plan")
    fun createPlacementPlan(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: DaycarePlacementPlan
    ): ResponseEntity<Unit> {
        Audit.PlacementPlanCreate.log(targetId = applicationId, objectId = body.unitId)
        accessControl.requirePermissionFor(user, Action.Application.CREATE_PLACEMENT_PLAN, applicationId)

        db.transaction { applicationStateService.createPlacementPlan(it, user, applicationId, body) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/respond-to-placement-proposal")
    fun respondToPlacementProposal(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: PlacementProposalConfirmationUpdate
    ): ResponseEntity<Unit> {
        db.transaction {
            applicationStateService.respondToPlacementProposal(
                it,
                user,
                applicationId,
                body.status,
                body.reason,
                body.otherReason
            )
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/accept-decision")
    fun acceptDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: AcceptDecisionRequest
    ): ResponseEntity<Unit> {
        db.transaction {
            applicationStateService.acceptDecision(it, user, applicationId, body.decisionId, body.requestedStartDate)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/reject-decision")
    fun rejectDecision(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: RejectDecisionRequest
    ): ResponseEntity<Unit> {
        db.transaction { applicationStateService.rejectDecision(it, user, applicationId, body.decisionId) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/{action}")
    fun simpleAction(
        db: Database,
        user: AuthenticatedUser,
        @PathVariable applicationId: ApplicationId,
        @PathVariable action: String
    ): ResponseEntity<Unit> {
        val simpleActions = mapOf(
            "move-to-waiting-placement" to applicationStateService::moveToWaitingPlacement,
            "return-to-sent" to applicationStateService::returnToSent,
            "cancel-application" to applicationStateService::cancelApplication,
            "set-verified" to applicationStateService::setVerified,
            "set-unverified" to applicationStateService::setUnverified,
            "cancel-placement-plan" to applicationStateService::cancelPlacementPlan,
            "send-decisions-without-proposal" to applicationStateService::sendDecisionsWithoutProposal,
            "send-placement-proposal" to applicationStateService::sendPlacementProposal,
            "withdraw-placement-proposal" to applicationStateService::withdrawPlacementProposal,
            "confirm-decision-mailed" to applicationStateService::confirmDecisionMailed
        )

        val actionFn = simpleActions[action] ?: throw NotFound("Action not recognized")
        db.transaction { actionFn.invoke(it, user, applicationId) }
        return ResponseEntity.noContent().build()
    }
}

data class PaperApplicationCreateRequest(
    val childId: UUID,
    val guardianId: UUID? = null,
    val guardianToBeCreated: CreatePersonBody?,
    val guardianSsn: String? = null,
    val type: ApplicationType,
    val sentDate: LocalDate,
    val hideFromGuardian: Boolean,
    val transferApplication: Boolean
)

data class ApplicationResponse(
    val application: ApplicationDetails,
    val decisions: List<Decision>,
    val guardians: List<PersonJSON>,
    val attachments: List<Attachment>
)

data class SimpleBatchRequest(
    val applicationIds: Set<ApplicationId>
)

data class PlacementProposalConfirmationUpdate(
    val status: PlacementPlanConfirmationStatus,
    val reason: PlacementPlanRejectReason?,
    val otherReason: String?
)

data class DaycarePlacementPlan(
    val unitId: DaycareId,
    val period: FiniteDateRange,
    val preschoolDaycarePeriod: FiniteDateRange? = null
)

data class AcceptDecisionRequest(
    val decisionId: DecisionId,
    val requestedStartDate: LocalDate
)

data class RejectDecisionRequest(
    val decisionId: DecisionId
)

data class DecisionDraftJSON(
    val decisions: List<DecisionDraft>,
    val placementUnitName: String,
    val unit: DecisionUnit,
    val guardian: GuardianInfo,
    val otherGuardian: GuardianInfo?,
    val child: ChildInfo
)

data class ChildInfo(
    val ssn: String?,
    val firstName: String,
    val lastName: String
)

data class GuardianInfo(
    val id: UUID? = null,
    val ssn: String?,
    val firstName: String,
    val lastName: String,
    val isVtjGuardian: Boolean
)

// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
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
import fi.espoo.evaka.shared.auth.AccessControlList
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.NotFound
import org.jdbi.v3.core.Jdbi
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
import javax.sql.DataSource

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

@RestController
@RequestMapping("/v2/applications")
class ApplicationControllerV2(
    private val jdbi: Jdbi,
    private val dataSource: DataSource,
    private val acl: AccessControlList,
    private val personService: PersonService,
    private val applicationStateService: ApplicationStateService,
    private val placementPlanService: PlacementPlanService,
    private val decisionDraftService: DecisionDraftService
) {
    @PostMapping
    fun createPaperApplication(
        user: AuthenticatedUser,
        @RequestBody body: PaperApplicationCreateRequest
    ): ResponseEntity<UUID> {
        Audit.ApplicationCreate.log(targetId = body.guardianId, objectId = body.childId)
        user.requireOneOfRoles(Roles.SERVICE_WORKER, Roles.ADMIN)

        val id = jdbi.transaction { h ->
            val child = personService.getUpToDatePerson(h, user, body.childId)
                ?: throw BadRequest("Could not find the child with id ${body.childId}")

            val guardianId =
                body.guardianId
                    ?: if (!body.guardianSsn.isNullOrEmpty())
                        personService.getOrCreatePerson(
                            h,
                            user,
                            ExternalIdentifier.SSN.getInstance(body.guardianSsn)
                        )?.id ?: throw BadRequest("Could not find the guardian with ssn ${body.guardianSsn}")
                    else if (body.guardianToBeCreated != null)
                        h.createPerson(body.guardianToBeCreated)
                    else
                        throw BadRequest("Could not find guardian info from paper application request for ${body.childId}")

            val guardian = personService.getUpToDatePerson(h, user, guardianId)
                ?: throw BadRequest("Could not find the guardian with id $guardianId")

            val id = insertApplication(
                h = h,
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
            updateForm(h, id, form, body.type, child.restrictedDetailsEnabled, guardian.restrictedDetailsEnabled)
            id
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(id)
    }

    @GetMapping
    fun getApplicationSummaries(
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
        @RequestParam(required = false) transferApplications: TransferApplicationFilter?
    ): ResponseEntity<ApplicationSummaries> {
        Audit.ApplicationSearch.log()
        user.requireOneOfRoles(Roles.ADMIN, Roles.FINANCE_ADMIN, Roles.SERVICE_WORKER)

        if (periodStart != null && periodEnd != null && periodStart > periodEnd)
            throw BadRequest("Date parameter periodEnd ($periodEnd) cannot be before periodStart ($periodStart)")

        return jdbi.transaction { h ->
            fetchApplicationSummaries(
                h = h.setReadOnly(true),
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
                transferApplications = transferApplications ?: TransferApplicationFilter.ALL
            )
        }.let { ResponseEntity.ok(it) }
    }

    @GetMapping("/by-guardian/{guardianId}")
    fun getGuardianApplicationSummaries(
        user: AuthenticatedUser,
        @PathVariable(value = "guardianId") guardianId: UUID
    ): ResponseEntity<List<PersonApplicationSummary>> {
        Audit.ApplicationRead.log(targetId = guardianId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER, Roles.FINANCE_ADMIN, Roles.UNIT_SUPERVISOR)

        return jdbi.transaction { h -> fetchApplicationSummariesForGuardian(h, guardianId) }
            .let { ResponseEntity.ok().body(it) }
    }

    @GetMapping("/by-child/{childId}")
    fun getChildApplicationSummaries(
        user: AuthenticatedUser,
        @PathVariable(value = "childId") childId: UUID
    ): ResponseEntity<List<PersonApplicationSummary>> {
        Audit.ApplicationRead.log(targetId = childId)
        acl.getRolesForChild(user, childId)
            .requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER, Roles.FINANCE_ADMIN, Roles.UNIT_SUPERVISOR)

        return jdbi.transaction { h -> fetchApplicationSummariesForChild(h, childId) }
            .let { ResponseEntity.ok().body(it) }
    }

    @GetMapping("/{applicationId}")
    fun getApplicationDetails(
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: UUID
    ): ResponseEntity<ApplicationResponse> {
        Audit.ApplicationRead.log(targetId = applicationId)
        Audit.DecisionRead.log(targetId = applicationId)
        acl.getRolesForApplication(user, applicationId)
            .requireOneOfRoles(Roles.ADMIN, Roles.FINANCE_ADMIN, Roles.SERVICE_WORKER, Roles.UNIT_SUPERVISOR)

        return jdbi.transaction { h ->
            val application = fetchApplicationDetails(h, applicationId)
                ?: throw NotFound("Application $applicationId was not found")
            val decisions = getDecisionsByApplication(h, applicationId, acl.getAuthorizedUnits(user))
            val guardians =
                personService.getGuardians(h, user, application.childId).map { personDTO -> PersonJSON.from(personDTO) }

            ResponseEntity.ok(
                ApplicationResponse(
                    application = application,
                    decisions = decisions.map { it.copy(documentUri = null) },
                    guardians = guardians
                )
            )
        }
    }

    @PutMapping("/{applicationId}")
    fun updateApplication(
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody applicationForm: ApplicationForm
    ): ResponseEntity<Unit> {
        jdbi.transaction { applicationStateService.updateApplicationContents(it, user, applicationId, applicationForm) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/send-application")
    fun sendApplication(
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID
    ): ResponseEntity<Unit> {
        jdbi.transaction { applicationStateService.sendApplication(it, user, applicationId) }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{applicationId}/placement-draft")
    fun getPlacementPlanDraft(
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: UUID
    ): ResponseEntity<PlacementPlanDraft> {
        Audit.PlacementPlanDraftRead.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.SERVICE_WORKER, Roles.ADMIN)
        return jdbi.transaction { placementPlanService.getPlacementPlanDraft(it, applicationId) }
            .let { ResponseEntity.ok(it) }
    }

    @GetMapping("/{applicationId}/decision-drafts")
    fun getDecisionDrafts(
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: UUID
    ): ResponseEntity<DecisionDraftJSON> {
        Audit.DecisionDraftRead.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.SERVICE_WORKER, Roles.ADMIN)

        return jdbi.transaction { h ->
            val application = fetchApplicationDetails(h, applicationId)
                ?: throw NotFound("Application $applicationId not found")

            val placementUnitName = getPlacementPlanUnitName(h, applicationId)

            val decisionDrafts = fetchDecisionDrafts(h, applicationId)
            val unit = decisionDraftService.getDecisionUnit(h, decisionDrafts[0].unitId)

            val applicationGuardian = personService.getUpToDatePerson(h, user, application.guardianId)
                ?: throw NotFound("Guardian ${application.guardianId} not found")
            val child = personService.getUpToDatePerson(h, user, application.childId)
                ?: throw NotFound("Child ${application.childId} not found")
            val vtjGuardians = personService.getGuardians(h, user, child.id)

            val applicationGuardianIsVtjGuardian: Boolean = vtjGuardians.any { it.id == application.guardianId }
            val otherGuardian = application.otherGuardianId?.let { personService.getUpToDatePerson(h, user, it) }

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
        user: AuthenticatedUser,
        @PathVariable(value = "applicationId") applicationId: UUID,
        @RequestBody body: List<DecisionDraftService.DecisionDraftUpdate>
    ): ResponseEntity<Unit> {
        Audit.DecisionDraftUpdate.log(targetId = applicationId)
        user.requireOneOfRoles(Roles.SERVICE_WORKER, Roles.ADMIN)

        jdbi.transaction { decisionDraftService.updateDecisionDrafts(it, applicationId, body) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/placement-proposals/{unitId}/accept")
    fun acceptPlacementProposal(
        user: AuthenticatedUser,
        @PathVariable(value = "unitId") unitId: UUID
    ): ResponseEntity<Unit> {
        jdbi.transaction { applicationStateService.acceptPlacementProposal(it, user, unitId) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/batch/actions/{action}")
    fun simpleBatchAction(
        user: AuthenticatedUser,
        @PathVariable action: String,
        @RequestBody body: SimpleBatchRequest
    ): ResponseEntity<Unit> {
        val simpleBatchActions = mapOf(
            "move-to-waiting-placement" to applicationStateService::moveToWaitingPlacement,
            "return-to-sent" to applicationStateService::returnToSent,
            "cancel-placement-plan" to applicationStateService::cancelPlacementPlan,
            "confirm-placement-without-decision" to applicationStateService::confirmPlacementWithoutDecision,
            "send-decisions-without-proposal" to applicationStateService::sendDecisionsWithoutProposal,
            "send-placement-proposal" to applicationStateService::sendPlacementProposal,
            "withdraw-placement-proposal" to applicationStateService::withdrawPlacementProposal,
            "confirm-decision-mailed" to applicationStateService::confirmDecisionMailed
        )

        val actionFn = simpleBatchActions[action] ?: throw NotFound("Batch action not recognized")
        jdbi.transaction { h ->
            body.applicationIds.forEach { applicationId -> actionFn.invoke(h, user, applicationId) }
        }

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/create-placement-plan")
    fun createPlacementPlan(
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody body: DaycarePlacementPlan
    ): ResponseEntity<Unit> {
        Audit.PlacementPlanCreate.log(targetId = applicationId, objectId = body.unitId)
        user.requireOneOfRoles(Roles.ADMIN, Roles.SERVICE_WORKER)

        jdbi.transaction { applicationStateService.createPlacementPlan(it, user, applicationId, body) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/respond-to-placement-proposal")
    fun respondToPlacementProposal(
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody body: PlacementProposalConfirmationUpdate
    ): ResponseEntity<Unit> {
        jdbi.transaction {
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
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody body: AcceptDecisionRequest
    ): ResponseEntity<Unit> {
        jdbi.transaction {
            applicationStateService.acceptDecision(it, user, applicationId, body.decisionId, body.requestedStartDate)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/reject-decision")
    fun rejectDecision(
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @RequestBody body: RejectDecisionRequest
    ): ResponseEntity<Unit> {
        jdbi.transaction { applicationStateService.rejectDecision(it, user, applicationId, body.decisionId) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{applicationId}/actions/{action}")
    fun simpleAction(
        user: AuthenticatedUser,
        @PathVariable applicationId: UUID,
        @PathVariable action: String
    ): ResponseEntity<Unit> {
        val simpleActions = mapOf(
            "move-to-waiting-placement" to applicationStateService::moveToWaitingPlacement,
            "return-to-sent" to applicationStateService::returnToSent,
            "cancel-application" to applicationStateService::cancelApplication,
            "set-verified" to applicationStateService::setVerified,
            "set-unverified" to applicationStateService::setUnverified,
            "cancel-placement-plan" to applicationStateService::cancelPlacementPlan,
            "confirm-placement-without-decision" to applicationStateService::confirmPlacementWithoutDecision,
            "send-decisions-without-proposal" to applicationStateService::sendDecisionsWithoutProposal,
            "send-placement-proposal" to applicationStateService::sendPlacementProposal,
            "withdraw-placement-proposal" to applicationStateService::withdrawPlacementProposal,
            "confirm-decision-mailed" to applicationStateService::confirmDecisionMailed
        )

        val actionFn = simpleActions[action] ?: throw NotFound("Action not recognized")
        jdbi.transaction { actionFn.invoke(it, user, applicationId) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/clear-old-drafts")
    fun removeOldDraftApplications(
        user: AuthenticatedUser
    ): ResponseEntity<Unit> {
        Audit.ApplicationsDeleteDrafts.log()
        jdbi.transaction { removeOldDrafts(it) }

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
    val guardians: List<PersonJSON>
)

data class SimpleBatchRequest(
    val applicationIds: Set<UUID>
)

data class PlacementProposalConfirmationUpdate(
    val status: PlacementPlanConfirmationStatus,
    val reason: PlacementPlanRejectReason?,
    val otherReason: String?
)

data class DaycarePlacementPlan(
    val unitId: UUID,
    val period: ClosedPeriod,
    val preschoolDaycarePeriod: ClosedPeriod? = null
)

data class AcceptDecisionRequest(
    val decisionId: UUID,
    val requestedStartDate: LocalDate
)

data class RejectDecisionRequest(
    val decisionId: UUID
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

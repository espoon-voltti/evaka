// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.Audit
import fi.espoo.evaka.AuditId
import fi.espoo.evaka.ConstList
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionDraft
import fi.espoo.evaka.decision.DecisionDraftUpdate
import fi.espoo.evaka.decision.DecisionUnit
import fi.espoo.evaka.decision.fetchDecisionDrafts
import fi.espoo.evaka.decision.getDecisionUnit
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.decision.updateDecisionDrafts
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.pis.controllers.CreatePersonBody
import fi.espoo.evaka.pis.createPerson
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.pis.service.PersonJSON
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.placement.PlacementPlanConfirmationStatus
import fi.espoo.evaka.placement.PlacementPlanDetails
import fi.espoo.evaka.placement.PlacementPlanDraft
import fi.espoo.evaka.placement.PlacementPlanRejectReason
import fi.espoo.evaka.placement.PlacementPlanService
import fi.espoo.evaka.placement.getPlacementPlanUnitName
import fi.espoo.evaka.placement.getPlacementPlans
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Conflict
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.AccessControl
import fi.espoo.evaka.shared.security.Action
import fi.espoo.evaka.shared.security.upsertCitizenUser
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

enum class ApplicationTypeToggle {
    CLUB,
    DAYCARE,
    PRESCHOOL,
    ALL;

    fun toApplicationType(): ApplicationType? =
        when (this) {
            CLUB -> ApplicationType.CLUB
            DAYCARE -> ApplicationType.DAYCARE
            PRESCHOOL -> ApplicationType.PRESCHOOL
            ALL -> null
        }
}

enum class ApplicationPreschoolTypeToggle {
    PRESCHOOL_ONLY,
    PRESCHOOL_DAYCARE,
    PRESCHOOL_CLUB,
    PREPARATORY_ONLY,
    PREPARATORY_DAYCARE,
    DAYCARE_ONLY,
}

enum class ApplicationDistinctions {
    SECONDARY
}

enum class ApplicationDateType {
    DUE,
    START,
    ARRIVAL,
}

@ConstList(name = "applicationStatusOptions")
enum class ApplicationStatusOption : DatabaseEnum {
    SENT,
    WAITING_PLACEMENT,
    WAITING_UNIT_CONFIRMATION,
    WAITING_DECISION,
    WAITING_MAILING,
    WAITING_CONFIRMATION,
    REJECTED,
    ACTIVE,
    CANCELLED;

    override val sqlType: String = "application_status_type"
}

enum class TransferApplicationFilter {
    TRANSFER_ONLY,
    NO_TRANSFER,
    ALL,
}

enum class VoucherApplicationFilter {
    VOUCHER_FIRST_CHOICE,
    VOUCHER_ONLY,
    NO_VOUCHER,
}

@RestController
@RequestMapping(
    "/v2/applications", // deprecated
    "/employee/applications",
)
class ApplicationControllerV2(
    private val accessControl: AccessControl,
    private val personService: PersonService,
    private val applicationStateService: ApplicationStateService,
    private val placementPlanService: PlacementPlanService,
) {
    @PostMapping
    fun createPaperApplication(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: PaperApplicationCreateRequest,
    ): ApplicationId {
        val (guardianId, applicationId) =
            db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Global.CREATE_PAPER_APPLICATION,
                    )

                    val child =
                        tx.getPersonById(body.childId)
                            ?: throw BadRequest("Could not find the child with id ${body.childId}")

                    val guardianId =
                        body.guardianId
                            ?: if (!body.guardianSsn.isNullOrEmpty()) {
                                personService
                                    .getOrCreatePerson(
                                        tx,
                                        user,
                                        ExternalIdentifier.SSN.getInstance(body.guardianSsn),
                                    )
                                    ?.id
                                    ?: throw BadRequest(
                                        "Could not find the guardian with ssn ${body.guardianSsn}"
                                    )
                            } else if (body.guardianToBeCreated != null) {
                                createPerson(tx, body.guardianToBeCreated)
                            } else {
                                throw BadRequest(
                                    "Could not find guardian info from paper application request for ${body.childId}"
                                )
                            }

                    val guardian =
                        tx.getPersonById(guardianId)
                            ?: throw BadRequest("Could not find the guardian with id $guardianId")

                    // If the guardian has never logged in to eVaka, evaka_user might not contain a
                    // row for them yet
                    tx.upsertCitizenUser(guardianId)

                    val applicationId =
                        applicationStateService.createApplication(
                            tx = tx,
                            user = user,
                            now = clock.now(),
                            type = body.type,
                            guardian = guardian,
                            child = child,
                            origin = ApplicationOrigin.PAPER,
                            hideFromGuardian = body.hideFromGuardian,
                            sentDate = body.sentDate,
                            allowOtherGuardianAccess = true,
                        )
                    Pair(guardianId, applicationId)
                }
            }
        Audit.ApplicationCreate.log(
            targetId = AuditId(body.childId),
            objectId = AuditId(applicationId),
            meta = mapOf("guardianId" to guardianId, "applicationType" to body.type),
        )
        return applicationId
    }

    @PostMapping("/search")
    fun getApplicationSummaries(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @RequestBody body: SearchApplicationRequest,
    ): PagedApplicationSummaries {
        if (
            body.periodStart != null && body.periodEnd != null && body.periodStart > body.periodEnd
        ) {
            throw BadRequest(
                "Date parameter periodEnd ($body.periodEnd) cannot be before periodStart ($body.periodStart)"
            )
        }
        return db.connect { dbc ->
                dbc.read { tx ->
                    val canReadServiceWorkerNotes =
                        accessControl.hasPermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Global.READ_SERVICE_WORKER_APPLICATION_NOTES,
                        )

                    val readWithoutAssistanceNeed =
                        accessControl.getAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.Application.READ,
                        )
                    val readWithAssistanceNeed =
                        accessControl.getAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.Application.READ_IF_HAS_ASSISTANCE_NEED,
                        )

                    if (readWithoutAssistanceNeed == null && readWithAssistanceNeed == null) {
                        throw Forbidden()
                    }

                    tx.fetchApplicationSummaries(
                        today = clock.today(),
                        params = body,
                        readWithoutAssistanceNeed = readWithoutAssistanceNeed,
                        readWithAssistanceNeed = readWithAssistanceNeed,
                        canReadServiceWorkerNotes = canReadServiceWorkerNotes,
                    )
                }
            }
            .also { Audit.ApplicationSearch.log(meta = mapOf("total" to it.total)) }
    }

    @GetMapping("/by-guardian/{guardianId}")
    fun getGuardianApplicationSummaries(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable guardianId: PersonId,
    ): List<PersonApplicationSummary> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Person.READ_APPLICATIONS,
                        guardianId,
                    )
                    it.fetchApplicationSummariesForGuardian(guardianId)
                }
            }
            .also { Audit.ApplicationRead.log(targetId = AuditId(guardianId)) }
    }

    @GetMapping("/by-child/{childId}")
    fun getChildApplicationSummaries(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): List<PersonApplicationSummary> {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Child.READ_APPLICATION,
                        childId,
                    )
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            it,
                            user,
                            clock,
                            Action.Application.READ,
                        )
                    it.fetchApplicationSummariesForChild(childId, filter)
                }
            }
            .also { Audit.ApplicationRead.log(targetId = AuditId(childId)) }
    }

    @GetMapping("/{applicationId}")
    fun getApplicationDetails(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ): ApplicationResponse {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    val application =
                        tx.fetchApplicationDetails(applicationId)
                            ?: throw NotFound("Application $applicationId was not found")

                    val action =
                        when {
                            application.form.child.assistanceNeeded ->
                                Action.Application.READ_IF_HAS_ASSISTANCE_NEED
                            else -> Action.Application.READ
                        }
                    accessControl.requirePermissionFor(tx, user, clock, action, applicationId)

                    val decisionFilter =
                        accessControl.requireAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.Decision.READ,
                        )

                    val decisions = tx.getDecisionsByApplication(applicationId, decisionFilter)
                    val guardians =
                        personService.getGuardians(tx, user, application.childId).map { personDTO ->
                            PersonJSON.from(personDTO)
                        }

                    val attachments =
                        tx.getApplicationAttachments(applicationId).let { allAttachments ->
                            val permissions =
                                accessControl.checkPermissionFor(
                                    tx,
                                    user,
                                    clock,
                                    Action.Attachment.READ_APPLICATION_ATTACHMENT,
                                    allAttachments.map { it.id },
                                )
                            allAttachments.filter { attachment ->
                                permissions[attachment.id]?.isPermitted() ?: false
                            }
                        }

                    val permittedActions =
                        accessControl.getPermittedActions<ApplicationId, Action.Application>(
                            tx,
                            user,
                            clock,
                            applicationId,
                        )

                    ApplicationResponse(
                        application = application.copy(attachments = attachments),
                        decisions = decisions,
                        guardians = guardians,
                        attachments = attachments,
                        permittedActions = permittedActions,
                    )
                }
            }
            .also {
                Audit.ApplicationRead.log(targetId = AuditId(applicationId))
                Audit.DecisionReadByApplication.log(
                    targetId = AuditId(applicationId),
                    meta = mapOf("count" to it.decisions.size),
                )
            }
    }

    @PutMapping("/{applicationId}")
    fun updateApplication(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody application: ApplicationUpdate,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Application.UPDATE,
                    applicationId,
                )
                applicationStateService.updateApplicationContentsServiceWorker(
                    it,
                    user,
                    clock.now(),
                    applicationId,
                    application,
                    user.evakaUserId,
                )
            }
        }
        Audit.ApplicationUpdate.log(targetId = AuditId(applicationId))
    }

    @PostMapping("/{applicationId}/actions/send-application")
    fun sendApplication(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.sendApplication(it, user, clock, applicationId)
            }
        }
    }

    @GetMapping("/{applicationId}/placement-draft")
    fun getPlacementPlanDraft(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ): PlacementPlanDraft {
        return db.connect { dbc ->
                dbc.read {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Application.READ_PLACEMENT_PLAN_DRAFT,
                        applicationId,
                    )
                    placementPlanService.getPlacementPlanDraft(
                        it,
                        applicationId,
                        minStartDate = clock.today(),
                    )
                }
            }
            .also { Audit.PlacementPlanDraftRead.log(targetId = AuditId(applicationId)) }
    }

    @GetMapping("/{applicationId}/decision-drafts")
    fun getDecisionDrafts(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ): DecisionDraftGroup {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Application.READ_DECISION_DRAFT,
                        applicationId,
                    )

                    val application =
                        tx.fetchApplicationDetails(applicationId)
                            ?: throw NotFound("Application $applicationId not found")

                    if (application.status != ApplicationStatus.WAITING_DECISION) {
                        throw Conflict(
                            "Cannot get decision drafts for application with status ${application.status}"
                        )
                    }

                    val placementUnitName = tx.getPlacementPlanUnitName(applicationId)

                    val decisionDrafts = tx.fetchDecisionDrafts(applicationId)
                    val unit = getDecisionUnit(tx, decisionDrafts[0].unitId)

                    val applicationGuardian =
                        tx.getPersonById(application.guardianId)
                            ?: throw NotFound("Guardian ${application.guardianId} not found")
                    val child =
                        tx.getPersonById(application.childId)
                            ?: throw NotFound("Child ${application.childId} not found")
                    val vtjGuardians = personService.getGuardians(tx, user, child.id)

                    val applicationGuardianIsVtjGuardian: Boolean =
                        vtjGuardians.any { it.id == application.guardianId }
                    val otherGuardian =
                        tx.getApplicationOtherGuardians(applicationId).firstOrNull()?.let {
                            tx.getPersonById(it)
                        }

                    DecisionDraftGroup(
                        decisions = decisionDrafts,
                        placementUnitName = placementUnitName,
                        unit = unit,
                        guardian =
                            GuardianInfo(
                                firstName = applicationGuardian.firstName,
                                lastName = applicationGuardian.lastName,
                                ssn =
                                    (applicationGuardian.identity as? ExternalIdentifier.SSN)
                                        ?.toString(),
                                isVtjGuardian = applicationGuardianIsVtjGuardian,
                            ),
                        child =
                            ChildInfo(
                                firstName = child.firstName,
                                lastName = child.lastName,
                                ssn = (child.identity as? ExternalIdentifier.SSN)?.toString(),
                            ),
                        otherGuardian =
                            otherGuardian?.let {
                                GuardianInfo(
                                    id = it.id,
                                    firstName = it.firstName,
                                    lastName = it.lastName,
                                    ssn = (it.identity as? ExternalIdentifier.SSN)?.toString(),
                                    isVtjGuardian = true,
                                )
                            },
                    )
                }
            }
            .also {
                Audit.DecisionDraftRead.log(
                    targetId = AuditId(applicationId),
                    meta = mapOf("count" to it.decisions.size),
                )
            }
    }

    @PutMapping("/{applicationId}/decision-drafts")
    fun updateDecisionDrafts(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: List<DecisionDraftUpdate>,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                accessControl.requirePermissionFor(
                    it,
                    user,
                    clock,
                    Action.Application.UPDATE_DECISION_DRAFT,
                    applicationId,
                )
                updateDecisionDrafts(it, applicationId, body)
            }
        }
        Audit.DecisionDraftUpdate.log(
            targetId = AuditId(applicationId),
            objectId = AuditId(body.map { it.id }),
        )
    }

    @PostMapping("/placement-proposals/{unitId}/accept")
    fun acceptPlacementProposal(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.confirmPlacementProposalChanges(it, user, clock, unitId)
            }
        }
    }

    @PostMapping("/batch/actions/{action}")
    fun simpleBatchAction(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable action: String,
        @RequestBody body: SimpleBatchRequest,
    ) {
        val simpleBatchActions =
            mapOf(
                "move-to-waiting-placement" to applicationStateService::moveToWaitingPlacement,
                "return-to-sent" to applicationStateService::returnToSent,
                "cancel-placement-plan" to applicationStateService::cancelPlacementPlan,
                "send-decisions-without-proposal" to
                    applicationStateService::sendDecisionsWithoutProposal,
                "send-placement-proposal" to applicationStateService::sendPlacementProposal,
                "withdraw-placement-proposal" to applicationStateService::withdrawPlacementProposal,
                "confirm-decision-mailed" to applicationStateService::confirmDecisionMailed,
            )

        val actionFn = simpleBatchActions[action] ?: throw NotFound("Batch action not recognized")
        db.connect { dbc ->
            dbc.transaction { tx ->
                body.applicationIds.forEach { applicationId ->
                    actionFn.invoke(tx, user, clock, applicationId)
                }
            }
        }
    }

    @PostMapping("/{applicationId}/actions/create-placement-plan")
    fun createPlacementPlan(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: DaycarePlacementPlan,
    ) {
        val placementPlanId =
            db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Application.CREATE_PLACEMENT_PLAN,
                        applicationId,
                    )
                    applicationStateService.createPlacementPlan(
                        it,
                        user,
                        clock,
                        applicationId,
                        body,
                    )
                }
            }
        Audit.PlacementPlanCreate.log(
            targetId = AuditId(listOf(applicationId, body.unitId)),
            objectId = AuditId(placementPlanId),
        )
    }

    @PostMapping("/{applicationId}/actions/respond-to-placement-proposal")
    fun respondToPlacementProposal(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: PlacementProposalConfirmationUpdate,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.respondToPlacementProposal(
                    it,
                    user,
                    clock,
                    applicationId,
                    body.status,
                    body.reason,
                    body.otherReason,
                )
            }
        }
    }

    @PostMapping("/{applicationId}/actions/accept-decision")
    fun acceptDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: AcceptDecisionRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.acceptDecision(
                    it,
                    user,
                    clock,
                    applicationId,
                    body.decisionId,
                    body.requestedStartDate,
                )
            }
        }
    }

    @PostMapping("/{applicationId}/actions/reject-decision")
    fun rejectDecision(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: RejectDecisionRequest,
    ) {
        db.connect { dbc ->
            dbc.transaction {
                applicationStateService.rejectDecision(
                    it,
                    user,
                    clock,
                    applicationId,
                    body.decisionId,
                )
            }
        }
    }

    @PostMapping("/{applicationId}/actions/{action}")
    fun simpleApplicationAction(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @PathVariable action: String,
    ) {
        val simpleActions =
            mapOf(
                "move-to-waiting-placement" to applicationStateService::moveToWaitingPlacement,
                "return-to-sent" to applicationStateService::returnToSent,
                "cancel-application" to applicationStateService::cancelApplication,
                "set-verified" to applicationStateService::setVerified,
                "set-unverified" to applicationStateService::setUnverified,
                "cancel-placement-plan" to applicationStateService::cancelPlacementPlan,
                "send-decisions-without-proposal" to
                    applicationStateService::sendDecisionsWithoutProposal,
                "send-placement-proposal" to applicationStateService::sendPlacementProposal,
                "withdraw-placement-proposal" to applicationStateService::withdrawPlacementProposal,
                "confirm-decision-mailed" to applicationStateService::confirmDecisionMailed,
            )

        val actionFn = simpleActions[action] ?: throw NotFound("Action not recognized")
        db.connect { dbc -> dbc.transaction { actionFn.invoke(it, user, clock, applicationId) } }
    }

    @GetMapping("/units/{unitId}")
    fun getUnitApplications(
        db: Database,
        user: AuthenticatedUser.Employee,
        clock: EvakaClock,
        @PathVariable unitId: DaycareId,
    ): UnitApplications {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.hasPermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Unit.READ_APPLICATIONS_AND_PLACEMENT_PLANS,
                        unitId,
                    )
                    val placementProposals =
                        tx.getPlacementPlans(
                            clock.today(),
                            unitId,
                            null,
                            null,
                            listOf(ApplicationStatus.WAITING_UNIT_CONFIRMATION),
                        )
                    val placementPlans =
                        tx.getPlacementPlans(
                            clock.today(),
                            unitId,
                            null,
                            null,
                            listOf(
                                ApplicationStatus.WAITING_CONFIRMATION,
                                ApplicationStatus.WAITING_MAILING,
                                ApplicationStatus.REJECTED,
                            ),
                        )
                    val applications = tx.getApplicationUnitSummaries(unitId)
                    UnitApplications(
                        placementProposals = placementProposals,
                        placementPlans = placementPlans,
                        applications = applications,
                    )
                }
            }
            .also { Audit.UnitApplicationsRead.log(targetId = AuditId(unitId)) }
    }
}

data class PaperApplicationCreateRequest(
    val childId: ChildId,
    val guardianId: PersonId? = null,
    val guardianToBeCreated: CreatePersonBody?,
    val guardianSsn: String? = null,
    val type: ApplicationType,
    val sentDate: LocalDate,
    val hideFromGuardian: Boolean,
    val transferApplication: Boolean,
)

data class SearchApplicationRequest(
    val page: Int?,
    val sortBy: ApplicationSortColumn?,
    val sortDir: ApplicationSortDirection?,
    val areas: List<AreaId>?,
    val units: List<DaycareId>?,
    val basis: List<ApplicationBasis>?,
    val type: ApplicationTypeToggle,
    val preschoolType: List<ApplicationPreschoolTypeToggle>?,
    val statuses: List<ApplicationStatusOption>?,
    val dateType: List<ApplicationDateType>?,
    val distinctions: List<ApplicationDistinctions>?,
    val periodStart: LocalDate?,
    val periodEnd: LocalDate?,
    val searchTerms: String?,
    val transferApplications: TransferApplicationFilter?,
    val voucherApplications: VoucherApplicationFilter?,
)

data class ApplicationResponse(
    val application: ApplicationDetails,
    val decisions: List<Decision>,
    val guardians: List<PersonJSON>,
    val attachments: List<ApplicationAttachment>,
    val permittedActions: Set<Action.Application>,
)

data class SimpleBatchRequest(val applicationIds: Set<ApplicationId>)

data class PlacementProposalConfirmationUpdate(
    val status: PlacementPlanConfirmationStatus,
    val reason: PlacementPlanRejectReason?,
    val otherReason: String?,
)

data class DaycarePlacementPlan(
    val unitId: DaycareId,
    val period: FiniteDateRange,
    val preschoolDaycarePeriod: FiniteDateRange? = null,
)

data class AcceptDecisionRequest(val decisionId: DecisionId, val requestedStartDate: LocalDate)

data class RejectDecisionRequest(val decisionId: DecisionId)

data class DecisionDraftGroup(
    val decisions: List<DecisionDraft>,
    val placementUnitName: String,
    val unit: DecisionUnit,
    val guardian: GuardianInfo,
    val otherGuardian: GuardianInfo?,
    val child: ChildInfo,
)

data class ChildInfo(val ssn: String?, val firstName: String, val lastName: String)

data class GuardianInfo(
    val id: PersonId? = null,
    val ssn: String?,
    val firstName: String,
    val lastName: String,
    val isVtjGuardian: Boolean,
)

data class UnitApplications(
    val placementProposals: List<PlacementPlanDetails>,
    val placementPlans: List<PlacementPlanDetails>,
    val applications: List<ApplicationUnitSummary>,
)

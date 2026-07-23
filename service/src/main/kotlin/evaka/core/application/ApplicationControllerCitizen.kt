// SPDX-FileCopyrightText: 2018-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.application

import evaka.core.Audit
import evaka.core.AuditContext
import evaka.core.AuditId
import evaka.core.children.getCitizenChildIds
import evaka.core.decision.Decision
import evaka.core.decision.DecisionService
import evaka.core.decision.DecisionStatus
import evaka.core.decision.DecisionType
import evaka.core.decision.getDecisionsByGuardian
import evaka.core.decision.getOwnDecisions
import evaka.core.decision.getSentDecision
import evaka.core.invoicing.data.getFeeDecisionByLiableCitizen
import evaka.core.invoicing.data.getVoucherValueDecisionByLiableCitizen
import evaka.core.invoicing.domain.FinanceDecisionType
import evaka.core.invoicing.service.FeeDecisionService
import evaka.core.invoicing.service.VoucherValueDecisionService
import evaka.core.pis.getPersonById
import evaka.core.pis.getPersonNameDetailsById
import evaka.core.pis.isDuplicate
import evaka.core.pis.service.PersonService
import evaka.core.shared.ApplicationId
import evaka.core.shared.ChildId
import evaka.core.shared.DecisionId
import evaka.core.shared.FeatureConfig
import evaka.core.shared.FeeDecisionId
import evaka.core.shared.PersonId
import evaka.core.shared.VoucherValueDecisionId
import evaka.core.shared.auth.AuthenticatedUser
import evaka.core.shared.db.Database
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.NotFound
import evaka.core.shared.security.AccessControl
import evaka.core.shared.security.Action
import java.time.LocalDate
import java.util.UUID
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/citizen")
class ApplicationControllerCitizen(
    private val accessControl: AccessControl,
    private val featureConfig: FeatureConfig,
    private val applicationStateService: ApplicationStateService,
    private val decisionService: DecisionService,
    private val personService: PersonService,
    private val feeDecisionService: FeeDecisionService,
    private val voucherValueDecisionService: VoucherValueDecisionService,
) {

    @GetMapping("/applications/by-guardian")
    fun getGuardianApplications(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<ApplicationsOfChild> {
        val audit = AuditContext()
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_APPLICATIONS,
                        user.id,
                    )
                    val allApplications =
                        tx.fetchApplicationSummariesForCitizen(user.id, clock.today())
                    val allPermittedActions: Map<ApplicationId, Set<Action.Citizen.Application>> =
                        accessControl.getPermittedActions(
                            tx,
                            user,
                            clock,
                            allApplications.map { it.applicationId },
                        )
                    val allDecidableApplications =
                        getDecidableApplications(
                            tx,
                            user,
                            clock,
                            allApplications.map { it.applicationId }.toSet(),
                        )
                    val existingApplicationsByChild = allApplications.groupBy { it.childId }

                    // Some children might not have applications, so add 0 application children
                    tx.getCitizenChildren(clock.today(), user.id).map { child ->
                        val applications = existingApplicationsByChild[child.id] ?: emptyList()
                        val permittedActions = applications.associate { application ->
                            application.applicationId to
                                (allPermittedActions[application.applicationId] ?: emptySet())
                        }
                        ApplicationsOfChild(
                            childId = child.id,
                            childName = "${child.firstName} ${child.lastName}",
                            applicationSummaries = applications,
                            permittedActions = permittedActions,
                            duplicateOf = child.duplicateOf,
                            decidableApplications =
                                applications
                                    .mapNotNull { application ->
                                        application.applicationId.takeIf {
                                            allDecidableApplications.contains(it)
                                        }
                                    }
                                    .toSet(),
                        )
                    }
                }
            }
            .also { applications ->
                audit
                    .add(applications.map { it.childId })
                    .add(
                        applications.flatMap { a ->
                            a.applicationSummaries.map { it.applicationId }
                        }
                    )
                    .observeDate(
                        applications
                            .flatMap { a -> a.applicationSummaries.mapNotNull { it.startDate } }
                            .minOrNull()
                    )
                audit.log(Audit.ApplicationRead, clock)
            }
    }

    @GetMapping("/applications/children")
    fun getApplicationChildren(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<CitizenChildren> {
        val audit = AuditContext()
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_APPLICATION_CHILDREN,
                        user.id,
                    )
                    tx.getCitizenChildren(clock.today(), user.id)
                }
            }
            .also { children ->
                audit.add(children.map { it.id })
                audit.log(Audit.ApplicationRead, clock)
            }
    }

    @GetMapping("/applications/{applicationId}")
    fun getApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ): ApplicationDetails {
        val audit = AuditContext().add(applicationId)
        val application = db.connect { dbc ->
            dbc.transaction { tx ->
                accessControl.requirePermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Citizen.Application.READ,
                    applicationId,
                )

                val attachmentFilter =
                    accessControl.getAuthorizationFilter(
                        tx,
                        user,
                        clock,
                        Action.Attachment.READ_APPLICATION_ATTACHMENT,
                    )
                tx.fetchApplicationDetails(applicationId, attachmentFilter)?.let { application ->
                    val otherGuardian =
                        personService.getOtherGuardian(
                            tx,
                            user,
                            clock.now(),
                            application.guardianId,
                            application.childId,
                        )
                    audit
                        .add(application.childId)
                        .add(application.guardianId)
                        .add(listOfNotNull(otherGuardian?.id))
                        .add(application.form.preferences.preferredUnits.map { it.id })
                        .add(application.attachments.map { it.id })
                        .observeDate(application.form.preferences.preferredStartDate)
                    application.copy(
                        hasOtherGuardian = otherGuardian != null,
                        otherGuardianLivesInSameAddress =
                            otherGuardian?.id?.let { otherGuardianId ->
                                personService.personsLiveInTheSameAddress(
                                    tx,
                                    application.guardianId,
                                    otherGuardianId,
                                )
                            },
                        // hide modification info from citizen
                        modifiedAt = null,
                        modifiedBy = null,
                    )
                }
            }
        }

        return if (application?.hideFromGuardian == false) {
                if (user.id == application.guardianId) {
                    application
                } else {
                    hideCriticalApplicationInfoFromOtherGuardian(application)
                }
            } else {
                throw NotFound("Application not found")
            }
            .also { audit.log(Audit.ApplicationRead, clock) }
    }

    @PostMapping("/applications")
    fun createApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @RequestBody body: CreateApplicationBody,
    ): ApplicationId {
        val audit = AuditContext().add(body.childId).addMeta("applicationType", body.type)
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.CREATE_APPLICATION,
                        body.childId,
                    )
                    if (
                        body.type != ApplicationType.CLUB &&
                            tx.duplicateApplicationExists(
                                guardianId = user.id,
                                childId = body.childId,
                                type = body.type,
                            )
                    ) {
                        throw BadRequest("Duplicate application")
                    }

                    val guardian =
                        tx.getPersonById(user.id)
                            ?: throw IllegalStateException("Guardian not found")

                    val child =
                        tx.getPersonById(body.childId)
                            ?: throw IllegalStateException("Child not found")

                    if (tx.isDuplicate(child.id)) {
                        throw IllegalStateException("Child is duplicate")
                    }

                    applicationStateService.createApplication(
                        tx = tx,
                        user = user,
                        now = clock.now(),
                        origin = ApplicationOrigin.ELECTRONIC,
                        type = body.type,
                        guardian = guardian,
                        child = child,
                    )
                }
            }
            .also { applicationId ->
                audit.add(applicationId)
                audit.log(Audit.ApplicationCreate, clock)
            }
    }

    @GetMapping("/applications/duplicates/{childId}")
    fun getChildDuplicateApplications(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): Map<ApplicationType, Boolean> {
        val audit = AuditContext().add(childId)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_DUPLICATE_APPLICATIONS,
                        childId,
                    )
                    ApplicationType.entries.associateWith { type ->
                        (type != ApplicationType.CLUB &&
                            tx.duplicateApplicationExists(
                                guardianId = user.id,
                                childId = childId,
                                type = type,
                            ))
                    }
                }
            }
            .also { audit.log(Audit.ApplicationReadDuplicates, clock) }
    }

    @GetMapping("/applications/active-placements/{childId}")
    fun getChildPlacementStatusByApplicationType(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable childId: ChildId,
    ): Map<ApplicationType, Boolean> {
        val audit = AuditContext().add(childId)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Child.READ_PLACEMENT_STATUS_BY_APPLICATION_TYPE,
                        childId,
                    )
                    ApplicationType.entries.associateWith { type ->
                        tx.activePlacementExists(
                            childId = childId,
                            type = type,
                            today = clock.today(),
                        )
                    }
                }
            }
            .also { audit.log(Audit.ApplicationReadActivePlacementsByType, clock) }
    }

    @PutMapping("/applications/{applicationId}")
    fun updateApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody update: CitizenApplicationUpdate,
    ) {
        db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.Application.UPDATE,
                        applicationId,
                    )
                    applicationStateService.updateOwnApplicationContentsCitizen(
                        it,
                        user,
                        clock.now(),
                        applicationId,
                        update,
                    )
                }
            }
            .also { applicationDetails ->
                Audit.ApplicationUpdate.log(
                    targetId = AuditId(applicationId),
                    objectId = AuditId(applicationDetails.childId),
                )
            }
    }

    @PutMapping("/applications/{applicationId}/draft")
    fun saveApplicationAsDraft(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody applicationForm: ApplicationFormUpdate,
    ) {
        db.connect { dbc ->
                dbc.transaction {
                    accessControl.requirePermissionFor(
                        it,
                        user,
                        clock,
                        Action.Citizen.Application.UPDATE,
                        applicationId,
                    )
                    applicationStateService.updateOwnApplicationContentsCitizen(
                        it,
                        user,
                        clock.now(),
                        applicationId,
                        CitizenApplicationUpdate(applicationForm, allowOtherGuardianAccess = false),
                        asDraft = true,
                    )
                }
            }
            .also { applicationDetails ->
                Audit.ApplicationUpdate.log(
                    targetId = AuditId(applicationId),
                    objectId = AuditId(applicationDetails.childId),
                )
            }
    }

    @DeleteMapping("/applications/{applicationId}")
    fun deleteOrCancelUnprocessedApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ) {
        val audit = AuditContext().add(applicationId)
        db.connect { dbc ->
                dbc.transaction { tx ->
                    val application =
                        tx.fetchApplicationDetails(applicationId)
                            ?: throw NotFound(
                                "Application $applicationId of guardian ${user.id} not found"
                            )
                    audit.add(application.childId).add(application.guardianId)

                    when (application.status) {
                        ApplicationStatus.CREATED -> {
                            accessControl.requirePermissionFor(
                                tx,
                                user,
                                clock,
                                Action.Citizen.Application.DELETE,
                                applicationId,
                            )
                            audit
                                .addMeta("type", application.type)
                                .addMeta(
                                    "preferredStartDate",
                                    application.form.preferences.preferredStartDate,
                                )
                            tx.deleteApplication(applicationId)
                            Audit.ApplicationDelete
                        }

                        ApplicationStatus.SENT -> {
                            applicationStateService.cancelApplication(
                                tx,
                                user,
                                clock,
                                audit,
                                applicationId,
                                null,
                            )
                            Audit.ApplicationCancel
                        }

                        else -> {
                            throw BadRequest(
                                "Only applications which are not yet being processed can be cancelled"
                            )
                        }
                    }
                }
            }
            .also { audit.log(it, clock) }
    }

    @PostMapping("/applications/{applicationId}/actions/send-application")
    fun sendApplication(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
    ) {
        val audit = AuditContext().add(applicationId)
        db.connect { dbc ->
                dbc.transaction {
                    applicationStateService.sendApplication(it, user, clock, audit, applicationId)
                }
            }
            .also { audit.log(Audit.ApplicationSend, clock) }
    }

    @GetMapping("/decisions")
    fun getDecisions(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): ApplicationDecisions {
        val audit = AuditContext()
        return db.connect { dbc ->
                dbc.read { tx ->
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.Citizen.Decision.READ,
                        )
                    val children = tx.getCitizenChildIds(clock.today(), user.id)
                    val decisions = tx.getOwnDecisions(user.id, children, filter)
                    audit
                        .add(decisions.map { it.id })
                        .add(decisions.map { it.childId })
                        .add(decisions.map { it.applicationId })
                        .observeDate(decisions.minOfOrNull { it.sentDate })
                    ApplicationDecisions(
                        decisions = decisions,
                        permittedActions =
                            accessControl.getPermittedActions(
                                tx,
                                user,
                                clock,
                                decisions.map { it.id },
                            ),
                        decidableApplications =
                            getDecidableApplications(
                                tx,
                                user,
                                clock,
                                decisions.map { it.applicationId }.toSet(),
                            ),
                    )
                }
            }
            .also { audit.log(Audit.DecisionRead, clock) }
    }

    data class DecisionWithValidStartDatePeriod(
        val decision: Decision,
        val validRequestedStartDatePeriod: FiniteDateRange,
        val permittedActions: Set<Action.Citizen.Decision>,
    )

    @GetMapping("/decisions/pending")
    fun getPendingDecisions(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<DecisionWithValidStartDatePeriod> {
        val audit = AuditContext().addMeta("pendingOnly", true)
        return db.connect { dbc ->
                dbc.read { tx ->
                    val filter =
                        accessControl.requireAuthorizationFilter(
                            tx,
                            user,
                            clock,
                            Action.Citizen.Decision.READ,
                        )
                    val childIds = tx.getCitizenChildIds(clock.today(), user.id)
                    val pendingDecisions =
                        tx.getDecisionsByGuardian(user.id, filter).filter {
                            it.status == DecisionStatus.PENDING && it.childId in childIds
                        }
                    val decidableApplications =
                        getDecidableApplications(
                            tx,
                            user,
                            clock,
                            pendingDecisions.map { it.applicationId }.toSet(),
                        )
                    val actionableDecisions = pendingDecisions.filter {
                        it.applicationId in decidableApplications
                    }
                    val permittedActions =
                        accessControl.getPermittedActions<DecisionId, Action.Citizen.Decision>(
                            tx,
                            user,
                            clock,
                            actionableDecisions.map { it.id },
                        )
                    audit
                        .add(actionableDecisions.map { it.id })
                        .add(actionableDecisions.map { it.childId })
                        .add(actionableDecisions.map { it.applicationId })
                        .add(actionableDecisions.map { it.unit.id })
                        .observeDate(actionableDecisions.minOfOrNull { it.startDate })
                    actionableDecisions.map { decision ->
                        DecisionWithValidStartDatePeriod(
                            decision,
                            decision.validRequestedStartDatePeriod(featureConfig, isCitizen = true),
                            permittedActions[decision.id] ?: emptySet(),
                        )
                    }
                }
            }
            .also { audit.log(Audit.DecisionRead, clock) }
    }

    @PostMapping("/applications/{applicationId}/actions/accept-decision")
    fun acceptDecision(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: AcceptDecisionRequest,
    ) {
        // note: applicationStateService handles authorization
        val audit = AuditContext().add(applicationId).add(body.decisionId)
        db.connect { dbc ->
                dbc.transaction {
                    applicationStateService.acceptDecision(
                        it,
                        user,
                        clock,
                        audit,
                        applicationId,
                        body.decisionId,
                        body.requestedStartDate,
                    )
                }
            }
            .also { audit.log(Audit.DecisionAccept, clock) }
    }

    @PostMapping("/applications/{applicationId}/actions/reject-decision")
    fun rejectDecision(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: RejectDecisionRequest,
    ) {
        // note: applicationStateService handles authorization
        val audit = AuditContext().add(applicationId).add(body.decisionId)
        db.connect { dbc ->
                dbc.transaction {
                    applicationStateService.rejectDecision(
                        it,
                        user,
                        clock,
                        audit,
                        applicationId,
                        body.decisionId,
                    )
                }
            }
            .also { audit.log(Audit.DecisionReject, clock) }
    }

    @GetMapping("/decisions/{id}/download", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadDecisionPdf(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: DecisionId,
    ): ResponseEntity<Any> {
        val audit = AuditContext().add(id)
        return db.connect { dbc ->
                val decision =
                    dbc.transaction { tx ->
                        accessControl.requirePermissionFor(
                            tx,
                            user,
                            clock,
                            Action.Citizen.Decision.DOWNLOAD_PDF,
                            id,
                        )
                        tx.getSentDecision(id)?.also { decision ->
                            audit
                                .add(decision.applicationId)
                                .add(decision.childId)
                                .add(decision.unit.id)
                                .observeDate(decision.startDate)
                            tx.fetchApplicationDetails(decision.applicationId)?.also { application
                                ->
                                audit.add(application.guardianId)
                            }
                            tx.getApplicationOtherGuardians(decision.applicationId).also {
                                audit.add(it)
                            }
                        }
                    } ?: throw NotFound("Decision $id does not exist")
                decisionService.getDecisionPdf(dbc, decision)
            }
            .also { audit.log(Audit.DecisionDownloadPdf, clock) }
    }

    @GetMapping("/decisions/{id}")
    fun getDecisionDetails(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: DecisionId,
    ): CitizenDecisionDetails {
        val audit = AuditContext().add(id)
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Decision.READ,
                        id,
                    )
                    val decision =
                        tx.getSentDecision(id) ?: throw NotFound("Decision $id does not exist")
                    audit
                        .add(decision.applicationId)
                        .add(decision.childId)
                        .add(decision.unit.id)
                        .observeDate(decision.startDate)
                    decision.toCitizenDecisionDetails()
                }
            }
            .also { audit.log(Audit.DecisionRead, clock) }
    }

    @GetMapping("/applications/by-guardian/notifications")
    fun getGuardianApplicationNotifications(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): Int {
        val audit = AuditContext()
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_APPLICATION_NOTIFICATIONS,
                        user.id,
                    )
                    tx.fetchApplicationNotificationCountForCitizen(user.id, clock.today())
                }
            }
            .also { count ->
                audit.addMeta("count", count)
                audit.log(Audit.ApplicationReadNotifications, clock)
            }
    }

    @GetMapping("/finance-decisions/by-liable-citizen")
    fun getLiableCitizenFinanceDecisions(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
    ): List<FinanceDecisionCitizenInfo> {
        return db.connect { dbc ->
                dbc.read { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.Person.READ_FINANCE_DECISIONS,
                        user.id,
                    )
                    val voucherValueDecisionRows =
                        tx.getVoucherValueDecisionByLiableCitizen(user.id)
                    val feeDecisionRows = tx.getFeeDecisionByLiableCitizen(user.id)

                    val citizenIds =
                        feeDecisionRows
                            .map { listOfNotNull(it.headOfFamilyId, it.partnerId) }
                            .flatten()
                            .toSet() +
                            voucherValueDecisionRows
                                .map { listOfNotNull(it.headOfFamilyId, it.partnerId) }
                                .flatten()
                                .toSet()

                    val childIds = voucherValueDecisionRows.map { it.childId }.toSet()
                    val personMap =
                        tx.getPersonNameDetailsById(citizenIds + childIds).associateBy { it.id }
                    val voucherValueDecisionInfos = voucherValueDecisionRows.map { row ->
                        val childInfo =
                            personMap[row.childId]
                                ?: throw IllegalStateException("Voucher value child not found")
                        FinanceDecisionCitizenInfo(
                            id = row.id.raw,
                            type = FinanceDecisionType.VOUCHER_VALUE_DECISION,
                            decisionChildren =
                                listOf(
                                    FinanceDecisionChildInfo(
                                        childInfo.id,
                                        childInfo.firstName,
                                        childInfo.lastName,
                                    )
                                ),
                            validFrom = row.validFrom,
                            validTo = row.validTo,
                            sentAt = row.sentAt,
                            coDebtors =
                                listOfNotNull(
                                        personMap[row.headOfFamilyId],
                                        personMap[row.partnerId],
                                    )
                                    .map {
                                        LiableCitizenInfo(
                                            id = it.id,
                                            firstName = it.firstName,
                                            lastName = it.lastName,
                                        )
                                    },
                        )
                    }
                    val feeDecisionInfos = feeDecisionRows.map { row ->
                        FinanceDecisionCitizenInfo(
                            id = row.id.raw,
                            decisionChildren = emptyList(),
                            type = FinanceDecisionType.FEE_DECISION,
                            validFrom = row.validDuring.start,
                            validTo = row.validDuring.end,
                            sentAt = row.sentAt,
                            coDebtors =
                                listOfNotNull(
                                        personMap[row.headOfFamilyId],
                                        personMap[row.partnerId],
                                    )
                                    .map {
                                        LiableCitizenInfo(
                                            id = it.id,
                                            firstName = it.firstName,
                                            lastName = it.lastName,
                                        )
                                    },
                        )
                    }
                    voucherValueDecisionInfos + feeDecisionInfos
                }
            }
            .also { financeDecisionCitizenInfoList ->
                val childIds = financeDecisionCitizenInfoList.flatMap { decision ->
                    decision.decisionChildren.map { child -> child.id }
                }
                Audit.FinanceDecisionCitizenRead.log(
                    targetId = AuditId(user.id),
                    objectId = AuditId(childIds),
                    meta = mapOf("count" to financeDecisionCitizenInfoList.size),
                )
            }
    }

    @GetMapping("/fee-decisions/{id}/download", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun downloadFeeDecisionPdf(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: FeeDecisionId,
    ): ResponseEntity<Any> {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.FeeDecision.DOWNLOAD,
                        id,
                    )
                }
                feeDecisionService.getFeeDecisionPdfResponse(dbc, id)
            }
            .also { Audit.CitizenFeeDecisionDownloadPdf.log(targetId = AuditId(id)) }
    }

    @GetMapping(
        "/voucher-value-decisions/{id}/download",
        produces = [MediaType.APPLICATION_PDF_VALUE],
    )
    fun downloadVoucherValueDecisionPdf(
        db: Database,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        @PathVariable id: VoucherValueDecisionId,
    ): ResponseEntity<Any> {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    accessControl.requirePermissionFor(
                        tx,
                        user,
                        clock,
                        Action.Citizen.VoucherValueDecision.DOWNLOAD,
                        id,
                    )
                }
                voucherValueDecisionService.getDecisionPdfResponse(dbc, id)
            }
            .also { Audit.CitizenVoucherValueDecisionDownloadPdf.log(targetId = AuditId(id)) }
    }

    private fun getDecidableApplications(
        tx: Database.Read,
        user: AuthenticatedUser.Citizen,
        clock: EvakaClock,
        applications: Set<ApplicationId>,
    ): Set<ApplicationId> {
        val canAccept =
            accessControl
                .checkPermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Application.ACCEPT_DECISION,
                    applications,
                )
                .filter { it.value.isPermitted() }
                .keys
        val canReject =
            accessControl
                .checkPermissionFor(
                    tx,
                    user,
                    clock,
                    Action.Application.REJECT_DECISION,
                    applications,
                )
                .filter { it.value.isPermitted() }
                .keys

        return tx.createQuery {
                sql(
                    """
            SELECT a.id
            FROM application a
            JOIN decision d ON d.application_id = a.id
            WHERE a.id = ANY(${bind(canAccept.intersect(canReject))}) AND d.status = 'PENDING'
        """
                )
            }
            .toSet()
    }
}

data class ApplicationsOfChild(
    val childId: ChildId,
    val childName: String,
    val applicationSummaries: List<CitizenApplicationSummary>,
    val permittedActions: Map<ApplicationId, Set<Action.Citizen.Application>>,
    val decidableApplications: Set<ApplicationId>,
    val duplicateOf: PersonId?,
)

data class CreateApplicationBody(val childId: ChildId, val type: ApplicationType)

data class ApplicationDecisions(
    val decisions: List<DecisionSummary>,
    val permittedActions: Map<DecisionId, Set<Action.Citizen.Decision>>,
    val decidableApplications: Set<ApplicationId>,
)

data class DecisionSummary(
    val id: DecisionId,
    val childId: ChildId,
    val applicationId: ApplicationId,
    val type: DecisionType,
    val status: DecisionStatus,
    val sentDate: LocalDate,
    val resolved: LocalDate?,
)

data class CitizenDecisionDetails(
    val unitName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val sentDate: LocalDate,
    val resolved: LocalDate?,
)

fun Decision.toCitizenDecisionDetails() =
    CitizenDecisionDetails(
        unitName =
            when (type) {
                DecisionType.DAYCARE,
                DecisionType.DAYCARE_PART_TIME ->
                    unit.daycareDecisionName.takeUnless { it.isBlank() }
                DecisionType.PRESCHOOL,
                DecisionType.PRESCHOOL_DAYCARE,
                DecisionType.PRESCHOOL_CLUB,
                DecisionType.PREPARATORY_EDUCATION ->
                    unit.preschoolDecisionName.takeUnless { it.isBlank() }
                else -> null
            } ?: unit.name,
        startDate = startDate,
        endDate = endDate,
        sentDate = sentDate!!,
        resolved = resolved,
    )

data class LiableCitizenInfo(val id: PersonId, val firstName: String, val lastName: String)

data class FinanceDecisionChildInfo(val id: PersonId, val firstName: String, val lastName: String)

data class FinanceDecisionCitizenInfo(
    val id: UUID,
    val type: FinanceDecisionType,
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val sentAt: HelsinkiDateTime,
    val coDebtors: List<LiableCitizenInfo>,
    val decisionChildren: List<FinanceDecisionChildInfo>,
)

private fun hideCriticalApplicationInfoFromOtherGuardian(
    application: ApplicationDetails
): ApplicationDetails =
    application.copy(
        form =
            application.form.copy(
                child =
                    application.form.child.copy(
                        person = application.form.child.person.copy(socialSecurityNumber = null),
                        address = null,
                        futureAddress = null,
                        assistanceDescription = "",
                    ),
                guardian =
                    application.form.guardian.copy(
                        person = application.form.guardian.person.copy(socialSecurityNumber = null),
                        address = null,
                        futureAddress = null,
                        phoneNumber = "",
                        email = "",
                    ),
                preferences =
                    application.form.preferences.copy(
                        siblingBasis =
                            application.form.preferences.siblingBasis?.copy(
                                siblingName = "",
                                siblingSsn = "",
                                siblingUnit = "",
                            )
                    ),
                otherPartner = null,
                otherChildren = emptyList(),
                otherInfo = "",
            )
    )

// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.EvakaEnv
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationForm
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStateService
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.assistance.DaycareAssistanceLevel
import fi.espoo.evaka.assistance.OtherAssistanceMeasureType
import fi.espoo.evaka.assistance.PreschoolAssistanceLevel
import fi.espoo.evaka.assistanceneed.decision.AssistanceLevel
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionEmployee
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionGuardian
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionLanguage
import fi.espoo.evaka.assistanceneed.decision.AssistanceNeedDecisionStatus
import fi.espoo.evaka.assistanceneed.decision.ServiceOptions
import fi.espoo.evaka.assistanceneed.decision.StructuralMotivationOptions
import fi.espoo.evaka.assistanceneed.decision.UnitInfo
import fi.espoo.evaka.assistanceneed.preschooldecision.AssistanceNeedPreschoolDecisionForm
import fi.espoo.evaka.attachment.AttachmentParent
import fi.espoo.evaka.attachment.insertAttachment
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.childdiscussion.ChildDiscussionData
import fi.espoo.evaka.childdiscussion.getChildDiscussions
import fi.espoo.evaka.children.consent.ChildConsentType
import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesType
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.ClubTerm
import fi.espoo.evaka.daycare.DaycareDecisionCustomization
import fi.espoo.evaka.daycare.MailingAddress
import fi.espoo.evaka.daycare.PreschoolTerm
import fi.espoo.evaka.daycare.UnitManager
import fi.espoo.evaka.daycare.VisitingAddress
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.insertClubTerm
import fi.espoo.evaka.daycare.insertPreschoolTerm
import fi.espoo.evaka.daycare.service.AbsenceCategory
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionStatus
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.getDecision
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.document.DocumentLanguage
import fi.espoo.evaka.document.DocumentTemplateContent
import fi.espoo.evaka.document.DocumentType
import fi.espoo.evaka.document.childdocument.DocumentContent
import fi.espoo.evaka.document.childdocument.DocumentStatus
import fi.espoo.evaka.emailclient.CalendarEventNotificationData
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.holidayperiod.FixedPeriodQuestionnaireBody
import fi.espoo.evaka.holidayperiod.HolidayPeriodBody
import fi.espoo.evaka.holidayperiod.createFixedPeriodQuestionnaire
import fi.espoo.evaka.holidayperiod.insertHolidayPeriod
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.incomestatement.IncomeStatementBody
import fi.espoo.evaka.incomestatement.createIncomeStatement
import fi.espoo.evaka.invoicing.data.markVoucherValueDecisionsSent
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.PaymentStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.invoicing.service.IncomeNotification
import fi.espoo.evaka.invoicing.service.IncomeNotificationType
import fi.espoo.evaka.invoicing.service.createIncomeNotification
import fi.espoo.evaka.messaging.MessageThreadStub
import fi.espoo.evaka.messaging.MessageType
import fi.espoo.evaka.messaging.createPersonMessageAccount
import fi.espoo.evaka.note.child.daily.ChildDailyNoteBody
import fi.espoo.evaka.note.child.daily.createChildDailyNote
import fi.espoo.evaka.note.child.sticky.ChildStickyNoteBody
import fi.espoo.evaka.note.child.sticky.createChildStickyNote
import fi.espoo.evaka.note.group.GroupNoteBody
import fi.espoo.evaka.note.group.createGroupNote
import fi.espoo.evaka.pairing.Pairing
import fi.espoo.evaka.pairing.PairingsController
import fi.espoo.evaka.pairing.challengePairing
import fi.espoo.evaka.pairing.incrementAttempts
import fi.espoo.evaka.pairing.initPairing
import fi.espoo.evaka.pairing.respondPairingChallengeCreateDevice
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.getEmployeeByExternalId
import fi.espoo.evaka.pis.getEmployees
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.placement.PlacementPlanService
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.DailyReservationRequest
import fi.espoo.evaka.reservations.createReservationsAndAbsences
import fi.espoo.evaka.s3.Document
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceFactorId
import fi.espoo.evaka.shared.AssistanceNeedDecisionId
import fi.espoo.evaka.shared.AssistanceNeedPreschoolDecisionId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.CalendarEventAttendeeId
import fi.espoo.evaka.shared.CalendarEventId
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildDocumentId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DailyServiceTimeNotificationId
import fi.espoo.evaka.shared.DailyServiceTimesId
import fi.espoo.evaka.shared.DaycareAssistanceId
import fi.espoo.evaka.shared.DaycareCaretakerId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.DocumentTemplateId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeThresholdsId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.MessageThreadId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.OtherAssistanceMeasureId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PaymentId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.PreschoolAssistanceId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.StaffAttendanceRealtimeId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.psqlCause
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.shared.security.upsertEmployeeUser
import fi.espoo.evaka.user.EvakaUser
import fi.espoo.evaka.user.EvakaUserType
import fi.espoo.evaka.vasu.CurriculumType
import fi.espoo.evaka.vasu.VasuLanguage
import fi.espoo.evaka.vasu.getDefaultTemplateContent
import fi.espoo.evaka.vasu.getVasuTemplate
import fi.espoo.evaka.vasu.insertVasuDocument
import fi.espoo.evaka.vasu.insertVasuTemplate
import fi.espoo.evaka.vasu.publishVasuDocument
import fi.espoo.evaka.vasu.revokeVasuGuardianHasGivenPermissionToShare
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import mu.KotlinLogging
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.jdbi.v3.json.Json
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

private val fakeAdmin =
    AuthenticatedUser.Employee(
        id = EmployeeId(UUID.fromString("00000000-0000-0000-0000-000000000001")),
        roles = setOf(UserRole.ADMIN)
    )

private val logger = KotlinLogging.logger {}

@Profile("enable_dev_api")
@RestController
@RequestMapping("/dev-api")
class DevApi(
    private val personService: PersonService,
    private val asyncJobRunners: List<AsyncJobRunner<*>>,
    private val placementPlanService: PlacementPlanService,
    private val applicationStateService: ApplicationStateService,
    private val decisionService: DecisionService,
    private val documentClient: DocumentService,
    private val env: EvakaEnv,
    private val bucketEnv: BucketEnv,
    private val emailMessageProvider: IEmailMessageProvider
) {
    private val filesBucket = bucketEnv.attachments
    private val digitransit = MockDigitransit()

    private fun runAllAsyncJobs(clock: EvakaClock) {
        asyncJobRunners.forEach {
            it.runPendingJobsSync(clock)
            it.waitUntilNoRunningJobs(timeout = Duration.ofSeconds(20))
        }
    }

    @GetMapping("/")
    fun healthCheck() {
        // HTTP 200
    }

    @PostMapping("/test-mode")
    fun setTestMode(db: Database, @RequestParam enabled: Boolean) {
        asyncJobRunners.forEach {
            if (enabled) {
                it.stopBackgroundPolling()
                it.disableAfterCommitHooks()
            } else if (!env.asyncJobRunnerDisabled) {
                it.startBackgroundPolling()
                it.enableAfterCommitHooks()
            }
        }
    }

    @PostMapping("/reset-db")
    fun resetDatabase(db: Database, clock: EvakaClock) {
        // Run async jobs before database reset to avoid database locks/deadlocks
        runAllAsyncJobs(clock)

        db.connect { dbc ->
            dbc.waitUntilNoQueriesRunning(timeout = Duration.ofSeconds(10))
            dbc.withLockedDatabase(timeout = Duration.ofSeconds(10)) { it.resetDatabase() }
        }
        MockEmailClient.clear()
    }

    @PostMapping("/run-jobs")
    fun runJobs(clock: EvakaClock) {
        runAllAsyncJobs(clock)
    }

    @PostMapping("/care-areas")
    fun createCareAreas(db: Database, @RequestBody careAreas: List<DevCareArea>) {
        db.connect { dbc ->
            dbc.transaction { careAreas.forEach { careArea -> it.insertTestCareArea(careArea) } }
        }
    }

    @PostMapping("/daycares")
    fun createDaycares(db: Database, @RequestBody daycares: List<DevDaycare>) {
        db.connect { dbc ->
            dbc.transaction { daycares.forEach { daycare -> it.insertTestDaycare(daycare) } }
        }
    }

    @DeleteMapping("/daycare/{daycareId}/cost-center")
    fun deleteDaycareCostCenter(db: Database, @PathVariable daycareId: DaycareId) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.createUpdate("UPDATE daycare SET cost_center = NULL WHERE id = :daycareId")
                    .bind("daycareId", daycareId)
                    .execute()
            }
        }
    }

    @PutMapping("/daycares/{daycareId}/acl")
    fun addAclRoleForDaycare(
        db: Database,
        @PathVariable daycareId: DaycareId,
        @RequestBody body: DaycareAclInsert
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.updateDaycareAcl(
                    daycareId,
                    body.externalId,
                    body.role ?: UserRole.UNIT_SUPERVISOR
                )
            }
        }
    }

    @PostMapping("/daycare-group-acl")
    fun createDaycareGroupAclRows(db: Database, @RequestBody rows: List<DevDaycareGroupAcl>) {
        db.connect { dbc ->
            dbc.transaction { tx -> rows.forEach { tx.insertTestDaycareGroupAcl(it) } }
        }
    }

    @PostMapping("/daycare-groups")
    fun createDaycareGroups(db: Database, @RequestBody groups: List<DevDaycareGroup>) {
        db.connect { dbc ->
            dbc.transaction { groups.forEach { group -> it.insertTestDaycareGroup(group) } }
        }
    }

    @PostMapping("/daycare-group-placements")
    fun createDaycareGroupPlacement(
        db: Database,
        @RequestBody placements: List<DevDaycareGroupPlacement>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                placements.forEach {
                    tx.insertTestDaycareGroupPlacement(
                        it.daycarePlacementId,
                        it.daycareGroupId,
                        it.id,
                        it.startDate,
                        it.endDate
                    )
                }
            }
        }
    }

    data class Caretaker(
        val groupId: GroupId,
        val amount: Double,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )

    @PostMapping("/daycare-caretakers")
    fun createDaycareCaretakers(db: Database, @RequestBody caretakers: List<Caretaker>) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                caretakers.forEach { caretaker ->
                    tx.insertTestCaretakers(
                        caretaker.groupId,
                        amount = caretaker.amount,
                        startDate = caretaker.startDate,
                        endDate = caretaker.endDate
                    )
                }
            }
        }
    }

    @PostMapping("/children")
    fun createChildren(db: Database, @RequestBody children: List<DevChild>) {
        db.connect { dbc -> dbc.transaction { tx -> children.forEach { tx.insertTestChild(it) } } }
    }

    @PostMapping("/daycare-placements")
    fun createDaycarePlacements(db: Database, @RequestBody placements: List<DevPlacement>) {
        db.connect { dbc ->
            dbc.transaction {
                placements.forEach { placement -> it.insertTestPlacement(placement) }
            }
        }
    }

    data class DevTerminatePlacementRequest(
        val placementId: PlacementId,
        val endDate: LocalDate,
        val terminationRequestedDate: LocalDate?,
        val terminatedBy: EvakaUserId?
    )

    @PostMapping("/placement/terminate")
    fun terminatePlacement(
        db: Database,
        @RequestBody terminationRequest: DevTerminatePlacementRequest
    ) {
        db.connect { dbc ->
            dbc.transaction {
                    it.createUpdate(
                        "UPDATE placement SET end_date = :endDate, termination_requested_date = :terminationRequestedDate, terminated_by = :terminatedBy WHERE id = :placementId "
                    )
                }
                .bindKotlin(terminationRequest)
                .execute()
        }
    }

    data class DecisionRequest(
        val id: DecisionId,
        val employeeId: EmployeeId,
        val applicationId: ApplicationId,
        val unitId: DaycareId,
        val type: DecisionType,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val status: DecisionStatus
    )

    @PostMapping("/decisions")
    fun createDecisions(
        db: Database,
        evakaClock: EvakaClock,
        @RequestBody decisions: List<DecisionRequest>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                decisions.forEach { decision ->
                    tx.insertTestDecision(
                        TestDecision(
                            id = decision.id,
                            createdBy = EvakaUserId(decision.employeeId.raw),
                            sentDate = evakaClock.today(),
                            unitId = decision.unitId,
                            applicationId = decision.applicationId,
                            type = decision.type,
                            startDate = decision.startDate,
                            endDate = decision.endDate,
                            status = decision.status,
                            requestedStartDate = null,
                            resolvedBy = null,
                            resolved = null,
                            pendingDecisionEmailsSentCount = null,
                            pendingDecisionEmailSent = null
                        )
                    )
                }
            }
        }
    }

    @PostMapping("/decisions/{id}/actions/create-pdf")
    fun createDecisionPdf(db: Database, @PathVariable id: DecisionId) {
        db.connect { dbc ->
            dbc.transaction { decisionService.createDecisionPdfs(it, fakeAdmin, id) }
        }
    }

    @PostMapping("/decisions/{id}/actions/reject-by-citizen")
    fun rejectDecisionByCitizen(db: Database, clock: EvakaClock, @PathVariable id: DecisionId) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val decision = tx.getDecision(id) ?: throw NotFound("decision not found")
                val application =
                    tx.fetchApplicationDetails(decision.applicationId)
                        ?: throw NotFound("application not found")
                applicationStateService.rejectDecision(
                    tx,
                    AuthenticatedUser.Citizen(application.guardianId, CitizenAuthLevel.STRONG),
                    clock,
                    application.id,
                    id
                )
            }
        }
    }

    @GetMapping("/applications/{applicationId}")
    fun getApplication(
        db: Database,
        @PathVariable applicationId: ApplicationId
    ): ApplicationDetails {
        return db.connect { dbc -> dbc.read { tx -> tx.fetchApplicationDetails(applicationId) } }
            ?: throw NotFound("application not found")
    }

    @GetMapping("/applications/{applicationId}/decisions")
    fun getApplicationDecisions(
        db: Database,
        @PathVariable applicationId: ApplicationId
    ): List<Decision> {
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getDecisionsByApplication(applicationId, AccessControlFilter.PermitAll)
            }
        }
    }

    @PostMapping("/fee-decisions")
    fun createFeeDecisions(db: Database, @RequestBody decisions: List<FeeDecision>) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.upsertFeeDecisions(decisions)
                decisions.forEach { fd ->
                    if (fd.sentAt != null) {
                        tx.updateFeeDecisionSentAt(fd)
                    }
                }
            }
        }
    }

    @PostMapping("/value-decisions")
    fun createVoucherValueDecisions(
        db: Database,
        @RequestBody decisions: List<VoucherValueDecision>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.upsertValueDecisions(decisions)
                decisions.forEach { fd ->
                    if (fd.sentAt != null) {
                        tx.markVoucherValueDecisionsSent(listOf(fd.id), fd.sentAt)
                    }
                }
            }
        }
    }

    @PostMapping("/invoices")
    fun createInvoices(db: Database, @RequestBody invoices: List<Invoice>) {
        db.connect { dbc -> dbc.transaction { tx -> tx.upsertInvoices(invoices) } }
    }

    @PostMapping("/fee-thresholds")
    fun createFeeThresholds(
        db: Database,
        @RequestBody feeThresholds: FeeThresholds
    ): FeeThresholdsId =
        db.connect { dbc -> dbc.transaction { it.insertTestFeeThresholds(feeThresholds) } }

    data class DevCreateIncomeStatements(
        val personId: PersonId,
        val data: List<IncomeStatementBody>
    )

    @PostMapping("/income-statements")
    fun createIncomeStatements(db: Database, @RequestBody body: DevCreateIncomeStatements) =
        db.connect { dbc ->
            dbc.transaction { tx ->
                body.data.forEach { tx.createIncomeStatement(body.personId, it) }
            }
        }

    @PostMapping("/income")
    fun createIncome(db: Database, @RequestBody body: DevIncome) {
        db.connect { dbc -> dbc.transaction { it.insertTestIncome(body) } }
    }

    @PostMapping("/income-notifications")
    fun createIncomeNotification(db: Database, @RequestBody body: IncomeNotification) {
        db.connect { dbc ->
            dbc.transaction {
                val id = it.createIncomeNotification(body.receiverId, body.notificationType)
                it.createUpdate("UPDATE income_notification SET created = :created WHERE id = :id")
                    .bind("id", id)
                    .bind("created", body.created)
                    .execute()
            }
        }
    }

    @PostMapping("/person")
    fun upsertPerson(db: Database, @RequestBody body: DevPerson): PersonDTO {
        if (body.ssn == null) throw BadRequest("SSN is required for using this endpoint")
        return db.connect { dbc ->
            dbc.transaction { tx ->
                val person = tx.getPersonBySSN(body.ssn)
                val personDTO = body.toPersonDTO()

                if (person != null) {
                    tx.updatePersonFromVtj(personDTO)
                } else {
                    createPersonFromVtj(tx, personDTO)
                }
            }
        }
    }

    @PostMapping("/person/create")
    fun createPerson(db: Database, @RequestBody body: DevPerson): PersonId {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                val personId = tx.insertTestPerson(body)
                tx.insertEvakaUser(
                    EvakaUser(
                        id = EvakaUserId(personId.raw),
                        type = EvakaUserType.CITIZEN,
                        name = "${body.lastName} ${body.firstName}"
                    )
                )
                tx.createPersonMessageAccount(personId)
                val dto = body.copy(id = personId).toPersonDTO()
                if (dto.identity is ExternalIdentifier.SSN) {
                    tx.updatePersonFromVtj(dto)
                }
                personId
            }
        }
    }

    @PostMapping("/parentship")
    fun createParentships(db: Database, @RequestBody parentships: List<DevParentship>) {
        db.connect { dbc ->
            dbc.transaction { tx -> parentships.forEach { tx.insertTestParentship(it) } }
        }
    }

    @GetMapping("/employee")
    fun getEmployees(db: Database): List<Employee> {
        return db.connect { dbc -> dbc.read { it.getEmployees() } }
    }

    @PostMapping("/employee")
    fun createEmployee(db: Database, @RequestBody body: DevEmployee): EmployeeId {
        return db.connect { dbc -> dbc.transaction { it.insertTestEmployee(body) } }
    }

    @GetMapping("/employee/external-id/{id}")
    fun getEmployee(db: Database, @PathVariable id: ExternalId): Employee {
        return db.connect { dbc ->
            dbc.transaction { tx -> tx.getEmployeeByExternalId(id) ?: throw NotFound() }
        }
    }

    @DeleteMapping("/employee/external-id/{externalId}")
    fun deleteEmployeeByExternalId(db: Database, @PathVariable externalId: ExternalId) {
        db.connect { dbc ->
            dbc.transaction { it.deleteAndCascadeEmployeeByExternalId(externalId) }
        }
    }

    @PostMapping("/employee/external-id/{externalId}")
    fun upsertEmployeeByExternalId(
        db: Database,
        @PathVariable externalId: ExternalId,
        @RequestBody employee: DevEmployee
    ): EmployeeId =
        db.connect { dbc ->
            dbc.transaction {
                it.createUpdate(
                        """
INSERT INTO employee (first_name, last_name, email, external_id, roles, active)
VALUES (:firstName, :lastName, :email, :externalId, :roles::user_role[], :active)
ON CONFLICT (external_id) DO UPDATE SET
    first_name = excluded.first_name,
    last_name = excluded.last_name,
    email = excluded.email,
    roles = excluded.roles
RETURNING id
"""
                    )
                    .bindKotlin(employee)
                    .executeAndReturnGeneratedKeys()
                    .exactlyOne<EmployeeId>()
            }
        }

    @GetMapping("/citizen/ssn/{ssn}")
    fun getCitizen(@PathVariable ssn: String): Citizen =
        Citizen.from(MockPersonDetailsService.getPerson(ssn) ?: throw NotFound())

    @GetMapping("/citizen")
    fun getCitizens(): List<Citizen> =
        MockPersonDetailsService.allPersons.values
            .filter { it.guardians.isEmpty() }
            .map(Citizen::from)

    @PostMapping("/guardian")
    fun insertGuardians(db: Database, @RequestBody guardians: List<DevGuardian>) {
        db.connect { dbc ->
            dbc.transaction { tx -> guardians.forEach { tx.insertTestGuardian(it) } }
        }
    }

    @PostMapping("/child")
    fun insertChild(db: Database, @RequestBody body: DevPerson): ChildId =
        db.connect { dbc ->
            dbc.transaction {
                it.insertTestPerson(
                        DevPerson(
                            id = body.id,
                            dateOfBirth = body.dateOfBirth,
                            firstName = body.firstName,
                            lastName = body.lastName,
                            ssn = body.ssn,
                            streetAddress = body.streetAddress,
                            postalCode = body.postalCode,
                            postOffice = body.postOffice,
                            restrictedDetailsEnabled = body.restrictedDetailsEnabled
                        )
                    )
                    .also { id -> it.insertTestChild(DevChild(id = id)) }
            }
        }

    @PostMapping("/message-account/upsert-all")
    fun createMessageAccounts(db: Database) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.execute(
                    "INSERT INTO message_account (daycare_group_id, type) SELECT id, 'GROUP'::message_account_type as type FROM daycare_group ON CONFLICT DO NOTHING"
                )
                tx.execute(
                    "INSERT INTO message_account (person_id, type) SELECT id, 'CITIZEN'::message_account_type as type FROM person ON CONFLICT DO NOTHING"
                )
                tx.execute(
                    "INSERT INTO message_account (employee_id, type) SELECT employee_id, 'PERSONAL'::message_account_type as type FROM daycare_acl WHERE role = 'UNIT_SUPERVISOR' OR role = 'SPECIAL_EDUCATION_TEACHER' ON CONFLICT DO NOTHING"
                )
                tx.execute(
                    "INSERT INTO message_account (daycare_group_id, person_id, employee_id, type) VALUES (NULL, NULL, NULL, 'MUNICIPAL') ON CONFLICT DO NOTHING"
                )
                tx.execute(
                    "INSERT INTO message_account (daycare_group_id, person_id, employee_id, type) VALUES (NULL, NULL, NULL, 'SERVICE_WORKER') ON CONFLICT DO NOTHING"
                )
            }
        }
    }

    @PostMapping("/backup-cares")
    fun createBackupCares(db: Database, @RequestBody backupCares: List<DevBackupCare>) {
        db.connect { dbc ->
            dbc.transaction { tx -> backupCares.forEach { tx.insertTestBackupCare(it) } }
        }
    }

    @PostMapping("/applications")
    fun createApplications(
        db: Database,
        @RequestBody applications: List<DevApplicationWithForm>
    ): List<ApplicationId> =
        db.connect { dbc ->
            dbc.transaction { tx ->
                applications.map { application ->
                    val id = tx.insertApplication(application)
                    tx.insertApplicationForm(
                        DevApplicationForm(
                            applicationId = id,
                            revision = 1,
                            document =
                                DaycareFormV0.fromForm2(
                                    application.form,
                                    application.type,
                                    false,
                                    false
                                )
                        )
                    )
                    id
                }
            }
        }

    @PostMapping("/placement-plan/{application-id}")
    fun createPlacementPlan(
        db: Database,
        @PathVariable("application-id") applicationId: ApplicationId,
        @RequestBody placementPlan: PlacementPlan
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val application =
                    tx.fetchApplicationDetails(applicationId)
                        ?: throw NotFound("application $applicationId not found")
                val preschoolDaycarePeriod =
                    if (placementPlan.preschoolDaycarePeriodStart != null) {
                        FiniteDateRange(
                            placementPlan.preschoolDaycarePeriodStart,
                            placementPlan.preschoolDaycarePeriodEnd!!
                        )
                    } else {
                        null
                    }

                placementPlanService.createPlacementPlan(
                    tx,
                    application,
                    DaycarePlacementPlan(
                        placementPlan.unitId,
                        FiniteDateRange(placementPlan.periodStart, placementPlan.periodEnd),
                        preschoolDaycarePeriod
                    )
                )
            }
        }
    }

    @GetMapping("/messages")
    fun getMessages(db: Database): List<SfiMessage> {
        return MockSfiMessagesClient.getMessages().map { it.first }
    }

    @PostMapping("/messages/clean-up")
    fun cleanUpMessages(db: Database) {
        MockSfiMessagesClient.clearMessages()
    }

    @PostMapping("/vtj-persons")
    fun upsertPerson(db: Database, @RequestBody person: VtjPerson) {
        MockPersonDetailsService.upsertPerson(person)
        db.connect { dbc ->
            dbc.transaction { tx ->
                val uuid =
                    tx.createQuery("SELECT id FROM person WHERE social_security_number = :ssn")
                        .bind("ssn", person.socialSecurityNumber)
                        .exactlyOneOrNull<PersonId>()

                uuid?.let {
                    // Refresh Pis data by forcing refresh from VTJ
                    personService.getUpToDatePersonFromVtj(
                        tx,
                        AuthenticatedUser.SystemInternalUser,
                        it
                    )
                }
            }
        }
    }

    @PostMapping("/persons/{person}/force-full-vtj-refresh")
    fun forceFullVtjRefresh(db: Database, @PathVariable person: PersonId) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val user = AuthenticatedUser.SystemInternalUser
                personService.getUpToDatePersonFromVtj(tx, user, person)
                personService.getGuardians(tx, user, person)
                personService.getPersonWithChildren(tx, user, person)
            }
        }
    }

    @GetMapping("/vtj-persons/{ssn}")
    fun getVtjPerson(@PathVariable ssn: String): VtjPerson {
        return MockPersonDetailsService.getPerson(ssn)
            ?: throw NotFound("vtj person $ssn was not found")
    }

    @GetMapping("/emails")
    fun getSentEmails(): List<Email> {
        return MockEmailClient.emails
    }

    @PostMapping("/applications/{applicationId}/actions/{action}")
    fun simpleAction(
        db: Database,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId,
        @PathVariable action: String
    ) {
        val simpleActions =
            mapOf(
                "move-to-waiting-placement" to applicationStateService::moveToWaitingPlacement,
                "cancel-application" to applicationStateService::cancelApplication,
                "set-verified" to applicationStateService::setVerified,
                "set-unverified" to applicationStateService::setUnverified,
                "send-decisions-without-proposal" to
                    applicationStateService::sendDecisionsWithoutProposal,
                "send-placement-proposal" to applicationStateService::sendPlacementProposal,
                "confirm-decision-mailed" to applicationStateService::confirmDecisionMailed
            )

        val actionFn = simpleActions[action] ?: throw NotFound("Action not recognized")
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.ensureFakeAdminExists()
                actionFn.invoke(tx, fakeAdmin, clock, applicationId)
            }
        }
        runAllAsyncJobs(clock)
    }

    @PostMapping("/applications/{applicationId}/actions/create-placement-plan")
    fun createPlacementPlan(
        db: Database,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: DaycarePlacementPlan
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.ensureFakeAdminExists()
                applicationStateService.createPlacementPlan(tx, fakeAdmin, applicationId, body)
            }
        }
    }

    @PostMapping("/applications/{applicationId}/actions/create-default-placement-plan")
    fun createDefaultPlacementPlan(
        db: Database,
        clock: EvakaClock,
        @PathVariable applicationId: ApplicationId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.ensureFakeAdminExists()
                placementPlanService
                    .getPlacementPlanDraft(tx, applicationId, minStartDate = clock.today())
                    .let {
                        DaycarePlacementPlan(
                            unitId = it.preferredUnits.first().id,
                            period = it.period,
                            preschoolDaycarePeriod = it.preschoolDaycarePeriod
                        )
                    }
                    .let {
                        applicationStateService.createPlacementPlan(
                            tx,
                            fakeAdmin,
                            applicationId,
                            it
                        )
                    }
            }
        }
    }

    @PostMapping("/mobile/pairings/challenge")
    fun postPairingChallenge(
        db: Database,
        clock: EvakaClock,
        @RequestBody body: PairingsController.PostPairingChallengeReq
    ): Pairing {
        return db.connect { dbc ->
            dbc.transaction { it.challengePairing(clock, body.challengeKey) }
        }
    }

    @PostMapping("/mobile/pairings/{id}/response")
    fun postPairingResponse(
        db: Database,
        clock: EvakaClock,
        @PathVariable id: PairingId,
        @RequestBody body: PairingsController.PostPairingResponseReq
    ): Pairing {
        return db.connect { dbc ->
            dbc.transaction { it.incrementAttempts(id, body.challengeKey) }
            dbc.transaction {
                it.respondPairingChallengeCreateDevice(
                    clock,
                    id,
                    body.challengeKey,
                    body.responseKey
                )
            }
        }
    }

    @PostMapping("/mobile/pairings")
    fun postPairing(
        db: Database,
        clock: EvakaClock,
        @RequestBody body: PairingsController.PostPairingReq
    ): Pairing {
        return db.connect { dbc ->
            dbc.transaction {
                when (body) {
                    is PairingsController.PostPairingReq.Unit ->
                        it.initPairing(clock, unitId = body.unitId)
                    is PairingsController.PostPairingReq.Employee ->
                        it.initPairing(clock, employeeId = body.employeeId)
                }
            }
        }
    }

    @PostMapping("/mobile/devices")
    fun postMobileDevice(db: Database, @RequestBody body: DevMobileDevice) {
        db.connect { dbc -> dbc.transaction { it.insertTestMobileDevice(body) } }
    }

    @PostMapping("/mobile/personal-devices")
    fun postPersonalMobileDevice(db: Database, @RequestBody body: DevPersonalMobileDevice) {
        db.connect { dbc -> dbc.transaction { it.insertTestPersonalMobileDevice(body) } }
    }

    @PostMapping("/holiday-period/{id}")
    fun createHolidayPeriod(
        db: Database,
        @PathVariable id: HolidayPeriodId,
        @RequestBody body: HolidayPeriodBody
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.insertHolidayPeriod(body.period, body.reservationDeadline).let {
                    tx.createUpdate("UPDATE holiday_period SET id = :id WHERE id = :prevId")
                        .bind("id", id)
                        .bind("prevId", it.id)
                        .execute()
                }
            }
        }
    }

    data class DevHoliday(val date: LocalDate, val description: String)

    @PostMapping("/holiday")
    fun createHoliday(db: Database, @RequestBody holiday: DevHoliday) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.createUpdate(
                        "INSERT INTO holiday(date, description) VALUES(:date, :description)"
                    )
                    .bind("date", holiday.date)
                    .bind("description", holiday.description)
                    .execute()
            }
        }
    }

    @PostMapping("/holiday-period/questionnaire/{id}")
    fun createHolidayQuestionnaire(
        db: Database,
        @PathVariable id: HolidayQuestionnaireId,
        @RequestBody body: FixedPeriodQuestionnaireBody
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.createFixedPeriodQuestionnaire(body).let {
                    tx.createUpdate(
                            "UPDATE holiday_period_questionnaire SET id = :id WHERE id = :prevId"
                        )
                        .bind("id", id)
                        .bind("prevId", it)
                        .execute()
                }
            }
        }
    }

    @PostMapping("/reservations")
    fun postReservations(
        db: Database,
        clock: EvakaClock,
        @RequestBody body: List<DailyReservationRequest>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.ensureFakeAdminExists()
                createReservationsAndAbsences(tx, clock.today(), fakeAdmin, body)
            }
        }
    }

    @PostMapping("/children/{childId}/child-daily-notes")
    fun postChildDailyNote(
        db: Database,
        @PathVariable childId: ChildId,
        @RequestBody body: ChildDailyNoteBody
    ): ChildDailyNoteId {
        return db.connect { dbc ->
            dbc.transaction { it.createChildDailyNote(childId = childId, note = body) }
        }
    }

    @PostMapping("/children/{childId}/child-sticky-notes")
    fun postChildStickyNote(
        db: Database,
        @PathVariable childId: ChildId,
        @RequestBody body: ChildStickyNoteBody
    ): ChildStickyNoteId {
        return db.connect { dbc ->
            dbc.transaction { it.createChildStickyNote(childId = childId, note = body) }
        }
    }

    @PostMapping("/daycare-groups/{groupId}/group-notes")
    fun postGroupNote(
        db: Database,
        @PathVariable groupId: GroupId,
        @RequestBody body: GroupNoteBody
    ): GroupNoteId {
        return db.connect { dbc ->
            dbc.transaction { it.createGroupNote(groupId = groupId, note = body) }
        }
    }

    @GetMapping("/digitransit/autocomplete")
    fun digitransitAutocomplete() = digitransit.autocomplete()

    @PutMapping("/digitransit/autocomplete")
    fun putDigitransitAutocomplete(@RequestBody mockResponse: MockDigitransit.Autocomplete) =
        digitransit.setAutocomplete(mockResponse)

    @PostMapping("/digitransit/query")
    fun postDigitransitQuery(@RequestBody body: String): String {
        return if (body.matches(Regex("^\\{\\s*plan\\("))) {
            "7"
        } else {
            val rex = Regex("(id.*):\\s*plan\\(")
            val contents =
                rex.findAll(body)
                    .mapIndexed { ix, match ->
                        """
                        "${match.groupValues[1]}": {
                            "itineraries": [{
                                "legs":[{"distance": $ix}]
                            }]
                        }
                    """
                    }
                    .joinToString(",")
            "{ \"data\": { $contents } }"
        }
    }

    @PostMapping("/family-contact")
    fun createFamilyContact(db: Database, @RequestBody contacts: List<DevFamilyContact>) {
        db.connect { dbc ->
            dbc.transaction { contacts.forEach { contact -> it.insertFamilyContact(contact) } }
        }
    }

    @PostMapping("/backup-pickup")
    fun createBackupPickup(db: Database, @RequestBody backupPickups: List<DevBackupPickup>) {
        db.connect { dbc ->
            dbc.transaction {
                backupPickups.forEach { backupPickup -> it.insertBackupPickup(backupPickup) }
            }
        }
    }

    @PostMapping("/fridge-child")
    fun createFridgeChild(db: Database, @RequestBody fridgeChildren: List<DevFridgeChild>) {
        db.connect { dbc ->
            dbc.transaction { fridgeChildren.forEach { child -> it.insertFridgeChild(child) } }
        }
    }

    @PostMapping("/fridge-partner")
    fun createFridgePartner(db: Database, @RequestBody fridgePartners: List<DevFridgePartner>) {
        db.connect { dbc ->
            dbc.transaction {
                fridgePartners.forEach { partner -> it.insertFridgePartner(partner) }
            }
        }
    }

    @PostMapping("/foster-parent")
    fun createFosterParent(db: Database, @RequestBody fosterParents: List<DevFosterParent>) {
        db.connect { dbc ->
            dbc.transaction { tx -> fosterParents.forEach { tx.insertFosterParent(it) } }
        }
    }

    @PostMapping("/employee-pin")
    fun createEmployeePins(db: Database, @RequestBody employeePins: List<DevEmployeePin>) {
        db.connect { dbc ->
            dbc.transaction {
                employeePins.forEach { employeePin ->
                    val userId =
                        if (employeePin.userId != null) {
                            employeePin.userId
                        } else if (!employeePin.employeeExternalId.isNullOrBlank()) {
                            it.getEmployeeIdByExternalId(employeePin.employeeExternalId)
                        } else {
                            throw Error(
                                "Cannot create dev employee pin: user id and external user id missing"
                            )
                        }

                    it.insertEmployeePin(employeePin.copy(userId = userId))
                }
            }
        }
    }

    @PostMapping("/pedagogical-document")
    fun createPedagogicalDocuments(
        db: Database,
        @RequestBody pedagogicalDocuments: List<DevPedagogicalDocument>
    ) {
        db.connect { dbc ->
            dbc.transaction {
                pedagogicalDocuments.forEach { pedagogicalDocument ->
                    it.insertPedagogicalDocument(pedagogicalDocument)
                }
            }
        }
    }

    @PostMapping("/pedagogical-document-attachment/{pedagogicalDocumentId}")
    fun createPedagogicalDocumentAttachment(
        db: Database,
        @PathVariable pedagogicalDocumentId: PedagogicalDocumentId,
        @RequestParam employeeId: EmployeeId,
        @RequestPart("file") file: MultipartFile
    ): String {
        return db.connect { dbc ->
                dbc.transaction { tx ->
                    val id = UUID.randomUUID()
                    tx.insertAttachment(
                        AuthenticatedUser.Employee(employeeId, emptySet()),
                        AttachmentId(id),
                        file.name,
                        file.contentType ?: "image/jpeg",
                        AttachmentParent.PedagogicalDocument(pedagogicalDocumentId),
                        type = null
                    )
                    documentClient.upload(
                        filesBucket,
                        Document(
                            name = id.toString(),
                            bytes = file.bytes,
                            contentType = file.contentType ?: "image/jpeg"
                        )
                    )
                }
            }
            .key
    }

    data class CreateVasuTemplateBody(
        val name: String = "testipohja",
        val valid: FiniteDateRange =
            FiniteDateRange(LocalDate.ofYearDay(2020, 1), LocalDate.ofYearDay(2200, 1)),
        val type: CurriculumType = CurriculumType.DAYCARE,
        val language: VasuLanguage = VasuLanguage.FI
    )

    @PostMapping("/vasu/template")
    fun createVasuTemplate(
        db: Database,
        @RequestBody body: CreateVasuTemplateBody
    ): VasuTemplateId {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                tx.insertVasuTemplate(
                    name = body.name,
                    valid = body.valid,
                    type = body.type,
                    language = body.language,
                    content = getDefaultTemplateContent(body.type, body.language)
                )
            }
        }
    }

    @PostMapping("/vasu/revokeSharingPermission/{docId}")
    fun revokeSharingPermission(db: Database, @PathVariable docId: VasuDocumentId) {
        return db.connect { dbc ->
            dbc.transaction { tx -> tx.revokeVasuGuardianHasGivenPermissionToShare(docId) }
        }
    }

    @DeleteMapping("/vasu/templates")
    fun deleteVasuTemplates(db: Database) {
        db.connect { dbc ->
            dbc.transaction { it.createUpdate("DELETE FROM curriculum_template").execute() }
        }
    }

    @PostMapping("/vasu/doc")
    fun createVasuDocument(
        db: Database,
        clock: EvakaClock,
        @RequestBody body: PostVasuDocBody
    ): VasuDocumentId {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                val template =
                    tx.getVasuTemplate(body.templateId)
                        ?: throw NotFound("Template with id ${body.templateId} not found")
                tx.insertVasuDocument(clock.now(), body.childId, template)
            }
        }
    }

    @PostMapping("/vasu/doc/publish/{documentId}")
    fun publishVasuDocument(
        db: Database,
        clock: EvakaClock,
        @PathVariable documentId: VasuDocumentId
    ) {
        return db.connect { dbc ->
            dbc.transaction { tx -> tx.publishVasuDocument(clock.now(), documentId) }
        }
    }

    @PostMapping("/document-templates")
    fun createDocumentTemplate(
        db: Database,
        clock: EvakaClock,
        @RequestBody body: DevDocumentTemplate
    ): DocumentTemplateId {
        return db.connect { dbc -> dbc.transaction { tx -> tx.insertTestDocumentTemplate(body) } }
    }

    @PostMapping("/child-documents")
    fun createChildDocument(
        db: Database,
        clock: EvakaClock,
        @RequestBody body: DevChildDocument
    ): ChildDocumentId {
        return db.connect { dbc -> dbc.transaction { tx -> tx.insertTestChildDocument(body) } }
    }

    @PostMapping("/service-need")
    fun createServiceNeeds(db: Database, @RequestBody serviceNeeds: List<DevServiceNeed>) {
        db.connect { dbc ->
            dbc.transaction {
                serviceNeeds.forEach { sn ->
                    it.insertTestServiceNeed(
                        placementId = sn.placementId,
                        period = FiniteDateRange(sn.startDate, sn.endDate),
                        optionId = sn.optionId,
                        shiftCare = sn.shiftCare,
                        id = sn.id,
                        confirmedBy = sn.confirmedBy
                    )
                }
            }
        }
    }

    @PostMapping("/service-need-option")
    fun createServiceNeedOption(
        db: Database,
        @RequestBody serviceNeedOptions: List<ServiceNeedOption>
    ) {
        db.connect { dbc ->
            dbc.transaction {
                serviceNeedOptions.forEach { option -> it.insertServiceNeedOption(option) }
            }
        }
    }

    @PostMapping("/service-need-options")
    fun createDefaultServiceNeedOptions(db: Database) {
        db.connect { dbc -> dbc.transaction { it.insertServiceNeedOptions() } }
    }

    @PostMapping("/voucher-values")
    fun createVoucherValues(db: Database) {
        db.connect { dbc -> dbc.transaction { it.insertServiceNeedOptionVoucherValues() } }
    }

    @PostMapping("/assistance-factors")
    fun createAssistanceFactors(
        db: Database,
        @RequestBody assistanceFactors: List<DevAssistanceFactor>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                assistanceFactors.forEach { tx.insertTestAssistanceFactor(it) }
            }
        }
    }

    @PostMapping("/daycare-assistances")
    fun createDaycareAssistances(
        db: Database,
        @RequestBody daycareAssistances: List<DevDaycareAssistance>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                daycareAssistances.forEach { tx.insertTestDaycareAssistance(it) }
            }
        }
    }

    @PostMapping("/preschool-assistances")
    fun createPreschoolAssistances(
        db: Database,
        @RequestBody preschoolAssistances: List<DevPreschoolAssistance>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                preschoolAssistances.forEach { tx.insertTestPreschoolAssistance(it) }
            }
        }
    }

    @PostMapping("/other-assistance-measures")
    fun createOtherAssistanceMeasures(
        db: Database,
        @RequestBody otherAssistanceMeasures: List<DevOtherAssistanceMeasure>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                otherAssistanceMeasures.forEach { tx.insertTestOtherAssistanceMeasure(it) }
            }
        }
    }

    @PostMapping("/assistance-need-decisions")
    fun createAssistanceNeedDecisions(
        db: Database,
        @RequestBody assistanceNeedDecisions: List<DevAssistanceNeedDecision>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                assistanceNeedDecisions.forEach {
                    tx.insertTestAssistanceNeedDecision(it.childId, it)
                }
            }
        }
    }

    @PostMapping("/assistance-need-preschool-decisions")
    fun createAssistanceNeedPreschoolDecisions(
        db: Database,
        @RequestBody assistanceNeedDecisions: List<DevAssistanceNeedPreschoolDecision>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                assistanceNeedDecisions.forEach { tx.insertTestAssistanceNeedPreschoolDecision(it) }
            }
        }
    }

    @PostMapping("/attendances")
    fun postAttendances(db: Database, @RequestBody attendances: List<DevChildAttendance>) =
        db.connect { dbc ->
            dbc.transaction { tx ->
                attendances.forEach {
                    tx.insertTestChildAttendance(
                        childId = it.childId,
                        unitId = it.unitId,
                        arrived = it.arrived,
                        departed = it.departed
                    )
                }
            }
        }

    data class DevVardaReset(val evakaChildId: ChildId, val resetTimestamp: Instant?)

    @PostMapping("/varda/reset-child")
    fun createVardaReset(db: Database, @RequestBody body: DevVardaReset) {
        db.connect { dbc ->
            dbc.transaction {
                it.createUpdate(
                        "INSERT INTO varda_reset_child(evaka_child_id, reset_timestamp) VALUES (:evakaChildId, :resetTimestamp)"
                    )
                    .bindKotlin(body)
                    .execute()
            }
        }
    }

    data class DevVardaServiceNeed(
        val evakaServiceNeedId: ServiceNeedId,
        val evakaServiceNeedUpdated: Instant,
        val evakaChildId: ChildId,
        val updateFailed: Boolean?,
        val errors: List<String>?
    )

    @PostMapping("/varda/varda-service-need")
    fun createVardaServiceNeed(db: Database, @RequestBody body: DevVardaServiceNeed) {
        db.connect { dbc ->
            dbc.transaction {
                it.createUpdate(
                        "INSERT INTO varda_service_need(evaka_service_need_id, evaka_service_need_updated, evaka_child_id, update_failed, errors) " +
                            "VALUES (:evakaServiceNeedId, :evakaServiceNeedUpdated, :evakaChildId, :updateFailed, :errors)"
                    )
                    .bindKotlin(body)
                    .execute()
            }
        }
    }

    @PostMapping("/occupancy-coefficient")
    fun upsertStaffOccupancyCoefficient(
        db: Database,
        @RequestBody body: DevUpsertStaffOccupancyCoefficient
    ) {
        db.connect { dbc ->
            dbc.transaction {
                it.createUpdate(
                        """
                    INSERT INTO staff_occupancy_coefficient (daycare_id, employee_id, coefficient)
                    VALUES (:unitId, :employeeId, :coefficient)
                    ON CONFLICT (daycare_id, employee_id) DO UPDATE SET coefficient = EXCLUDED.coefficient
                    """
                            .trimIndent()
                    )
                    .bindKotlin(body)
                    .execute()
            }
        }
    }

    @PostMapping("/realtime-staff-attendance")
    fun addStaffAttendance(db: Database, @RequestBody body: DevStaffAttendance) =
        db.connect { dbc ->
            dbc.transaction {
                it.createUpdate(
                        """
                    INSERT INTO staff_attendance_realtime (id, employee_id, group_id, arrived, departed, occupancy_coefficient, type)
                    VALUES (:id, :employeeId, :groupId, :arrived, :departed, :occupancyCoefficient, :type)
                    """
                            .trimIndent()
                    )
                    .bindKotlin(body)
                    .execute()
            }
        }

    @PostMapping("/staff-attendance-plan")
    fun addStaffAttendancePlan(db: Database, @RequestBody body: DevStaffAttendancePlan) =
        db.connect { dbc -> dbc.transaction { it.insertTestStaffAttendancePlan(body) } }

    @PostMapping("/daily-service-time")
    fun addDailyServiceTime(db: Database, @RequestBody body: DevDailyServiceTimes) =
        db.connect { dbc -> dbc.transaction { it.insertTestDailyServiceTimes(body) } }

    @PostMapping("/daily-service-time-notification")
    fun addDailyServiceTimeNotification(
        db: Database,
        @RequestBody body: DevDailyServiceTimeNotification
    ) =
        db.connect { dbc ->
            dbc.transaction {
                it.createUpdate(
                        """
                    INSERT INTO daily_service_time_notification (id, guardian_id, daily_service_time_id, date_from, has_deleted_reservations)
                    VALUES (:id, :guardianId, :dailyServiceTimeId, :dateFrom, :hasDeletedReservations)
                    """
                            .trimIndent()
                    )
                    .bindKotlin(body)
                    .execute()
            }
        }

    @PostMapping("/child-consent")
    fun addChildConsent(db: Database, @RequestBody body: DevChildConsent) =
        db.connect { dbc ->
            dbc.transaction {
                it.createUpdate(
                        """
                    INSERT INTO child_consent (given_by_guardian, given, type, child_id)
                    VALUES (:guardianId, :given, :type, :childId)
                    """
                            .trimIndent()
                    )
                    .bindKotlin(body)
                    .execute()
            }
        }

    @PostMapping("/payments")
    fun addPayment(db: Database, @RequestBody body: DevPayment) =
        db.connect { dbc -> dbc.transaction { it.insertDevPayment(body) } }

    @PostMapping("/calendar-event")
    fun addPayment(db: Database, @RequestBody body: DevCalendarEvent) =
        db.connect { dbc -> dbc.transaction { it.insertCalendarEvent(body) } }

    @PostMapping("/calendar-event-attendee")
    fun addPayment(db: Database, @RequestBody body: DevCalendarEventAttendee) =
        db.connect { dbc -> dbc.transaction { it.insertCalendarEventAttendee(body) } }

    @PostMapping("/absence")
    fun addAbsence(db: Database, @RequestBody body: DevAbsence) =
        db.connect { dbc -> dbc.transaction { it.insertTestAbsence(body) } }

    @GetMapping("/child-discussions/{childId}")
    fun getChildDiscussions(
        db: Database,
        @PathVariable childId: ChildId
    ): List<ChildDiscussionData> {
        return db.connect { dbc -> dbc.read { tx -> tx.getChildDiscussions(childId) } }
    }

    @PostMapping("/club-term")
    fun createClubTerm(db: Database, @RequestBody body: ClubTerm) {
        db.connect { dbc -> dbc.transaction { tx -> tx.insertClubTerm(body) } }
    }

    @PostMapping("/preschool-term")
    fun createPreschoolTerm(db: Database, @RequestBody body: PreschoolTerm) {
        db.connect { dbc -> dbc.transaction { tx -> tx.insertPreschoolTerm(body) } }
    }

    @Suppress("EmailMessageType", "ktlint:standard:enum-entry-name-case")
    enum class EmailMessageType {
        pendingDecisionNotification,
        clubApplicationReceived,
        daycareApplicationReceived,
        preschoolApplicationReceived,
        assistanceNeedDecisionNotification,
        assistanceNeedPreschoolDecisionNotification,
        missingReservationsNotification,
        messageNotification,
        vasuNotification,
        pedagogicalDocumentNotification,
        outdatedIncomeNotification,
        calendarEventNotification,
    }

    @GetMapping("/email-content")
    fun getEmails(
        @RequestParam("message", required = false, defaultValue = "pendingDecisionNotification")
        message: EmailMessageType,
        @RequestParam("format", required = false, defaultValue = "html") format: String
    ): ResponseEntity<Any> {
        val emailContent =
            when (message) {
                EmailMessageType.pendingDecisionNotification ->
                    emailMessageProvider.pendingDecisionNotification(Language.fi)
                EmailMessageType.clubApplicationReceived ->
                    emailMessageProvider.clubApplicationReceived(Language.fi)
                EmailMessageType.daycareApplicationReceived ->
                    emailMessageProvider.daycareApplicationReceived(Language.fi)
                EmailMessageType.preschoolApplicationReceived ->
                    emailMessageProvider.preschoolApplicationReceived(Language.fi, true)
                EmailMessageType.assistanceNeedDecisionNotification ->
                    emailMessageProvider.assistanceNeedDecisionNotification(Language.fi)
                EmailMessageType.assistanceNeedPreschoolDecisionNotification ->
                    emailMessageProvider.assistanceNeedPreschoolDecisionNotification(Language.fi)
                EmailMessageType.missingReservationsNotification ->
                    emailMessageProvider.missingReservationsNotification(
                        Language.fi,
                        FiniteDateRange(LocalDate.now().minusDays(7), LocalDate.now())
                    )
                EmailMessageType.messageNotification ->
                    emailMessageProvider.messageNotification(
                        Language.fi,
                        MessageThreadStub(
                            id = MessageThreadId(UUID.randomUUID()),
                            type = MessageType.MESSAGE,
                            title = "Testiviesti",
                            urgent = false,
                            sensitive = false,
                            isCopy = false
                        )
                    )
                EmailMessageType.vasuNotification ->
                    emailMessageProvider.vasuNotification(Language.fi, ChildId(UUID.randomUUID()))
                EmailMessageType.pedagogicalDocumentNotification ->
                    emailMessageProvider.pedagogicalDocumentNotification(
                        Language.fi,
                        ChildId(UUID.randomUUID())
                    )
                EmailMessageType.outdatedIncomeNotification ->
                    emailMessageProvider.outdatedIncomeNotification(
                        IncomeNotificationType.INITIAL_EMAIL,
                        Language.fi
                    )
                EmailMessageType.calendarEventNotification ->
                    emailMessageProvider.calendarEventNotification(
                        Language.fi,
                        listOf(
                            CalendarEventNotificationData(
                                "Esimerkki 1",
                                FiniteDateRange(LocalDate.now(), LocalDate.now().plusDays(1))
                            ),
                            CalendarEventNotificationData(
                                "Esimerkki 2",
                                FiniteDateRange(
                                    LocalDate.now().plusDays(7),
                                    LocalDate.now().plusDays(7)
                                )
                            ),
                        )
                    )
            }
        val content =
            if (format == "html") emailContent.html
            else
                "<div style=\"font-family: monospace; white-space: pre-wrap\">${emailContent.text}</div>"

        val options =
            EmailMessageType.values().joinToString("") {
                "<option value=\"$it\" ${if (it == message) "selected" else ""}>$it</option>"
            }
        val form =
            """
<form>
<select name="message">$options</select>
<select name="format">
  <option value="html" ${if (format == "html") "selected" else ""}>html</option>
  <option value="text" ${if (format == "text") "selected" else ""}>text</option>
</select>
<button>Go</button>
</form>
"""

        val body =
            """
<!DOCTYPE html>
<html>
<body>
$form
<hr>
<div style="max-width: 900px">$content</div>
</body>
"""
        return ResponseEntity.status(HttpStatus.OK).header("Content-Type", "text/html").body(body)
    }
}

// https://www.postgresql.org/docs/14/errcodes-appendix.html
// Class 55 — Object Not In Prerequisite State: lock_not_available
private const val LOCK_NOT_AVAILABLE: String = "55P03"

private fun <T> Database.Connection.withLockedDatabase(
    timeout: Duration,
    f: (tx: Database.Transaction) -> T
): T {
    val start = Instant.now()
    do {
        try {
            return transaction {
                it.execute("SELECT lock_database_nowait()")
                f(it)
            }
        } catch (e: UnableToExecuteStatementException) {
            when (e.psqlCause()?.sqlState) {
                LOCK_NOT_AVAILABLE -> {}
                else -> throw e
            }
        }
        logger.warn { "Failed to obtain database lock" }
        TimeUnit.MILLISECONDS.sleep(100)
    } while (Duration.between(start, Instant.now()).abs() < timeout)
    error("Timed out while waiting for database lock")
}

private data class ActiveConnection(
    val state: String,
    val xactStart: Instant?,
    val queryStart: Instant?,
    val query: String?
)

private fun Database.Connection.waitUntilNoQueriesRunning(timeout: Duration) {
    val start = Instant.now()
    var connections: List<ActiveConnection>
    do {
        connections = read { it.getActiveConnections() }
        if (connections.isEmpty()) {
            return
        }
        TimeUnit.MILLISECONDS.sleep(100)
    } while (Duration.between(start, Instant.now()).abs() < timeout)
    error("Timed out while waiting for database activity to finish: $connections")
}

private fun Database.Read.getActiveConnections(): List<ActiveConnection> =
    createQuery(
            """
SELECT state, xact_start, query_start, left(query, 100) AS query FROM pg_stat_activity
WHERE pid <> pg_backend_pid() AND datname = current_database() AND usename = current_user AND backend_type = 'client backend'
AND state != 'idle'
    """
                .trimIndent()
        )
        .toList<ActiveConnection>()

fun Database.Transaction.ensureFakeAdminExists() {
    // language=sql
    val sql =
        """
        INSERT INTO employee (id, first_name, last_name, email, external_id, roles, active)
        VALUES (:id, 'Dev', 'API', 'dev.api@espoo.fi', 'espoo-ad:' || :id, '{ADMIN, SERVICE_WORKER}'::user_role[], TRUE)
        ON CONFLICT DO NOTHING
        """
            .trimIndent()

    createUpdate(sql).bind("id", fakeAdmin.id).execute()
    upsertEmployeeUser(fakeAdmin.id)
}

fun Database.Transaction.deleteAndCascadeEmployee(id: EmployeeId) {
    execute("DELETE FROM message_account WHERE employee_id = ?", id)
    execute("DELETE FROM employee_pin WHERE user_id = ?", id)
    execute("DELETE FROM employee WHERE id = ?", id)
}

fun Database.Transaction.deleteAndCascadeEmployeeByExternalId(externalId: ExternalId) {
    val employeeId =
        createQuery("SELECT id FROM employee WHERE external_id = :externalId")
            .bind("externalId", externalId)
            .exactlyOneOrNull<EmployeeId>()
    if (employeeId != null) {
        deleteAndCascadeEmployee(employeeId)
    }
}

fun Database.Transaction.insertServiceNeedOptions() {
    execute(
        """
INSERT INTO service_need_option (id, name_fi, name_sv, name_en, valid_placement_type, default_option, fee_coefficient, occupancy_coefficient, occupancy_coefficient_under_3y, realized_occupancy_coefficient, realized_occupancy_coefficient_under_3y, daycare_hours_per_week, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv) VALUES
    ('7406df92-e715-11ec-9ec2-9b7ff580dcb4', 'Kokopäiväinen', 'Kokopäiväinen', 'Kokopäiväinen', 'DAYCARE', TRUE, 1.0, 1.0, 1.75, 1.0, 1.75, 35, FALSE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', 'yli 25h/viikko', 'mer än 25 h/vecka'),
    ('7406e0be-e715-11ec-9ec2-c76f979e1897', 'Osapäiväinen', 'Osapäiväinen', 'Osapäiväinen', 'DAYCARE_PART_TIME', TRUE, 0.6, 0.54, 1.75, 0.54, 1.75, 25, TRUE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', 'korkeintaan 25 h/viikko', 'högst 25 h/vecka'),
    ('7406e0dc-e715-11ec-9ec2-2bcfc9bc9aad', 'Viisivuotiaiden kokopäiväinen', 'Viisivuotiaiden kokopäiväinen', 'Viisivuotiaiden kokopäiväinen', 'DAYCARE_FIVE_YEAR_OLDS', TRUE, 0.8, 1.0, 1.75, 1.0, 1.75, 45, FALSE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', 'yli 25 h/viikko', 'mer än 25 h/vecka'),
    ('7406e0f0-e715-11ec-9ec2-cf6f7df1c620', 'Viisivuotiaiden osapäiväinen', 'Viisivuotiaiden osapäiväinen', 'Viisivuotiaiden osapäiväinen', 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS', TRUE, 0.0, 0.5, 1.75, 0.5, 1.75, 20, TRUE, FALSE, 'ei maksullista varhaiskasvatusta', 'ingen avgiftsbelagd småbarnspedagogik', 'korkeintaan 25 h/viikko', 'högst 25 h/vecka'),
    ('7406e10e-e715-11ec-9ec2-9bd530964cd8', 'Esiopetus', 'Esiopetus', 'Esiopetus', 'PRESCHOOL', TRUE, 0.0, 0.5, 1.75, 0.5, 1.75, 0, TRUE, FALSE, 'ei maksullista varhaiskasvatusta', 'ingen avgiftsbelagd småbarnspedagogik', '', ''),
    ('7406e122-e715-11ec-9ec2-af8eb77613c5', 'Esiopetus ja liittyvä varhaiskasvatus', 'Esiopetus ja liittyvä varhaiskasvatus', 'Esiopetus ja liittyvä varhaiskasvatus', 'PRESCHOOL_DAYCARE', TRUE, 0.8, 1.0, 1.75, 1.0, 1.75, 25, TRUE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', '', ''),
    ('7406e140-e715-11ec-9ec2-0ba1b44c71d2', 'Valmistava opetus', 'Valmistava opetus', 'Valmistava opetus', 'PREPARATORY', TRUE, 0.0, 0.5, 1.75, 0.5, 1.75, 0, TRUE, FALSE, 'ei maksullista varhaiskasvatusta', 'ingen avgiftsbelagd småbarnspedagogik', '', ''),
    ('7406e154-e715-11ec-9ec2-13c1c4a4ea32', 'Valmistava opetus ja liittyvä varhaiskasvatus', 'Valmistava opetus ja liittyvä varhaiskasvatus', 'Valmistava opetus ja liittyvä varhaiskasvatus', 'PREPARATORY_DAYCARE', TRUE, 0.8, 1.0, 1.75, 1.0, 1.75, 25, TRUE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', '', ''),
    ('7406e172-e715-11ec-9ec2-77c5dee24145', 'Kerho', 'Kerho', 'Kerho', 'CLUB', TRUE, 0.0, 1.0, 1.75, 1.0, 1.75, 0, TRUE, TRUE, '', '', '', ''),
    ('7406e186-e715-11ec-9ec2-73d12d14969d', 'Kokopäiväinen tilapäinen', 'Kokopäiväinen tilapäinen', 'Kokopäiväinen tilapäinen', 'TEMPORARY_DAYCARE', TRUE, 1.0, 1.0, 1.75, 1.0, 1.75, 35, FALSE, TRUE, '', '', '', ''),
    ('7406e19a-e715-11ec-9ec2-875be5d177c9', 'Osapäiväinen tilapäinen', 'Osapäiväinen tilapäinen', 'Osapäiväinen tilapäinen', 'TEMPORARY_DAYCARE_PART_DAY', TRUE, 0.5, 0.54, 1.75, 0.54, 1.75, 25, TRUE, TRUE, '', '', '', '')
"""
    )
}

fun Database.Transaction.insertServiceNeedOptionVoucherValues() {
    execute(
        """
INSERT INTO service_need_option_voucher_value (service_need_option_id, validity, base_value, coefficient, value, base_value_under_3y, coefficient_under_3y, value_under_3y) VALUES
    ('7406df92-e715-11ec-9ec2-9b7ff580dcb4', daterange('2000-01-01', NULL, '[]'), 87000, 1.0, 87000, 134850, 1.0, 134850),
    ('7406e0be-e715-11ec-9ec2-c76f979e1897', daterange('2000-01-01', NULL, '[]'), 87000, 0.6, 52200, 134850, 0.6, 80910),
    ('7406e0dc-e715-11ec-9ec2-2bcfc9bc9aad', daterange('2000-01-01', NULL, '[]'), 87000, 1.0, 87000, 134850, 1.0, 134850),
    ('7406e0f0-e715-11ec-9ec2-cf6f7df1c620', daterange('2000-01-01', NULL, '[]'), 87000, 0.6, 52200, 134850, 0.6, 80910),
    ('7406e10e-e715-11ec-9ec2-9bd530964cd8', daterange('2000-01-01', NULL, '[]'), 87000, 0.5, 43500, 134850, 0.5, 64725),
    ('7406e122-e715-11ec-9ec2-af8eb77613c5', daterange('2000-01-01', NULL, '[]'), 87000, 0.5, 43500, 134850, 0.5, 64725),
    ('7406e140-e715-11ec-9ec2-0ba1b44c71d2', daterange('2000-01-01', NULL, '[]'), 87000, 0.5, 43500, 134850, 0.5, 64725),
    ('7406e154-e715-11ec-9ec2-13c1c4a4ea32', daterange('2000-01-01', NULL, '[]'), 87000, 0.5, 43500, 134850, 0.5, 64725),
    ('7406e172-e715-11ec-9ec2-77c5dee24145', daterange('2000-01-01', NULL, '[]'), 87000, 0.0, 0, 134850, 0.0, 0),
    ('7406e186-e715-11ec-9ec2-73d12d14969d', daterange('2000-01-01', NULL, '[]'), 87000, 0.0, 0, 134850, 0.0, 0),
    ('7406e19a-e715-11ec-9ec2-875be5d177c9', daterange('2000-01-01', NULL, '[]'), 87000, 0.0, 0, 134850, 0.0, 0)
"""
    )
}

fun Database.Transaction.updateFeeDecisionSentAt(feeDecision: FeeDecision) =
    createUpdate(
            """
UPDATE fee_decision SET sent_at = :sentAt WHERE id = :id    
    """
                .trimIndent()
        )
        .bind("id", feeDecision.id)
        .bind("sentAt", feeDecision.sentAt)
        .execute()

data class DevCareArea(
    val id: AreaId = AreaId(UUID.randomUUID()),
    val name: String = "Test Care Area",
    val shortName: String = "test_area",
    val areaCode: Int? = 200,
    val subCostCenter: String? = "00"
)

data class DevBackupCare(
    val id: BackupCareId? = null,
    val childId: ChildId,
    val unitId: DaycareId,
    val groupId: GroupId? = null,
    val period: FiniteDateRange
)

data class DevChild(
    val id: ChildId,
    val allergies: String = "",
    val diet: String = "",
    val medication: String = "",
    val additionalInfo: String = "",
    val languageAtHome: String = "",
    val languageAtHomeDetails: String = ""
)

data class DevDaycare(
    val id: DaycareId = DaycareId(UUID.randomUUID()),
    val name: String = "Test Daycare",
    val openingDate: LocalDate? = null,
    val closingDate: LocalDate? = null,
    val areaId: AreaId,
    val type: Set<CareType> =
        setOf(CareType.CENTRE, CareType.PRESCHOOL, CareType.PREPARATORY_EDUCATION),
    val daycareApplyPeriod: DateRange? = DateRange(LocalDate.of(2020, 3, 1), null),
    val preschoolApplyPeriod: DateRange? = DateRange(LocalDate.of(2020, 3, 1), null),
    val clubApplyPeriod: DateRange? = null,
    val providerType: ProviderType = ProviderType.MUNICIPAL,
    val capacity: Int = 0,
    val language: Language = Language.fi,
    val ghostUnit: Boolean = false,
    val uploadToVarda: Boolean = true,
    val uploadChildrenToVarda: Boolean = true,
    val uploadToKoski: Boolean = true,
    val invoicedByMunicipality: Boolean = true,
    val costCenter: String? = "31500",
    val dwCostCenter: String? = "dw-test",
    val additionalInfo: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val url: String? = null,
    val visitingAddress: VisitingAddress =
        VisitingAddress(streetAddress = "Joku katu 9", postalCode = "02100", postOffice = "ESPOO"),
    val location: Coordinate? = null,
    val mailingAddress: MailingAddress = MailingAddress(),
    val unitManager: UnitManager = UnitManager(name = "Unit Manager", phone = "", email = ""),
    val decisionCustomization: DaycareDecisionCustomization =
        DaycareDecisionCustomization(
            daycareName = name,
            preschoolName = name,
            handler = "Decision Handler",
            handlerAddress = "Decision Handler Street 1"
        ),
    val ophUnitOid: String? = "1.2.3.4.5",
    val ophOrganizerOid: String? = "1.2.3.4.5",
    val roundTheClock: Boolean? = false,
    val operationTimes: List<TimeRange?> =
        listOf(
            TimeRange(start = LocalTime.parse("00:00"), end = LocalTime.parse("23:59")),
            TimeRange(start = LocalTime.parse("00:00"), end = LocalTime.parse("23:59")),
            TimeRange(start = LocalTime.parse("00:00"), end = LocalTime.parse("23:59")),
            TimeRange(start = LocalTime.parse("00:00"), end = LocalTime.parse("23:59")),
            TimeRange(start = LocalTime.parse("00:00"), end = LocalTime.parse("23:59")),
            null,
            null
        ),
    val enabledPilotFeatures: Set<PilotFeature> = setOf(),
    val financeDecisionHandler: EmployeeId? = null,
    val businessId: String = "",
    val iban: String = "",
    val providerId: String = ""
)

data class DevDaycareGroup(
    val id: GroupId = GroupId(UUID.randomUUID()),
    val daycareId: DaycareId,
    val name: String = "Testiläiset",
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate? = null
)

data class DevDaycareGroupPlacement(
    val id: GroupPlacementId = GroupPlacementId(UUID.randomUUID()),
    val daycarePlacementId: PlacementId,
    val daycareGroupId: GroupId,
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate = LocalDate.of(2019, 12, 31),
)

data class DevAssistanceNeedDecision(
    val id: AssistanceNeedDecisionId = AssistanceNeedDecisionId(UUID.randomUUID()),
    val decisionNumber: Long?,
    val childId: ChildId,
    val validityPeriod: DateRange,
    val status: AssistanceNeedDecisionStatus,
    val language: AssistanceNeedDecisionLanguage,
    val decisionMade: LocalDate?,
    val sentForDecision: LocalDate?,
    @Nested("selected_unit") val selectedUnit: UnitInfo?,
    @Nested("preparer_1") val preparedBy1: AssistanceNeedDecisionEmployee?,
    @Nested("preparer_2") val preparedBy2: AssistanceNeedDecisionEmployee?,
    @Nested("decision_maker") val decisionMaker: AssistanceNeedDecisionEmployee?,
    val pedagogicalMotivation: String?,
    @Nested("structural_motivation_opt")
    val structuralMotivationOptions: StructuralMotivationOptions,
    val structuralMotivationDescription: String?,
    val careMotivation: String?,
    @Nested("service_opt") val serviceOptions: ServiceOptions,
    val servicesMotivation: String?,
    val expertResponsibilities: String?,
    val guardiansHeardOn: LocalDate?,
    @Json val guardianInfo: Set<AssistanceNeedDecisionGuardian>,
    val viewOfGuardians: String?,
    val otherRepresentativeHeard: Boolean,
    val otherRepresentativeDetails: String?,
    val assistanceLevels: Set<AssistanceLevel>,
    val motivationForDecision: String?,
    val unreadGuardianIds: List<PersonId>?,
    val annulmentReason: String,
)

data class DevAssistanceNeedPreschoolDecision(
    val id: AssistanceNeedPreschoolDecisionId =
        AssistanceNeedPreschoolDecisionId(UUID.randomUUID()),
    val decisionNumber: Long,
    val childId: ChildId,
    val form: AssistanceNeedPreschoolDecisionForm,
    val status: AssistanceNeedDecisionStatus,
    val annulmentReason: String,
    val sentForDecision: LocalDate?,
    val decisionMade: LocalDate?,
    val unreadGuardianIds: Set<PersonId>?
)

data class DevChildAttendance(
    val childId: ChildId,
    val unitId: DaycareId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?
)

data class DevAssistanceAction(
    val id: AssistanceActionId = AssistanceActionId(UUID.randomUUID()),
    val childId: ChildId,
    val updatedBy: EvakaUserId = AuthenticatedUser.SystemInternalUser.evakaUserId,
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate = LocalDate.of(2019, 12, 31),
    val actions: Set<String> = emptySet(),
    val otherAction: String = ""
)

data class DevPlacement(
    val id: PlacementId = PlacementId(UUID.randomUUID()),
    val type: PlacementType = PlacementType.DAYCARE,
    val childId: ChildId,
    val unitId: DaycareId,
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate = LocalDate.of(2019, 12, 31),
    val terminationRequestedDate: LocalDate? = null,
    val terminatedBy: EvakaUserId? = null,
    val placeGuarantee: Boolean = false
)

data class DevPerson(
    val id: PersonId = PersonId(UUID.randomUUID()),
    val dateOfBirth: LocalDate = LocalDate.of(1980, 1, 1),
    val dateOfDeath: LocalDate? = null,
    val firstName: String = "Test",
    val lastName: String = "Person",
    val preferredName: String = "",
    val ssn: String? = null,
    val ssnAddingDisabled: Boolean? = null,
    val email: String? = null,
    val phone: String = "",
    val backupPhone: String = "",
    val language: String? = null,
    val streetAddress: String = "",
    val postalCode: String = "",
    val postOffice: String = "",
    val residenceCode: String = "",
    val nationalities: List<String> = emptyList(),
    val restrictedDetailsEnabled: Boolean = false,
    val restrictedDetailsEndDate: LocalDate? = null,
    val invoiceRecipientName: String = "",
    val invoicingStreetAddress: String = "",
    val invoicingPostalCode: String = "",
    val invoicingPostOffice: String = "",
    val forceManualFeeDecisions: Boolean = false,
    val dependants: List<DevPerson> = emptyList(),
    val guardians: List<DevPerson> = emptyList(),
    val updatedFromVtj: HelsinkiDateTime? = null,
    val ophPersonOid: String = "",
    val duplicateOf: PersonId? = null,
    val enabledEmailTypes: List<EmailMessageType>? = null,
) {
    fun toPersonDTO() =
        PersonDTO(
            id = this.id,
            identity = this.ssn?.let { ExternalIdentifier.SSN.getInstance(it) }
                    ?: ExternalIdentifier.NoID,
            ssnAddingDisabled = this.ssnAddingDisabled ?: false,
            firstName = this.firstName,
            lastName = this.lastName,
            preferredName = this.preferredName,
            email = this.email,
            phone = this.phone,
            backupPhone = this.backupPhone,
            language = this.language,
            dateOfBirth = this.dateOfBirth,
            dateOfDeath = this.dateOfDeath,
            streetAddress = this.streetAddress,
            postalCode = this.postalCode,
            postOffice = this.postOffice,
            residenceCode = this.residenceCode,
            nationalities = this.nationalities,
            restrictedDetailsEnabled = this.restrictedDetailsEnabled,
            restrictedDetailsEndDate = this.restrictedDetailsEndDate,
            invoicingStreetAddress = this.invoicingStreetAddress,
            invoicingPostalCode = this.invoicingPostalCode,
            invoicingPostOffice = this.invoicingPostOffice,
            ophPersonOid = this.ophPersonOid
        )
}

data class DevParentship(
    val id: ParentshipId = ParentshipId(UUID.randomUUID()),
    val childId: ChildId,
    val headOfChildId: PersonId,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class DevEmployee(
    val id: EmployeeId = EmployeeId(UUID.randomUUID()),
    val preferredFirstName: String? = null,
    val firstName: String = "Test",
    val lastName: String = "Person",
    val email: String? = "test.person@espoo.fi",
    val externalId: ExternalId? = null,
    val employeeNumber: String? = null,
    val roles: Set<UserRole> = setOf(),
    val lastLogin: HelsinkiDateTime? = HelsinkiDateTime.now(),
    val active: Boolean = true
)

data class DevMobileDevice(
    val id: MobileDeviceId = MobileDeviceId(UUID.randomUUID()),
    val unitId: DaycareId,
    val name: String = "Laite",
    val longTermToken: UUID? = null
)

data class DevPersonalMobileDevice(
    val id: MobileDeviceId = MobileDeviceId(UUID.randomUUID()),
    val employeeId: EmployeeId,
    val name: String = "Laite",
    val longTermToken: UUID? = null
)

data class DaycareAclInsert(val externalId: ExternalId, val role: UserRole?)

data class PlacementPlan(
    val unitId: DaycareId,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val preschoolDaycarePeriodStart: LocalDate?,
    val preschoolDaycarePeriodEnd: LocalDate?
)

data class DevApplicationWithForm(
    val id: ApplicationId,
    val type: ApplicationType,
    val createdDate: HelsinkiDateTime?,
    val modifiedDate: HelsinkiDateTime?,
    var sentDate: LocalDate?,
    var dueDate: LocalDate?,
    val status: ApplicationStatus,
    val guardianId: PersonId,
    val childId: ChildId,
    val origin: ApplicationOrigin,
    val checkedByAdmin: Boolean,
    val hideFromGuardian: Boolean,
    val transferApplication: Boolean,
    val otherGuardianId: PersonId?,
    val allowOtherGuardianAccess: Boolean = true,
    val form: ApplicationForm
)

data class DevApplicationForm(
    val id: UUID? = UUID.randomUUID(),
    val applicationId: ApplicationId,
    val createdDate: HelsinkiDateTime? = HelsinkiDateTime.now(),
    val revision: Int,
    val document: DaycareFormV0,
    val updated: HelsinkiDateTime? = HelsinkiDateTime.now()
)

data class DevDaycareGroupAcl(val groupId: GroupId, val employeeId: EmployeeId)

data class DevServiceNeed(
    val id: ServiceNeedId,
    val placementId: PlacementId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val optionId: ServiceNeedOptionId,
    val shiftCare: ShiftCareType,
    val confirmedBy: EvakaUserId,
    val confirmedAt: LocalDate? = null
)

data class PostVasuDocBody(val childId: ChildId, val templateId: VasuTemplateId)

data class DevGuardianBlocklistEntry(val guardianId: PersonId, val childId: ChildId)

data class DevUpsertStaffOccupancyCoefficient(
    val unitId: DaycareId,
    val employeeId: EmployeeId,
    val coefficient: BigDecimal
)

data class DevStaffAttendance(
    val id: StaffAttendanceRealtimeId,
    val employeeId: EmployeeId,
    val groupId: GroupId,
    val arrived: HelsinkiDateTime,
    val departed: HelsinkiDateTime?,
    val occupancyCoefficient: BigDecimal,
    val type: StaffAttendanceType
)

data class DevDailyServiceTimes(
    val id: DailyServiceTimesId = DailyServiceTimesId(UUID.randomUUID()),
    val childId: ChildId,
    val type: DailyServiceTimesType = DailyServiceTimesType.REGULAR,
    val validityPeriod: DateRange,
    val regularTimes: TimeRange? = TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
    val mondayTimes: TimeRange? = null,
    val tuesdayTimes: TimeRange? = null,
    val wednesdayTimes: TimeRange? = null,
    val thursdayTimes: TimeRange? = null,
    val fridayTimes: TimeRange? = null,
    val saturdayTimes: TimeRange? = null,
    val sundayTimes: TimeRange? = null
)

data class DevDailyServiceTimeNotification(
    val id: DailyServiceTimeNotificationId,
    val guardianId: PersonId,
    val dailyServiceTimeId: DailyServiceTimesId,
    val dateFrom: LocalDate,
    val hasDeletedReservations: Boolean
)

data class DevChildConsent(
    val guardianId: PersonId,
    val childId: ChildId,
    val type: ChildConsentType,
    val given: Boolean
)

data class DevPayment(
    val id: PaymentId,
    val unitId: DaycareId,
    val unitName: String,
    val unitBusinessId: String?,
    val unitIban: String?,
    val unitProviderId: String?,
    val period: FiniteDateRange,
    val number: Int,
    val amount: Int,
    val status: PaymentStatus,
    val paymentDate: LocalDate?,
    val dueDate: LocalDate?,
    val sentAt: HelsinkiDateTime?,
    val sentBy: EmployeeId?
)

data class DevCalendarEvent(
    val id: CalendarEventId = CalendarEventId(UUID.randomUUID()),
    val title: String,
    val description: String,
    val period: FiniteDateRange
)

data class DevCalendarEventAttendee(
    val id: CalendarEventAttendeeId = CalendarEventAttendeeId(UUID.randomUUID()),
    val calendarEventId: CalendarEventId,
    val unitId: DaycareId,
    val groupId: GroupId? = null,
    val childId: ChildId? = null,
)

data class DevAbsence(
    val id: UUID = UUID.randomUUID(),
    val childId: ChildId,
    val date: LocalDate,
    val absenceType: AbsenceType = AbsenceType.OTHER_ABSENCE,
    val modifiedAt: HelsinkiDateTime = HelsinkiDateTime.now(),
    val modifiedBy: EvakaUserId = AuthenticatedUser.SystemInternalUser.evakaUserId,
    val absenceCategory: AbsenceCategory,
    val questionnaireId: HolidayQuestionnaireId? = null,
)

data class DevGuardian(val guardianId: PersonId, val childId: ChildId)

data class DevDaycareCaretaker(
    val id: DaycareCaretakerId = DaycareCaretakerId(UUID.randomUUID()),
    val groupId: GroupId,
    val amount: BigDecimal = BigDecimal.ZERO,
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate? = null
)

data class DevDocumentTemplate(
    val id: DocumentTemplateId = DocumentTemplateId(UUID.randomUUID()),
    val name: String = "Pedagoginen arvio 2023",
    val type: DocumentType = DocumentType.PEDAGOGICAL_ASSESSMENT,
    val language: DocumentLanguage = DocumentLanguage.FI,
    val confidential: Boolean = true,
    val legalBasis: String = "§15",
    val validity: DateRange,
    val published: Boolean = true,
    @Json val content: DocumentTemplateContent
)

data class DevChildDocument(
    val id: ChildDocumentId = ChildDocumentId(UUID.randomUUID()),
    val status: DocumentStatus,
    val childId: ChildId,
    val templateId: DocumentTemplateId,
    @Json val content: DocumentContent,
    @Json val publishedContent: DocumentContent?,
    val modifiedAt: HelsinkiDateTime,
    val publishedAt: HelsinkiDateTime?
)

data class Citizen(
    val ssn: String,
    val firstName: String,
    val lastName: String,
    val dependantCount: Int
) {
    companion object {
        fun from(vtjPerson: VtjPerson) =
            Citizen(
                ssn = vtjPerson.socialSecurityNumber,
                firstName = vtjPerson.firstNames,
                lastName = vtjPerson.lastName,
                dependantCount = vtjPerson.dependants.size
            )
    }
}

data class DevAssistanceFactor(
    val id: AssistanceFactorId = AssistanceFactorId(UUID.randomUUID()),
    val childId: ChildId,
    val validDuring: FiniteDateRange =
        FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 6, 1)),
    val capacityFactor: Double = 1.0,
    val modified: HelsinkiDateTime = HelsinkiDateTime.now(),
    val modifiedBy: EvakaUserId = AuthenticatedUser.SystemInternalUser.evakaUserId,
)

data class DevDaycareAssistance(
    val id: DaycareAssistanceId = DaycareAssistanceId(UUID.randomUUID()),
    val childId: ChildId,
    val validDuring: FiniteDateRange =
        FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 6, 1)),
    val level: DaycareAssistanceLevel = DaycareAssistanceLevel.GENERAL_SUPPORT,
    val modified: HelsinkiDateTime = HelsinkiDateTime.now(),
    val modifiedBy: EvakaUserId = AuthenticatedUser.SystemInternalUser.evakaUserId,
)

data class DevPreschoolAssistance(
    val id: PreschoolAssistanceId = PreschoolAssistanceId(UUID.randomUUID()),
    val childId: ChildId,
    val validDuring: FiniteDateRange =
        FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 6, 1)),
    val level: PreschoolAssistanceLevel = PreschoolAssistanceLevel.INTENSIFIED_SUPPORT,
    val modified: HelsinkiDateTime = HelsinkiDateTime.now(),
    val modifiedBy: EvakaUserId = AuthenticatedUser.SystemInternalUser.evakaUserId,
)

data class DevOtherAssistanceMeasure(
    val id: OtherAssistanceMeasureId = OtherAssistanceMeasureId(UUID.randomUUID()),
    val childId: ChildId,
    val validDuring: FiniteDateRange =
        FiniteDateRange(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 6, 1)),
    val type: OtherAssistanceMeasureType,
    val modified: HelsinkiDateTime = HelsinkiDateTime.now(),
    val modifiedBy: EvakaUserId = AuthenticatedUser.SystemInternalUser.evakaUserId,
)

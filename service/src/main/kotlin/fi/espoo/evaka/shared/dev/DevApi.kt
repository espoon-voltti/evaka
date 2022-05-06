// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationForm
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStateService
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.assistanceaction.AssistanceMeasure
import fi.espoo.evaka.attachment.AttachmentParent
import fi.espoo.evaka.attachment.insertAttachment
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.DaycareDecisionCustomization
import fi.espoo.evaka.daycare.MailingAddress
import fi.espoo.evaka.daycare.UnitManager
import fi.espoo.evaka.daycare.VisitingAddress
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.decision.Decision
import fi.espoo.evaka.decision.DecisionService
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.getDecision
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.decision.insertDecision
import fi.espoo.evaka.emailclient.MockEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.holidayperiod.FixedPeriodQuestionnaireBody
import fi.espoo.evaka.holidayperiod.HolidayPeriodBody
import fi.espoo.evaka.holidayperiod.createFixedPeriodQuestionnaire
import fi.espoo.evaka.holidayperiod.createHolidayPeriod
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
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
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
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.getEmployees
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.service.insertGuardian
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.placement.PlacementPlanService
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.DailyReservationRequest
import fi.espoo.evaka.reservations.createReservations
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.DocumentWrapper
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.sficlient.MockSfiMessagesClient
import fi.espoo.evaka.sficlient.SfiMessage
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.BackupCareId
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.FeeThresholdsId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.HolidayPeriodId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.VasuDocumentId
import fi.espoo.evaka.shared.VasuTemplateId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.SuomiFiAsyncJob
import fi.espoo.evaka.shared.async.VardaAsyncJob
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.CitizenAuthLevel
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.psqlCause
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.shared.security.upsertEmployeeUser
import fi.espoo.evaka.user.EvakaUser
import fi.espoo.evaka.user.EvakaUserType
import fi.espoo.evaka.vasu.CurriculumType
import fi.espoo.evaka.vasu.VasuLanguage
import fi.espoo.evaka.vasu.getDefaultTemplateContent
import fi.espoo.evaka.vasu.getVasuTemplate
import fi.espoo.evaka.vasu.insertVasuDocument
import fi.espoo.evaka.vasu.insertVasuTemplate
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.UnableToExecuteStatementException
import org.springframework.context.annotation.Profile
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
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.TimeUnit

private val fakeAdmin = AuthenticatedUser.Employee(
    id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    roles = setOf(UserRole.ADMIN)
)

private val logger = KotlinLogging.logger {}

@Profile("enable_dev_api")
@RestController
@RequestMapping("/dev-api")
@ExcludeCodeGen
class DevApi(
    private val personService: PersonService,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val vardaAsyncJobRunner: AsyncJobRunner<VardaAsyncJob>,
    private val suomiFiAsyncJobRunner: AsyncJobRunner<SuomiFiAsyncJob>,
    private val placementPlanService: PlacementPlanService,
    private val applicationStateService: ApplicationStateService,
    private val decisionService: DecisionService,
    private val documentClient: DocumentService,
    bucketEnv: BucketEnv
) {
    private val filesBucket = bucketEnv.attachments
    private val digitransit = MockDigitransit()

    private fun runAllAsyncJobs() {
        listOf(asyncJobRunner, vardaAsyncJobRunner, suomiFiAsyncJobRunner).forEach {
            it.runPendingJobsSync()
            it.waitUntilNoRunningJobs(timeout = Duration.ofSeconds(20))
        }
    }

    @GetMapping
    fun healthCheck() {
        // HTTP 200
    }

    @PostMapping("/reset-db")
    fun resetDatabase(db: Database) {
        // Run async jobs before database reset to avoid database locks/deadlocks
        runAllAsyncJobs()

        db.connect { dbc ->
            dbc.waitUntilNoQueriesRunning(timeout = Duration.ofSeconds(10))
            dbc.withLockedDatabase(timeout = Duration.ofSeconds(10)) {
                it.resetDatabase()

                // Terms are not inserted by fixtures
                it.runDevScript("preschool-terms.sql")
                it.runDevScript("club-terms.sql")
            }
        }
    }

    @PostMapping("/run-jobs")
    fun runJobs() {
        runAllAsyncJobs()
    }

    @PostMapping("/care-areas")
    fun createCareAreas(db: Database, @RequestBody careAreas: List<DevCareArea>) {
        db.connect { dbc -> dbc.transaction { careAreas.forEach { careArea -> it.insertTestCareArea(careArea) } } }
    }

    @PostMapping("/daycares")
    fun createDaycares(db: Database, @RequestBody daycares: List<DevDaycare>) {
        db.connect { dbc -> dbc.transaction { daycares.forEach { daycare -> it.insertTestDaycare(daycare) } } }
    }

    @PutMapping("/daycares/{daycareId}/acl")
    fun addAclRoleForDaycare(
        db: Database,
        @PathVariable daycareId: DaycareId,
        @RequestBody body: DaycareAclInsert
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.updateDaycareAcl(daycareId, body.externalId, body.role ?: UserRole.UNIT_SUPERVISOR)
            }
        }
    }

    @PostMapping("/daycare-group-acl")
    fun createDaycareGroupAclRows(db: Database, @RequestBody rows: List<DevDaycareGroupAcl>) {
        db.connect { dbc -> dbc.transaction { tx -> rows.forEach { tx.insertTestDaycareGroupAcl(it) } } }
    }

    @PostMapping("/daycare-groups")
    fun createDaycareGroups(db: Database, @RequestBody groups: List<DevDaycareGroup>) {
        db.connect { dbc ->
            dbc.transaction {
                groups.forEach { group -> it.insertTestDaycareGroup(group) }
            }
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
        db.connect { dbc ->
            dbc.transaction { tx ->
                children.forEach {
                    tx.insertTestChild(it)
                }
            }
        }
    }

    @PostMapping("/daycare-placements")
    fun createDaycarePlacements(db: Database, @RequestBody placements: List<DevPlacement>) {
        db.connect { dbc -> dbc.transaction { placements.forEach { placement -> it.insertTestPlacement(placement) } } }
    }

    data class DevTerminatePlacementRequest(
        val placementId: PlacementId,
        val endDate: LocalDate,
        val terminationRequestedDate: LocalDate?,
        val terminatedBy: EvakaUserId?
    )

    @PostMapping("/placement/terminate")
    fun terminatePlacement(db: Database, @RequestBody terminationRequest: DevTerminatePlacementRequest) {
        db.connect { dbc ->
            dbc.transaction { it.createUpdate("UPDATE placement SET end_date = :endDate, termination_requested_date = :terminationRequestedDate, terminated_by = :terminatedBy WHERE id = :placementId ") }
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
        val endDate: LocalDate
    )

    @PostMapping("/decisions")
    fun createDecisions(db: Database, @RequestBody decisions: List<DecisionRequest>) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                decisions.forEach { decision ->
                    tx.insertDecision(
                        decision.id,
                        EvakaUserId(decision.employeeId.raw),
                        LocalDate.now(ZoneId.of("Europe/Helsinki")),
                        decision.applicationId,
                        decision.unitId,
                        decision.type.toString(),
                        decision.startDate,
                        decision.endDate
                    )
                }
            }
        }
    }

    @PostMapping("/decisions/{id}/actions/create-pdf")
    fun createDecisionPdf(db: Database, @PathVariable id: DecisionId) {
        db.connect { dbc -> dbc.transaction { decisionService.createDecisionPdfs(it, fakeAdmin, id) } }
    }

    @PostMapping("/decisions/{id}/actions/reject-by-citizen")
    fun rejectDecisionByCitizen(
        db: Database,
        @PathVariable id: DecisionId,
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                val decision = tx.getDecision(id) ?: throw NotFound("decision not found")
                val application =
                    tx.fetchApplicationDetails(decision.applicationId) ?: throw NotFound("application not found")
                applicationStateService.rejectDecision(
                    tx,
                    AuthenticatedUser.Citizen(application.guardianId.raw, CitizenAuthLevel.STRONG),
                    application.id,
                    id,
                    isEnduser = true
                )
            }
        }
    }

    @GetMapping("/applications/{applicationId}")
    fun getApplication(
        db: Database,
        @PathVariable applicationId: ApplicationId
    ): ApplicationDetails {
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.fetchApplicationDetails(applicationId)
            }
        } ?: throw NotFound("application not found")
    }

    @GetMapping("/applications/{applicationId}/decisions")
    fun getApplicationDecisions(
        db: Database,
        @PathVariable applicationId: ApplicationId
    ): List<Decision> {
        return db.connect { dbc ->
            dbc.read { tx ->
                tx.getDecisionsByApplication(applicationId, AclAuthorization.All)
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
    fun createFeeThresholds(db: Database, @RequestBody feeThresholds: FeeThresholds): FeeThresholdsId =
        db.connect { dbc -> dbc.transaction { it.insertTestFeeThresholds(feeThresholds) } }

    data class DevCreateIncomeStatements(val personId: PersonId, val data: List<IncomeStatementBody>)

    @PostMapping("/income-statements")
    fun createIncomeStatements(db: Database, @RequestBody body: DevCreateIncomeStatements) =
        db.connect { dbc -> dbc.transaction { tx -> body.data.forEach { tx.createIncomeStatement(body.personId, it) } } }

    @PostMapping("/income")
    fun createIncome(db: Database, @RequestBody body: DevIncome) {
        db.connect { dbc -> dbc.transaction { it.insertTestIncome(body) } }
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
                tx.insertEvakaUser(EvakaUser(id = EvakaUserId(personId.raw), type = EvakaUserType.CITIZEN, name = "${body.lastName} ${body.firstName}"))
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
    fun createParentships(
        db: Database,
        @RequestBody parentships: List<DevParentship>
    ): List<DevParentship> {
        return db.connect { dbc -> dbc.transaction { tx -> parentships.map { tx.insertTestParentship(it) } } }
    }

    @GetMapping("/employee")
    fun getEmployees(db: Database): List<Employee> {
        return db.connect { dbc -> dbc.read { it.getEmployees() } }
    }

    @PostMapping("/employee")
    fun createEmployee(db: Database, @RequestBody body: DevEmployee): EmployeeId {
        return db.connect { dbc -> dbc.transaction { it.insertTestEmployee(body) } }
    }

    @DeleteMapping("/employee/external-id/{externalId}")
    fun deleteEmployeeByExternalId(db: Database, @PathVariable externalId: ExternalId) {
        db.connect { dbc -> dbc.transaction { it.deleteAndCascadeEmployeeByExternalId(externalId) } }
    }

    @PostMapping("/employee/external-id/{externalId}")
    fun upsertEmployeeByExternalId(
        db: Database,
        @PathVariable externalId: ExternalId,
        @RequestBody employee: DevEmployee
    ): EmployeeId = db.connect { dbc ->
        dbc.transaction {
            it.createUpdate(
                """
INSERT INTO employee (first_name, last_name, email, external_id, roles)
VALUES (:firstName, :lastName, :email, :externalId, :roles::user_role[])
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
                .mapTo<EmployeeId>()
                .single()
        }
    }

    data class DevGuardian(
        val guardianId: PersonId,
        val childId: ChildId
    )

    @PostMapping("/guardian")
    fun insertGuardians(db: Database, @RequestBody guardians: List<DevGuardian>) {
        db.connect { dbc ->
            dbc.transaction {
                guardians.forEach { guardian ->
                    it.insertGuardian(guardian.guardianId, guardian.childId)
                }
            }
        }
    }

    @PostMapping("/child")
    fun insertChild(db: Database, @RequestBody body: DevPerson): ChildId = db.connect { dbc ->
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
            ).also { id -> it.insertTestChild(DevChild(id = id)) }
        }
    }

    @PostMapping("/message-account/upsert-all")
    fun createMessageAccounts(db: Database) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.execute("INSERT INTO message_account (daycare_group_id) SELECT id FROM daycare_group ON CONFLICT DO NOTHING")
                tx.execute("INSERT INTO message_account (person_id) SELECT id FROM person ON CONFLICT DO NOTHING")
                tx.execute("INSERT INTO message_account (employee_id) SELECT id FROM employee ON CONFLICT DO NOTHING")
            }
        }
    }

    @PostMapping("/backup-cares")
    fun createBackupCares(db: Database, @RequestBody backupCares: List<DevBackupCare>) {
        db.connect { dbc -> dbc.transaction { tx -> backupCares.forEach { tx.insertTestBackupCare(it) } } }
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
                            document = DaycareFormV0.fromForm2(application.form, application.type, false, false)
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
                val application = tx.fetchApplicationDetails(applicationId)
                    ?: throw NotFound("application $applicationId not found")
                val preschoolDaycarePeriod = if (placementPlan.preschoolDaycarePeriodStart != null) FiniteDateRange(
                    placementPlan.preschoolDaycarePeriodStart, placementPlan.preschoolDaycarePeriodEnd!!
                ) else null

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
        return MockSfiMessagesClient.getMessages()
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
                val uuid = tx.createQuery("SELECT id FROM person WHERE social_security_number = :ssn")
                    .bind("ssn", person.socialSecurityNumber)
                    .mapTo<PersonId>()
                    .firstOrNull()

                uuid?.let {
                    // Refresh Pis data by forcing refresh from VTJ
                    personService.getUpToDatePersonFromVtj(tx, AuthenticatedUser.SystemInternalUser, it)
                }
            }
        }
    }

    @GetMapping("/vtj-persons/{ssn}")
    fun getVtjPerson(@PathVariable ssn: String): VtjPerson {
        return MockPersonDetailsService.getPerson(ssn)
            ?: throw NotFound("vtj person $ssn was not found")
    }

    @GetMapping("/application-emails")
    fun getApplicationEmails(): List<MockEmail> {
        return MockEmailClient.emails
    }

    @PostMapping("/applications/{applicationId}/actions/{action}")
    fun simpleAction(
        db: Database,
        @PathVariable applicationId: ApplicationId,
        @PathVariable action: String
    ) {
        val simpleActions = mapOf(
            "move-to-waiting-placement" to applicationStateService::moveToWaitingPlacement,
            "cancel-application" to applicationStateService::cancelApplication,
            "set-verified" to applicationStateService::setVerified,
            "set-unverified" to applicationStateService::setUnverified,
            "send-decisions-without-proposal" to applicationStateService::sendDecisionsWithoutProposal,
            "send-placement-proposal" to applicationStateService::sendPlacementProposal,
            "confirm-decision-mailed" to applicationStateService::confirmDecisionMailed
        )

        val actionFn = simpleActions[action] ?: throw NotFound("Action not recognized")
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.ensureFakeAdminExists()
                actionFn.invoke(tx, fakeAdmin, applicationId)
            }
        }
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
        @PathVariable applicationId: ApplicationId
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.ensureFakeAdminExists()
                placementPlanService.getPlacementPlanDraft(tx, applicationId)
                    .let {
                        DaycarePlacementPlan(
                            unitId = it.preferredUnits.first().id,
                            period = it.period,
                            preschoolDaycarePeriod = it.preschoolDaycarePeriod
                        )
                    }
                    .let { applicationStateService.createPlacementPlan(tx, fakeAdmin, applicationId, it) }
            }
        }
    }

    @PostMapping("/mobile/pairings/challenge")
    fun postPairingChallenge(
        db: Database,
        @RequestBody body: PairingsController.PostPairingChallengeReq
    ): Pairing {
        return db.connect { dbc -> dbc.transaction { it.challengePairing(body.challengeKey) } }
    }

    @PostMapping("/mobile/pairings/{id}/response")
    fun postPairingResponse(
        db: Database,
        @PathVariable id: PairingId,
        @RequestBody body: PairingsController.PostPairingResponseReq
    ): Pairing {
        return db.connect { dbc ->
            dbc.transaction { it.incrementAttempts(id, body.challengeKey) }
            dbc.transaction { it.respondPairingChallengeCreateDevice(id, body.challengeKey, body.responseKey) }
        }
    }

    @PostMapping("/mobile/pairings")
    fun postPairing(
        db: Database,
        @RequestBody body: PairingsController.PostPairingReq
    ): Pairing {
        return db.connect { dbc ->
            dbc.transaction {
                when (body) {
                    is PairingsController.PostPairingReq.Unit -> it.initPairing(unitId = body.unitId)
                    is PairingsController.PostPairingReq.Employee -> it.initPairing(employeeId = body.employeeId)
                }
            }
        }
    }

    @PostMapping("/mobile/devices")
    fun postMobileDevice(
        db: Database,
        @RequestBody body: DevMobileDevice
    ) {
        db.connect { dbc ->
            dbc.transaction {
                it.insertTestMobileDevice(body)
            }
        }
    }

    @PostMapping("/holiday-period/{id}")
    fun createHolidayPeriod(
        db: Database,
        @PathVariable id: HolidayPeriodId,
        @RequestBody body: HolidayPeriodBody
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.createHolidayPeriod(body).let {
                    tx.createUpdate("UPDATE holiday_period SET id = :id WHERE id = :prevId")
                        .bind("id", id)
                        .bind("prevId", it.id)
                        .execute()
                }
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
                    tx.createUpdate("UPDATE holiday_period_questionnaire SET id = :id WHERE id = :prevId")
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
        @RequestBody body: List<DailyReservationRequest>
    ) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                tx.ensureFakeAdminExists()
                createReservations(tx, fakeAdmin.id, body)
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
            dbc.transaction {
                it.createChildDailyNote(
                    childId = childId,
                    note = body
                )
            }
        }
    }

    @PostMapping("/children/{childId}/child-sticky-notes")
    fun postChildStickyNote(
        db: Database,
        @PathVariable childId: ChildId,
        @RequestBody body: ChildStickyNoteBody
    ): ChildStickyNoteId {
        return db.connect { dbc ->
            dbc.transaction {
                it.createChildStickyNote(
                    childId = childId,
                    note = body
                )
            }
        }
    }

    @PostMapping("/daycare-groups/{groupId}/group-notes")
    fun postGroupNote(
        db: Database,
        @PathVariable groupId: GroupId,
        @RequestBody body: GroupNoteBody
    ): GroupNoteId {
        return db.connect { dbc ->
            dbc.transaction {
                it.createGroupNote(
                    groupId = groupId,
                    note = body
                )
            }
        }
    }

    @GetMapping("/digitransit/autocomplete")
    fun digitransitAutocomplete() = digitransit.autocomplete()

    @PutMapping("/digitransit/autocomplete")
    fun putDigitransitAutocomplete(@RequestBody mockResponse: MockDigitransit.Autocomplete) =
        digitransit.setAutocomplete(mockResponse)

    @PostMapping("/family-contact")
    fun createFamilyContact(db: Database, @RequestBody contacts: List<DevFamilyContact>) {
        db.connect { dbc -> dbc.transaction { contacts.forEach { contact -> it.insertFamilyContact(contact) } } }
    }

    @PostMapping("/backup-pickup")
    fun createBackupPickup(db: Database, @RequestBody backupPickups: List<DevBackupPickup>) {
        db.connect { dbc -> dbc.transaction { backupPickups.forEach { backupPickup -> it.insertBackupPickup(backupPickup) } } }
    }

    @PostMapping("/fridge-child")
    fun createFridgeChild(db: Database, @RequestBody fridgeChildren: List<DevFridgeChild>) {
        db.connect { dbc -> dbc.transaction { fridgeChildren.forEach { child -> it.insertFridgeChild(child) } } }
    }

    @PostMapping("/fridge-partner")
    fun createFridgePartner(db: Database, @RequestBody fridgePartners: List<DevFridgePartner>) {
        db.connect { dbc -> dbc.transaction { fridgePartners.forEach { partner -> it.insertFridgePartner(partner) } } }
    }

    @PostMapping("/employee-pin")
    fun createEmployeePins(db: Database, @RequestBody employeePins: List<DevEmployeePin>) {
        db.connect { dbc ->
            dbc.transaction {
                employeePins.forEach { employeePin ->
                    val userId =
                        if (employeePin.userId != null) employeePin.userId
                        else if (!employeePin.employeeExternalId.isNullOrBlank()) it.getEmployeeIdByExternalId(employeePin.employeeExternalId)
                        else throw Error("Cannot create dev employee pin: user id and external user id missing")

                    it.insertEmployeePin(employeePin.copy(userId = userId))
                }
            }
        }
    }

    @PostMapping("/pedagogical-document")
    fun createPedagogicalDocuments(db: Database, @RequestBody pedagogicalDocuments: List<DevPedagogicalDocument>) {
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
                    AuthenticatedUser.Employee(employeeId.raw, emptySet()),
                    AttachmentId(id),
                    file.name,
                    file.contentType ?: "image/jpeg",
                    AttachmentParent.PedagogicalDocument(pedagogicalDocumentId),
                    type = null
                )
                documentClient.upload(
                    filesBucket,
                    DocumentWrapper(
                        name = id.toString(),
                        bytes = file.bytes
                    ),
                    file.contentType ?: "image/jpeg"
                )
            }
        }.key
    }

    @PostMapping("/vasu/template")
    fun createVasuTemplate(db: Database): VasuTemplateId {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                tx.insertVasuTemplate(
                    name = "testipohja",
                    valid = FiniteDateRange(LocalDate.ofYearDay(2020, 1), LocalDate.ofYearDay(2200, 1)),
                    type = CurriculumType.DAYCARE,
                    language = VasuLanguage.FI,
                    content = getDefaultTemplateContent(CurriculumType.DAYCARE, VasuLanguage.FI)
                )
            }
        }
    }

    @DeleteMapping("/vasu/templates")
    fun deleteVasuTemplates(db: Database) {
        db.connect { dbc -> dbc.transaction { it.createUpdate("DELETE FROM curriculum_template").execute() } }
    }

    @PostMapping("/vasu/doc")
    fun createVasuDocument(db: Database, @RequestBody body: PostVasuDocBody): VasuDocumentId {
        return db.connect { dbc ->
            dbc.transaction { tx ->
                val template = tx.getVasuTemplate(body.templateId)
                    ?: throw NotFound("Template with id ${body.templateId} not found")
                tx.insertVasuDocument(body.childId, template)
            }
        }
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
    fun createServiceNeedOption(db: Database, @RequestBody serviceNeedOptions: List<ServiceNeedOption>) {
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
        db.connect { dbc -> dbc.transaction { it.insertVoucherValues() } }
    }

    @PostMapping("/assistance-needs")
    fun createAssistanceNeeds(db: Database, @RequestBody assistanceNeeds: List<DevAssistanceNeed>) {
        db.connect { dbc ->
            dbc.transaction { tx ->
                assistanceNeeds.forEach {
                    tx.insertTestAssistanceNeed(it)
                }
            }
        }
    }

    @PostMapping("/attendances")
    fun postAttendances(
        db: Database,
        @RequestBody attendances: List<DevChildAttendance>
    ) = db.connect { dbc ->
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

    data class DevVardaReset(
        val evakaChildId: ChildId,
        val resetTimestamp: Instant?
    )

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
    fun upsertStaffOccupancyCoefficient(db: Database, @RequestBody body: DevUpsertStaffOccupancyCoefficient) {
        db.connect { dbc ->
            dbc.transaction {
                it.createUpdate(
                    """
                    INSERT INTO staff_occupancy_coefficient (daycare_id, employee_id, coefficient)
                    VALUES (:unitId, :employeeId, :coefficient)
                    ON CONFLICT (daycare_id, employee_id) DO UPDATE SET coefficient = EXCLUDED.coefficient
                    """.trimIndent()
                )
                    .bindKotlin(body)
                    .execute()
            }
        }
    }
}

// https://www.postgresql.org/docs/14/errcodes-appendix.html
// Class 55 — Object Not In Prerequisite State: lock_not_available
private const val LOCK_NOT_AVAILABLE: String = "55P03"

private fun <T> Database.Connection.withLockedDatabase(timeout: Duration, f: (tx: Database.Transaction) -> T): T {
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

private data class ActiveConnection(val state: String, val xactStart: Instant?, val queryStart: Instant?, val query: String?)

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

private fun Database.Read.getActiveConnections(): List<ActiveConnection> = createQuery(
    """
SELECT state, xact_start, query_start, left(query, 100) AS query FROM pg_stat_activity
WHERE pid <> pg_backend_pid() AND datname = current_database() AND usename = current_user AND backend_type = 'client backend'
AND state != 'idle'
    """.trimIndent()
).mapTo<ActiveConnection>().list()

fun Database.Transaction.ensureFakeAdminExists() {
    // language=sql
    val sql =
        """
        INSERT INTO employee (id, first_name, last_name, email, external_id, roles)
        VALUES (:id, 'Dev', 'API', 'dev.api@espoo.fi', 'espoo-ad:' || :id, '{ADMIN, SERVICE_WORKER}'::user_role[])
        ON CONFLICT DO NOTHING
        """.trimIndent()

    createUpdate(sql).bind("id", fakeAdmin.id).execute()
    upsertEmployeeUser(EmployeeId(fakeAdmin.id))
}

fun Database.Transaction.deleteAndCascadeEmployee(id: EmployeeId) {
    execute("DELETE FROM message_account WHERE employee_id = ?", id)
    execute("DELETE FROM employee_pin WHERE user_id = ?", id)
    execute("DELETE FROM employee WHERE id = ?", id)
}

fun Database.Transaction.deleteAndCascadeEmployeeByExternalId(externalId: ExternalId) {
    val employeeId = createQuery("SELECT id FROM employee WHERE external_id = :externalId")
        .bind("externalId", externalId)
        .mapTo<EmployeeId>()
        .findOne()
    if (employeeId.isPresent) {
        deleteAndCascadeEmployee(employeeId.get())
    }
}

fun Database.Transaction.insertServiceNeedOptions() {
    execute(
        """
INSERT INTO service_need_option (name_fi, name_sv, name_en, valid_placement_type, default_option, fee_coefficient, voucher_value_coefficient, occupancy_coefficient, occupancy_coefficient_under_3y, daycare_hours_per_week, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv) VALUES
    ('Kokopäiväinen', 'Kokopäiväinen', 'Kokopäiväinen', 'DAYCARE', TRUE, 1.0, 1.0, 1.0, 1.75, 35, FALSE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', 'yli 25h/viikko', 'mer än 25 h/vecka'),
    ('Osapäiväinen', 'Osapäiväinen', 'Osapäiväinen', 'DAYCARE_PART_TIME', TRUE, 0.6, 0.6, 0.54, 1.75, 25, TRUE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', 'korkeintaan 25 h/viikko', 'högst 25 h/vecka'),
    ('Viisivuotiaiden kokopäiväinen', 'Viisivuotiaiden kokopäiväinen', 'Viisivuotiaiden kokopäiväinen', 'DAYCARE_FIVE_YEAR_OLDS', TRUE, 0.8, 1.0, 1.0, 1.75, 45, FALSE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', 'yli 25 h/viikko', 'mer än 25 h/vecka'),
    ('Viisivuotiaiden osapäiväinen', 'Viisivuotiaiden osapäiväinen', 'Viisivuotiaiden osapäiväinen', 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS', TRUE, 0.0, 0.6, 0.5, 1.75, 20, TRUE, FALSE, 'ei maksullista varhaiskasvatusta', 'ingen avgiftsbelagd småbarnspedagogik', 'korkeintaan 25 h/viikko', 'högst 25 h/vecka'),
    ('Esiopetus', 'Esiopetus', 'Esiopetus', 'PRESCHOOL', TRUE, 0.0, 0.5, 0.5, 1.75, 0, TRUE, FALSE, 'ei maksullista varhaiskasvatusta', 'ingen avgiftsbelagd småbarnspedagogik', '', ''),
    ('Esiopetus ja liittyvä varhaiskasvatus', 'Esiopetus ja liittyvä varhaiskasvatus', 'Esiopetus ja liittyvä varhaiskasvatus', 'PRESCHOOL_DAYCARE', TRUE, 0.8, 0.5, 1.0, 1.75, 25, TRUE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', '', ''),
    ('Valmistava opetus', 'Valmistava opetus', 'Valmistava opetus', 'PREPARATORY', TRUE, 0.0, 0.5, 0.5, 1.75, 0, TRUE, FALSE, 'ei maksullista varhaiskasvatusta', 'ingen avgiftsbelagd småbarnspedagogik', '', ''),
    ('Valmistava opetus ja liittyvä varhaiskasvatus', 'Valmistava opetus ja liittyvä varhaiskasvatus', 'Valmistava opetus ja liittyvä varhaiskasvatus', 'PREPARATORY_DAYCARE', TRUE, 0.8, 0.5, 1.0, 1.75, 25, TRUE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', '', ''),
    ('Kerho', 'Kerho', 'Kerho', 'CLUB', TRUE, 0.0, 0.0, 1.0, 1.75, 0, TRUE, TRUE, '', '', '', ''),
    ('Kokopäiväinen tilapäinen', 'Kokopäiväinen tilapäinen', 'Kokopäiväinen tilapäinen', 'TEMPORARY_DAYCARE', TRUE, 1.0, 0.0, 1.0, 1.75, 35, FALSE, TRUE, '', '', '', ''),
    ('Osapäiväinen tilapäinen', 'Osapäiväinen tilapäinen', 'Osapäiväinen tilapäinen', 'TEMPORARY_DAYCARE_PART_DAY', TRUE, 0.5, 0.0, 0.54, 1.75, 25, TRUE, TRUE, '', '', '', '')
"""
    )
}

fun Database.Transaction.insertVoucherValues() {
    execute(
        """
INSERT INTO voucher_value (id, validity, base_value, base_value_age_under_three) VALUES ('084314dc-ed7f-4725-92f2-5c220bb4bb7e', daterange('2000-01-01', NULL, '[]'), 87000, 134850);
"""
    )
}

fun Database.Transaction.updateFeeDecisionSentAt(feeDecision: FeeDecision) = createUpdate(
    """
UPDATE fee_decision SET sent_at = :sentAt WHERE id = :id    
    """.trimIndent()
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
    val additionalInfo: String = ""
)

data class DevDaycare(
    val id: DaycareId = DaycareId(UUID.randomUUID()),
    val name: String = "Test Daycare",
    val openingDate: LocalDate? = null,
    val closingDate: LocalDate? = null,
    val areaId: AreaId,
    val type: Set<CareType> = setOf(CareType.CENTRE, CareType.PRESCHOOL, CareType.PREPARATORY_EDUCATION),
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
    val additionalInfo: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val url: String? = null,
    val visitingAddress: VisitingAddress = VisitingAddress(
        streetAddress = "Joku katu 9",
        postalCode = "02100",
        postOffice = "ESPOO"
    ),
    val location: Coordinate? = null,
    val mailingAddress: MailingAddress = MailingAddress(),
    val unitManager: UnitManager = UnitManager(
        name = "Unit Manager",
        phone = null,
        email = null
    ),
    val decisionCustomization: DaycareDecisionCustomization = DaycareDecisionCustomization(
        daycareName = name,
        preschoolName = name,
        handler = "Decision Handler",
        handlerAddress = "Decision Handler Street 1"
    ),
    val ophUnitOid: String? = "1.2.3.4.5",
    val ophOrganizerOid: String? = "1.2.3.4.5",
    val roundTheClock: Boolean? = false,
    val operationDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val enabledPilotFeatures: Set<PilotFeature> = setOf(),
    val financeDecisionHandler: EmployeeId? = null
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
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class DevAssistanceNeed(
    val id: AssistanceNeedId = AssistanceNeedId(UUID.randomUUID()),
    val childId: ChildId,
    val updatedBy: EvakaUserId,
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate = LocalDate.of(2019, 12, 31),
    val capacityFactor: Double = 1.0,
    val bases: Set<String> = emptySet(),
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
    val updatedBy: EvakaUserId,
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate = LocalDate.of(2019, 12, 31),
    val actions: Set<String> = emptySet(),
    val measures: Set<AssistanceMeasure> = emptySet(),
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
    val terminatedBy: EvakaUserId? = null
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
    val ophPersonOid: String = ""
) {
    fun toPersonDTO() = PersonDTO(
        id = this.id,
        identity = this.ssn?.let { ExternalIdentifier.SSN.getInstance(it) } ?: ExternalIdentifier.NoID,
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
    val id: ParentshipId?,
    val childId: ChildId,
    val headOfChildId: PersonId,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class DevEmployee(
    val id: EmployeeId = EmployeeId(UUID.randomUUID()),
    val firstName: String = "Test",
    val lastName: String = "Person",
    val email: String? = "test.person@espoo.fi",
    val externalId: ExternalId? = null,
    val roles: Set<UserRole> = setOf(),
    val lastLogin: HelsinkiDateTime? = HelsinkiDateTime.now()
)

data class DevMobileDevice(
    val id: MobileDeviceId = MobileDeviceId(UUID.randomUUID()),
    val unitId: DaycareId,
    val name: String = "Laite",
    val longTermToken: UUID? = null
)

data class DaycareAclInsert(
    val externalId: ExternalId,
    val role: UserRole?
)

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
    val createdDate: OffsetDateTime?,
    val modifiedDate: OffsetDateTime?,
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
    val form: ApplicationForm
)

data class DevApplicationForm(
    val id: UUID? = UUID.randomUUID(),
    val applicationId: ApplicationId,
    val createdDate: OffsetDateTime? = OffsetDateTime.now(),
    val revision: Int,
    val document: DaycareFormV0,
    val updated: OffsetDateTime? = OffsetDateTime.now()
)

data class DevDaycareGroupAcl(val groupId: GroupId, val employeeId: EmployeeId)

data class DevServiceNeed(
    val id: ServiceNeedId,
    val placementId: PlacementId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val optionId: ServiceNeedOptionId,
    val shiftCare: Boolean,
    val confirmedBy: EvakaUserId,
    val confirmedAt: LocalDate? = null
)

data class PostVasuDocBody(
    val childId: ChildId,
    val templateId: VasuTemplateId
)

data class DevGuardianBlocklistEntry(val guardianId: PersonId, val childId: ChildId)

data class DevUpsertStaffOccupancyCoefficient(
    val unitId: DaycareId,
    val employeeId: EmployeeId,
    val coefficient: BigDecimal
)

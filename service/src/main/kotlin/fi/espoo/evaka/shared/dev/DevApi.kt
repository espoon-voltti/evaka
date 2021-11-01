// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import com.fasterxml.jackson.module.kotlin.treeToValue
import fi.espoo.evaka.BucketEnv
import fi.espoo.evaka.ExcludeCodeGen
import fi.espoo.evaka.application.ApplicationDetails
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStateService
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.application.persistence.objectMapper
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
import fi.espoo.evaka.decision.getDecisionsByApplication
import fi.espoo.evaka.decision.insertDecision
import fi.espoo.evaka.emailclient.MockEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.incomestatement.IncomeStatementBody
import fi.espoo.evaka.incomestatement.createIncomeStatement
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
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
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.placement.PlacementPlanService
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.s3.DocumentService
import fi.espoo.evaka.s3.DocumentWrapper
import fi.espoo.evaka.serviceneed.ServiceNeedOption
import fi.espoo.evaka.shared.ApplicationId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.AssistanceActionId
import fi.espoo.evaka.shared.AssistanceNeedId
import fi.espoo.evaka.shared.AttachmentId
import fi.espoo.evaka.shared.ChildDailyNoteId
import fi.espoo.evaka.shared.ChildStickyNoteId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.DecisionId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupNoteId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.MobileDeviceId
import fi.espoo.evaka.shared.PairingId
import fi.espoo.evaka.shared.ParentshipId
import fi.espoo.evaka.shared.PedagogicalDocumentId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AclAuthorization
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.MockEvakaMessageClient
import fi.espoo.evaka.shared.message.SuomiFiMessage
import fi.espoo.evaka.shared.security.PilotFeature
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.context.annotation.Profile
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
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

private val fakeAdmin = AuthenticatedUser.Employee(
    id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
    roles = setOf(UserRole.ADMIN)
)

@Profile("enable_dev_api")
@RestController
@RequestMapping("/dev-api")
@ExcludeCodeGen
class DevApi(
    private val personService: PersonService,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val placementPlanService: PlacementPlanService,
    private val applicationStateService: ApplicationStateService,
    private val decisionService: DecisionService,
    private val documentClient: DocumentService,
    bucketEnv: BucketEnv
) {
    private val filesBucket = bucketEnv.attachments
    private val digitransit = MockDigitransit()

    @GetMapping
    fun healthCheck(): ResponseEntity<Unit> {
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/reset-db")
    fun resetDatabase(db: Database.Connection): ResponseEntity<Unit> {
        // Run async jobs before database reset to avoid database locks/deadlocks
        asyncJobRunner.runPendingJobsSync()
        asyncJobRunner.waitUntilNoRunningJobs(timeout = Duration.ofSeconds(20))

        db.transaction {
            it.resetDatabase()

            // Terms are not inserted by fixtures
            it.runDevScript("preschool-terms.sql")
            it.runDevScript("club-terms.sql")
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/run-jobs")
    fun runJobs(): ResponseEntity<Unit> {
        asyncJobRunner.runPendingJobsSync()
        asyncJobRunner.waitUntilNoRunningJobs(timeout = Duration.ofSeconds(20))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/care-areas")
    fun createCareAreas(db: Database.Connection, @RequestBody careAreas: List<DevCareArea>): ResponseEntity<Unit> {
        db.transaction { careAreas.forEach { careArea -> it.insertTestCareArea(careArea) } }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/daycares")
    fun createDaycares(db: Database.Connection, @RequestBody daycares: List<DevDaycare>): ResponseEntity<Unit> {
        db.transaction { daycares.forEach { daycare -> it.insertTestDaycare(daycare) } }
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/daycares/{daycareId}/acl")
    fun addAclRoleForDaycare(
        db: Database.Connection,
        @PathVariable daycareId: DaycareId,
        @RequestBody body: DaycareAclInsert
    ): ResponseEntity<Unit> {
        db.transaction { tx ->
            tx.updateDaycareAcl(daycareId, body.externalId, body.role ?: UserRole.UNIT_SUPERVISOR)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/daycare-group-acl")
    fun createDaycareGroupAclRows(db: Database.Connection, @RequestBody rows: List<DevDaycareGroupAcl>) {
        db.transaction { tx -> rows.forEach { tx.insertTestDaycareGroupAcl(it) } }
    }

    @PostMapping("/daycare-groups")
    fun createDaycareGroups(db: Database.Connection, @RequestBody groups: List<DevDaycareGroup>): ResponseEntity<Unit> {
        db.transaction {
            groups.forEach { group -> it.insertTestDaycareGroup(group) }
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/daycare-group-placements")
    fun createDaycareGroupPlacement(
        db: Database.Connection,
        @RequestBody placements: List<DevDaycareGroupPlacement>
    ): ResponseEntity<Unit> {
        db.transaction { tx ->
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
        return ResponseEntity.noContent().build()
    }

    data class Caretaker(
        val groupId: GroupId,
        val amount: Double,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )

    @PostMapping("/daycare-caretakers")
    fun createDaycareCaretakers(db: Database.Connection, @RequestBody caretakers: List<Caretaker>): ResponseEntity<Unit> {
        db.transaction { tx ->
            caretakers.forEach { caretaker ->
                tx.insertTestCaretakers(
                    caretaker.groupId,
                    amount = caretaker.amount,
                    startDate = caretaker.startDate,
                    endDate = caretaker.endDate
                )
            }
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/children")
    fun createChildren(db: Database.Connection, @RequestBody children: List<DevChild>): ResponseEntity<Unit> {
        db.transaction { tx ->
            children.forEach {
                tx.insertTestChild(it)
            }
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/daycare-placements")
    fun createDaycarePlacements(db: Database.Connection, @RequestBody placements: List<DevPlacement>): ResponseEntity<Unit> {
        db.transaction { placements.forEach { placement -> it.insertTestPlacement(placement) } }
        return ResponseEntity.noContent().build()
    }

    data class DecisionRequest(
        val id: DecisionId,
        val employeeId: UUID,
        val applicationId: ApplicationId,
        val unitId: DaycareId,
        val type: DecisionType,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    @PostMapping("/decisions")
    fun createDecisions(db: Database.Connection, @RequestBody decisions: List<DecisionRequest>): ResponseEntity<Unit> {
        db.transaction { tx ->
            decisions.forEach { decision ->
                tx.insertDecision(
                    decision.id,
                    decision.employeeId,
                    LocalDate.now(ZoneId.of("Europe/Helsinki")),
                    decision.applicationId,
                    decision.unitId,
                    decision.type.toString(),
                    decision.startDate,
                    decision.endDate
                )
            }
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/decisions/{id}/actions/create-pdf")
    fun createDecisionPdf(db: Database.Connection, @PathVariable id: DecisionId): ResponseEntity<Unit> {
        db.transaction { decisionService.createDecisionPdfs(it, fakeAdmin, id) }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/applications/{applicationId}")
    fun getApplication(
        db: Database.Connection,
        @PathVariable applicationId: ApplicationId
    ): ResponseEntity<ApplicationDetails> {
        return db.read { tx ->
            tx.fetchApplicationDetails(applicationId)
        }?.let { ResponseEntity.ok(it) } ?: throw NotFound("application not found")
    }

    @GetMapping("/applications/{applicationId}/decisions")
    fun getApplicationDecisions(
        db: Database.Connection,
        @PathVariable applicationId: ApplicationId
    ): ResponseEntity<List<Decision>> {
        return db.read { tx ->
            tx.getDecisionsByApplication(applicationId, AclAuthorization.All)
        }.let { ResponseEntity.ok(it) }
    }

    @PostMapping("/fee-decisions")
    fun createFeeDecisions(db: Database.Connection, @RequestBody decisions: List<FeeDecision>): ResponseEntity<Unit> {
        db.transaction { tx ->
            tx.upsertFeeDecisions(decisions)
            decisions.forEach { fd ->
                if (fd.sentAt != null) {
                    tx.updateFeeDecisionSentAt(fd)
                }
            }
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/value-decisions")
    fun createVoucherValueDecisions(
        db: Database.Connection,
        @RequestBody decisions: List<VoucherValueDecision>
    ): ResponseEntity<Unit> {
        db.transaction { tx -> tx.upsertValueDecisions(decisions) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/invoices")
    fun createInvoices(db: Database.Connection, @RequestBody invoices: List<Invoice>): ResponseEntity<Unit> {
        db.transaction { tx -> tx.upsertInvoices(invoices) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/fee-thresholds")
    fun createFeeThresholds(db: Database.Connection, @RequestBody feeThresholds: FeeThresholds): ResponseEntity<UUID> =
        db.transaction {
            ResponseEntity.ok(
                it.insertTestFeeThresholds(feeThresholds)
            )
        }

    data class DevCreateIncomeStatements(val personId: UUID, val data: List<IncomeStatementBody>)
    @PostMapping("/income-statements")
    fun createIncomeStatements(db: Database.Connection, @RequestBody body: DevCreateIncomeStatements) =
        db.transaction { tx -> body.data.forEach { tx.createIncomeStatement(body.personId, it) } }

    @PostMapping("/person")
    fun upsertPerson(db: Database.Connection, @RequestBody body: DevPerson): ResponseEntity<PersonDTO> {
        if (body.ssn == null) throw BadRequest("SSN is required for using this endpoint")
        return db.transaction { tx ->
            val person = tx.getPersonBySSN(body.ssn)
            val personDTO = body.toPersonDTO()

            if (person != null) {
                tx.updatePersonFromVtj(personDTO).let { ResponseEntity.ok(it) }
            } else {
                createPersonFromVtj(tx, personDTO).let { ResponseEntity.ok(it) }
            }
        }
    }

    @PostMapping("/person/create")
    fun createPerson(db: Database.Connection, @RequestBody body: DevPerson): ResponseEntity<UUID> {
        return db.transaction { tx ->
            val personId = tx.insertTestPerson(body)
            val dto = body.copy(id = personId).toPersonDTO()
            if (dto.identity is ExternalIdentifier.SSN) {
                tx.updatePersonFromVtj(dto)
            }
            ResponseEntity.ok(personId)
        }
    }

    @PostMapping("/parentship")
    fun createParentships(
        db: Database.Connection,
        @RequestBody parentships: List<DevParentship>
    ): ResponseEntity<List<DevParentship>> {
        return db.transaction { tx -> parentships.map { tx.insertTestParentship(it) } }
            .let { ResponseEntity.ok(it) }
    }

    @GetMapping("/employee")
    fun getEmployees(db: Database.Connection): ResponseEntity<List<Employee>> {
        return ResponseEntity.ok(db.read { it.getEmployees() })
    }

    @PostMapping("/employee")
    fun createEmployee(db: Database.Connection, @RequestBody body: DevEmployee): ResponseEntity<UUID> {
        return ResponseEntity.ok(db.transaction { it.insertTestEmployee(body) })
    }

    @DeleteMapping("/employee/external-id/{externalId}")
    fun deleteEmployeeByExternalId(db: Database.Connection, @PathVariable externalId: ExternalId): ResponseEntity<Unit> {
        db.transaction { it.deleteAndCascadeEmployeeByExternalId(externalId) }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/employee/external-id/{externalId}")
    fun upsertEmployeeByExternalId(
        db: Database.Connection,
        @PathVariable externalId: ExternalId,
        @RequestBody employee: DevEmployee
    ): ResponseEntity<UUID> = db.transaction {
        ResponseEntity.ok(
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
            ).bindKotlin(employee).executeAndReturnGeneratedKeys().mapTo<UUID>().single()
        )
    }

    @PostMapping("/child")
    fun insertChild(db: Database.Connection, @RequestBody body: DevPerson): ResponseEntity<UUID> = db.transaction {
        val id = it.insertTestPerson(
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
        it.insertTestChild(DevChild(id = id))
        ResponseEntity.ok(id)
    }

    @PostMapping("/message-account/upsert-all")
    fun createMessageAccounts(db: Database.Connection) {
        db.transaction { tx ->
            tx.execute("INSERT INTO message_account (daycare_group_id) SELECT id FROM daycare_group ON CONFLICT DO NOTHING")
            tx.execute("INSERT INTO message_account (person_id) SELECT id FROM person ON CONFLICT DO NOTHING")
            tx.execute("INSERT INTO message_account (employee_id) SELECT id FROM employee ON CONFLICT DO NOTHING")
        }
    }

    @PostMapping("/backup-cares")
    fun createBackupCares(db: Database.Connection, @RequestBody backupCares: List<DevBackupCare>): ResponseEntity<Unit> {
        db.transaction { tx -> backupCares.forEach { tx.insertTestBackupCare(it) } }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/applications")
    fun createApplications(
        db: Database.Connection,
        @RequestBody applications: List<ApplicationWithForm>
    ): ResponseEntity<List<ApplicationId>> {
        val uuids =
            db.transaction { tx ->
                applications.map { application ->
                    val id = tx.insertApplication(application)
                    application.form?.let { applicationFormString ->
                        tx.insertApplicationForm(
                            ApplicationForm(
                                applicationId = id,
                                revision = 1,
                                document = deserializeApplicationForm(applicationFormString)
                            )
                        )
                    }
                    id
                }
            }
        return ResponseEntity.ok(uuids)
    }

    fun deserializeApplicationForm(jsonString: String): DaycareFormV0 {
        val mapper = objectMapper()
        return mapper.treeToValue<EnduserDaycareFormJSON>(mapper.readTree(jsonString))!!.deserialize()
    }

    @PostMapping("/placement-plan/{application-id}")
    fun createPlacementPlan(
        db: Database.Connection,
        @PathVariable("application-id") applicationId: ApplicationId,
        @RequestBody placementPlan: PlacementPlan
    ): ResponseEntity<Unit> {
        db.transaction { tx ->
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

        return ResponseEntity.noContent().build()
    }

    @GetMapping("/messages")
    fun getMessages(db: Database): ResponseEntity<List<SuomiFiMessage>> {
        return ResponseEntity.ok(MockEvakaMessageClient.getMessages())
    }

    @PostMapping("/messages/clean-up")
    fun cleanUpMessages(db: Database): ResponseEntity<Unit> {
        MockEvakaMessageClient.clearMessages()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/vtj-persons")
    fun upsertPerson(db: Database.Connection, @RequestBody person: VtjPerson): ResponseEntity<Unit> {
        MockPersonDetailsService.upsertPerson(person)
        db.transaction { tx ->
            val uuid = tx.createQuery("SELECT id FROM person WHERE social_security_number = :ssn")
                .bind("ssn", person.socialSecurityNumber)
                .mapTo<UUID>()
                .firstOrNull()

            uuid?.let {
                // Refresh Pis data by forcing refresh from VTJ
                val dummyUser = AuthenticatedUser.Employee(it, setOf(UserRole.SERVICE_WORKER))
                personService.getUpToDatePersonFromVtj(tx, dummyUser, it)
            }
        }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/vtj-persons/{ssn}")
    fun getVtjPerson(db: Database.Connection, @PathVariable ssn: String): ResponseEntity<VtjPerson> {
        return MockPersonDetailsService.getPerson(ssn)?.let { person -> ResponseEntity.ok(person) }
            ?: throw NotFound("vtj person $ssn was not found")
    }

    @GetMapping("/application-emails")
    fun getApplicationEmails(): ResponseEntity<List<MockEmail>> {
        return ResponseEntity.ok(MockEmailClient.emails)
    }

    @PostMapping("/applications/{applicationId}/actions/{action}")
    fun simpleAction(
        db: Database.Connection,
        @PathVariable applicationId: ApplicationId,
        @PathVariable action: String
    ): ResponseEntity<Unit> {
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
        db.transaction { tx ->
            tx.ensureFakeAdminExists()
            actionFn.invoke(tx, fakeAdmin, applicationId)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/applications/{applicationId}/actions/create-placement-plan")
    fun createPlacementPlan(
        db: Database.Connection,
        @PathVariable applicationId: ApplicationId,
        @RequestBody body: DaycarePlacementPlan
    ): ResponseEntity<Unit> {
        db.transaction { tx ->
            tx.ensureFakeAdminExists()
            applicationStateService.createPlacementPlan(tx, fakeAdmin, applicationId, body)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/applications/{applicationId}/actions/create-default-placement-plan")
    fun createDefaultPlacementPlan(
        db: Database.Connection,
        @PathVariable applicationId: ApplicationId
    ): ResponseEntity<Unit> {
        db.transaction { tx ->
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

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/mobile/pairings/challenge")
    fun postPairingChallenge(
        db: Database.Connection,
        @RequestBody body: PairingsController.PostPairingChallengeReq
    ): ResponseEntity<Pairing> {
        return db
            .transaction { it.challengePairing(body.challengeKey) }
            .let { ResponseEntity.ok(it) }
    }

    @PostMapping("/mobile/pairings/{id}/response")
    fun postPairingResponse(
        db: Database.Connection,
        @PathVariable id: PairingId,
        @RequestBody body: PairingsController.PostPairingResponseReq
    ): ResponseEntity<Pairing> {
        db.transaction { it.incrementAttempts(id, body.challengeKey) }

        return db
            .transaction {
                it.respondPairingChallengeCreateDevice(id, body.challengeKey, body.responseKey)
            }
            .let { ResponseEntity.ok(it) }
    }

    @PostMapping("/mobile/pairings")
    fun postPairing(
        db: Database.Connection,
        @RequestBody body: PairingsController.PostPairingReq
    ): ResponseEntity<Pairing> {
        return db.transaction {
            it.initPairing(body.unitId)
        }.let { ResponseEntity.ok(it) }
    }

    data class MobileDeviceReq(
        val id: MobileDeviceId,
        val unitId: DaycareId,
        val name: String,
        val deleted: Boolean,
        val longTermToken: UUID
    )

    @PostMapping("/mobile/devices")
    fun postMobileDevice(
        db: Database.Connection,
        @RequestBody body: MobileDeviceReq
    ): ResponseEntity<Unit> {
        return db.transaction {
            it.createUpdate(
                """
INSERT INTO mobile_device (id, unit_id, name, deleted, long_term_token)
VALUES(:id, :unitId, :name, :deleted, :longTermToken)
                """.trimIndent()
            )
                .bindKotlin(body)
                .execute()
        }.let { ResponseEntity.noContent().build() }
    }

    @PostMapping("/children/{childId}/child-daily-notes")
    fun postChildDailyNote(
        db: Database.Connection,
        @PathVariable childId: UUID,
        @RequestBody body: ChildDailyNoteBody
    ): ChildDailyNoteId {
        return db.transaction {
            it.createChildDailyNote(
                childId = childId,
                note = body
            )
        }
    }

    @PostMapping("/children/{childId}/child-sticky-notes")
    fun postChildStickyNote(
        db: Database.Connection,
        @PathVariable childId: UUID,
        @RequestBody body: ChildStickyNoteBody
    ): ChildStickyNoteId {
        return db.transaction {
            it.createChildStickyNote(
                childId = childId,
                note = body
            )
        }
    }

    @PostMapping("/daycare-groups/{groupId}/group-notes")
    fun postGroupNote(
        db: Database.Connection,
        @PathVariable groupId: GroupId,
        @RequestBody body: GroupNoteBody
    ): GroupNoteId {
        return db.transaction {
            it.createGroupNote(
                groupId = groupId,
                note = body
            )
        }
    }

    @GetMapping("/digitransit/autocomplete")
    fun digitransitAutocomplete() = ResponseEntity.ok(digitransit.autocomplete())

    @PutMapping("/digitransit/autocomplete")
    fun putDigitransitAutocomplete(@RequestBody mockResponse: MockDigitransit.Autocomplete) =
        digitransit.setAutocomplete(mockResponse)

    @PostMapping("/family-contact")
    fun createFamilyContact(db: Database.Connection, @RequestBody contacts: List<DevFamilyContact>): ResponseEntity<Unit> {
        db.transaction { contacts.forEach { contact -> it.insertFamilyContact(contact) } }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/backup-pickup")
    fun createBackupPickup(db: Database.Connection, @RequestBody backupPickups: List<DevBackupPickup>): ResponseEntity<Unit> {
        db.transaction { backupPickups.forEach { backupPickup -> it.insertBackupPickup(backupPickup) } }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/fridge-child")
    fun createFridgeChild(db: Database.Connection, @RequestBody fridgeChildren: List<DevFridgeChild>): ResponseEntity<Unit> {
        db.transaction { fridgeChildren.forEach { child -> it.insertFridgeChild(child) } }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/fridge-partner")
    fun createFridgePartner(db: Database.Connection, @RequestBody fridgePartners: List<DevFridgePartner>): ResponseEntity<Unit> {
        db.transaction { fridgePartners.forEach { partner -> it.insertFridgePartner(partner) } }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/employee-pin")
    fun createEmployeePins(db: Database.Connection, @RequestBody employeePins: List<DevEmployeePin>): ResponseEntity<Unit> {
        db.transaction {
            employeePins.forEach { employeePin ->
                val userId =
                    if (employeePin.userId != null) employeePin.userId
                    else if (!employeePin.employeeExternalId.isNullOrBlank()) it.getEmployeeIdByExternalId(employeePin.employeeExternalId)
                    else throw Error("Cannot create dev employee pin: user id and external user id missing")

                it.insertEmployeePin(employeePin.copy(userId = userId))
            }
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/pedagogical-document")
    fun createPedagogicalDocuments(db: Database.Connection, @RequestBody pedagogicalDocuments: List<DevPedagogicalDocument>): ResponseEntity<Unit> {
        db.transaction {
            pedagogicalDocuments.forEach { pedagogicalDocument ->
                it.insertPedagogicalDocument(pedagogicalDocument)
            }
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/pedagogical-document-attachment/{pedagogicalDocumentId}")
    fun createPedagogicalDocumentAttachment(
        db: Database.Connection,
        @PathVariable pedagogicalDocumentId: PedagogicalDocumentId,
        @RequestParam employeeId: UUID,
        @RequestPart("file") file: MultipartFile
    ): String {
        return db.transaction { tx ->
            val id = UUID.randomUUID()
            tx.insertAttachment(
                AttachmentId(id),
                file.name,
                file.contentType ?: "image/jpeg",
                AttachmentParent.PedagogicalDocument(pedagogicalDocumentId),
                uploadedByPerson = null,
                uploadedByEmployee = employeeId,
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
        }.key
    }

    @PostMapping("/service-need")
    fun createServiceNeeds(db: Database.Connection, @RequestBody serviceNeeds: List<DevServiceNeed>): ResponseEntity<Unit> {
        db.transaction {
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
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/service-need-option")
    fun createServiceNeedOption(db: Database.Connection, @RequestBody serviceNeedOptions: List<ServiceNeedOption>): ResponseEntity<Unit> {
        db.transaction {
            serviceNeedOptions.forEach { option -> it.insertServiceNeedOption(option) }
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/service-need-options")
    fun createDefaultServiceNeedOptions(db: Database.Connection): ResponseEntity<Unit> {
        db.transaction { it.insertServiceNeedOptions() }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/voucher-values")
    fun createVoucherValues(db: Database.Connection): ResponseEntity<Unit> {
        db.transaction { it.insertVoucherValues() }
        return ResponseEntity.noContent().build()
    }
}

fun Database.Transaction.ensureFakeAdminExists() {
    // language=sql
    val sql =
        """
        INSERT INTO employee (id, first_name, last_name, email, external_id, roles)
        VALUES (:id, 'Dev', 'API', 'dev.api@espoo.fi', 'espoo-ad:' || :id, '{ADMIN, SERVICE_WORKER}'::user_role[])
        ON CONFLICT DO NOTHING
        """.trimIndent()

    createUpdate(sql).bind("id", fakeAdmin.id).execute()
}

fun Database.Transaction.deleteAndCascadeEmployee(id: UUID) {
    execute("DELETE FROM message_account WHERE employee_id = ?", id)
    execute("DELETE FROM mobile_device WHERE id = ?", id)
    execute("DELETE FROM employee_pin WHERE user_id = ?", id)
    execute("DELETE FROM employee WHERE id = ?", id)
}

fun Database.Transaction.deleteAndCascadeEmployeeByExternalId(externalId: ExternalId) {
    val employeeId = createQuery("SELECT id FROM employee WHERE external_id = :externalId")
        .bind("externalId", externalId)
        .mapTo<UUID>()
        .findOne()
    if (employeeId.isPresent) {
        deleteAndCascadeEmployee(employeeId.get())
    }
}

fun Database.Transaction.insertServiceNeedOptions() {
    execute(
        """
INSERT INTO service_need_option (name_fi, name_sv, name_en, valid_placement_type, default_option, fee_coefficient, voucher_value_coefficient, occupancy_coefficient, daycare_hours_per_week, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv) VALUES
    ('Kokopäiväinen', 'Kokopäiväinen', 'Kokopäiväinen', 'DAYCARE', TRUE, 1.0, 1.0, 1.0, 35, FALSE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', 'yli 25h/viikko', 'mer än 25 h/vecka'),
    ('Osapäiväinen', 'Osapäiväinen', 'Osapäiväinen', 'DAYCARE_PART_TIME', TRUE, 0.6, 0.6, 0.54, 25, TRUE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', 'korkeintaan 25 h/viikko', 'högst 25 h/vecka'),
    ('Viisivuotiaiden kokopäiväinen', 'Viisivuotiaiden kokopäiväinen', 'Viisivuotiaiden kokopäiväinen', 'DAYCARE_FIVE_YEAR_OLDS', TRUE, 0.8, 1.0, 1.0, 45, FALSE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', 'yli 25 h/viikko', 'mer än 25 h/vecka'),
    ('Viisivuotiaiden osapäiväinen', 'Viisivuotiaiden osapäiväinen', 'Viisivuotiaiden osapäiväinen', 'DAYCARE_PART_TIME_FIVE_YEAR_OLDS', TRUE, 0.0, 0.6, 0.5, 20, TRUE, FALSE, 'ei maksullista varhaiskasvatusta', 'ingen avgiftsbelagd småbarnspedagogik', 'korkeintaan 25 h/viikko', 'högst 25 h/vecka'),
    ('Esiopetus', 'Esiopetus', 'Esiopetus', 'PRESCHOOL', TRUE, 0.0, 0.5, 0.5, 0, TRUE, FALSE, 'ei maksullista varhaiskasvatusta', 'ingen avgiftsbelagd småbarnspedagogik', '', ''),
    ('Esiopetus ja liittyvä varhaiskasvatus', 'Esiopetus ja liittyvä varhaiskasvatus', 'Esiopetus ja liittyvä varhaiskasvatus', 'PRESCHOOL_DAYCARE', TRUE, 0.8, 0.5, 1.0, 25, TRUE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', '', ''),
    ('Valmistava opetus', 'Valmistava opetus', 'Valmistava opetus', 'PREPARATORY', TRUE, 0.0, 0.5, 0.5, 0, TRUE, FALSE, 'ei maksullista varhaiskasvatusta', 'ingen avgiftsbelagd småbarnspedagogik', '', ''),
    ('Valmistava opetus ja liittyvä varhaiskasvatus', 'Valmistava opetus ja liittyvä varhaiskasvatus', 'Valmistava opetus ja liittyvä varhaiskasvatus', 'PREPARATORY_DAYCARE', TRUE, 0.8, 0.5, 1.0, 25, TRUE, FALSE, 'palveluntarve puuttuu, korkein maksu', 'vårdbehovet saknas, högsta avgift', '', ''),
    ('Kerho', 'Kerho', 'Kerho', 'CLUB', TRUE, 0.0, 0.0, 1.0, 0, TRUE, TRUE, '', '', '', ''),
    ('Kokopäiväinen tilapäinen', 'Kokopäiväinen tilapäinen', 'Kokopäiväinen tilapäinen', 'TEMPORARY_DAYCARE', TRUE, 1.0, 0.0, 1.0, 35, FALSE, TRUE, '', '', '', ''),
    ('Osapäiväinen tilapäinen', 'Osapäiväinen tilapäinen', 'Osapäiväinen tilapäinen', 'TEMPORARY_DAYCARE_PART_DAY', TRUE, 0.5, 0.0, 0.54, 25, TRUE, TRUE, '', '', '', '')
"""
    )
}

fun Database.Transaction.insertVoucherValues() {
    execute(
        """
INSERT INTO voucher_value (id, validity, base_value, age_under_three_coefficient) VALUES ('084314dc-ed7f-4725-92f2-5c220bb4bb7e', daterange('2000-01-01', NULL, '[]'), 87000, 1.55);
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
    val id: UUID? = null,
    val childId: UUID,
    val unitId: DaycareId,
    val groupId: GroupId?,
    val period: FiniteDateRange
)

data class DevChild(
    val id: UUID,
    val allergies: String = "",
    val diet: String = "",
    val preferredName: String = "",
    val medication: String = "",
    val additionalInfo: String = ""
)

data class DevDaycare(
    val id: DaycareId? = DaycareId(UUID.randomUUID()),
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
    val ophOrganizationOid: String? = "1.2.3.4.5",
    val roundTheClock: Boolean? = false,
    val operationDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val enabledPilotFeatures: Set<PilotFeature> = setOf()
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
    val childId: UUID,
    val updatedBy: UUID,
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate = LocalDate.of(2019, 12, 31),
    val capacityFactor: Double = 1.0,
    val bases: Set<String> = emptySet(),
    val otherBasis: String = "",
    val description: String = ""
)

data class DevAssistanceAction(
    val id: AssistanceActionId = AssistanceActionId(UUID.randomUUID()),
    val childId: UUID,
    val updatedBy: UUID,
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate = LocalDate.of(2019, 12, 31),
    val actions: Set<String> = emptySet(),
    val measures: Set<AssistanceMeasure> = emptySet(),
    val otherAction: String = ""
)

data class DevPlacement(
    val id: PlacementId = PlacementId(UUID.randomUUID()),
    val type: PlacementType = PlacementType.DAYCARE,
    val childId: UUID,
    val unitId: DaycareId,
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate = LocalDate.of(2019, 12, 31)
)

data class DevPerson(
    val id: UUID = UUID.randomUUID(),
    val dateOfBirth: LocalDate = LocalDate.of(1980, 1, 1),
    val dateOfDeath: LocalDate? = null,
    val firstName: String = "Test",
    val lastName: String = "Person",
    val ssn: String? = null,
    val ssnAddingDisabled: Boolean? = null,
    val email: String? = null,
    val phone: String? = null,
    val backupPhone: String = "",
    val language: String? = null,
    val streetAddress: String = "",
    val postalCode: String = "",
    val postOffice: String = "",
    val residenceCode: String? = "",
    val nationalities: List<String> = emptyList(),
    val restrictedDetailsEnabled: Boolean = false,
    val restrictedDetailsEndDate: LocalDate? = null,
    val invoicingStreetAddress: String = "",
    val invoicingPostalCode: String = "",
    val invoicingPostOffice: String = "",
    val dependants: List<DevPerson> = emptyList(),
    val guardians: List<DevPerson> = emptyList(),
    val updatedFromVtj: HelsinkiDateTime? = null,
    val ophPersonOid: String = ""
) {
    fun toPersonDTO() = PersonDTO(
        id = this.id,
        identity = this.ssn?.let { ExternalIdentifier.SSN.getInstance(it) } ?: ExternalIdentifier.NoID(),
        ssnAddingDisabled = this.ssnAddingDisabled ?: false,
        firstName = this.firstName,
        lastName = this.lastName,
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
    val childId: UUID,
    val headOfChildId: UUID,
    val startDate: LocalDate,
    val endDate: LocalDate
)

data class DevEmployee(
    val id: UUID = UUID.randomUUID(),
    val firstName: String = "Test",
    val lastName: String = "Person",
    val email: String? = "test.person@espoo.fi",
    val externalId: ExternalId? = null,
    val roles: Set<UserRole> = setOf(),
    val lastLogin: Instant? = Instant.now()
)

data class DevMobileDevice(
    val id: MobileDeviceId = MobileDeviceId(UUID.randomUUID()),
    val unitId: DaycareId,
    val name: String = "Laite",
    val deleted: Boolean = false,
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

data class ApplicationWithForm(
    val id: ApplicationId?,
    val createdDate: OffsetDateTime?,
    val modifiedDate: OffsetDateTime?,
    var sentDate: LocalDate?,
    var dueDate: LocalDate?,
    val status: ApplicationStatus,
    val guardianId: UUID,
    val childId: UUID,
    val origin: ApplicationOrigin,
    val checkedByAdmin: Boolean,
    val hideFromGuardian: Boolean,
    val transferApplication: Boolean,
    val otherGuardianId: UUID?,
    val form: String?
)

data class ApplicationForm(
    val id: UUID? = UUID.randomUUID(),
    val applicationId: ApplicationId,
    val createdDate: OffsetDateTime? = OffsetDateTime.now(),
    val revision: Int,
    val document: DaycareFormV0,
    val updated: OffsetDateTime? = OffsetDateTime.now()
)

data class DevDaycareGroupAcl(val groupId: GroupId, val employeeId: UUID)

data class DevServiceNeed(
    val id: ServiceNeedId,
    val placementId: PlacementId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val optionId: ServiceNeedOptionId,
    val shiftCare: Boolean,
    val confirmedBy: UUID,
    val confirmedAt: LocalDate? = null
)

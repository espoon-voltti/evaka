// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.dev

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import fi.espoo.evaka.application.ApplicationOrigin
import fi.espoo.evaka.application.ApplicationStateService
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.DaycarePlacementPlan
import fi.espoo.evaka.application.enduser.daycare.EnduserDaycareFormJSON
import fi.espoo.evaka.application.enduser.objectMapper
import fi.espoo.evaka.application.fetchApplicationDetails
import fi.espoo.evaka.application.persistence.daycare.DaycareFormV0
import fi.espoo.evaka.assistanceaction.AssistanceActionType
import fi.espoo.evaka.assistanceaction.AssistanceMeasure
import fi.espoo.evaka.assistanceneed.AssistanceBasis
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.DaycareDecisionCustomization
import fi.espoo.evaka.daycare.MailingAddress
import fi.espoo.evaka.daycare.UnitManager
import fi.espoo.evaka.daycare.VisitingAddress
import fi.espoo.evaka.daycare.deleteDaycareGroup
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.decision.DecisionType
import fi.espoo.evaka.decision.insertDecision
import fi.espoo.evaka.emailclient.MockApplicationEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.identity.ExternalId
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.pairing.Pairing
import fi.espoo.evaka.pairing.PairingsController
import fi.espoo.evaka.pairing.challengePairing
import fi.espoo.evaka.pairing.incrementAttempts
import fi.espoo.evaka.pairing.initPairing
import fi.espoo.evaka.pairing.respondPairingChallengeCreateDevice
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.deleteEmployee
import fi.espoo.evaka.pis.deleteEmployeeByExternalId
import fi.espoo.evaka.pis.deleteEmployeeRolesByExternalId
import fi.espoo.evaka.pis.getEmployees
import fi.espoo.evaka.pis.getPersonBySSN
import fi.espoo.evaka.pis.service.PersonDTO
import fi.espoo.evaka.pis.service.PersonService
import fi.espoo.evaka.pis.updatePersonFromVtj
import fi.espoo.evaka.placement.PlacementPlanService
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.MockEvakaMessageClient
import fi.espoo.evaka.shared.message.SuomiFiMessage
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import org.jdbi.v3.core.Handle
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
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.UUID

private val fakeAdmin = AuthenticatedUser(
    id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    roles = setOf(UserRole.ADMIN)
)

@Profile("enable_dev_api")
@RestController
@RequestMapping("/dev-api")
class DevApi(
    private val objectMapper: ObjectMapper,
    private val personService: PersonService,
    private val asyncJobRunner: AsyncJobRunner,
    private val placementPlanService: PlacementPlanService,
    private val applicationStateService: ApplicationStateService
) {
    @PostMapping("/clean-up")
    fun cleanUpDatabase(db: Database): ResponseEntity<Unit> {
        db.transaction { it.handle.clearDatabase() }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/run-jobs")
    fun runJobs(): ResponseEntity<Unit> {
        asyncJobRunner.runPendingJobsSync()
        asyncJobRunner.waitUntilNoRunningJobs(timeout = Duration.ofSeconds(20))
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/applications/{id}")
    fun deleteApplicationByApplicationId(db: Database, @PathVariable id: UUID): ResponseEntity<Unit> {
        db.transaction { it.handle.deleteApplication(id) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/care-areas")
    fun createCareAreas(db: Database, @RequestBody careAreas: List<DevCareArea>): ResponseEntity<Unit> {
        db.transaction { careAreas.forEach { careArea -> it.handle.insertTestCareArea(careArea) } }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/care-areas/{id}")
    fun deleteArea(db: Database, @PathVariable id: UUID): ResponseEntity<Unit> {
        db.transaction { it.handle.deleteCareArea(id) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/daycares")
    fun createDaycares(db: Database, @RequestBody daycares: List<DevDaycare>): ResponseEntity<Unit> {
        db.transaction { daycares.forEach { daycare -> it.handle.insertTestDaycare(daycare) } }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/daycares/{id}")
    fun deleteDaycare(db: Database, @PathVariable id: UUID): ResponseEntity<Unit> {
        db.transaction { it.handle.deleteDaycare(id) }
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/daycares/{daycareId}/acl")
    fun allowSupervisorToAccessDaycare(
        db: Database,
        @PathVariable daycareId: UUID,
        @RequestBody body: DaycareAclInsert
    ): ResponseEntity<Unit> {
        db.transaction { tx ->
            updateDaycareAcl(tx.handle, daycareId, body.externalId, UserRole.UNIT_SUPERVISOR)
        }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/daycares/{daycareId}/acl/{externalId}")
    fun removeUserAccessToDaycare(db: Database, @PathVariable daycareId: UUID, @PathVariable externalId: ExternalId): ResponseEntity<Unit> {
        db.transaction { tx ->
            removeDaycareAcl(tx.handle, daycareId, externalId)
        }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/daycare-groups")
    fun createDaycareGroups(db: Database, @RequestBody groups: List<DevDaycareGroup>): ResponseEntity<Unit> {
        db.transaction {
            groups.forEach { group -> it.handle.insertTestDaycareGroup(group) }
        }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/daycare-groups/{id}")
    fun removeDaycareGroup(db: Database, @PathVariable id: UUID): ResponseEntity<Unit> {
        db.transaction { tx ->
            tx.handle.deleteDaycareGroup(id)
        }
        return ResponseEntity.ok().build()
    }

    data class Caretaker(
        val groupId: UUID,
        val amount: Double,
        val startDate: LocalDate,
        val endDate: LocalDate?
    )

    @PostMapping("/daycare-caretakers")
    fun createDaycareCaretakers(db: Database, @RequestBody caretakers: List<Caretaker>): ResponseEntity<Unit> {
        db.transaction { tx ->
            caretakers.forEach { caretaker ->
                insertTestCaretakers(
                    tx.handle,
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
    fun createChildren(db: Database, @RequestBody children: List<DevChild>): ResponseEntity<Unit> {
        db.transaction { children.forEach { child -> it.handle.insertTestChild(child) } }
        return ResponseEntity.noContent().build()
    }

    // also cascades delete to daycare_placement and group_placement
    @DeleteMapping("/children/{id}")
    fun deleteChild(db: Database, @PathVariable id: UUID): ResponseEntity<Unit> {
        db.transaction { it.handle.deleteChild(id) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/daycare-placements")
    fun createDaycarePlacements(db: Database, @RequestBody placements: List<DevPlacement>): ResponseEntity<Unit> {
        db.transaction { placements.forEach { placement -> it.handle.insertTestPlacement(placement) } }
        return ResponseEntity.noContent().build()
    }

    data class DecisionRequest(
        val id: UUID,
        val employeeId: UUID,
        val applicationId: UUID,
        val unitId: UUID,
        val type: DecisionType,
        val startDate: LocalDate,
        val endDate: LocalDate
    )

    @PostMapping("/decisions")
    fun createDecisions(db: Database, @RequestBody decisions: List<DecisionRequest>): ResponseEntity<Unit> {
        db.transaction { tx ->
            decisions.forEach { decision ->
                insertDecision(
                    tx.handle,
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

    @PostMapping("/fee-decisions")
    fun createFeeDecisions(db: Database, @RequestBody decisions: List<FeeDecision>): ResponseEntity<Unit> {
        db.transaction { tx -> upsertFeeDecisions(tx.handle, objectMapper, decisions) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/value-decisions")
    fun createVoucherValueDecisions(db: Database, @RequestBody decisions: List<VoucherValueDecision>): ResponseEntity<Unit> {
        db.transaction { tx -> tx.handle.upsertValueDecisions(objectMapper, decisions) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/invoices")
    fun createInvoices(db: Database, @RequestBody invoices: List<Invoice>): ResponseEntity<Unit> {
        db.transaction { tx -> insertInvoices(invoices)(tx.handle) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/pricing")
    fun createPricing(db: Database, @RequestBody pricing: DevPricing): ResponseEntity<UUID> = db.transaction {
        ResponseEntity.ok(
            it.createUpdate(
                """
INSERT INTO pricing (id, valid_from, valid_to, multiplier, max_threshold_difference, min_threshold_2, min_threshold_3, min_threshold_4, min_threshold_5, min_threshold_6, threshold_increase_6_plus)
VALUES (:id, :validFrom, :validTo, :multiplier, :maxThresholdDifference, :minThreshold2, :minThreshold3, :minThreshold4, :minThreshold5, :minThreshold6, :thresholdIncrease6Plus)
RETURNING id
"""
            ).bindKotlin(pricing).executeAndReturnGeneratedKeys().mapTo<UUID>().single()
        )
    }

    @PostMapping("/pricing/clean-up")
    fun clearPricing(db: Database): ResponseEntity<Unit> {
        db.transaction { it.execute("DELETE FROM pricing") }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/pricing/{id}")
    fun deletePricing(db: Database, @PathVariable id: UUID): ResponseEntity<Unit> {
        db.transaction { it.handle.deletePricing(id) }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/incomes/person/{id}")
    fun deleteIncomesByPerson(db: Database, @PathVariable id: UUID): ResponseEntity<Unit> {
        db.transaction { it.handle.deleteIncome(id) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/person")
    fun upsertPerson(db: Database, @RequestBody body: DevPerson): ResponseEntity<PersonDTO> {
        if (body.ssn == null) throw BadRequest("SSN is required for using this endpoint")
        return db.transaction { tx ->
            val person = tx.handle.getPersonBySSN(body.ssn)
            val personDTO = body.toPersonDTO()

            if (person != null) {
                tx.handle.updatePersonFromVtj(personDTO).let { ResponseEntity.ok(it) }
            } else {
                tx.handle.createPersonFromVtj(personDTO).let { ResponseEntity.ok(it) }
            }
        }
    }

    @PostMapping("/person/create")
    fun createPerson(db: Database, @RequestBody body: DevPerson): ResponseEntity<UUID> {
        return db.transaction { tx ->
            val personId = tx.handle.insertTestPerson(body)
            val dto = body.copy(id = personId).toPersonDTO()
            if (dto.identity is ExternalIdentifier.SSN) {
                tx.handle.updatePersonFromVtj(dto)
            }
            ResponseEntity.ok(personId)
        }
    }

    @DeleteMapping("/person/{id}")
    fun deletePerson(db: Database, @PathVariable id: UUID): ResponseEntity<Unit> {
        db.transaction {
            it.execute(
                """
WITH ApplicationsDeleted AS (DELETE FROM application WHERE child_id = ? OR guardian_id = ? RETURNING id),
AttachmentsDeleted AS (DELETE FROM application_form USING ApplicationsDeleted WHERE application_id = ApplicationsDeleted.id)
DELETE FROM attachment USING ApplicationsDeleted WHERE application_id = ApplicationsDeleted.id""",
                id, id
            )
            it.execute("DELETE FROM fee_decision_part WHERE child = ?", id)
            it.execute("DELETE FROM fee_decision WHERE head_of_family = ?", id)
            it.execute("DELETE FROM income WHERE person_id = ?", id)
            it.execute("DELETE FROM absence WHERE child_id = ?", id)
            it.execute("DELETE FROM backup_care WHERE child_id = ?", id)
            it.execute(
                "DELETE FROM daycare_group_placement WHERE daycare_placement_id IN (SELECT id FROM placement WHERE child_id = ?)",
                id
            )
            it.execute("DELETE FROM placement WHERE child_id = ?", id)
            it.execute("DELETE FROM fridge_child WHERE head_of_child = ? OR child_id = ?", id, id)
            it.execute("DELETE FROM guardian WHERE (guardian_id = ? OR child_id = ?)", id, id)
            it.execute("DELETE FROM child WHERE id = ?", id)
            it.execute("DELETE FROM person WHERE id = ?", id)
        }
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/person/ssn/{ssn}")
    fun deletePersonBySsn(db: Database, @PathVariable ssn: String): ResponseEntity<Unit> {
        db.read { h ->
            h.createQuery("SELECT id FROM person WHERE social_security_number = :ssn")
                .bind("ssn", ssn)
                .mapTo<UUID>()
                .firstOrNull()
        }?.let { uuid -> deletePerson(db, uuid) }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/parentship")
    fun createParentships(db: Database, @RequestBody parentships: List<DevParentship>): ResponseEntity<List<DevParentship>> {
        return db.transaction { tx -> parentships.map { tx.handle.insertTestParentship(it) } }.let { ResponseEntity.ok(it) }
    }

    @GetMapping("/employee")
    fun getEmployees(db: Database): ResponseEntity<List<Employee>> {
        return ResponseEntity.ok(db.read { it.handle.getEmployees() })
    }

    @PostMapping("/employee")
    fun createEmployee(db: Database, @RequestBody body: DevEmployee): ResponseEntity<UUID> {
        return ResponseEntity.ok(db.transaction { it.handle.insertTestEmployee(body) })
    }

    @DeleteMapping("/employee/{id}")
    fun deleteEmployee(db: Database, @PathVariable id: UUID): ResponseEntity<Unit> {
        db.transaction { it.handle.deleteEmployee(id) }
        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/employee/external-id/{externalId}")
    fun deleteEmployeeByExternalId(db: Database, @PathVariable externalId: ExternalId): ResponseEntity<Unit> {
        db.transaction { it.handle.deleteEmployeeByExternalId(externalId) }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/employee/external-id/{externalId}")
    fun upsertEmployeeByExternalId(db: Database, @PathVariable externalId: ExternalId, @RequestBody employee: DevEmployee): ResponseEntity<UUID> = db.transaction {
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

    @DeleteMapping("/employee/roles/external-id/{externalId}")
    fun deleteEmployeeRolesByExternalId(db: Database, @PathVariable externalId: ExternalId): ResponseEntity<Unit> {
        db.transaction { it.handle.deleteEmployeeRolesByExternalId(externalId) }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/child")
    fun insertChild(db: Database, @RequestBody body: DevPerson): ResponseEntity<UUID> = db.transaction {
        val id = it.handle.insertTestPerson(
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
        it.handle.insertTestChild(DevChild(id = id))
        ResponseEntity.ok(id)
    }

    @PostMapping("/backup-cares")
    fun createBackupCares(db: Database, @RequestBody backupCares: List<DevBackupCare>): ResponseEntity<Unit> {
        db.transaction { tx -> backupCares.forEach { insertTestBackupCare(tx.handle, it) } }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/applications")
    fun createApplications(db: Database, @RequestBody applications: List<ApplicationWithForm>): ResponseEntity<List<UUID>> {
        val uuids =
            db.transaction { tx ->
                applications.map { application ->
                    val id = insertApplication(tx.handle, application)
                    application.form?.let { applicationFormString ->
                        insertApplicationForm(
                            tx.handle,
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

    @PostMapping("/placement-plan/{application-id}")
    fun createPlacementPlan(
        db: Database,
        @PathVariable("application-id") applicationId: UUID,
        @RequestBody placementPlan: PlacementPlan
    ): ResponseEntity<Unit> {
        db.transaction { tx ->
            val application = fetchApplicationDetails(tx.handle, applicationId)
                ?: throw NotFound("application $applicationId not found")
            val preschoolDaycarePeriod = if (placementPlan.preschoolDaycarePeriodStart != null) ClosedPeriod(
                placementPlan.preschoolDaycarePeriodStart, placementPlan.preschoolDaycarePeriodEnd!!
            ) else null

            placementPlanService.createPlacementPlan(
                tx,
                application,
                DaycarePlacementPlan(
                    placementPlan.unitId,
                    ClosedPeriod(placementPlan.periodStart, placementPlan.periodEnd),
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
    fun upsertPerson(db: Database, @RequestBody person: VtjPerson): ResponseEntity<Unit> {
        MockPersonDetailsService.upsertPerson(person)
        db.transaction { tx ->
            val uuid = tx.createQuery("SELECT id FROM person WHERE social_security_number = :ssn")
                .bind("ssn", person.socialSecurityNumber)
                .mapTo<UUID>()
                .firstOrNull()

            uuid?.let {
                // Refresh Pis data by forcing refresh from VTJ
                val dummyUser = AuthenticatedUser(it, setOf(UserRole.SERVICE_WORKER))
                personService.getUpToDatePerson(tx, dummyUser, it)
            }
        }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/vtj-persons/{ssn}")
    fun getVtjPerson(db: Database, @PathVariable ssn: String): ResponseEntity<VtjPerson> {
        return MockPersonDetailsService.getPerson(ssn)?.let { person -> ResponseEntity.ok(person) }
            ?: throw NotFound("vtj person $ssn was not found")
    }

    @DeleteMapping("/vtj-persons/{ssn}")
    fun deleteVtjPerson(db: Database, @PathVariable ssn: String): ResponseEntity<Unit> {
        MockPersonDetailsService.deletePerson(ssn)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/application-emails")
    fun getApplicationEmails(): ResponseEntity<List<MockApplicationEmail>> {
        return ResponseEntity.ok(MockEmailClient.applicationEmails)
    }

    @PostMapping("/application-emails/clean-up")
    fun cleanApplicationEmails(): ResponseEntity<Unit> {
        MockEmailClient.applicationEmails.clear()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/applications/{applicationId}/actions/{action}")
    fun simpleAction(
        db: Database,
        @PathVariable applicationId: UUID,
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
            ensureFakeAdminExists(tx.handle)
            actionFn.invoke(tx, fakeAdmin, applicationId)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/applications/{applicationId}/actions/create-placement-plan")
    fun createPlacementPlan(
        db: Database,
        @PathVariable applicationId: UUID,
        @RequestBody body: DaycarePlacementPlan
    ): ResponseEntity<Unit> {
        db.transaction { tx ->
            ensureFakeAdminExists(tx.handle)
            applicationStateService.createPlacementPlan(tx, fakeAdmin, applicationId, body)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/applications/{applicationId}/actions/create-default-placement-plan")
    fun createDefaultPlacementPlan(
        db: Database,
        @PathVariable applicationId: UUID
    ): ResponseEntity<Unit> {
        db.transaction { tx ->
            ensureFakeAdminExists(tx.handle)
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
        @PathVariable id: UUID,
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

    @DeleteMapping("/mobile/pairings/{id}")
    fun deletePairing(db: Database, @PathVariable id: UUID): ResponseEntity<Unit> {
        db.transaction { it.deletePairing(id) }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/mobile/devices/{id}")
    fun deleteMobileDevice(db: Database, @PathVariable id: UUID): ResponseEntity<Unit> {
        db.transaction { it.deleteMobileDevice(id) }
        return ResponseEntity.noContent().build()
    }
}

fun ensureFakeAdminExists(h: Handle) {
    // language=sql
    val sql =
        """
        INSERT INTO employee (id, first_name, last_name, email, external_id, roles)
        VALUES (:id, 'Dev', 'API', 'dev.api@espoo.fi', 'espoo-ad' || :id, '{ADMIN, SERVICE_WORKER}'::user_role[])
        ON CONFLICT DO NOTHING
        """.trimIndent()

    h.createUpdate(sql).bind("id", fakeAdmin.id).execute()
}

fun deserializeApplicationForm(jsonString: String): DaycareFormV0 {
    return objectMapper().treeToValue<EnduserDaycareFormJSON>(objectMapper().readTree(jsonString))!!.deserialize()
}

fun Handle.clearDatabase() = listOf(
    "attachment",
    "guardian",
    "decision",
    "placement_plan",
    "application_form",
    "backup_care",
    "placement",
    "application_note",
    "application",
    "income",
    "fee_alteration",
    "invoice",
    "fee_decision",
    "voucher_value_decision",
    "async_job"
).forEach {
    execute("DELETE FROM $it")
}

fun Database.Transaction.deletePairing(id: UUID) {
    execute("DELETE FROM pairing WHERE id = ?", id)
}

fun Database.Transaction.deleteMobileDevice(id: UUID) {
    execute("DELETE FROM mobile_device WHERE id = ?", id)
    execute("DELETE FROM employee WHERE id = ?", id)
}

fun Handle.deleteApplication(id: UUID) {
    execute("DELETE FROM attachment WHERE application_id = ?", id)
    execute("DELETE FROM decision WHERE application_id = ?", id)
    execute("DELETE FROM placement_plan WHERE application_id = ?", id)
    execute("DELETE FROM application_form WHERE application_id = ?", id)
    execute("DELETE FROM application WHERE id = ?", id)
}

fun Handle.deleteCareArea(id: UUID) {
    execute(
        """
WITH deleted_ids AS (
    DELETE FROM fee_decision_part
    WHERE placement_unit IN (SELECT id FROM daycare WHERE care_area_id = ?)
    RETURNING fee_decision_id
) DELETE FROM fee_decision WHERE id IN (SELECT fee_decision_id FROM deleted_ids)
""",
        id
    )
    execute(
        "DELETE FROM daycare_group_placement WHERE daycare_placement_id IN (SELECT id FROM placement WHERE unit_id IN (SELECT id FROM daycare WHERE care_area_id = ?))",
        id
    )
    execute("DELETE FROM decision WHERE unit_id IN (SELECT id FROM daycare WHERE care_area_id = ?)", id)
    execute("DELETE FROM backup_care WHERE unit_id IN (SELECT id FROM daycare WHERE care_area_id = ?)", id)
    execute("DELETE FROM placement_plan WHERE unit_id IN (SELECT id FROM daycare WHERE care_area_id = ?)", id)
    execute("DELETE FROM placement WHERE unit_id IN (SELECT id FROM daycare WHERE care_area_id = ?)", id)
    execute("DELETE FROM pairing WHERE unit_id IN (SELECT id FROM daycare WHERE care_area_id = ?)", id)
    execute(
        """
        DELETE
        FROM fee_decision
        WHERE id IN (
            SELECT fee_decision_id
            FROM fee_decision_part
            WHERE placement_unit IN (
                SELECT id
                FROM daycare
                WHERE care_area_id = ?)
                )
""",
        id
    )
    execute(
        """
        DELETE
        FROM fee_decision_part
        WHERE placement_unit IN (
            SELECT id
            FROM daycare
            WHERE care_area_id = ?)
""",
        id
    )
    execute("DELETE FROM daycare_acl WHERE daycare_id IN (SELECT id FROM daycare WHERE care_area_id = ?)", id)
    execute("DELETE FROM mobile_device WHERE unit_id IN (SELECT id FROM daycare WHERE care_area_id = ?)", id)
    execute("DELETE FROM daycare WHERE care_area_id = ?", id)
    execute("DELETE FROM care_area WHERE id = ?", id)
}

fun Handle.deleteDaycare(id: UUID) {
    execute("DELETE FROM mobile_device WHERE unit_id = ?", id)
    execute(
        "DELETE FROM daycare WHERE id = ?",
        id
    )
}

fun Handle.deleteChild(id: UUID) {
    execute(
        "DELETE FROM daycare_group_placement WHERE daycare_placement_id IN (SELECT id FROM placement WHERE child_id = ?)",
        id
    )
    execute("DELETE FROM absence WHERE child_id = ?", id)
    execute("DELETE FROM backup_care WHERE child_id = ?", id)
    execute("DELETE FROM placement WHERE child_id = ?", id)
    execute("DELETE FROM child WHERE id = ?", id)
}

fun insertInvoices(invoices: List<Invoice>) = { h: Handle ->
    upsertInvoices(h, invoices)
}

fun Handle.deleteIncome(id: UUID) = execute("DELETE FROM income WHERE person_id = ?", id)
fun Handle.deletePricing(id: UUID) = execute("DELETE FROM pricing WHERE id = ?", id)

data class DevCareArea(
    val id: UUID = UUID.randomUUID(),
    val name: String = "Test Care Area",
    val shortName: String = "test_area",
    val areaCode: Int? = 200,
    val subCostCenter: String? = "00"
)

data class DevBackupCare(
    val id: UUID? = null,
    val childId: UUID,
    val unitId: UUID,
    val groupId: UUID?,
    val period: ClosedPeriod
)

data class DevChild(
    val id: UUID,
    val allergies: String = "",
    val diet: String = "",
    val additionalInfo: String = ""
)

data class DevClubAcl(val employeeId: UUID)

data class DevClubGroup(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val careAreaId: UUID,
    val providerType: ProviderType = ProviderType.MUNICIPAL,
    val language: Language = Language.fi,
    val ghostUnit: Boolean = false,
    val description: String = "",
    val unitManagerId: UUID? = null,
    val clubId: UUID = UUID.randomUUID(),
    val minAge: Int,
    val maxAge: Int,
    val schedule: String = "",
    val phone: String? = null,
    val email: String? = null,
    val additionalInfo: String? = null,
    val url: String? = null,
    val location: Coordinate? = null,
    val visitingAddress: VisitingAddress = VisitingAddress(),
    val mailingAddress: MailingAddress = MailingAddress()
)

data class DevDaycare(
    val id: UUID? = UUID.randomUUID(),
    val name: String = "Test Daycare",
    val openingDate: LocalDate? = null,
    val closingDate: LocalDate? = null,
    val areaId: UUID,
    val type: Set<CareType> = setOf(CareType.CENTRE, CareType.PRESCHOOL, CareType.PREPARATORY_EDUCATION),
    val canApplyClub: Boolean = false,
    val canApplyDaycare: Boolean = true,
    val canApplyPreschool: Boolean = true,
    val providerType: ProviderType = ProviderType.MUNICIPAL,
    val capacity: Int = 0,
    val language: Language = Language.fi,
    val ghostUnit: Boolean = false,
    val uploadToVarda: Boolean = true,
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
    val ophOrganizationOid: String? = "1.2.3.4.5"
)

data class DevDaycareGroup(
    val id: UUID = UUID.randomUUID(),
    val daycareId: UUID,
    val name: String = "Testiläiset",
    val startDate: LocalDate = LocalDate.of(2019, 1, 1)
)

data class DevAssistanceNeed(
    val id: UUID = UUID.randomUUID(),
    val childId: UUID,
    val updatedBy: UUID,
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate = LocalDate.of(2019, 12, 31),
    val capacityFactor: Double = 1.0,
    val bases: Set<AssistanceBasis> = emptySet(),
    val otherBasis: String = "",
    val description: String = ""
)

data class DevAssistanceAction(
    val id: UUID = UUID.randomUUID(),
    val childId: UUID,
    val updatedBy: UUID,
    val startDate: LocalDate = LocalDate.of(2019, 1, 1),
    val endDate: LocalDate = LocalDate.of(2019, 12, 31),
    val actions: Set<AssistanceActionType> = emptySet(),
    val measures: Set<AssistanceMeasure> = emptySet(),
    val otherAction: String = ""
)

data class DevPlacement(
    val id: UUID = UUID.randomUUID(),
    val type: PlacementType = PlacementType.DAYCARE,
    val childId: UUID,
    val unitId: UUID,
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
    val email: String? = null,
    val phone: String? = null,
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
    val guardians: List<DevPerson> = emptyList()
) {
    fun toPersonDTO() = PersonDTO(
        id = this.id,
        customerId = 0, // not used
        identity = this.ssn?.let { ExternalIdentifier.SSN.getInstance(it) } ?: ExternalIdentifier.NoID(),
        firstName = this.firstName,
        lastName = this.lastName,
        email = this.email,
        phone = this.phone,
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
        invoicingPostOffice = this.invoicingPostOffice
    )
}

data class DevParentship(
    val id: UUID?,
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
    val roles: Set<UserRole> = setOf()
)

data class DevMobileDevice(
    val id: UUID?,
    val unitId: UUID,
    val name: String = "Laite",
    val deleted: Boolean = false,
    val longTermToken: UUID? = null
)

data class DaycareAclInsert(
    val externalId: ExternalId
)

data class ClubTerm(
    val id: UUID?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val applicationPeriodStartDate: LocalDate,
    val applicationPeriodEndDate: LocalDate
)

data class PlacementPlan(
    val unitId: UUID,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val preschoolDaycarePeriodStart: LocalDate?,
    val preschoolDaycarePeriodEnd: LocalDate?
)

data class ApplicationWithForm(
    val id: UUID?,
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
    val applicationId: UUID,
    val createdDate: OffsetDateTime? = OffsetDateTime.now(),
    val revision: Int,
    val document: DaycareFormV0,
    val updated: OffsetDateTime? = OffsetDateTime.now()
)

data class DevPricing(
    val id: UUID? = UUID.randomUUID(),
    val validFrom: LocalDate,
    val validTo: LocalDate?,
    val multiplier: Double,
    val maxThresholdDifference: Int,
    val minThreshold2: Int,
    val minThreshold3: Int,
    val minThreshold4: Int,
    val minThreshold5: Int,
    val minThreshold6: Int,
    val thresholdIncrease6Plus: Int
)

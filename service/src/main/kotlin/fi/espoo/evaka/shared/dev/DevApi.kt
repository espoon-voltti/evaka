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
import fi.espoo.evaka.identity.ExternalIdentifier
import fi.espoo.evaka.invoicing.data.upsertFeeDecisions
import fi.espoo.evaka.invoicing.data.upsertInvoices
import fi.espoo.evaka.invoicing.data.upsertValueDecisions
import fi.espoo.evaka.invoicing.domain.FeeDecision
import fi.espoo.evaka.invoicing.domain.Invoice
import fi.espoo.evaka.invoicing.domain.VoucherValueDecision
import fi.espoo.evaka.pis.Employee
import fi.espoo.evaka.pis.createPersonFromVtj
import fi.espoo.evaka.pis.deleteEmployeeByAad
import fi.espoo.evaka.pis.deleteEmployeeRolesByAad
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
import fi.espoo.evaka.shared.config.Roles
import fi.espoo.evaka.shared.db.handle
import fi.espoo.evaka.shared.db.transaction
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.Coordinate
import fi.espoo.evaka.shared.domain.NotFound
import fi.espoo.evaka.shared.message.MockEvakaMessageClient
import fi.espoo.evaka.shared.message.SuomiFiMessage
import fi.espoo.evaka.vtjclient.dto.VtjPerson
import fi.espoo.evaka.vtjclient.service.persondetails.MockPersonDetailsService
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
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

@Profile("enable_dev_api")
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
class DevApiSecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .requestMatchers()
            .antMatchers("/dev-api/**/*")
            .and()
            .authorizeRequests()
            .anyRequest()
            .permitAll()
    }
}

private val fakeAdmin = AuthenticatedUser(
    id = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    roles = setOf(UserRole.ADMIN)
)

@Profile("enable_dev_api")
@RestController
@RequestMapping("/dev-api")
class DevApi(
    private val jdbi: Jdbi,
    private val objectMapper: ObjectMapper,
    private val personService: PersonService,
    private val asyncJobRunner: AsyncJobRunner,
    private val placementPlanService: PlacementPlanService,
    private val applicationStateService: ApplicationStateService
) {
    @PostMapping("/clean-up")
    fun cleanUpDatabase(): ResponseEntity<Unit> {
        jdbi.transaction { it.clearDatabase() }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/run-jobs")
    fun runJobs(): ResponseEntity<Unit> {
        asyncJobRunner.runPendingJobsSync()
        asyncJobRunner.waitUntilNoRunningJobs(timeout = Duration.ofSeconds(20))
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/applications/{id}")
    fun deleteApplicationByApplicationId(@PathVariable id: UUID): ResponseEntity<Unit> {
        jdbi.transaction { it.deleteApplication(id) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/care-areas")
    fun createCareAreas(@RequestBody careAreas: List<DevCareArea>): ResponseEntity<Unit> {
        jdbi.transaction { careAreas.forEach { careArea -> it.insertTestCareArea(careArea) } }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/care-areas/{id}")
    fun deleteArea(@PathVariable id: UUID): ResponseEntity<Unit> {
        jdbi.transaction { it.deleteCareArea(id) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/daycares")
    fun createDaycares(@RequestBody daycares: List<DevDaycare>): ResponseEntity<Unit> {
        jdbi.transaction { daycares.forEach { daycare -> it.insertTestDaycare(daycare) } }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/daycares/{id}")
    fun deleteDaycare(@PathVariable id: UUID): ResponseEntity<Unit> {
        jdbi.transaction { it.deleteDaycare(id) }
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/daycares/{daycareId}/acl")
    fun allowSupervisorToAccessDaycare(
        @PathVariable daycareId: UUID,
        @RequestBody body: DaycareAclInsert
    ): ResponseEntity<Unit> {
        jdbi.transaction { h ->
            updateDaycareAcl(h, daycareId, body.personAad, UserRole.UNIT_SUPERVISOR)
        }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/daycares/{daycareId}/acl/{userAad}")
    fun removeUserAccessToDaycare(@PathVariable daycareId: UUID, @PathVariable userAad: UUID): ResponseEntity<Unit> {
        jdbi.transaction { h ->
            removeDaycareAcl(h, daycareId, userAad)
        }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/daycare-groups")
    fun createDaycareGroups(@RequestBody groups: List<DevDaycareGroup>): ResponseEntity<Unit> {
        jdbi.transaction {
            groups.forEach { group -> it.insertTestDaycareGroup(group) }
        }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/daycare-groups/{id}")
    fun removeDaycareGroup(@PathVariable id: UUID): ResponseEntity<Unit> {
        jdbi.transaction { h ->
            h.deleteDaycareGroup(id)
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
    fun createDaycareCaretakers(@RequestBody caretakers: List<Caretaker>): ResponseEntity<Unit> {
        jdbi.transaction { h ->
            caretakers.forEach { caretaker ->
                insertTestCaretakers(
                    h,
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
    fun createChildren(@RequestBody children: List<DevChild>): ResponseEntity<Unit> {
        jdbi.transaction { children.forEach { child -> it.insertTestChild(child) } }
        return ResponseEntity.noContent().build()
    }

    // also cascades delete to daycare_placement and group_placement
    @DeleteMapping("/children/{id}")
    fun deleteChild(@PathVariable id: UUID): ResponseEntity<Unit> {
        jdbi.transaction { it.deleteChild(id) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/daycare-placements")
    fun createDaycarePlacements(@RequestBody placements: List<DevPlacement>): ResponseEntity<Unit> {
        jdbi.transaction { placements.forEach { placement -> it.insertTestPlacement(placement) } }
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
    fun createDecisions(@RequestBody decisions: List<DecisionRequest>): ResponseEntity<Unit> {
        decisions.forEach { decision ->
            jdbi.handle { h ->
                insertDecision(
                    h,
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
    fun createFeeDecisions(@RequestBody decisions: List<FeeDecision>): ResponseEntity<Unit> {
        jdbi.transaction { h -> upsertFeeDecisions(h, objectMapper, decisions) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/value-decisions")
    fun createVoucherValueDecisions(@RequestBody decisions: List<VoucherValueDecision>): ResponseEntity<Unit> {
        jdbi.transaction { h -> h.upsertValueDecisions(objectMapper, decisions) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/invoices")
    fun createInvoices(@RequestBody invoices: List<Invoice>): ResponseEntity<Unit> {
        jdbi.handle(insertInvoices(invoices))
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/pricing")
    fun createPricing(@RequestBody pricing: DevPricing): ResponseEntity<UUID> = jdbi.transaction {
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
    fun clearPricing(): ResponseEntity<Unit> {
        jdbi.handle { it.createUpdate("DELETE FROM pricing").execute() }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/pricing/{id}")
    fun deletePricing(@PathVariable id: UUID): ResponseEntity<Unit> {
        jdbi.transaction { it.deletePricing(id) }
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/incomes/person/{id}")
    fun deleteIncomesByPerson(@PathVariable id: UUID): ResponseEntity<Unit> {
        jdbi.transaction { it.deleteIncome(id) }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/person")
    fun upsertPerson(@RequestBody body: DevPerson): ResponseEntity<PersonDTO> {
        if (body.ssn == null) throw BadRequest("SSN is required for using this endpoint")
        return jdbi.transaction { h ->
            val person = h.getPersonBySSN(body.ssn)
            val personDTO = body.toPersonDTO()

            if (person != null) {
                h.updatePersonFromVtj(personDTO).let { ResponseEntity.ok(it) }
            } else {
                h.createPersonFromVtj(personDTO).let { ResponseEntity.ok(it) }
            }
        }
    }

    @PostMapping("/person/create")
    fun createPerson(@RequestBody body: DevPerson): ResponseEntity<UUID> {
        return jdbi.transaction { h ->
            val personId = h.insertTestPerson(body)
            val dto = body.copy(id = personId).toPersonDTO()
            if (dto.identity is ExternalIdentifier.SSN) {
                h.updatePersonFromVtj(dto)
            }
            ResponseEntity.ok(personId)
        }
    }

    @DeleteMapping("/person/{id}")
    fun deletePerson(@PathVariable id: UUID): ResponseEntity<Unit> {
        jdbi.transaction {
            it.execute(
                """
WITH applications AS (DELETE FROM application WHERE child_id = ? OR guardian_id = ? RETURNING id)
DELETE FROM application_form USING applications WHERE application_id = applications.id""",
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
    fun deletePersonBySsn(@PathVariable ssn: String): ResponseEntity<Unit> {
        jdbi.handle { h ->
            h.createQuery("SELECT id FROM person WHERE social_security_number = :ssn")
                .bind("ssn", ssn)
                .mapTo<UUID>()
                .firstOrNull()
        }?.let { uuid ->
            deletePerson(uuid)
        }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/parentship")
    fun createParentships(@RequestBody parentships: List<DevParentship>): ResponseEntity<List<DevParentship>> {
        return jdbi.handle { h -> parentships.map { h.insertTestParentship(it) } }.let { ResponseEntity.ok(it) }
    }

    @GetMapping("/employee")
    fun getEmployees(): ResponseEntity<List<Employee>> {
        return ResponseEntity.ok(jdbi.transaction { it.getEmployees() })
    }

    @PostMapping("/employee")
    fun createEmployee(@RequestBody body: DevEmployee): ResponseEntity<UUID> {
        return ResponseEntity.ok(jdbi.transaction { it.insertTestEmployee(body) })
    }

    @DeleteMapping("/employee/{aad}")
    fun deleteEmployee(@PathVariable aad: UUID): ResponseEntity<Unit> {
        jdbi.transaction { it.deleteEmployeeByAad(aad) }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/employee/aad/{aad}")
    fun upsertEmployeeByAad(@PathVariable aad: UUID, @RequestBody employee: DevEmployee): ResponseEntity<UUID> = jdbi.transaction {
        ResponseEntity.ok(
            it.createUpdate(
                """
INSERT INTO employee (first_name, last_name, email, aad_object_id, roles)
VALUES (:firstName, :lastName, :email, :aad, :roles::user_role[])
ON CONFLICT (aad_object_id) DO UPDATE SET
    first_name = excluded.first_name,
    last_name = excluded.last_name,
    email = excluded.email,
    roles = excluded.roles
RETURNING id
"""
            ).bindKotlin(employee).executeAndReturnGeneratedKeys().mapTo<UUID>().single()
        )
    }

    @DeleteMapping("/employee/roles/aad/{aad}")
    fun deleteEmployeeRolesByAad(@PathVariable aad: UUID): ResponseEntity<Unit> {
        jdbi.transaction { it.deleteEmployeeRolesByAad(aad) }
        return ResponseEntity.ok().build()
    }

    @PostMapping("/child")
    fun insertChild(@RequestBody body: DevPerson): ResponseEntity<UUID> = jdbi.transaction {
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

    @PostMapping("/backup-cares")
    fun createBackupCares(@RequestBody backupCares: List<DevBackupCare>): ResponseEntity<Unit> {
        jdbi.transaction { h -> backupCares.forEach { insertTestBackupCare(h, it) } }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/applications")
    fun createApplications(@RequestBody applications: List<ApplicationWithForm>): ResponseEntity<List<UUID>> {
        val uuids =
            jdbi.transaction { h ->
                applications.map { application ->
                    val id = insertApplication(h, application)
                    application.form?.let { applicationFormString ->
                        insertApplicationForm(
                            h,
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
        @PathVariable("application-id") applicationId: UUID,
        @RequestBody placementPlan: PlacementPlan
    ): ResponseEntity<Unit> {
        jdbi.transaction { h ->
            val application = fetchApplicationDetails(h, applicationId)
                ?: throw NotFound("application $applicationId not found")
            val preschoolDaycarePeriod = if (placementPlan.preschoolDaycarePeriodStart != null) ClosedPeriod(
                placementPlan.preschoolDaycarePeriodStart, placementPlan.preschoolDaycarePeriodEnd!!
            ) else null

            placementPlanService.createPlacementPlan(
                h,
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
    fun getMessages(): ResponseEntity<List<SuomiFiMessage>> {
        return ResponseEntity.ok(MockEvakaMessageClient.getMessages())
    }

    @PostMapping("/messages/clean-up")
    fun cleanUpMessages(): ResponseEntity<Unit> {
        MockEvakaMessageClient.clearMessages()
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/vtj-persons")
    fun upsertPerson(@RequestBody person: VtjPerson): ResponseEntity<Unit> {
        MockPersonDetailsService.upsertPerson(person)
        jdbi.handle { h ->
            val uuid = h.createQuery("SELECT id FROM person WHERE social_security_number = :ssn")
                .bind("ssn", person.socialSecurityNumber)
                .mapTo<UUID>()
                .firstOrNull()

            uuid?.let {
                // Refresh Pis data by forcing refresh from VTJ
                val dummyUser = AuthenticatedUser(it, setOf(Roles.SERVICE_WORKER))
                personService.getUpToDatePerson(h, dummyUser, it)
            }
        }
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/vtj-persons/{ssn}")
    fun getVtjPerson(@PathVariable ssn: String): ResponseEntity<VtjPerson> {
        return MockPersonDetailsService.getPerson(ssn)?.let { person -> ResponseEntity.ok(person) }
            ?: throw NotFound("vtj person $ssn was not found")
    }

    @DeleteMapping("/vtj-persons/{ssn}")
    fun deleteVtjPerson(@PathVariable ssn: String): ResponseEntity<Unit> {
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
        @PathVariable applicationId: UUID,
        @PathVariable action: String
    ): ResponseEntity<Unit> {
        val simpleActions = mapOf(
            "move-to-waiting-placement" to applicationStateService::moveToWaitingPlacement,
            "cancel-application" to applicationStateService::cancelApplication,
            "set-verified" to applicationStateService::setVerified,
            "set-unverified" to applicationStateService::setUnverified,
            "confirm-placement-without-decision" to applicationStateService::confirmPlacementWithoutDecision,
            "send-decisions-without-proposal" to applicationStateService::sendDecisionsWithoutProposal,
            "send-placement-proposal" to applicationStateService::sendPlacementProposal,
            "confirm-decision-mailed" to applicationStateService::confirmDecisionMailed
        )

        val actionFn = simpleActions[action] ?: throw NotFound("Action not recognized")
        jdbi.transaction { h ->
            ensureFakeAdminExists(h)
            actionFn.invoke(h, fakeAdmin, applicationId)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/applications/{applicationId}/actions/create-placement-plan")
    fun createPlacementPlan(
        @PathVariable applicationId: UUID,
        @RequestBody body: DaycarePlacementPlan
    ): ResponseEntity<Unit> {
        jdbi.transaction { h ->
            ensureFakeAdminExists(h)
            applicationStateService.createPlacementPlan(h, fakeAdmin, applicationId, body)
        }
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/applications/{applicationId}/actions/create-default-placement-plan")
    fun createDefaultPlacementPlan(
        @PathVariable applicationId: UUID
    ): ResponseEntity<Unit> {
        jdbi.transaction { h ->
            ensureFakeAdminExists(h)
            placementPlanService.getPlacementPlanDraft(h, applicationId)
                .let {
                    DaycarePlacementPlan(
                        unitId = it.preferredUnits.first().id,
                        period = it.period,
                        preschoolDaycarePeriod = it.preschoolDaycarePeriod
                    )
                }
                .let { applicationStateService.createPlacementPlan(h, fakeAdmin, applicationId, it) }
        }

        return ResponseEntity.noContent().build()
    }
}

fun ensureFakeAdminExists(h: Handle) {
    // language=sql
    val sql =
        """
        INSERT INTO employee (id, first_name, last_name, email, aad_object_id, roles)
        VALUES (:id, 'Dev', 'API', 'dev.api@espoo.fi', :id, '{ADMIN, SERVICE_WORKER}'::user_role[])
        ON CONFLICT DO NOTHING
        """.trimIndent()

    h.createUpdate(sql).bind("id", fakeAdmin.id).execute()
}

fun deserializeApplicationForm(jsonString: String): DaycareFormV0 {
    return objectMapper().treeToValue<EnduserDaycareFormJSON>(objectMapper().readTree(jsonString))!!.deserialize()
}

fun Handle.clearDatabase() = listOf(
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

fun Handle.deleteApplication(id: UUID) {
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
    execute("DELETE FROM daycare WHERE care_area_id = ?", id)
    execute("DELETE FROM care_area WHERE id = ?", id)
}

fun Handle.deleteDaycare(id: UUID) {
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
    val aad: UUID? = null,
    val roles: Set<UserRole> = setOf()
)

data class DaycareAclInsert(
    val personAad: UUID
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

// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.AuditContext
import evaka.core.FullApplicationTest
import evaka.core.shared.McpAuthorizationId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.BadRequest
import evaka.core.shared.domain.HelsinkiDateTime
import evaka.core.shared.domain.MockEvakaClock
import evaka.core.snDefaultDaycare
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import tools.jackson.databind.JsonNode
import tools.jackson.databind.exc.InvalidDefinitionException
import tools.jackson.databind.json.JsonMapper

class McpToolsRowsIntegrationTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var tools: McpTools
    @Autowired private lateinit var config: McpServerConfig
    @Autowired private lateinit var serviceJsonMapper: JsonMapper

    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 9, 23), LocalTime.of(12, 0))
    private val clock = MockEvakaClock(now)
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private lateinit var authorizationId: McpAuthorizationId

    @BeforeEach
    fun beforeEach() {
        authorizationId = db.transaction { tx ->
            tx.insert(admin)
            tx.insert(snDefaultDaycare)
            val clientId =
                tx.insertMcpClient(
                    "Test",
                    null,
                    null,
                    null,
                    listOf(CALLBACK),
                    "none",
                    null,
                    null,
                    now,
                )
            tx.insertMcpAuthorization(
                clientId,
                admin.id,
                McpServerConfig.MCP_SCOPE,
                now.plusDays(1),
                "code",
                now,
                "challenge",
                CALLBACK,
                null,
                now,
            )
        }
    }

    @Test
    fun `every row type can be deserialized with the service JSON mapper`() {
        McpToolsRows(serviceJsonMapper).rowTypes.forEach { (type, rowType) ->
            val error = runCatching {
                serviceJsonMapper.readValue("{}", rowType.clazz.java)
            }
                .exceptionOrNull()
            assertFalse(error is InvalidDefinitionException, "$type: ${error?.message}")
        }
    }

    @Test
    fun `every row type is tracked in an existing table that the cascade can delete from`() {
        val tablesWithUuidId = db.read { tx -> McpSchemaCatalog(tx).tablesWithUuidId }
        McpToolsRows(serviceJsonMapper).rowTypes.forEach { (type, rowType) ->
            if (rowType.table != null) {
                assertTrue(rowType.table in tablesWithUuidId, "$type: ${rowType.table}")
            }
        }
    }

    @Test
    fun `a scenario built with insert_rows is deleted without untracked rows`() {
        val area = UUID.randomUUID()
        val unit = UUID.randomUUID()
        val group = UUID.randomUUID()
        val employee = UUID.randomUUID()
        val guardian = UUID.randomUUID()
        val partner = UUID.randomUUID()
        val child = UUID.randomUUID()
        val placement = UUID.randomUUID()
        val template = UUID.randomUUID()
        val ts = "2026-09-23T12:00:00+03:00"
        val result =
            insertRows(
                "care_area" to """{"id":"$area","name":"Testialue","shortName":"testialue"}""",
                "daycare" to
                    """{"id":"$unit","areaId":"$area","name":"Testipäiväkoti","enabledPilotFeatures":["MESSAGING","MOBILE","RESERVATIONS"]}""",
                "daycare_group" to
                    """{"id":"$group","daycareId":"$unit","name":"Menninkäiset","startDate":"2026-01-01"}""",
                "daycare_caretaker" to
                    """{"groupId":"$group","amount":3,"startDate":"2026-01-01"}""",
                "employee" to """{"id":"$employee","firstName":"Essi","lastName":"Esimies"}""",
                "daycare_acl" to
                    """{"daycareId":"$unit","employeeId":"$employee","role":"UNIT_SUPERVISOR"}""",
                "daycare_group_acl" to """{"groupId":"$group","employeeId":"$employee"}""",
                "adult" to
                    """{"id":"$guardian","firstName":"Anna","lastName":"Ankka"},{"id":"$partner","firstName":"Aku","lastName":"Ankka"}""",
                "child" to
                    """{"id":"$child","firstName":"Hupu","lastName":"Ankka","dateOfBirth":"2022-05-05"}""",
                "guardian" to """{"guardianId":"$guardian","childId":"$child"}""",
                "fridge_child" to
                    """{"childId":"$child","headOfChildId":"$guardian","startDate":"2022-05-05","endDate":"2040-05-04"}""",
                "fridge_partnership" to
                    """{"first":"$guardian","second":"$partner","startDate":"2020-01-01"}""",
                "placement" to
                    """{"id":"$placement","childId":"$child","unitId":"$unit","startDate":"2026-08-01","endDate":"2027-07-31"}""",
                "service_need" to
                    """{"placementId":"$placement","startDate":"2026-08-01","endDate":"2027-07-31","optionId":"${snDefaultDaycare.id}","confirmedBy":"${admin.id}"}""",
                "daycare_group_placement" to
                    """{"daycarePlacementId":"$placement","daycareGroupId":"$group","startDate":"2026-08-01","endDate":"2027-07-31"}""",
                "attendance_reservation" to
                    """{"childId":"$child","date":"2026-09-24","startTime":"08:00","endTime":"16:00","createdBy":"${admin.id}"}""",
                "absence" to
                    """{"childId":"$child","date":"2026-09-25","absenceCategory":"BILLABLE"}""",
                "child_attendance" to
                    """{"childId":"$child","unitId":"$unit","date":"2026-09-23","arrived":"08:00","departed":"16:00"}""",
                "document_template" to
                    """{"id":"$template","validity":{"start":"2026-01-01","end":null},"content":{"sections":[]}}""",
                "child_document" to
                    """{"status":"DRAFT","childId":"$child","templateId":"$template","content":{"answers":[]},"modifiedAt":"$ts","modifiedBy":"${admin.id}","contentLockedAt":"$ts","contentLockedBy":null}""",
                "application" to
                    """{"id":"${UUID.randomUUID()}","type":"DAYCARE","createdAt":"$ts","createdBy":"${admin.id}","modifiedAt":"$ts","modifiedBy":"${admin.id}","sentDate":"2026-09-01","status":"SENT","guardianId":"$guardian","childId":"$child","origin":"ELECTRONIC","checkedByAdmin":false,"hideFromGuardian":false,"transferApplication":false,"otherGuardians":[],
                    "form":{"child":{"person":{"firstName":"Hupu","lastName":"Ankka"},"dateOfBirth":"2022-05-05","nationality":"FI","language":"fi","allergies":"","diet":"","assistanceNeeded":false,"assistanceDescription":""},
                    "guardian":{"person":{"firstName":"Anna","lastName":"Ankka"},"phoneNumber":"0401234567"},"otherChildren":[],"otherInfo":"",
                    "preferences":{"preferredUnits":[{"id":"$unit","name":"Testipäiväkoti"}],"preferredStartDate":"2026-11-01","preparatory":false,"urgent":false}}}""",
            )
        assertEquals(2, result.path("inserted").path("adult").asInt())
        assertEquals(1, result.path("trackedDependentRows").path("child").asInt())
        assertEquals(2, result.path("trackedDependentRows").path("message_account").asInt())

        val dryRun = callTool("delete_test_data", """{"batchName":"demo","dryRun":true}""")
        assertTrue(dryRun.path("untrackedRowCounts").isEmpty, dryRun.toString())

        callTool("delete_test_data", """{"batchName":"demo"}""")
        listOf("care_area", "daycare", "person", "placement", "application", "child_document")
            .forEach { assertEquals(0, countRows(it), it) }
        assertEquals(1, countRows("employee"))
    }

    @Test
    fun `a failing row names the row and nothing is saved`() {
        val area = UUID.randomUUID()
        val error =
            assertThrows<BadRequest> {
                insertRows(
                    "care_area" to """{"id":"$area","name":"Testialue"}""",
                    "daycare" to
                        """{"areaId":"$area","name":"Ok"},{"areaId":"${UUID.randomUUID()}","name":"Broken"}""",
                )
            }
        assertTrue(
            error.message.startsWith("Row 1 of group 1 (daycare): ") &&
                error.message.contains("violates foreign key constraint"),
            error.message,
        )
        assertEquals(0, countRows("care_area"))

        val parseError =
            assertThrows<BadRequest> { insertRows("placement" to """{"unitId":"$area"}""") }
        assertTrue(parseError.message.startsWith("Row 0 of group 0 (placement): "))
    }

    @Test
    fun `employee pins can be set only for employees created via MCP`() {
        val employee = UUID.randomUUID()
        insertRows(
            "employee" to """{"id":"$employee"}""",
            "employee_pin" to """{"userId":"$employee","pin":"1234"}""",
        )
        val error =
            assertThrows<BadRequest> {
                insertRows("employee_pin" to """{"userId":"${admin.id}","pin":"1234"}""")
            }
        assertTrue(error.message.contains("was not created via MCP"), error.message)
        assertEquals(1, countRows("employee_pin"))
    }

    private fun insertRows(vararg groups: Pair<String, String>): JsonNode =
        callTool(
            "insert_rows",
            """{"batch":"demo","rows":[${groups.joinToString { """{"type":"${it.first}","rows":[${it.second}]}""" }}]}""",
        )

    private fun callTool(name: String, arguments: String): JsonNode = db.transaction { tx ->
        val ctx = McpToolContext(tx, admin.user, clock, authorizationId, config, AuditContext())
        jsonMapper.valueToTree(
            tools.findTool(name)!!.call(ctx, jsonMapper.readTree(arguments), jsonMapper)
        )
    }

    private fun countRows(table: String): Int = db.read { tx ->
        tx.createQuery { sql("SELECT count(*) FROM $table") }.exactlyOne<Int>()
    }

    companion object {
        private const val CALLBACK = "http://localhost/callback"
    }
}

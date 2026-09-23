// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.mcp

import evaka.core.PureJdbiTest
import evaka.core.shared.Id
import evaka.core.shared.McpTestDataBatchId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.db.Database
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevGuardian
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.Conflict
import evaka.core.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class McpTestDataServiceIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val now = HelsinkiDateTime.of(LocalDate.of(2026, 9, 23), LocalTime.of(12, 0))
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val otherAdmin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val area = DevCareArea()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(otherAdmin)
            tx.insert(area)
        }
    }

    @Test
    fun `the same batch name of the same user always resolves to one batch`() {
        val (first, second) = db.transaction { tx -> tx.batch("demo") to tx.batch("demo") }
        assertEquals(first, second)

        val otherUsers = db.transaction { tx -> tx.batch("demo", createdBy = otherAdmin) }
        assertNotEquals(first, otherUsers)

        // Two concurrent tool calls may both miss the existing batch and insert; the insert must
        // then resolve to the existing row instead of creating a duplicate
        val racing = db.transaction { tx ->
            tx.insertMcpTestDataBatch(
                name = "demo",
                description = "",
                createdBy = admin.evakaUserId,
                authorizationId = null,
                now = now,
            )
        }
        assertEquals(first, racing)
        assertEquals(2, countRows("mcp_test_data_batch"))
    }

    @Test
    fun `deleting a tracked row clears ON DELETE SET NULL references instead of cascading`() {
        val handler = DevEmployee()
        val unit = DevDaycare(areaId = area.id, financeDecisionHandler = handler.id)
        val batchId = db.transaction { tx ->
            tx.insert(handler)
            tx.insert(unit)
            val batchId = tx.batch("employees")
            tx.track(batchId, "employee", handler.id)
            batchId
        }

        val result = db.transaction { tx -> tx.deleteBatch(batchId) }

        assertEquals(1, result.deletedRowCounts["employee"])
        assertEquals(null, result.deletedRowCounts["daycare"])
        assertEquals(1, countRows("daycare"))
        assertTrue(
            db.read { tx ->
                tx.createQuery {
                        sql(
                            "SELECT finance_decision_handler IS NULL FROM daycare WHERE id = ${bind(unit.id)}"
                        )
                    }
                    .exactlyOne<Boolean>()
            }
        )
        // the employee's evaka_user row is kept for audit trails, only its link is cleared
        assertEquals(
            0,
            db.read { tx ->
                tx.createQuery {
                        sql(
                            "SELECT count(*) FROM evaka_user WHERE employee_id = ${bind(handler.id)}"
                        )
                    }
                    .exactlyOne<Int>()
            },
        )
    }

    @Test
    fun `deleting a batch refuses to cascade into rows tracked in another batch`() {
        val unit = DevDaycare(areaId = area.id)
        val child = DevPerson()
        val placement = DevPlacement(childId = child.id, unitId = unit.id)
        val (unitsBatch, placementsBatch) =
            db.transaction { tx ->
                tx.insert(unit)
                tx.insert(child, DevPersonType.CHILD)
                tx.insert(placement)
                val unitsBatch = tx.batch("units")
                tx.track(unitsBatch, "daycare", unit.id)
                val placementsBatch = tx.batch("placements", createdBy = otherAdmin)
                tx.track(placementsBatch, "person", child.id)
                tx.track(placementsBatch, "placement", placement.id)
                unitsBatch to placementsBatch
            }

        val error = assertThrows<Conflict> { db.transaction { tx -> tx.deleteBatch(unitsBatch) } }
        assertTrue(error.message.contains("placements"), error.message)
        assertEquals(1, countRows("daycare"))
        assertEquals(1, countRows("placement"))
        assertEquals(2, countRows("mcp_test_data_batch"))

        db.transaction { tx -> tx.deleteBatch(placementsBatch) }
        db.transaction { tx -> tx.deleteBatch(unitsBatch) }
        assertEquals(0, countRows("daycare"))
        assertEquals(0, countRows("placement"))
        assertEquals(0, countRows("person"))
        assertEquals(0, countRows("mcp_test_data_batch"))
    }

    @Test
    fun `rows created outside MCP on top of the batch are only deleted when explicitly allowed`() {
        val unit = DevDaycare(areaId = area.id)
        val child = DevPerson()
        val placement = DevPlacement(childId = child.id, unitId = unit.id)
        val batchId = db.transaction { tx ->
            tx.insert(unit)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            val batchId = tx.batch("units")
            tx.track(batchId, "daycare", unit.id)
            batchId
        }

        val preview = db.transaction { tx -> McpTestDataService.previewDeletion(tx, batchId) }
        assertEquals(1, preview.trackedEntities)
        assertEquals(1, preview.deletedRowCounts["daycare"])
        assertEquals(1, preview.deletedRowCounts["placement"])
        assertEquals(mapOf("placement" to 1), preview.untrackedRowCounts)
        assertEquals(1, countRows("daycare"))
        assertEquals(1, countRows("placement"))
        assertEquals(1, countRows("mcp_test_data_batch"))

        val refused =
            assertThrows<Conflict> {
                db.transaction { tx -> tx.deleteBatch(batchId, allowUntrackedRows = false) }
            }
        assertTrue(refused.message.contains("placement: 1"), refused.message)
        assertEquals(1, countRows("daycare"), "the refused deletion was rolled back")
        assertEquals(1, countRows("placement"))
        assertEquals(1, countRows("mcp_test_data_batch"))

        val result = db.transaction { tx -> tx.deleteBatch(batchId, allowUntrackedRows = true) }
        assertEquals(preview.deletedRowCounts, result.deletedRowCounts)
        assertEquals(preview.untrackedRowCounts, result.untrackedRowCounts)
        assertEquals(0, countRows("daycare"))
        assertEquals(0, countRows("placement"))
        assertEquals(1, countRows("person"), "the child was not part of the batch")
        assertEquals(0, countRows("mcp_test_data_batch"))
    }

    @Test
    fun `link rows that cannot be tracked do not count as rows created outside MCP`() {
        val guardian = DevPerson()
        val child = DevPerson()
        val batchId = db.transaction { tx ->
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(DevGuardian(guardianId = guardian.id, childId = child.id))
            val batchId = tx.batch("family")
            tx.track(batchId, "person", child.id)
            tx.track(batchId, "child", child.id)
            batchId
        }

        // the guardian table has no id column, so its rows can only ever be deleted along with the
        // child; deleting them must not require allowing untracked rows
        val result = db.transaction { tx -> tx.deleteBatch(batchId, allowUntrackedRows = false) }
        assertEquals(mapOf("child" to 1, "guardian" to 1, "person" to 1), result.deletedRowCounts)
        assertEquals(emptyMap(), result.untrackedRowCounts)
        assertEquals(1, countRows("person"), "the guardian person was not part of the batch")
        assertEquals(0, countRows("guardian"))
    }

    private fun Database.Transaction.batch(
        name: String,
        createdBy: DevEmployee = admin,
    ): McpTestDataBatchId =
        McpTestDataService.getOrCreateBatch(this, name, createdBy.evakaUserId, null, now)

    private fun Database.Transaction.track(batchId: McpTestDataBatchId, table: String, id: Id<*>) =
        McpTestDataService.track(this, batchId, table, id, "", now)

    private fun Database.Transaction.deleteBatch(
        batchId: McpTestDataBatchId,
        allowUntrackedRows: Boolean = true,
    ) = McpTestDataService.deleteBatch(this, batchId, allowUntrackedRows)

    private fun countRows(table: String): Int = db.read { tx ->
        tx.createQuery { sql("SELECT count(*) FROM $table") }.exactlyOne<Int>()
    }
}

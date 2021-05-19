package fi.espoo.evaka.varda

import fi.espoo.evaka.FixtureBuilder
import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.serviceneednew.ServiceNeedOption
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestCaretakers
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestEmployee
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.snDefaultPartDayDaycare
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class VardaUpdateServiceV2IntegrationTest : PureJdbiTest() {
    val today = HelsinkiDateTime.of(LocalDateTime.of(2021, 5, 19, 12, 0))

    val careArea1: UUID = UUID.randomUUID()
    val daycareInArea1: UUID = UUID.randomUUID()
    val daycareGroup1: UUID = UUID.randomUUID()
    val employeeId: UUID = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        db.transaction {
            it.resetDatabase()

            it.insertTestEmployee(
                DevEmployee(
                    id = employeeId
                )
            )

            it.insertTestCareArea(DevCareArea(id = careArea1, name = "1", shortName = "1"))
            it.insertTestDaycare(
                DevDaycare(
                    id = daycareInArea1,
                    areaId = careArea1,
                    type = setOf(CareType.CENTRE, CareType.PRESCHOOL, CareType.PREPARATORY_EDUCATION)
                )
            )
            it.insertTestDaycareGroup(DevDaycareGroup(id = daycareGroup1, daycareId = daycareInArea1))
            it.insertTestCaretakers(groupId = daycareGroup1, amount = 3.0)
        }

        insertTestVardaUnit(db, daycareInArea1)
    }

    @Test
    fun `calculateServiceNeedsChanges finds changed service need since given moment`() {
        val earliestIncludedTime = HelsinkiDateTime.now()
        val shouldBeIncluded = earliestIncludedTime.plusHours(1)
        val shouldBeExcluded = earliestIncludedTime.plusHours(-1)
        val includedServiceNeedId = createServiceNeed(
            db, shouldBeIncluded,
            snDefaultDaycare.copy(
                updated = shouldBeIncluded
            )
        )
        createServiceNeed(
            db, shouldBeExcluded,
            snDefaultPartDayDaycare.copy(
                updated = shouldBeExcluded
            )
        )

        db.read {
            val changes = it.getEvakaServiceNeedChanges(earliestIncludedTime)
            assertEquals(1, changes.size)
            assertEquals(includedServiceNeedId, changes.get(0).evakaServiceNeedId)
            assertEquals(shouldBeIncluded, changes.get(0).evakaLastUpdated)
        }
    }

    @Test
    fun `calculateServiceNeedsChanges finds changed service need option since given moment`() {
        val earliestIncludedTime = HelsinkiDateTime.now()
        val shouldBeIncluded = earliestIncludedTime.plusHours(1)
        val shouldBeExcluded = earliestIncludedTime.plusHours(-1)
        val includedServiceNeedId = createServiceNeed(
            db, shouldBeExcluded,
            snDefaultDaycare.copy(
                updated = shouldBeIncluded
            )
        )
        createServiceNeed(
            db, shouldBeExcluded,
            snDefaultPartDayDaycare.copy(
                updated = shouldBeExcluded
            )
        )

        db.read {
            val changes = it.getEvakaServiceNeedChanges(earliestIncludedTime)
            assertEquals(1, changes.size)
            assertEquals(includedServiceNeedId, changes.get(0).evakaServiceNeedId)
            assertEquals(shouldBeIncluded, changes.get(0).evakaLastUpdated)
        }
    }

    @Test
    fun `calculateEvakaVsVardaServiceNeedChangesByChild finds new evaka service need when varda has none`() {
        val since = HelsinkiDateTime.now()
        val option = snDefaultDaycare.copy(updated = since)
        val snId = createServiceNeed(db, since, option)
        val childId = db.read { it.getChidIdByServiceNeedId(snId) }

        val diffs = calculateEvakaVsVardaServiceNeedChangesByChild(db, since)
        assertEquals(1, diffs.keys.size)
        assertServiceNeedDiffSizes(diffs.get(childId), 1, 0, 0)
    }

    private fun assertServiceNeedDiffSizes(diff: VardaChildCalculatedServiceNeedChanges?, expectedAdditions: Int, expectedUpdates: Int, expectedDeletes: Int) {
        assertNotNull(diff)
        if (diff != null) {
            assertEquals(expectedAdditions, diff.additions.size)
            assertEquals(expectedUpdates, diff.updates.size)
            assertEquals(expectedDeletes, diff.deletes.size)
        }
    }

    private fun createServiceNeed(db: Database.Connection, updated: HelsinkiDateTime, option: ServiceNeedOption): UUID {
        var serviceNeedId = UUID.randomUUID()
        db.transaction { tx ->
            FixtureBuilder(tx, today.toLocalDate())
                .addChild().withAge(3, 0).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).fromDay(-100).saveAnd {
                        addServiceNeed()
                            .withId(serviceNeedId)
                            .withUpdated(updated)
                            .createdBy(employeeId)
                            .withOption(option)
                            .save()
                    }
                }
        }
        return serviceNeedId
    }
}

private fun insertTestVardaUnit(db: Database.Connection, id: UUID) = db.transaction {
    // language=sql
    val sql =
        """
        INSERT INTO varda_unit (evaka_daycare_id, varda_unit_id, uploaded_at)
        VALUES (:id, 123222, now())
        """.trimIndent()
    it.createUpdate(sql)
        .bind("id", id)
        .execute()

    val sql2 = "UPDATE daycare SET oph_unit_oid = :ophUnitOid WHERE daycare.id = :id;"

    it.createUpdate(sql2)
        .bind("id", id)
        .bind("ophUnitOid", "1.2.3332211")
        .execute()
}

private fun Database.Read.getChidIdByServiceNeedId(serviceNeedId: UUID): UUID? = createQuery(
    """
SELECT p.child_id FROM placement p LEFT JOIN new_service_need sn ON p.id = sn.placement_id
WHERE sn.id = :serviceNeedId
        """
).bind("serviceNeedId", serviceNeedId)
    .mapTo<UUID>()
    .first()

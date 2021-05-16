package fi.espoo.evaka.occupancy

import fi.espoo.evaka.ChildBuilder
import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
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
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.snDefaultPartDayDaycare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class OccupancyTest : PureJdbiTest() {
    val today = LocalDate.of(2020, 1, 1)

    val careArea1: UUID = UUID.randomUUID()
    val careArea2: UUID = UUID.randomUUID()

    val daycareInArea1: UUID = UUID.randomUUID()
    val daycareGroup1: UUID = UUID.randomUUID()
    val daycareGroup2: UUID = UUID.randomUUID()

    val familyUnitInArea2: UUID = UUID.randomUUID()
    val familyGroup1: UUID = UUID.randomUUID()
    val familyGroup2: UUID = UUID.randomUUID()

    val employeeId: UUID = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        db.transaction {
            it.resetDatabase()

            it.insertServiceNeedOptions()
            it.insertTestEmployee(
                DevEmployee(
                    id = employeeId
                )
            )

            it.insertTestCareArea(DevCareArea(id = careArea1, name = "1", shortName = "1"))
            it.insertTestCareArea(DevCareArea(id = careArea2, name = "2", shortName = "2"))

            it.insertTestDaycare(
                DevDaycare(
                    id = daycareInArea1,
                    areaId = careArea1,
                    type = setOf(CareType.CENTRE, CareType.PRESCHOOL, CareType.PREPARATORY_EDUCATION)
                )
            )
            it.insertTestDaycareGroup(DevDaycareGroup(id = daycareGroup1, daycareId = daycareInArea1))
            it.insertTestDaycareGroup(DevDaycareGroup(id = daycareGroup2, daycareId = daycareInArea1))
            it.insertTestCaretakers(groupId = daycareGroup1, amount = 3.0)
            it.insertTestCaretakers(groupId = daycareGroup2, amount = 3.0)

            it.insertTestDaycare(
                DevDaycare(
                    id = familyUnitInArea2,
                    areaId = careArea2,
                    type = setOf(CareType.FAMILY)
                )
            )
            it.insertTestDaycareGroup(DevDaycareGroup(id = familyGroup1, daycareId = familyUnitInArea2))
            it.insertTestDaycareGroup(DevDaycareGroup(id = familyGroup2, daycareId = familyUnitInArea2))
            it.insertTestCaretakers(groupId = familyGroup1, amount = 3.0)
            it.insertTestCaretakers(groupId = familyGroup2, amount = 3.0)
        }
    }

    @Test
    fun `calculateDailyUnitOccupancyValues smoke test`() {
        db.transaction { tx ->
            ChildBuilder(tx, today).childOfAge(3)
                .hasPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).fromDay(-1).toDay(0).exec()
                .withGroupPlacement().toGroup(daycareGroup1).execAndDone()
                .done()
        }

        db.read {
            val period = getPeriod(-1, 0)
            val occupancyValues =
                it.calculateDailyUnitOccupancyValues(today, period, OccupancyType.CONFIRMED, areaId = careArea1)

            assertEquals(1, occupancyValues.size)
            assertEquals(daycareInArea1, occupancyValues[0].key.unitId)
            assertEquals(2, occupancyValues[0].occupancies.size)
            assertEquals(1.75, occupancyValues[0].occupancies[period.start]!!.sum)
            assertEquals(1.0, occupancyValues[0].occupancies[period.end]!!.sum)
            assertEquals(1, occupancyValues[0].occupancies[period.start]!!.headcount)
            assertEquals(1, occupancyValues[0].occupancies[period.end]!!.headcount)
            assertEquals(6.0, occupancyValues[0].occupancies[period.start]!!.caretakers)
            assertEquals(6.0, occupancyValues[0].occupancies[period.end]!!.caretakers)
            assertEquals(4.2, occupancyValues[0].occupancies[period.start]!!.percentage)
            assertEquals(2.4, occupancyValues[0].occupancies[period.end]!!.percentage)
        }
    }

    @Test
    fun `calculateDailyGroupOccupancyValues smoke test`() {
        db.transaction { tx ->
            ChildBuilder(tx, today).childOfAge(3)
                .hasPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).fromDay(-1).toDay(0).exec()
                .withGroupPlacement().toGroup(daycareGroup1).execAndDone()
                .done()
        }

        db.read { tx ->
            val occupancyValues =
                tx.calculateDailyGroupOccupancyValues(today, getPeriod(0, 0), OccupancyType.CONFIRMED, unitId = daycareInArea1)

            assertEquals(2, occupancyValues.size)
            assertEquals(1, occupancyValues.filter { it.key.groupId == daycareGroup1 }.size)
            val occupancy = occupancyValues.find { it.key.groupId == daycareGroup1 }!!.occupancies[today]!!
            assertEquals(3.0, occupancy.caretakers)
            assertEquals(1, occupancy.headcount)
            assertEquals(4.8, occupancy.percentage)
        }
    }

    @Test
    fun `occupancy for a child under 3 year old is 1,75 and `() {
        db.transaction { tx ->
            ChildBuilder(tx, today).childOfAge(3)
                .hasPlacement().ofType(PlacementType.DAYCARE_PART_TIME).toUnit(daycareInArea1).fromDay(-1).toDay(0).execAndDone()
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today.minusDays(1L), 1.75)
        }
    }

    @Test
    fun `occupancy is 1,75 when unit is family unit`() {
        db.transaction { tx ->
            ChildBuilder(tx, today).childOfAge(4)
                .hasPlacement().ofType(PlacementType.DAYCARE).toUnit(familyUnitInArea2).execAndDone()
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, familyUnitInArea2, OccupancyType.CONFIRMED, today, 1.75)
        }
    }

    @Test
    fun `daycare occupancy with default service need for a child over 3 year old is 1,0`() {
        db.transaction { tx ->
            ChildBuilder(tx, today).childOfAge(3)
                .hasPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).execAndDone()
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
        }
    }

    @Test
    fun `part time daycare occupancy with default service need for a child over 3 year old is 0,54`() {
        db.transaction { tx ->
            ChildBuilder(tx, today).childOfAge(3)
                .hasPlacement().ofType(PlacementType.DAYCARE_PART_TIME).toUnit(daycareInArea1).execAndDone()
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.54)
        }
    }

    @Test
    fun `preschool occupancy with default service need is 0,5`() {
        db.transaction { tx ->
            ChildBuilder(tx, today).childOfAge(6, 5)
                .hasPlacement().ofType(PlacementType.PRESCHOOL).toUnit(daycareInArea1).execAndDone()
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.5)
        }
    }

    @Test
    fun `preschool daycare occupancy with default service need is 1,0`() {
        db.transaction { tx ->
            ChildBuilder(tx, today).childOfAge(6, 5)
                .hasPlacement().ofType(PlacementType.PRESCHOOL_DAYCARE).toUnit(daycareInArea1).execAndDone()
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
        }
    }

    @Test
    fun `occupancy is based on service need`() {
        db.transaction { tx ->
            ChildBuilder(tx, today).childOfAge(6, 5)
                // a valid PRESCHOOL_DAYCARE service need would have occupancy 1.0
                .hasPlacement().ofType(PlacementType.PRESCHOOL_DAYCARE).toUnit(daycareInArea1).exec()
                // but child has service need of daycare part time with occupancy 0.54
                .withServiceNeed().createdBy(employeeId).withOption(snDefaultPartDayDaycare.id).execAndDone()
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.54)
        }
    }

    @Test
    fun `occupancy is multiplied by assistance need factor`() {
        db.transaction { tx ->
            ChildBuilder(tx, today).childOfAge(3)
                .hasPlacement().ofType(PlacementType.DAYCARE).fromDay(0).toDay(1).toUnit(daycareInArea1).execAndDone()
                .hasAssistanceNeed().createdBy(employeeId).withFactor(2.0).fromDay(1).toDay(1).execAndDone()
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today.plusDays(1L), 2.0)
        }
    }

    @Test
    fun `placement plan affects only planned occupancy`() {
        db.transaction { tx ->
            ChildBuilder(tx, today).childOfAge(3)
                .hasPlacementPlan().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).execAndDone()
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 1.0)
        }
    }

    private fun getPeriod(from: Int, to: Int) = FiniteDateRange(today.plusDays(from.toLong()), today.plusDays(to.toLong()))

    private fun getAndAssertOccupancyInUnit(
        tx: Database.Read,
        unitId: UUID,
        type: OccupancyType,
        date: LocalDate,
        expectedSum: Double
    ) {
        assertOccupancySum(
            expectedSum = expectedSum,
            date = date,
            groupingId = unitId,
            occupancyValues = tx.calculateDailyUnitOccupancyValues(
                today = today,
                queryPeriod = FiniteDateRange(date, date),
                type = type,
                unitId = unitId
            )
        )
    }

    private fun <K : OccupancyGroupingKey> assertOccupancySum(
        expectedSum: Double,
        period: FiniteDateRange,
        groupingId: UUID,
        occupancyValues: List<DailyOccupancyValues<K>>
    ) {
        period.dates().forEach { date -> assertOccupancySum(expectedSum, date, groupingId, occupancyValues) }
    }

    private fun <K : OccupancyGroupingKey> assertOccupancySum(
        expectedSum: Double,
        date: LocalDate,
        groupingId: UUID,
        occupancyValues: List<DailyOccupancyValues<K>>
    ) {
        assertEquals(
            expectedSum,
            occupancyValues.find { it.key.groupingId == groupingId }?.occupancies?.get(date)?.sum ?: 0.0
        )
    }
}

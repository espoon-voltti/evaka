package fi.espoo.evaka.occupancy

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.resetDatabase
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.insertTestCareArea
import fi.espoo.evaka.shared.dev.insertTestCaretakers
import fi.espoo.evaka.shared.dev.insertTestChild
import fi.espoo.evaka.shared.dev.insertTestDaycare
import fi.espoo.evaka.shared.dev.insertTestDaycareGroup
import fi.espoo.evaka.shared.dev.insertTestDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.insertTestPerson
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
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

    @BeforeEach
    internal fun setUp() {
        db.transaction {
            it.resetDatabase()

            it.insertServiceNeedOptions()

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
    fun `confirmed daycare occupancy for child under 3 year old is 1,75 and over 3 years old is 1,0`() {
        db.transaction { tx ->
            ChildBuilder(tx, today)
                .childOfAge(3)
                .hasPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).fromDay(-1).toDay(0).exec()
                .withGroupPlacement().toGroup(daycareGroup1).exec()
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

    private fun getPeriod(from: Int, to: Int) = FiniteDateRange(today.plusDays(from.toLong()), today.plusDays(to.toLong()))
}

private class ChildBuilder(
    private val tx: Database.Transaction,
    private val today: LocalDate
) {
    fun childOfAge(years: Int, months: Int = 0, days: Int = 0): ChildFixture {
        val dob = today.minusYears(years.toLong()).minusMonths(months.toLong()).minusDays(days.toLong())
        val childId = tx.insertTestPerson(DevPerson(dateOfBirth = dob))
        tx.insertTestChild(DevChild(childId))
        return ChildFixture(tx, today, childId)
    }

    class ChildFixture(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        val childId: UUID
    ) {
        fun hasPlacement() = PlacementBuilder(tx, today, childId)
    }

    class PlacementBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val childId: UUID
    ) {
        private var unitId: UUID? = null
        private var type: PlacementType? = null
        private var from: Int = 0
        private var to: Int = 0

        fun toUnit(id: UUID) = this.apply { this.unitId = id }
        fun ofType(type: PlacementType) = this.apply { this.type = type }
        fun fromDay(day: Int) = this.apply { this.from = day }
        fun toDay(day: Int) = this.apply { this.to = day }

        fun exec(): PlacementFixture {
            val placementId = tx.insertTestPlacement(
                childId = childId,
                unitId = unitId ?: throw IllegalStateException("unit not set"),
                type = type ?: throw IllegalStateException("type not set"),
                startDate = today.plusDays(from.toLong()),
                endDate = today.plusDays(to.toLong())
            )
            return PlacementFixture(
                tx = tx,
                today = today,
                childId = childId,
                placementId = placementId,
                placementPeriod = FiniteDateRange(today.plusDays(from.toLong()), today.plusDays(to.toLong()))
            )
        }
    }

    class PlacementFixture(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        val childId: UUID,
        val placementId: UUID,
        val placementPeriod: FiniteDateRange
    ) {
        fun withGroupPlacement() = GroupPlacementBuilder(tx, today, this)
    }

    class GroupPlacementBuilder(
        private val tx: Database.Transaction,
        private val today: LocalDate,
        private val placementFixture: PlacementFixture
    ) {
        private var groupId: UUID? = null
        private var from: LocalDate? = null
        private var to: LocalDate? = null

        fun toGroup(id: UUID) = this.apply { this.groupId = id }
        fun fromDay(day: Int) = this.apply { this.from = today.plusDays(day.toLong()) }
        fun toDay(day: Int) = this.apply { this.to = today.plusDays(day.toLong()) }

        fun exec(): PlacementFixture {
            tx.insertTestDaycareGroupPlacement(
                daycarePlacementId = placementFixture.placementId,
                groupId = groupId ?: throw IllegalStateException("group not set"),
                startDate = from ?: placementFixture.placementPeriod.start,
                endDate = to ?: placementFixture.placementPeriod.end
            )
            return placementFixture
        }
    }
}

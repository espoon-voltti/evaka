package fi.espoo.evaka.occupancy

import fi.espoo.evaka.FixtureBuilder
import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.service.AbsenceType
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
import fi.espoo.evaka.shared.dev.insertTestStaffAttendance
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.snDefaultPartDayDaycare
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import fi.espoo.evaka.daycare.service.CareType as AbsenceCareType

class OccupancyTest : PureJdbiTest() {
    val today = LocalDate.of(2020, 1, 16) // Thursday

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
            FixtureBuilder(tx, today)
                .addChild().withAge(3).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).fromDay(-1).toDay(0).saveAnd {
                        addGroupPlacement().toGroup(daycareGroup1).save()
                    }
                }
        }

        db.read {
            val period = FiniteDateRange(today.minusDays(1), today)
            val occupancyValues =
                it.calculateDailyUnitOccupancyValues(today, period, OccupancyType.CONFIRMED, areaId = careArea1)

            assertEquals(1, occupancyValues.size)
            assertEquals(daycareInArea1, occupancyValues[0].key.unitId)
            assertEquals(
                mapOf(
                    period.start to OccupancyValues(
                        sum = 1.75,
                        headcount = 1,
                        caretakers = 6.0,
                        percentage = 4.2
                    ),
                    period.end to OccupancyValues(
                        sum = 1.0,
                        headcount = 1,
                        caretakers = 6.0,
                        percentage = 2.4
                    )
                ),
                occupancyValues[0].occupancies
            )
        }
    }

    @Test
    fun `calculateDailyGroupOccupancyValues smoke test`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(3).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).fromDay(-1).toDay(0).saveAnd {
                        addGroupPlacement().toGroup(daycareGroup1).save()
                    }
                }
        }

        db.read { tx ->
            val occupancyValues =
                tx.calculateDailyGroupOccupancyValues(today, FiniteDateRange(today, today), OccupancyType.CONFIRMED, unitId = daycareInArea1)

            assertEquals(2, occupancyValues.size)
            assertEquals(1, occupancyValues.filter { it.key.groupId == daycareGroup1 }.size)
            val occupancy = occupancyValues.find { it.key.groupId == daycareGroup1 }!!.occupancies[today]!!
            assertEquals(3.0, occupancy.caretakers)
            assertEquals(1, occupancy.headcount)
            assertEquals(4.8, occupancy.percentage)
        }
    }

    @Test
    fun `occupancy for a child under 3 year old is 1,75`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(3).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE_PART_TIME).toUnit(daycareInArea1).fromDay(-1).toDay(0).save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today.minusDays(1L), 1.75)
        }
    }

    @Test
    fun `occupancy is 1,75 when unit is family unit`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(4).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(familyUnitInArea2).fromDay(-1).toDay(0).save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, familyUnitInArea2, OccupancyType.CONFIRMED, today, 1.75)
        }
    }

    @Test
    fun `daycare occupancy with default service need for a child over 3 year old is 1,0`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(3).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
        }
    }

    @Test
    fun `part time daycare occupancy with default service need for a child over 3 year old is 0,54`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(3).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE_PART_TIME).toUnit(daycareInArea1).save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.54)
        }
    }

    @Test
    fun `preschool occupancy with default service need is 0,5`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(6, 5).saveAnd {
                    addPlacement().ofType(PlacementType.PRESCHOOL).toUnit(daycareInArea1).save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.5)
        }
    }

    @Test
    fun `preschool daycare occupancy with default service need is 1,0`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(6, 5).saveAnd {
                    addPlacement().ofType(PlacementType.PRESCHOOL_DAYCARE).toUnit(daycareInArea1).save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
        }
    }

    @Test
    fun `occupancy is based on service need`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(6, 5).saveAnd {
                    // a valid PRESCHOOL_DAYCARE service need would have occupancy 1.0
                    addPlacement().ofType(PlacementType.PRESCHOOL_DAYCARE).toUnit(daycareInArea1).saveAnd {
                        addServiceNeed().createdBy(employeeId).withOption(snDefaultPartDayDaycare.id).save()
                    }
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.54)
        }
    }

    @Test
    fun `occupancy is multiplied by assistance need factor`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(3).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).fromDay(0).toDay(1).save()
                    addAssistanceNeed().createdBy(employeeId).withFactor(2.0).fromDay(1).toDay(1).save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today.plusDays(1), 2.0)
        }
    }

    @Test
    fun `placement plan affects only planned occupancy`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(3).saveAnd {
                    addPlacementPlan().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 1.0)
        }
    }

    @Test
    fun `deleted placement plan has no effect`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(3).saveAnd {
                    addPlacementPlan().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).asDeleted().save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 0.0)
        }
    }

    @Test
    fun `if placement plan is to different unit than overlapping placement then both are counted to planned occupancy`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(3).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).save()
                    addPlacementPlan().ofType(PlacementType.DAYCARE).toUnit(familyUnitInArea2).save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 1.0)
            getAndAssertOccupancyInUnit(tx, familyUnitInArea2, OccupancyType.PLANNED, today, 1.75)
        }
    }

    @Test
    fun `if placement plan is to same unit as overlapping placement then the maximum is counted to planned occupancy - test 1`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(3).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE_PART_TIME).toUnit(daycareInArea1).save()
                    addPlacementPlan().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.54)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 1.0)
        }
    }

    @Test
    fun `if placement plan is to same unit as overlapping placement then the maximum is counted to planned occupancy - test 2`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(3).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).save()
                    addPlacementPlan().ofType(PlacementType.DAYCARE_PART_TIME).toUnit(daycareInArea1).save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 1.0)
        }
    }

    @Test
    fun `preschool placement plan with separate daycare period is handled correctly`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(6).saveAnd {
                    addPlacementPlan().ofType(PlacementType.PRESCHOOL_DAYCARE).toUnit(daycareInArea1)
                        .fromDay(today.minusDays(1)).toDay(today.plusDays(1))
                        .withPreschoolDaycareDates(FiniteDateRange(today, today))
                        .save()
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today.minusDays(1), 0.5)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 1.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today.plusDays(1), 0.5)
        }
    }

    @Test
    fun `realized occupancy uses staff attendance for caretaker count`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(4).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).fromDay(-2).toDay(1).saveAnd {
                        addGroupPlacement().toGroup(daycareGroup1).save()
                    }
                }

            tx.insertTestStaffAttendance(groupId = daycareGroup1, date = today.minusDays(2), count = 1.5)
            // day between has staff attendance missing
            tx.insertTestStaffAttendance(groupId = daycareGroup1, date = today, count = 2.0)
        }

        db.read { tx ->
            val dailyOccupancyValues = tx.calculateDailyGroupOccupancyValues(
                today = today,
                queryPeriod = FiniteDateRange(today.minusDays(2), today.plusDays(1)),
                type = OccupancyType.REALIZED,
                unitId = daycareInArea1
            ).first { it.key.groupId == daycareGroup1 }.occupancies

            dailyOccupancyValues[today.minusDays(2)]!!.let { values ->
                assertEquals(1.5, values.caretakers)
                assertEquals(1.0, values.sum)
                assertEquals(9.5, values.percentage)
            }

            dailyOccupancyValues[today.minusDays(1)]!!.let { values ->
                assertNull(values.caretakers)
                assertEquals(1.0, values.sum)
                assertNull(values.percentage)
            }

            dailyOccupancyValues[today]!!.let { values ->
                assertEquals(2.0, values.caretakers)
                assertEquals(1.0, values.sum)
                assertEquals(7.1, values.percentage)
            }

            // future days are excluded
            assertFalse(dailyOccupancyValues.containsKey(today.plusDays(1)))
        }
    }

    @Test
    fun `realized occupancy cannot be calculated into future`() {
        db.read { tx ->
            val values = tx.calculateDailyUnitOccupancyValues(
                today = today,
                queryPeriod = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                type = OccupancyType.REALIZED,
                unitId = daycareInArea1
            )
            assertTrue(values.isEmpty())
        }
    }

    @Test
    fun `realized occupancy does not count absent children`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(4).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).fromDay(-2).toDay(0).saveAnd {
                        addGroupPlacement().toGroup(daycareGroup1).save()
                    }
                    addAbsence().ofType(AbsenceType.SICKLEAVE).onDay(-1).forCareTypes(AbsenceCareType.DAYCARE).save()
                    addAbsence().ofType(AbsenceType.PLANNED_ABSENCE).onDay(0).forCareTypes(AbsenceCareType.DAYCARE).save()
                }

            FiniteDateRange(today.minusDays(2), today).dates().forEach { date ->
                tx.insertTestStaffAttendance(groupId = daycareGroup1, date = date, count = 3.0)
            }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.REALIZED, today.minusDays(2), 1.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.REALIZED, today.minusDays(1), 0.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.REALIZED, today, 0.0)

            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today.minusDays(2), 1.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today.minusDays(1), 1.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
        }
    }

    @Test
    fun `when child is in backup care the realized occupancy is taken from backup location`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(4).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).save()
                    addBackupCare().toUnit(familyUnitInArea2).save()
                }

            tx.insertTestStaffAttendance(groupId = daycareGroup1, date = today, count = 3.0)
            tx.insertTestStaffAttendance(groupId = daycareGroup2, date = today, count = 3.0)
            tx.insertTestStaffAttendance(groupId = familyGroup1, date = today, count = 3.0)
            tx.insertTestStaffAttendance(groupId = familyGroup2, date = today, count = 3.0)
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.REALIZED, today, 0.0)
            getAndAssertOccupancyInUnit(tx, familyUnitInArea2, OccupancyType.CONFIRMED, today, 0.0)
            getAndAssertOccupancyInUnit(tx, familyUnitInArea2, OccupancyType.REALIZED, today, 1.75)
        }
    }

    @Test
    fun `child can be in backup care within same unit`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(4).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(familyUnitInArea2).saveAnd {
                        addGroupPlacement().toGroup(familyGroup1).save()
                    }
                    addBackupCare().toUnit(familyUnitInArea2).toGroup(familyGroup2).save()
                }

            tx.insertTestStaffAttendance(groupId = familyGroup1, date = today, count = 3.0)
            tx.insertTestStaffAttendance(groupId = familyGroup2, date = today, count = 3.0)
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, familyUnitInArea2, OccupancyType.CONFIRMED, today, 1.75)
            getAndAssertOccupancyInUnit(tx, familyUnitInArea2, OccupancyType.REALIZED, today, 1.75)
            getAndAssertOccupancyInGroup(tx, familyUnitInArea2, familyGroup1, OccupancyType.CONFIRMED, today, 1.75)
            getAndAssertOccupancyInGroup(tx, familyUnitInArea2, familyGroup1, OccupancyType.REALIZED, today, 0.0)
            getAndAssertOccupancyInGroup(tx, familyUnitInArea2, familyGroup2, OccupancyType.CONFIRMED, today, 0.0)
            getAndAssertOccupancyInGroup(tx, familyUnitInArea2, familyGroup2, OccupancyType.REALIZED, today, 1.75)
        }
    }

    @Test
    fun `reduceDailyOccupancyValues merges periods`() {
        db.transaction { tx ->
            FixtureBuilder(tx, today)
                .addChild().withAge(4).saveAnd {
                    addPlacement().ofType(PlacementType.DAYCARE).toUnit(daycareInArea1).fromDay(-3).toDay(5).save()
                    addPlacement().ofType(PlacementType.DAYCARE_PART_TIME).toUnit(daycareInArea1).fromDay(6).toDay(10).save()
                }
        }

        db.read { tx ->
            val map = tx.calculateDailyUnitOccupancyValues(
                today = today,
                queryPeriod = FiniteDateRange(today.minusDays(3), today.plusDays(10)),
                type = OccupancyType.CONFIRMED,
                unitId = daycareInArea1
            ).let { reduceDailyOccupancyValues(it) }

            assertEquals(1, map.entries.size)
            assertEquals(daycareInArea1, map.keys.first().unitId)

            val periods = map.values.first()
            assertEquals(3, periods.size)
            assertEquals(
                periods[0],
                OccupancyPeriod(
                    period = FiniteDateRange(today.minusDays(3), today.plusDays(1)),
                    sum = 1.0,
                    headcount = 1,
                    caretakers = 6.0,
                    percentage = 2.4
                )
            )
            // first split is due to weekend
            assertEquals(
                periods[1],
                OccupancyPeriod(
                    period = FiniteDateRange(today.plusDays(4), today.plusDays(5)),
                    sum = 1.0,
                    headcount = 1,
                    caretakers = 6.0,
                    percentage = 2.4
                )
            )
            // second split is due to placement change
            assertEquals(
                periods[2],
                OccupancyPeriod(
                    period = FiniteDateRange(today.plusDays(6), today.plusDays(8)),
                    sum = 0.54,
                    headcount = 1,
                    caretakers = 6.0,
                    percentage = 1.3
                )
            )
        }
    }

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

    private fun getAndAssertOccupancyInGroup(
        tx: Database.Read,
        unitId: UUID,
        groupId: UUID,
        type: OccupancyType,
        date: LocalDate,
        expectedSum: Double
    ) {
        assertOccupancySum(
            expectedSum = expectedSum,
            date = date,
            groupingId = groupId,
            occupancyValues = tx.calculateDailyGroupOccupancyValues(
                today = today,
                queryPeriod = FiniteDateRange(date, date),
                type = type,
                unitId = unitId
            )
        )
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

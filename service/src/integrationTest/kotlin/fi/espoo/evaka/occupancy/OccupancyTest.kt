// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.occupancy

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.application.ApplicationStatus
import fi.espoo.evaka.application.ApplicationType
import fi.espoo.evaka.attendance.StaffAttendanceType
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.Id
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevAssistanceFactor
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.DevStaffAttendance
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestApplication
import fi.espoo.evaka.shared.dev.insertTestCaretakers
import fi.espoo.evaka.shared.dev.insertTestPlacementPlan
import fi.espoo.evaka.shared.dev.insertTestStaffAttendance
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import fi.espoo.evaka.shared.security.actionrule.AccessControlFilter
import fi.espoo.evaka.snDaycareContractDays10
import fi.espoo.evaka.snDefaultPartDayDaycare
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class OccupancyTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val today = LocalDate.of(2020, 1, 16) // Thursday

    private val careArea1: AreaId = AreaId(UUID.randomUUID())
    private val careArea2: AreaId = AreaId(UUID.randomUUID())

    private val daycareInArea1: DaycareId = DaycareId(UUID.randomUUID())
    private val daycareGroup1: GroupId = GroupId(UUID.randomUUID())
    private val daycareGroup2: GroupId = GroupId(UUID.randomUUID())

    private val familyUnitInArea2: DaycareId = DaycareId(UUID.randomUUID())
    private val familyGroup1: GroupId = GroupId(UUID.randomUUID())
    private val familyGroup2: GroupId = GroupId(UUID.randomUUID())

    @BeforeEach
    fun setUp() {
        db.transaction {
            it.insertServiceNeedOptions()

            it.insert(DevCareArea(id = careArea1, name = "1", shortName = "1"))
            it.insert(DevCareArea(id = careArea2, name = "2", shortName = "2"))

            it.insert(
                DevDaycare(
                    id = daycareInArea1,
                    areaId = careArea1,
                    providerType = ProviderType.MUNICIPAL,
                    type =
                        setOf(CareType.CENTRE, CareType.PRESCHOOL, CareType.PREPARATORY_EDUCATION)
                )
            )
            it.insert(DevDaycareGroup(id = daycareGroup1, daycareId = daycareInArea1))
            it.insert(DevDaycareGroup(id = daycareGroup2, daycareId = daycareInArea1))
            it.insertTestCaretakers(groupId = daycareGroup1, amount = 3.0)
            it.insertTestCaretakers(groupId = daycareGroup2, amount = 3.0)

            it.insert(
                DevDaycare(
                    id = familyUnitInArea2,
                    areaId = careArea2,
                    providerType = ProviderType.PURCHASED,
                    type = setOf(CareType.FAMILY)
                )
            )
            it.insert(DevDaycareGroup(id = familyGroup1, daycareId = familyUnitInArea2))
            it.insert(DevDaycareGroup(id = familyGroup2, daycareId = familyUnitInArea2))
            it.insertTestCaretakers(groupId = familyGroup1, amount = 3.0)
            it.insertTestCaretakers(groupId = familyGroup2, amount = 3.0)
        }
    }

    @Test
    fun `calculateDailyUnitOccupancyValues smoke test`() {
        val child = DevPerson(dateOfBirth = today.minusYears(3))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)
        }

        db.read {
            val period = FiniteDateRange(today.minusDays(1), today)
            val occupancyValues =
                it.calculateDailyUnitOccupancyValues(
                    today,
                    period,
                    OccupancyType.CONFIRMED,
                    AccessControlFilter.PermitAll,
                    areaId = careArea1,
                )

            assertEquals(1, occupancyValues.size)
            assertEquals(daycareInArea1, occupancyValues[0].key.unitId)
            assertEquals(
                mapOf(
                    period.start to
                        OccupancyValues(
                            sumUnder3y = 1.75,
                            sumOver3y = 0.0,
                            headcount = 1,
                            caretakers = 6.0,
                            percentage = 4.2
                        ),
                    period.end to
                        OccupancyValues(
                            sumUnder3y = 0.0,
                            sumOver3y = 1.0,
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
        val child = DevPerson(dateOfBirth = today.minusYears(3))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)
        }

        db.read { tx ->
            val occupancyValues =
                tx.calculateDailyGroupOccupancyValues(
                    today,
                    FiniteDateRange(today, today),
                    OccupancyType.CONFIRMED,
                    AccessControlFilter.PermitAll,
                    unitId = daycareInArea1
                )

            assertEquals(2, occupancyValues.size)
            assertEquals(1, occupancyValues.filter { it.key.groupId == daycareGroup1 }.size)
            val occupancy =
                occupancyValues.find { it.key.groupId == daycareGroup1 }!!.occupancies[today]!!
            assertEquals(3.0, occupancy.caretakers)
            assertEquals(1, occupancy.headcount)
        }
    }

    private fun realtimeAttendanceToday(
        employeeId: EmployeeId,
        minusDaysFromNow: Long = 0,
        type: StaffAttendanceType = StaffAttendanceType.PRESENT
    ) =
        DevStaffAttendance(
            employeeId = employeeId,
            groupId = daycareGroup1,
            type = type,
            arrived = HelsinkiDateTime.of(today.minusDays(minusDaysFromNow), LocalTime.of(8, 0)),
            departed = HelsinkiDateTime.of(today.minusDays(minusDaysFromNow), LocalTime.of(15, 39)),
        )

    private fun assertRealtimeAttendances(
        expectedCaretakers: List<Pair<LocalDate, OccupancyValues>>
    ) {
        val (rangeStart, rangeEnd) =
            expectedCaretakers.map { it.first }.let { it.minOrNull()!! to it.maxOrNull()!! }

        val occupancies =
            db.read { tx ->
                tx.calculateDailyGroupOccupancyValues(
                        today,
                        FiniteDateRange(rangeStart, rangeEnd),
                        OccupancyType.REALIZED,
                        AccessControlFilter.PermitAll,
                        unitId = daycareInArea1
                    )
                    .find { it.key.groupId == daycareGroup1 }!!
                    .occupancies
            }

        expectedCaretakers.forEach { (date, expectedValue) ->
            assertEquals(expectedValue.sum, occupancies[date]?.sum, message = "bad sum")
            assertEquals(
                expectedValue.headcount,
                occupancies[date]?.headcount,
                message = "bad headcount"
            )
            assertEquals(
                expectedValue.percentage,
                occupancies[date]?.percentage,
                message = "bad percentage"
            )
            assertEquals(
                expectedValue.caretakers,
                occupancies[date]?.caretakers,
                message = "bad caretaker count"
            )
        }
    }

    @Test
    fun `calculateDailyGroupOccupancyValues with realtime staff attendance`() {
        val child = DevPerson(dateOfBirth = today.minusYears(3))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val employee = DevEmployee()
        val attendance = realtimeAttendanceToday(employee.id)

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)
            tx.insert(employee)
            tx.insert(attendance)
        }

        assertRealtimeAttendances(listOf(today to OccupancyValues(1.0, 0.0, 1, 1.0, 14.3)))

        db.read { tx ->
            val occupancyValues =
                tx.calculateDailyGroupOccupancyValues(
                    today,
                    FiniteDateRange(today, today),
                    OccupancyType.REALIZED,
                    AccessControlFilter.PermitAll,
                    unitId = daycareInArea1
                )

            assertEquals(2, occupancyValues.size)
            assertEquals(1, occupancyValues.filter { it.key.groupId == daycareGroup1 }.size)
            val occupancy =
                occupancyValues.find { it.key.groupId == daycareGroup1 }!!.occupancies[today]!!
            assertEquals(1, occupancy.headcount)
        }
    }

    @Test
    fun `calculateDailyGroupOccupancyValues with realtime staff attendance for one employee & two days`() {
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val employee = DevEmployee()
        val attendance1 = realtimeAttendanceToday(employee.id, 1)
        val attendance2 = realtimeAttendanceToday(employee.id)

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)
            tx.insert(employee)
            tx.insert(attendance1)
            tx.insert(attendance2)
        }

        assertRealtimeAttendances(
            listOf(
                today.minusDays(1) to OccupancyValues(1.0, 0.0, 1, 1.0, 14.3),
                today to OccupancyValues(1.0, 0.0, 1, 1.0, 14.3)
            )
        )
    }

    @Test
    fun `calculateDailyGroupOccupancyValues with realtime staff attendance for two employees & one day`() {
        val child = DevPerson(dateOfBirth = today.minusYears(3))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val employee1 = DevEmployee()
        val employee2 = DevEmployee()
        val attendances =
            listOf(realtimeAttendanceToday(employee1.id), realtimeAttendanceToday(employee2.id))

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)
            tx.insert(employee1)
            tx.insert(employee2)
            attendances.forEach { tx.insert(it) }
        }

        db.read { tx ->
            val occupancyValues =
                tx.calculateDailyGroupOccupancyValues(
                    today,
                    FiniteDateRange(today, today),
                    OccupancyType.REALIZED,
                    AccessControlFilter.PermitAll,
                    unitId = daycareInArea1
                )

            val occupancies = occupancyValues.find { it.key.groupId == daycareGroup1 }!!.occupancies
            assertEquals(2.0, occupancies[today]!!.caretakers)
        }

        assertRealtimeAttendances(listOf(today to OccupancyValues(1.0, 0.0, 1, 2.0, 7.1)))
    }

    @Test
    fun `calculateDailyGroupOccupancyValues with realtime staff attendance for two days & two employees`() {
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val employee1 = DevEmployee()
        val employee2 = DevEmployee()
        val attendances =
            listOf(
                realtimeAttendanceToday(employee1.id, 1),
                realtimeAttendanceToday(employee1.id),
                realtimeAttendanceToday(employee2.id, 1),
                realtimeAttendanceToday(employee2.id)
            )

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)

            tx.insert(employee1)
            tx.insert(employee2)
            attendances.forEach { tx.insert(it) }
        }

        assertRealtimeAttendances(
            listOf(
                today.minusDays(1) to OccupancyValues(1.0, 0.0, 1, 2.0, 7.1),
                today to OccupancyValues(1.0, 0.0, 1, 2.0, 7.1)
            )
        )
    }

    @Test
    fun `calculateDailyGroupOccupancyValues for realtime attendance for half occupancy coefficient`() {
        val child = DevPerson(dateOfBirth = today.minusYears(3))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val employee = DevEmployee()
        val attendance =
            DevStaffAttendance(
                employeeId = employee.id,
                groupId = daycareGroup1,
                occupancyCoefficient = BigDecimal(defaultOccupancyCoefficient / 2.0),
                arrived = HelsinkiDateTime.of(today, LocalTime.of(16, 21)),
                departed =
                    HelsinkiDateTime.of(
                        today.plusDays(1),
                        LocalTime.of(0, 0)
                    ), // At work for 7 hours 39 minutes
            )

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)
            tx.insert(employee)
            tx.insert(attendance)
        }

        assertRealtimeAttendances(listOf(today to OccupancyValues(1.0, 0.0, 1, 0.5, 28.6)))
    }

    @Test
    fun `calculateDailyGroupOccupancyValues for realtime attendance without departing time and staff attendance`() {
        val child = DevPerson(dateOfBirth = today.minusYears(3))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val employee = DevEmployee()
        val attendance =
            DevStaffAttendance(
                employeeId = employee.id,
                groupId = daycareGroup1,
                occupancyCoefficient = BigDecimal(3.5),
                arrived = HelsinkiDateTime.of(today, LocalTime.of(16, 15)),
                departed = null,
            )

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)
            tx.insert(employee)
            tx.insert(attendance)
        }

        assertRealtimeAttendances(listOf(today to OccupancyValues(1.0, 0.0, 1, null, null)))
    }

    @Test
    fun `calculateDailyGroupOccupancyValues with both realtime staff attendance and another staff attendance count`() {
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val employee1 = DevEmployee()
        val employee2 = DevEmployee()
        val attendances =
            listOf(
                realtimeAttendanceToday(employee1.id),

                // not included because of no departure time
                DevStaffAttendance(
                    employeeId = employee2.id,
                    groupId = daycareGroup1,
                    arrived = HelsinkiDateTime.of(today, LocalTime.of(8, 0)),
                    departed = null
                ),
            )

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)

            tx.insert(employee1)
            tx.insert(employee2)
            attendances.forEach { tx.insert(it) }

            // this is ignored
            tx.insertTestStaffAttendance(groupId = daycareGroup1, date = today, count = 5.0)
        }

        assertRealtimeAttendances(listOf(today to OccupancyValues(1.0, 0.0, 1, 1.0, 14.3)))
    }

    @Test
    fun `calculateDailyGroupOccupancyValues with realtime staff attendance one day and non-realtime the other`() {
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(2),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(2),
                endDate = today
            )

        val employee = DevEmployee()
        val attendance = realtimeAttendanceToday(employee.id, 1)

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)

            tx.insert(employee)
            tx.insert(attendance)

            tx.insertTestStaffAttendance(groupId = daycareGroup1, date = today, count = 2.0)
        }

        assertRealtimeAttendances(
            listOf(
                today.minusDays(1) to OccupancyValues(1.0, 0.0, 1, 1.0, 14.3),
                today to OccupancyValues(1.0, 0.0, 1, 2.0, 7.1)
            )
        )
    }

    @ParameterizedTest(name = "Realized occupancy with realtime staff attendance type {0}")
    @EnumSource(names = ["PRESENT", "OVERTIME", "JUSTIFIED_CHANGE", "TRAINING", "OTHER_WORK"])
    fun `calculateDailyGroupOccupancyValues with different realtime staff attendance types`(
        type: StaffAttendanceType
    ) {
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today,
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today,
                endDate = today
            )

        val employee = DevEmployee()
        val attendance = realtimeAttendanceToday(employee.id, 0, type)

        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)
            tx.insert(employee)
            tx.insert(attendance)
        }

        val (expectedCaretakers, expectedPercentage) =
            when (type) {
                StaffAttendanceType.PRESENT,
                StaffAttendanceType.OVERTIME,
                StaffAttendanceType.JUSTIFIED_CHANGE -> 1.0 to 14.3
                StaffAttendanceType.TRAINING,
                StaffAttendanceType.OTHER_WORK -> null to null
            }

        assertRealtimeAttendances(
            listOf(today to OccupancyValues(1.0, 0.0, 1, expectedCaretakers, expectedPercentage))
        )
    }

    @Test
    fun `realizedOccupancyCoefficientUnder3y is used for realized occupancy of a child under 3 years`() {
        val employee = DevEmployee()
        val child = DevPerson(dateOfBirth = today.minusYears(2))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                type = PlacementType.DAYCARE_PART_TIME,
                startDate = today.minusDays(1),
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(
                DevServiceNeed(
                    placementId = placement.id,
                    optionId = snDaycareContractDays10.id,
                    startDate = today.minusDays(1),
                    endDate = today,
                    confirmedBy = employee.evakaUserId
                )
            )
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(
                tx,
                daycareInArea1,
                OccupancyType.REALIZED,
                today.minusDays(1L),
                1.25,
            )
        }
    }

    @Test
    fun `realizedOccupancyCoefficient used for realized occupancy of a child over 3 years`() {
        val employee = DevEmployee()
        val child = DevPerson(dateOfBirth = today.minusYears(3).minusDays(1))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(
                DevServiceNeed(
                    placementId = placement.id,
                    optionId = snDaycareContractDays10.id,
                    startDate = today.minusDays(1),
                    endDate = today,
                    confirmedBy = employee.evakaUserId
                )
            )
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(
                tx,
                daycareInArea1,
                OccupancyType.REALIZED,
                today.minusDays(1L),
                0.5,
            )
        }
    }

    @Test
    fun `confirmed occupancy for a child under 3 year old is 1,75`() {
        val child = DevPerson(dateOfBirth = today.minusYears(3))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                type = PlacementType.DAYCARE_PART_TIME,
                startDate = today.minusDays(1),
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(
                tx,
                daycareInArea1,
                OccupancyType.CONFIRMED,
                today.minusDays(1L),
                1.75
            )
        }
    }

    @Test
    fun `occupancy is 1,75 when unit is family unit`() {
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = familyUnitInArea2,
                startDate = today.minusDays(1),
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, familyUnitInArea2, OccupancyType.CONFIRMED, today, 1.75)
        }
    }

    @Test
    fun `daycare occupancy with default service need for a child over 3 year old is 1,0`() {
        val child = DevPerson(dateOfBirth = today.minusYears(3))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today,
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
        }
    }

    @Test
    fun `part time daycare occupancy with default service need for a child over 3 year old is 0,54`() {
        val child = DevPerson(dateOfBirth = today.minusYears(3))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                type = PlacementType.DAYCARE_PART_TIME,
                startDate = today,
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.54)
        }
    }

    @Test
    fun `preschool occupancy with default service need is 0,5`() {
        val child = DevPerson(dateOfBirth = today.minusYears(6).minusMonths(5))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                type = PlacementType.PRESCHOOL,
                startDate = today,
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.5)
        }
    }

    @Test
    fun `preschool daycare occupancy with default service need is 1,0`() {
        val child = DevPerson(dateOfBirth = today.minusYears(6).minusMonths(5))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = today,
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
        }
    }

    @Test
    fun `occupancy is based on service need`() {
        val employee = DevEmployee()
        val child = DevPerson(dateOfBirth = today.minusYears(6).minusMonths(5))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = today,
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            // a valid PRESCHOOL_DAYCARE service need would have occupancy 1.0
            tx.insert(
                DevServiceNeed(
                    placementId = placement.id,
                    optionId = snDefaultPartDayDaycare.id,
                    startDate = today,
                    endDate = today,
                    confirmedBy = employee.evakaUserId
                )
            )
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.54)
        }
    }

    @Test
    fun `occupancy is multiplied by assistance factor`() {
        val child = DevPerson(dateOfBirth = today.minusYears(3))
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareInArea1,
                    startDate = today,
                    endDate = today.plusDays(1)
                )
            )
            tx.insert(
                DevAssistanceFactor(
                    childId = child.id,
                    capacityFactor = 2.0,
                    validDuring = today.plusDays(1).toFiniteDateRange()
                )
            )
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(
                tx,
                daycareInArea1,
                OccupancyType.CONFIRMED,
                today,
                1.0,
            )
            getAndAssertOccupancyInUnit(
                tx,
                daycareInArea1,
                OccupancyType.CONFIRMED,
                today.plusDays(1),
                2.0,
            )
        }
    }

    @Test
    fun `placement plan affects only planned occupancy`() {
        db.transaction { tx ->
            val guardian = DevPerson()
            val child = DevPerson(dateOfBirth = today.minusYears(3))
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertTestApplication(
                    guardianId = guardian.id,
                    childId = child.id,
                    status = ApplicationStatus.WAITING_DECISION,
                    type = ApplicationType.DAYCARE,
                )
                .also { applicationId ->
                    tx.insertTestPlacementPlan(
                        applicationId = applicationId,
                        unitId = daycareInArea1,
                        startDate = today,
                        endDate = today,
                    )
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 1.0)
        }
    }

    private fun placementPlanTest(
        preschool: IntRange,
        preschoolDaycare: IntRange,
        expectedSums: Map<Int, Double>
    ) {
        db.transaction { tx ->
            val guardian = DevPerson()
            val child = DevPerson(dateOfBirth = today.minusYears(6))
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertTestApplication(
                    guardianId = guardian.id,
                    childId = child.id,
                    status = ApplicationStatus.WAITING_DECISION,
                    type = ApplicationType.PRESCHOOL,
                )
                .also { applicationId ->
                    tx.insertTestPlacementPlan(
                        applicationId = applicationId,
                        unitId = daycareInArea1,
                        type = PlacementType.PRESCHOOL_DAYCARE,
                        startDate = today.plusDays(preschool.first.toLong()),
                        endDate = today.plusDays(preschool.last.toLong()),
                        preschoolDaycareStartDate = today.plusDays(preschoolDaycare.first.toLong()),
                        preschoolDaycareEndDate = today.plusDays(preschoolDaycare.last.toLong()),
                    )
                }
        }
        db.read { tx ->
            expectedSums.forEach { (day, expectedSum) ->
                getAndAssertOccupancyInUnit(
                    tx,
                    daycareInArea1,
                    OccupancyType.PLANNED,
                    today.plusDays(day.toLong()),
                    expectedSum
                )
                getAndAssertOccupancyInUnit(
                    tx,
                    daycareInArea1,
                    OccupancyType.CONFIRMED,
                    today.plusDays(day.toLong()),
                    0.0
                )
            }
        }
    }

    @Test
    fun `placement plan - pure preschool before preschool+daycare`() =
        placementPlanTest(
            // 45678
            // PP    <- pure preschool
            //    DD <- preschool+daycare
            preschool = 4..5,
            preschoolDaycare = 7..8,
            expectedSums = mapOf(5 to 0.5, 6 to 0.0, 7 to 1.0)
        )

    @Test
    fun `placement plan - pure preschool, overlap, preschool+daycare`() =
        placementPlanTest(
            // 45678
            // PPP   <- pure preschool
            //   DDD <- preschool+daycare
            preschool = 4..6,
            preschoolDaycare = 6..8,
            expectedSums = mapOf(5 to 0.5, 6 to 1.0, 7 to 1.0)
        )

    @Test
    fun `placement plan - pure preschool, overlap, pure preschool`() =
        placementPlanTest(
            // 45678
            // PPPPP <- pure preschool
            //  DDD  <- preschool+daycare
            preschool = 4..8,
            preschoolDaycare = 5..7,
            expectedSums = mapOf(4 to 0.5, 6 to 1.0, 8 to 0.5)
        )

    @Test
    fun `placement plan - preschool+daycare before pure preschool`() =
        placementPlanTest(
            // 45678
            //    PP <- pure preschool
            // DD    <- preschool+daycare
            preschool = 7..8,
            preschoolDaycare = 4..5,
            expectedSums = mapOf(5 to 1.0, 6 to 0.0, 7 to 0.5)
        )

    @Test
    fun `placement plan - preschool+daycare, overlap, pure preschool`() =
        placementPlanTest(
            // 45678
            //   PPP <- pure preschool
            // DDD   <- preschool+daycare
            preschool = 6..8,
            preschoolDaycare = 4..6,
            expectedSums = mapOf(5 to 1.0, 6 to 1.0, 7 to 0.5)
        )

    @Test
    fun `placement plan - preschool+daycare, overlap, preschool+daycare`() =
        placementPlanTest(
            // 45678
            //   P   <- pure preschool
            // DDDDD <- preschool+daycare
            preschool = 5..6,
            preschoolDaycare = 4..8,
            expectedSums = mapOf(5 to 1.0, 6 to 1.0, 7 to 1.0)
        )

    @Test
    fun `deleted placement plan has no effect`() {
        db.transaction { tx ->
            val guardian = DevPerson()
            val child = DevPerson(dateOfBirth = today.minusYears(3))
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertTestApplication(
                    childId = child.id,
                    guardianId = guardian.id,
                    type = ApplicationType.DAYCARE,
                    status = ApplicationStatus.WAITING_DECISION
                )
                .also { applicationId ->
                    tx.insertTestPlacementPlan(
                        applicationId = applicationId,
                        unitId = daycareInArea1,
                        startDate = today,
                        endDate = today,
                        deleted = true
                    )
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
            val guardian = DevPerson()
            val child = DevPerson(dateOfBirth = today.minusYears(3))
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertTestApplication(
                    childId = child.id,
                    guardianId = guardian.id,
                    type = ApplicationType.DAYCARE,
                    status = ApplicationStatus.WAITING_DECISION
                )
                .also { applicationId ->
                    tx.insertTestPlacementPlan(
                        applicationId = applicationId,
                        unitId = daycareInArea1,
                        startDate = today,
                        endDate = today,
                    )
                }
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = familyUnitInArea2,
                    startDate = today,
                    endDate = today
                )
            )
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 1.0)
            getAndAssertOccupancyInUnit(tx, familyUnitInArea2, OccupancyType.PLANNED, today, 1.75)
        }
    }

    @Test
    fun `if placement plan is to same unit as overlapping placement then the maximum is counted to planned occupancy - test 1`() {
        db.transaction { tx ->
            val guardian = DevPerson()
            val child = DevPerson(dateOfBirth = today.minusYears(3))
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertTestApplication(
                    childId = child.id,
                    guardianId = guardian.id,
                    type = ApplicationType.DAYCARE,
                    status = ApplicationStatus.WAITING_DECISION
                )
                .also { applicationId ->
                    tx.insertTestPlacementPlan(
                        applicationId = applicationId,
                        unitId = daycareInArea1,
                        startDate = today,
                        endDate = today,
                    )
                }
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareInArea1,
                    type = PlacementType.DAYCARE_PART_TIME,
                    startDate = today,
                    endDate = today
                )
            )
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 0.54)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 1.0)
        }
    }

    @Test
    fun `if placement plan is to same unit as overlapping placement then the maximum is counted to planned occupancy - test 2`() {
        db.transaction { tx ->
            val guardian = DevPerson()
            val child = DevPerson(dateOfBirth = today.minusYears(3))
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertTestApplication(
                    childId = child.id,
                    guardianId = guardian.id,
                    type = ApplicationType.DAYCARE,
                    status = ApplicationStatus.WAITING_DECISION
                )
                .also { applicationId ->
                    tx.insertTestPlacementPlan(
                        applicationId = applicationId,
                        unitId = daycareInArea1,
                        type = PlacementType.DAYCARE_PART_TIME,
                        startDate = today,
                        endDate = today,
                    )
                }
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = daycareInArea1,
                    startDate = today,
                    endDate = today
                )
            )
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 1.0)
        }
    }

    @Test
    fun `preschool placement plan with separate daycare period is handled correctly`() {
        db.transaction { tx ->
            val guardian = DevPerson()
            val child = DevPerson(dateOfBirth = today.minusYears(6))
            tx.insert(guardian, DevPersonType.ADULT)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertTestApplication(
                    childId = child.id,
                    guardianId = guardian.id,
                    type = ApplicationType.PRESCHOOL,
                    status = ApplicationStatus.WAITING_DECISION
                )
                .also { applicationId ->
                    tx.insertTestPlacementPlan(
                        applicationId = applicationId,
                        unitId = daycareInArea1,
                        type = PlacementType.PRESCHOOL_DAYCARE,
                        startDate = today.minusDays(1),
                        endDate = today.plusDays(1),
                        preschoolDaycareStartDate = today,
                        preschoolDaycareEndDate = today
                    )
                }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(
                tx,
                daycareInArea1,
                OccupancyType.PLANNED,
                today.minusDays(1),
                0.5
            )
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.PLANNED, today, 1.0)
            getAndAssertOccupancyInUnit(
                tx,
                daycareInArea1,
                OccupancyType.PLANNED,
                today.plusDays(1),
                0.5
            )
        }
    }

    @Test
    fun `realized occupancy uses staff attendance for caretaker count`() {
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(2),
                endDate = today.plusDays(1)
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(2),
                endDate = today.plusDays(1)
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)

            tx.insertTestStaffAttendance(
                groupId = daycareGroup1,
                date = today.minusDays(2),
                count = 1.5
            )
            // day between has staff attendance missing
            tx.insertTestStaffAttendance(groupId = daycareGroup1, date = today, count = 2.0)
        }

        db.read { tx ->
            val dailyOccupancyValues =
                tx.calculateDailyGroupOccupancyValues(
                        today = today,
                        queryPeriod = FiniteDateRange(today.minusDays(2), today.plusDays(1)),
                        type = OccupancyType.REALIZED,
                        unitFilter = AccessControlFilter.PermitAll,
                        unitId = daycareInArea1
                    )
                    .first { it.key.groupId == daycareGroup1 }
                    .occupancies

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
            val values =
                tx.calculateDailyUnitOccupancyValues(
                    today = today,
                    queryPeriod = FiniteDateRange(today.plusDays(1), today.plusDays(2)),
                    type = OccupancyType.REALIZED,
                    unitFilter = AccessControlFilter.PermitAll,
                    unitId = daycareInArea1,
                )
            assertTrue(values.isEmpty())
        }
    }

    @Test
    fun `realized occupancy does not count absent children`() {
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(2),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(2),
                endDate = today
            )
        val absences =
            listOf(
                DevAbsence(
                    childId = child.id,
                    date = today.minusDays(1),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE
                ),
                DevAbsence(
                    childId = child.id,
                    date = today,
                    absenceType = AbsenceType.PLANNED_ABSENCE,
                    absenceCategory = AbsenceCategory.BILLABLE
                )
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)
            absences.forEach { tx.insert(it) }

            FiniteDateRange(today.minusDays(2), today).dates().forEach { date ->
                tx.insertTestStaffAttendance(groupId = daycareGroup1, date = date, count = 3.0)
            }
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(
                tx,
                daycareInArea1,
                OccupancyType.REALIZED,
                today.minusDays(2),
                1.0
            )
            getAndAssertOccupancyInUnit(
                tx,
                daycareInArea1,
                OccupancyType.REALIZED,
                today.minusDays(1),
                0.0
            )
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.REALIZED, today, 0.0)

            getAndAssertOccupancyInUnit(
                tx,
                daycareInArea1,
                OccupancyType.CONFIRMED,
                today.minusDays(2),
                1.0
            )
            getAndAssertOccupancyInUnit(
                tx,
                daycareInArea1,
                OccupancyType.CONFIRMED,
                today.minusDays(1),
                1.0
            )
            getAndAssertOccupancyInUnit(tx, daycareInArea1, OccupancyType.CONFIRMED, today, 1.0)
        }
    }

    @Test
    fun `when child is in backup care the realized occupancy is taken from backup location`() {
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today,
                endDate = today
            )
        val backupCare =
            DevBackupCare(
                childId = child.id,
                unitId = familyUnitInArea2,
                groupId = familyGroup2,
                period = FiniteDateRange(today, today)
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(backupCare)

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
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = familyUnitInArea2,
                startDate = today,
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = familyGroup1,
                startDate = today,
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)

            tx.insert(
                DevBackupCare(
                    childId = child.id,
                    unitId = familyUnitInArea2,
                    groupId = familyGroup2,
                    period = FiniteDateRange(today, today)
                )
            )

            tx.insertTestStaffAttendance(groupId = familyGroup1, date = today, count = 3.0)
            tx.insertTestStaffAttendance(groupId = familyGroup2, date = today, count = 3.0)
        }

        db.read { tx ->
            getAndAssertOccupancyInUnit(tx, familyUnitInArea2, OccupancyType.CONFIRMED, today, 1.75)
            getAndAssertOccupancyInUnit(tx, familyUnitInArea2, OccupancyType.REALIZED, today, 1.75)
            getAndAssertOccupancyInGroup(
                tx,
                familyUnitInArea2,
                familyGroup1,
                OccupancyType.CONFIRMED,
                today,
                1.75
            )
            getAndAssertOccupancyInGroup(
                tx,
                familyUnitInArea2,
                familyGroup1,
                OccupancyType.REALIZED,
                today,
                0.0
            )
            getAndAssertOccupancyInGroup(
                tx,
                familyUnitInArea2,
                familyGroup2,
                OccupancyType.CONFIRMED,
                today,
                0.0
            )
            getAndAssertOccupancyInGroup(
                tx,
                familyUnitInArea2,
                familyGroup2,
                OccupancyType.REALIZED,
                today,
                1.75
            )
        }
    }

    @Test
    fun `reduceDailyOccupancyValues merges periods`() {
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement1 =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                type = PlacementType.DAYCARE,
                startDate = today.minusDays(3),
                endDate = today.plusDays(5)
            )
        val placement2 =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                type = PlacementType.DAYCARE_PART_TIME,
                startDate = today.plusDays(6),
                endDate = today.plusDays(10)
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement1)
            tx.insert(placement2)
        }

        db.read { tx ->
            val map =
                tx.calculateDailyUnitOccupancyValues(
                        today = today,
                        queryPeriod = FiniteDateRange(today.minusDays(3), today.plusDays(10)),
                        type = OccupancyType.CONFIRMED,
                        unitFilter = AccessControlFilter.PermitAll,
                        unitId = daycareInArea1
                    )
                    .let { reduceDailyOccupancyValues(it) }

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

    @Test
    fun `calculateDailyUnitOccupancyValues should filter with provider type`() {
        val child1 = DevPerson(dateOfBirth = today.minusYears(3))
        val placement1 =
            DevPlacement(
                childId = child1.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement1 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement1.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val child2 = DevPerson(dateOfBirth = today.minusYears(3))
        val placement2 =
            DevPlacement(
                childId = child2.id,
                unitId = familyUnitInArea2,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement2 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement2.id,
                daycareGroupId = familyGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        db.transaction { tx ->
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(placement1)
            tx.insert(groupPlacement1)

            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(placement2)
            tx.insert(groupPlacement2)
        }

        db.read {
            val calculateDailyUnitOccupancyValuesByProviderType:
                (ProviderType?) -> List<DailyOccupancyValues<UnitKey>> =
                { providerType ->
                    it.calculateDailyUnitOccupancyValues(
                        today,
                        FiniteDateRange(today.minusDays(1), today),
                        OccupancyType.CONFIRMED,
                        AccessControlFilter.PermitAll,
                        providerType = providerType
                    )
                }

            assertThat(calculateDailyUnitOccupancyValuesByProviderType(null))
                .extracting<DaycareId> { value -> value.key.unitId }
                .containsExactlyInAnyOrder(daycareInArea1, familyUnitInArea2)
            assertThat(calculateDailyUnitOccupancyValuesByProviderType(ProviderType.MUNICIPAL))
                .extracting<DaycareId> { value -> value.key.unitId }
                .containsExactlyInAnyOrder(daycareInArea1)
            assertThat(calculateDailyUnitOccupancyValuesByProviderType(ProviderType.PURCHASED))
                .extracting<DaycareId> { value -> value.key.unitId }
                .containsExactlyInAnyOrder(familyUnitInArea2)
            assertThat(
                    calculateDailyUnitOccupancyValuesByProviderType(
                        ProviderType.PRIVATE_SERVICE_VOUCHER
                    )
                )
                .isEmpty()
        }
    }

    @Test
    fun `calculateDailyUnitOccupancyValues should filter with unit type`() {
        val child1 = DevPerson(dateOfBirth = today.minusYears(3))
        val placement1 =
            DevPlacement(
                childId = child1.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement1 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement1.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val child2 = DevPerson(dateOfBirth = today.minusYears(3))
        val placement2 =
            DevPlacement(
                childId = child2.id,
                unitId = familyUnitInArea2,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement2 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement2.id,
                daycareGroupId = familyGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        db.transaction { tx ->
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(placement1)
            tx.insert(groupPlacement1)

            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(placement2)
            tx.insert(groupPlacement2)
        }

        db.read {
            val calculateDailyUnitOccupancyValuesByUnitTypes:
                (Set<CareType>) -> List<DailyOccupancyValues<UnitKey>> =
                { unitTypes ->
                    it.calculateDailyUnitOccupancyValues(
                        today,
                        FiniteDateRange(today.minusDays(1), today),
                        OccupancyType.CONFIRMED,
                        AccessControlFilter.PermitAll,
                        unitTypes = unitTypes
                    )
                }

            assertThat(calculateDailyUnitOccupancyValuesByUnitTypes(emptySet()))
                .extracting<DaycareId> { value -> value.key.unitId }
                .containsExactlyInAnyOrder(daycareInArea1, familyUnitInArea2)
            assertThat(calculateDailyUnitOccupancyValuesByUnitTypes(setOf(CareType.CENTRE)))
                .extracting<DaycareId> { value -> value.key.unitId }
                .containsExactlyInAnyOrder(daycareInArea1)
            assertThat(calculateDailyUnitOccupancyValuesByUnitTypes(setOf(CareType.FAMILY)))
                .extracting<DaycareId> { value -> value.key.unitId }
                .containsExactlyInAnyOrder(familyUnitInArea2)
            assertThat(
                    calculateDailyUnitOccupancyValuesByUnitTypes(
                        setOf(CareType.CENTRE, CareType.FAMILY)
                    )
                )
                .extracting<DaycareId> { value -> value.key.unitId }
                .containsExactlyInAnyOrder(daycareInArea1, familyUnitInArea2)
            assertThat(calculateDailyUnitOccupancyValuesByUnitTypes(setOf(CareType.CLUB))).isEmpty()
        }
    }

    @Test
    fun `calculateDailyGroupOccupancyValues should filter with provider type`() {
        val child1 = DevPerson(dateOfBirth = today.minusYears(3))
        val placement1 =
            DevPlacement(
                childId = child1.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement1 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement1.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val child2 = DevPerson(dateOfBirth = today.minusYears(3))
        val placement2 =
            DevPlacement(
                childId = child2.id,
                unitId = familyUnitInArea2,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement2 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement2.id,
                daycareGroupId = familyGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(placement1)
            tx.insert(groupPlacement1)

            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(placement2)
            tx.insert(groupPlacement2)
        }

        db.read {
            val calculateDailyGroupOccupancyValuesByProviderType:
                (ProviderType?) -> List<DailyOccupancyValues<UnitGroupKey>> =
                { providerType ->
                    it.calculateDailyGroupOccupancyValues(
                        today,
                        FiniteDateRange(today.minusDays(1), today),
                        OccupancyType.CONFIRMED,
                        AccessControlFilter.PermitAll,
                        providerType = providerType
                    )
                }

            assertThat(calculateDailyGroupOccupancyValuesByProviderType(null))
                .extracting<GroupId> { value -> value.key.groupId }
                .containsExactlyInAnyOrder(daycareGroup1, daycareGroup2, familyGroup1, familyGroup2)
            assertThat(calculateDailyGroupOccupancyValuesByProviderType(ProviderType.MUNICIPAL))
                .extracting<GroupId> { value -> value.key.groupId }
                .containsExactlyInAnyOrder(daycareGroup1, daycareGroup2)
            assertThat(calculateDailyGroupOccupancyValuesByProviderType(ProviderType.PURCHASED))
                .extracting<GroupId> { value -> value.key.groupId }
                .containsExactlyInAnyOrder(familyGroup1, familyGroup2)
            assertThat(
                    calculateDailyGroupOccupancyValuesByProviderType(
                        ProviderType.PRIVATE_SERVICE_VOUCHER
                    )
                )
                .isEmpty()
        }
    }

    @Test
    fun `calculateDailyGroupOccupancyValues should filter with unit type`() {
        val child1 = DevPerson(dateOfBirth = today.minusYears(3))
        val placement1 =
            DevPlacement(
                childId = child1.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement1 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement1.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val child2 = DevPerson(dateOfBirth = today.minusYears(3))
        val placement2 =
            DevPlacement(
                childId = child2.id,
                unitId = familyUnitInArea2,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement2 =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement2.id,
                daycareGroupId = familyGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )
        db.transaction { tx ->
            tx.insert(child1, DevPersonType.CHILD)
            tx.insert(placement1)
            tx.insert(groupPlacement1)

            tx.insert(child2, DevPersonType.CHILD)
            tx.insert(placement2)
            tx.insert(groupPlacement2)
        }

        db.read {
            val calculateDailyGroupOccupancyValuesByUnitTypes:
                (Set<CareType>) -> List<DailyOccupancyValues<UnitGroupKey>> =
                { unitTypes ->
                    it.calculateDailyGroupOccupancyValues(
                        today,
                        FiniteDateRange(today.minusDays(1), today),
                        OccupancyType.CONFIRMED,
                        AccessControlFilter.PermitAll,
                        unitTypes = unitTypes
                    )
                }

            assertThat(calculateDailyGroupOccupancyValuesByUnitTypes(emptySet()))
                .extracting<GroupId> { value -> value.key.groupId }
                .containsExactlyInAnyOrder(daycareGroup1, daycareGroup2, familyGroup1, familyGroup2)
            assertThat(calculateDailyGroupOccupancyValuesByUnitTypes(setOf(CareType.CENTRE)))
                .extracting<GroupId> { value -> value.key.groupId }
                .containsExactlyInAnyOrder(daycareGroup1, daycareGroup2)
            assertThat(calculateDailyGroupOccupancyValuesByUnitTypes(setOf(CareType.FAMILY)))
                .extracting<GroupId> { value -> value.key.groupId }
                .containsExactlyInAnyOrder(familyGroup1, familyGroup2)
            assertThat(
                    calculateDailyGroupOccupancyValuesByUnitTypes(
                        setOf(CareType.CENTRE, CareType.FAMILY)
                    )
                )
                .extracting<GroupId> { value -> value.key.groupId }
                .containsExactlyInAnyOrder(daycareGroup1, daycareGroup2, familyGroup1, familyGroup2)
            assertThat(calculateDailyGroupOccupancyValuesByUnitTypes(setOf(CareType.CLUB)))
                .isEmpty()
        }
    }

    @Test
    fun `calculateDailyGroupOccupancyValues should split overnight staff attendances to the respective days`() {
        val child = DevPerson(dateOfBirth = today.minusYears(4))
        val placement =
            DevPlacement(
                childId = child.id,
                unitId = daycareInArea1,
                startDate = today.minusDays(1),
                endDate = today
            )
        val groupPlacement =
            DevDaycareGroupPlacement(
                daycarePlacementId = placement.id,
                daycareGroupId = daycareGroup1,
                startDate = today.minusDays(1),
                endDate = today
            )

        val employee = DevEmployee()
        val attendance =
            DevStaffAttendance(
                employeeId = employee.id,
                groupId = daycareGroup1,
                arrived = HelsinkiDateTime.of(today.minusDays(1), LocalTime.of(21, 0)),
                departed = HelsinkiDateTime.of(today, LocalTime.of(4, 45)),
            )
        db.transaction { tx ->
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(placement)
            tx.insert(groupPlacement)
            tx.insert(employee)
            tx.insert(attendance)
        }

        assertRealtimeAttendances(
            listOf(
                today.minusDays(1) to OccupancyValues(1.0, 0.0, 1, 0.3922, 36.4),
                today to OccupancyValues(1.0, 0.0, 1, 0.6209, 23.0)
            )
        )
    }

    private fun getAndAssertOccupancyInUnit(
        tx: Database.Read,
        unitId: DaycareId,
        type: OccupancyType,
        date: LocalDate,
        expectedSum: Double,
    ) {
        assertOccupancySum(
            expectedSum = expectedSum,
            date = date,
            groupingId = unitId,
            occupancyValues =
                tx.calculateDailyUnitOccupancyValues(
                    today = today,
                    queryPeriod = FiniteDateRange(date, date),
                    type = type,
                    unitFilter = AccessControlFilter.PermitAll,
                    unitId = unitId
                )
        )
    }

    private fun getAndAssertOccupancyInGroup(
        tx: Database.Read,
        unitId: DaycareId,
        groupId: GroupId,
        type: OccupancyType,
        date: LocalDate,
        expectedSum: Double
    ) {
        assertOccupancySum(
            expectedSum = expectedSum,
            date = date,
            groupingId = groupId,
            occupancyValues =
                tx.calculateDailyGroupOccupancyValues(
                    today = today,
                    queryPeriod = FiniteDateRange(date, date),
                    type = type,
                    unitFilter = AccessControlFilter.PermitAll,
                    unitId = unitId
                )
        )
    }

    private fun <K : OccupancyGroupingKey> assertOccupancySum(
        expectedSum: Double,
        date: LocalDate,
        groupingId: Id<*>,
        occupancyValues: List<DailyOccupancyValues<K>>
    ) {
        assertEquals(
            expectedSum,
            occupancyValues.find { it.key.groupingId == groupingId }?.occupancies?.get(date)?.sum
                ?: 0.0
        )
    }
}

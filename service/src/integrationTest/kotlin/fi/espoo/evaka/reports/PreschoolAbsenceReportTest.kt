// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.insertPreschoolTerm
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AbsenceId
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EmployeeId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.GroupPlacementId
import fi.espoo.evaka.shared.PreschoolTermId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevDaycareGroupPlacement
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.Forbidden
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.shared.domain.TimeRange
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

internal class PreschoolAbsenceReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var preschoolAbsenceReport: PreschoolAbsenceReport

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adminLoginUser = AuthenticatedUser.Employee(admin.id, admin.roles)

    private val unitSupervisorA = DevEmployee(id = EmployeeId(UUID.randomUUID()), roles = setOf())
    private val unitSupervisorB = DevEmployee(id = EmployeeId(UUID.randomUUID()), roles = setOf())

    // Wednesday
    private val mockClock =
        MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 7), LocalTime.of(12, 15)))

    @Test
    fun `Unit supervisor can see own unit's report results`() {

        val testData = initTestData(mockClock.today())
        val results =
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                testData.daycareAId,
                null,
                testData.termId
            )

        assertThat(results.isNotEmpty())
    }

    @Test
    fun `Admin can see report results`() {
        val testData = initTestData(mockClock.today())
        val results =
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                testData.daycareAId,
                null,
                testData.termId
            )
        assertThat(results.isNotEmpty())
    }

    @Test
    fun `Unit supervisor cannot see another unit's report results`() {
        val testData = initTestData(mockClock.today())
        assertThrows<Forbidden> {
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                unitSupervisorA.user,
                testData.daycareBId,
                null,
                testData.termId
            )
        }
    }

    @Test
    fun `Report returns correct absence hours for all groups`() {
        val testData = initTestData(mockClock.today())

        val reportResults =
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                testData.daycareAId,
                null,
                testData.termId
            )

        val (groupAExpectation, groupBExpectation) = getExpectedResults(testData)
        assertThat(reportResults).hasSameElementsAs(groupAExpectation + groupBExpectation)
    }

    @Test
    fun `Report returns correct absence hours for just group B`() {
        val testData = initTestData(mockClock.today())

        val reportResults =
            preschoolAbsenceReport.getPreschoolAbsenceReport(
                dbInstance(),
                mockClock,
                adminLoginUser,
                testData.daycareAId,
                testData.groupBId,
                testData.termId
            )

        val (_, groupBExpectation) = getExpectedResults(testData)

        assertThat(reportResults).hasSameElementsAs(groupBExpectation)
    }

    private fun getExpectedResults(
        testData: PreschoolAbsenceReportTestData
    ): Pair<List<ChildPreschoolAbsenceRow>, List<ChildPreschoolAbsenceRow>> {
        val preschoolAbsenceTypes =
            AbsenceType.entries.filter {
                when (it) {
                    AbsenceType.OTHER_ABSENCE,
                    AbsenceType.SICKLEAVE,
                    AbsenceType.UNKNOWN_ABSENCE -> true
                    else -> false
                }
            }

        val groupAExpectation =
            listOf(
                ChildPreschoolAbsenceRow(
                    childId = testData.childA.id,
                    firstName = testData.childA.firstName,
                    lastName = testData.childA.lastName,
                    hourlyTypeResults =
                        preschoolAbsenceTypes.associateWith { type ->
                            when (type) {
                                // 5h full day absence + arrived 1h late + left 1h early + arrived
                                // 45min late
                                AbsenceType.OTHER_ABSENCE -> 7
                                else -> 0
                            }
                        }
                ),
                ChildPreschoolAbsenceRow(
                    childId = testData.childB.id,
                    firstName = testData.childB.firstName,
                    lastName = testData.childB.lastName,
                    hourlyTypeResults =
                        preschoolAbsenceTypes.associateWith { type ->
                            when (type) {
                                AbsenceType.SICKLEAVE -> 5
                                else -> 0
                            }
                        }
                )
            )

        val groupBExpectation =
            listOf(
                ChildPreschoolAbsenceRow(
                    childId = testData.childC.id,
                    firstName = testData.childC.firstName,
                    lastName = testData.childC.lastName,
                    hourlyTypeResults =
                        // 5h full day absence + 1.5h late arrival
                        preschoolAbsenceTypes.associateWith { type ->
                            when (type) {
                                AbsenceType.UNKNOWN_ABSENCE -> 5
                                AbsenceType.OTHER_ABSENCE -> 1
                                else -> 0
                            }
                        }
                )
            )
        return Pair(groupAExpectation, groupBExpectation)
    }

    private fun initTestData(keyDate: LocalDate): PreschoolAbsenceReportTestData {
        return db.transaction { tx ->
            tx.insert(admin)

            val testTerm = FiniteDateRange(keyDate.minusMonths(6), keyDate.plusMonths(6))
            val termId =
                tx.insertPreschoolTerm(testTerm, testTerm, testTerm, testTerm, DateSet.empty())

            val areaAId = tx.insert(DevCareArea(name = "Area A", shortName = "Area A"))
            val areaBId = tx.insert(DevCareArea(name = "Area B", shortName = "Area B"))

            val preschoolAId =
                tx.insert(
                    DevDaycare(
                        name = "Preschool A",
                        areaId = areaAId,
                        openingDate = keyDate.minusDays(7),
                        type = setOf(CareType.PRESCHOOL),
                        dailyPreschoolTime =
                            TimeRange(LocalTime.of(9, 0, 0), LocalTime.of(14, 0, 0))
                    )
                )
            val groupAId =
                tx.insert(
                    DevDaycareGroup(
                        GroupId(UUID.randomUUID()),
                        preschoolAId,
                        "Testiryhmä A",
                        startDate = keyDate.minusDays(7)
                    )
                )

            val groupBId =
                tx.insert(
                    DevDaycareGroup(
                        GroupId(UUID.randomUUID()),
                        preschoolAId,
                        "Testiryhmä B",
                        startDate = keyDate.minusDays(7)
                    )
                )

            val preschoolBId =
                tx.insert(
                    DevDaycare(
                        name = "Preschool B",
                        areaId = areaBId,
                        openingDate = keyDate.minusDays(7),
                        type = setOf(CareType.PRESCHOOL),
                        dailyPreschoolTime =
                            TimeRange(LocalTime.of(9, 0, 0), LocalTime.of(13, 0, 0))
                    )
                )

            tx.insert(unitSupervisorA)
            tx.insert(unitSupervisorB)

            tx.insertDaycareAclRow(preschoolAId, unitSupervisorA.id, UserRole.UNIT_SUPERVISOR)
            tx.insertDaycareAclRow(preschoolBId, unitSupervisorB.id, UserRole.UNIT_SUPERVISOR)

            val testChildAapo =
                DevPerson(
                    id = ChildId(UUID.randomUUID()),
                    dateOfBirth = keyDate.minusYears(6),
                    firstName = "Aapo",
                    lastName = "Aarnio"
                )
            tx.insert(testChildAapo, DevPersonType.CHILD)

            val testChildBertil =
                DevPerson(
                    ChildId(UUID.randomUUID()),
                    dateOfBirth = keyDate.minusYears(6),
                    firstName = "Bertil",
                    lastName = "Becker"
                )
            tx.insert(testChildBertil, DevPersonType.CHILD)

            val testChildCecil =
                DevPerson(
                    ChildId(UUID.randomUUID()),
                    dateOfBirth = keyDate.minusYears(6),
                    firstName = "Cecil",
                    lastName = "Cilliacus"
                )
            tx.insert(testChildCecil, DevPersonType.CHILD)

            val placementDuration = FiniteDateRange(keyDate.minusMonths(1), keyDate.plusMonths(1))
            val placementAId =
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL,
                        childId = testChildAapo.id,
                        unitId = preschoolAId,
                        startDate = placementDuration.start,
                        endDate = placementDuration.end
                    )
                )

            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL,
                    childId = testChildBertil.id,
                    unitId = preschoolAId,
                    startDate = placementDuration.start,
                    endDate = placementDuration.end
                )
            )

            val placementCId =
                tx.insert(
                    DevPlacement(
                        type = PlacementType.PRESCHOOL_DAYCARE,
                        childId = testChildCecil.id,
                        unitId = preschoolAId,
                        startDate = keyDate.minusMonths(1),
                        endDate = keyDate.plusMonths(1)
                    )
                )

            tx.insert(
                DevDaycareGroupPlacement(
                    GroupPlacementId(UUID.randomUUID()),
                    placementAId,
                    groupAId,
                    placementDuration.start,
                    placementDuration.end
                )
            )

            tx.insert(
                DevDaycareGroupPlacement(
                    GroupPlacementId(UUID.randomUUID()),
                    placementCId,
                    groupBId,
                    placementDuration.start,
                    placementDuration.end
                )
            )

            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildAapo.id,
                    keyDate,
                    AbsenceType.OTHER_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(keyDate),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE
                )
            )

            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildBertil.id,
                    keyDate,
                    AbsenceType.SICKLEAVE,
                    HelsinkiDateTime.atStartOfDay(keyDate),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE
                )
            )

            tx.insert(
                DevAbsence(
                    id = AbsenceId(UUID.randomUUID()),
                    testChildCecil.id,
                    keyDate,
                    AbsenceType.UNKNOWN_ABSENCE,
                    HelsinkiDateTime.atStartOfDay(keyDate),
                    EvakaUserId(admin.id.raw),
                    AbsenceCategory.NONBILLABLE
                )
            )

            tx.insertTestChildAttendance(
                testChildAapo.id,
                preschoolAId,
                HelsinkiDateTime.of(keyDate, LocalTime.of(8, 0)),
                // left an hour early
                HelsinkiDateTime.of(keyDate, LocalTime.of(13, 0))
            )

            tx.insertTestChildAttendance(
                testChildAapo.id,
                preschoolAId,
                // arrived an hour late
                HelsinkiDateTime.of(keyDate.plusDays(1), LocalTime.of(10, 0)),
                HelsinkiDateTime.of(keyDate.plusDays(1), LocalTime.of(16, 0))
            )

            tx.insertTestChildAttendance(
                testChildAapo.id,
                preschoolAId,
                // arrived 45 minutes late
                HelsinkiDateTime.of(keyDate.plusDays(2), LocalTime.of(9, 45)),
                HelsinkiDateTime.of(keyDate.plusDays(2), LocalTime.of(16, 0))
            )

            tx.insertTestChildAttendance(
                testChildBertil.id,
                preschoolBId,
                HelsinkiDateTime.of(keyDate, LocalTime.of(9, 0)),
                HelsinkiDateTime.of(keyDate, LocalTime.of(16, 0))
            )

            // arrives 1.5h late
            tx.insertTestChildAttendance(
                testChildCecil.id,
                preschoolAId,
                HelsinkiDateTime.of(keyDate.plusDays(1), LocalTime.of(10, 30)),
                HelsinkiDateTime.of(keyDate.plusDays(1), LocalTime.of(15, 30))
            )

            // unfinished attendance shouldn't show up
            tx.insertTestChildAttendance(
                testChildCecil.id,
                preschoolAId,
                HelsinkiDateTime.of(keyDate.plusDays(2), LocalTime.of(9, 0)),
                null
            )

            PreschoolAbsenceReportTestData(
                termId,
                testChildAapo,
                testChildBertil,
                testChildCecil,
                groupAId,
                groupBId,
                preschoolAId,
                preschoolBId,
                areaAId,
                areaBId
            )
        }
    }

    data class PreschoolAbsenceReportTestData(
        val termId: PreschoolTermId,
        val childA: DevPerson,
        val childB: DevPerson,
        val childC: DevPerson,
        val groupAId: GroupId,
        val groupBId: GroupId,
        val daycareAId: DaycareId,
        val daycareBId: DaycareId,
        val areaAId: AreaId,
        val areaBId: AreaId
    )
}

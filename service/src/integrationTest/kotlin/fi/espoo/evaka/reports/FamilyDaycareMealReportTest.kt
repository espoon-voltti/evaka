// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.AreaId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.auth.insertDaycareAclRow
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevChild
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertTestChildAttendance
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FamilyDaycareMealReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired private lateinit var familyDaycareMealReport: FamilyDaycareMealReport

    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val adminLoginUser = AuthenticatedUser.Employee(admin.id, admin.roles)

    private val unitSupervisor = DevEmployee(roles = setOf())
    private val unitSupervisorUser =
        AuthenticatedUser.Employee(unitSupervisor.id, unitSupervisor.roles)

    @Test
    fun `Report returns correct meal counts`() {
        val mockToday =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 8), LocalTime.of(12, 15)))

        val testData = initTestData(mockToday.today())

        val reportAll =
            familyDaycareMealReport.getFamilyDaycareMealReport(
                dbInstance(),
                adminLoginUser,
                mockToday,
                mockToday.today().minusDays(7),
                mockToday.today()
            )

        val expectedBaseResult =
            FamilyDaycareMealReport.FamilyDaycareMealReportResult(
                breakfastCount = 1,
                lunchCount = 2,
                snackCount = 2,
                areaResults =
                    listOf(
                        FamilyDaycareMealReport.FamilyDaycareMealAreaResult(
                            areaName = "Area A",
                            areaId = testData.areaAId,
                            breakfastCount = 1,
                            lunchCount = 1,
                            snackCount = 1,
                            daycareResults =
                                listOf(
                                    FamilyDaycareMealReport.FamilyDaycareMealDaycareResult(
                                        daycareName = "Family Daycare A",
                                        daycareId = testData.daycareAId,
                                        breakfastCount = 1,
                                        lunchCount = 1,
                                        snackCount = 1,
                                        childResults =
                                            listOf(
                                                FamilyDaycareMealReport
                                                    .FamilyDaycareMealChildResult(
                                                        childId = testData.childAId,
                                                        firstName = "Aapo",
                                                        lastName = "Aarnio",
                                                        breakfastCount = 1,
                                                        lunchCount = 1,
                                                        snackCount = 1,
                                                    )
                                            )
                                    )
                                )
                        ),
                        FamilyDaycareMealReport.FamilyDaycareMealAreaResult(
                            areaName = "Area B",
                            areaId = testData.areaBId,
                            breakfastCount = 0,
                            lunchCount = 1,
                            snackCount = 1,
                            daycareResults =
                                listOf(
                                    FamilyDaycareMealReport.FamilyDaycareMealDaycareResult(
                                        daycareName = "Family Daycare B",
                                        daycareId = testData.daycareBId,
                                        breakfastCount = 0,
                                        lunchCount = 1,
                                        snackCount = 1,
                                        childResults =
                                            listOf(
                                                FamilyDaycareMealReport
                                                    .FamilyDaycareMealChildResult(
                                                        childId = testData.childBId,
                                                        firstName = "Bertil",
                                                        lastName = "Becker",
                                                        breakfastCount = 0,
                                                        lunchCount = 1,
                                                        snackCount = 1,
                                                    )
                                            )
                                    )
                                )
                        )
                    )
            )

        assertEquals(expectedBaseResult, reportAll)
    }

    @Test
    fun `Report returns data based on access rights`() {
        val mockToday =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 8), LocalTime.of(12, 15)))
        val testData = initTestData(mockToday.today())
        val reportAll =
            familyDaycareMealReport.getFamilyDaycareMealReport(
                dbInstance(),
                unitSupervisorUser,
                mockToday,
                mockToday.today().minusDays(7),
                mockToday.today()
            )

        val expectedResult =
            FamilyDaycareMealReport.FamilyDaycareMealReportResult(
                breakfastCount = 0,
                lunchCount = 1,
                snackCount = 1,
                areaResults =
                    listOf(
                        FamilyDaycareMealReport.FamilyDaycareMealAreaResult(
                            areaName = "Area B",
                            areaId = testData.areaBId,
                            breakfastCount = 0,
                            lunchCount = 1,
                            snackCount = 1,
                            daycareResults =
                                listOf(
                                    FamilyDaycareMealReport.FamilyDaycareMealDaycareResult(
                                        daycareName = "Family Daycare B",
                                        daycareId = testData.daycareBId,
                                        breakfastCount = 0,
                                        lunchCount = 1,
                                        snackCount = 1,
                                        childResults =
                                            listOf(
                                                FamilyDaycareMealReport
                                                    .FamilyDaycareMealChildResult(
                                                        childId = testData.childBId,
                                                        firstName = "Bertil",
                                                        lastName = "Becker",
                                                        breakfastCount = 0,
                                                        lunchCount = 1,
                                                        snackCount = 1,
                                                    )
                                            )
                                    )
                                )
                        )
                    )
            )

        assertEquals(expectedResult, reportAll)
    }

    @Test
    fun `Multiple attendance meal time markings are only counted once`() {
        val mockToday =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2022, 12, 8), LocalTime.of(12, 15)))
        val testData = initTestData(mockToday.today())

        val childMId =
            db.transaction { tx ->
                val childMId =
                    tx.insert(
                        DevPerson(firstName = "Mark", lastName = "Multiple"),
                        DevPersonType.RAW_ROW
                    )
                tx.insert(DevChild(id = childMId))
                tx.insertTestPlacement(
                    childId = childMId,
                    unitId = testData.daycareBId,
                    startDate = mockToday.today().minusMonths(1),
                    endDate = mockToday.today().plusMonths(1),
                    type = PlacementType.DAYCARE,
                )
                tx.insertTestChildAttendance(
                    childMId,
                    testData.daycareBId,
                    HelsinkiDateTime.of(mockToday.today(), LocalTime.of(10, 30)),
                    HelsinkiDateTime.of(mockToday.today(), LocalTime.of(11, 0))
                )

                tx.insertTestChildAttendance(
                    childMId,
                    testData.daycareBId,
                    HelsinkiDateTime.of(mockToday.today(), LocalTime.of(11, 30)),
                    HelsinkiDateTime.of(mockToday.today(), LocalTime.of(12, 0))
                )

                tx.insertTestChildAttendance(
                    childMId,
                    testData.daycareBId,
                    HelsinkiDateTime.of(mockToday.today(), LocalTime.of(12, 0)),
                    HelsinkiDateTime.of(mockToday.today(), LocalTime.of(12, 30))
                )
                childMId
            }
        val reportAll =
            familyDaycareMealReport.getFamilyDaycareMealReport(
                dbInstance(),
                unitSupervisorUser,
                mockToday,
                mockToday.today().minusDays(7),
                mockToday.today()
            )

        val expectedResult =
            FamilyDaycareMealReport.FamilyDaycareMealReportResult(
                breakfastCount = 0,
                lunchCount = 2,
                snackCount = 1,
                areaResults =
                    listOf(
                        FamilyDaycareMealReport.FamilyDaycareMealAreaResult(
                            areaName = "Area B",
                            areaId = testData.areaBId,
                            breakfastCount = 0,
                            lunchCount = 2,
                            snackCount = 1,
                            daycareResults =
                                listOf(
                                    FamilyDaycareMealReport.FamilyDaycareMealDaycareResult(
                                        daycareName = "Family Daycare B",
                                        daycareId = testData.daycareBId,
                                        breakfastCount = 0,
                                        lunchCount = 2,
                                        snackCount = 1,
                                        childResults =
                                            listOf(
                                                FamilyDaycareMealReport
                                                    .FamilyDaycareMealChildResult(
                                                        childId = testData.childBId,
                                                        firstName = "Bertil",
                                                        lastName = "Becker",
                                                        breakfastCount = 0,
                                                        lunchCount = 1,
                                                        snackCount = 1,
                                                    ),
                                                FamilyDaycareMealReport
                                                    .FamilyDaycareMealChildResult(
                                                        childId = childMId,
                                                        firstName = "Mark",
                                                        lastName = "Multiple",
                                                        breakfastCount = 0,
                                                        lunchCount = 1,
                                                        snackCount = 0,
                                                    )
                                            )
                                    )
                                )
                        )
                    )
            )

        assertEquals(expectedResult, reportAll)
    }

    @Test
    fun `Placement change won't cause count duplication`() {
        val examinationDay = LocalDate.of(2022, 12, 8)
        val mockToday = MockEvakaClock(HelsinkiDateTime.of(examinationDay, LocalTime.of(12, 15)))
        val testData = initTestData(examinationDay)

        val previousAttendanceDay = examinationDay.minusDays(3)
        val childPId =
            db.transaction { tx ->
                val childPId =
                    tx.insert(
                        DevPerson(firstName = "Peter", lastName = "Placer"),
                        DevPersonType.RAW_ROW
                    )
                tx.insert(DevChild(id = childPId))
                tx.insertTestPlacement(
                    childId = childPId,
                    unitId = testData.daycareBId,
                    startDate = previousAttendanceDay.minusMonths(1),
                    endDate = previousAttendanceDay.plusDays(1),
                    type = PlacementType.DAYCARE,
                )
                tx.insertTestPlacement(
                    childId = childPId,
                    unitId = testData.daycareBId,
                    startDate = previousAttendanceDay.plusDays(2),
                    endDate = examinationDay.plusMonths(1),
                    type = PlacementType.DAYCARE,
                )
                tx.insertTestChildAttendance(
                    childPId,
                    testData.daycareBId,
                    HelsinkiDateTime.of(previousAttendanceDay, LocalTime.of(8, 0)),
                    HelsinkiDateTime.of(previousAttendanceDay, LocalTime.of(16, 0))
                )

                tx.insertTestChildAttendance(
                    childPId,
                    testData.daycareBId,
                    HelsinkiDateTime.of(examinationDay, LocalTime.of(8, 0)),
                    HelsinkiDateTime.of(examinationDay, LocalTime.of(16, 0))
                )

                childPId
            }
        val reportAll =
            familyDaycareMealReport.getFamilyDaycareMealReport(
                dbInstance(),
                unitSupervisorUser,
                mockToday,
                mockToday.today().minusDays(7),
                mockToday.today()
            )

        val expectedResult =
            FamilyDaycareMealReport.FamilyDaycareMealReportResult(
                breakfastCount = 2,
                lunchCount = 3,
                snackCount = 3,
                areaResults =
                    listOf(
                        FamilyDaycareMealReport.FamilyDaycareMealAreaResult(
                            areaName = "Area B",
                            areaId = testData.areaBId,
                            breakfastCount = 2,
                            lunchCount = 3,
                            snackCount = 3,
                            daycareResults =
                                listOf(
                                    FamilyDaycareMealReport.FamilyDaycareMealDaycareResult(
                                        daycareName = "Family Daycare B",
                                        daycareId = testData.daycareBId,
                                        breakfastCount = 2,
                                        lunchCount = 3,
                                        snackCount = 3,
                                        childResults =
                                            listOf(
                                                FamilyDaycareMealReport
                                                    .FamilyDaycareMealChildResult(
                                                        childId = testData.childBId,
                                                        firstName = "Bertil",
                                                        lastName = "Becker",
                                                        breakfastCount = 0,
                                                        lunchCount = 1,
                                                        snackCount = 1,
                                                    ),
                                                FamilyDaycareMealReport
                                                    .FamilyDaycareMealChildResult(
                                                        childId = childPId,
                                                        firstName = "Peter",
                                                        lastName = "Placer",
                                                        breakfastCount = 2,
                                                        lunchCount = 2,
                                                        snackCount = 2,
                                                    )
                                            )
                                    )
                                )
                        )
                    )
            )

        assertEquals(expectedResult, reportAll)
    }

    @Test
    fun `Report won't calculate backup care attendances`() {
        val examinationDay = LocalDate.of(2022, 12, 8)
        val mockToday = MockEvakaClock(HelsinkiDateTime.of(examinationDay, LocalTime.of(12, 15)))
        val testData = initTestData(examinationDay)

        val previousAttendanceDay = examinationDay.minusDays(3)
        val childPId =
            db.transaction { tx ->
                val childPId =
                    tx.insert(
                        DevPerson(firstName = "Peter", lastName = "Placer"),
                        DevPersonType.RAW_ROW
                    )
                tx.insert(DevChild(id = childPId))

                val backupDaycareId =
                    tx.insert(
                        DevDaycare(
                            name = "Backup Daycare",
                            areaId = testData.areaBId,
                            openingDate = previousAttendanceDay.minusDays(7),
                            type = setOf(CareType.FAMILY)
                        )
                    )

                tx.insertTestPlacement(
                    childId = childPId,
                    unitId = testData.daycareBId,
                    startDate = previousAttendanceDay.minusMonths(1),
                    endDate = examinationDay.plusMonths(1),
                    type = PlacementType.DAYCARE,
                )

                tx.insert(
                    DevBackupCare(
                        childId = childPId,
                        unitId = backupDaycareId,
                        period = FiniteDateRange(previousAttendanceDay, previousAttendanceDay)
                    )
                )

                tx.insertTestChildAttendance(
                    childPId,
                    backupDaycareId,
                    HelsinkiDateTime.of(previousAttendanceDay, LocalTime.of(8, 0)),
                    HelsinkiDateTime.of(previousAttendanceDay, LocalTime.of(16, 0))
                )

                tx.insertTestChildAttendance(
                    childPId,
                    testData.daycareBId,
                    HelsinkiDateTime.of(examinationDay, LocalTime.of(8, 0)),
                    HelsinkiDateTime.of(examinationDay, LocalTime.of(16, 0))
                )

                childPId
            }
        val reportAll =
            familyDaycareMealReport.getFamilyDaycareMealReport(
                dbInstance(),
                adminLoginUser,
                mockToday,
                examinationDay.minusDays(7),
                examinationDay
            )

        val expectedResult =
            FamilyDaycareMealReport.FamilyDaycareMealReportResult(
                breakfastCount = 2,
                lunchCount = 3,
                snackCount = 3,
                areaResults =
                    listOf(
                        FamilyDaycareMealReport.FamilyDaycareMealAreaResult(
                            areaName = "Area A",
                            areaId = testData.areaAId,
                            breakfastCount = 1,
                            lunchCount = 1,
                            snackCount = 1,
                            daycareResults =
                                listOf(
                                    FamilyDaycareMealReport.FamilyDaycareMealDaycareResult(
                                        daycareName = "Family Daycare A",
                                        daycareId = testData.daycareAId,
                                        breakfastCount = 1,
                                        lunchCount = 1,
                                        snackCount = 1,
                                        childResults =
                                            listOf(
                                                FamilyDaycareMealReport
                                                    .FamilyDaycareMealChildResult(
                                                        childId = testData.childAId,
                                                        firstName = "Aapo",
                                                        lastName = "Aarnio",
                                                        breakfastCount = 1,
                                                        lunchCount = 1,
                                                        snackCount = 1,
                                                    )
                                            )
                                    )
                                )
                        ),
                        FamilyDaycareMealReport.FamilyDaycareMealAreaResult(
                            areaName = "Area B",
                            areaId = testData.areaBId,
                            breakfastCount = 1,
                            lunchCount = 2,
                            snackCount = 2,
                            daycareResults =
                                listOf(
                                    FamilyDaycareMealReport.FamilyDaycareMealDaycareResult(
                                        daycareName = "Family Daycare B",
                                        daycareId = testData.daycareBId,
                                        breakfastCount = 1,
                                        lunchCount = 2,
                                        snackCount = 2,
                                        childResults =
                                            listOf(
                                                FamilyDaycareMealReport
                                                    .FamilyDaycareMealChildResult(
                                                        childId = testData.childBId,
                                                        firstName = "Bertil",
                                                        lastName = "Becker",
                                                        breakfastCount = 0,
                                                        lunchCount = 1,
                                                        snackCount = 1,
                                                    ),
                                                FamilyDaycareMealReport
                                                    .FamilyDaycareMealChildResult(
                                                        childId = childPId,
                                                        firstName = "Peter",
                                                        lastName = "Placer",
                                                        breakfastCount = 1,
                                                        lunchCount = 1,
                                                        snackCount = 1,
                                                    )
                                            )
                                    )
                                )
                        )
                    )
            )

        assertEquals(expectedResult, reportAll)
    }

    private fun initTestData(keyDate: LocalDate): FamilyDaycareReportTestData {
        return db.transaction { tx ->
            tx.insert(admin)
            val unitSupervisorId = tx.insert(unitSupervisor)

            val areaAId = tx.insert(DevCareArea(name = "Area A", shortName = "Area A"))
            val areaBId = tx.insert(DevCareArea(name = "Area B", shortName = "Area B"))
            val daycareAId =
                tx.insert(
                    DevDaycare(
                        name = "Family Daycare A",
                        areaId = areaAId,
                        openingDate = keyDate.minusDays(7),
                        type = setOf(CareType.FAMILY)
                    )
                )
            val daycareBId =
                tx.insert(
                    DevDaycare(
                        name = "Family Daycare B",
                        areaId = areaBId,
                        openingDate = keyDate.minusDays(7),
                        type = setOf(CareType.FAMILY)
                    )
                )

            tx.insertDaycareAclRow(daycareBId, unitSupervisorId, UserRole.UNIT_SUPERVISOR)

            val childAId =
                tx.insert(
                    DevPerson(
                        dateOfBirth = keyDate.minusYears(4),
                        firstName = "Aapo",
                        lastName = "Aarnio"
                    ),
                    DevPersonType.RAW_ROW
                )
            val childBId =
                tx.insert(
                    DevPerson(
                        dateOfBirth = keyDate.minusYears(4),
                        firstName = "Bertil",
                        lastName = "Becker"
                    ),
                    DevPersonType.RAW_ROW
                )
            tx.insert(DevChild(id = childAId))
            tx.insert(DevChild(id = childBId))
            tx.insertTestPlacement(
                childId = childAId,
                unitId = daycareAId,
                startDate = keyDate.minusMonths(1),
                endDate = keyDate.plusMonths(1),
                type = PlacementType.DAYCARE
            )

            tx.insertTestPlacement(
                childId = childBId,
                unitId = daycareBId,
                startDate = keyDate.minusMonths(1),
                endDate = keyDate.plusMonths(1),
                type = PlacementType.DAYCARE
            )

            tx.insertTestChildAttendance(
                childAId,
                daycareAId,
                HelsinkiDateTime.of(keyDate, LocalTime.of(8, 0)),
                HelsinkiDateTime.of(keyDate, LocalTime.of(16, 0))
            )

            tx.insertTestChildAttendance(
                childBId,
                daycareBId,
                HelsinkiDateTime.of(keyDate, LocalTime.of(9, 0)),
                HelsinkiDateTime.of(keyDate, LocalTime.of(16, 0))
            )

            FamilyDaycareReportTestData(
                childAId,
                childBId,
                daycareAId,
                daycareBId,
                areaAId,
                areaBId
            )
        }
    }

    data class FamilyDaycareReportTestData(
        val childAId: PersonId,
        val childBId: PersonId,
        val daycareAId: DaycareId,
        val daycareBId: DaycareId,
        val areaAId: AreaId,
        val areaBId: AreaId
    )
}

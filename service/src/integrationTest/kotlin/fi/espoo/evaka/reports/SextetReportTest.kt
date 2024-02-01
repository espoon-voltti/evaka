// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.insertGeneralTestFixtures
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.dev.insertTestAbsence
import fi.espoo.evaka.shared.dev.insertTestBackUpCare
import fi.espoo.evaka.shared.dev.insertTestHoliday
import fi.espoo.evaka.shared.dev.insertTestPlacement
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testChild_7
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testRoundTheClockDaycare
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SextetReportTest : PureJdbiTest(resetDbBeforeEach = true) {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insertGeneralTestFixtures()
            tx.insertTestHoliday(LocalDate.of(2021, 12, 6))
        }
    }

    @Test
    fun `it works`() {
        // 10 operational days (6.12. is a holiday)
        val startDate = LocalDate.of(2021, 12, 1)
        val endDate = LocalDate.of(2021, 12, 15)

        // midpoint
        val midDate = LocalDate.of(2021, 12, 8)

        db.transaction { tx ->
            // DAYCARE

            // 10
            tx.insertTestPlacement(
                childId = testChild_1.id,
                unitId = testDaycare.id,
                type = PlacementType.DAYCARE,
                startDate = startDate,
                endDate = endDate
            )

            // 9
            tx.insertTestPlacement(
                childId = testChild_2.id,
                unitId = testDaycare.id,
                type = PlacementType.DAYCARE,
                startDate = startDate,
                endDate = endDate
            )
            tx.insertTestAbsence(
                childId = testChild_2.id,
                date = LocalDate.of(2021, 12, 1),
                category = AbsenceCategory.BILLABLE
            )

            // 10
            tx.insertTestPlacement(
                childId = testChild_3.id,
                unitId = testDaycare2.id,
                type = PlacementType.DAYCARE,
                startDate = startDate,
                endDate = endDate
            )

            // Round the clock -> 15
            tx.insertTestPlacement(
                childId = testChild_4.id,
                unitId = testRoundTheClockDaycare.id,
                type = PlacementType.DAYCARE,
                startDate = startDate,
                endDate = endDate
            )

            // PRESCHOOL_DAYCARE

            // 9
            tx.insertTestPlacement(
                childId = testChild_5.id,
                unitId = testDaycare.id,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = startDate,
                endDate = endDate
            )
            // Does not affect because only half day off
            tx.insertTestAbsence(
                childId = testChild_5.id,
                date = LocalDate.of(2021, 12, 1),
                category = AbsenceCategory.BILLABLE
            )
            // Affects because both halves off
            tx.insertTestAbsence(
                childId = testChild_5.id,
                date = LocalDate.of(2021, 12, 2),
                category = AbsenceCategory.BILLABLE
            )
            tx.insertTestAbsence(
                childId = testChild_5.id,
                date = LocalDate.of(2021, 12, 2),
                category = AbsenceCategory.NONBILLABLE
            )

            // 5 to daycare 1
            tx.insertTestPlacement(
                childId = testChild_6.id,
                unitId = testDaycare.id,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = startDate,
                endDate = endDate
            )
            // 5 to daycare 2 (backup care)
            tx.insertTestBackUpCare(
                childId = testChild_6.id,
                unitId = testDaycare2.id,
                startDate = startDate,
                endDate = midDate
            )

            // 10 to daycare 1 (same unit in backup care)
            tx.insertTestPlacement(
                childId = testChild_7.id,
                unitId = testDaycare.id,
                type = PlacementType.PRESCHOOL_DAYCARE,
                startDate = startDate,
                endDate = endDate
            )
            tx.insertTestBackUpCare(
                childId = testChild_7.id,
                unitId = testDaycare.id,
                startDate = startDate,
                endDate = midDate
            )
        }

        val report1 =
            db.read {
                it.sextetReport(
                    from = LocalDate.of(2021, 1, 1),
                    to = LocalDate.of(2021, 12, 31),
                    placementType = PlacementType.DAYCARE
                )
            }

        assertEquals(
            listOf(
                SextetReportRow(testDaycare.id, testDaycare.name, PlacementType.DAYCARE, 10 + 9),
                SextetReportRow(testDaycare2.id, testDaycare2.name, PlacementType.DAYCARE, 10),
                SextetReportRow(
                    testRoundTheClockDaycare.id,
                    testRoundTheClockDaycare.name,
                    PlacementType.DAYCARE,
                    15
                )
            ),
            report1
        )

        val report2 =
            db.read {
                it.sextetReport(
                    from = LocalDate.of(2021, 1, 1),
                    to = LocalDate.of(2021, 12, 31),
                    placementType = PlacementType.PRESCHOOL_DAYCARE
                )
            }

        assertEquals(
            listOf(
                SextetReportRow(
                    testDaycare.id,
                    testDaycare.name,
                    PlacementType.PRESCHOOL_DAYCARE,
                    9 + 5 + 10
                ),
                SextetReportRow(
                    testDaycare2.id,
                    testDaycare2.name,
                    PlacementType.PRESCHOOL_DAYCARE,
                    5
                )
            ),
            report2
        )
    }
}

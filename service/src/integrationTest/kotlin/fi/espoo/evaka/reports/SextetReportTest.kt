// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevHoliday
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testChild_2
import fi.espoo.evaka.testChild_3
import fi.espoo.evaka.testChild_4
import fi.espoo.evaka.testChild_5
import fi.espoo.evaka.testChild_6
import fi.espoo.evaka.testChild_7
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testDaycare2
import fi.espoo.evaka.testDecisionMaker_1
import fi.espoo.evaka.testRoundTheClockDaycare
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SextetReportTest : PureJdbiTest(resetDbBeforeEach = true) {
    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(testDecisionMaker_1)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testDaycare2)
            tx.insert(testRoundTheClockDaycare)
            listOf(
                    testChild_1,
                    testChild_2,
                    testChild_3,
                    testChild_4,
                    testChild_5,
                    testChild_6,
                    testChild_7,
                )
                .forEach { tx.insert(it, DevPersonType.CHILD) }
            tx.insertServiceNeedOptions()
            tx.insert(DevHoliday(LocalDate.of(2021, 12, 6), "holiday"))
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
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_1.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // 9
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_2.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = testChild_2.id,
                    date = LocalDate.of(2021, 12, 1),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )

            // 10
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = testChild_3.id,
                    unitId = testDaycare2.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Round the clock -> 15
            tx.insert(
                    DevPlacement(
                        type = PlacementType.DAYCARE,
                        childId = testChild_4.id,
                        unitId = testRoundTheClockDaycare.id,
                        startDate = startDate,
                        endDate = endDate,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = startDate,
                            endDate = endDate,
                            optionId = snDaycareFullDay35.id,
                            shiftCare = ShiftCareType.FULL,
                            confirmedBy = testDecisionMaker_1.evakaUserId,
                        )
                    )
                }

            // PRESCHOOL_DAYCARE

            // 9
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_5.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            // Does not affect because only half day off
            tx.insert(
                DevAbsence(
                    childId = testChild_5.id,
                    date = LocalDate.of(2021, 12, 1),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            // Affects because both halves off
            tx.insert(
                DevAbsence(
                    childId = testChild_5.id,
                    date = LocalDate.of(2021, 12, 2),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = testChild_5.id,
                    date = LocalDate.of(2021, 12, 2),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )

            // 5 to daycare 1
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_6.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            // 5 to daycare 2 (backup care)
            tx.insert(
                DevBackupCare(
                    childId = testChild_6.id,
                    unitId = testDaycare2.id,
                    groupId = null,
                    period = FiniteDateRange(startDate, midDate),
                )
            )

            // 10 to daycare 1 (same unit in backup care)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = testChild_7.id,
                    unitId = testDaycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            tx.insert(
                DevBackupCare(
                    childId = testChild_7.id,
                    unitId = testDaycare.id,
                    groupId = null,
                    period = FiniteDateRange(startDate, midDate),
                )
            )
        }

        val report1 =
            db.read {
                it.sextetReport(
                    from = LocalDate.of(2021, 1, 1),
                    to = LocalDate.of(2021, 12, 31),
                    placementType = PlacementType.DAYCARE,
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
                    15,
                ),
            ),
            report1,
        )

        val report2 =
            db.read {
                it.sextetReport(
                    from = LocalDate.of(2021, 1, 1),
                    to = LocalDate.of(2021, 12, 31),
                    placementType = PlacementType.PRESCHOOL_DAYCARE,
                )
            }

        assertEquals(
            listOf(
                SextetReportRow(
                    testDaycare.id,
                    testDaycare.name,
                    PlacementType.PRESCHOOL_DAYCARE,
                    9 + 5 + 10,
                ),
                SextetReportRow(
                    testDaycare2.id,
                    testDaycare2.name,
                    PlacementType.PRESCHOOL_DAYCARE,
                    5,
                ),
            ),
            report2,
        )
    }
}

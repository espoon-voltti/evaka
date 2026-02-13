// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.dev.DevAbsence
import fi.espoo.evaka.shared.dev.DevBackupCare
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import fi.espoo.evaka.snDaycareFullDay35
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SextetReportTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id, name = "Daycare 1")
    private val daycare2 =
        DevDaycare(areaId = area.id, name = "Daycare 2", providerType = ProviderType.PURCHASED)
    private val roundTheClockDaycare =
        DevDaycare(
            areaId = area.id,
            name = "Round the Clock Daycare",
            shiftCareOperationTimes =
                List(7) { TimeRange(LocalTime.of(0, 0), LocalTime.of(23, 59)) },
            shiftCareOpenOnHolidays = true,
        )
    private val employee = DevEmployee()
    private val child1 = DevPerson()
    private val child2 = DevPerson()
    private val child3 = DevPerson()
    private val child4 = DevPerson()
    private val child5 = DevPerson()
    private val child6 = DevPerson()
    private val child7 = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(employee)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(daycare2)
            tx.insert(roundTheClockDaycare)
            listOf(child1, child2, child3, child4, child5, child6, child7).forEach {
                tx.insert(it, DevPersonType.CHILD)
            }
            tx.insertServiceNeedOptions()
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
                    childId = child1.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // 9
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child2.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = child2.id,
                    date = LocalDate.of(2021, 12, 1),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )

            // 10
            tx.insert(
                DevPlacement(
                    type = PlacementType.DAYCARE,
                    childId = child3.id,
                    unitId = daycare2.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )

            // Round the clock -> 15
            tx.insert(
                    DevPlacement(
                        type = PlacementType.DAYCARE,
                        childId = child4.id,
                        unitId = roundTheClockDaycare.id,
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
                            confirmedBy = employee.evakaUserId,
                        )
                    )
                }

            // PRESCHOOL_DAYCARE

            // 9
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child5.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            // Does not affect because only half day off
            tx.insert(
                DevAbsence(
                    childId = child5.id,
                    date = LocalDate.of(2021, 12, 1),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            // Affects because both halves off
            tx.insert(
                DevAbsence(
                    childId = child5.id,
                    date = LocalDate.of(2021, 12, 2),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.BILLABLE,
                )
            )
            tx.insert(
                DevAbsence(
                    childId = child5.id,
                    date = LocalDate.of(2021, 12, 2),
                    absenceType = AbsenceType.SICKLEAVE,
                    absenceCategory = AbsenceCategory.NONBILLABLE,
                )
            )

            // 5 to daycare 1
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child6.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            // 5 to daycare 2 (backup care)
            tx.insert(
                DevBackupCare(
                    childId = child6.id,
                    unitId = daycare2.id,
                    groupId = null,
                    period = FiniteDateRange(startDate, midDate),
                )
            )

            // 10 to daycare 1 (same unit in backup care)
            tx.insert(
                DevPlacement(
                    type = PlacementType.PRESCHOOL_DAYCARE,
                    childId = child7.id,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            tx.insert(
                DevBackupCare(
                    childId = child7.id,
                    unitId = daycare.id,
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
                    unitProviderTypes = null,
                )
            }

        assertEquals(
            listOf(
                SextetReportRow(daycare.id, daycare.name, PlacementType.DAYCARE, 10 + 9),
                SextetReportRow(daycare2.id, daycare2.name, PlacementType.DAYCARE, 10),
                SextetReportRow(
                    roundTheClockDaycare.id,
                    roundTheClockDaycare.name,
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
                    unitProviderTypes = null,
                )
            }

        assertEquals(
            listOf(
                SextetReportRow(
                    daycare.id,
                    daycare.name,
                    PlacementType.PRESCHOOL_DAYCARE,
                    9 + 5 + 10,
                ),
                SextetReportRow(daycare2.id, daycare2.name, PlacementType.PRESCHOOL_DAYCARE, 5),
            ),
            report2,
        )

        val report3 =
            db.read {
                it.sextetReport(
                    from = LocalDate.of(2021, 1, 1),
                    to = LocalDate.of(2021, 12, 31),
                    placementType = PlacementType.PRESCHOOL_DAYCARE,
                    unitProviderTypes = setOf(ProviderType.MUNICIPAL),
                )
            }

        assertEquals(
            listOf(
                SextetReportRow(
                    daycare.id,
                    daycare.name,
                    PlacementType.PRESCHOOL_DAYCARE,
                    9 + 5 + 10,
                )
            ),
            report3,
        )

        val report4 =
            db.read {
                it.sextetReport(
                    from = LocalDate.of(2021, 1, 1),
                    to = LocalDate.of(2021, 12, 31),
                    placementType = PlacementType.PRESCHOOL_DAYCARE,
                    unitProviderTypes = setOf(ProviderType.PURCHASED),
                )
            }

        assertEquals(
            listOf(SextetReportRow(daycare2.id, daycare2.name, PlacementType.PRESCHOOL_DAYCARE, 5)),
            report4,
        )

        val report5 =
            db.read {
                it.sextetReport(
                    from = LocalDate.of(2021, 1, 1),
                    to = LocalDate.of(2021, 12, 31),
                    placementType = PlacementType.PRESCHOOL_DAYCARE,
                    unitProviderTypes = setOf(ProviderType.MUNICIPAL, ProviderType.PURCHASED),
                )
            }

        assertEquals(
            listOf(
                SextetReportRow(
                    daycare.id,
                    daycare.name,
                    PlacementType.PRESCHOOL_DAYCARE,
                    9 + 5 + 10,
                ),
                SextetReportRow(daycare2.id, daycare2.name, PlacementType.PRESCHOOL_DAYCARE, 5),
            ),
            report5,
        )
    }
}

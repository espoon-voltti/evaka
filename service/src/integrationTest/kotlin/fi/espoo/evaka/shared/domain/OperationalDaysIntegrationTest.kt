// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.shared.domain

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevHoliday
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.snDaycareFullDay35
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OperationalDaysIntegrationTest : PureJdbiTest(resetDbBeforeEach = true) {
    val month = FiniteDateRange.ofMonth(2024, Month.MAY)
    val fullDay = TimeRange(LocalTime.parse("00:00"), LocalTime.parse("23:59"))
    val placementRange = FiniteDateRange(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 5, 28))

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(DevHoliday(date = LocalDate.of(2024, 5, 1), description = "Vappu"))
            tx.insert(
                DevHoliday(
                    date = LocalDate.of(2024, 5, 19),
                    description = "Helluntai", // On Sunday
                )
            )
            tx.insertServiceNeedOption(snDaycareFullDay35)
        }
    }

    @Test
    fun `daycare without shift care`() {
        val daycareId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        operationTimes =
                            listOf(fullDay, fullDay, fullDay, fullDay, fullDay, null, null),
                        shiftCareOperationTimes = null,
                        shiftCareOpenOnHolidays = false,
                    )
                )
            }
        val normalChild = db.transaction { tx -> insertChild(tx, daycareId, ShiftCareType.NONE) }
        val shiftCareChild = db.transaction { tx -> insertChild(tx, daycareId, ShiftCareType.FULL) }

        val result =
            db.transaction {
                it.getOperationalDatesForChildren(month, setOf(normalChild, shiftCareChild))
            }

        // 20 mon-fri days during placement range minus one holiday
        assertEquals(19, result[normalChild]!!.size)

        // shift care is not provided in the unit so service need shift care has no effect
        assertEquals(result[normalChild], result[shiftCareChild])
    }

    @Test
    fun `daycare with shift care, not open on holidays`() {
        val daycareId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        operationTimes =
                            listOf(fullDay, fullDay, fullDay, fullDay, fullDay, null, null),
                        shiftCareOperationTimes =
                            listOf(fullDay, fullDay, fullDay, fullDay, fullDay, fullDay, fullDay),
                        shiftCareOpenOnHolidays = false,
                    )
                )
            }
        val normalChild = db.transaction { tx -> insertChild(tx, daycareId, ShiftCareType.NONE) }
        val shiftCareChild = db.transaction { tx -> insertChild(tx, daycareId, ShiftCareType.FULL) }

        val result =
            db.transaction {
                it.getOperationalDatesForChildren(month, setOf(normalChild, shiftCareChild))
            }

        // 20 mon-fri days during placement range minus one holiday
        assertEquals(19, result[normalChild]!!.size)

        // 28 mon-sun days during placement range minus two holidays
        assertEquals(26, result[shiftCareChild]!!.size)
    }

    @Test
    fun `daycare with shift care, shift care also open on holidays`() {
        val daycareId =
            db.transaction { tx ->
                val areaId = tx.insert(DevCareArea())
                tx.insert(
                    DevDaycare(
                        areaId = areaId,
                        operationTimes =
                            listOf(fullDay, fullDay, fullDay, fullDay, fullDay, null, null),
                        shiftCareOperationTimes =
                            listOf(fullDay, fullDay, fullDay, fullDay, fullDay, fullDay, fullDay),
                        shiftCareOpenOnHolidays = true,
                    )
                )
            }
        val normalChild = db.transaction { tx -> insertChild(tx, daycareId, ShiftCareType.NONE) }
        val shiftCareChild = db.transaction { tx -> insertChild(tx, daycareId, ShiftCareType.FULL) }

        val result =
            db.transaction {
                it.getOperationalDatesForChildren(month, setOf(normalChild, shiftCareChild))
            }

        // 20 mon-fri days during placement range minus one holiday
        assertEquals(19, result[normalChild]!!.size)

        // 28 mon-sun days during placement range
        assertEquals(28, result[shiftCareChild]!!.size)
    }

    private fun insertChild(
        tx: Database.Transaction,
        daycareId: DaycareId,
        shiftCare: ShiftCareType,
    ): ChildId =
        tx.insert(DevPerson(), DevPersonType.CHILD).also { childId ->
            tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = daycareId,
                        startDate = placementRange.start,
                        endDate = placementRange.end,
                    )
                )
                .also { placementId ->
                    tx.insert(
                        DevServiceNeed(
                            placementId = placementId,
                            startDate = placementRange.start,
                            endDate = placementRange.end,
                            optionId = snDaycareFullDay35.id,
                            confirmedBy = AuthenticatedUser.SystemInternalUser.evakaUserId,
                            shiftCare = shiftCare,
                        )
                    )
                }
        }
}

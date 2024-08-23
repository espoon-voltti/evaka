// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.assistanceneed

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.assistanceneed.vouchercoefficient.endOutdatedAssistanceNeedVoucherCoefficients
import fi.espoo.evaka.shared.dev.DevAssistanceNeedVoucherCoefficient
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class AssistanceNeedVoucerCoefficientEndOutdatedTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val jan1 = LocalDate.of(2024, 1, 1)
    private val dec31 = LocalDate.of(2024, 12, 31)

    private val today = LocalDate.of(2024, 8, 1)
    private val yesterday = today.minusDays(1)

    @Test
    fun `coefficient is not ended if placement continues`() {
        db.transaction { tx ->
            val area = DevCareArea()
            val unit = DevDaycare(areaId = area.id)
            val child = DevPerson()

            tx.insert(area)
            tx.insert(unit)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = unit.id,
                    startDate = jan1,
                    endDate = today,
                )
            )
            tx.insert(
                DevAssistanceNeedVoucherCoefficient(
                    childId = child.id,
                    validityPeriod = FiniteDateRange(jan1, dec31),
                )
            )
        }
        assertEquals(listOf(FiniteDateRange(jan1, dec31)), getCoefficientRanges())

        db.transaction { tx -> tx.endOutdatedAssistanceNeedVoucherCoefficients(today) }

        assertEquals(listOf(FiniteDateRange(jan1, dec31)), getCoefficientRanges())
    }

    @Test
    fun `coefficient is not ended if placement changed to same unit`() {
        db.transaction { tx ->
            val area = DevCareArea()
            val unit = DevDaycare(areaId = area.id)
            val child = DevPerson()

            tx.insert(area)
            tx.insert(unit)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = unit.id,
                    startDate = jan1,
                    endDate = yesterday,
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = unit.id,
                    startDate = today,
                    endDate = dec31,
                )
            )
            tx.insert(
                DevAssistanceNeedVoucherCoefficient(
                    childId = child.id,
                    validityPeriod = FiniteDateRange(jan1, dec31),
                )
            )
        }
        assertEquals(listOf(FiniteDateRange(jan1, dec31)), getCoefficientRanges())

        db.transaction { tx -> tx.endOutdatedAssistanceNeedVoucherCoefficients(today) }

        assertEquals(listOf(FiniteDateRange(jan1, dec31)), getCoefficientRanges())
    }

    @Test
    fun `coefficient is ended if placement has ended`() {
        db.transaction { tx ->
            val area = DevCareArea()
            val unit = DevDaycare(areaId = area.id)
            val child = DevPerson()

            tx.insert(area)
            tx.insert(unit)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = unit.id,
                    startDate = jan1,
                    endDate = yesterday,
                )
            )
            tx.insert(
                DevAssistanceNeedVoucherCoefficient(
                    childId = child.id,
                    validityPeriod = FiniteDateRange(jan1, dec31),
                )
            )
        }
        assertEquals(listOf(FiniteDateRange(jan1, dec31)), getCoefficientRanges())

        db.transaction { tx -> tx.endOutdatedAssistanceNeedVoucherCoefficients(today) }

        assertEquals(listOf(FiniteDateRange(jan1, yesterday)), getCoefficientRanges())
    }

    @Test
    fun `coefficient is ended if placement has changed to a different unit`() {
        db.transaction { tx ->
            val area = DevCareArea()
            val unit = DevDaycare(areaId = area.id)
            val unit2 = DevDaycare(areaId = area.id)
            val child = DevPerson()

            tx.insert(area)
            tx.insert(unit)
            tx.insert(unit2)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = unit.id,
                    startDate = jan1,
                    endDate = yesterday,
                )
            )
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = unit2.id,
                    startDate = today,
                    endDate = dec31,
                )
            )
            tx.insert(
                DevAssistanceNeedVoucherCoefficient(
                    childId = child.id,
                    validityPeriod = FiniteDateRange(jan1, dec31),
                )
            )
        }
        assertEquals(listOf(FiniteDateRange(jan1, dec31)), getCoefficientRanges())

        db.transaction { tx -> tx.endOutdatedAssistanceNeedVoucherCoefficients(today) }

        assertEquals(listOf(FiniteDateRange(jan1, yesterday)), getCoefficientRanges())
    }

    @Test
    fun `past data is not affected`() {
        db.transaction { tx ->
            val area = DevCareArea()
            val unit = DevDaycare(areaId = area.id)
            val child = DevPerson()

            tx.insert(area)
            tx.insert(unit)
            tx.insert(child, DevPersonType.CHILD)
            tx.insert(
                DevPlacement(
                    childId = child.id,
                    unitId = unit.id,
                    startDate = jan1,
                    // Placement already ended 2 days ago
                    endDate = today.minusDays(2),
                )
            )
            tx.insert(
                DevAssistanceNeedVoucherCoefficient(
                    childId = child.id,
                    validityPeriod = FiniteDateRange(jan1, dec31),
                )
            )
        }
        assertEquals(listOf(FiniteDateRange(jan1, dec31)), getCoefficientRanges())

        db.transaction { tx -> tx.endOutdatedAssistanceNeedVoucherCoefficients(today) }

        assertEquals(listOf(FiniteDateRange(jan1, dec31)), getCoefficientRanges())
    }

    private fun getCoefficientRanges() =
        db.read {
            it.createQuery {
                    sql(
                        "SELECT validity_period FROM assistance_need_voucher_coefficient ORDER BY validity_period"
                    )
                }
                .toList<FiniteDateRange>()
        }
}

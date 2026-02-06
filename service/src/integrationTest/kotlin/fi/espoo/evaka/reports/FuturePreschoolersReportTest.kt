// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.PureJdbiTest
import fi.espoo.evaka.daycare.CareType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevDaycareGroup
import fi.espoo.evaka.shared.dev.DevGuardian
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import java.time.LocalDate
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FuturePreschoolersReportTest : PureJdbiTest(resetDbBeforeEach = true) {
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id, openingDate = LocalDate.of(2000, 1, 1))
    private val voucherDaycare =
        DevDaycare(
            areaId = area.id,
            name = "Voucher Daycare",
            openingDate = LocalDate.of(2000, 1, 1),
            providerType = ProviderType.PRIVATE_SERVICE_VOUCHER,
        )
    private val roundTheClockDaycare =
        DevDaycare(
            areaId = area.id,
            name = "Round The Clock Daycare",
            openingDate = LocalDate.of(2000, 1, 1),
            type = setOf(CareType.CENTRE),
        )

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(voucherDaycare)
            tx.insert(roundTheClockDaycare)
            tx.insert(DevDaycareGroup(daycareId = daycare.id, name = "Test group 1"))
            tx.insert(DevDaycareGroup(daycareId = voucherDaycare.id, name = "Test voucher group 1"))
        }
    }

    @Test
    fun `children with correct age are found in report`() {
        val adult1 = DevPerson()
        val adult2 = DevPerson()
        // Names document intent: "Sopiva" = right age, "Nuori" = too young, "Vanha" = too old
        val children =
            listOf(
                DevPerson(
                    dateOfBirth = LocalDate.of(2018, 6, 1),
                    firstName = "Just",
                    lastName = "Sopiva",
                ),
                DevPerson(
                    dateOfBirth = LocalDate.of(2019, 7, 1),
                    firstName = "Turhan",
                    lastName = "Nuori",
                ),
                DevPerson(
                    dateOfBirth = LocalDate.of(2017, 8, 1),
                    firstName = "Liian",
                    lastName = "Vanha",
                ),
            )
        db.transaction { tx ->
            tx.insert(adult1, DevPersonType.RAW_ROW)
            tx.insert(adult2, DevPersonType.RAW_ROW)
            children.forEachIndexed { i, child ->
                tx.insert(child, DevPersonType.CHILD)
                tx.insert(
                    DevPlacement(childId = child.id, unitId = daycare.id, endDate = LocalDate.now())
                )
                tx.insert(DevGuardian(guardianId = adult1.id, childId = child.id))
                if (i % 2 == 1) tx.insert(DevGuardian(guardianId = adult2.id, childId = child.id))
            }
        }

        val report = getChildrenReport()
        assertEquals(1, report.size)
    }

    @Test
    fun `open preschool units are found in report`() {
        val report = getUnitReport()
        assertEquals(2, report.size)
    }

    @Test
    fun `open source are found in report`() {
        val report = getSourceUnitReport()
        assertEquals(3, report.size)
    }

    private fun getChildrenReport(): List<FuturePreschoolersReportRow> {
        return db.read { it.getFuturePreschoolerRows(LocalDate.of(2023, 1, 1)) }
    }

    private fun getUnitReport(): List<PreschoolUnitsReportRow> {
        return db.read { it.getPreschoolUnitsRows(LocalDate.of(2023, 1, 1)) }
    }

    private fun getSourceUnitReport(): List<SourceUnitsReportRow> {
        return db.read { it.getSourceUnitsRows(LocalDate.of(2023, 1, 1)) }
    }
}

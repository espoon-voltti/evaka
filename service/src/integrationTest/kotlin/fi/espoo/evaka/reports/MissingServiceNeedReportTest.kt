// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ServiceNeedOption
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDefaultDaycare
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MissingServiceNeedReportTest : FullApplicationTest(resetDbBeforeEach = true) {
    @Autowired
    private lateinit var missingServiceNeedReportController: MissingServiceNeedReportController
    val today: LocalDate = LocalDate.now()
    private val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
    private val area = DevCareArea()
    private val daycare = DevDaycare(areaId = area.id)
    private val voucherDaycare =
        DevDaycare(areaId = area.id, providerType = ProviderType.PRIVATE_SERVICE_VOUCHER)
    private val child = DevPerson()

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(area)
            tx.insert(daycare)
            tx.insert(voucherDaycare)
            tx.insert(child, DevPersonType.CHILD)
            tx.insertServiceNeedOptions()
        }
    }

    @Test
    fun `child without service need is reported`() {
        insertPlacement(child.id, today, today, daycare)
        val rows =
            missingServiceNeedReportController.getMissingServiceNeedReport(
                dbInstance(),
                admin.user,
                RealEvakaClock(),
                today,
                today,
            )
        assertThat(rows)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("defaultOption.updated")
            .isEqualTo(
                listOf(
                    MissingServiceNeedReportResultRow(
                        careAreaName = area.name,
                        childId = child.id,
                        unitId = daycare.id,
                        unitName = daycare.name,
                        daysWithoutServiceNeed = 1,
                        firstName = child.firstName,
                        lastName = child.lastName,
                        defaultOption = ServiceNeedOption.fromFullServiceNeed(snDefaultDaycare),
                    )
                )
            )
    }

    @Test
    fun `child without service need in service voucher unit shown on service voucher care area`() {
        insertPlacement(child.id, today, today, voucherDaycare)
        val rows =
            missingServiceNeedReportController.getMissingServiceNeedReport(
                dbInstance(),
                admin.user,
                RealEvakaClock(),
                today,
                today,
            )
        assertThat(rows)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("defaultOption.updated")
            .isEqualTo(
                listOf(
                    MissingServiceNeedReportResultRow(
                        careAreaName = "palvelusetelialue",
                        childId = child.id,
                        unitId = voucherDaycare.id,
                        unitName = voucherDaycare.name,
                        daysWithoutServiceNeed = 1,
                        firstName = child.firstName,
                        lastName = child.lastName,
                        defaultOption = ServiceNeedOption.fromFullServiceNeed(snDefaultDaycare),
                    )
                )
            )
    }

    private fun insertPlacement(
        childId: ChildId,
        startDate: LocalDate,
        endDate: LocalDate = startDate.plusYears(1),
        daycare: DevDaycare,
    ) =
        db.transaction { tx ->
            tx.insert(
                DevPlacement(
                    childId = childId,
                    unitId = daycare.id,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
        }
}

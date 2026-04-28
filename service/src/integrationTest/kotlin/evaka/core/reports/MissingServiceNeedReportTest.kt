// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reports

import evaka.core.FullApplicationTest
import evaka.core.application.ServiceNeedOption
import evaka.core.daycare.domain.ProviderType
import evaka.core.insertServiceNeedOptions
import evaka.core.shared.ChildId
import evaka.core.shared.auth.UserRole
import evaka.core.shared.dev.DevCareArea
import evaka.core.shared.dev.DevDaycare
import evaka.core.shared.dev.DevEmployee
import evaka.core.shared.dev.DevPerson
import evaka.core.shared.dev.DevPersonType
import evaka.core.shared.dev.DevPlacement
import evaka.core.shared.dev.insert
import evaka.core.shared.domain.RealEvakaClock
import evaka.core.snDefaultDaycare
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
    ) = db.transaction { tx ->
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

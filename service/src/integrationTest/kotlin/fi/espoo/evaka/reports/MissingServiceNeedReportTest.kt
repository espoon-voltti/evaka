// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.application.ServiceNeedOption
import fi.espoo.evaka.insertServiceNeedOptions
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.domain.RealEvakaClock
import fi.espoo.evaka.snDefaultDaycare
import fi.espoo.evaka.testArea
import fi.espoo.evaka.testChild_1
import fi.espoo.evaka.testDaycare
import fi.espoo.evaka.testVoucherDaycare
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

    @BeforeEach
    fun beforeEach() {
        db.transaction { tx ->
            tx.insert(admin)
            tx.insert(testArea)
            tx.insert(testDaycare)
            tx.insert(testVoucherDaycare)
            tx.insert(testChild_1, DevPersonType.CHILD)
            tx.insertServiceNeedOptions()
        }
    }

    @Test
    fun `child without service need is reported`() {
        insertPlacement(testChild_1.id, today, today, testDaycare)
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
                        careAreaName = testArea.name,
                        childId = testChild_1.id,
                        unitId = testDaycare.id,
                        unitName = testDaycare.name,
                        daysWithoutServiceNeed = 1,
                        firstName = testChild_1.firstName,
                        lastName = testChild_1.lastName,
                        defaultOption = ServiceNeedOption.fromFullServiceNeed(snDefaultDaycare),
                    )
                )
            )
    }

    @Test
    fun `child without service need in service voucher unit shown on service voucher care area`() {
        insertPlacement(testChild_1.id, today, today, testVoucherDaycare)
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
                        childId = testChild_1.id,
                        unitId = testVoucherDaycare.id,
                        unitName = testVoucherDaycare.name,
                        daysWithoutServiceNeed = 1,
                        firstName = testChild_1.firstName,
                        lastName = testChild_1.lastName,
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

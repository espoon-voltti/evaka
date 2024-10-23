// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reports

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.shared.auth.UserRole
import fi.espoo.evaka.shared.dev.DevCareArea
import fi.espoo.evaka.shared.dev.DevDaycare
import fi.espoo.evaka.shared.dev.DevEmployee
import fi.espoo.evaka.shared.dev.DevPerson
import fi.espoo.evaka.shared.dev.DevPersonType
import fi.espoo.evaka.shared.dev.DevPlacement
import fi.espoo.evaka.shared.dev.DevServiceNeed
import fi.espoo.evaka.shared.dev.insert
import fi.espoo.evaka.shared.dev.insertServiceNeedOption
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.MockEvakaClock
import fi.espoo.evaka.snDaycareFullDay35
import fi.espoo.evaka.snDefaultDaycare
import java.time.LocalDate
import java.time.LocalTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RawReportControllerTest : FullApplicationTest(resetDbBeforeEach = true) {

    @Autowired private lateinit var rawReportController: RawReportController

    @Test
    fun `service need works`() {
        val user =
            db.transaction { tx ->
                val admin = DevEmployee(roles = setOf(UserRole.ADMIN))
                tx.insert(admin)
                admin.user
            }
        val clock =
            MockEvakaClock(HelsinkiDateTime.of(LocalDate.of(2024, 10, 23), LocalTime.of(8, 47)))
        db.transaction { tx ->
            val areaId = tx.insert(DevCareArea())
            val unitId = tx.insert(DevDaycare(areaId = areaId))
            tx.insertServiceNeedOption(snDefaultDaycare)
            tx.insertServiceNeedOption(snDaycareFullDay35)

            tx.insert(DevPerson(firstName = "Eka"), DevPersonType.CHILD).also { childId ->
                val placementId =
                    tx.insert(
                        DevPlacement(
                            childId = childId,
                            unitId = unitId,
                            startDate = clock.today(),
                            endDate = clock.today(),
                        )
                    )
                tx.insert(
                    DevServiceNeed(
                        placementId = placementId,
                        optionId = snDaycareFullDay35.id,
                        startDate = clock.today(),
                        endDate = clock.today(),
                        confirmedBy = user.evakaUserId,
                    )
                )
            }

            tx.insert(DevPerson(firstName = "Toka"), DevPersonType.CHILD).also { childId ->
                tx.insert(
                    DevPlacement(
                        childId = childId,
                        unitId = unitId,
                        startDate = clock.today(),
                        endDate = clock.today(),
                    )
                )
            }
        }

        val rows =
            rawReportController.getRawReport(
                dbInstance(),
                user,
                clock,
                clock.today(),
                clock.today(),
            )

        assertThat(rows)
            .extracting({ it.firstName }, { it.serviceNeed })
            .containsExactlyInAnyOrder(Tuple("Eka", snDaycareFullDay35.nameFi), Tuple("Toka", null))
    }
}

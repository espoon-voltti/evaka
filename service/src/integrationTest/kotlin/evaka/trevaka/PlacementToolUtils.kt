// SPDX-FileCopyrightText: 2023-2025 Tampere region
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.trevaka

import evaka.core.EvakaEnv
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.findServiceNeedOptionById
import evaka.core.shared.db.Database
import evaka.core.shared.domain.DateRange
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertTrue
import org.assertj.core.api.Assertions.assertThat

fun assertPlacementToolServiceNeedOptionIdExists(
    db: Database.Connection,
    evakaEnv: EvakaEnv,
    nameFi: String,
) {
    val serviceNeedOptionId =
        evakaEnv.placementToolServiceNeedOptionId
            ?: error("Service need option id is not configured")
    val serviceNeedOption =
        db.read { tx -> tx.findServiceNeedOptionById(serviceNeedOptionId) }
            ?: error("Service need option id $serviceNeedOptionId not found")

    assertThat(serviceNeedOption)
        .returns(PlacementType.PRESCHOOL_DAYCARE) { it.validPlacementType }
        .returns(false) { it.defaultOption }
        .returns(nameFi) { it.nameFi }

    val nextFirstOfAugust =
        LocalDate.now().withDayOfMonth(1).let {
            (if (it.month >= Month.AUGUST) it.plusYears(1) else it).withMonth(8)
        }
    assertTrue(
        DateRange(serviceNeedOption.validFrom, serviceNeedOption.validTo)
            .includes(nextFirstOfAugust)
    )
}

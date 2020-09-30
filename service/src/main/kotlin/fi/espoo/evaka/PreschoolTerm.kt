// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.shared.domain.ClosedPeriod
import java.time.LocalDate
import java.time.Month

// TODO: hard-coding is not a good solution
val preschoolTerm2019 = ClosedPeriod(LocalDate.of(2019, 8, 8), LocalDate.of(2020, 5, 29))
val preschoolTerm2020 = ClosedPeriod(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4))

fun derivePreschoolTerm(placementStartDate: LocalDate): ClosedPeriod {
    if (placementStartDate > preschoolTerm2019.end && placementStartDate <= preschoolTerm2020.end) {
        return preschoolTerm2020
    } else if (placementStartDate >= LocalDate.of(2019, 7, 31) && placementStartDate <= preschoolTerm2019.end) {
        return preschoolTerm2019
    } else {
        // educated guess for placements in the past and in the far future
        return if (placementStartDate.month >= Month.AUGUST) {
            ClosedPeriod(
                LocalDate.of(placementStartDate.year, 8, 1),
                LocalDate.of(placementStartDate.year + 1, 5, 31)
            )
        } else {
            ClosedPeriod(
                LocalDate.of(placementStartDate.year - 1, 8, 1),
                LocalDate.of(placementStartDate.year, 5, 31)
            )
        }
    }
}

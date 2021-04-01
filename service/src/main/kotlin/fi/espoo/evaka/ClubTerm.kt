// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka

import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

// TODO: hard-coding is not a good solution
val clubTerm2020 = FiniteDateRange(LocalDate.of(2020, 8, 13), LocalDate.of(2021, 6, 4))
val clubTerm2021 = FiniteDateRange(LocalDate.of(2021, 8, 11), LocalDate.of(2022, 6, 3))

fun deriveClubTerm(placementStartDate: LocalDate): FiniteDateRange {
    if (clubTerm2020.includes(placementStartDate)) {
        return clubTerm2020
    }
    if (clubTerm2021.includes(placementStartDate)) {
        return clubTerm2021
    }
    throw IllegalStateException("No club term found for start date of $placementStartDate")
}

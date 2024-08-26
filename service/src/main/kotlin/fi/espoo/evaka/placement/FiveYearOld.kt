// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.placement

import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

fun resolveFiveYearOldPlacementPeriods(
    tx: Database.Read,
    childId: ChildId,
    placements: List<Pair<FiniteDateRange, PlacementType>>,
): List<Pair<FiniteDateRange, PlacementType>> {
    val child = tx.getPersonById(childId) ?: error("Child not found ($childId)")
    val fiveYearOldTermStart = LocalDate.of(child.dateOfBirth.plusYears(5).year, 8, 1)
    val fiveYearOldTermEnd = fiveYearOldTermStart.plusYears(1).minusDays(1)

    return placements
        .map { (period, type) ->
            val (normalPlacementType, fiveYearOldPlacementType) =
                when (type) {
                    PlacementType.DAYCARE,
                    PlacementType.DAYCARE_FIVE_YEAR_OLDS ->
                        PlacementType.DAYCARE to PlacementType.DAYCARE_FIVE_YEAR_OLDS
                    PlacementType.DAYCARE_PART_TIME,
                    PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS ->
                        PlacementType.DAYCARE_PART_TIME to
                            PlacementType.DAYCARE_PART_TIME_FIVE_YEAR_OLDS
                    else -> return@map listOf(period to type)
                }

            listOfNotNull(
                if (period.start < fiveYearOldTermStart) {
                    FiniteDateRange(
                        period.start,
                        minOf(fiveYearOldTermStart.minusDays(1), period.end),
                    ) to normalPlacementType
                } else {
                    null
                },
                if (fiveYearOldTermStart <= period.end && period.start <= fiveYearOldTermEnd) {
                    FiniteDateRange(
                        maxOf(fiveYearOldTermStart, period.start),
                        minOf(fiveYearOldTermEnd, period.end),
                    ) to fiveYearOldPlacementType
                } else {
                    null
                },
                if (fiveYearOldTermEnd < period.end) {
                    FiniteDateRange(
                        maxOf(fiveYearOldTermEnd.plusDays(1), period.start),
                        period.end,
                    ) to normalPlacementType
                } else {
                    null
                },
            )
        }
        .flatten()
}

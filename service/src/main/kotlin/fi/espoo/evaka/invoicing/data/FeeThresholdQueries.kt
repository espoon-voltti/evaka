// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.data

import fi.espoo.evaka.invoicing.domain.FeeThresholds
import fi.espoo.evaka.shared.db.Database
import java.time.LocalDate

fun Database.Read.getFeeThresholds(from: LocalDate? = null): List<FeeThresholds> {
    return createQuery {
            sql(
                """
SELECT
    valid_during,
    min_income_threshold_2,
    min_income_threshold_3,
    min_income_threshold_4,
    min_income_threshold_5,
    min_income_threshold_6,
    income_multiplier_2,
    income_multiplier_3,
    income_multiplier_4,
    income_multiplier_5,
    income_multiplier_6,
    max_income_threshold_2,
    max_income_threshold_3,
    max_income_threshold_4,
    max_income_threshold_5,
    max_income_threshold_6,
    income_threshold_increase_6_plus,
    sibling_discount_2,
    sibling_discount_2_plus,
    max_fee,
    min_fee,
    temporary_fee,
    temporary_fee_part_day,
    temporary_fee_sibling,
    temporary_fee_sibling_part_day
FROM fee_thresholds
WHERE valid_during && daterange(${bind(from)}, null)
"""
            )
        }
        .toList<FeeThresholds>()
}

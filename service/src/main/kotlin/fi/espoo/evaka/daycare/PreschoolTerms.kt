// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

data class PreschoolTerm(
    /*The period during which finnish speaking preschool is arranged.*/
    val finnishPreschool: FiniteDateRange,
    /*The period during which swedish speaking preschool is arranged.*/
    val swedishPreschool: FiniteDateRange,
    /*Children going to preschool may apply for connected daycare slightly outside the actual preschool term,
    usually starting few weeks earlier. That is then essentially normal daycare with discounted price.
    This period defines when that connected daycare is available.*/
    val extendedTerm: FiniteDateRange,
    /*The official application period. The end date is not enforced though, but applications are accepted
    until end of term.*/
    val applicationPeriod: FiniteDateRange
) {
    fun isApplicationAccepted(date: LocalDate) =
        FiniteDateRange(applicationPeriod.start, extendedTerm.end).includes(date)
}

fun Database.Read.getPreschoolTerms(): List<PreschoolTerm> {
    return createQuery(
            """
        SELECT
            finnish_preschool,
            swedish_preschool,
            extended_term,
            application_period
        FROM preschool_term
        ORDER BY extended_term
        """.trimIndent(
            )
        )
        .mapTo<PreschoolTerm>()
        .list()
}

fun Database.Read.getActivePreschoolTermAt(date: LocalDate): PreschoolTerm? {
    return getPreschoolTerms().firstOrNull { it.extendedTerm.includes(date) }
}

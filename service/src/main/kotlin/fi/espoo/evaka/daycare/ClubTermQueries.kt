// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import org.jdbi.v3.core.kotlin.mapTo
import java.time.LocalDate

data class ClubTerm(
    /*The period during which club activity is arranged.*/
    val term: FiniteDateRange,
    /*The official application period.*/
    val applicationPeriod: FiniteDateRange
)

fun Database.Read.getClubTerms(): List<ClubTerm> {
    return createQuery("SELECT term, application_period FROM club_term order by term")
        .mapTo<ClubTerm>()
        .list()
}

fun Database.Read.getActiveClubTermAt(date: LocalDate): ClubTerm? {
    return createQuery("SELECT term, application_period FROM club_term WHERE term @> :date LIMIT 1")
        .bind("date", date)
        .mapTo<ClubTerm>().firstOrNull()
}

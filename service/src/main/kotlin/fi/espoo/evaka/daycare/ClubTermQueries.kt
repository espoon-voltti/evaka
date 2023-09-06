// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

data class ClubTerm(
    /*The period during which club activity is arranged.*/
    val term: FiniteDateRange,
    /*The official application period.*/
    val applicationPeriod: FiniteDateRange,
    /*Club is not arranged during these periods (e.g. Christmas holiday).*/
    val termBreaks: DateSet
) {
    fun scheduleType(date: LocalDate): ScheduleType? =
        when {
            term.includes(date) -> {
                if (termBreaks.includes(date)) ScheduleType.TERM_BREAK
                else ScheduleType.FIXED_SCHEDULE
            }
            else -> null
        }
}

fun Database.Read.getClubTerms(): List<ClubTerm> {
    return createQuery("SELECT term, application_period, term_breaks FROM club_term order by term")
        .mapTo<ClubTerm>()
        .list()
}

fun Database.Read.getActiveClubTermAt(date: LocalDate): ClubTerm? {
    return createQuery(
            "SELECT term, application_period, term_breaks FROM club_term WHERE term @> :date LIMIT 1"
        )
        .bind("date", date)
        .mapTo<ClubTerm>()
        .firstOrNull()
}

fun Database.Transaction.insertClubTerm(term: ClubTerm) {
    createUpdate(
            """
        INSERT INTO club_term (term, application_period, term_breaks)
        VALUES (:term, :applicationPeriod, :termBreaks)
        """
        )
        .bind("term", term.term)
        .bind("applicationPeriod", term.applicationPeriod)
        .bind("termBreaks", term.termBreaks)
        .execute()
}

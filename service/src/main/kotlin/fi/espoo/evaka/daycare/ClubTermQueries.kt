// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.daycare

import fi.espoo.evaka.placement.ScheduleType
import fi.espoo.evaka.shared.ClubTermId
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate

data class ClubTerm(
    val id: ClubTermId,
    /*The period during which club activity is arranged.*/
    val term: FiniteDateRange,
    /*The official application period.*/
    val applicationPeriod: FiniteDateRange,
    /*Club is not arranged during these periods (e.g. Christmas holiday).*/
    val termBreaks: DateSet,
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
    return createQuery {
            sql(
                """
               SELECT id, term, application_period, term_breaks FROM club_term order by term
           """
            )
        }
        .toList()
}

fun Database.Read.getClubTerm(id: ClubTermId): ClubTerm? =
    createQuery {
            sql(
                """
               SELECT id, term, application_period, term_breaks FROM club_term WHERE id = ${bind(id)}
            """
            )
        }
        .exactlyOneOrNull()

fun Database.Read.getActiveClubTermAt(date: LocalDate): ClubTerm? =
    createQuery {
            sql(
                "SELECT id, term, application_period, term_breaks FROM club_term WHERE term @> ${bind(date)} LIMIT 1"
            )
        }
        .exactlyOneOrNull()

fun Database.Transaction.insertClubTerm(
    term: FiniteDateRange,
    applicationPeriod: FiniteDateRange,
    termBreaks: DateSet,
): ClubTermId {
    return createUpdate {
            sql(
                """
        INSERT INTO club_term (term, application_period, term_breaks)
        VALUES (${bind(term)}, ${bind(applicationPeriod)}, ${bind(termBreaks)})
        RETURNING id
        """
            )
        }
        .executeAndReturnGeneratedKeys()
        .exactlyOne()
}

fun Database.Transaction.updateClubTerm(
    id: ClubTermId,
    term: FiniteDateRange,
    applicationPeriod: FiniteDateRange,
    termBreaks: DateSet,
) =
    createUpdate {
            sql(
                """
            UPDATE club_term 
            SET 
                term = ${bind(term)},
                application_period = ${bind(applicationPeriod)},
                term_breaks =  ${bind(termBreaks)}
            WHERE id = ${bind(id)}
            """
            )
        }
        .updateExactlyOne()

fun Database.Transaction.deleteFutureClubTerm(clock: EvakaClock, termId: ClubTermId) {
    createUpdate {
            sql(
                """
           DELETE FROM club_term
           WHERE id = ${bind(termId)} AND lower(term) > ${bind(clock.today())}
        """
            )
        }
        .updateExactlyOne()
}

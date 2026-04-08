// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.daycare

import evaka.core.placement.ScheduleType
import evaka.core.shared.ClubTermId
import evaka.core.shared.data.DateSet
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.toFiniteDateRange
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

            else -> {
                null
            }
        }
}

fun Database.Read.getClubTerms(range: FiniteDateRange?): List<ClubTerm> {
    return createQuery {
            sql(
                """
    SELECT id, term, application_period, term_breaks 
    FROM club_term
    ${if (range != null) "WHERE term && ${bind(range)}" else ""}
    ORDER BY term
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

fun Database.Read.getClubTerm(date: LocalDate): ClubTerm? =
    getClubTerms(range = date.toFiniteDateRange()).firstOrNull()

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

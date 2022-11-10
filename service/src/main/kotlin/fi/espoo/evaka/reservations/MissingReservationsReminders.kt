// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.QuerySql
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.DayOfWeek
import org.springframework.stereotype.Service

@Service
class MissingReservationsReminders(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: IEmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv
) {
    init {
        asyncJobRunner.registerHandler { db, _, msg: AsyncJob.SendMissingReservationsReminder ->
            sendReminder(db, msg)
        }
    }

    fun scheduleReminders(tx: Database.Transaction, clock: EvakaClock): Int {
        tx.removeUnclaimedJobs(setOf(AsyncJobType(AsyncJob.SendMissingReservationsReminder::class)))

        val range =
            clock.today().plusDays(8).with(DayOfWeek.MONDAY).let { start ->
                FiniteDateRange(start, start.plusDays(7))
            }
        val guardians =
            tx.createQuery<Any> {
                    sql(
                        """
SELECT DISTINCT missing.guardian_id
FROM (${subquery(missingReservationsQuery(range))}) missing
JOIN person p ON missing.guardian_id = p.id
WHERE p.email IS NOT NULL
    """
                            .trimIndent()
                    )
                }
                .mapTo<PersonId>()
                .toList()

        asyncJobRunner.plan(
            tx,
            payloads = guardians.map { AsyncJob.SendMissingReservationsReminder(it, range) },
            runAt = clock.now()
        )
        return guardians.size
    }

    fun sendReminder(db: Database.Connection, msg: AsyncJob.SendMissingReservationsReminder) {
        val (recipient, language) =
            db.read { tx ->
                tx.createQuery<DatabaseTable> {
                        sql(
                            """
SELECT email, language
FROM (${subquery(missingReservationsQuery(msg.range))}) missing
JOIN person p ON missing.guardian_id = p.id
WHERE missing.guardian_id = ${bind(msg.guardian)}
AND email IS NOT NULL
        """
                                .trimIndent()
                        )
                    }
                    .map { row ->
                        Pair(
                            row.mapColumn<String>("email"),
                            row.mapColumn<String?>("language")
                                ?.lowercase()
                                ?.let(Language::tryValueOf)
                                ?: Language.fi
                        )
                    }
                    .firstOrNull()
            }
                ?: return
        val email = emailMessageProvider.missingReservationsNotification(language, msg.range)
        emailClient.sendEmail(
            traceId = msg.guardian.toString(),
            toAddress = recipient,
            fromAddress = emailEnv.sender(language),
            subject = email.subject,
            htmlBody = email.html,
            textBody = email.text,
        )
    }
}

private fun missingReservationsQuery(range: FiniteDateRange) =
    QuerySql.of<PersonId> {
        sql(
            """
SELECT guardian_id
FROM (
    SELECT p.child_id, t::date AS date
    FROM generate_series(${bind(range.start)}, ${bind(range.end)}, '1 day') t
    JOIN placement p ON daterange(p.start_date, p.end_date, '[]') @> t::date
    JOIN daycare d ON p.unit_id = d.id
    LEFT JOIN holiday h ON t::date = h.date AND NOT d.operation_days @> ARRAY[1, 2, 3, 4, 5, 6, 7]
    WHERE date_part('isodow', t::date) = ANY(d.operation_days)
    AND h.date IS NULL
    AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
    AND NOT EXISTS (
        SELECT 1
        FROM attendance_reservation ar
        WHERE ar.child_id = p.child_id AND ar.date = t::date
    )
    AND NOT EXISTS (
        SELECT 1
        FROM child_attendance a
        WHERE a.child_id = p.child_id AND a.date = t::date
    )
    AND NOT EXISTS (
        SELECT 1
        FROM absence a
        WHERE a.child_id = p.child_id AND a.date = t::date
    )
) missing
JOIN LATERAL (
    SELECT guardian_id
    FROM guardian
    WHERE missing.child_id = guardian.child_id
    
    UNION ALL
    
    SELECT parent_id AS guardian_id
    FROM foster_parent
    WHERE missing.child_id = foster_parent.child_id AND valid_during @> missing.date
) guardian ON TRUE
    """
                .trimIndent()
        )
    }

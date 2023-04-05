// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.FeatureConfig
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
import org.springframework.stereotype.Service

@Service
class MissingReservationsReminders(
    private val featureConfig: FeatureConfig,
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

        val monday =
            getNextReservableMonday(clock.now(), featureConfig.citizenReservationThresholdHours)
        val range = FiniteDateRange(monday, monday.plusDays(6))

        val guardians =
            tx.createQuery<Any> {
                    sql(
                        """
SELECT DISTINCT missing.guardian_id
FROM (${subquery(missingReservationsQuery(range, guardian = null))}) missing
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
FROM (${subquery(missingReservationsQuery(msg.range, msg.guardian))}) missing
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
        emailClient.sendEmail(
            traceId = msg.guardian.toString(),
            toAddress = recipient,
            fromAddress = emailEnv.sender(language),
            content = emailMessageProvider.missingReservationsNotification(language, msg.range)
        )
    }
}

private fun missingReservationsQuery(range: FiniteDateRange, guardian: PersonId?) =
    QuerySql.of<PersonId> {
        sql(
            """
SELECT guardian_id
FROM (
    SELECT p.child_id, t::date AS date
    FROM generate_series(${bind(range.start)}, ${bind(range.end)}, '1 day') t
    JOIN placement p ON daterange(p.start_date, p.end_date, '[]') @> t::date
    JOIN daycare d ON p.unit_id = d.id
    JOIN service_need sn ON p.id = sn.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> t::date
    LEFT JOIN holiday h ON t::date = h.date AND NOT d.operation_days @> ARRAY[1, 2, 3, 4, 5, 6, 7]
    WHERE date_part('isodow', t::date) = ANY(d.operation_days)
    AND h.date IS NULL
    AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
    AND NOT EXISTS (
        SELECT 1
        FROM attendance_reservation ar
        WHERE
            ar.child_id = p.child_id AND
            ar.date = t::date AND
            ar.start_time IS NOT NULL AND
            ar.end_time IS NOT NULL
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
JOIN (
    SELECT guardian_id, child_id, NULL AS valid_during
    FROM guardian
    ${if (guardian == null) "" else "WHERE guardian_id = ${bind(guardian)}"}

    UNION ALL

    SELECT parent_id AS guardian_id, child_id, valid_during
    FROM foster_parent
    ${if (guardian == null) "" else "WHERE parent_id = ${bind(guardian)}"}
) guardian
ON missing.child_id = guardian.child_id
AND (valid_during IS NULL OR valid_during @> missing.date)
"""
        )
    }

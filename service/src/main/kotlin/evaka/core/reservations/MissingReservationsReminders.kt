// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.reservations

import evaka.core.EmailEnv
import evaka.core.daycare.domain.Language
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.pis.EmailMessageType
import evaka.core.placement.PlacementType
import evaka.core.shared.FeatureConfig
import evaka.core.shared.PersonId
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.db.QuerySql
import evaka.core.shared.domain.EvakaClock
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.getHolidays
import org.springframework.stereotype.Service

@Service
class MissingReservationsReminders(
    private val featureConfig: FeatureConfig,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
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
            tx.createQuery {
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
                .toList<PersonId>()

        asyncJobRunner.plan(
            tx,
            payloads = guardians.map { AsyncJob.SendMissingReservationsReminder(it, range) },
            runAt = clock.now(),
        )
        return guardians.size
    }

    fun sendReminder(db: Database.Connection, msg: AsyncJob.SendMissingReservationsReminder) {
        val language =
            db.read { tx ->
                tx.createQuery {
                        sql(
                            """
SELECT language
FROM (${subquery(missingReservationsQuery(msg.range, msg.guardian))}) missing
JOIN person p ON missing.guardian_id = p.id
WHERE missing.guardian_id = ${bind(msg.guardian)}
AND email IS NOT NULL
LIMIT 1
        """
                                .trimIndent()
                        )
                    }
                    .exactlyOneOrNull {
                        column<String?>("language")?.lowercase()?.let(Language::tryValueOf)
                            ?: Language.fi
                    }
            } ?: return

        Email.create(
                dbc = db,
                personId = msg.guardian,
                emailType = EmailMessageType.ATTENDANCE_RESERVATION_NOTIFICATION,
                fromAddress = emailEnv.sender(language),
                content = emailMessageProvider.missingReservationsNotification(language, msg.range),
                traceId = msg.guardian.toString(),
            )
            ?.also { emailClient.send(it) }
    }
}

private fun missingReservationsQuery(range: FiniteDateRange, guardian: PersonId?) = QuerySql {
    val holidays = getHolidays(range)
    sql(
        """
SELECT guardian_id
FROM (
    SELECT p.child_id, t::date AS date
    FROM unnest(${bind(range.dates().toList())}) t
    JOIN placement p ON daterange(p.start_date, p.end_date, '[]') @> t::date
    JOIN daycare d ON p.unit_id = d.id
    LEFT JOIN service_need sn ON sn.placement_id = p.id AND daterange(sn.start_date, sn.end_date, '[]') @> t::date
    WHERE date_part('isodow', t::date) = ANY(
        CASE WHEN sn.shift_care IS NULL OR sn.shift_care = 'NONE' THEN d.operation_days ELSE d.shift_care_operation_days END
    )
        AND p.type = ANY(${bind(PlacementType.requiringAttendanceReservations)})
        AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
        AND ((d.shift_care_open_on_holidays AND sn.shift_care = ANY('{FULL,INTERMITTENT}'::shift_care_type[])) OR t::date != ALL(${bind(holidays)}))
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

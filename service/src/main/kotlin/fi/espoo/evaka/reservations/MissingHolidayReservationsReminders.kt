// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.holidayperiod.getHolidayPeriodsWithReservationDeadline
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.pis.getPersonById
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import java.time.LocalDate
import mu.KotlinLogging
import org.springframework.stereotype.Service

@Service
class MissingHolidayReservationsReminders(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv
) {
    private val logger = KotlinLogging.logger {}

    init {
        asyncJobRunner.registerHandler {
            db,
            clock,
            msg: AsyncJob.SendMissingHolidayReservationsReminder ->
            sendMissingHolidayReminders(db, msg, clock)
        }
    }

    fun scheduleReminders(tx: Database.Transaction, clock: EvakaClock): Int {
        tx.removeUnclaimedJobs(
            setOf(AsyncJobType(AsyncJob.SendMissingHolidayReservationsReminder::class))
        )

        return tx.getHolidayPeriodsWithReservationDeadline(clock.today().plusDays(2))
            .firstOrNull()
            ?.let { holidayPeriod ->
                logger.info(
                    "Holiday ${holidayPeriod.id} reservation deadline is due, sending missing reservation reminders"
                )

                val childrenWithMaybeMissingHolidayReservations =
                    holidayPeriod.period
                        .dates()
                        .map { tx.getChildrenWithWithMissingReservations(it) }
                        .flatten()
                        .toSet()

                val personsToBeNotified =
                    tx.getChildGuardiansToNotify(
                            clock.today(),
                            childrenWithMaybeMissingHolidayReservations.toList()
                        )
                        .toSet()

                logger.info(
                    "Got ${childrenWithMaybeMissingHolidayReservations.size} children with maybe missing holiday reservations and will notify ${personsToBeNotified.size} persons for holiday ${holidayPeriod.period}"
                )

                asyncJobRunner.plan(
                    tx,
                    payloads =
                        personsToBeNotified.map {
                            AsyncJob.SendMissingHolidayReservationsReminder(
                                it,
                                holidayPeriod.period
                            )
                        },
                    runAt = clock.now()
                )
                personsToBeNotified.size
            } ?: 0
    }

    fun Database.Read.getChildrenWithWithMissingReservations(
        theDate: LocalDate,
        personId: PersonId? = null
    ): List<PersonId> =
        createQuery {
                sql(
                    """
WITH reservable_placements AS
     (SELECT p.child_id, p.unit_id
      FROM placement p
      WHERE daterange(p.start_date, p.end_date, '[]') @> ${bind(theDate)}
        AND p.type = ANY
            (${bind(PlacementType.requiringAttendanceReservations)})
     )
SELECT p.child_id
FROM reservable_placements p
     JOIN daycare d ON d.id = p.unit_id 
        AND extract(isodow FROM ${bind(theDate)}) = ANY(d.operation_days) 
        AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
WHERE
    NOT EXISTS (
        SELECT 1
        FROM attendance_reservation ar
        WHERE ar.child_id = p.child_id AND ar.date = ${bind(theDate)}
    )
    AND NOT EXISTS (
        SELECT 1
        FROM absence a
        WHERE a.child_id = p.child_id AND a.date = ${bind(theDate)}
    )
"""
                )
            }
            .mapTo<PersonId>()
            .toList()

    fun Database.Read.getChildGuardiansToNotify(
        today: LocalDate,
        childIds: List<PersonId>
    ): List<PersonId> =
        createQuery {
                sql(
                    """
SELECT DISTINCT(COALESCE(g.guardian_id, fp.parent_id))
FROM person p
LEFT JOIN guardian g ON p.id = g.child_id
LEFT JOIN foster_parent fp ON fp.child_id = p.id AND fp.valid_during @> ${bind(today)}
WHERE p.id = ANY(${bind(childIds)})
 AND (g.guardian_id IS NOT NULL OR fp.parent_id IS NOT NULL)
                    """
                        .trimIndent()
                )
            }
            .mapTo<PersonId>()
            .toList()

    fun sendMissingHolidayReminders(
        db: Database.Connection,
        msg: AsyncJob.SendMissingHolidayReservationsReminder,
        clock: EvakaClock
    ) {
        val receiver = db.read { tx -> tx.getPersonById(msg.guardian) }

        if (receiver == null) {
            logger.warn("Person ${msg.guardian} not found when sending email!")
            return
        }

        if (receiver.email.isNullOrBlank()) return
        val language = receiver.language?.lowercase()?.let(Language::tryValueOf) ?: Language.fi

        Email.create(
                dbc = db,
                personId = msg.guardian,
                emailType = EmailMessageType.MISSING_HOLIDAY_ATTENDANCE_RESERVATION_NOTIFICATION,
                fromAddress = emailEnv.sender(language),
                content = emailMessageProvider.missingHolidayReservationsNotification(language),
                traceId = msg.guardian.toString(),
            )
            ?.also { emailClient.send(it) }
    }
}

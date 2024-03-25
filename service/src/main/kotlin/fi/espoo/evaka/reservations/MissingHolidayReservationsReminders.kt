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
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.FeatureConfig
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
    private val featureConfig: FeatureConfig,
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

        return tx.getHolidayPeriodsWithReservationDeadline(clock.today().plusDays(1))
            .firstOrNull()
            ?.let { holidayPeriod ->
                logger.info(
                    "Holiday ${holidayPeriod.id} reservation deadline is due, sending missing reservation reminders"
                )

                // Get a rough guess of persons with missing holiday reservations, then do an exact
                // check per person
                // in a separate job
                val personsWithMaybeMissingHolidayReservations =
                    holidayPeriod.period
                        .dates()
                        .map { tx.getPersonsWithChildrenWithMissingReservation(it) }
                        .flatten()
                        .toSet()

                logger.info(
                    "Got ${personsWithMaybeMissingHolidayReservations.size} persons with maybe missing holiday reservations for holiday ${holidayPeriod.period}"
                )

                asyncJobRunner.plan(
                    tx,
                    payloads =
                        personsWithMaybeMissingHolidayReservations.map {
                            AsyncJob.SendMissingHolidayReservationsReminder(
                                it,
                                holidayPeriod.period
                            )
                        },
                    runAt = clock.now()
                )
                1
            } ?: 0
    }

    fun Database.Read.getPersonsWithChildrenWithMissingReservation(
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
SELECT COALESCE(g.guardian_id, fp.id) AS guardian_id -- distinct by storing these in a set in kotlin
FROM reservable_placements p
     JOIN daycare d ON d.id = p.unit_id AND extract(isodow FROM ${bind(theDate)}) = ANY(d.operation_days) AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
     LEFT JOIN guardian g ON g.child_id = p.child_id
     LEFT JOIN foster_parent fp ON fp.child_id = p.child_id AND fp.valid_during @> ${bind(theDate)}   
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
    AND (g.guardian_id IS NOT NULL OR fp.parent_id IS NOT NULL)
    AND d.provider_type != 'PRIVATE_SERVICE_VOUCHER'
    ${if (personId == null) "" else "AND guardian_id = ${bind(personId)}"}

"""
                )
            }
            .mapTo<PersonId>()
            .toList()

    fun sendMissingHolidayReminders(
        db: Database.Connection,
        msg: AsyncJob.SendMissingHolidayReservationsReminder,
        clock: EvakaClock
    ) {
        val language = db.read { tx -> getLanguage(tx, msg.guardian) }

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

    private fun getLanguage(tx: Database.Read, personId: PersonId): Language {
        return tx.createQuery {
                sql(
                    """
SELECT language
FROM person p
WHERE p.id = ${bind(personId)}
AND email IS NOT NULL
LIMIT 1
        """
                        .trimIndent()
                )
            }
            .exactlyOneOrNull {
                column<String?>("language")?.lowercase()?.let(Language::tryValueOf) ?: Language.fi
            } ?: Language.fi
    }
}

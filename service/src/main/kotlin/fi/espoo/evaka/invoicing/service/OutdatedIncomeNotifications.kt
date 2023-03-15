// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import com.fasterxml.jackson.databind.json.JsonMapper
import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.invoicing.data.upsertIncome
import fi.espoo.evaka.invoicing.domain.Income
import fi.espoo.evaka.invoicing.domain.IncomeEffect
import fi.espoo.evaka.shared.IncomeId
import fi.espoo.evaka.shared.IncomeNotificationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.auth.AuthenticatedUser
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class OutdatedIncomeNotifications(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: IEmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
    private val mapper: JsonMapper
) {
    init {
        asyncJobRunner.registerHandler(::sendEmail)
    }

    fun scheduleNotifications(tx: Database.Transaction, clock: EvakaClock): Int {
        tx.removeUnclaimedJobs(
            setOf(AsyncJobType(AsyncJob.SendOutdatedIncomeNotificationEmail::class))
        )

        val guardiansForInitialNotification =
            tx.guardiansWithOutdatedIncomeWithoutSentNotification(
                DateRange(clock.today(), clock.today().plusWeeks(4)),
                IncomeNotificationType.INITIAL_EMAIL,
                clock.now().toLocalDate()
            )

        val guardiansForReminderNotification =
            tx.guardiansWithOutdatedIncomeWithoutSentNotification(
                    DateRange(clock.today(), clock.today().plusWeeks(2)),
                    IncomeNotificationType.REMINDER_EMAIL,
                    clock.now().toLocalDate()
                )
                .filter { !guardiansForInitialNotification.contains(it) }

        val guardiansForExpirationNotification =
            tx.guardiansWithOutdatedIncomeWithoutSentNotification(
                    DateRange(clock.today(), clock.today()),
                    IncomeNotificationType.EXPIRED_EMAIL,
                    clock.now().toLocalDate()
                )
                .filter { !guardiansForInitialNotification.contains(it) }
                .filter { !guardiansForReminderNotification.contains(it) }

        asyncJobRunner.plan(
            tx,
            payloads =
                guardiansForInitialNotification
                    .map {
                        AsyncJob.SendOutdatedIncomeNotificationEmail(
                            it,
                            IncomeNotificationType.INITIAL_EMAIL
                        )
                    }
                    .plus(
                        guardiansForReminderNotification.map {
                            AsyncJob.SendOutdatedIncomeNotificationEmail(
                                it,
                                IncomeNotificationType.REMINDER_EMAIL
                            )
                        }
                    )
                    .plus(
                        guardiansForExpirationNotification.map {
                            AsyncJob.SendOutdatedIncomeNotificationEmail(
                                it,
                                IncomeNotificationType.EXPIRED_EMAIL
                            )
                        }
                    ),
            runAt = clock.now()
        )

        logger.info(
            "OutdatedIncomeNotification scheduled notification emails: ${guardiansForInitialNotification.size } initial, ${guardiansForReminderNotification.size} reminders and ${guardiansForExpirationNotification.size} expired"
        )

        return guardiansForInitialNotification.size +
            guardiansForReminderNotification.size +
            guardiansForExpirationNotification.size
    }

    fun sendEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendOutdatedIncomeNotificationEmail
    ) {
        val (recipient, language) =
            db.read { tx ->
                tx.createQuery(
                        """
SELECT email, language
FROM person p
WHERE p.id = :guardianId
AND email IS NOT NULL
        """
                            .trimIndent()
                    )
                    .bind("guardianId", msg.guardianId)
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

        logger.info("OutdatedIncomeNotifications: sending ${msg.type} email to ${msg.guardianId}")
        emailClient.sendEmail(
            traceId = msg.guardianId.toString(),
            toAddress = recipient,
            fromAddress = emailEnv.sender(language),
            content = emailMessageProvider.outdatedIncomeNotification(msg.type, language)
        )

        db.transaction {
            it.createIncomeNotification(msg.guardianId, msg.type)

            val firstDayAfterExpiration = clock.today().plusDays(1)
            val validFrom =
                if (firstDayAfterExpiration.dayOfMonth == 1) firstDayAfterExpiration
                else firstDayAfterExpiration.plusMonths(1).withDayOfMonth(1)
            if (
                msg.type == IncomeNotificationType.EXPIRED_EMAIL &&
                    !it.personHasActiveIncomeOnDate(msg.guardianId, validFrom)
            ) {
                it.upsertIncome(
                    clock = clock,
                    mapper = mapper,
                    income =
                        Income(
                            id = IncomeId(UUID.randomUUID()),
                            personId = msg.guardianId,
                            effect = IncomeEffect.INCOMPLETE,
                            updatedBy = AuthenticatedUser.SystemInternalUser.toString(),
                            validFrom = validFrom,
                            validTo = null,
                            data = emptyMap(),
                            notes = "Created automatically because previous income expired"
                        ),
                    updatedBy = AuthenticatedUser.SystemInternalUser.evakaUserId
                )

                asyncJobRunner.plan(
                    it,
                    listOf(
                        AsyncJob.GenerateFinanceDecisions.forAdult(
                            msg.guardianId,
                            DateRange(validFrom, null)
                        )
                    ),
                    runAt = clock.now()
                )
            }
        }
    }
}

fun Database.Read.personHasActiveIncomeOnDate(personId: PersonId, theDate: LocalDate): Boolean {
    return createQuery(
            """
                    SELECT 1
                    FROM income
                    WHERE daterange(valid_from, valid_to, '[]') @> :the_date
                        AND person_id = :personId
                """
                .trimIndent()
        )
        .bind("personId", personId)
        .bind("the_date", theDate)
        .mapTo<Int>()
        .list()
        .isNotEmpty()
}

enum class IncomeNotificationType {
    INITIAL_EMAIL,
    REMINDER_EMAIL,
    EXPIRED_EMAIL
}

fun Database.Read.guardiansWithOutdatedIncomeWithoutSentNotification(
    checkForExpirationRange: DateRange,
    notificationType: IncomeNotificationType,
    today: LocalDate
): List<PersonId> {
    return createQuery(
            """
WITH latest_income AS (
    SELECT DISTINCT ON (person_id)
    person_id, valid_to
    FROM income i 
    ORDER BY person_id, valid_to DESC
), billable_placements_day_after_expiration AS (
    SELECT pl.id, pl.child_id, g.guardian_id
    FROM placement pl
    JOIN service_need sn ON pl.id = sn.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> upper(:checkForExpirationRange)
    JOIN service_need_option sno ON sn.option_id = sno.id AND sno.fee_coefficient > 0
    JOIN guardian g ON g.child_id = pl.child_id
    JOIN latest_income i ON i.person_id = g.guardian_id
    WHERE :checkForExpirationRange @> i.valid_to
     AND daterange(pl.start_date, pl.end_date, '[]') @> (i.valid_to + INTERVAL '1 day')::date
)
SELECT pl.guardian_id
FROM billable_placements_day_after_expiration pl 
WHERE NOT EXISTS (
    SELECT 1 FROM income_notification 
    WHERE receiver_id = pl.guardian_id AND notification_type = :notificationType AND created > :today - INTERVAL '1 month' )
AND NOT EXISTS (
    SELECT 1 FROM income_statement
    WHERE person_id = pl.guardian_id AND created > :today - INTERVAL '1 month'
        AND (end_date IS NULL OR UPPER(:checkForExpirationRange) <= end_date)
)    
    """
                .trimIndent()
        )
        .bind("checkForExpirationRange", checkForExpirationRange)
        .bind("notificationType", notificationType)
        .bind("today", today)
        .mapTo<PersonId>()
        .list()
}

data class IncomeNotification(
    val receiverId: PersonId,
    val notificationType: IncomeNotificationType,
    val created: HelsinkiDateTime
)

fun Database.Transaction.createIncomeNotification(
    receiverId: PersonId,
    notificationType: IncomeNotificationType
): IncomeNotificationId {
    return createUpdate(
            """
        INSERT INTO income_notification(receiver_id, notification_type)
        VALUES (:receiverId, :notificationType)
        RETURNING id
    """
                .trimIndent()
        )
        .bind("receiverId", receiverId)
        .bind("notificationType", notificationType)
        .executeAndReturnGeneratedKeys()
        .mapTo<IncomeNotificationId>()
        .one()
}

fun Database.Read.getIncomeNotifications(receiverId: PersonId): List<IncomeNotification> =
    createQuery(
            """SELECT receiver_id, notification_type, created FROM income_notification WHERE receiver_id = :receiverId"""
        )
        .bind("receiverId", receiverId)
        .mapTo<IncomeNotification>()
        .list()

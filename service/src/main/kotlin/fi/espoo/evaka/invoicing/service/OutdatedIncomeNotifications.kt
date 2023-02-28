// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.IEmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.shared.DatabaseTable
import fi.espoo.evaka.shared.FeatureConfig
import fi.espoo.evaka.shared.IncomeNotificationId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import org.springframework.stereotype.Service

@Service
class OutdatedIncomeNotifications(
    private val featureConfig: FeatureConfig,
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: IEmailClient,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv
) {
    init {
        asyncJobRunner.registerHandler { db, _, msg: AsyncJob.SendOutdatedIncomeNotificationEmail ->
            sendEmail(db, msg)
        }
    }

    fun scheduleNotifications(tx: Database.Transaction, clock: EvakaClock): Int {
        tx.removeUnclaimedJobs(
            setOf(AsyncJobType(AsyncJob.SendOutdatedIncomeNotificationEmail::class))
        )

        val guardiansForInitialNotification =
            tx.guardiansWithOutdatedIncomeWithoutSentNotification(
                DateRange(clock.today(), clock.today().plusDays(13)),
                IncomeNotificationType.INITIAL_EMAIL,
                clock.now()
            )

        val guardiansForReminderNotification =
            tx.guardiansWithOutdatedIncomeWithoutSentNotification(
                    DateRange(clock.today(), clock.today().plusDays(6)),
                    IncomeNotificationType.REMINDER_EMAIL,
                    clock.now()
                )
                .filter { !guardiansForInitialNotification.contains(it) }

        val guardiansForExpirationNotification =
            tx.guardiansWithOutdatedIncomeWithoutSentNotification(
                    DateRange(clock.today(), clock.today()),
                    IncomeNotificationType.EXPIRED_EMAIL,
                    clock.now()
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

        return guardiansForInitialNotification.size +
            guardiansForReminderNotification.size +
            guardiansForExpirationNotification.size
    }

    fun sendEmail(db: Database.Connection, msg: AsyncJob.SendOutdatedIncomeNotificationEmail) {
        val (recipient, language) =
            db.read { tx ->
                tx.createQuery<DatabaseTable> {
                        sql(
                            """
SELECT email, language
FROM person p
WHERE p.id = ${bind(msg.guardianId)}
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
        emailClient
            .sendEmail(
                traceId = msg.guardianId.toString(),
                toAddress = recipient,
                fromAddress = emailEnv.sender(language),
                content = emailMessageProvider.outdatedIncomeNotification(msg.type, language)
            )
            .also { db.transaction { it.createIncomeNotification(msg.guardianId, msg.type) } }
    }
}

enum class IncomeNotificationType {
    INITIAL_EMAIL,
    REMINDER_EMAIL,
    EXPIRED_EMAIL
}

// TODO: max_fee_accepted mutta on valid_to asetettu?
// TODO: jos uusi tuloselvitys, ei muistuteta tuloista
fun Database.Read.guardiansWithOutdatedIncomeWithoutSentNotification(
    checkForExpirationRange: DateRange,
    notificationType: IncomeNotificationType,
    now: HelsinkiDateTime
): List<PersonId> {
    return createQuery(
            """
WITH latest_income AS (
    SELECT DISTINCT ON (person_id)
    person_id, valid_to
    FROM income i 
    ORDER BY person_id, valid_to DESC
)
SELECT g.guardian_id
FROM placement pl 
LEFT JOIN guardian g ON g.child_id = pl.child_id
LEFT JOIN latest_income i ON i.person_id = g.guardian_id
WHERE daterange(pl.start_date, pl.end_date, '[]') @> i.valid_to
AND pl.type != 'CLUB'::placement_type   
AND :checkForExpirationRange @> i.valid_to
AND NOT EXISTS (
    SELECT 1 FROM income_notification 
    WHERE receiver_id = g.guardian_id AND notification_type = :notificationType AND created > :now - INTERVAL '1 month' )
AND NOT EXISTS (
    SELECT 1 FROM income_statement
    WHERE person_id = g.guardian_id AND :checkForExpirationRange << daterange(income_statement.start_date, income_statement.end_date)
)    
    """
                .trimIndent()
        )
        .bind("checkForExpirationRange", checkForExpirationRange)
        .bind("notificationType", notificationType)
        .bind("now", now)
        .mapTo<PersonId>()
        .list()
}

data class IncomeNotification(
    val receiverId: PersonId,
    val notificationType: IncomeNotificationType
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
            """SELECT receiver_id, notification_type FROM income_notification WHERE receiver_id = :receiverId"""
        )
        .bind("receiverId", receiverId)
        .mapTo<IncomeNotification>()
        .list()

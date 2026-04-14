// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.messaging.mobile

import fi.espoo.evaka.shared.MessageId
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import org.springframework.stereotype.Service

@Service
class CitizenMessagePushNotifications(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val expoPushClient: ExpoPushClient,
) {
    init {
        asyncJobRunner.registerHandler(::fanOut)
        asyncJobRunner.registerHandler(::sendOne)
    }

    fun getAsyncJobs(
        tx: Database.Read,
        messages: Collection<MessageId>,
    ): List<AsyncJob.NotifyCitizenOfNewMessage> =
        tx.createQuery {
                sql(
                    """
                    SELECT mr.message_id, ma.person_id AS recipient_id
                    FROM message_recipients mr
                    JOIN message_account ma ON mr.recipient_id = ma.id
                    WHERE mr.message_id = ANY(${bind(messages)})
                    AND ma.type = 'CITIZEN'
                    AND mr.read_at IS NULL
                    AND EXISTS (
                        SELECT 1 FROM citizen_push_subscription cps
                        WHERE cps.citizen_id = ma.person_id
                    )
                    """
                )
            }
            .toList<AsyncJob.NotifyCitizenOfNewMessage>()

    private fun fanOut(
        db: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.NotifyCitizenOfNewMessage,
    ) {
        val subscriptions = db.read { it.getCitizenPushSubscriptions(job.recipientId) }
        val jobs =
            subscriptions.map { sub ->
                AsyncJob.SendCitizenMessagePushNotification(
                    messageId = job.messageId,
                    citizenId = sub.citizenId,
                    deviceId = sub.deviceId,
                )
            }
        if (jobs.isNotEmpty()) {
            db.transaction { tx -> asyncJobRunner.plan(tx, jobs, runAt = clock.now()) }
        }
    }

    private fun sendOne(
        db: Database.Connection,
        clock: EvakaClock,
        job: AsyncJob.SendCitizenMessagePushNotification,
    ) {
        val subscription =
            db.read { it.getCitizenPushSubscription(job.citizenId, job.deviceId) } ?: return

        val result =
            expoPushClient.send(
                to = subscription.expoPushToken,
                title = "Uusi viesti",
                body = "Sinulle on uusi viesti eVakassa",
                data = mapOf("messageId" to job.messageId.toString()),
            )

        if (result.deviceNotRegistered) {
            db.transaction { it.deleteCitizenPushSubscription(job.citizenId, job.deviceId) }
        }
    }
}

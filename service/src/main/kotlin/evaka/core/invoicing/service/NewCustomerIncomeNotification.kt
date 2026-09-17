// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.invoicing.service

import evaka.core.EmailEnv
import evaka.core.emailclient.Email
import evaka.core.emailclient.EmailClient
import evaka.core.emailclient.IEmailMessageProvider
import evaka.core.pis.NotificationCategory
import evaka.core.shared.async.AsyncJob
import evaka.core.shared.async.AsyncJobRunner
import evaka.core.shared.async.AsyncJobType
import evaka.core.shared.async.removeUnclaimedJobs
import evaka.core.shared.db.Database
import evaka.core.shared.domain.EvakaClock
import evaka.core.webpush.CitizenPushNotification
import evaka.core.webpush.CitizenPushNotifications
import evaka.core.webpush.hasCitizenPushSubscriptions
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Duration
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class NewCustomerIncomeNotification(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
    private val citizenPushNotifications: CitizenPushNotifications,
    private val emailMessageProvider: IEmailMessageProvider,
    private val emailEnv: EmailEnv,
) {
    init {
        asyncJobRunner.registerHandler(::sendEmail)
    }

    fun scheduleNotifications(tx: Database.Transaction, clock: EvakaClock): Int {
        tx.removeUnclaimedJobs(
            setOf(AsyncJobType(AsyncJob.SendNewCustomerIncomeNotificationEmail::class))
        )

        tx.setStatementTimeout(Duration.ofSeconds(300))
        val guardiansForNotification = tx.newCustomerIdsForIncomeNotifications(clock.today(), null)

        asyncJobRunner.plan(
            tx,
            payloads =
                guardiansForNotification.map {
                    AsyncJob.SendNewCustomerIncomeNotificationEmail(it)
                },
            runAt = clock.now(),
        )

        logger.info {
            "NewCustomerIncomeNotification scheduled notification emails: ${guardiansForNotification.size}"
        }

        return guardiansForNotification.size
    }

    fun sendEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendNewCustomerIncomeNotificationEmail,
    ) {
        val recipient =
            db.read { tx -> tx.getIncomeNotificationRecipient(msg.guardianId) } ?: return

        if (
            db.read { tx -> tx.newCustomerIdsForIncomeNotifications(clock.today(), msg.guardianId) }
                .contains(msg.guardianId)
        ) {
            val hasPush = db.read { tx -> tx.hasCitizenPushSubscriptions(msg.guardianId) }
            if (!recipient.hasEmail && !hasPush) return

            logger.info {
                "NewCustomerIncomeNotification: sending notification to ${msg.guardianId}"
            }

            if (recipient.hasEmail) {
                Email.create(
                        dbc = db,
                        category = NotificationCategory.INCOME_NOTIFICATION,
                        personId = msg.guardianId,
                        fromAddress = emailEnv.sender(recipient.language),
                        content =
                            emailMessageProvider.incomeNotification(
                                IncomeNotificationType.NEW_CUSTOMER,
                                recipient.language,
                            ),
                        traceId = msg.guardianId.toString(),
                    )
                    ?.also { emailClient.send(it) }
            }

            db.transaction { tx ->
                citizenPushNotifications.plan(
                    tx,
                    clock.now(),
                    msg.guardianId,
                    CitizenPushNotification.Income(IncomeNotificationType.NEW_CUSTOMER),
                )
                tx.createIncomeNotification(msg.guardianId, IncomeNotificationType.NEW_CUSTOMER)
            }
        } else {
            logger.info {
                "Skipping NewCustomerIncomeNotification: ${msg.guardianId} is no longer valid recipient"
            }
        }
    }
}

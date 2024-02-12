// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.shared.async.AsyncJob
import fi.espoo.evaka.shared.async.AsyncJobRunner
import fi.espoo.evaka.shared.async.AsyncJobType
import fi.espoo.evaka.shared.async.removeUnclaimedJobs
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class NewCustomerIncomeNotification(
    private val asyncJobRunner: AsyncJobRunner<AsyncJob>,
    private val emailClient: EmailClient,
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

        val guardiansForNotification =
            tx.newCustomerIdsForIncomeNotifications(
                clock.today(),
            )

        asyncJobRunner.plan(
            tx,
            payloads =
                guardiansForNotification.map {
                    AsyncJob.SendNewCustomerIncomeNotificationEmail(it)
                },
            runAt = clock.now()
        )

        logger.info(
            "NewCustomerIncomeNotification scheduled notification emails: ${guardiansForNotification.size}"
        )

        return guardiansForNotification.size
    }

    fun sendEmail(
        db: Database.Connection,
        clock: EvakaClock,
        msg: AsyncJob.SendNewCustomerIncomeNotificationEmail
    ) {
        val language =
            db.read { tx ->
                tx.createQuery(
                        """
                        SELECT language
                        FROM person p
                        WHERE p.id = :guardianId
                        AND email IS NOT NULL
                    """
                            .trimIndent()
                    )
                    .bind("guardianId", msg.guardianId)
                    .exactlyOneOrNull {
                        column<String?>("language")?.lowercase()?.let(Language::tryValueOf)
                            ?: Language.fi
                    }
            } ?: return

        if (
            db.read { tx ->
                    tx.newCustomerIdsForIncomeNotifications(
                        clock.today(),
                    )
                }
                .contains(msg.guardianId)
        ) {
            logger.info(
                "NewCustomerIncomeNotification: sending notification email to ${msg.guardianId}"
            )

            Email.create(
                    dbc = db,
                    emailType = EmailMessageType.NEW_CUSTOMER_INCOME_NOTIFICATION,
                    personId = msg.guardianId,
                    fromAddress = emailEnv.sender(language),
                    content =
                        emailMessageProvider.incomeNotification(
                            IncomeNotificationType.NEW_CUSTOMER,
                            language
                        ),
                    traceId = msg.guardianId.toString()
                )
                ?.also { emailClient.send(it) }

            db.transaction {
                it.createIncomeNotification(msg.guardianId, IncomeNotificationType.NEW_CUSTOMER)
            }
        } else {
            logger.info(
                "Skipping NewCustomerIncomeNotification: ${msg.guardianId} is no longer valid recipient"
            )
        }
    }
}

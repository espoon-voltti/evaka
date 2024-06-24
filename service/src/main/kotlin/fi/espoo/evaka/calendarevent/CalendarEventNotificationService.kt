// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.calendarevent

import fi.espoo.evaka.EmailEnv
import fi.espoo.evaka.emailclient.Email
import fi.espoo.evaka.emailclient.EmailClient
import fi.espoo.evaka.emailclient.IEmailMessageProvider
import fi.espoo.evaka.pis.EmailMessageType
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class CalendarEventNotificationService(
    private val emailClient: EmailClient,
    private val emailEnv: EmailEnv,
    private val emailMessageProvider: IEmailMessageProvider
) {
    fun sendCalendarEventDigests(
        dbc: Database.Connection,
        now: HelsinkiDateTime
    ) {
        dbc
            .read { tx -> tx.getParentsWithNewEventsAfter(now.minusHours(24)) }
            .also { parents ->
                logger.info { "Sending calendar event notifications to ${parents.size} parents" }
            }.forEach { parent ->
                Email
                    .create(
                        dbc,
                        parent.parentId,
                        EmailMessageType.CALENDAR_EVENT_NOTIFICATION,
                        emailEnv.sender(parent.language),
                        emailMessageProvider.calendarEventNotification(
                            parent.language,
                            parent.events
                        ),
                        "${now.toLocalDate()}:${parent.parentId}"
                    )?.also { emailClient.send(it) }
            }
    }
}

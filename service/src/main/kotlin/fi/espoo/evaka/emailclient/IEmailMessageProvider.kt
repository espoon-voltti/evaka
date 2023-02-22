// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.invoicing.service.IncomeNotificationType
import fi.espoo.evaka.messaging.MessageThreadStub
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.domain.FiniteDateRange

data class EmailContent(
    val subject: String,
    val text: String,
    @org.intellij.lang.annotations.Language("html") val html: String
)

interface IEmailMessageProvider {
    fun pendingDecisionNotification(language: Language): EmailContent

    fun clubApplicationReceived(language: Language): EmailContent
    fun daycareApplicationReceived(language: Language): EmailContent
    fun preschoolApplicationReceived(
        language: Language,
        withinApplicationPeriod: Boolean
    ): EmailContent

    fun assistanceNeedDecisionNotification(language: Language): EmailContent

    fun missingReservationsNotification(
        language: Language,
        checkedRange: FiniteDateRange
    ): EmailContent

    fun messageNotification(language: Language, thread: MessageThreadStub): EmailContent

    fun vasuNotification(language: Language, childId: ChildId): EmailContent

    fun pedagogicalDocumentNotification(language: Language): EmailContent

    fun outdatedIncomeNotification(
        notificationType: IncomeNotificationType,
        language: Language
    ): EmailContent
}

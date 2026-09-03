// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.messaging.MessageType
import evaka.core.shared.domain.UiLanguage

data class PushNotificationContent(val title: String, val body: String?)

data class MessagePushNotificationData(
    val type: MessageType,
    val urgent: Boolean,
    val sensitive: Boolean,
    val senderName: String?,
    val title: String,
    val isSenderMunicipalAccount: Boolean,
)

interface PushNotificationMessageProvider {
    fun messageNotification(
        language: UiLanguage,
        data: MessagePushNotificationData,
    ): PushNotificationContent

    fun testNotification(language: UiLanguage): PushNotificationContent
}

// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.webpush

import evaka.core.messaging.MessageType
import evaka.core.shared.domain.UiLanguage

class EvakaPushNotificationMessageProvider : PushNotificationMessageProvider {
    override fun messageNotification(
        language: UiLanguage,
        data: MessagePushNotificationData,
    ): PushNotificationContent {
        val kind =
            when (data.type) {
                MessageType.MESSAGE ->
                    when (language) {
                        UiLanguage.FI ->
                            if (data.urgent) "Uusi kiireellinen viesti" else "Uusi viesti"
                        UiLanguage.SV ->
                            if (data.urgent) "Nytt brådskande meddelande" else "Nytt meddelande"
                        UiLanguage.EN -> if (data.urgent) "New urgent message" else "New message"
                    }

                MessageType.BULLETIN ->
                    when (language) {
                        UiLanguage.FI ->
                            if (data.urgent) "Uusi kiireellinen tiedote" else "Uusi tiedote"
                        UiLanguage.SV ->
                            if (data.urgent) "Nytt brådskande meddelande" else "Nytt meddelande"
                        UiLanguage.EN -> if (data.urgent) "New urgent bulletin" else "New bulletin"
                    }
            }

        val showTitle = data.isSenderMunicipalAccount && data.type == MessageType.BULLETIN
        return PushNotificationContent(
            title = kind,
            body =
                when {
                    showTitle -> data.title
                    data.sensitive -> null
                    else -> data.senderName
                },
        )
    }

    override fun testNotification(language: UiLanguage): PushNotificationContent =
        when (language) {
            UiLanguage.FI ->
                PushNotificationContent(
                    "Testi-ilmoitus",
                    "Push-ilmoitukset toimivat tällä laitteella.",
                )

            UiLanguage.SV ->
                PushNotificationContent(
                    "Testnotis",
                    "Push-notiser fungerar på den här enheten.",
                )

            UiLanguage.EN ->
                PushNotificationContent(
                    "Test notification",
                    "Push notifications work on this device.",
                )
        }
}

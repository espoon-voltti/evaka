// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

data class PushTitleAndBody(val title: String, val body: String)

object CitizenPushMessages {
    fun forMessage(
        category: CitizenPushCategory,
        language: CitizenPushLanguage,
        senderName: String,
    ): PushTitleAndBody =
        when (language) {
            CitizenPushLanguage.FI -> finnish(category, senderName)
            CitizenPushLanguage.SV -> swedish(category, senderName)
            CitizenPushLanguage.EN -> english(category, senderName)
        }

    fun forTest(language: CitizenPushLanguage): PushTitleAndBody =
        when (language) {
            CitizenPushLanguage.FI ->
                PushTitleAndBody("eVaka", "Push-ilmoitukset on otettu käyttöön.")
            CitizenPushLanguage.SV ->
                PushTitleAndBody("eVaka", "Push-notiser har aktiverats.")
            CitizenPushLanguage.EN ->
                PushTitleAndBody("eVaka", "Push notifications are now enabled.")
        }

    private fun finnish(category: CitizenPushCategory, sender: String) =
        when (category) {
            CitizenPushCategory.URGENT_MESSAGE ->
                PushTitleAndBody("Kiireellinen viesti", "$sender lähetti sinulle kiireellisen viestin.")
            CitizenPushCategory.MESSAGE ->
                PushTitleAndBody("Uusi viesti", "$sender lähetti sinulle viestin.")
            CitizenPushCategory.BULLETIN ->
                PushTitleAndBody("Uusi tiedote", "$sender on lähettänyt uuden tiedotteen.")
        }

    private fun swedish(category: CitizenPushCategory, sender: String) =
        when (category) {
            CitizenPushCategory.URGENT_MESSAGE ->
                PushTitleAndBody("Brådskande meddelande", "$sender har skickat dig ett brådskande meddelande.")
            CitizenPushCategory.MESSAGE ->
                PushTitleAndBody("Nytt meddelande", "$sender har skickat dig ett meddelande.")
            CitizenPushCategory.BULLETIN ->
                PushTitleAndBody("Nytt meddelande", "$sender har publicerat ett nytt meddelande.")
        }

    private fun english(category: CitizenPushCategory, sender: String) =
        when (category) {
            CitizenPushCategory.URGENT_MESSAGE ->
                PushTitleAndBody("Urgent message", "$sender sent you an urgent message.")
            CitizenPushCategory.MESSAGE ->
                PushTitleAndBody("New message", "$sender sent you a message.")
            CitizenPushCategory.BULLETIN ->
                PushTitleAndBody("New bulletin", "$sender posted a new bulletin.")
        }
}

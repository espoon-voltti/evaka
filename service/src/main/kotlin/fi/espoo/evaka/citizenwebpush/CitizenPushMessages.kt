// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

data class PushTitleAndBody(val title: String, val body: String)

data class PushReplyActionStrings(
    val actionLabel: String,
    val actionPlaceholder: String,
    val successTitle: String,
    val successBody: String,
    val errorTitle: String,
    val errorBody: String,
)

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
            CitizenPushLanguage.SV -> PushTitleAndBody("eVaka", "Push-notiser har aktiverats.")
            CitizenPushLanguage.EN ->
                PushTitleAndBody("eVaka", "Push notifications are now enabled.")
        }

    fun forReplyAction(language: CitizenPushLanguage): PushReplyActionStrings =
        when (language) {
            CitizenPushLanguage.FI ->
                PushReplyActionStrings(
                    actionLabel = "Vastaa",
                    actionPlaceholder = "Kirjoita vastaus…",
                    successTitle = "Vastaus lähetetty",
                    successBody = "Vastauksesi on toimitettu.",
                    errorTitle = "Vastauksen lähetys epäonnistui",
                    errorBody = "Avaa eVaka ja yritä uudelleen.",
                )
            CitizenPushLanguage.SV ->
                PushReplyActionStrings(
                    actionLabel = "Svara",
                    actionPlaceholder = "Skriv ett svar…",
                    successTitle = "Svaret skickat",
                    successBody = "Ditt svar har levererats.",
                    errorTitle = "Svaret kunde inte skickas",
                    errorBody = "Öppna eVaka och försök igen.",
                )
            CitizenPushLanguage.EN ->
                PushReplyActionStrings(
                    actionLabel = "Reply",
                    actionPlaceholder = "Type a reply…",
                    successTitle = "Reply sent",
                    successBody = "Your reply was delivered.",
                    errorTitle = "Reply not sent",
                    errorBody = "Open eVaka to retry.",
                )
        }

    private fun finnish(category: CitizenPushCategory, sender: String) =
        when (category) {
            CitizenPushCategory.URGENT_MESSAGE ->
                PushTitleAndBody(
                    "Kiireellinen viesti",
                    "$sender lähetti sinulle kiireellisen viestin.",
                )
            CitizenPushCategory.MESSAGE ->
                PushTitleAndBody("Uusi viesti", "$sender lähetti sinulle viestin.")
            CitizenPushCategory.BULLETIN ->
                PushTitleAndBody("Uusi tiedote", "$sender on lähettänyt uuden tiedotteen.")
        }

    private fun swedish(category: CitizenPushCategory, sender: String) =
        when (category) {
            CitizenPushCategory.URGENT_MESSAGE ->
                PushTitleAndBody(
                    "Brådskande meddelande",
                    "$sender har skickat dig ett brådskande meddelande.",
                )
            CitizenPushCategory.MESSAGE ->
                PushTitleAndBody("Nytt meddelande", "$sender har skickat dig ett meddelande.")
            CitizenPushCategory.BULLETIN ->
                PushTitleAndBody(
                    "Nytt meddelande från kommunen",
                    "$sender har publicerat ett nytt meddelande.",
                )
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

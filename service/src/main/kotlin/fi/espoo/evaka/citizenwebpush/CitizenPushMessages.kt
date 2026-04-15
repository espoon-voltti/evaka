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
    // Keep the payload well within the per-platform limits Chrome / Safari
    // actually render without truncation.
    private const val MAX_TITLE_LENGTH = 80
    private const val MAX_BODY_LENGTH = 200

    /**
     * Build a push title + body from the actual message context so the notification shown on the
     * user's device carries the thread title and a preview of the message text instead of a generic
     * "Alice sent you a message". Urgency, category and icon are already carried by the payload
     * separately and rendered by the service worker, so we don't add any text prefix here.
     *
     * Title = thread title. Body = "sender: preview". Both are clamped to supported length budgets;
     * whitespace in the message content is collapsed so multi-paragraph messages still preview as a
     * single line.
     */
    fun forMessage(
        senderName: String,
        threadTitle: String,
        messageContent: String,
    ): PushTitleAndBody {
        val normalizedContent = messageContent.replace(Regex("\\s+"), " ").trim()
        return PushTitleAndBody(
            title = truncate(threadTitle.trim(), MAX_TITLE_LENGTH),
            body = truncate("$senderName: $normalizedContent", MAX_BODY_LENGTH),
        )
    }

    private fun truncate(s: String, max: Int): String =
        if (s.length > max) s.take(max - 1).trimEnd() + "…" else s

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
}

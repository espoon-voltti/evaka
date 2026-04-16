// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class CitizenPushMessagesTest {
    @Test
    fun `forReplyAction returns Finnish strings`() {
        val s = CitizenPushMessages.forReplyAction(CitizenPushLanguage.FI)
        assertEquals("Vastaa", s.actionLabel)
        assertEquals("Kirjoita vastaus…", s.actionPlaceholder)
        assertEquals("Vastaus lähetetty", s.successTitle)
        assertEquals("Vastauksesi on toimitettu.", s.successBody)
        assertEquals("Vastauksen lähetys epäonnistui", s.errorTitle)
        assertEquals("Avaa eVaka ja yritä uudelleen.", s.errorBody)
    }

    @Test
    fun `forReplyAction returns Swedish strings`() {
        val s = CitizenPushMessages.forReplyAction(CitizenPushLanguage.SV)
        assertEquals("Svara", s.actionLabel)
        assertEquals("Skriv ett svar…", s.actionPlaceholder)
        assertEquals("Svaret skickat", s.successTitle)
        assertEquals("Ditt svar har levererats.", s.successBody)
        assertEquals("Svaret kunde inte skickas", s.errorTitle)
        assertEquals("Öppna eVaka och försök igen.", s.errorBody)
    }

    @Test
    fun `forReplyAction returns English strings`() {
        val s = CitizenPushMessages.forReplyAction(CitizenPushLanguage.EN)
        assertEquals("Reply", s.actionLabel)
        assertEquals("Type a reply…", s.actionPlaceholder)
        assertEquals("Reply sent", s.successTitle)
        assertEquals("Your reply was delivered.", s.successBody)
        assertEquals("Reply not sent", s.errorTitle)
        assertEquals("Open eVaka to retry.", s.errorBody)
    }
}

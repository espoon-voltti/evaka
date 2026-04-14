// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.citizenwebpush

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class CitizenPushMessagesTest {
    @Test
    fun `forReplyAction returns Finnish strings`() {
        val s = CitizenPushMessages.forReplyAction(CitizenPushLanguage.FI)
        assertEquals("Vastaa", s.actionLabel)
        assertEquals("Kirjoita vastaus…", s.actionPlaceholder)
        assertEquals("Vastaus lähetetty", s.successTitle)
        assertTrue(s.successBody.isNotBlank())
        assertEquals("Vastauksen lähetys epäonnistui", s.errorTitle)
        assertTrue(s.errorBody.isNotBlank())
    }

    @Test
    fun `forReplyAction returns Swedish strings`() {
        val s = CitizenPushMessages.forReplyAction(CitizenPushLanguage.SV)
        assertEquals("Svara", s.actionLabel)
        assertEquals("Skriv ett svar…", s.actionPlaceholder)
        assertEquals("Svaret skickat", s.successTitle)
        assertTrue(s.successBody.isNotBlank())
        assertEquals("Svaret kunde inte skickas", s.errorTitle)
        assertTrue(s.errorBody.isNotBlank())
    }

    @Test
    fun `forReplyAction returns English strings`() {
        val s = CitizenPushMessages.forReplyAction(CitizenPushLanguage.EN)
        assertEquals("Reply", s.actionLabel)
        assertEquals("Type a reply…", s.actionPlaceholder)
        assertEquals("Reply sent", s.successTitle)
        assertTrue(s.successBody.isNotBlank())
        assertEquals("Reply not sent", s.errorTitle)
        assertTrue(s.errorBody.isNotBlank())
    }
}

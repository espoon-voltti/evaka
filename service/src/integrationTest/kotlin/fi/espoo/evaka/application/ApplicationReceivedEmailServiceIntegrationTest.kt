// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.application

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.emailclient.MockEmail
import fi.espoo.evaka.emailclient.MockEmailClient
import fi.espoo.evaka.testAdult_1
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ApplicationReceivedEmailServiceIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var sendApplicationReceivedEmailService: SendApplicationReceivedEmailService
    private val personId = testAdult_1.id

    @AfterEach
    fun cleanup() {
        MockEmailClient.emails.clear()
    }

    @Test
    fun `valid email is sent`() {
        sendApplicationReceivedEmailService.sendApplicationEmail(personId, "working@test.fi", Language.fi)
        assertEmail(
            MockEmailClient.getEmail("working@test.fi"),
            "working@test.fi",
            "Test email sender fi <testemail_fi@test.com>",
            "Olemme vastaanottaneet hakemuksenne",
            "Varhaiskasvatushakemuksella on <strong>neljän (4) kuukauden hakuaika",
            "Varhaiskasvatushakemuksella on neljän (4) kuukauden hakuaika"
        )

        sendApplicationReceivedEmailService.sendApplicationEmail(personId, "Working.Email@Test.Com", Language.sv)
        assertEmail(
            MockEmailClient.getEmail("Working.Email@Test.Com"),
            "Working.Email@Test.Com",
            "Test email sender sv <testemail_sv@test.com>",
            "Vi har tagit emot din ansökan",
            "Ansökan om småbarnspedagogik har en <strong>ansökningstid på fyra (4) månader",
            "Ansökan om småbarnspedagogik har en ansökningstid på fyra (4) månader"
        )
    }

    @Test
    fun `email with invalid toAddress is not sent`() {
        sendApplicationReceivedEmailService.sendApplicationEmail(personId, "not.working.com", Language.fi)
        sendApplicationReceivedEmailService.sendApplicationEmail(personId, "@test.fi", Language.fi)

        assertEquals(0, MockEmailClient.emails.size)
    }

    private fun assertEmail(email: MockEmail?, expectedToAddress: String, expectedFromAddress: String, expectedSubject: String, expectedHtmlPart: String, expectedTextPart: String) {
        assertNotNull(email)
        assertEquals(expectedToAddress, email?.toAddress)
        assertEquals(expectedFromAddress, email?.fromAddress)
        assertEquals(expectedSubject, email?.subject)
        assert(email!!.htmlBody.contains(expectedHtmlPart, true))
        assert(email!!.textBody.contains(expectedTextPart, true))
    }
}

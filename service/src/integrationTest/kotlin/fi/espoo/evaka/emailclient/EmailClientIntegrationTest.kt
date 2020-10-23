// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.emailclient

import fi.espoo.evaka.FullApplicationTest
import fi.espoo.evaka.daycare.domain.Language
import fi.espoo.evaka.testAdult_1
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EmailClientIntegrationTest : FullApplicationTest() {
    @Autowired
    lateinit var emailClient: IEmailClient
    private val personId = testAdult_1.id

    @Test
    fun `email validation works`() {
        MockEmailClient.applicationEmails.clear()
        val valid1 = "working@test.fi"
        val valid2 = "Working.Email@Test.Com"
        val valid3 = "working.email@test.fi.fi"
        val notValid1 = "not.working.com"
        val notValid2 = "@test.fi"

        emailClient.sendApplicationEmail(personId, valid1, Language.fi)
        emailClient.sendApplicationEmail(personId, valid2, Language.fi)
        emailClient.sendApplicationEmail(personId, valid3, Language.fi)
        assertEquals(3, MockEmailClient.applicationEmails.size)
        emailClient.sendApplicationEmail(personId, notValid1, Language.fi)
        emailClient.sendApplicationEmail(personId, notValid2, Language.fi)
        emailClient.sendApplicationEmail(personId, null, Language.fi)

        assertEquals(3, MockEmailClient.applicationEmails.size)
    }
}
